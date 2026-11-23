package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ImportValidationResult
import com.example.data.MoodBackupSerializer
import com.example.data.MoodRepository
import com.example.data.MoodyDatabase
import com.example.data.MoodyPreferences
import com.example.model.DailyContext
import com.example.model.MoodEntry
import com.example.model.PhysicalActivityLevel
import com.example.ui.components.EntryDetailSheet
import com.example.ui.components.MoodFaceSelectorRow
import com.example.ui.screens.InsightsScreen
import com.example.ui.theme.AppearanceMode
import com.example.ui.theme.MoodyPalette
import com.example.ui.theme.MoodyTheme
import com.example.viewmodel.InsightTimeRange
import com.example.viewmodel.MoodAnalytics
import com.example.viewmodel.MoodyUiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MoodyEnhancementsRobolectricTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun `json export and import roundtrips contextual and daily fields and preserves v1 compatibility`() {
    val ts = 1_763_885_644_000L
    val entryWithContext = MoodEntry(
      id = "entry-1",
      mood = 4,
      timestamp = ts,
      note = "Rested morning",
      tags = listOf("calm", "walk"),
      sleepDurationMinutes = 450,
      sleepQuality = 5,
      energyLevel = 4,
      stressLevel = 2,
      anxietyLevel = 1,
      physicalActivity = PhysicalActivityLevel.MODERATE,
    )

    val json = MoodBackupSerializer.exportJson(
      entries = listOf(entryWithContext),
      dailyContexts = listOf(DailyContext("2025-11-23", sleepDurationMinutes = 450, sleepQuality = 5)),
      nowMillis = ts,
    )
    assertFalse(json.contains("isDemo"))

    val result = MoodBackupSerializer.parseAndValidateJson(json)
    assertTrue(result is ImportValidationResult.Valid)
    val imported = (result as ImportValidationResult.Valid).entries.single()
    assertEquals(450, imported.sleepDurationMinutes)
    assertEquals(5, imported.sleepQuality)
    assertEquals(4, imported.energyLevel)
    assertEquals(2, imported.stressLevel)
    assertEquals(1, imported.anxietyLevel)
    assertEquals(PhysicalActivityLevel.MODERATE, imported.physicalActivity)

    val v1Json = """
      {
        "app": "MOODY",
        "version": 1,
        "entries": [
          {
            "id": "legacy-1",
            "mood": 5,
            "timestamp": 1700050000000,
            "tags": ["grateful"],
            "note": "Old journal note",
            "isDemo": false
          }
        ]
      }
    """.trimIndent()
    val v1Result = MoodBackupSerializer.parseAndValidateJson(v1Json) as ImportValidationResult.Valid
    val v1Entry = v1Result.entries.single()
    assertEquals("legacy-1", v1Entry.id)
    assertEquals(5, v1Entry.mood)
    assertNull(v1Entry.sleepDurationMinutes)
  }

  @Test
  fun `migration 1 to 2 removes demo entries while preserving genuine user entries`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dbName = "migration_test_${System.nanoTime()}.db"

    val helper = object : SQLiteOpenHelper(context, dbName, null, 1) {
      override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `mood_entries` (
            `id` TEXT NOT NULL,
            `mood` INTEGER NOT NULL,
            `timestamp` INTEGER NOT NULL,
            `note` TEXT NOT NULL,
            `tagsJson` TEXT NOT NULL,
            `isDemo` INTEGER NOT NULL,
            `createdAt` INTEGER NOT NULL,
            `updatedAt` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
          )
          """.trimIndent(),
        )
      }

      override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }

    helper.writableDatabase.use { sqlDb ->
      sqlDb.execSQL(
        """
        INSERT INTO mood_entries (id, mood, timestamp, note, tagsJson, isDemo, createdAt, updatedAt)
        VALUES ('user-1', 4, 1763800000000, 'My real journal entry', '["calm","walk"]', 0, 1763800000000, 1763800000000)
        """.trimIndent(),
      )
      sqlDb.execSQL(
        """
        INSERT INTO mood_entries (id, mood, timestamp, note, tagsJson, isDemo, createdAt, updatedAt)
        VALUES ('demo-1', 5, 1763700000000, 'Seeded sample entry', '["demo"]', 1, 1763700000000, 1763700000000)
        """.trimIndent(),
      )
    }
    helper.close()

    val migratedDb = Room.databaseBuilder(context, MoodyDatabase::class.java, dbName)
      .addMigrations(MoodyDatabase.MIGRATION_1_2)
      .allowMainThreadQueries()
      .build()

    try {
      val repository = MoodRepository(
        dao = migratedDb.moodEntryDao(),
        dailyContextDao = migratedDb.dailyContextDao(),
        preferences = MoodyPreferences(context),
      )

      val entries = repository.allEntries.first()
      assertEquals(1, entries.size)
      val preserved = entries.single()
      assertEquals("user-1", preserved.id)
      assertEquals(4, preserved.mood)
      assertEquals(1763800000000L, preserved.timestamp)
      assertEquals("My real journal entry", preserved.note)
      assertEquals(listOf("calm", "walk"), preserved.tags)
      assertNull(preserved.sleepDurationMinutes)
      assertNull(preserved.energyLevel)
    } finally {
      migratedDb.close()
      context.deleteDatabase(dbName)
    }
  }

  @Test
  fun `fresh database launches genuinely empty and daily sleep does not overwrite unintentionally`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, MoodyDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val repository = MoodRepository(
        dao = db.moodEntryDao(),
        dailyContextDao = db.dailyContextDao(),
        preferences = MoodyPreferences(context),
      )

      assertTrue(repository.allEntries.first().isEmpty())

      val ts1 = 1_763_885_644_000L
      val ts2 = ts1 + 3_600_000L
      val e1 = MoodEntry(id = "e1", mood = 4, timestamp = ts1)
      val e2 = MoodEntry(id = "e2", mood = 3, timestamp = ts2)
      repository.upsertEntry(e1)
      repository.upsertEntry(e2)

      repository.saveEditedEntry(
        e1.copy(sleepDurationMinutes = 450, sleepQuality = 4, energyLevel = 4),
        updateDailySleep = true,
      )

      repository.saveEditedEntry(
        e2.copy(note = "Afternoon focus", stressLevel = 3, sleepDurationMinutes = null, sleepQuality = null),
        updateDailySleep = false,
      )

      val loaded = repository.allEntries.first()
      assertEquals(2, loaded.size)
      val loaded1 = loaded.first { it.id == "e1" }
      val loaded2 = loaded.first { it.id == "e2" }
      assertEquals(450, loaded1.sleepDurationMinutes)
      assertEquals(4, loaded1.sleepQuality)
      assertEquals(4, loaded1.energyLevel)
      assertEquals(450, loaded2.sleepDurationMinutes)
      assertEquals(4, loaded2.sleepQuality)
      assertEquals(3, loaded2.stressLevel)
      assertEquals("Afternoon focus", loaded2.note)
    } finally {
      db.close()
    }
  }

  @Test
  fun `appearance mode and color palette persist in preferences`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = MoodyPreferences(context)

    prefs.setAppearanceMode(AppearanceMode.LIGHT)
    prefs.setColorPalette(MoodyPalette.SAGE_DAYLIGHT)

    assertEquals(AppearanceMode.LIGHT, prefs.appearanceMode.first())
    assertEquals(MoodyPalette.SAGE_DAYLIGHT, prefs.colorPalette.first())

    prefs.setAppearanceMode(AppearanceMode.DARK)
    prefs.setColorPalette(MoodyPalette.VIOLET_MIDNIGHT)

    assertEquals(AppearanceMode.DARK, prefs.appearanceMode.first())
    assertEquals(MoodyPalette.VIOLET_MIDNIGHT, prefs.colorPalette.first())
  }

  @Test
  fun `additional context is collapsed by default and preserves values when collapsed and saved`() {
    val originalTs = 1_763_885_644_000L
    val entry = MoodEntry(
      id = "ui-entry-1",
      mood = 4,
      timestamp = originalTs,
      note = "Quiet evening",
      tags = listOf("calm"),
    )
    var savedEntry: MoodEntry? = null
    var savedUpdateDailySleep: Boolean? = null

    composeRule.setContent {
      MoodyTheme(appearanceMode = AppearanceMode.DARK, palette = MoodyPalette.TEAL_OBSIDIAN) {
        EntryDetailSheet(
          entry = entry,
          suggestedTags = listOf("calm", "walk"),
          onSave = { updated, updateDaily ->
            savedEntry = updated
            savedUpdateDailySleep = updateDaily
          },
          onDelete = {},
          onDismiss = {},
        )
      }
    }

    composeRule.waitForIdle()

    composeRule.onNodeWithTag("additional_context_toggle").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("sleep_quality_4").assertDoesNotExist()

    composeRule.onNodeWithTag("additional_context_toggle").performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("sleep_hours_plus").performScrollTo().performClick()
    composeRule.onNodeWithTag("sleep_minutes_plus").performScrollTo().performClick()
    composeRule.onNodeWithTag("sleep_quality_4").performScrollTo().performClick()
    composeRule.onNodeWithTag("energy_level_5").performScrollTo().performClick()
    composeRule.onNodeWithTag("stress_level_2").performScrollTo().performClick()
    composeRule.onNodeWithTag("anxiety_level_1").performScrollTo().performClick()
    composeRule.onNodeWithTag("activity_moderate").performScrollTo().performClick()

    composeRule.onNodeWithTag("additional_context_toggle").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("sleep_quality_4").assertDoesNotExist()

    composeRule.onNodeWithText("8h 5m sleep · Sleep 4/5 · Energy 5/5 · Stress 2/5 · Anxiety 1/5 · Moderate")
      .assertIsDisplayed()

    composeRule.onNodeWithTag("save_entry_button").performScrollTo().performClick()
    composeRule.waitForIdle()

    assertEquals(4, savedEntry?.mood)
    assertEquals(originalTs, savedEntry?.timestamp)
    assertEquals("Quiet evening", savedEntry?.note)
    assertEquals(listOf("calm"), savedEntry?.tags)
    assertEquals(8 * 60 + 5, savedEntry?.sleepDurationMinutes)
    assertEquals(4, savedEntry?.sleepQuality)
    assertEquals(5, savedEntry?.energyLevel)
    assertEquals(2, savedEntry?.stressLevel)
    assertEquals(1, savedEntry?.anxietyLevel)
    assertEquals(PhysicalActivityLevel.MODERATE, savedEntry?.physicalActivity)
    assertEquals(true, savedUpdateDailySleep)
  }

  @Test
  fun `mood selector displays numerical scores 1 to 5 without descriptive labels`() {
    var tappedScore: Int? = null
    composeRule.setContent {
      MoodyTheme(appearanceMode = AppearanceMode.DARK, palette = MoodyPalette.TEAL_OBSIDIAN) {
        MoodFaceSelectorRow(
          selectedMood = null,
          onSelectMood = { tappedScore = it },
        )
      }
    }
    composeRule.waitForIdle()

    (1..5).forEach { score ->
      composeRule.onNodeWithText(score.toString()).assertIsDisplayed()
    }
    listOf("Heavy", "Low", "Steady", "Warm", "Radiant").forEach { legacyWord ->
      composeRule.onNodeWithText(legacyWord).assertDoesNotExist()
    }

    composeRule.onNodeWithTag("mood_button_4").performClick()
    assertEquals(4, tappedScore)
  }

  @Test
  fun `insights screen renders sleep and recovery section and empty state accurately`() {
    val now = System.currentTimeMillis()
    val entries = listOf(MoodEntry(id = "m1", mood = 4, timestamp = now))
    val emptySleepState = MoodyUiState(
      allEntries = entries,
      insightRange = InsightTimeRange.WEEK,
      sleepAnalytics = MoodAnalytics.computeSleepAnalytics(emptyList(), InsightTimeRange.WEEK, now),
    )

    composeRule.setContent {
      MoodyTheme(appearanceMode = AppearanceMode.DARK, palette = MoodyPalette.TEAL_OBSIDIAN) {
        InsightsScreen(
          uiState = emptySleepState,
          onSelectRange = {},
          onSelectHeatmapDay = {},
          onSelectTimeOfDayMode = {},
        )
      }
    }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("sleep_recovery_header").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("No sleep data recorded for this period.").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `insights screen displays all four sleep and recovery components when sleep data exists`() {
    val now = 1_763_885_644_000L
    val entries = listOf(MoodEntry(id = "m1", mood = 4, timestamp = now))
    val dayKeyToday = com.example.util.DateTimeUtils.formatFileDate(now)
    val dayKeyYesterday = com.example.util.DateTimeUtils.formatFileDate(now - 86_400_000L)
    val contexts = listOf(
      DailyContext(dayKey = dayKeyToday, sleepDurationMinutes = 450, sleepQuality = 4),
      DailyContext(dayKey = dayKeyYesterday, sleepDurationMinutes = 420, sleepQuality = 3),
    )
    val populatedState = MoodyUiState(
      allEntries = entries,
      insightRange = InsightTimeRange.WEEK,
      sleepAnalytics = MoodAnalytics.computeSleepAnalytics(contexts, InsightTimeRange.WEEK, now),
    )

    composeRule.setContent {
      MoodyTheme(appearanceMode = AppearanceMode.DARK, palette = MoodyPalette.TEAL_OBSIDIAN) {
        InsightsScreen(
          uiState = populatedState,
          onSelectRange = {},
          onSelectHeatmapDay = {},
          onSelectTimeOfDayMode = {},
        )
      }
    }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("sleep_stat_avg_quality").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("3.5 / 5").assertIsDisplayed()
    composeRule.onNodeWithText("7h 15m").assertIsDisplayed()
    composeRule.onNodeWithTag("sleep_quality_history_card").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("sleep_weekday_card").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("Monday").assertIsDisplayed()
    composeRule.onNodeWithText("Sunday").assertIsDisplayed()
  }
}
