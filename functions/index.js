/**
 * Konektto Cloud Functions -- push notification delivery.
 *
 * Why this exists at all: an Android device cannot push a notification to
 * another Android device directly. FCM tokens are one-way delivery
 * addresses -- something with server credentials (this backend) has to be
 * the one asking Google's servers to deliver a message to a specific
 * token. The Android client (KonecttoMessagingService) only *receives*;
 * everything here is what *sends*.
 *
 * Deployment (from the repo root):
 *   1. npm install -g firebase-tools          (if you don't have it already)
 *   2. firebase login
 *   3. firebase use --add                     (select your Konektto Firebase project)
 *   4. cd functions && npm install
 *   5. firebase deploy --only functions
 *
 * Requires the Blaze (pay-as-you-go) plan -- Firestore-triggered Cloud
 * Functions aren't available on the free Spark plan. In practice, for an
 * app this size, actual cost is close to $0/month; Firebase's free
 * invocation tier comfortably covers casual usage.
 */

const {onDocumentCreated, onDocumentUpdated} = require("firebase-functions/v2/firestore");
const {defineSecret} = require("firebase-functions/params");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");
const {logger} = require("firebase-functions");

// Stored via `firebase functions:secrets:set PERSPECTIVE_API_KEY`, never
// hardcoded here or anywhere in the Android app -- an API key baked into
// the client is visible to anyone who decompiles the APK.
const perspectiveApiKey = defineSecret("PERSPECTIVE_API_KEY");

initializeApp();
const db = getFirestore();
const messaging = getMessaging();

/**
 * Sends a data-only push to one user. Data-only (never a top-level
 * "notification" payload) is deliberate -- it keeps KonecttoMessagingService
 * in control of building the notification and its deep-link intent in
 * every app state (foreground, background, killed), rather than letting
 * Android auto-display a generic system notification whenever the app
 * isn't in the foreground.
 */
async function sendPushToUser(userId, title, body, data = {}) {

  const userDoc = await db.collection("users").doc(userId).get();
  const token = userDoc.exists ? userDoc.data().fcmToken : null;

  if (!token) {
    logger.info(`No FCM token for user ${userId}, skipping push.`);
    return;
  }

  try {

    await messaging.send({
      token,
      data: {
        title,
        body,
        ...data,
      },
      android: {
        priority: "high",
      },
    });

  } catch (err) {

    logger.error(`Failed to send push to ${userId}:`, err.message);

    // A stale token (app uninstalled, data cleared, token rotated without
    // us hearing about it yet) is routine, not exceptional. Clean it up
    // so we stop paying the cost of retrying a dead destination forever.
    if (err.code === "messaging/registration-token-not-registered") {
      await db.collection("users").doc(userId).update({fcmToken: null});
    }

  }

}

async function getUsername(userId) {
  const doc = await db.collection("users").doc(userId).get();
  return doc.exists ? (doc.data().username || "Someone") : "Someone";
}

exports.onPrivateMessageCreated = onDocumentCreated(
  "privateChats/{chatId}/messages/{messageId}",
  async (event) => {

    const message = event.data?.data();
    if (!message) return;

    const senderName = await getUsername(message.senderId);

    // Attachment-only messages have an empty text field -- a blank
    // notification body would tell the recipient nothing useful.
    const body = message.text && message.text.trim().length > 0
      ? message.text
      : attachmentPreviewText(message.attachmentType);

    await sendPushToUser(
      message.receiverId,
      senderName,
      body,
      {
        type: "private_message",
        senderId: message.senderId,
        channelId: "messages",
      },
    );

  },
);

function attachmentPreviewText(attachmentType) {
  switch (attachmentType) {
    case "image": return "📷 Photo";
    case "gif": return "GIF";
    case "audio": return "🎤 Voice note";
    case "file": return "📎 File";
    default: return "New message";
  }
}

exports.onFriendRequestCreated = onDocumentCreated(
  "friendRequests/{requestId}",
  async (event) => {

    const request = event.data?.data();
    if (!request || request.status !== "pending") return;

    const senderName = await getUsername(request.senderId);

    await sendPushToUser(
      request.receiverId,
      "New Friend Request",
      `${senderName} wants to connect with you`,
      {
        type: "friend_request",
        senderId: request.senderId,
        channelId: "social",
      },
    );

  },
);

exports.onFriendRequestAccepted = onDocumentUpdated(
  "friendRequests/{requestId}",
  async (event) => {

    const before = event.data?.before?.data();
    const after = event.data?.after?.data();
    if (!before || !after) return;

    if (before.status === "pending" && after.status === "accepted") {

      const accepterName = await getUsername(after.receiverId);

      await sendPushToUser(
        after.senderId,
        "Friend Request Accepted",
        `${accepterName} accepted your friend request!`,
        {
          type: "friend_accepted",
          senderId: after.receiverId,
          channelId: "social",
        },
      );

    }

  },
);

/**
 * Smart moderation for community chat, using Google's Perspective API.
 *
 * Deliberately scoped to community/room messages only, not private DMs --
 * moderating a private conversation between two consenting adults is a
 * different (and much more privacy-sensitive) thing than moderating a
 * public community space, and this only ever touches the latter.
 *
 * Deliberately flags rather than deletes. Auto-deleting on an automated
 * toxicity score means a single false positive silently erases someone's
 * message with no recourse -- flagging instead means a human (the
 * message's own sender, or a moderator) still makes the actual call.
 * The Android client (MessageAdapter.kt) shows flagged messages as a
 * "tap to view" placeholder to everyone except the sender and
 * moderators, who see the real text with a warning badge so they can
 * act on it.
 */
exports.onRoomMessageCreated = onDocumentCreated(
  {
    document: "rooms/{roomId}/messages/{messageId}",
    secrets: [perspectiveApiKey],
  },
  async (event) => {

    const message = event.data?.data();
    if (!message || !message.text || message.text.trim().length === 0) {
      // Attachment-only messages have nothing to analyze.
      return;
    }

    const apiKey = perspectiveApiKey.value();

    if (!apiKey) {
      logger.warn("PERSPECTIVE_API_KEY not set -- skipping moderation check.");
      return;
    }

    try {

      const response = await fetch(
        `https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze?key=${apiKey}`,
        {
          method: "POST",
          headers: {"Content-Type": "application/json"},
          body: JSON.stringify({
            comment: {text: message.text},
            languages: ["en"],
            requestedAttributes: {TOXICITY: {}},
          }),
        },
      );

      if (!response.ok) {
        logger.error(`Perspective API returned ${response.status}`);
        return;
      }

      const result = await response.json();

      const score =
        result?.attributeScores?.TOXICITY?.summaryScore?.value ?? 0;

      // 0.75 is a deliberately conservative threshold -- Perspective
      // scores run 0-1, and erring toward fewer false positives matters
      // more here than catching every borderline case, since the cost
      // of a wrongly-flagged message (friction, a "tap to view" wall on
      // something innocent) is worse than the cost of an occasional
      // truly toxic message slipping through unflagged.
      if (score >= 0.75) {

        await event.data.ref.update({
          flagged: true,
          toxicityScore: score,
        });

        logger.info(
          `Flagged message ${event.params.messageId} in room ${event.params.roomId} (score: ${score.toFixed(2)})`,
        );

      }

    } catch (err) {

      logger.error("Perspective API request failed:", err.message);

    }

  },
);
