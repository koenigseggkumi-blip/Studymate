package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Assignment
import com.example.data.model.Course
import com.example.data.model.SubtaskItem
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssignmentEditorDialog(
    initialAssignment: Assignment? = null,
    courses: List<Course>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        courseName: String,
        courseColorHex: String,
        dueTimestamp: Timestamp,
        priority: String,
        notes: String?,
        estimatedMinutes: Long?,
        subtasks: List<SubtaskItem>
    ) -> Unit,
    onRequestNewCourse: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialAssignment?.title ?: "") }
    var selectedCourse by remember {
        mutableStateOf(
            courses.find { it.name == initialAssignment?.courseName }
                ?: courses.firstOrNull()
        )
    }
    var priority by remember { mutableStateOf(initialAssignment?.priority ?: "MEDIUM") }
    var notes by remember { mutableStateOf(initialAssignment?.notes ?: "") }
    var estimatedMinutesText by remember {
        mutableStateOf(initialAssignment?.estimatedMinutes?.toString() ?: "60")
    }

    // Due date state
    val calendar = remember {
        Calendar.getInstance().apply {
            if (initialAssignment?.dueTimestamp != null) {
                time = initialAssignment.dueTimestamp.toDate()
            } else {
                // Default due tomorrow at 11:59 PM
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 0)
            }
        }
    }
    var dueCalendarState by remember { mutableStateOf<Calendar>(calendar.clone() as Calendar) }

    // Subtasks
    var subtasks by remember {
        mutableStateOf<List<SubtaskItem>>(
            initialAssignment?.subtasks?.map { SubtaskItem.fromMap(it) } ?: emptyList()
        )
    }
    var newSubtaskText by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialAssignment == null) "New Assignment" else "Edit Assignment",
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close dialog")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    label = { Text("Assignment Title *") },
                    placeholder = { Text("e.g. Chapter 4 Problem Set, Research Paper Draft") },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text("Title cannot be empty") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("assignment_title_input")
                )

                // Course Selection
                Text(
                    text = "Course / Subject",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    courses.forEach { course ->
                        val isSelected = selectedCourse?.id == course.id
                        val color = try {
                            Color(android.graphics.Color.parseColor(course.colorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCourse = course },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(color, CircleShape)
                                )
                            },
                            label = { Text("${course.code} - ${course.name}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    // Add course button
                    OutlinedButton(
                        onClick = onRequestNewCourse,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Course")
                    }
                }

                // Due Date & Time
                Text(
                    text = "Due Date & Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Date picker trigger
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1.3f)
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        dueCalendarState.set(Calendar.YEAR, year)
                                        dueCalendarState.set(Calendar.MONTH, month)
                                        dueCalendarState.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        dueCalendarState = dueCalendarState.clone() as Calendar
                                    },
                                    dueCalendarState.get(Calendar.YEAR),
                                    dueCalendarState.get(Calendar.MONTH),
                                    dueCalendarState.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = dateFormat.format(dueCalendarState.time),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Time picker trigger
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        dueCalendarState.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        dueCalendarState.set(Calendar.MINUTE, minute)
                                        dueCalendarState = dueCalendarState.clone() as Calendar
                                    },
                                    dueCalendarState.get(Calendar.HOUR_OF_DAY),
                                    dueCalendarState.get(Calendar.MINUTE),
                                    false
                                ).show()
                            }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = timeFormat.format(dueCalendarState.time),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Priority
                Text(
                    text = "Priority Level",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val priorities = listOf(
                        Triple("LOW", "Low", Color(0xFF10B981)),
                        Triple("MEDIUM", "Medium", Color(0xFFF59E0B)),
                        Triple("HIGH", "High (Urgent)", Color(0xFFEF4444))
                    )
                    priorities.forEach { (code, label, color) ->
                        val isSelected = priority == code
                        FilterChip(
                            selected = isSelected,
                            onClick = { priority = code },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.2f),
                                selectedLabelColor = color
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Estimated Workload (Minutes)
                OutlinedTextField(
                    value = estimatedMinutesText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) estimatedMinutesText = input
                    },
                    label = { Text("Estimated Study Time (minutes)") },
                    placeholder = { Text("e.g. 45, 90, 120") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Subtask Checklist (Anti-forgetting breakdown!)
                Text(
                    text = "Action Steps & Checklist",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Breaking large assignments into small steps prevents procrastination and forgetting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Add subtask input row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newSubtaskText,
                        onValueChange = { newSubtaskText = it },
                        placeholder = { Text("Add step (e.g. Gather sources, Write outline)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("subtask_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newSubtaskText.isNotBlank()) {
                                subtasks = subtasks + SubtaskItem(
                                    id = UUID.randomUUID().toString(),
                                    title = newSubtaskText.trim(),
                                    completed = false
                                )
                                newSubtaskText = ""
                            }
                        },
                        modifier = Modifier.testTag("add_subtask_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add step")
                    }
                }

                // Existing subtasks list
                subtasks.forEachIndexed { index, subtask ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = subtask.completed,
                            onCheckedChange = { checked ->
                                subtasks = subtasks.toMutableList().also {
                                    it[index] = subtask.copy(completed = checked)
                                }
                            }
                        )
                        Text(
                            text = subtask.title,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                subtasks = subtasks.toMutableList().also { it.removeAt(index) }
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove subtask",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Notes / Submission requirements
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes & Submission Link") },
                    placeholder = { Text("Submission guidelines, professor tips, portal URL...") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        return@Button
                    }
                    val course = selectedCourse ?: Course(name = "General", code = "GEN", colorHex = "#4F46E5")
                    val estimated = estimatedMinutesText.toLongOrNull() ?: 60L
                    val dueTimestamp = Timestamp(dueCalendarState.time)

                    onSave(
                        title.trim(),
                        course.name,
                        course.colorHex,
                        dueTimestamp,
                        priority,
                        notes.trim().ifEmpty { null },
                        estimated,
                        subtasks
                    )
                },
                modifier = Modifier.testTag("save_assignment_button")
            ) {
                Text(if (initialAssignment == null) "Create Assignment" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
