package com.example.ui.components

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.unit.sp
import com.example.data.model.Course
import com.example.data.model.SubtaskItem
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Compose UI Component with text fields and a date picker to capture new assignment details
 * for the database.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewAssignmentInputComponent(
    courses: List<Course>,
    onSaveAssignment: (
        title: String,
        courseName: String,
        courseColorHex: String,
        dueTimestamp: Timestamp,
        priority: String,
        notes: String?,
        estimatedMinutes: Long?,
        subtasks: List<SubtaskItem>
    ) -> Unit,
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Form field states
    var title by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }
    var selectedCourse by remember { mutableStateOf(courses.firstOrNull()) }
    var customCourseName by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var notes by remember { mutableStateOf("") }
    var estimatedMinutes by remember { mutableStateOf("60") }

    // Calendar for selected Due Date & Time
    val dueCalendar = remember {
        Calendar.getInstance().apply {
            // Default: Due Tomorrow at 11:59 PM
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)
        }
    }
    var selectedDateMillis by remember { mutableStateOf(dueCalendar.timeInMillis) }
    var selectedHour by remember { mutableStateOf(dueCalendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(dueCalendar.get(Calendar.MINUTE)) }

    // Subtasks
    var subtasks by remember { mutableStateOf<List<SubtaskItem>>(emptyList()) }
    var newSubtaskText by remember { mutableStateOf("") }

    // DatePicker Dialog visibility
    var showM3DatePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("new_assignment_form_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "New Assignment",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Add details to stay ahead of your deadline",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag("cancel_assignment_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close form")
                }
            }

            // 1. Title Text Field
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isNotBlank()) titleError = false
                },
                label = { Text("Assignment Title *") },
                placeholder = { Text("e.g. Research Paper, Calculus Homework 4") },
                singleLine = true,
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("Title is required", color = MaterialTheme.colorScheme.error) }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("assignment_title_input")
            )

            // 2. Course Selection & Text Input
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Course / Subject",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (courses.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        courses.forEach { course ->
                            val isSelected = selectedCourse?.id == course.id && customCourseName.isBlank()
                            val color = try {
                                Color(android.graphics.Color.parseColor(course.colorHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCourse = course
                                    customCourseName = ""
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(color, CircleShape)
                                    )
                                },
                                label = { Text("${course.code} • ${course.name}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color.copy(alpha = 0.2f),
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                // Custom Course Name field (if user types a subject directly)
                OutlinedTextField(
                    value = customCourseName,
                    onValueChange = {
                        customCourseName = it
                        if (it.isNotBlank()) selectedCourse = null
                    },
                    label = { Text(if (courses.isEmpty()) "Course Name *" else "Or type other course name") },
                    placeholder = { Text("e.g. Organic Chemistry, CS50") },
                    leadingIcon = {
                        Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("assignment_course_input")
                )
            }

            // 3. Date Picker & Time Picker Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Due Date & Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Quick Date Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(
                        "Today" to 0,
                        "Tomorrow" to 1,
                        "In 3 Days" to 3,
                        "Next Week" to 7
                    )
                    presets.forEach { (label, daysOffset) ->
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, daysOffset)
                                    set(Calendar.HOUR_OF_DAY, selectedHour)
                                    set(Calendar.MINUTE, selectedMinute)
                                    set(Calendar.SECOND, 0)
                                }
                                selectedDateMillis = cal.timeInMillis
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = label, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }

                // Interactive Pickers (Date + Time)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Date Picker Trigger Button
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .weight(1.3f)
                            .clickable { showM3DatePicker = true }
                            .testTag("open_date_picker_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = "Select Date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Due Date",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = dateFormatter.format(Date(selectedDateMillis)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Time Picker Trigger Button
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        selectedHour = hourOfDay
                                        selectedMinute = minute
                                        val cal = Calendar.getInstance().apply {
                                            timeInMillis = selectedDateMillis
                                            set(Calendar.HOUR_OF_DAY, hourOfDay)
                                            set(Calendar.MINUTE, minute)
                                        }
                                        selectedDateMillis = cal.timeInMillis
                                    },
                                    selectedHour,
                                    selectedMinute,
                                    false
                                ).show()
                            }
                            .testTag("open_time_picker_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = "Select Time",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Due Time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val timeCal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, selectedHour)
                                    set(Calendar.MINUTE, selectedMinute)
                                }
                                Text(
                                    text = timeFormatter.format(timeCal.time),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 4. Priority Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Priority Level",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val priorityOptions = listOf(
                        Triple("LOW", "Low", Color(0xFF10B981)),
                        Triple("MEDIUM", "Medium", Color(0xFFF59E0B)),
                        Triple("HIGH", "High (Urgent)", Color(0xFFEF4444))
                    )
                    priorityOptions.forEach { (code, label, color) ->
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
            }

            // 5. Estimated Workload Text Field
            OutlinedTextField(
                value = estimatedMinutes,
                onValueChange = { input ->
                    if (input.all { it.isDigit() }) estimatedMinutes = input
                },
                label = { Text("Estimated Completion Time (minutes)") },
                placeholder = { Text("e.g. 45, 90, 120") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("assignment_duration_input")
            )

            // 6. Subtask Checklist Builder (Proactive anti-forgetting breakdown)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Checklist & Action Steps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newSubtaskText,
                        onValueChange = { newSubtaskText = it },
                        placeholder = { Text("Add step (e.g. Draft outline, Proofread)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_subtask_input")
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
                        modifier = Modifier.testTag("add_subtask_item_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Step")
                    }
                }

                // Subtask list
                subtasks.forEachIndexed { index, item ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "• ${item.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    subtasks = subtasks.toMutableList().also { it.removeAt(index) }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove step",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 7. Notes Text Field
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes & Instructions") },
                placeholder = { Text("Specific instructions, lecture notes, or portal URL...") },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("assignment_notes_input")
            )

            // 8. Submit & Cancel Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (title.isBlank()) {
                            titleError = true
                            return@Button
                        }

                        val courseName = when {
                            customCourseName.isNotBlank() -> customCourseName.trim()
                            selectedCourse != null -> selectedCourse!!.name
                            else -> "General"
                        }
                        val courseColorHex = selectedCourse?.colorHex ?: "#4F46E5"

                        val finalCal = Calendar.getInstance().apply {
                            timeInMillis = selectedDateMillis
                            set(Calendar.HOUR_OF_DAY, selectedHour)
                            set(Calendar.MINUTE, selectedMinute)
                            set(Calendar.SECOND, 0)
                        }
                        val dueTimestamp = Timestamp(finalCal.time)
                        val est = estimatedMinutes.toLongOrNull() ?: 60L

                        onSaveAssignment(
                            title.trim(),
                            courseName,
                            courseColorHex,
                            dueTimestamp,
                            priority,
                            notes.trim().ifEmpty { null },
                            est,
                            subtasks
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(50.dp)
                        .testTag("submit_new_assignment_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save Assignment",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Material 3 DatePickerDialog
    if (showM3DatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )

        DatePickerDialog(
            onDismissRequest = { showM3DatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            // Preserve selected hour and minute
                            val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = millis
                            }
                            val localCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, selectedHour)
                                set(Calendar.MINUTE, selectedMinute)
                                set(Calendar.SECOND, 0)
                            }
                            selectedDateMillis = localCal.timeInMillis
                        }
                        showM3DatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showM3DatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
