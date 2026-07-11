package com.example.konektto.konektto.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.konektto.R
import com.example.konektto.konektto.utils.RoomJoiner

class JoinByCodeActivity : AppCompatActivity() {

    private lateinit var etInviteCode: EditText
    private lateinit var btnJoinByCode: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_by_code)

        supportActionBar?.hide()

        etInviteCode = findViewById(R.id.etInviteCode)
        btnJoinByCode = findViewById(R.id.btnJoinByCode)

        // Support being launched with a code already known (e.g. from a
        // deep link that couldn't complete the join itself for some reason).
        intent.getStringExtra("prefillCode")?.let {
            etInviteCode.setText(it)
        }

        btnJoinByCode.setOnClickListener {
            attemptJoin()
        }

    }

    private fun attemptJoin() {

        val code = etInviteCode.text.toString().trim()

        if (code.isEmpty()) {
            etInviteCode.error = "Enter an invite code"
            etInviteCode.requestFocus()
            return
        }

        btnJoinByCode.isEnabled = false

        RoomJoiner.joinByCode(code) { result ->

            btnJoinByCode.isEnabled = true

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
                    finish()

                }

                is RoomJoiner.Result.NotFound ->
                    Toast.makeText(this, "No community found with that code.", Toast.LENGTH_LONG).show()

                is RoomJoiner.Result.NotLoggedIn ->
                    Toast.makeText(this, "Please log in first.", Toast.LENGTH_LONG).show()

                is RoomJoiner.Result.Error ->
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()

            }

        }

    }

}
