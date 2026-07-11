package com.example.konektto.konektto.utils

import com.example.konektto.konektto.models.RecommendedRoom
import com.example.konektto.konektto.models.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * "Recommendations" here don't need an AI model -- the signal that
 * actually predicts whether someone will enjoy a community is mundane:
 * do they already like this category, and are people they know already
 * there. Both are plain Firestore queries. A real AI call would be
 * solving a problem this app doesn't have yet.
 */
object RoomRecommendationEngine {

    data class RecommendationResult(
        val recommendations: List<RecommendedRoom>,
        val isFallbackPopular: Boolean
    )

    fun recommend(maxResults: Int = 8, onResult: (RecommendationResult) -> Unit) {

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            onResult(RecommendationResult(emptyList(), false))
            return
        }

        val db = FirebaseFirestore.getInstance()

        db.collection("rooms").get().addOnSuccessListener { roomDocs ->

            val allRooms = roomDocs.mapNotNull { it.toObject(Room::class.java) }

            db.collectionGroup("members")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener { myMemberDocs ->

                    val joinedRoomIds = myMemberDocs.documents
                        .mapNotNull { it.reference.parent.parent?.id }
                        .toSet()

                    val joinedCategories = allRooms
                        .filter { it.roomId in joinedRoomIds }
                        .map { it.category }
                        .filter { it.isNotBlank() }
                        .toSet()

                    fetchFriendIds(uid) { friendIds ->

                        fetchFriendRoomCounts(friendIds) { friendRoomCounts ->

                            onResult(
                                buildRecommendations(
                                    allRooms = allRooms,
                                    joinedRoomIds = joinedRoomIds,
                                    joinedCategories = joinedCategories,
                                    friendRoomCounts = friendRoomCounts,
                                    maxResults = maxResults
                                )
                            )

                        }

                    }

                }
                .addOnFailureListener {
                    onResult(RecommendationResult(emptyList(), false))
                }

        }.addOnFailureListener {
            onResult(RecommendationResult(emptyList(), false))
        }

    }

    private fun buildRecommendations(
        allRooms: List<Room>,
        joinedRoomIds: Set<String>,
        joinedCategories: Set<String>,
        friendRoomCounts: Map<String, Int>,
        maxResults: Int
    ): RecommendationResult {

        val candidates = allRooms.filter { it.roomId !in joinedRoomIds }

        data class Scored(val room: Room, val score: Double, val reason: String)

        val scored = candidates.map { room ->

            val friendCount = friendRoomCounts[room.roomId] ?: 0
            val categoryMatch = room.category.isNotBlank() && joinedCategories.contains(room.category)

            var score = 0.0
            var reason = ""

            if (friendCount > 0) {

                score += friendCount * 5.0

                reason = if (friendCount == 1) "👥 A friend is here"
                else "👥 $friendCount friends are here"

            }

            if (categoryMatch) {

                score += 3.0

                if (reason.isEmpty()) {
                    reason = "🏷️ Because you like ${room.category}"
                }

            }

            // A small popularity nudge, capped so a handful of giant rooms
            // can't drown out genuine category/friend signal -- popularity
            // is a tiebreaker here, not the main signal.
            score += minOf(room.memberCount, 20) * 0.05

            if (reason.isEmpty()) {
                reason = "📈 Popular right now"
            }

            Scored(room, score, reason)

        }

        // "Real signal" means something beyond the popularity nudge alone --
        // a brand new user with no joined rooms and no friends yet would
        // otherwise see a 'recommended' section that's really just
        // 'everything, sorted by size', mislabeled.
        val hasRealSignal = scored.any { it.score > (20 * 0.05) }

        val top = if (hasRealSignal) {

            scored.sortedByDescending { it.score }
                .filter { it.score > 0 }
                .take(maxResults)

        } else {

            allRooms.sortedByDescending { it.memberCount }
                .take(maxResults)
                .map { Scored(it, 0.0, "📈 Popular right now") }

        }

        return RecommendationResult(
            recommendations = top.map { RecommendedRoom(it.room, it.reason) },
            isFallbackPopular = !hasRealSignal
        )

    }

    private fun fetchFriendIds(uid: String, onResult: (Set<String>) -> Unit) {

        val db = FirebaseFirestore.getInstance()
        val friendIds = mutableSetOf<String>()

        db.collection("friendRequests")
            .whereEqualTo("senderId", uid)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { sentDocs ->

                sentDocs.forEach { doc ->
                    doc.getString("receiverId")?.let { friendIds.add(it) }
                }

                db.collection("friendRequests")
                    .whereEqualTo("receiverId", uid)
                    .whereEqualTo("status", "accepted")
                    .get()
                    .addOnSuccessListener { receivedDocs ->

                        receivedDocs.forEach { doc ->
                            doc.getString("senderId")?.let { friendIds.add(it) }
                        }

                        onResult(friendIds)

                    }
                    .addOnFailureListener { onResult(friendIds) }

            }
            .addOnFailureListener { onResult(friendIds) }

    }

    /**
     * roomId -> how many of my friends are in that room. Bounded by
     * friend count, not by total rooms in the app -- someone with 20
     * friends runs 20 small queries, not one query per candidate room.
     */
    private fun fetchFriendRoomCounts(
        friendIds: Set<String>,
        onResult: (Map<String, Int>) -> Unit
    ) {

        if (friendIds.isEmpty()) {
            onResult(emptyMap())
            return
        }

        val db = FirebaseFirestore.getInstance()
        val counts = mutableMapOf<String, Int>()
        var remaining = friendIds.size

        for (friendId in friendIds) {

            db.collectionGroup("members")
                .whereEqualTo("userId", friendId)
                .get()
                .addOnSuccessListener { docs ->

                    docs.forEach { doc ->

                        val roomId = doc.reference.parent.parent?.id

                        if (roomId != null) {
                            counts[roomId] = (counts[roomId] ?: 0) + 1
                        }

                    }

                    remaining--
                    if (remaining == 0) onResult(counts)

                }
                .addOnFailureListener {

                    remaining--
                    if (remaining == 0) onResult(counts)

                }

        }

    }

}
