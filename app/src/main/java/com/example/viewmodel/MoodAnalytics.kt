package com.example.viewmodel

import com.example.model.DailyContext
import com.example.model.MoodEntry
import com.example.util.DateTimeUtils
import java.util.Calendar
import java.util.TimeZone
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

data class SleepQualityPoint(
  val dayStartMillis: Long,
  val label: String,
  val sleepQuality: Int?,
)

data class WeekdaySleepAverage(
  val weekdayIndex: Int,
  val weekdayLabel: String,
  val shortLabel: String,
  val averageQuality: Float?,
  val nightCount: Int,
)

data class SleepAnalyticsSummary(
  val averageQuality: Float? = null,
  val qualityNightCount: Int = 0,
  val averageDurationMinutes: Int? = null,
  val durationNightCount: Int = 0,
  val qualityHistory: List<SleepQualityPoint> = emptyList(),
  val weekdayAverages: List<WeekdaySleepAverage> = emptyList(),
  val hasAnySleepData: Boolean = false,
)

enum class WellbeingMetric(
  val label: String,
  val shortTag: String,
  val invertSemanticScale: Boolean,
) {
  ENERGY(label = "Energy", shortTag = "E", invertSemanticScale = false),
  STRESS(label = "Stress", shortTag = "S", invertSemanticScale = true),
  ANXIETY(label = "Anxiety", shortTag = "A", invertSemanticScale = true),
}

data class WellbeingHistoryPoint(
  val dayStartMillis: Long,
  val label: String,
  val averageValue: Float?,
  val entryCount: Int = 0,
)

data class WellbeingMetricSummary(
  val metric: WellbeingMetric,
  val average: Float? = null,
  val entryCount: Int = 0,
  val history: List<WellbeingHistoryPoint> = emptyList(),
) {
  val hasData: Boolean
    get() = entryCount > 0 && average != null
}

data class WeekdayWellbeingAverage(
  val weekdayIndex: Int, // 0 = Monday .. 6 = Sunday
  val weekdayLabel: String,
  val shortLabel: String,
  val energyAverage: Float? = null,
  val energyDayCount: Int = 0,
  val stressAverage: Float? = null,
  val stressDayCount: Int = 0,
  val anxietyAverage: Float? = null,
  val anxietyDayCount: Int = 0,
) {
  val hasAnyData: Boolean
    get() = energyAverage != null || stressAverage != null || anxietyAverage != null
}

