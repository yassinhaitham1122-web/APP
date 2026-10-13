package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeUtils {

    fun getCurrentDate(timezoneId: String = "Africa/Cairo"): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = getTimeZoneSafely(timezoneId)
        return sdf.format(Date())
    }

    fun getCurrentTime(timezoneId: String = "Africa/Cairo"): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
        sdf.timeZone = getTimeZoneSafely(timezoneId)
        return sdf.format(Date())
    }

    fun getCurrentFormattedDate(timezoneId: String = "Africa/Cairo", isArabic: Boolean = false): String {
        val locale = if (isArabic) Locale("ar") else Locale.US
        val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", locale)
        sdf.timeZone = getTimeZoneSafely(timezoneId)
        return sdf.format(Date())
    }

    fun getTimeZoneSafely(timezoneId: String): TimeZone {
        return try {
            TimeZone.getTimeZone(timezoneId)
        } catch (e: Exception) {
            TimeZone.getDefault()
        }
    }

    fun parseTimeToSeconds(timeStr: String): Int {
        val parts = timeStr.trim().split(":")
        if (parts.isEmpty()) return 0
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val s = parts.getOrNull(2)?.toIntOrNull() ?: 0
        return h * 3600 + m * 60 + s
    }

    fun formatSecondsToHms(totalSeconds: Int): String {
        val sec = if (totalSeconds < 0) 0 else totalSeconds
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    }

    fun formatSecondsHuman(totalSeconds: Int, isArabic: Boolean = false): String {
        val sec = if (totalSeconds < 0) 0 else totalSeconds
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return if (isArabic) {
            when {
                h > 0 && m > 0 -> "$h س و $m د"
                h > 0 -> "$h س"
                m > 0 -> "$m د"
                else -> "$s ث"
            }
        } else {
            when {
                h > 0 && m > 0 -> "${h}h ${m}m"
                h > 0 -> "${h}h"
                m > 0 -> "${m}m"
                else -> "${s}s"
            }
        }
    }

    fun calculateLatenessSeconds(
        actualCheckIn: String,
        officialStart: String,
        graceMinutes: Int
    ): Int {
        val actualSec = parseTimeToSeconds(actualCheckIn)
        val startSec = parseTimeToSeconds(officialStart)
        val thresholdSec = startSec + (graceMinutes * 60)
        return if (actualSec > thresholdSec) {
            actualSec - thresholdSec
        } else {
            0
        }
    }

    fun calculateEarlyDepartureSeconds(
        actualCheckOut: String,
        officialEnd: String
    ): Int {
        val actualSec = parseTimeToSeconds(actualCheckOut)
        val endSec = parseTimeToSeconds(officialEnd)
        return if (actualSec < endSec) {
            endSec - actualSec
        } else {
            0
        }
    }

    fun calculateOvertimeSeconds(
        actualCheckOut: String,
        officialEnd: String
    ): Int {
        val actualSec = parseTimeToSeconds(actualCheckOut)
        val endSec = parseTimeToSeconds(officialEnd)
        return if (actualSec > endSec) {
            actualSec - endSec
        } else {
            0
        }
    }

    fun calculateWorkDurationSeconds(
        checkIn: String?,
        checkOut: String?
    ): Int {
        if (checkIn.isNullOrBlank() || checkOut.isNullOrBlank()) return 0
        val inSec = parseTimeToSeconds(checkIn)
        val outSec = parseTimeToSeconds(checkOut)
        return if (outSec >= inSec) outSec - inSec else 0
    }

    /**
     * Checks if given date is a weekly day off.
     * weeklyDaysOffStr is comma-separated Calendar day of week integers:
     * Calendar.SUNDAY=1, MONDAY=2, TUESDAY=3, WEDNESDAY=4, THURSDAY=5, FRIDAY=6, SATURDAY=7
     */
    fun isWeeklyDayOff(dateStr: String, weeklyDaysOffStr: String, timezoneId: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.timeZone = getTimeZoneSafely(timezoneId)
            val date = sdf.parse(dateStr) ?: return false
            val cal = Calendar.getInstance(getTimeZoneSafely(timezoneId))
            cal.time = date
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 6=Fri, 7=Sat
            val offDays = weeklyDaysOffStr.split(",").mapNotNull { it.trim().toIntOrNull() }
            offDays.contains(dayOfWeek)
        } catch (e: Exception) {
            false
        }
    }
}
