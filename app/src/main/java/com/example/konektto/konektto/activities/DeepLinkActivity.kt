package com.example.konektto.konektto.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.konektto.konektto.utils.RoomJoiner
import com.google.firebase.auth.FirebaseAuth

/**
 * Handles konektto://join/{code} links, registered in the manifest with
 * an intent-filter for that custom scheme.
 *
 * Known, deliberate scope limit: if the person isn't logged in yet, this
 * does NOT carry the invite code through the login flow and auto-join
 * afterward -- it just tells them the code and sends them to log in
 * manually. Building real continuity through login/registration would
 * mean both of those screens accepting and forwarding a pending-invite
 * extra, which felt like meaningfully more surface area for a case
 * (someone tapping an invite link before they've ever logged in) that's
 * the less common path. The primary, always-reliable path is the manual
 * Join by Code screen, which this always mentions as a fallback anyway.
 */
class DeepLinkActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val code = intent.data?.lastPathSegment

        if (code.isNullOrBlank()) {
            finish()
            return
        }

        if (FirebaseAuth.getInstance().currentUser == null) {

            Toast.makeText(
                this,
                "Log in, then use \"Join by Code\" with code: $code",
                Toast.LENGTH_LONG
            ).show()

            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return

        }

        RoomJoiner.joinByCode(code) { result ->

            when (result) {

                is RoomJoiner.Result.Success -> {

                    val message = if (result.alreadyMember)
                        "You're already in ${result.room.roomName}!"
                    else
                        "Joined ${result.room.roomName}!"

                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, RoomDashboardActivity::class.java)
                    intent.putExtra("roomId", result.room.roomId)
                    intent.putExtra("roomName", result.room.roomName)
                    intent.putExtra("roomDescription", result.room.roomDescription)
                    startActivity(intent)

                }

                is RoomJoiner.Result.NotFound ->
                    Toast.makeText(this, "That invite link isn't valid anymore.", Toast.LENGTH_LONG).show()

                is RoomJoiner.Result.NotLoggedIn ->
                    Toast.makeText(this, "Please log in first.", Toast.LENGTH_LONG).show()

                is RoomJoiner.Result.Error ->
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()

            }

            finish()

        }

    }

}
