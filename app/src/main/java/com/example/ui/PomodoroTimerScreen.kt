package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Assignment

@Composable
fun PomodoroTimerScreen(
    viewModel: AssignmentViewModel,
    modifier: Modifier = Modifier
) {
    val mode by viewModel.pomodoroMode.collectAsStateWithLifecycle()
    val timeLeftSeconds by viewModel.timeLeftSeconds.collectAsStateWithLifecycle()
    val isRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()
    val completedSessions by viewModel.completedSessionsCount.collectAsStateWithLifecycle()
    val linkedAssignment by viewModel.linkedAssignment.collectAsStateWithLifecycle()
    val assignmentsState by viewModel.assignmentsState.collectAsStateWithLifecycle()

    val totalDurationSeconds = mode.durationMinutes * 60
    val progress = (timeLeftSeconds.toFloat() / totalDurationSeconds.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "timerProgress")

    val minutes = timeLeftSeconds / 60
    val seconds = timeLeftSeconds % 60
    val timeFormatted = "%02d:%02d".format(minutes, seconds)

    val timerColor by animateColorAsState(
        targetValue = when (mode) {
            PomodoroMode.FOCUS -> MaterialTheme.colorScheme.primary
            PomodoroMode.SHORT_BREAK -> Color(0xFF10B981) // Emerald
            PomodoroMode.LONG_BREAK -> Color(0xFF0EA5E9)  // Sky
        },
        label = "timerColor"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Mode Selector Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PomodoroMode.values().forEach { m ->
                val isSelected = mode == m
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setPomodoroMode(m) },
                    label = { Text(m.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = timerColor.copy(alpha = 0.2f),
                        selectedLabelColor = timerColor
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Circular Timer Visual
        Box(
            modifier = Modifier
                .size(270.dp)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                // Background Track
                drawCircle(
                    color = timerColor.copy(alpha = 0.15f),
                    style = Stroke(width = strokeWidth)
                )
                // Active Arc
                drawArc(
                    color = timerColor,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = timeFormatted,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-1).sp
                )

                Text(
                    text = if (isRunning) "Focusing..." else if (timeLeftSeconds == 0) "Complete!" else "Paused",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isRunning) timerColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                if (linkedAssignment != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = timerColor.copy(alpha = 0.12f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "📌 ${linkedAssignment!!.title}",
                            style = MaterialTheme.typography.labelSmall,
                            color = timerColor,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Timer Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reset
            IconButton(
                onClick = { viewModel.resetTimer() },
                modifier = Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .testTag("reset_timer_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset timer")
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Primary Play / Pause Button
            Button(
                onClick = {
                    if (isRunning) viewModel.pauseTimer() else viewModel.startTimer()
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = timerColor),
                modifier = Modifier
                    .size(76.dp)
                    .testTag("toggle_timer_button")
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Start",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Quick +5 Min Break / Study extension
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(52.dp)
                    .clickable {
                        // Quick switch to break or reset
                        viewModel.resetTimer()
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "25m",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Linked Assignment Picker
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Assignment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Link to Active Task / Pasco",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (linkedAssignment != null) {
                        TextButton(onClick = { viewModel.selectLinkedAssignment(null) }) {
                            Text("Clear", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                when (val state = assignmentsState) {
                    is UiState.Success -> {
                        val pending = state.data.filter { it.status != "COMPLETED" }
                        if (pending.isEmpty()) {
                            Text(
                                text = "No pending assignments. You can still focus on revision or exam questions!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pending.take(3).forEach { assignment ->
                                    val isSelected = linkedAssignment?.id == assignment.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.selectLinkedAssignment(if (isSelected) null else assignment)
                                        },
                                        label = {
                                            Text(
                                                text = assignment.title,
                                                maxLines = 1,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        // Daily Study Momentum
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$completedSessions",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Pomodoros Done",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${completedSessions * 25}m",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Total Focus Time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Pomodoro Advice for WASSCE / Exam students
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "💡 Exam Prep Strategy",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Dedicate 1 Pomodoro (25 min) to solving 5 Pasco (Past Questions) under exam conditions without checking answers. Use the 5 min break to review mistakes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
