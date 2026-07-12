package com.example.konektto.konektto.utils

import android.graphics.Color

/**
 * One place for "what does this category look like" -- used by every
 * community card in the app (RoomAdapter, RecommendedRoomAdapter), so a
 * Football community always looks like the same Football regardless of
 * which list it's showing up in.
 */
object CategoryStyle {

    data class Style(val emoji: String, val displayName: String, val color: Int)

    /**
     * Colors are fixed regardless of light/dark theme -- a category's
     * identity shouldn't flip when the theme does, same reasoning as the
     * online-status green and the brand gradient elsewhere in this app.
     * Chosen dark/saturated enough that white text stays readable on
     * every one of them.
     */
    fun of(category: String): Style {

        return when (category) {

            "Football" -> Style("⚽", "Football", Color.parseColor("#16A34A"))
            "Gaming" -> Style("🎮", "Gaming", Color.parseColor("#4F46E5"))
            "Music" -> Style("🎵", "Music", Color.parseColor("#DB2777"))
            "Programming" -> Style("💻", "Programming", Color.parseColor("#0EA5E9"))
            "Technology" -> Style("📱", "Technology", Color.parseColor("#6366F1"))
            "Business" -> Style("💼", "Business", Color.parseColor("#B45309"))
            "Fashion" -> Style("👕", "Fashion", Color.parseColor("#EC4899"))
            "Movies" -> Style("🎬", "Movies", Color.parseColor("#DC2626"))
            "Relationships" -> Style("❤️", "Relationships", Color.parseColor("#E11D48"))
            "Education" -> Style("📚", "Education", Color.parseColor("#0891B2"))

            else -> Style("💬", "General", Color.parseColor("#6B7280"))

        }

    }

}
