# Konektto Cloud Functions -- Push Notifications

This is the server-side half of push notifications. The Android app
(`KonecttoMessagingService`, `NotificationHelper`) can only *receive* a push
and display it -- something with backend credentials has to *send* it.
That's what lives here.

## What it does

Three Firestore-triggered functions, all in `index.js`:

- **onPrivateMessageCreated** -- fires when a new document is created under
  `privateChats/{chatId}/messages/{messageId}`. Sends a push to the
  message's receiver.
- **onFriendRequestCreated** -- fires when a new `friendRequests/{requestId}`
  doc is created with `status: "pending"`. Notifies the receiver.
- **onFriendRequestAccepted** -- fires when a `friendRequests/{requestId}`
  doc transitions from `status: "pending"` to `status: "accepted"`.
  Notifies whoever originally sent the request.

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

# 4. Install function dependencies
cd functions
npm install

# 5. Deploy
cd ..
firebase deploy --only functions
```

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
