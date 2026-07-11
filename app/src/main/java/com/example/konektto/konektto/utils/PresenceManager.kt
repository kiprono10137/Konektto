package com.example.konektto.konektto.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Tracks whether the signed-in user's app is in the foreground or background,
 * and mirrors that into Firestore so other users can see online/offline status.
 *
 * Uses ProcessLifecycleOwner (registered once, in KonecttoApplication) rather
 * than any single Activity's lifecycle, so status reflects "is the app open
 * at all" -- not "is this particular screen open". Navigating between
 * activities inside the app must never flicker the user's status to offline.
 *
 * lastSeen is written with Firestore's server timestamp rather than the
 * device clock. Two phones never agree on what time it is; the server does.
 */
object PresenceManager : DefaultLifecycleObserver {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onStart(owner: LifecycleOwner) {
        setOnline(true)
    }

    override fun onStop(owner: LifecycleOwner) {
        setOnline(false)
    }

    private fun setOnline(isOnline: Boolean) {

        val uid = auth.currentUser?.uid ?: return

        val update: Map<String, Any> = if (isOnline) {

            mapOf("isOnline" to true)

        } else {

            mapOf(
                "isOnline" to false,
                "lastSeen" to FieldValue.serverTimestamp()
            )

        }

        db.collection("users")
            .document(uid)
            .set(update, com.google.firebase.firestore.SetOptions.merge())
    }

    /**
     * Called explicitly right before signing out, so status flips to offline
     * immediately rather than waiting for the process lifecycle to catch up
     * (which could lag behind a fast logout -> re-login on a different account).
     */
    fun setOfflineImmediately() {
        setOnline(false)
    }

}
