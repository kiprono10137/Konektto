package com.example.konektto.konektto.utils

import com.example.konektto.konektto.models.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * One place for "find the room with this invite code, and join it if I'm
 * not already a member" -- both the manual Join by Code screen and the
 * konektto://join/{code} deep link need exactly this, and neither should
 * have its own copy of the Firestore query + join-or-already-a-member
 * branching logic.
 */
object RoomJoiner {

    sealed class Result {
        data class Success(val room: Room, val alreadyMember: Boolean) : Result()
        data class NotFound(val code: String) : Result()
        data class Error(val message: String) : Result()
        object NotLoggedIn : Result()
    }

    fun joinByCode(code: String, onResult: (Result) -> Unit) {

        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            onResult(Result.NotLoggedIn)
            return
        }

        val normalizedCode = code.trim().uppercase()
        val db = FirebaseFirestore.getInstance()

        db.collection("rooms")
            .whereEqualTo("inviteCode", normalizedCode)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->

                val roomDoc = documents.documents.firstOrNull()

                if (roomDoc == null) {
                    onResult(Result.NotFound(normalizedCode))
                    return@addOnSuccessListener
                }

                val room = roomDoc.toObject(Room::class.java)

                if (room == null) {
                    onResult(Result.Error("Couldn't read that community's details."))
                    return@addOnSuccessListener
                }

                val roomRef = db.collection("rooms").document(room.roomId)

                roomRef.collection("members")
                    .document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { memberDoc ->

                        if (memberDoc.exists()) {

                            onResult(Result.Success(room, alreadyMember = true))

                        } else {

                            db.collection("users")
                                .document(currentUser.uid)
                                .get()
                                .addOnSuccessListener { userDoc ->

                                    val username = userDoc.getString("username") ?: "Unknown User"

                                    val memberData = hashMapOf(
                                        "userId" to currentUser.uid,
                                        "username" to username,
                                        "joinedAt" to System.currentTimeMillis(),
                                        "role" to "member"
                                    )

                                    roomRef.collection("members")
                                        .document(currentUser.uid)
                                        .set(memberData)
                                        .addOnSuccessListener {

                                            roomRef.update(
                                                "memberCount",
                                                room.memberCount + 1
                                            )

                                            onResult(Result.Success(room, alreadyMember = false))

                                        }
                                        .addOnFailureListener { e ->
                                            onResult(Result.Error(e.localizedMessage ?: "Failed to join."))
                                        }

                                }
                                .addOnFailureListener { e ->
                                    onResult(Result.Error(e.localizedMessage ?: "Failed to join."))
                                }

                        }

                    }
                    .addOnFailureListener { e ->
                        onResult(Result.Error(e.localizedMessage ?: "Failed to join."))
                    }

            }
            .addOnFailureListener { e ->
                onResult(Result.Error(e.localizedMessage ?: "Something went wrong."))
            }

    }

}
