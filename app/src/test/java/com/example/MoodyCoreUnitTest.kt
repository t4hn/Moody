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

  @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
  @Test
  fun `entry sheet state guard blocks accidental hidden transition during ime open or closing`() {
    assertFalse(
      com.example.ui.components.shouldConfirmEntrySheetValueChange(
        targetValue = androidx.compose.material3.SheetValue.Hidden,
        isImeVisible = true,
        isImeClosing = false,
        isUserDraggingSheet = false,
        isExplicitDismissRequested = false,
      ),
    )
    assertFalse(
      com.example.ui.components.shouldConfirmEntrySheetValueChange(
        targetValue = androidx.compose.material3.SheetValue.Hidden,
        isImeVisible = false,
        isImeClosing = true,
        isUserDraggingSheet = false,
        isExplicitDismissRequested = false,
      ),
    )
    assertFalse(
      com.example.ui.components.shouldConfirmEntrySheetValueChange(
        targetValue = androidx.compose.material3.SheetValue.PartiallyExpanded,
        isImeVisible = false,
        isImeClosing = false,
        isUserDraggingSheet = true,
        isExplicitDismissRequested = true,
      ),
    )
    // Intentional swipe down succeeds even while keyboard is visible or closing
    assertTrue(
      com.example.ui.components.shouldConfirmEntrySheetValueChange(
        targetValue = androidx.compose.material3.SheetValue.Hidden,
        isImeVisible = true,
        isImeClosing = true,
        isUserDraggingSheet = true,
        isExplicitDismissRequested = false,
      ),
    )
    // Explicit dismiss or idle dismiss succeeds when keyboard is closed
    assertTrue(
      com.example.ui.components.shouldConfirmEntrySheetValueChange(
        targetValue = androidx.compose.material3.SheetValue.Hidden,
        isImeVisible = false,
        isImeClosing = false,
        isUserDraggingSheet = false,
        isExplicitDismissRequested = false,
      ),
    )
    assertTrue(
      com.example.ui.components.shouldConfirmEntrySheetValueChange(
        targetValue = androidx.compose.material3.SheetValue.Hidden,
        isImeVisible = false,
        isImeClosing = true,
        isUserDraggingSheet = false,
        isExplicitDismissRequested = true,
      ),
    )
  }

  @Test
  fun `entry sheet back action hides keyboard first when visible and dismisses sheet when hidden`() {
    assertEquals(
      com.example.ui.components.EntrySheetBackAction.HIDE_KEYBOARD_ONLY,
      com.example.ui.components.resolveEntrySheetBackAction(isImeVisible = true),
    )
    assertEquals(
      com.example.ui.components.EntrySheetBackAction.DISMISS_SHEET,
      com.example.ui.components.resolveEntrySheetBackAction(isImeVisible = false),
    )
  }

  @Test
  fun `wellbeing semantic colors keep energy 1-to-5 red-to-green and invert stress and anxiety`() {
    assertEquals(MoodColor1.toArgb(), com.example.ui.theme.moodColorInterpolated(1f, invertScale = false).toArgb())
    assertEquals(MoodColor5.toArgb(), com.example.ui.theme.moodColorInterpolated(5f, invertScale = false).toArgb())

    // Inverted for Stress & Anxiety: 1 = Green, 2 = Lime, 3 = Yellow, 4 = Orange, 5 = Red
    assertEquals(MoodColor5.toArgb(), com.example.ui.theme.moodColorInterpolated(1f, invertScale = true).toArgb())
    assertEquals(MoodColor4.toArgb(), com.example.ui.theme.moodColorInterpolated(2f, invertScale = true).toArgb())
    assertEquals(MoodColor3.toArgb(), com.example.ui.theme.moodColorInterpolated(3f, invertScale = true).toArgb())
    assertEquals(MoodColor2.toArgb(), com.example.ui.theme.moodColorInterpolated(4f, invertScale = true).toArgb())
    assertEquals(MoodColor1.toArgb(), com.example.ui.theme.moodColorInterpolated(5f, invertScale = true).toArgb())
  }

  @Test
  fun `wellbeing analytics computes independent averages, excludes invalid scores, and handles missing values`() {
    val tz = TimeZone.getTimeZone("UTC")
    val day25 = DateTimeUtils.parseDayKeyStartMillisOrNull("2026-11-25", tz)!! + 10 * 3_600_000L
    val day24 = DateTimeUtils.parseDayKeyStartMillisOrNull("2026-11-24", tz)!! + 10 * 3_600_000L

    val entries = listOf(
      // Valid Energy=5, Stress=2, Anxiety=null
      MoodEntry(id = "w1", mood = 4, timestamp = day25, energyLevel = 5, stressLevel = 2, anxietyLevel = null),
      // Invalid Energy=0 & 6 should be excluded; valid Stress=4, Anxiety=1 kept
      MoodEntry(id = "w2", mood = 3, timestamp = day25 + 3_600_000L, energyLevel = 0, stressLevel = 4, anxietyLevel = 1),
      MoodEntry(id = "w3", mood = 3, timestamp = day24, energyLevel = 6, stressLevel = -1, anxietyLevel = 5),
      // Valid Energy=3, Stress=null, Anxiety=9 (invalid)
      MoodEntry(id = "w4", mood = 4, timestamp = day24 + 3_600_000L, energyLevel = 3, stressLevel = null, anxietyLevel = 9),
    )

    val summary = MoodAnalytics.computeWellbeingAnalytics(entries, InsightTimeRange.WEEK, day25, tz)
    assertTrue(summary.hasAnyWellbeingData)

    // Energy: valid [5, 3] -> 4.0 across 2 entries
    assertEquals(2, summary.energy.entryCount)
    assertEquals(4.0f, summary.energy.average!!, 0.001f)

    // Stress: valid [2, 4] -> 3.0 across 2 entries
    assertEquals(2, summary.stress.entryCount)
    assertEquals(3.0f, summary.stress.average!!, 0.001f)

    // Anxiety: valid [1, 5] -> 3.0 across 2 entries
    assertEquals(2, summary.anxiety.entryCount)
    assertEquals(3.0f, summary.anxiety.average!!, 0.001f)
  }

  @Test
  fun `wellbeing trends average same-day entries and leave missing calendar days null`() {
    val tz = TimeZone.getTimeZone("UTC")
    // 2026-11-25 is Wednesday
    val now = DateTimeUtils.parseDayKeyStartMillisOrNull("2026-11-25", tz)!! + 14 * 3_600_000L
    val nov25 = DateTimeUtils.parseDayKeyStartMillisOrNull("2026-11-25", tz)!!
    val nov23 = DateTimeUtils.parseDayKeyStartMillisOrNull("2026-11-23", tz)!!

    val entries = listOf(
      MoodEntry(id = "d1", mood = 4, timestamp = nov25 + 9 * 3_600_000L, energyLevel = 2, stressLevel = 4),
      MoodEntry(id = "d2", mood = 5, timestamp = nov25 + 13 * 3_600_000L, energyLevel = 5, stressLevel = 2),
      MoodEntry(id = "d3", mood = 3, timestamp = nov23 + 11 * 3_600_000L, energyLevel = 4, stressLevel = null),
    )

    val summary = MoodAnalytics.computeWellbeingAnalytics(entries, InsightTimeRange.WEEK, now, tz)

    // Week has 7 calendar days: Nov 19..Nov 25
    assertEquals(7, summary.energy.history.size)
    assertNull(summary.energy.history[0].averageValue) // Nov 19 missing
    assertEquals(4.0f, summary.energy.history[4].averageValue!!, 0.001f) // Nov 23
    assertNull(summary.energy.history[5].averageValue) // Nov 24 missing (no interpolation)
    assertEquals(3.5f, summary.energy.history[6].averageValue!!, 0.001f) // Nov 25 avg (2+5)/2
    assertEquals(2, summary.energy.history[6].entryCount)

    // Stress only recorded on Nov 25 -> Nov 23 remains null
    assertEquals(7, summary.stress.history.size)
    assertNull(summary.stress.history[4].averageValue)
    assertEquals(3.0f, summary.stress.history[6].averageValue!!, 0.001f)

    // Anxiety has no entries at all
    assertFalse(summary.anxiety.hasData)
    assertEquals(0, summary.anxiety.entryCount)
    assertTrue(summary.anxiety.history.isEmpty())
  }

  @Test
  fun `wellbeing weekday comparison averages daily averages rather than raw entry counts in Monday-to-Sunday order`() {
    val tz = TimeZone.getTimeZone("UTC")
    // 2026-11-25 is Wednesday
    val now = DateTimeUtils.parseDayKeyStartMillisOrNull("2026-11-25", tz)!! + 12 * 3_600_000L
    val monNov23 = DateTimeUtils.parseDayKeyStartMillisOrNull("2026-11-23", tz)!!
    val monNov16 = DateTimeUtils.parseDayKeyStartMillisOrNull("2026-11-16", tz)!!
    val tueNov24 = DateTimeUtils.parseDayKeyStartMillisOrNull("2026-11-24", tz)!!

    // 5 high-energy entries on Monday Nov 23 (daily avg = 5.0) + 1 low-energy entry on Monday Nov 16 (daily avg = 1.0).
    // Equal day weighting => Monday weekday avg must be (5.0 + 1.0) / 2 = 3.0, NOT (25 + 1) / 6 = 4.33.
    val entries = buildList {
      repeat(5) { idx ->
        add(
          MoodEntry(
            id = "m23-$idx",
            mood = 5,
            timestamp = monNov23 + (idx + 8) * 3_600_000L,
            energyLevel = 5,
            stressLevel = 1,
          ),
        )
      }
      add(MoodEntry(id = "m16", mood = 2, timestamp = monNov16 + 10 * 3_600_000L, energyLevel = 1, stressLevel = 5))
      add(MoodEntry(id = "t24", mood = 4, timestamp = tueNov24 + 10 * 3_600_000L, energyLevel = 4, anxietyLevel = 2))
    }

    val monthSummary = MoodAnalytics.computeWellbeingAnalytics(entries, InsightTimeRange.MONTH, now, tz)
    assertEquals(7, monthSummary.weekdayAverages.size)
    assertEquals(
      listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"),
      monthSummary.weekdayAverages.map { it.weekdayLabel },
    )

    val monday = monthSummary.weekdayAverages[0]
    assertEquals(2, monday.energyDayCount)
    assertEquals(3.0f, monday.energyAverage!!, 0.001f)
    assertEquals(2, monday.stressDayCount)
    assertEquals(3.0f, monday.stressAverage!!, 0.001f)
    assertNull(monday.anxietyAverage)

    val tuesday = monthSummary.weekdayAverages[1]
    assertEquals(1, tuesday.energyDayCount)
    assertEquals(4.0f, tuesday.energyAverage!!, 0.001f)
    assertNull(tuesday.stressAverage)
    assertEquals(2.0f, tuesday.anxietyAverage!!, 0.001f)

    // Sunday has no entries -> all null
    val sunday = monthSummary.weekdayAverages[6]
    assertFalse(sunday.hasAnyData)
    assertNull(sunday.energyAverage)
    assertNull(sunday.stressAverage)
    assertNull(sunday.anxietyAverage)
  }

  @Test
  fun `wellbeing analytics respects Day Week Month All filters, empty periods, and timezone DST boundaries`() {
    val tz = TimeZone.getTimeZone("America/Los_Angeles")
    // 2026-11-25 12:00 PST
    val now = DateTimeUtils.parseDayKeyStartMillisOrNull("2026-11-25", tz)!! + 12 * 3_600_000L
    // Entry at 2026-11-25 23:30 PST (in UTC this is 2026-11-26 07:30 UTC - tests local calendar day)
    val lateTodayPst = DateTimeUtils.parseDayKeyStartMillisOrNull("2026-11-25", tz)!! + 23 * 3_600_000L + 30 * 60_000L
    // Entry 3 days ago (Week + Month + All)
    val threeDaysAgo = DateTimeUtils.parseDayKeyStartMillisOrNull("2026-11-22", tz)!! + 10 * 3_600_000L
    // Entry on 2026-11-01 (DST fall-back 25-hour day in America/Los_Angeles, 24 days ago -> Month + All)
    val dstDay = DateTimeUtils.parseDayKeyStartMillisOrNull("2026-11-01", tz)!! + 15 * 3_600_000L
    // Entry 45 days ago (All only)
    val oldDay = DateTimeUtils.parseDayKeyStartMillisOrNull("2026-10-11", tz)!! + 10 * 3_600_000L

    val entries = listOf(
      MoodEntry(id = "e1", mood = 4, timestamp = lateTodayPst, energyLevel = 4, stressLevel = 2, anxietyLevel = 1),
      MoodEntry(id = "e2", mood = 3, timestamp = threeDaysAgo, energyLevel = 2, stressLevel = 4, anxietyLevel = 3),
      MoodEntry(id = "e3", mood = 5, timestamp = dstDay, energyLevel = 5, stressLevel = 1, anxietyLevel = 2),
      MoodEntry(id = "e4", mood = 2, timestamp = oldDay, energyLevel = 1, stressLevel = 5, anxietyLevel = 5),
    )

    val day = MoodAnalytics.computeWellbeingAnalytics(entries, InsightTimeRange.DAY, now, tz)
    assertEquals(1, day.energy.entryCount)
    assertEquals(4.0f, day.energy.average!!, 0.001f)
    assertEquals(1, day.energy.history.size)

    val week = MoodAnalytics.computeWellbeingAnalytics(entries, InsightTimeRange.WEEK, now, tz)
    assertEquals(2, week.energy.entryCount)
    assertEquals(3.0f, week.energy.average!!, 0.001f)
    assertEquals(7, week.energy.history.size)

    val month = MoodAnalytics.computeWellbeingAnalytics(entries, InsightTimeRange.MONTH, now, tz)
    assertEquals(3, month.energy.entryCount)
    assertEquals(3.6666f, month.energy.average!!, 0.01f)
    assertEquals(30, month.energy.history.size)
    // Verify DST day 2026-11-01 aligns cleanly on its Sunday slot in MONTH history
    assertEquals(5.0f, month.energy.history[5].averageValue!!, 0.001f)

    val all = MoodAnalytics.computeWellbeingAnalytics(entries, InsightTimeRange.ALL, now, tz)
    assertEquals(4, all.energy.entryCount)
    assertEquals(3.0f, all.energy.average!!, 0.001f)
    assertEquals(46, all.energy.history.size) // Oct 11 .. Nov 25 inclusive = 46 days

    // Empty entries / entries without wellbeing metrics
    val emptySummary = MoodAnalytics.computeWellbeingAnalytics(
      listOf(MoodEntry(id = "plain", mood = 4, timestamp = now)),
      InsightTimeRange.ALL,
      now,
      tz,
    )
    assertFalse(emptySummary.hasAnyWellbeingData)
    assertNull(emptySummary.energy.average)
    assertNull(emptySummary.stress.average)
    assertNull(emptySummary.anxiety.average)
    assertTrue(emptySummary.energy.history.isEmpty())
    assertEquals(7, emptySummary.weekdayAverages.size)
    assertTrue(emptySummary.weekdayAverages.all { !it.hasAnyData })
  }
}
