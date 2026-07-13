# Konektto Cloud Functions -- Push Notifications

This is the server-side half of push notifications. The Android app
(`KonecttoMessagingService`, `NotificationHelper`) can only *receive* a push
and display it -- something with backend credentials has to *send* it.
That's what lives here.

## What it does

Four Firestore-triggered functions, all in `index.js`:

- **onPrivateMessageCreated** -- fires when a new document is created under
  `privateChats/{chatId}/messages/{messageId}`. Sends a push to the
  message's receiver.
- **onFriendRequestCreated** -- fires when a new `friendRequests/{requestId}`
  doc is created with `status: "pending"`. Notifies the receiver.
- **onFriendRequestAccepted** -- fires when a `friendRequests/{requestId}`
  doc transitions from `status: "pending"` to `status: "accepted"`.
  Notifies whoever originally sent the request.
- **onRoomMessageCreated** -- smart moderation for community chat. Fires
  on every new `rooms/{roomId}/messages/{messageId}` doc, sends the
  message text to Google's Perspective API for a toxicity score, and
  flags the message (`flagged: true`, `toxicityScore: <0-1>`) if it
  scores 0.75 or higher. Deliberately does NOT touch private messages --
  moderating a private conversation between two consenting adults is a
  different, more privacy-sensitive thing than moderating a public
  community space. Deliberately flags rather than auto-deletes -- the
  Android client shows a flagged message as a "tap to view" placeholder
  to everyone except its sender and room moderators, who see the real
  text with a warning badge, so a human still makes the actual call
  rather than a single automated score silently erasing someone's message.

All three look up the target user's `fcmToken` field on their
`users/{uid}` document (written automatically by the Android app --
see `NotificationHelper.refreshFcmToken()`) and send a **data-only**
FCM message (no top-level `notification` payload). That's deliberate:
it keeps the Android client in control of building the actual
notification and its deep-link, in every app state, rather than letting
the OS auto-display a generic notification when the app isn't in the
foreground.

## One-time setup

This requires the **Blaze (pay-as-you-go) plan** on your Firebase
project -- Firestore-triggered Cloud Functions aren't available on the
free Spark plan. For an app at this scale, real-world cost is close to
$0/month; Firebase's free invocation tier comfortably covers casual use.
You can check/upgrade your plan at
https://console.firebase.google.com/project/_/usage/details

```bash
# 1. Install the Firebase CLI if you don't have it
npm install -g firebase-tools

# 2. Log in with the Google account that owns the Konektto Firebase project
firebase login

# 3. From the repo root, link this folder to your actual Firebase project
#    (this creates .firebaserc locally -- it's gitignored on purpose,
#    since it's specific to whoever is deploying)
firebase use --add

# 4. Set the Perspective API key as a secret (needed for smart
#    moderation -- see "Getting a Perspective API key" below if you
#    don't have one yet). This prompts for the key value and stores it
#    securely in Google Cloud Secret Manager -- it is never written to
#    any file in this repo.
firebase functions:secrets:set PERSPECTIVE_API_KEY

# 5. Install function dependencies
cd functions
npm install

# 6. Deploy
cd ..
firebase deploy --only functions
```

## Getting a Perspective API key

1. console.cloud.google.com -> select your Konektto project (same
   underlying Google Cloud project as Firebase)
2. Search for "Perspective Comment Analyzer API" -> Enable
3. APIs & Services -> Credentials -> + Create Credentials -> API key
4. Click "Restrict Key" on the new key -- under API restrictions,
   select "Restrict key" and check only "Perspective Comment Analyzer
   API". An unrestricted key that leaks is a much bigger problem than a
   restricted one.
5. Use that key value in step 4 above (`firebase functions:secrets:set`)

If you ever need to rotate the key, `firebase functions:secrets:set
PERSPECTIVE_API_KEY` again with the new value, then redeploy.

## Verifying it worked

After deploying, send yourself a private message from a second test
account (or a second device/emulator). You should get a system
notification within a few seconds. If not, check:

```bash
firebase functions:log
```

Common first-run issues:
- **"fcmToken for user X, skipping push"** in the logs -- the recipient's
  Android app hasn't registered a token yet. This happens automatically
  on app launch (`MainActivity.onCreate()` calls
  `NotificationHelper.refreshFcmToken()`), so just open the app once on
  that account.
- **Permission denied on Android 13+** -- the app requests
  `POST_NOTIFICATIONS` at runtime the first time `MainActivity` opens.
  If it was denied, there's currently no in-app way to re-prompt; the
  user would need to grant it from system Settings > Apps > Konektto >
  Notifications.
