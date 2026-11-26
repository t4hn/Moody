package com.example.viewmodel

import com.example.model.MoodEntry
import com.example.util.DateTimeUtils
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt

enum class TagTrendDirection {
  UP,
  DOWN,
  UNCHANGED,
}

data class MoodScoreDistributionBucket(
  val moodScore: Int,
  val count: Int,
  val percentage: Float,
)

data class TagFrequencyStat(
  val normalizedTag: String,
  val displayTag: String,
  val entryCount: Int,
  val percentageOfEntries: Float,
  val averageMood: Float?,
  val moodDistribution: List<MoodScoreDistributionBucket>,
  val hasSufficientMoodData: Boolean,
  val previousCount: Int? = null,
  val deltaCount: Int? = null,
  val percentChange: Float? = null,
  val trendDirection: TagTrendDirection? = null,
)

data class TagTrendItem(
  val normalizedTag: String,
  val displayTag: String,
  val currentCount: Int,
  val previousCount: Int,
  val deltaCount: Int,
  val percentChange: Float?,
  val direction: TagTrendDirection,
)

data class TagsAnalyticsSummary(
  val eligibleEntryCount: Int = 0,
  val taggedEntryCount: Int = 0,
  val distinctTagCount: Int = 0,
  val topTags: List<TagFrequencyStat> = emptyList(),
  val wordCloudTags: List<TagFrequencyStat> = emptyList(),
  val trendItems: List<TagTrendItem> = emptyList(),
  val supportsPeriodComparison: Boolean = true,
  val hasAnyTags: Boolean = false,
  val hasInsufficientTrendData: Boolean = false,
  val observation: String? = null,
)

internal object TagAnalyticsCalculator {

  private const val MAX_WORD_CLOUD_TAGS = 18
  private const val MAX_TREND_ITEMS = 6
  private const val MIN_MOOD_DISTRIBUTION_SAMPLES = 3
  private const val MIN_COMBINED_TREND_SAMPLES = 2

  fun normalizeTagKey(raw: String): String =
    raw.trim().trimStart('#').trim().lowercase(Locale.ROOT)

  fun cleanDisplayTag(raw: String): String =
    raw.trim().trimStart('#').trim()

