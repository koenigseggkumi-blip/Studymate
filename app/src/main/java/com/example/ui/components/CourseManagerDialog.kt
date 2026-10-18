package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Course

private val PRESET_COLORS = listOf(
    "#4F46E5", // Indigo
    "#0EA5E9", // Sky Blue
    "#10B981", // Emerald
    "#F59E0B", // Amber
    "#EF4444", // Crimson
    "#8B5CF6", // Purple
    "#EC4899", // Pink
    "#14B8A6"  // Teal
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CourseManagerDialog(
    courses: List<Course>,
    onDismiss: () -> Unit,
    onCreateCourse: (name: String, code: String, colorHex: String) -> Unit,
    onDeleteCourse: (courseId: String) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var newCode by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf(PRESET_COLORS.first()) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Manage Courses", style = MaterialTheme.typography.headlineMedium)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
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
                Text(
                    text = "Add New Course",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        showError = false
                    },
                    label = { Text("Course Name *") },
                    placeholder = { Text("e.g. Data Structures & Algorithms") },
                    singleLine = true,
                    isError = showError && newName.isBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("course_name_input")
                )

                OutlinedTextField(
                    value = newCode,
                    onValueChange = {
                        newCode = it
                        showError = false
                    },
                    label = { Text("Course Code *") },
                    placeholder = { Text("e.g. CS 210") },
                    singleLine = true,
                    isError = showError && newCode.isBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("course_code_input")
                )

                Text(
                    text = "Color Badge",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PRESET_COLORS.forEach { hex ->
                        val isSelected = selectedColorHex == hex
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(color, CircleShape)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }

                Button(
                    onClick = {
                        if (newName.isBlank() || newCode.isBlank()) {
                            showError = true
                            return@Button
                        }
                        onCreateCourse(newName, newCode, selectedColorHex)
                        newName = ""
                        newCode = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_course_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Course")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Existing Courses (${courses.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (courses.isEmpty()) {
                    Text(
                        text = "No courses added yet. Add one above to organize your assignments.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    courses.forEach { course ->
                        val color = try {
                            Color(android.graphics.Color.parseColor(course.colorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(color, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = course.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = course.code,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteCourse(course.id) }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete course",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
