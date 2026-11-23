package com.example.viewmodel

import com.example.model.MoodEntry
import com.example.util.DateTimeUtils
import java.util.Calendar
import kotlin.math.roundToInt

enum class InsightTimeRange(val label: String) {
  DAY("Day"),
  WEEK("Week"),
  MONTH("Month"),
  ALL("All"),
}

enum class TimeOfDayMode(val label: String) {
  PART_OF_DAY("Part of day"),
  HOURLY("By hour"),
}

enum class HistoryDateFilter(val label: String) {
  ALL("All time"),
  TODAY("Today"),
  LAST_7_DAYS("7 days"),
  LAST_30_DAYS("30 days"),
  CUSTOM_DAY("Pick date"),
}

data class MoodGraphPoint(
  val timestamp: Long,
  val value: Float,
  val label: String,
  val entryCount: Int,
)

data class HeatmapDayCell(
  val dayStartMillis: Long,
  val averageMood: Float?,
  val entryCount: Int,
  val weekColumn: Int,
  val dayOfWeekRow: Int, // 0 = Mon .. 6 = Sun
  val isToday: Boolean,
  val isFuture: Boolean,
)

data class TimeOfDayBucket(
  val label: String,
  val shortLabel: String,
  val averageMood: Float?,
  val entryCount: Int,
)

data class MoodStats(
  val averageMood: Float?,
  val totalEntries: Int,
  val currentStreakDays: Int,
  val distinctTagCount: Int,
)

object MoodAnalytics {

  private val defaultSuggestedTags = listOf(
    "calm",
    "tired",
    "anxious",
    "social",
    "focused",
    "grateful",
    "energized",
    "overwhelmed",
    "walk",
    "coffee",
    "rainy",
  )

  fun computeStats(entries: List<MoodEntry>, nowMillis: Long = System.currentTimeMillis()): MoodStats {
    if (entries.isEmpty()) {
      return MoodStats(
        averageMood = null,
        totalEntries = 0,
        currentStreakDays = 0,
        distinctTagCount = 0,
      )
    }

    val avg = entries.map { it.mood }.average().toFloat()
    val distinctTags = entries.flatMap { it.tags }.map { it.lowercase() }.toSet().size

    val loggedDays = entries.map { DateTimeUtils.dayKey(it.timestamp) }.toSet()
    val todayStart = DateTimeUtils.startOfDayMillis(nowMillis)
    val yesterdayStart = DateTimeUtils.daysAgoStartMillis(1, nowMillis)

    var streak = 0
    var cursorDay = when {
      todayStart in loggedDays -> todayStart
      yesterdayStart in loggedDays -> yesterdayStart
      else -> null
    }

    while (cursorDay != null && cursorDay in loggedDays) {
      streak++
      val cal = Calendar.getInstance().apply {
        timeInMillis = cursorDay
        add(Calendar.DAY_OF_YEAR, -1)
      }
      cursorDay = DateTimeUtils.startOfDayMillis(cal.timeInMillis)
    }

    return MoodStats(
      averageMood = avg,
      totalEntries = entries.size,
      currentStreakDays = streak,
      distinctTagCount = distinctTags,
    )
  }

  fun computeLineGraphPoints(
    entries: List<MoodEntry>,
    range: InsightTimeRange,
    nowMillis: Long = System.currentTimeMillis(),
  ): List<MoodGraphPoint> {
    if (entries.isEmpty()) return emptyList()

    val filtered = when (range) {
      InsightTimeRange.DAY -> {
        val start = DateTimeUtils.startOfDayMillis(nowMillis)
        entries.filter { it.timestamp >= start && it.timestamp <= nowMillis }
      }
      InsightTimeRange.WEEK -> {
        val start = DateTimeUtils.daysAgoStartMillis(6, nowMillis)
        entries.filter { it.timestamp >= start }
      }
      InsightTimeRange.MONTH -> {
        val start = DateTimeUtils.daysAgoStartMillis(29, nowMillis)
        entries.filter { it.timestamp >= start }
      }
      InsightTimeRange.ALL -> entries
    }.sortedBy { it.timestamp }

    if (filtered.isEmpty()) return emptyList()

    if (range == InsightTimeRange.DAY) {
      return filtered.map { entry ->
        MoodGraphPoint(
          timestamp = entry.timestamp,
          value = entry.mood.toFloat(),
          label = DateTimeUtils.formatTime(entry.timestamp),
          entryCount = 1,
        )
      }
    }

    // Aggregate by day for Week / Month / All
    return filtered
      .groupBy { DateTimeUtils.dayKey(it.timestamp) }
      .toSortedMap()
      .map { (dayStart, dayEntries) ->
        MoodGraphPoint(
          timestamp = dayStart,
          value = dayEntries.map { it.mood }.average().toFloat(),
          label = DateTimeUtils.formatShortDate(dayStart),
          entryCount = dayEntries.size,
        )
      }
  }

