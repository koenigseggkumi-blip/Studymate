package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Assignment
import com.example.ui.FilterType
import com.example.util.DateUtils
import com.example.util.UrgencyLevel

@Composable
fun UrgencyHeaderBanner(
    assignments: List<Assignment>,
    onSelectFilter: (FilterType) -> Unit
) {
    val overdueList = assignments.filter {
        it.status != "COMPLETED" && DateUtils.getUrgencyLevel(it.dueTimestamp) == UrgencyLevel.OVERDUE
    }
    val dueTodayList = assignments.filter {
        it.status != "COMPLETED" && DateUtils.getUrgencyLevel(it.dueTimestamp) == UrgencyLevel.DUE_TODAY
    }
    val dueSoonList = assignments.filter {
        it.status != "COMPLETED" && DateUtils.getUrgencyLevel(it.dueTimestamp) == UrgencyLevel.DUE_SOON
    }
    val completedList = assignments.filter { it.status == "COMPLETED" }
    val totalPending = assignments.count { it.status != "COMPLETED" }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Urgent alert banner if overdue exists
        if (overdueList.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectFilter(FilterType.OVERDUE) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Alarm,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${overdueList.size} Overdue Assignment${if (overdueList.size > 1) "s" else ""}!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B)
                        )
                        Text(
                            text = "Tap to review and prevent further delays",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB91C1C)
                        )
                    }
                    Text(
                        text = "View →",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF991B1B)
                    )
                }
            }
        }

        // Metrics Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "Overdue",
                count = overdueList.size,
                icon = Icons.Default.ErrorOutline,
                accentColor = Color(0xFFEF4444),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectFilter(FilterType.OVERDUE) }
            )
            MetricCard(
                title = "Due Today",
                count = dueTodayList.size,
                icon = Icons.Default.Today,
                accentColor = Color(0xFFF59E0B),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectFilter(FilterType.TODAY) }
            )
            MetricCard(
                title = "Pending",
                count = totalPending,
                icon = Icons.Default.Alarm,
                accentColor = Color(0xFF4F46E5),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectFilter(FilterType.ALL) }
            )
            MetricCard(
                title = "Done",
                count = completedList.size,
                icon = Icons.Default.CheckCircle,
                accentColor = Color(0xFF10B981),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectFilter(FilterType.COMPLETED) }
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    count: Int,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (count > 0 && accentColor == Color(0xFFEF4444)) accentColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
