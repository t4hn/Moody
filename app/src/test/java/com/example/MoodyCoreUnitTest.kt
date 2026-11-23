package com.example

import androidx.compose.ui.graphics.toArgb
import com.example.data.ImportValidationResult
import com.example.data.MoodBackupSerializer
import com.example.model.DailyContext
import com.example.model.MoodEntry
import com.example.model.MoodLevel
import com.example.model.PhysicalActivityLevel
import com.example.ui.theme.AppearanceMode
import com.example.ui.theme.MoodColor1
import com.example.ui.theme.MoodColor2
import com.example.ui.theme.MoodColor3
import com.example.ui.theme.MoodColor4
import com.example.ui.theme.MoodColor5
import com.example.ui.theme.MoodyPalette
import com.example.ui.theme.resolveMoodyColors
import com.example.util.DateTimeUtils
import com.example.viewmodel.InsightTimeRange
import com.example.viewmodel.MoodAnalytics
import com.example.viewmodel.TimeOfDayMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

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

  @Test
  fun `csv export and import roundtrips extended contextual columns and legacy 4-column csv`() {
    val ts = 1_763_885_644_000L
    val entries = listOf(
      MoodEntry(
        id = "c1",
        mood = 4,
        timestamp = ts,
        note = "Line with, comma and \"quotes\"",
        tags = listOf("calm", "coffee"),
        sleepDurationMinutes = 435,
        sleepQuality = 4,
        energyLevel = 3,
        stressLevel = 2,
        anxietyLevel = 1,
        physicalActivity = PhysicalActivityLevel.LIGHT,
      ),
      MoodEntry(
        id = "c2",
        mood = 2,
        timestamp = ts - 86_400_000L,
        note = "Minimal entry",
      ),
    )

    val csv = MoodBackupSerializer.exportCsv(entries)
    assertTrue(csv.startsWith("timestamp,mood,tags,note,sleep_duration_minutes,sleep_quality,energy_level,stress_level,anxiety_level,physical_activity"))
    val parsed = MoodBackupSerializer.parseAndValidateCsv(csv)
    assertTrue(parsed is ImportValidationResult.Valid)
    val valid = parsed as ImportValidationResult.Valid
    assertEquals(2, valid.entries.size)
    assertEquals(1, valid.dailyContexts.size)

    val first = valid.entries.first()
    assertEquals(4, first.mood)
    assertEquals("Line with, comma and \"quotes\"", first.note)
    assertEquals(listOf("calm", "coffee"), first.tags)
    assertEquals(435, first.sleepDurationMinutes)
    assertEquals(4, first.sleepQuality)
    assertEquals(3, first.energyLevel)
    assertEquals(2, first.stressLevel)
    assertEquals(1, first.anxietyLevel)
    assertEquals(PhysicalActivityLevel.LIGHT, first.physicalActivity)

    val second = valid.entries.last()
    assertEquals(2, second.mood)
    assertNull(second.sleepDurationMinutes)
    assertNull(second.physicalActivity)

    // Legacy 4-column CSV compatibility
    val legacyCsv = "timestamp,mood,tags,note\n2025-11-20T09:00:00Z,5,grateful; walk,Great walk\n"
    val legacyResult = MoodBackupSerializer.parseAndValidateCsv(legacyCsv)
    assertTrue(legacyResult is ImportValidationResult.Valid)
    val legacyEntry = (legacyResult as ImportValidationResult.Valid).entries.single()
    assertEquals(5, legacyEntry.mood)
    assertEquals(listOf("grateful", "walk"), legacyEntry.tags)
    assertEquals("Great walk", legacyEntry.note)
    assertNull(legacyEntry.energyLevel)
  }

  @Test
  fun `csv import rejects invalid inputs safely`() {
    assertTrue(MoodBackupSerializer.parseAndValidateCsv("") is ImportValidationResult.Invalid)
    assertTrue(MoodBackupSerializer.parseAndValidateCsv("timestamp,mood\n") is ImportValidationResult.Invalid)
    assertTrue(MoodBackupSerializer.parseAndValidateCsv("foo,bar\n1,2\n") is ImportValidationResult.Invalid)
    assertTrue(MoodBackupSerializer.parseAndValidateCsv("timestamp,mood\n2025-11-20T09:00:00Z,9\n") is ImportValidationResult.Invalid)
  }

  @Test
  fun `contextual model helpers and physical activity parsing handle missing values safely`() {
    val empty = MoodEntry(mood = 3, timestamp = 1_700_050_000_000L)
    assertFalse(empty.hasAnyContext)

    val withActivity = empty.copy(physicalActivity = PhysicalActivityLevel.HEAVY)
    assertTrue(withActivity.hasAnyContext)

    assertEquals(PhysicalActivityLevel.NONE, PhysicalActivityLevel.fromNameOrNull("none"))
    assertEquals(PhysicalActivityLevel.MODERATE, PhysicalActivityLevel.fromNameOrNull("MODERATE"))
    assertNull(PhysicalActivityLevel.fromNameOrNull("invalid"))
    assertNull(PhysicalActivityLevel.fromNameOrNull(null))

    val emptyDaily = DailyContext(dayKey = "2026-11-23")
    assertFalse(emptyDaily.hasAnyData)
    assertTrue(emptyDaily.copy(sleepQuality = 4).hasAnyData)
  }

  @Test
  fun `mood colors stay constant and all theme palettes resolve with distinct foreground and background`() {
    val expectedMoodColors = listOf(MoodColor1, MoodColor2, MoodColor3, MoodColor4, MoodColor5)
    assertEquals(0xFFE5484D.toInt(), MoodColor1.toArgb())
    assertEquals(0xFFE88A36.toInt(), MoodColor2.toArgb())
    assertEquals(0xFFE4C34B.toInt(), MoodColor3.toArgb())
    assertEquals(0xFF91BC52.toInt(), MoodColor4.toArgb())
    assertEquals(0xFF28A66A.toInt(), MoodColor5.toArgb())
    assertEquals(listOf(1, 2, 3, 4, 5), MoodLevel.entries.map { it.score })
    assertEquals(
      listOf("Very poor mood", "Poor mood", "Neutral / middle", "Good mood", "Very good mood"),
      MoodLevel.entries.map { it.scaleDescription },
    )

    MoodyPalette.entries.forEach { palette ->
      listOf(true, false).forEach { isDark ->
        val colors = resolveMoodyColors(palette, isDark)
        assertEquals(isDark, colors.isDark)
        assertTrue(colors.background != colors.textPrimary)
        assertTrue(colors.cardSurface != colors.textPrimary)
        assertEquals(expectedMoodColors, MoodLevel.entries.map { it.color })
      }
    }
    assertEquals(AppearanceMode.DARK, AppearanceMode.fromNameOrDefault("UNKNOWN"))
    assertEquals(MoodyPalette.TEAL_OBSIDIAN, MoodyPalette.fromNameOrDefault("UNKNOWN"))
    assertEquals(3, AppearanceMode.entries.size)
    assertEquals(4, MoodyPalette.entries.size)
  }

  @Test
  fun `csv import preserves daily context without blank overwrite and sanitizes invalid optional fields`() {
    val csv = """
      timestamp,mood,tags,note,sleep_duration_minutes,sleep_quality,energy_level,stress_level,anxiety_level,physical_activity
      2026-11-23T09:00:00Z,4,calm,Morning,450,4,5,2,1,MODERATE
      2026-11-23T15:00:00Z,3,focused,Afternoon,,,9,0,-2,INVALID_ACTIVITY
    """.trimIndent()

    val result = MoodBackupSerializer.parseAndValidateCsv(csv)
    assertTrue(result is ImportValidationResult.Valid)
    val valid = result as ImportValidationResult.Valid
    assertEquals(2, valid.entries.size)
    assertEquals(1, valid.dailyContexts.size)
    assertEquals(450, valid.dailyContexts.single().sleepDurationMinutes)
    assertEquals(4, valid.dailyContexts.single().sleepQuality)

    val second = valid.entries[1]
    assertEquals(3, second.mood)
    assertNull(second.energyLevel)
    assertNull(second.stressLevel)
    assertNull(second.anxietyLevel)
    assertNull(second.physicalActivity)
  }

  @Test
  fun `iso date formatting and parsing roundtrip works on jvm`() {
    val ts = 1_763_885_644_000L
    val iso = DateTimeUtils.formatIsoUtc(ts)
    val parsed = DateTimeUtils.parseIsoUtcOrNull(iso)
    assertEquals(ts, parsed)
  }

  @Test
  fun `sleep analytics computes averages, duration formatting, and excludes missing values`() {
    val tz = TimeZone.getTimeZone("UTC")
    // 2026-11-23 is Monday
    val now = DateTimeUtils.parseDayKeyStartMillisOrNull("2026-11-23", tz)!! + 12 * 3_600_000L

    val contexts = listOf(
      // Mon Nov 23: quality=4, duration=435 (7h 15m)
      DailyContext(dayKey = "2026-11-23", sleepDurationMinutes = 435, sleepQuality = 4),
      // Sun Nov 22: quality=2, duration=null (missing duration must NOT count as 0)
      DailyContext(dayKey = "2026-11-22", sleepDurationMinutes = null, sleepQuality = 2),
      // Sat Nov 21: quality=null, duration=480 (missing quality must NOT count as 0)
      DailyContext(dayKey = "2026-11-21", sleepDurationMinutes = 480, sleepQuality = null),
      // Fri Nov 20: quality=5, duration=429 (7h 9m)
      DailyContext(dayKey = "2026-11-20", sleepDurationMinutes = 429, sleepQuality = 5),
    )

    val summary = MoodAnalytics.computeSleepAnalytics(contexts, InsightTimeRange.WEEK, now, tz)

    assertTrue(summary.hasAnySleepData)
    // Quality average: (4 + 2 + 5) / 3 = 3.6666 -> "3.7"
    assertEquals(3, summary.qualityNightCount)
    assertEquals("3.7", MoodAnalytics.formatScore(summary.averageQuality))

    // Duration average: (435 + 480 + 429) / 3 = 448 -> "7h 28m"
    assertEquals(3, summary.durationNightCount)
    assertEquals(448, summary.averageDurationMinutes)
    assertEquals("7h 28m", MoodAnalytics.formatSleepDuration(summary.averageDurationMinutes))
    assertEquals("—", MoodAnalytics.formatSleepDuration(null))

    // Week history has 7 consecutive days (Nov 17 .. Nov 23) without fabricating missing days
    assertEquals(7, summary.qualityHistory.size)
    assertNull(summary.qualityHistory[0].sleepQuality) // Tue Nov 17
    assertEquals(5, summary.qualityHistory[3].sleepQuality) // Fri Nov 20
    assertNull(summary.qualityHistory[4].sleepQuality) // Sat Nov 21 (duration-only, missing quality)
    assertEquals(2, summary.qualityHistory[5].sleepQuality) // Sun Nov 22
    assertEquals(4, summary.qualityHistory[6].sleepQuality) // Mon Nov 23
  }

  @Test
  fun `sleep analytics deduplicates multiple mood entries or duplicate day records per calendar day`() {
    val tz = TimeZone.getTimeZone("UTC")
    val now = DateTimeUtils.parseDayKeyStartMillisOrNull("2026-11-23", tz)!! + 10 * 3_600_000L

    // Simulate multiple mood entries referencing the same day's DailyContext map
    val dailyMap = linkedMapOf(
      "2026-11-23" to DailyContext("2026-11-23", sleepDurationMinutes = 420, sleepQuality = 4, updatedAt = 100L),
      "2026-11-22" to DailyContext("2026-11-22", sleepDurationMinutes = 360, sleepQuality = 2, updatedAt = 100L),
    )
    // Even if someone passes duplicate dayKey entries in a collection, only the latest per day counts once
    val withDuplicate = dailyMap.values.toList() + listOf(
      DailyContext("2026-11-23", sleepDurationMinutes = 450, sleepQuality = 5, updatedAt = 200L),
    )

    val summary = MoodAnalytics.computeSleepAnalytics(withDuplicate, InsightTimeRange.WEEK, now, tz)
    assertEquals(2, summary.qualityNightCount)
    assertEquals(2, summary.durationNightCount)
    assertEquals(3.5f, summary.averageQuality!!, 0.001f) // (5 + 2) / 2
    assertEquals(405, summary.averageDurationMinutes) // (450 + 360) / 2
  }

  @Test
  fun `sleep analytics groups recorded quality by Monday-to-Sunday weekdays and leaves empty weekdays null`() {
    val tz = TimeZone.getTimeZone("America/Los_Angeles")
    // 2026-11-23 is Monday
    val now = DateTimeUtils.parseDayKeyStartMillisOrNull("2026-11-23", tz)!! + 12 * 3_600_000L

    val contexts = listOf(
      DailyContext("2026-11-23", sleepQuality = 5), // Monday
      DailyContext("2026-11-16", sleepQuality = 3), // Monday (previous week -> included in MONTH/ALL)
      DailyContext("2026-11-19", sleepQuality = 2), // Thursday
      DailyContext("2026-11-22", sleepQuality = 4), // Sunday
    )

    val monthSummary = MoodAnalytics.computeSleepAnalytics(contexts, InsightTimeRange.MONTH, now, tz)
    assertEquals(7, monthSummary.weekdayAverages.size)
    assertEquals(
      listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"),
      monthSummary.weekdayAverages.map { it.weekdayLabel },
    )

    val monday = monthSummary.weekdayAverages[0]
    assertEquals(2, monday.nightCount)
    assertEquals(4.0f, monday.averageQuality!!, 0.001f)

    val tuesday = monthSummary.weekdayAverages[1]
    assertEquals(0, tuesday.nightCount)
    assertNull(tuesday.averageQuality)

    val thursday = monthSummary.weekdayAverages[3]
    assertEquals(1, thursday.nightCount)
    assertEquals(2.0f, thursday.averageQuality!!, 0.001f)

    val sunday = monthSummary.weekdayAverages[6]
    assertEquals(1, sunday.nightCount)
    assertEquals(4.0f, sunday.averageQuality!!, 0.001f)
  }

  @Test
  fun `sleep analytics respects Week Month All Day date filters and empty periods`() {
    val tz = TimeZone.getTimeZone("UTC")
    val now = DateTimeUtils.parseDayKeyStartMillisOrNull("2026-11-23", tz)!! + 12 * 3_600_000L

    val contexts = listOf(
      DailyContext("2026-11-22", sleepDurationMinutes = 420, sleepQuality = 4), // 1 day ago (Week + Month + All)
      DailyContext("2026-11-10", sleepDurationMinutes = 360, sleepQuality = 2), // 13 days ago (Month + All)
      DailyContext("2026-10-01", sleepDurationMinutes = 480, sleepQuality = 5), // 53 days ago (All only)
    )

    val daySummary = MoodAnalytics.computeSleepAnalytics(contexts, InsightTimeRange.DAY, now, tz)
    assertFalse(daySummary.hasAnySleepData)
    assertEquals(0, daySummary.qualityNightCount)
    assertNull(daySummary.averageQuality)
    assertNull(daySummary.averageDurationMinutes)
    assertTrue(daySummary.qualityHistory.isEmpty())

    val weekSummary = MoodAnalytics.computeSleepAnalytics(contexts, InsightTimeRange.WEEK, now, tz)
    assertEquals(1, weekSummary.qualityNightCount)
    assertEquals(4.0f, weekSummary.averageQuality!!, 0.001f)
    assertEquals(420, weekSummary.averageDurationMinutes)
    assertEquals(7, weekSummary.qualityHistory.size)

    val monthSummary = MoodAnalytics.computeSleepAnalytics(contexts, InsightTimeRange.MONTH, now, tz)
    assertEquals(2, monthSummary.qualityNightCount)
    assertEquals(3.0f, monthSummary.averageQuality!!, 0.001f)
    assertEquals(390, monthSummary.averageDurationMinutes)
    assertEquals(30, monthSummary.qualityHistory.size)

    val allSummary = MoodAnalytics.computeSleepAnalytics(contexts, InsightTimeRange.ALL, now, tz)
    assertEquals(3, allSummary.qualityNightCount)
    assertEquals(3.6666f, allSummary.averageQuality!!, 0.01f)
    assertEquals(420, allSummary.averageDurationMinutes)
    assertEquals(53, allSummary.qualityHistory.size) // Oct 1 .. Nov 22 inclusive = 53 days

    val emptySummary = MoodAnalytics.computeSleepAnalytics(emptyList(), InsightTimeRange.ALL, now, tz)
    assertFalse(emptySummary.hasAnySleepData)
    assertNull(emptySummary.averageQuality)
    assertNull(emptySummary.averageDurationMinutes)
    assertTrue(emptySummary.qualityHistory.isEmpty())
    assertTrue(emptySummary.weekdayAverages.all { it.nightCount == 0 && it.averageQuality == null })

    // Invalid calendar dates are ignored safely
    assertNull(DateTimeUtils.parseDayKeyStartMillisOrNull("2026-02-30", tz))
    assertNull(DateTimeUtils.parseDayKeyStartMillisOrNull("not-a-date", tz))
  }
}
