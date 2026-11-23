package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTimeUtils {

  fun startOfDayMillis(timestampMillis: Long): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = timestampMillis
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
  }

  fun endOfDayMillis(timestampMillis: Long): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = timestampMillis
      set(Calendar.HOUR_OF_DAY, 23)
      set(Calendar.MINUTE, 59)
      set(Calendar.SECOND, 59)
      set(Calendar.MILLISECOND, 999)
    }
    return cal.timeInMillis
  }

  fun daysAgoStartMillis(daysAgo: Int, nowMillis: Long = System.currentTimeMillis()): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = startOfDayMillis(nowMillis)
      add(Calendar.DAY_OF_YEAR, -daysAgo)
    }
    return cal.timeInMillis
  }

  fun dayKey(timestampMillis: Long): Long = startOfDayMillis(timestampMillis)

  fun hourOfDay(timestampMillis: Long): Int {
    val cal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
    return cal.get(Calendar.HOUR_OF_DAY)
  }

  fun formatTime(timestampMillis: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestampMillis))
  }

  fun formatShortDate(timestampMillis: Long): String {
    val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
    return sdf.format(Date(timestampMillis))
  }

  fun formatFullDate(timestampMillis: Long): String {
    val sdf = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestampMillis))
  }

  fun formatDateAndTime(timestampMillis: Long): String {
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(Date(timestampMillis))
  }

  fun formatHeaderDate(timestampMillis: Long): String {
    val sdf = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    return sdf.format(Date(timestampMillis))
  }

  fun formatRelativeDay(dayStartMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val todayStart = startOfDayMillis(nowMillis)
    val yesterdayStart = daysAgoStartMillis(1, nowMillis)
    return when (dayStartMillis) {
      todayStart -> "Today"
      yesterdayStart -> "Yesterday"
      else -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(dayStartMillis))
    }
  }

  fun formatFileDate(nowMillis: Long = System.currentTimeMillis()): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return sdf.format(Date(nowMillis))
  }

  fun formatIsoUtc(timestampMillis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
    return sdf.format(Date(timestampMillis))
  }

  fun parseIsoUtcOrNull(iso: String): Long? {
    val trimmed = iso.trim()
    val patterns = listOf(
      "yyyy-MM-dd'T'HH:mm:ss'Z'",
      "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
      "yyyy-MM-dd'T'HH:mm:ssXXX",
      "yyyy-MM-dd'T'HH:mm:ss",
      "yyyy-MM-dd HH:mm:ss",
    )
    for (pattern in patterns) {
      try {
        val sdf = SimpleDateFormat(pattern, Locale.US).apply {
          if (pattern.endsWith("'Z'")) {
            timeZone = TimeZone.getTimeZone("UTC")
          }
        }
        val parsed = sdf.parse(trimmed)
        if (parsed != null) return parsed.time
      } catch (_: Exception) {
        // try next pattern
      }
    }
    return null
  }

  fun combineDateAndTime(
    dateUtcMillis: Long,
    hourOfDay: Int,
    minute: Int,
  ): Long {
    // Material3 DatePicker returns UTC midnight millis for the chosen calendar date
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      timeInMillis = dateUtcMillis
    }
    val localCal = Calendar.getInstance().apply {
      set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
      set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
      set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
      set(Calendar.HOUR_OF_DAY, hourOfDay)
      set(Calendar.MINUTE, minute)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    return localCal.timeInMillis
  }

  fun toUtcDatePickerMillis(localTimestampMillis: Long): Long {
    val localCal = Calendar.getInstance().apply { timeInMillis = localTimestampMillis }
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      set(Calendar.YEAR, localCal.get(Calendar.YEAR))
      set(Calendar.MONTH, localCal.get(Calendar.MONTH))
      set(Calendar.DAY_OF_MONTH, localCal.get(Calendar.DAY_OF_MONTH))
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    return utcCal.timeInMillis
  }
}
