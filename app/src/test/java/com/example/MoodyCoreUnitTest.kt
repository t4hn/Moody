package com.example

import com.example.model.MoodEntry
import com.example.viewmodel.InsightTimeRange
import com.example.viewmodel.MoodAnalytics
import com.example.viewmodel.TimeOfDayMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodyCoreUnitTest {

  @Test
  fun `analytics calculates streak average and buckets accurately`() {
    val now = 1_700_050_000_000L
    val oneDay = 86_400_000L
    val entries = listOf(
      MoodEntry(id = "a", mood = 4, timestamp = now - 10_000L, tags = listOf("calm")),
      MoodEntry(id = "b", mood = 2, timestamp = now - oneDay, tags = listOf("tired", "calm")),
      MoodEntry(id = "c", mood = 5, timestamp = now - 2 * oneDay, tags = listOf("social")),
    )

    val stats = MoodAnalytics.computeStats(entries, now)
    assertEquals(3, stats.totalEntries)
    assertEquals(3, stats.currentStreakDays)
    assertEquals(3, stats.distinctTagCount)
    assertEquals("3.7", MoodAnalytics.formatScore(stats.averageMood))

    val weekPoints = MoodAnalytics.computeLineGraphPoints(entries, InsightTimeRange.WEEK, now)
    assertEquals(3, weekPoints.size)

    val heatmap = MoodAnalytics.computeCalendarHeatmap(entries, weeks = 12, nowMillis = now)
    assertEquals(84, heatmap.size)
    assertTrue(heatmap.any { it.entryCount == 1 })

    val partBuckets = MoodAnalytics.computeTimeOfDayBuckets(entries, TimeOfDayMode.PART_OF_DAY)
    assertEquals(4, partBuckets.size)

    val hourBuckets = MoodAnalytics.computeTimeOfDayBuckets(entries, TimeOfDayMode.HOURLY)
    assertEquals(24, hourBuckets.size)

    val suggestions = MoodAnalytics.computeSuggestedTags(entries)
    assertEquals("calm", suggestions.first())
  }
}