  fun compute(
    allEntries: List<MoodEntry>,
    range: InsightTimeRange,
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
  ): TagsAnalyticsSummary {
    val eligibleCurrent = MoodAnalytics.filterEntriesByRange(
      entries = allEntries,
      range = range,
      nowMillis = nowMillis,
      timeZone = timeZone,
    ).filter { it.mood in 1..5 }

    val supportsComparison = range != InsightTimeRange.ALL
    val eligiblePrevious = if (supportsComparison) {
      filterEntriesInPrecedingRange(
        entries = allEntries,
        range = range,
        nowMillis = nowMillis,
        timeZone = timeZone,
      ).filter { it.mood in 1..5 }
    } else {
      emptyList()
    }

    if (eligibleCurrent.isEmpty()) {
      val prevOnly = extractPreviousTagAccumulators(eligiblePrevious)
      val trendItems = if (supportsComparison) {
        buildTrendItems(
          currentDisplayByKey = emptyMap(),
          currentCountByKey = emptyMap(),
          prevDisplayByKey = prevOnly.displayByKey,
          prevCountByKey = prevOnly.countByKey,
        )
      } else {
        emptyList()
      }
      return TagsAnalyticsSummary(
        eligibleEntryCount = 0,
        taggedEntryCount = 0,
        distinctTagCount = 0,
        topTags = emptyList(),
        wordCloudTags = emptyList(),
        trendItems = trendItems,
        supportsPeriodComparison = supportsComparison,
        hasAnyTags = false,
        hasInsufficientTrendData = false,
        observation = null,
      )
    }

    var taggedEntryCount = 0
    val entriesByTag = LinkedHashMap<String, MutableList<MoodEntry>>()
    val displayVariantsByTag = LinkedHashMap<String, LinkedHashMap<String, Int>>()

    eligibleCurrent.forEach { entry ->
      val uniqueEntryTags = extractUniqueTagsForEntry(entry)
      if (uniqueEntryTags.isNotEmpty()) {
        taggedEntryCount++
        uniqueEntryTags.forEach { (key, candidateDisplay) ->
          entriesByTag.getOrPut(key) { mutableListOf() }.add(entry)
          val variantCounts = displayVariantsByTag.getOrPut(key) { LinkedHashMap() }
          variantCounts[candidateDisplay] = (variantCounts[candidateDisplay] ?: 0) + 1
        }
      }
    }

    val prevAcc = extractPreviousTagAccumulators(eligiblePrevious)

    if (entriesByTag.isEmpty()) {
      val trendItems = if (supportsComparison) {
        buildTrendItems(
          currentDisplayByKey = emptyMap(),
          currentCountByKey = emptyMap(),
          prevDisplayByKey = prevAcc.displayByKey,
          prevCountByKey = prevAcc.countByKey,
        )
      } else {
        emptyList()
      }
      return TagsAnalyticsSummary(
        eligibleEntryCount = eligibleCurrent.size,
        taggedEntryCount = 0,
        distinctTagCount = 0,
        topTags = emptyList(),
        wordCloudTags = emptyList(),
        trendItems = trendItems,
        supportsPeriodComparison = supportsComparison,
        hasAnyTags = false,
        hasInsufficientTrendData = false,
        observation = null,
      )
    }

    val denominator = eligibleCurrent.size.toFloat()
    val currentDisplayByKey = LinkedHashMap<String, String>(entriesByTag.size)
    val currentCountByKey = LinkedHashMap<String, Int>(entriesByTag.size)

    val rankedStats = entriesByTag.entries.map { (key, matchingEntries) ->
      val displayTag = pickRepresentativeDisplay(
        variants = displayVariantsByTag[key].orEmpty(),
        fallback = key,
      )
      currentDisplayByKey[key] = displayTag
      currentCountByKey[key] = matchingEntries.size

      val count = matchingEntries.size
      val pctOfEntries = ((count.toFloat() / denominator) * 100f).coerceIn(0f, 100f)
      val avgMood = matchingEntries.map { it.mood }.average().toFloat()

      val moodCounts = IntArray(6)
      matchingEntries.forEach { entry ->
        if (entry.mood in 1..5) {
          moodCounts[entry.mood]++
        }
      }
      val distribution = (1..5).map { score ->
        val scoreCount = moodCounts[score]
        val scorePct = if (count > 0) (scoreCount.toFloat() / count.toFloat()) * 100f else 0f
        MoodScoreDistributionBucket(
          moodScore = score,
          count = scoreCount,
          percentage = scorePct,
        )
      }

      val prevCount = if (supportsComparison) prevAcc.countByKey[key] ?: 0 else null
      val delta = prevCount?.let { count - it }
      val pctChange = if (prevCount != null && prevCount > 0 && delta != null) {
        (delta.toFloat() / prevCount.toFloat()) * 100f
      } else {
        null
      }
      val direction = delta?.let {
        when {
          it > 0 -> TagTrendDirection.UP
          it < 0 -> TagTrendDirection.DOWN
          else -> TagTrendDirection.UNCHANGED
        }
      }

      TagFrequencyStat(
        normalizedTag = key,
        displayTag = displayTag,
        entryCount = count,
        percentageOfEntries = pctOfEntries,
        averageMood = avgMood,
        moodDistribution = distribution,
        hasSufficientMoodData = count >= MIN_MOOD_DISTRIBUTION_SAMPLES,
        previousCount = prevCount,
        deltaCount = delta,
        percentChange = pctChange,
        trendDirection = direction,
      )
    }.sortedWith(
      compareByDescending<TagFrequencyStat> { it.entryCount }
        .thenBy { it.normalizedTag },
    )

    val wordCloudTags = arrangeWordCloudTags(rankedStats.take(MAX_WORD_CLOUD_TAGS))

    val trendItems = if (supportsComparison) {
      buildTrendItems(
        currentDisplayByKey = currentDisplayByKey,
        currentCountByKey = currentCountByKey,
        prevDisplayByKey = prevAcc.displayByKey,
        prevCountByKey = prevAcc.countByKey,
      )
    } else {
      emptyList()
    }

    val observation = buildConciseObservation(
      topTags = rankedStats,
      taggedEntryCount = taggedEntryCount,
      eligibleEntryCount = eligibleCurrent.size,
    )

    return TagsAnalyticsSummary(
      eligibleEntryCount = eligibleCurrent.size,
      taggedEntryCount = taggedEntryCount,
      distinctTagCount = rankedStats.size,
      topTags = rankedStats,
      wordCloudTags = wordCloudTags,
      trendItems = trendItems,
      supportsPeriodComparison = supportsComparison,
      hasAnyTags = true,
      hasInsufficientTrendData = supportsComparison && trendItems.isEmpty(),
      observation = observation,
    )
  }

  private fun extractUniqueTagsForEntry(entry: MoodEntry): LinkedHashMap<String, String> {
    val unique = LinkedHashMap<String, String>()
    entry.tags.forEach { raw ->
      val key = normalizeTagKey(raw)
      if (key.isNotEmpty() && key !in unique) {
        val cleaned = cleanDisplayTag(raw)
        unique[key] = cleaned.ifEmpty { key }
      }
    }
    return unique
  }

  private data class PreviousTagAccumulators(
    val countByKey: Map<String, Int>,
    val displayByKey: Map<String, String>,
  )

