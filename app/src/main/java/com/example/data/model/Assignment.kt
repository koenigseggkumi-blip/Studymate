package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.UUID

@IgnoreExtraProperties
data class Assignment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val courseName: String = "",
    val courseColorHex: String = "#4F46E5",
    val dueTimestamp: Timestamp? = null,
    val priority: String = "MEDIUM", // "HIGH", "MEDIUM", "LOW"
    val status: String = "NOT_STARTED", // "NOT_STARTED", "IN_PROGRESS", "COMPLETED"
    val notes: String? = null,
    val estimatedMinutes: Long? = null,
    val subtasks: List<Map<String, Any>> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class SubtaskItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val completed: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "completed" to completed
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): SubtaskItem {
            return SubtaskItem(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                title = map["title"] as? String ?: "",
                completed = map["completed"] as? Boolean ?: false
            )
        }
    }
}