data class WellbeingAnalyticsSummary(
  val energy: WellbeingMetricSummary = WellbeingMetricSummary(WellbeingMetric.ENERGY),
  val stress: WellbeingMetricSummary = WellbeingMetricSummary(WellbeingMetric.STRESS),
  val anxiety: WellbeingMetricSummary = WellbeingMetricSummary(WellbeingMetric.ANXIETY),
  val weekdayAverages: List<WeekdayWellbeingAverage> = emptyList(),
) {
  val hasAnyWellbeingData: Boolean
    get() = energy.hasData || stress.hasData || anxiety.hasData
}

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

  fun formatSleepDuration(minutes: Int?): String {
    if (minutes == null || minutes < 0) return "—"
    val hours = minutes / 60
    val mins = minutes % 60
    return "${hours}h ${mins}m"
  }

  private data class ParsedDailySleep(
    val dayStartMillis: Long,
    val sleepDurationMinutes: Int?,
    val sleepQuality: Int?,
    val updatedAt: Long,
  )

  private val weekdayDefinitions = listOf(
    "Monday" to "Mon",
    "Tuesday" to "Tue",
    "Wednesday" to "Wed",
    "Thursday" to "Thu",
    "Friday" to "Fri",
    "Saturday" to "Sat",
    "Sunday" to "Sun",
  )

  fun computeSleepAnalytics(
    dailyContexts: Collection<DailyContext>,
    range: InsightTimeRange,
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
  ): SleepAnalyticsSummary {
    val todayStart = DateTimeUtils.startOfDayMillis(nowMillis, timeZone)

    // Normalize and deduplicate to at most one record per local calendar day
    val deduplicatedByDay = LinkedHashMap<Long, ParsedDailySleep>()
    dailyContexts.forEach { ctx ->
      val dayStart = DateTimeUtils.parseDayKeyStartMillisOrNull(ctx.dayKey, timeZone) ?: return@forEach
      val quality = ctx.sleepQuality?.takeIf { it in 1..5 }
      val duration = ctx.sleepDurationMinutes?.takeIf { it >= 0 }
      if (quality == null && duration == null) return@forEach

      val existing = deduplicatedByDay[dayStart]
      if (existing == null || ctx.updatedAt >= existing.updatedAt) {
        deduplicatedByDay[dayStart] = ParsedDailySleep(
          dayStartMillis = dayStart,
          sleepDurationMinutes = duration,
          sleepQuality = quality,
          updatedAt = ctx.updatedAt,
        )
      }
    }

    val filtered = when (range) {
      InsightTimeRange.DAY -> {
        deduplicatedByDay.values.filter { it.dayStartMillis == todayStart }
      }
      InsightTimeRange.WEEK -> {
        val start = DateTimeUtils.daysAgoStartMillis(6, nowMillis, timeZone)
        deduplicatedByDay.values.filter { it.dayStartMillis in start..todayStart }
      }
      InsightTimeRange.MONTH -> {
        val start = DateTimeUtils.daysAgoStartMillis(29, nowMillis, timeZone)
        deduplicatedByDay.values.filter { it.dayStartMillis in start..todayStart }
      }
      InsightTimeRange.ALL -> {
        deduplicatedByDay.values.toList()
      }
    }.sortedBy { it.dayStartMillis }

    val qualityValues = filtered.mapNotNull { it.sleepQuality }
    val qualityNightCount = qualityValues.size
    val averageQuality = if (qualityNightCount > 0) qualityValues.average().toFloat() else null

    val durationValues = filtered.mapNotNull { it.sleepDurationMinutes }
    val durationNightCount = durationValues.size
    val averageDurationMinutes = if (durationNightCount > 0) durationValues.average().roundToInt() else null

    val recordedQualityByDay = filtered
      .filter { it.sleepQuality != null }
      .associateBy { it.dayStartMillis }

    val qualityHistory = if (recordedQualityByDay.isEmpty()) {
      emptyList()
    } else {
      val (spanStart, spanEnd) = when (range) {
        InsightTimeRange.DAY -> todayStart to todayStart
        InsightTimeRange.WEEK -> DateTimeUtils.daysAgoStartMillis(6, nowMillis, timeZone) to todayStart
        InsightTimeRange.MONTH -> DateTimeUtils.daysAgoStartMillis(29, nowMillis, timeZone) to todayStart
        InsightTimeRange.ALL -> recordedQualityByDay.keys.min() to recordedQualityByDay.keys.max()
      }

      val approxDays = ((spanEnd - spanStart) / 86_400_000L).coerceAtLeast(0L)
      if (approxDays > 1095L) {
        recordedQualityByDay.keys.sorted().map { dayStart ->
          SleepQualityPoint(
            dayStartMillis = dayStart,
            label = DateTimeUtils.formatShortDate(dayStart, timeZone),
            sleepQuality = recordedQualityByDay[dayStart]?.sleepQuality,
          )
        }
      } else {
        buildList {
          var cursor = spanStart
          var guard = 0
          while (cursor <= spanEnd && guard < 1200) {
            add(
              SleepQualityPoint(
                dayStartMillis = cursor,
                label = DateTimeUtils.formatShortDate(cursor, timeZone),
                sleepQuality = recordedQualityByDay[cursor]?.sleepQuality,
              ),
            )
            cursor = DateTimeUtils.nextDayStartMillis(cursor, timeZone)
            guard++
          }
        }
      }
    }

    val groupedByWeekday = recordedQualityByDay.values.groupBy {
      DateTimeUtils.weekdayMondayFirstIndex(it.dayStartMillis, timeZone)
    }

    val weekdayAverages = weekdayDefinitions.mapIndexed { idx, (fullName, shortName) ->
      val values = groupedByWeekday[idx].orEmpty().mapNotNull { it.sleepQuality }
      WeekdaySleepAverage(
        weekdayIndex = idx,
        weekdayLabel = fullName,
        shortLabel = shortName,
        averageQuality = if (values.isNotEmpty()) values.average().toFloat() else null,
        nightCount = values.size,
      )
    }

    return SleepAnalyticsSummary(
      averageQuality = averageQuality,
      qualityNightCount = qualityNightCount,
      averageDurationMinutes = averageDurationMinutes,
      durationNightCount = durationNightCount,
      qualityHistory = qualityHistory,
      weekdayAverages = weekdayAverages,
      hasAnySleepData = qualityNightCount > 0 || durationNightCount > 0,
    )
  }

  fun computeWellbeingAnalytics(
    entries: List<MoodEntry>,
    range: InsightTimeRange,
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
  ): WellbeingAnalyticsSummary {
    val todayStart = DateTimeUtils.startOfDayMillis(nowMillis, timeZone)
    val weekStart = DateTimeUtils.daysAgoStartMillis(6, nowMillis, timeZone)
    val monthStart = DateTimeUtils.daysAgoStartMillis(29, nowMillis, timeZone)

    val filteredWithDay = entries.mapNotNull { entry ->
      val dayStart = DateTimeUtils.startOfDayMillis(entry.timestamp, timeZone)
      val included = when (range) {
        InsightTimeRange.DAY -> dayStart == todayStart
        InsightTimeRange.WEEK -> dayStart in weekStart..todayStart
        InsightTimeRange.MONTH -> dayStart in monthStart..todayStart
        InsightTimeRange.ALL -> true
      }
      if (included) dayStart to entry else null
    }

    fun computeMetric(
      metric: WellbeingMetric,
      selector: (MoodEntry) -> Int?,
    ): Pair<WellbeingMetricSummary, Map<Long, Float>> {
      val validSamples = filteredWithDay.mapNotNull { (dayStart, entry) ->
        val score = selector(entry)?.takeIf { it in 1..5 } ?: return@mapNotNull null
        dayStart to score
      }

      if (validSamples.isEmpty()) {
        return WellbeingMetricSummary(metric = metric) to emptyMap()
      }

      val overallAverage = validSamples.map { it.second }.average().toFloat()
      val groupedByDay = validSamples.groupBy(
        keySelector = { it.first },
        valueTransform = { it.second },
      )
      val dailyAverageByDay = groupedByDay.mapValues { (_, scores) ->
        scores.average().toFloat()
      }

      val (spanStart, spanEnd) = when (range) {
        InsightTimeRange.DAY -> todayStart to todayStart
        InsightTimeRange.WEEK -> weekStart to todayStart
        InsightTimeRange.MONTH -> monthStart to todayStart
        InsightTimeRange.ALL -> dailyAverageByDay.keys.min() to dailyAverageByDay.keys.max()
      }

      val approxDays = ((spanEnd - spanStart) / 86_400_000L).coerceAtLeast(0L)
      val history = if (approxDays > 1095L) {
        dailyAverageByDay.keys.sorted().map { dayStart ->
          WellbeingHistoryPoint(
            dayStartMillis = dayStart,
            label = DateTimeUtils.formatShortDate(dayStart, timeZone),
            averageValue = dailyAverageByDay[dayStart],
            entryCount = groupedByDay[dayStart]?.size ?: 0,
          )
        }
      } else {
        buildList {
          var cursor = spanStart
          var guard = 0
          while (cursor <= spanEnd && guard < 1200) {
            add(
              WellbeingHistoryPoint(
                dayStartMillis = cursor,
                label = DateTimeUtils.formatShortDate(cursor, timeZone),
                averageValue = dailyAverageByDay[cursor],
                entryCount = groupedByDay[cursor]?.size ?: 0,
              ),
            )
            cursor = DateTimeUtils.nextDayStartMillis(cursor, timeZone)
            guard++
          }
        }
      }

      return WellbeingMetricSummary(
        metric = metric,
        average = overallAverage,
        entryCount = validSamples.size,
        history = history,
      ) to dailyAverageByDay
    }

    val (energySummary, energyByDay) = computeMetric(WellbeingMetric.ENERGY) { it.energyLevel }
    val (stressSummary, stressByDay) = computeMetric(WellbeingMetric.STRESS) { it.stressLevel }
    val (anxietySummary, anxietyByDay) = computeMetric(WellbeingMetric.ANXIETY) { it.anxietyLevel }

    fun groupDailyByWeekday(dailyMap: Map<Long, Float>): Map<Int, List<Float>> {
      val buckets = HashMap<Int, MutableList<Float>>(7)
      dailyMap.forEach { (dayStart, dailyAvg) ->
        val dow = DateTimeUtils.weekdayMondayFirstIndex(dayStart, timeZone)
        buckets.getOrPut(dow) { mutableListOf() }.add(dailyAvg)
      }
      return buckets
    }

    val energyByWeekday = groupDailyByWeekday(energyByDay)
    val stressByWeekday = groupDailyByWeekday(stressByDay)
    val anxietyByWeekday = groupDailyByWeekday(anxietyByDay)

    val weekdayAverages = weekdayDefinitions.mapIndexed { idx, (fullName, shortName) ->
      val energyDays = energyByWeekday[idx].orEmpty()
      val stressDays = stressByWeekday[idx].orEmpty()
      val anxietyDays = anxietyByWeekday[idx].orEmpty()
      WeekdayWellbeingAverage(
        weekdayIndex = idx,
        weekdayLabel = fullName,
        shortLabel = shortName,
        energyAverage = if (energyDays.isNotEmpty()) energyDays.average().toFloat() else null,
        energyDayCount = energyDays.size,
        stressAverage = if (stressDays.isNotEmpty()) stressDays.average().toFloat() else null,
        stressDayCount = stressDays.size,
        anxietyAverage = if (anxietyDays.isNotEmpty()) anxietyDays.average().toFloat() else null,
        anxietyDayCount = anxietyDays.size,
      )
    }

    return WellbeingAnalyticsSummary(
      energy = energySummary,
      stress = stressSummary,
      anxiety = anxietySummary,
      weekdayAverages = weekdayAverages,
    )
  }
}
