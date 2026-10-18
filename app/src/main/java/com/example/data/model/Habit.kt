package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Habit(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "Exam Prep",
    val colorHex: String = "#F59E0B",
    val streakCount: Long = 0L,
    val bestStreak: Long = 0L,
    val completedDates: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