  fun computeCalendarHeatmap(
    entries: List<MoodEntry>,
    weeks: Int = 12,
    nowMillis: Long = System.currentTimeMillis(),
  ): List<HeatmapDayCell> {
    val todayStart = DateTimeUtils.startOfDayMillis(nowMillis)
    val dayBuckets = entries.groupBy { DateTimeUtils.dayKey(it.timestamp) }

    val cal = Calendar.getInstance().apply {
      timeInMillis = todayStart
    }
    // Find Monday of current week (Mon = 0 .. Sun = 6)
    val calDow = cal.get(Calendar.DAY_OF_WEEK) // Sun=1, Mon=2 .. Sat=7
    val todayRow = (calDow + 5) % 7
    val currentMondayCal = Calendar.getInstance().apply {
      timeInMillis = todayStart
      add(Calendar.DAY_OF_YEAR, -todayRow)
      add(Calendar.WEEK_OF_YEAR, -(weeks - 1))
    }

    val cells = mutableListOf<HeatmapDayCell>()
    for (week in 0 until weeks) {
      for (row in 0 until 7) {
        val cellStart = DateTimeUtils.startOfDayMillis(currentMondayCal.timeInMillis)
        val bucket = dayBuckets[cellStart]
        val avg = bucket?.takeIf { it.isNotEmpty() }?.map { it.mood }?.average()?.toFloat()
        cells.add(
          HeatmapDayCell(
            dayStartMillis = cellStart,
            averageMood = avg,
            entryCount = bucket?.size ?: 0,
            weekColumn = week,
            dayOfWeekRow = row,
            isToday = cellStart == todayStart,
            isFuture = cellStart > todayStart,
          ),
        )
        currentMondayCal.add(Calendar.DAY_OF_YEAR, 1)
      }
    }
    return cells
  }

  fun computeTimeOfDayBuckets(
    entries: List<MoodEntry>,
    mode: TimeOfDayMode,
  ): List<TimeOfDayBucket> {
    if (mode == TimeOfDayMode.PART_OF_DAY) {
      val periods = listOf(
        Triple("Morning", "5a–12p") { h: Int -> h in 5..11 },
        Triple("Afternoon", "12p–5p") { h: Int -> h in 12..16 },
        Triple("Evening", "5p–9p") { h: Int -> h in 17..20 },
        Triple("Night", "9p–5a") { h: Int -> h >= 21 || h <= 4 },
      )
      return periods.map { (name, shortLabel, matcher) ->
        val matched = entries.filter { matcher(DateTimeUtils.hourOfDay(it.timestamp)) }
        TimeOfDayBucket(
          label = name,
          shortLabel = shortLabel,
          averageMood = matched.takeIf { it.isNotEmpty() }?.map { it.mood }?.average()?.toFloat(),
          entryCount = matched.size,
        )
      }
    } else {
      val byHour = entries.groupBy { DateTimeUtils.hourOfDay(it.timestamp) }
      return (0..23).map { hour ->
        val matched = byHour[hour].orEmpty()
        val hourLabel = when {
          hour == 0 -> "12a"
          hour < 12 -> "${hour}a"
          hour == 12 -> "12p"
          else -> "${hour - 12}p"
        }
        TimeOfDayBucket(
          label = hourLabel,
          shortLabel = if (hour % 6 == 0) hourLabel else "",
          averageMood = matched.takeIf { it.isNotEmpty() }?.map { it.mood }?.average()?.toFloat(),
          entryCount = matched.size,
        )
      }
    }
  }

  fun computeSuggestedTags(entries: List<MoodEntry>): List<String> {
    val freq = mutableMapOf<String, Int>()
    entries.forEach { entry ->
      entry.tags.forEach { tag ->
        val clean = tag.trim().lowercase()
        if (clean.isNotEmpty()) {
          freq[clean] = (freq[clean] ?: 0) + 1
        }
      }
    }
    val userTagsSorted = freq.entries
      .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
      .map { it.key }
    val extras = defaultSuggestedTags.filter { it !in freq }
    return userTagsSorted + extras
  }

  fun formatScore(value: Float?): String {
    if (value == null) return "—"
    val rounded = (value * 10f).roundToInt() / 10f
    return String.format(java.util.Locale.US, "%.1f", rounded)
  }
}
