package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Assignment
import com.example.data.model.SubtaskItem
import com.example.util.DateUtils
import com.example.util.UrgencyLevel

@Composable
fun AssignmentCard(
    assignment: Assignment,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    onToggleSubtask: (subtaskId: String) -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = assignment.status == "COMPLETED"
    val urgency = DateUtils.getUrgencyLevel(assignment.dueTimestamp)
    val courseColor = try {
        Color(android.graphics.Color.parseColor(assignment.courseColorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    // Border highlight for urgent / overdue items
    val borderColor = when {
        isCompleted -> Color.Transparent
        urgency == UrgencyLevel.OVERDUE -> Color(0xFFEF4444)
        urgency == UrgencyLevel.DUE_TODAY -> Color(0xFFF59E0B)
        else -> Color.Transparent
    }

    val cardBackground by animateColorAsState(
        targetValue = if (isCompleted) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        } else if (urgency == UrgencyLevel.OVERDUE) {
            Color(0xFFFEF2F2).copy(alpha = 0.9f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "cardBackground"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
                } else Modifier
            )
            .clickable { onClick() }
            .testTag("assignment_card_${assignment.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Course Badge + Priority + Urgency Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Course tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = courseColor.copy(alpha = 0.15f),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(courseColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = assignment.courseName,
                            style = MaterialTheme.typography.labelSmall,
                            color = courseColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Urgency & Priority Badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!isCompleted && assignment.priority == "HIGH") {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "HIGH",
                                color = Color(0xFFEF4444),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Urgency Badge
                    val (urgencyText, urgencyColor) = when {
                        isCompleted -> Pair("Completed", Color(0xFF10B981))
                        urgency == UrgencyLevel.OVERDUE -> Pair("OVERDUE", Color(0xFFDC2626))
                        urgency == UrgencyLevel.DUE_TODAY -> Pair("TODAY", Color(0xFFD97706))
                        urgency == UrgencyLevel.DUE_SOON -> Pair("SOON", Color(0xFF2563EB))
                        else -> Pair("", Color.Transparent)
                    }

                    if (urgencyText.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = urgencyColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = urgencyText,
                                color = urgencyColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Title and Completion Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val checkScale by animateFloatAsState(
                    targetValue = if (isCompleted) 1.15f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "checkScale"
                )

                // Circular completion button
                Surface(
                    shape = CircleShape,
                    color = if (isCompleted) Color(0xFF10B981) else Color.Transparent,
                    modifier = Modifier
                        .size(32.dp)
                        .graphicsLayer(scaleX = checkScale, scaleY = checkScale)
                        .border(
                            width = 2.dp,
                            color = if (isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .clickable { onToggleComplete() }
                        .testTag("toggle_complete_${assignment.id}")
                ) {
                    if (isCompleted) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = assignment.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                        color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Due date string
                    Text(
                        text = DateUtils.formatDueString(assignment.dueTimestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
                            urgency == UrgencyLevel.OVERDUE -> Color(0xFFDC2626)
                            urgency == UrgencyLevel.DUE_TODAY -> Color(0xFFD97706)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (urgency == UrgencyLevel.OVERDUE || urgency == UrgencyLevel.DUE_TODAY) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete assignment",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Subtask Breakdown Progress & Preview
            if (assignment.subtasks.isNotEmpty()) {
                val totalSteps = assignment.subtasks.size
                val doneSteps = assignment.subtasks.count { it["completed"] == true }
                val progress = if (totalSteps > 0) doneSteps.toFloat() / totalSteps else 0f

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.FormatListBulleted,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Action Checklist: $doneSteps / $totalSteps completed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = if (progress >= 1f) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Quick inline checklist items
                    assignment.subtasks.take(3).forEach { rawSubtask ->
                        val item = SubtaskItem.fromMap(rawSubtask)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSubtask(item.id) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.completed,
                                onCheckedChange = { onToggleSubtask(item.id) },
                                modifier = Modifier.size(24.dp),
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (item.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (totalSteps > 3) {
                        Text(
                            text = "+ ${totalSteps - 3} more steps (tap card to see all)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 24.dp)
                        )
                    }
                }
            }

            // Footer: Estimated study time & notes indicator
            val estimatedStr = DateUtils.formatEstimatedTime(assignment.estimatedMinutes)
            if (estimatedStr != null || !assignment.notes.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (estimatedStr != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = estimatedStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!assignment.notes.isNullOrBlank()) {
                        Text(
                            text = "📝 Notes attached",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
