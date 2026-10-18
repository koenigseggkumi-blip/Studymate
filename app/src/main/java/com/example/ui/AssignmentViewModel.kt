package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Assignment
import com.example.data.model.Course
import com.example.data.model.SubtaskItem
import com.example.data.repository.AssignmentRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

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
        val newStatus = if (assignment.status == "COMPLETED") "IN_PROGRESS" else "COMPLETED"
        val updated = assignment.copy(status = newStatus)
        viewModelScope.launch {
            val result = repository.updateAssignment(currentUserId, updated)
            if (result.isSuccess) {
                if (newStatus == "COMPLETED") {
                    _userMessage.value = "🎉 Great job completing '${assignment.title}'!"
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
                Course(userId = currentUserId, name = "Computer Science", code = "CS 101", colorHex = "#4F46E5"),
                Course(userId = currentUserId, name = "Mathematics", code = "MATH 220", colorHex = "#0EA5E9"),
                Course(userId = currentUserId, name = "Literature & History", code = "LIT 105", colorHex = "#F59E0B")
            )
            for (c in starters) {
                repository.createCourse(currentUserId, c)
            }
        }
    }
}