  private fun extractPreviousTagAccumulators(entries: List<MoodEntry>): PreviousTagAccumulators {
    if (entries.isEmpty()) return PreviousTagAccumulators(emptyMap(), emptyMap())
    val countByKey = LinkedHashMap<String, Int>()
    val variantsByKey = LinkedHashMap<String, LinkedHashMap<String, Int>>()
    entries.forEach { entry ->
      val uniqueEntryTags = extractUniqueTagsForEntry(entry)
      uniqueEntryTags.forEach { (key, candidateDisplay) ->
        countByKey[key] = (countByKey[key] ?: 0) + 1
        val variants = variantsByKey.getOrPut(key) { LinkedHashMap() }
        variants[candidateDisplay] = (variants[candidateDisplay] ?: 0) + 1
      }
    }
    val displayByKey = variantsByKey.mapValues { (key, variants) ->
      pickRepresentativeDisplay(variants, key)
    }
    return PreviousTagAccumulators(countByKey = countByKey, displayByKey = displayByKey)
  }

  private fun pickRepresentativeDisplay(
    variants: Map<String, Int>,
    fallback: String,
  ): String {
    if (variants.isEmpty()) return fallback
    return variants.entries
      .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key == it.key.lowercase(Locale.ROOT) }.thenBy { it.key })
      .first()
      .key
  }

  private fun filterEntriesInPrecedingRange(
    entries: List<MoodEntry>,
    range: InsightTimeRange,
    nowMillis: Long,
    timeZone: TimeZone,
  ): List<MoodEntry> {
    if (entries.isEmpty() || range == InsightTimeRange.ALL) return emptyList()
    val (prevStart, prevEnd) = when (range) {
      InsightTimeRange.DAY -> {
        val yesterday = DateTimeUtils.daysAgoStartMillis(1, nowMillis, timeZone)
        yesterday to yesterday
      }
      InsightTimeRange.WEEK -> {
        DateTimeUtils.daysAgoStartMillis(13, nowMillis, timeZone) to
          DateTimeUtils.daysAgoStartMillis(7, nowMillis, timeZone)
      }
      InsightTimeRange.MONTH -> {
        DateTimeUtils.daysAgoStartMillis(59, nowMillis, timeZone) to
          DateTimeUtils.daysAgoStartMillis(30, nowMillis, timeZone)
      }
      InsightTimeRange.THREE_MONTHS -> {
        DateTimeUtils.daysAgoStartMillis(179, nowMillis, timeZone) to
          DateTimeUtils.daysAgoStartMillis(90, nowMillis, timeZone)
      }
      InsightTimeRange.ALL -> return emptyList()
    }
    return entries.filter { entry ->
      val dayStart = DateTimeUtils.startOfDayMillis(entry.timestamp, timeZone)
      dayStart in prevStart..prevEnd
    }
  }

  private fun buildTrendItems(
    currentDisplayByKey: Map<String, String>,
    currentCountByKey: Map<String, Int>,
    prevDisplayByKey: Map<String, String>,
    prevCountByKey: Map<String, Int>,
  ): List<TagTrendItem> {
    val allKeys = (currentCountByKey.keys + prevCountByKey.keys).distinct()
    return allKeys.mapNotNull { key ->
      val curr = currentCountByKey[key] ?: 0
      val prev = prevCountByKey[key] ?: 0
      if (curr + prev < MIN_COMBINED_TREND_SAMPLES) return@mapNotNull null

      val delta = curr - prev
      val pctChange = if (prev > 0) (delta.toFloat() / prev.toFloat()) * 100f else null
      val direction = when {
        delta > 0 -> TagTrendDirection.UP
        delta < 0 -> TagTrendDirection.DOWN
        else -> TagTrendDirection.UNCHANGED
      }
      val display = currentDisplayByKey[key] ?: prevDisplayByKey[key] ?: key
      TagTrendItem(
        normalizedTag = key,
        displayTag = display,
        currentCount = curr,
        previousCount = prev,
        deltaCount = delta,
        percentChange = pctChange,
        direction = direction,
      )
    }.sortedWith(
      compareByDescending<TagTrendItem> { abs(it.deltaCount) }
        .thenByDescending { maxOf(it.currentCount, it.previousCount) }
        .thenByDescending { it.currentCount }
        .thenBy { it.normalizedTag },
    ).take(MAX_TREND_ITEMS)
  }

  private fun arrangeWordCloudTags(descending: List<TagFrequencyStat>): List<TagFrequencyStat> {
    if (descending.size <= 2) return descending
    // Center-weighted deterministic placement: largest tags gravitate to the middle of the cloud
    val deque = ArrayDeque<TagFrequencyStat>(descending.size)
    descending.forEachIndexed { index, item ->
      if (index % 2 == 0) {
        deque.addLast(item)
      } else {
        deque.addFirst(item)
      }
    }
    return deque.toList()
  }

  private fun buildConciseObservation(
    topTags: List<TagFrequencyStat>,
    taggedEntryCount: Int,
    eligibleEntryCount: Int,
  ): String? {
    if (eligibleEntryCount < 3 || topTags.isEmpty()) return null
    val top = topTags.first()
    if (top.entryCount < 3) return null
    val sharePct = top.percentageOfEntries.roundToInt()
    return "#${top.displayTag} appears in $sharePct% of recorded entries (${top.entryCount} of $eligibleEntryCount) in this period."
  }
}
