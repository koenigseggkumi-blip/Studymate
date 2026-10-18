package com.example.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class UrgencyLevel {
    OVERDUE,
    DUE_TODAY,
    DUE_SOON, // next 48 hours
    THIS_WEEK,
    UPCOMING
}

object DateUtils {

    fun formatDueString(timestamp: Timestamp?): String {
        if (timestamp == null) return "No due date"
        val date = timestamp.toDate()
        val now = Date()
        val diffMillis = date.time - now.time

        val sdfTime = SimpleDateFormat("h:mm a", Locale.getDefault())
        val sdfDate = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        val sdfFull = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())

        return when {
            diffMillis < 0 -> {
                val absDiff = -diffMillis
                val hours = TimeUnit.MILLISECONDS.toHours(absDiff)
                val days = TimeUnit.MILLISECONDS.toDays(absDiff)
                if (days > 0) "Overdue by $days d"
                else if (hours > 0) "Overdue by $hours h"
                else "Just expired"
            }
            diffMillis < TimeUnit.HOURS.toMillis(1) -> {
                val mins = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
                "Due in $mins min"
            }
            diffMillis < TimeUnit.HOURS.toMillis(24) && isSameDay(date, now) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
                "Due today at ${sdfTime.format(date)} ($hours h left)"
            }
            diffMillis < TimeUnit.HOURS.toMillis(48) -> {
                "Tomorrow at ${sdfTime.format(date)}"
            }
            diffMillis < TimeUnit.DAYS.toMillis(7) -> {
                "${sdfDate.format(date)} at ${sdfTime.format(date)}"
            }
            else -> {
                sdfFull.format(date)
            }
        }
    }

    fun getUrgencyLevel(timestamp: Timestamp?): UrgencyLevel {
        if (timestamp == null) return UrgencyLevel.UPCOMING
        val date = timestamp.toDate()
        val now = Date()
        val diff = date.time - now.time

        return when {
            diff < 0 -> UrgencyLevel.OVERDUE
            isSameDay(date, now) -> UrgencyLevel.DUE_TODAY
            diff <= TimeUnit.HOURS.toMillis(48) -> UrgencyLevel.DUE_SOON
            diff <= TimeUnit.DAYS.toMillis(7) -> UrgencyLevel.THIS_WEEK
            else -> UrgencyLevel.UPCOMING
        }
    }

    fun isSameDay(d1: Date, d2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = d1 }
        val cal2 = Calendar.getInstance().apply { time = d2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun formatEstimatedTime(minutes: Long?): String? {
        if (minutes == null || minutes <= 0) return null
        return if (minutes < 60) {
            "${minutes}m est."
        } else {
            val hours = minutes / 60
            val rem = minutes % 60
            if (rem == 0L) "${hours}h est." else "${hours}h ${rem}m est."
        }
    }

    fun getTodayIsoString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    data class DayColumn(val dateStr: String, val dayLabel: String, val isToday: Boolean)

    fun getPast7DaysColumns(): List<DayColumn> {
        val list = mutableListOf<DayColumn>()
        val cal = Calendar.getInstance()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        val todayStr = isoFormat.format(cal.time)

        for (i in 6 downTo 0) {
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dateStr = isoFormat.format(c.time)
            val label = dayFormat.format(c.time).take(1) // M, T, W...
            list.add(DayColumn(dateStr = dateStr, dayLabel = label, isToday = dateStr == todayStr))
        }
        return list
    }

    fun calculateStreak(completedDates: List<String>): Long {
        if (completedDates.isEmpty()) return 0L
        val dateSet = completedDates.toSet()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val todayStr = isoFormat.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = isoFormat.format(cal.time)

        // Check if streak is active (completed today or completed yesterday)
        val startCal = Calendar.getInstance()
        if (!dateSet.contains(todayStr)) {
            if (!dateSet.contains(yesterdayStr)) {
                return 0L
            }
            startCal.add(Calendar.DAY_OF_YEAR, -1)
        }

        var streak = 0L
        while (true) {
            val checkStr = isoFormat.format(startCal.time)
            if (dateSet.contains(checkStr)) {
                streak++
                startCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }
}

