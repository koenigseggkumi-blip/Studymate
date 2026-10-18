package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Assignment
import com.example.data.model.Course
import com.example.data.model.Habit
import com.example.data.model.SubtaskItem
import com.example.data.repository.AssignmentRepository
import com.example.util.DateUtils
import com.google.firebase.Timestamp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class FilterType(val label: String) {
    ALL("All"),
    DUE_SOON("⚡ Due Soon"),
    OVERDUE("🚨 Overdue"),
    TODAY("Today"),
    COMPLETED("✅ Completed")
}

enum class SortBy(val label: String) {
    DUE_DATE("Urgency / Due Date"),
    PRIORITY("Priority"),
    COURSE("Course")
}

enum class PomodoroMode(val title: String, val durationMinutes: Int) {
    FOCUS("Focus Session", 25),
    SHORT_BREAK("Short Break", 5),
    LONG_BREAK("Long Break", 15)
}

class AssignmentViewModel(
    private val repository: AssignmentRepository,
    private val currentUserId: String
) : ViewModel() {

    val assignmentsState: StateFlow<UiState<List<Assignment>>> = repository.observeAssignments(currentUserId)
        .map<List<Assignment>, UiState<List<Assignment>>> { UiState.Success(it) }
        .catch { error ->
            emit(UiState.Error(error.message ?: "Failed to load assignments"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val coursesState: StateFlow<List<Course>> = repository.observeCourses(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val habitsState: StateFlow<List<Habit>> = repository.observeHabits(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _selectedFilter = MutableStateFlow(FilterType.ALL)
    val selectedFilter: StateFlow<FilterType> = _selectedFilter.asStateFlow()

    private val _selectedCourseName = MutableStateFlow<String?>(null)
    val selectedCourseName: StateFlow<String?> = _selectedCourseName.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortBy = MutableStateFlow(SortBy.DUE_DATE)
    val sortBy: StateFlow<SortBy> = _sortBy.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Celebration event for live completion animation
    private val _celebrationEvent = MutableStateFlow<String?>(null)
    val celebrationEvent: StateFlow<String?> = _celebrationEvent.asStateFlow()

    fun dismissCelebration() {
        _celebrationEvent.value = null
    }

    // Pomodoro Timer State
    private val _pomodoroMode = MutableStateFlow(PomodoroMode.FOCUS)
    val pomodoroMode: StateFlow<PomodoroMode> = _pomodoroMode.asStateFlow()

    private val _timeLeftSeconds = MutableStateFlow(25 * 60)
    val timeLeftSeconds: StateFlow<Int> = _timeLeftSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _completedSessionsCount = MutableStateFlow(0)
    val completedSessionsCount: StateFlow<Int> = _completedSessionsCount.asStateFlow()

    private val _linkedAssignment = MutableStateFlow<Assignment?>(null)
    val linkedAssignment: StateFlow<Assignment?> = _linkedAssignment.asStateFlow()

    private var timerJob: Job? = null

    fun selectLinkedAssignment(assignment: Assignment?) {
        _linkedAssignment.value = assignment
    }

    fun setPomodoroMode(mode: PomodoroMode) {
        pauseTimer()
        _pomodoroMode.value = mode
        _timeLeftSeconds.value = mode.durationMinutes * 60
    }

    fun startTimer() {
        if (_isTimerRunning.value) return
        _isTimerRunning.value = true
        timerJob = viewModelScope.launch {
            while (isActive && _isTimerRunning.value && _timeLeftSeconds.value > 0) {
                delay(1000L)
                _timeLeftSeconds.value -= 1
                if (_timeLeftSeconds.value <= 0) {
                    _isTimerRunning.value = false
                    if (_pomodoroMode.value == PomodoroMode.FOCUS) {
                        _completedSessionsCount.value += 1
                        _celebrationEvent.value = "🎯 Pomodoro completed! Take a well-earned break."
                    } else {
                        _userMessage.value = "Break finished! Ready to focus again?"
                    }
                    break
                }
            }
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        timerJob = null
    }

    fun resetTimer() {
        pauseTimer()
        _timeLeftSeconds.value = _pomodoroMode.value.durationMinutes * 60
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun setFilter(filter: FilterType) {
        _selectedFilter.value = filter
    }

    fun setSelectedCourse(courseName: String?) {
        _selectedCourseName.value = courseName
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortBy(sort: SortBy) {
        _sortBy.value = sort
    }

    fun createAssignment(
        title: String,
        courseName: String,
        courseColorHex: String,
        dueTimestamp: Timestamp,
        priority: String,
        notes: String?,
        estimatedMinutes: Long?,
        subtasks: List<SubtaskItem>
    ) {
        if (title.isBlank()) {
            _userMessage.value = "Assignment title cannot be empty"
            return
        }
        viewModelScope.launch {
            val assignment = Assignment(
                userId = currentUserId,
                title = title.trim(),
                courseName = courseName.ifBlank { "General" },
                courseColorHex = courseColorHex,
                dueTimestamp = dueTimestamp,
                priority = priority,
                status = "NOT_STARTED",
                notes = notes?.takeIf { it.isNotBlank() },
                estimatedMinutes = estimatedMinutes,
                subtasks = subtasks.map { it.toMap() }
            )
            val result = repository.createAssignment(currentUserId, assignment)
            if (result.isSuccess) {
                _userMessage.value = "Assignment added: ${assignment.title}"
            } else {
                _userMessage.value = result.exceptionOrNull()?.message ?: "Failed to add assignment"
            }
        }
    }

    fun updateAssignment(assignment: Assignment) {
        viewModelScope.launch {
            val result = repository.updateAssignment(currentUserId, assignment)
            if (result.isSuccess) {
                _userMessage.value = "Updated: ${assignment.title}"
            } else {
                _userMessage.value = result.exceptionOrNull()?.message ?: "Failed to update"
            }
        }
    }

    fun toggleAssignmentCompleted(assignment: Assignment) {
        val willComplete = assignment.status != "COMPLETED"
        val newStatus = if (willComplete) "COMPLETED" else "IN_PROGRESS"
        val updated = assignment.copy(status = newStatus)
        viewModelScope.launch {
            val result = repository.updateAssignment(currentUserId, updated)
            if (result.isSuccess) {
                if (willComplete) {
                    // Trigger live celebration animation
                    _celebrationEvent.value = "🎉 '${assignment.title}' Completed!"
                }
            } else {
                _userMessage.value = result.exceptionOrNull()?.message ?: "Failed to update status"
            }
        }
    }

    fun toggleSubtask(assignment: Assignment, subtaskId: String) {
        val updatedSubtasks = assignment.subtasks.map { rawMap ->
            val item = SubtaskItem.fromMap(rawMap)
            if (item.id == subtaskId) {
                item.copy(completed = !item.completed).toMap()
            } else {
                rawMap
            }
        }
        val allDone = updatedSubtasks.isNotEmpty() && updatedSubtasks.all { it["completed"] == true }
        val newStatus = if (allDone) "COMPLETED" else if (assignment.status == "COMPLETED") "IN_PROGRESS" else assignment.status
        val updated = assignment.copy(subtasks = updatedSubtasks, status = newStatus)
        viewModelScope.launch {
            repository.updateAssignment(currentUserId, updated)
            if (allDone && assignment.status != "COMPLETED") {
                _celebrationEvent.value = "🎉 All steps done for '${assignment.title}'!"
            }
        }
    }

    fun deleteAssignment(assignmentId: String) {
        viewModelScope.launch {
            val result = repository.deleteAssignment(currentUserId, assignmentId)
            if (result.isSuccess) {
                _userMessage.value = "Assignment deleted"
            } else {
                _userMessage.value = result.exceptionOrNull()?.message ?: "Failed to delete"
            }
        }
    }

    fun createCourse(name: String, code: String, colorHex: String) {
        if (name.isBlank() || code.isBlank()) {
            _userMessage.value = "Course name and code are required"
            return
        }
        viewModelScope.launch {
            val course = Course(
                userId = currentUserId,
                name = name.trim(),
                code = code.trim().uppercase(),
                colorHex = colorHex
            )
            val result = repository.createCourse(currentUserId, course)
            if (result.isSuccess) {
                _userMessage.value = "Course created: ${course.name}"
            } else {
                _userMessage.value = result.exceptionOrNull()?.message ?: "Failed to create course"
            }
        }
    }

    fun deleteCourse(courseId: String) {
        viewModelScope.launch {
            val result = repository.deleteCourse(currentUserId, courseId)
            if (result.isSuccess) {
                _userMessage.value = "Course deleted"
            }
        }
    }

    fun seedStarterCoursesIfEmpty(currentCourses: List<Course>) {
        if (currentCourses.isNotEmpty()) return
        viewModelScope.launch {
            val starters = listOf(
                Course(userId = currentUserId, name = "General Science / Maths", code = "SCI 101", colorHex = "#4F46E5"),
                Course(userId = currentUserId, name = "English & Literature", code = "ENG 201", colorHex = "#0EA5E9"),
                Course(userId = currentUserId, name = "WASSCE Past Questions", code = "PASCO", colorHex = "#F59E0B")
            )
            for (c in starters) {
                repository.createCourse(currentUserId, c)
            }
        }
    }

    // Habit Tracker Actions
    fun createHabit(title: String, category: String, colorHex: String) {
        if (title.isBlank()) {
            _userMessage.value = "Habit title cannot be empty"
            return
        }
        viewModelScope.launch {
            val habit = Habit(
                userId = currentUserId,
                title = title.trim(),
                category = category.trim().ifBlank { "Study" },
                colorHex = colorHex,
                streakCount = 0L,
                bestStreak = 0L,
                completedDates = emptyList()
            )
            val result = repository.createHabit(currentUserId, habit)
            if (result.isSuccess) {
                _userMessage.value = "Habit created: ${habit.title}"
            } else {
                _userMessage.value = result.exceptionOrNull()?.message ?: "Failed to create habit"
            }
        }
    }

    fun toggleHabit(habit: Habit) {
        val todayStr = DateUtils.getTodayIsoString()
        val willComplete = !habit.completedDates.contains(todayStr)
        viewModelScope.launch {
            val result = repository.toggleHabitForDate(currentUserId, habit, todayStr)
            if (result.isSuccess && willComplete) {
                val newStreak = habit.streakCount + 1
                _celebrationEvent.value = "🔥 $newStreak Day Streak! Kept '${habit.title}' alive!"
            }
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            val result = repository.deleteHabit(currentUserId, habitId)
            if (result.isSuccess) {
                _userMessage.value = "Habit deleted"
            }
        }
    }

    fun seedDefaultHabitsIfEmpty(currentHabits: List<Habit>) {
        if (currentHabits.isNotEmpty()) return
        viewModelScope.launch {
            val defaults = listOf(
                Habit(
                    userId = currentUserId,
                    title = "Solve 5 WASSCE Pasco Questions",
                    category = "Exam Prep",
                    colorHex = "#F59E0B"
                ),
                Habit(
                    userId = currentUserId,
                    title = "Review Core Notes & Flashcards",
                    category = "Study Habit",
                    colorHex = "#4F46E5"
                ),
                Habit(
                    userId = currentUserId,
                    title = "Complete 1 Focused Pomodoro",
                    category = "Discipline",
                    colorHex = "#10B981"
                )
            )
            for (h in defaults) {
                repository.createHabit(currentUserId, h)
            }
        }
    }
}
