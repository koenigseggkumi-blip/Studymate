package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Assignment
import com.example.data.model.Course
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AssignmentRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeAssignments(userId: String): Flow<List<Assignment>> {
        val path = "users/$userId/assignments"
        return db.collection("users").document(userId).collection("assignments")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Assignment::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun getAssignment(userId: String, assignmentId: String): Result<Assignment?> {
        val docRef = db.collection("users").document(userId).collection("assignments").document(assignmentId)
        return try {
            val snapshot = docRef.get().await()
            val assignment = snapshot.toObject(
                Assignment::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(assignment)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createAssignment(userId: String, assignment: Assignment): Result<String> {
        val collectionRef = db.collection("users").document(userId).collection("assignments")
        val docRef = collectionRef.document()
        val payload = mutableMapOf<String, Any>(
            "userId" to userId,
            "title" to assignment.title.trim(),
            "courseName" to assignment.courseName.trim(),
            "courseColorHex" to assignment.courseColorHex,
            "dueTimestamp" to (assignment.dueTimestamp ?: Timestamp.now()),
            "priority" to assignment.priority,
            "status" to assignment.status,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!assignment.notes.isNullOrBlank()) {
            payload["notes"] = assignment.notes.trim()
        }
        if (assignment.estimatedMinutes != null && assignment.estimatedMinutes >= 0) {
            payload["estimatedMinutes"] = assignment.estimatedMinutes
        }
        if (assignment.subtasks.isNotEmpty()) {
            payload["subtasks"] = assignment.subtasks
        }

        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateAssignment(userId: String, assignment: Assignment): Result<Unit> {
        require(assignment.id.isNotBlank()) { "Assignment ID must not be blank" }
        val docRef = db.collection("users").document(userId).collection("assignments").document(assignment.id)
        val updatePayload = mutableMapOf<String, Any>(
            "title" to assignment.title.trim(),
            "courseName" to assignment.courseName.trim(),
            "courseColorHex" to assignment.courseColorHex,
            "dueTimestamp" to (assignment.dueTimestamp ?: Timestamp.now()),
            "priority" to assignment.priority,
            "status" to assignment.status,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (assignment.notes != null) {
            updatePayload["notes"] = assignment.notes.trim()
        }
        if (assignment.estimatedMinutes != null) {
            updatePayload["estimatedMinutes"] = assignment.estimatedMinutes
        }
        updatePayload["subtasks"] = assignment.subtasks

        return try {
            docRef.update(updatePayload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteAssignment(userId: String, assignmentId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("assignments").document(assignmentId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeCourses(userId: String): Flow<List<Course>> {
        val path = "users/$userId/courses"
        return db.collection("users").document(userId).collection("courses")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Course::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun createCourse(userId: String, course: Course): Result<String> {
        val collectionRef = db.collection("users").document(userId).collection("courses")
        val docRef = collectionRef.document()
        val payload = mapOf(
            "userId" to userId,
            "name" to course.name.trim(),
            "code" to course.code.trim(),
            "colorHex" to course.colorHex,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCourse(userId: String, courseId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("courses").document(courseId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
