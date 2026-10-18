package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Assignment
import com.example.data.model.Course
import com.example.data.model.Habit
import com.example.util.DateUtils
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AssignmentRepository(
    private val db: FirebaseFirestore,
    private val appContext: Context? = null
) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        context.applicationContext
    )

    // In-memory offline local stores for pure offline mode / guest mode
    private val _offlineAssignments = MutableStateFlow<List<Assignment>>(emptyList())
    private val _offlineCourses = MutableStateFlow<List<Course>>(emptyList())
    private val _offlineHabits = MutableStateFlow<List<Habit>>(emptyList())
    private var offlineInitialized = false

    private fun isOfflineUser(userId: String): Boolean = userId.startsWith("offline_")

    private fun ensureOfflineDataInitialized(userId: String) {
        if (offlineInitialized) return
        offlineInitialized = true

        val starters = listOf(
            Course(id = "c1", userId = userId, name = "General Science / Maths", code = "SCI 101", colorHex = "#4F46E5"),
            Course(id = "c2", userId = userId, name = "English & Literature", code = "ENG 201", colorHex = "#0EA5E9"),
            Course(id = "c3", userId = userId, name = "WASSCE Past Questions", code = "PASCO", colorHex = "#F59E0B")
        )
        _offlineCourses.value = starters

        val nowSec = Timestamp.now().seconds
        val defaultAssignments = listOf(
            Assignment(
                id = "offline_task_1",
                userId = userId,
                title = "Solve 2023 WASSCE Core Maths Pasco (Q1-Q10)",
                courseName = "WASSCE Past Questions",
                courseColorHex = "#F59E0B",
                dueTimestamp = Timestamp(nowSec + 3600 * 4, 0),
                priority = "HIGH",
                status = "NOT_STARTED",
                notes = "Focus on circle theorem & quadratic equations.",
                estimatedMinutes = 60,
                subtasks = listOf(
                    mapOf("id" to "st1", "title" to "Open 2023 WASSCE Paper 2 booklet", "completed" to true),
                    mapOf("id" to "st2", "title" to "Solve algebra and geometry sections", "completed" to false),
                    mapOf("id" to "st3", "title" to "Cross-check answers with marking scheme", "completed" to false)
                )
            ),
            Assignment(
                id = "offline_task_2",
                userId = userId,
                title = "English Language Essay Draft",
                courseName = "English & Literature",
                courseColorHex = "#0EA5E9",
                dueTimestamp = Timestamp(nowSec + 86400 * 2, 0),
                priority = "MEDIUM",
                status = "NOT_STARTED",
                notes = "Write a 450-word formal article on youth digital literacy.",
                estimatedMinutes = 45,
                subtasks = listOf(
                    mapOf("id" to "st4", "title" to "Draft thesis & 4 paragraph topics", "completed" to true),
                    mapOf("id" to "st5", "title" to "Review vocabulary and grammar points", "completed" to false)
                )
            ),
            Assignment(
                id = "offline_task_3",
                userId = userId,
                title = "Integrated Science Notes Revision",
                courseName = "General Science / Maths",
                courseColorHex = "#4F46E5",
                dueTimestamp = Timestamp(nowSec + 86400 * 4, 0),
                priority = "LOW",
                status = "NOT_STARTED",
                notes = "Review cell division, ecology, and energy conservation units.",
                estimatedMinutes = 30
            )
        )
        _offlineAssignments.value = defaultAssignments

        val todayStr = DateUtils.getTodayIsoString()
        val defaultHabits = listOf(
            Habit(
                id = "offline_habit_1",
                userId = userId,
                title = "Solve 5 WASSCE Pasco Questions",
                category = "Exam Prep",
                colorHex = "#F59E0B",
                streakCount = 3L,
                bestStreak = 5L,
                completedDates = listOf(todayStr)
            ),
            Habit(
                id = "offline_habit_2",
                userId = userId,
                title = "Review Core Notes & Flashcards",
                category = "Study Habit",
                colorHex = "#4F46E5",
                streakCount = 2L,
                bestStreak = 4L,
                completedDates = emptyList()
            ),
            Habit(
                id = "offline_habit_3",
                userId = userId,
                title = "Complete 1 Focused Pomodoro",
                category = "Discipline",
                colorHex = "#10B981",
                streakCount = 1L,
                bestStreak = 3L,
                completedDates = emptyList()
            )
        )
        _offlineHabits.value = defaultHabits
    }

    fun observeAssignments(userId: String): Flow<List<Assignment>> {
        if (isOfflineUser(userId)) {
            ensureOfflineDataInitialized(userId)
            return _offlineAssignments.asStateFlow()
        }
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
        if (isOfflineUser(userId)) {
            ensureOfflineDataInitialized(userId)
            val item = _offlineAssignments.value.find { it.id == assignmentId }
            return Result.success(item)
        }
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
        if (isOfflineUser(userId)) {
            ensureOfflineDataInitialized(userId)
            val generatedId = "local_" + UUID.randomUUID().toString().take(8)
            val newAssignment = assignment.copy(
                id = generatedId,
                userId = userId,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            _offlineAssignments.value = listOf(newAssignment) + _offlineAssignments.value
            return Result.success(generatedId)
        }

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

        if (isOfflineUser(userId)) {
            ensureOfflineDataInitialized(userId)
            val updated = assignment.copy(updatedAt = Timestamp.now())
            _offlineAssignments.value = _offlineAssignments.value.map {
                if (it.id == assignment.id) updated else it
            }
            return Result.success(Unit)
        }

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
        if (isOfflineUser(userId)) {
            ensureOfflineDataInitialized(userId)
            _offlineAssignments.value = _offlineAssignments.value.filter { it.id != assignmentId }
            return Result.success(Unit)
        }

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
        if (isOfflineUser(userId)) {
            ensureOfflineDataInitialized(userId)
            return _offlineCourses.asStateFlow()
        }

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
        if (isOfflineUser(userId)) {
            ensureOfflineDataInitialized(userId)
            val generatedId = "local_c_" + UUID.randomUUID().toString().take(6)
            val newC = course.copy(
                id = generatedId,
                userId = userId,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            _offlineCourses.value = _offlineCourses.value + newC
            return Result.success(generatedId)
        }

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
        if (isOfflineUser(userId)) {
            ensureOfflineDataInitialized(userId)
            _offlineCourses.value = _offlineCourses.value.filter { it.id != courseId }
            return Result.success(Unit)
        }

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

    fun observeHabits(userId: String): Flow<List<Habit>> {
        if (isOfflineUser(userId)) {
            ensureOfflineDataInitialized(userId)
            return _offlineHabits.asStateFlow()
        }

        val path = "users/$userId/habits"
        return db.collection("users").document(userId).collection("habits")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Habit::class.java,
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

    suspend fun createHabit(userId: String, habit: Habit): Result<String> {
        if (isOfflineUser(userId)) {
            ensureOfflineDataInitialized(userId)
            val generatedId = "local_h_" + UUID.randomUUID().toString().take(6)
            val newH = habit.copy(
                id = generatedId,
                userId = userId,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            _offlineHabits.value = _offlineHabits.value + newH
            return Result.success(generatedId)
        }

        val collectionRef = db.collection("users").document(userId).collection("habits")
        val docRef = collectionRef.document()
        val payload = mapOf(
            "userId" to userId,
            "title" to habit.title.trim(),
            "category" to habit.category.trim(),
            "colorHex" to habit.colorHex,
            "streakCount" to habit.streakCount,
            "bestStreak" to habit.bestStreak,
            "completedDates" to habit.completedDates,
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

    suspend fun toggleHabitForDate(userId: String, habit: Habit, dateStr: String): Result<Unit> {
        require(habit.id.isNotBlank()) { "Habit ID must not be blank" }

        val isAlreadyDone = habit.completedDates.contains(dateStr)
        val newDates = if (isAlreadyDone) {
            habit.completedDates.filter { it != dateStr }
        } else {
            (habit.completedDates + dateStr).distinct()
        }

        val newStreak = DateUtils.calculateStreak(newDates)
        val newBest = maxOf(habit.bestStreak, newStreak)

        if (isOfflineUser(userId)) {
            ensureOfflineDataInitialized(userId)
            val updated = habit.copy(
                completedDates = newDates,
                streakCount = newStreak,
                bestStreak = newBest,
                updatedAt = Timestamp.now()
            )
            _offlineHabits.value = _offlineHabits.value.map {
                if (it.id == habit.id) updated else it
            }
            return Result.success(Unit)
        }

        val docRef = db.collection("users").document(userId).collection("habits").document(habit.id)
        val payload = mapOf(
            "completedDates" to newDates,
            "streakCount" to newStreak,
            "bestStreak" to newBest,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteHabit(userId: String, habitId: String): Result<Unit> {
        if (isOfflineUser(userId)) {
            ensureOfflineDataInitialized(userId)
            _offlineHabits.value = _offlineHabits.value.filter { it.id != habitId }
            return Result.success(Unit)
        }

        val docRef = db.collection("users").document(userId).collection("habits").document(habitId)
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
