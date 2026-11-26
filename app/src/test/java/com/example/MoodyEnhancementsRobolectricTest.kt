package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.activity.ComponentDialog
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.screens.InsightsCategory
import com.example.ui.screens.InsightsHabitsScreen
import com.example.ui.screens.InsightsMoodScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.InsightsSleepScreen
import com.example.ui.screens.InsightsTagsScreen
import com.example.ui.screens.InsightsWellbeingScreen
import com.example.ui.theme.AppearanceMode
import com.example.ui.theme.MoodyPalette
import com.example.ui.theme.MoodyTheme
import com.example.viewmodel.InsightTimeRange
import com.example.viewmodel.MoodAnalytics
import com.example.viewmodel.MoodyUiState
import com.example.viewmodel.TimeOfDayMode
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
import org.robolectric.shadows.ShadowDialog

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
  fun `insights sleep screen renders sleep and recovery section and empty state accurately`() {
    val now = System.currentTimeMillis()
    val entries = listOf(MoodEntry(id = "m1", mood = 4, timestamp = now))
    val emptySleepState = MoodyUiState(
      allEntries = entries,
      insightRange = InsightTimeRange.WEEK,
      sleepAnalytics = MoodAnalytics.computeSleepAnalytics(emptyList(), InsightTimeRange.WEEK, now),
    )

    composeRule.setContent {
      MoodyTheme(appearanceMode = AppearanceMode.DARK, palette = MoodyPalette.TEAL_OBSIDIAN) {
        InsightsSleepScreen(
          uiState = emptySleepState,
          onBack = {},
          onSelectRange = {},
        )
      }
    }
    composeRule.waitForIdle()
    composeRule.onNodeWithText("Sleep & Recovery").assertIsDisplayed()
    composeRule.onNodeWithTag("insights_back_button").assertIsDisplayed()
    composeRule.onNodeWithTag("sleep_empty_state").assertIsDisplayed()
    composeRule.onNodeWithText("No sleep data recorded for this period.").assertIsDisplayed()
  }

  @Test
  fun `insights sleep screen displays all four sleep and recovery components including duration history`() {
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
        InsightsSleepScreen(
          uiState = populatedState,
          onBack = {},
          onSelectRange = {},
        )
      }
    }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("sleep_stat_avg_quality"))
    composeRule.onNodeWithTag("sleep_stat_avg_quality").assertIsDisplayed()
    composeRule.onNodeWithText("3.5 / 5").assertIsDisplayed()
    composeRule.onNodeWithText("7h 15m").assertIsDisplayed()
    composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("sleep_quality_history_card"))
    composeRule.onNodeWithTag("sleep_quality_history_card").assertIsDisplayed()
    composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("sleep_duration_history_card"))
    composeRule.onNodeWithText("Sleep Duration History").assertIsDisplayed()
    composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("sleep_weekday_card"))
    composeRule.onNodeWithTag("sleep_weekday_card").assertIsDisplayed()
    composeRule.onNodeWithText("Monday").assertIsDisplayed()
    composeRule.onNodeWithText("Sunday").assertIsDisplayed()
  }

  @Test
  fun `hiding keyboard via hide button, back press, or outside tap keeps entry details open and preserves unsaved data`() {
    val entry = MoodEntry(
      id = "kbd-entry-1",
      mood = 4,
      timestamp = 1_763_885_644_000L,
      note = "",
      tags = listOf("calm"),
    )
    var dismissCount = 0

    composeRule.setContent {
      MoodyTheme(appearanceMode = AppearanceMode.DARK, palette = MoodyPalette.TEAL_OBSIDIAN) {
        EntryDetailSheet(
          entry = entry,
          suggestedTags = listOf("calm", "focused"),
          onSave = { _, _ -> },
          onDelete = {},
          onDismiss = { dismissCount++ },
        )
      }
    }
    composeRule.waitForIdle()

    val dialog = ShadowDialog.getLatestDialog() as ComponentDialog
    val decorView = dialog.window!!.decorView

    fun dispatchImeVisible(visible: Boolean) {
      composeRule.runOnUiThread {
        val insets = WindowInsetsCompat.Builder()
          .setInsets(
            WindowInsetsCompat.Type.ime(),
            if (visible) Insets.of(0, 0, 0, 600) else Insets.NONE,
          )
          .setVisible(WindowInsetsCompat.Type.ime(), visible)
          .build()
        ViewCompat.dispatchApplyWindowInsets(decorView, insets)
      }
      composeRule.waitForIdle()
    }

    // Populate Additional Context first (without saving) to verify unsaved context survives
    composeRule.onNodeWithTag("additional_context_toggle").performScrollTo().performClick()
    composeRule.onNodeWithTag("sleep_quality_4").performScrollTo().performClick()
    composeRule.onNodeWithTag("energy_level_5").performScrollTo().performClick()
    composeRule.waitForIdle()

    // Focus Note field, show IME, enter text, then hide keyboard (simulating keyboard hide button)
    composeRule.onNodeWithTag("entry_note_input").performScrollTo().performClick()
    dispatchImeVisible(true)
    composeRule.onNodeWithTag("entry_note_input").assertIsFocused()
    composeRule.onNodeWithTag("entry_note_input").performTextInput("First line")
    composeRule.waitForIdle()

    dispatchImeVisible(false)
    composeRule.mainClock.advanceTimeBy(450L)
    composeRule.waitForIdle()

    assertEquals(0, dismissCount)
    composeRule.onNodeWithTag("entry_detail_sheet_content").assertIsDisplayed()
    composeRule.onNodeWithTag("entry_note_input").assertIsNotFocused()
    composeRule.onNodeWithText("First line").assertIsDisplayed()

    // Reopen keyboard, add more text, and dismiss via Back button
    composeRule.onNodeWithTag("entry_note_input").performScrollTo().performClick()
    dispatchImeVisible(true)
    composeRule.onNodeWithTag("entry_note_input").assertIsFocused()
    composeRule.onNodeWithTag("entry_note_input").performTextInput(" + second line")
    composeRule.waitForIdle()

    composeRule.runOnUiThread {
      dialog.onBackPressedDispatcher.onBackPressed()
    }
    dispatchImeVisible(false)
    composeRule.mainClock.advanceTimeBy(450L)
    composeRule.waitForIdle()

    assertEquals(0, dismissCount)
    composeRule.onNodeWithTag("entry_detail_sheet_content").assertIsDisplayed()
    composeRule.onNodeWithTag("entry_note_input").assertIsNotFocused()
    composeRule.onNodeWithText("First line + second line").assertIsDisplayed()

    // Add tag, then tap outside text field on non-input sheet area to release focus
    composeRule.onNodeWithTag("entry_tag_input").performScrollTo().performClick()
    dispatchImeVisible(true)
    composeRule.onNodeWithTag("entry_tag_input").performTextInput("evening")
    composeRule.onNodeWithTag("add_tag_button").performClick()
    composeRule.onNodeWithTag("entry_note_input").performScrollTo().performClick()
    composeRule.onNodeWithTag("entry_note_input").assertIsFocused()

    composeRule.onNodeWithTag("entry_detail_sheet_content").performTouchInput {
      click(topLeft)
    }
    dispatchImeVisible(false)
    composeRule.mainClock.advanceTimeBy(450L)
    composeRule.waitForIdle()

    assertEquals(0, dismissCount)
    composeRule.onNodeWithTag("entry_detail_sheet_content").assertIsDisplayed()
    composeRule.onNodeWithText("#evening").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("Sleep 4/5 · Energy 5/5").assertIsDisplayed()

    // Now that keyboard is already hidden, system Back dismisses Entry Details
    composeRule.runOnUiThread {
      dialog.onBackPressedDispatcher.onBackPressed()
    }
    composeRule.waitForIdle()
    assertEquals(1, dismissCount)
  }

  @Test
  fun `intentional swipe down on drag handle still dismisses entry details sheet`() {
    val entry = MoodEntry(
      id = "swipe-entry-1",
      mood = 3,
      timestamp = 1_763_885_644_000L,
    )
    var dismissCount = 0

    composeRule.setContent {
      MoodyTheme(appearanceMode = AppearanceMode.DARK, palette = MoodyPalette.TEAL_OBSIDIAN) {
        EntryDetailSheet(
          entry = entry,
          suggestedTags = emptyList(),
          onSave = { _, _ -> },
          onDelete = {},
          onDismiss = { dismissCount++ },
        )
      }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("entry_sheet_drag_handle").performTouchInput {
      swipeDown()
    }
    composeRule.waitForIdle()

    assertEquals(1, dismissCount)
  }

  @Test
  fun `insights wellbeing screen renders empty state when no wellbeing metrics exist in period`() {
    val now = 1_764_140_000_000L
    val entries = listOf(MoodEntry(id = "plain-1", mood = 4, timestamp = now))
    val emptyWellbeingState = MoodyUiState(
      allEntries = entries,
      insightRange = InsightTimeRange.WEEK,
      sleepAnalytics = MoodAnalytics.computeSleepAnalytics(emptyList(), InsightTimeRange.WEEK, now),
      wellbeingAnalytics = MoodAnalytics.computeWellbeingAnalytics(entries, InsightTimeRange.WEEK, now),
    )

    composeRule.setContent {
      MoodyTheme(appearanceMode = AppearanceMode.DARK, palette = MoodyPalette.TEAL_OBSIDIAN) {
        InsightsWellbeingScreen(
          uiState = emptyWellbeingState,
          onBack = {},
          onSelectRange = {},
        )
      }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Energy, Stress & Anxiety").assertIsDisplayed()
    composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("wellbeing_empty_state"))
    composeRule.onNodeWithText("No wellbeing data recorded for this period.").assertIsDisplayed()
  }

  @Test
  fun `insights wellbeing screen displays populated tiles, three charts, and 7-day weekday comparison`() {
    val now = 1_764_140_000_000L
    val entries = listOf(
      MoodEntry(
        id = "wb-1",
        mood = 4,
        timestamp = now,
        energyLevel = 4,
        stressLevel = 2,
        anxietyLevel = 1,
      ),
      MoodEntry(
        id = "wb-2",
        mood = 3,
        timestamp = now - 86_400_000L,
        energyLevel = 3,
        stressLevel = 4,
        anxietyLevel = 3,
      ),
    )
    val state = MoodyUiState(
      allEntries = entries,
      insightRange = InsightTimeRange.WEEK,
      sleepAnalytics = MoodAnalytics.computeSleepAnalytics(emptyList(), InsightTimeRange.WEEK, now),
      wellbeingAnalytics = MoodAnalytics.computeWellbeingAnalytics(entries, InsightTimeRange.WEEK, now),
    )

    composeRule.setContent {
      MoodyTheme(appearanceMode = AppearanceMode.DARK, palette = MoodyPalette.TEAL_OBSIDIAN) {
        InsightsWellbeingScreen(
          uiState = state,
          onBack = {},
          onSelectRange = {},
        )
      }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("wellbeing_stat_energy"))
    composeRule.onNodeWithTag("wellbeing_stat_energy").assertIsDisplayed()
    composeRule.onNodeWithTag("wellbeing_stat_stress").assertIsDisplayed()
    composeRule.onNodeWithTag("wellbeing_stat_anxiety").assertIsDisplayed()
    composeRule.onNodeWithText("Avg Energy").assertIsDisplayed()
    composeRule.onNodeWithText("Avg Stress").assertIsDisplayed()
    composeRule.onNodeWithText("Avg Anxiety").assertIsDisplayed()
    composeRule.onAllNodesWithText("3.5 / 5").onFirst().assertIsDisplayed()
    composeRule.onAllNodesWithText("3.0 / 5").onFirst().assertIsDisplayed()
    composeRule.onAllNodesWithText("2.0 / 5").onFirst().assertIsDisplayed()
    assertTrue(composeRule.onAllNodesWithText("2 entries").fetchSemanticsNodes().size >= 3)

    composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("wellbeing_history_energy"))
    composeRule.onNodeWithText("Energy Over Time").assertIsDisplayed()

    composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("wellbeing_history_stress"))
    composeRule.onNodeWithText("Stress Over Time").assertIsDisplayed()

    composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("wellbeing_history_anxiety"))
    composeRule.onNodeWithText("Anxiety Over Time").assertIsDisplayed()

    composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("wellbeing_weekday_card"))
    composeRule.onNodeWithText("Wellbeing by Weekday").assertIsDisplayed()
    listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday").forEach { day ->
      composeRule.onNodeWithText(day).assertIsDisplayed()
    }
  }

  @Test
  fun `insights wellbeing screen handles partial data without hiding recorded metrics`() {
    val now = 1_764_140_000_000L
    val entries = listOf(
      MoodEntry(
        id = "partial-1",
        mood = 4,
        timestamp = now,
        energyLevel = 5,
        stressLevel = null,
        anxietyLevel = 2,
      ),
    )
    val state = MoodyUiState(
      allEntries = entries,
      insightRange = InsightTimeRange.WEEK,
      sleepAnalytics = MoodAnalytics.computeSleepAnalytics(emptyList(), InsightTimeRange.WEEK, now),
      wellbeingAnalytics = MoodAnalytics.computeWellbeingAnalytics(entries, InsightTimeRange.WEEK, now),
    )

    composeRule.setContent {
      MoodyTheme(appearanceMode = AppearanceMode.LIGHT, palette = MoodyPalette.SAGE_DAYLIGHT) {
        InsightsWellbeingScreen(
          uiState = state,
          onBack = {},
          onSelectRange = {},
        )
      }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("wellbeing_stat_energy"))
    composeRule.onAllNodesWithText("5.0 / 5").onFirst().assertIsDisplayed()
    composeRule.onAllNodesWithText("2.0 / 5").onFirst().assertIsDisplayed()
    composeRule.onNodeWithText("No entries").assertIsDisplayed()

    composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("wellbeing_history_stress"))
    composeRule.onNodeWithText("No stress data recorded for this period.").assertIsDisplayed()
  }

  @Test
  fun `insights overview displays snapshot, standardized ranges, and dispatches category navigation`() {
    val now = 1_764_140_000_000L
    val dayKey = com.example.util.DateTimeUtils.formatFileDate(now)
    val entries = listOf(
      MoodEntry(id = "ov-1", mood = 4, timestamp = now, energyLevel = 5),
      MoodEntry(id = "ov-2", mood = 4, timestamp = now - 86_400_000L, energyLevel = 3),
    )
    val contexts = listOf(
      DailyContext(dayKey = dayKey, sleepDurationMinutes = 450, sleepQuality = 4),
    )
    val state = MoodyUiState(
      allEntries = entries,
      insightRange = InsightTimeRange.MONTH,
      stats = MoodAnalytics.computeStatsForRange(entries, InsightTimeRange.MONTH, now),
      sleepAnalytics = MoodAnalytics.computeSleepAnalytics(contexts, InsightTimeRange.MONTH, now),
      wellbeingAnalytics = MoodAnalytics.computeWellbeingAnalytics(entries, InsightTimeRange.MONTH, now),
    )

    val selectedCategories = mutableListOf<InsightsCategory>()
    var selectedRange: InsightTimeRange? = null

    composeRule.setContent {
      MoodyTheme(appearanceMode = AppearanceMode.DARK, palette = MoodyPalette.TEAL_OBSIDIAN) {
        InsightsScreen(
          uiState = state,
          onSelectRange = { selectedRange = it },
          onSelectCategory = { selectedCategories.add(it) },
        )
      }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Wellbeing Overview").assertIsDisplayed()
    composeRule.onNodeWithTag("range_week").assertIsDisplayed()
    composeRule.onNodeWithTag("range_month").assertIsDisplayed()
    composeRule.onNodeWithTag("range_three_months").assertIsDisplayed()
    composeRule.onNodeWithTag("range_all").assertIsDisplayed()
    composeRule.onNodeWithTag("range_day").assertDoesNotExist()

    composeRule.onNodeWithTag("snapshot_avg_mood").assertIsDisplayed()
    composeRule.onNodeWithTag("snapshot_avg_energy").assertIsDisplayed()
    composeRule.onNodeWithTag("snapshot_avg_sleep").assertIsDisplayed()

    composeRule.onNodeWithTag("range_three_months").performClick()
    assertEquals(InsightTimeRange.THREE_MONTHS, selectedRange)

    listOf(
      "category_mood" to InsightsCategory.MOOD,
      "category_wellbeing" to InsightsCategory.WELLBEING,
      "category_sleep" to InsightsCategory.SLEEP,
      "category_habits" to InsightsCategory.HABITS,
      "category_tags" to InsightsCategory.TAGS,
    ).forEach { (tag, expected) ->
      composeRule.onNodeWithTag("insights_overview_list").performScrollToNode(hasTestTag(tag))
      composeRule.onNodeWithTag(tag).performClick()
      assertEquals(expected, selectedCategories.last())
    }
  }

  @Test
  fun `insights mood screen displays stats, mood over time, mood calendar, and back action`() {
    val now = 1_764_140_000_000L
    val entries = listOf(
      MoodEntry(id = "md-1", mood = 2, timestamp = now - 3 * 86_400_000L),
      MoodEntry(id = "md-2", mood = 4, timestamp = now - 86_400_000L),
      MoodEntry(id = "md-3", mood = 5, timestamp = now),
    )
    val points = MoodAnalytics.computeLineGraphPoints(entries, InsightTimeRange.WEEK, now)
    val state = MoodyUiState(
      allEntries = entries,
      insightRange = InsightTimeRange.WEEK,
      stats = MoodAnalytics.computeStatsForRange(entries, InsightTimeRange.WEEK, now),
      lineGraphPoints = points,
      heatmapCells = MoodAnalytics.computeCalendarHeatmap(entries, nowMillis = now),
      moodObservation = MoodAnalytics.computeMoodObservation(points),
    )
    var backClicks = 0

    composeRule.setContent {
      MoodyTheme(appearanceMode = AppearanceMode.DARK, palette = MoodyPalette.TEAL_OBSIDIAN) {
        InsightsMoodScreen(
          uiState = state,
          onBack = { backClicks++ },
          onSelectRange = {},
          onSelectHeatmapDay = {},
        )
      }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Mood & Emotional Patterns").assertIsDisplayed()
    composeRule.onNodeWithTag("stat_avg_mood").assertIsDisplayed()
    composeRule.onNodeWithTag("stat_total_entries").assertIsDisplayed()
    composeRule.onNodeWithTag("stat_streak").assertIsDisplayed()
    composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("mood_over_time_card"))
    composeRule.onNodeWithText("Mood Over Time").assertIsDisplayed()
    composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("mood_calendar_card"))
    composeRule.onNodeWithText("Mood Calendar").assertIsDisplayed()

    composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("insights_back_button"))
    composeRule.onNodeWithTag("insights_back_button").performClick()
    assertEquals(1, backClicks)
  }

  @Test
  fun `insights habits screen displays time of day, frequency stats, and mode toggle`() {
    val now = 1_764_140_000_000L
    val entries = listOf(
      MoodEntry(id = "hb-1", mood = 4, timestamp = now),
      MoodEntry(id = "hb-2", mood = 5, timestamp = now - 3_600_000L),
      MoodEntry(id = "hb-3", mood = 3, timestamp = now - 86_400_000L),
    )
    val state = MoodyUiState(
      allEntries = entries,
      insightRange = InsightTimeRange.WEEK,
      timeOfDayMode = TimeOfDayMode.PART_OF_DAY,
      habitsAnalytics = MoodAnalytics.computeHabitsAnalytics(entries, InsightTimeRange.WEEK, TimeOfDayMode.PART_OF_DAY, now),
    )
    var toggledMode: TimeOfDayMode? = null

    composeRule.setContent {
      MoodyTheme(appearanceMode = AppearanceMode.DARK, palette = MoodyPalette.TEAL_OBSIDIAN) {
        InsightsHabitsScreen(
          uiState = state,
          onBack = {},
          onSelectRange = {},
          onSelectTimeOfDayMode = { toggledMode = it },
        )
      }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Habits & Time of Day").assertIsDisplayed()
    composeRule.onNodeWithTag("habits_stat_active_days").assertIsDisplayed()
    composeRule.onNodeWithTag("habits_stat_consistency").assertIsDisplayed()
    composeRule.onNodeWithTag("habits_stat_streak").assertIsDisplayed()
    composeRule.onNodeWithText("2/7").assertIsDisplayed()
    composeRule.onNodeWithText("29%").assertIsDisplayed()

    composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("habits_time_of_day_card"))
    composeRule.onNodeWithText("Time of Day").assertIsDisplayed()
    composeRule.onNodeWithTag("tod_mode_hourly").performClick()
    assertEquals(TimeOfDayMode.HOURLY, toggledMode)

    composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("habits_frequency_card"))
    composeRule.onNodeWithText("Logging Frequency").assertIsDisplayed()
    composeRule.onNodeWithText("3 entries").assertIsDisplayed()
    composeRule.onNodeWithText("1.5 logs").assertIsDisplayed()
  }

  @Test
  fun `insights tags screen renders empty state when no tags exist in period`() {
    val now = 1_764_140_000_000L
    val entries = listOf(MoodEntry(id = "plain", mood = 4, timestamp = now, tags = emptyList()))
    val state = MoodyUiState(
      allEntries = entries,
      insightRange = InsightTimeRange.WEEK,
      tagsAnalytics = MoodAnalytics.computeTagsAnalytics(entries, InsightTimeRange.WEEK, now),
    )

    composeRule.setContent {
      MoodyTheme(appearanceMode = AppearanceMode.DARK, palette = MoodyPalette.TEAL_OBSIDIAN) {
        InsightsTagsScreen(
          uiState = state,
          onBack = {},
          onSelectRange = {},
          onSelectTag = {},
        )
      }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Tags & Themes").assertIsDisplayed()
    composeRule.onNodeWithTag("tags_empty_state").assertIsDisplayed()
    composeRule.onNodeWithText("No tags recorded for this period.").assertIsDisplayed()
  }

  @Test
  fun `insights tags screen renders sized word cloud, tappable tag detail sheet, expandable ranked list, trends, and mood distribution`() {
    val now = 1_764_140_000_000L
    val entries = listOf(
      MoodEntry(id = "e1", mood = 2, timestamp = now, tags = listOf("Work", "tired", "coffee", "focus", "walk", "reading")),
      MoodEntry(id = "e2", mood = 3, timestamp = now - 3_600_000L, tags = listOf("work", "tired")),
      MoodEntry(id = "e3", mood = 4, timestamp = now - 86_400_000L, tags = listOf("work", "calm")),
      MoodEntry(id = "e4", mood = 5, timestamp = now - 2 * 86_400_000L, tags = listOf("work")),
      MoodEntry(id = "e5", mood = 1, timestamp = now - 9 * 86_400_000L, tags = listOf("work")),
    )
    val tagsSummary = MoodAnalytics.computeTagsAnalytics(entries, InsightTimeRange.WEEK, now)
    val state = MoodyUiState(
      allEntries = entries,
      insightRange = InsightTimeRange.WEEK,
      tagsAnalytics = tagsSummary,
      selectedInsightTagKey = "work",
    )
    val tappedTags = mutableListOf<String>()

    composeRule.setContent {
      MoodyTheme(appearanceMode = AppearanceMode.LIGHT, palette = MoodyPalette.SAGE_DAYLIGHT) {
        Box(modifier = Modifier.width(320.dp).height(600.dp)) {
          InsightsTagsScreen(
            uiState = state,
            onBack = {},
            onSelectRange = {},
            onSelectTag = { tappedTags.add(it) },
          )
        }
      }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("tags_word_cloud_card"))
    val workHeight = composeRule.onNodeWithTag("word_cloud_tag_work").fetchSemanticsNode().size.height
    val readingHeight = composeRule.onNodeWithTag("word_cloud_tag_reading").fetchSemanticsNode().size.height
    assertTrue("More frequent tag pill should be larger than infrequent tag pill", workHeight > readingHeight)

    composeRule.onNodeWithTag("word_cloud_tag_work").performClick()
    composeRule.waitForIdle()
    assertEquals("work", tappedTags.last())
    composeRule.onNodeWithTag("tag_detail_sheet").assertIsDisplayed()
    composeRule.onNodeWithTag("tag_detail_close_button").performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("tags_ranked_list_card"))
    composeRule.onNodeWithTag("ranked_tag_work").assertIsDisplayed()
    composeRule.onNodeWithTag("ranked_tag_reading").assertDoesNotExist()
    composeRule.onNodeWithTag("toggle_all_tags_button").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("ranked_tag_reading").assertIsDisplayed()

    composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("tags_trends_card"))
    composeRule.onNodeWithTag("trend_tag_work").assertIsDisplayed()

    composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("tags_mood_distribution_card"))
    composeRule.onNodeWithText("Mood Distribution by Tag").assertIsDisplayed()
    (1..5).forEach { score ->
      composeRule.onNodeWithTag("mood_dist_row_$score").assertIsDisplayed()
    }
  }

  @Test
  fun `tags analytics updates reactively when entries are edited or deleted in repository`() = runTest {
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
      val now = System.currentTimeMillis()
      val entry = MoodEntry(id = "react-1", mood = 4, timestamp = now, tags = listOf("calm", "walk"))
      repository.upsertEntry(entry)

      val initialTags = MoodAnalytics.computeTagsAnalytics(repository.allEntries.first(), InsightTimeRange.WEEK, now)
      assertEquals(2, initialTags.distinctTagCount)

      repository.saveEditedEntry(entry.copy(tags = listOf("energized")), updateDailySleep = false)
      val editedTags = MoodAnalytics.computeTagsAnalytics(repository.allEntries.first(), InsightTimeRange.WEEK, now)
      assertEquals(1, editedTags.distinctTagCount)
      assertEquals("energized", editedTags.topTags.single().normalizedTag)

      repository.deleteEntry(entry.id)
      val deletedTags = MoodAnalytics.computeTagsAnalytics(repository.allEntries.first(), InsightTimeRange.WEEK, now)
      assertFalse(deletedTags.hasAnyTags)
    } finally {
      db.close()
    }
  }

  @Test
  fun `insights tags screen shows insufficient data notice for sparse tags and renders across palettes`() {
    val now = 1_764_140_000_000L
    val entries = listOf(
      MoodEntry(id = "s1", mood = 4, timestamp = now, tags = listOf("travel")),
      MoodEntry(id = "s2", mood = 5, timestamp = now - 3_600_000L, tags = listOf("travel")),
    )
    val state = MoodyUiState(
      allEntries = entries,
      insightRange = InsightTimeRange.WEEK,
      tagsAnalytics = MoodAnalytics.computeTagsAnalytics(entries, InsightTimeRange.WEEK, now),
      selectedInsightTagKey = "travel",
    )

    composeRule.setContent {
      MoodyTheme(appearanceMode = AppearanceMode.DARK, palette = MoodyPalette.VIOLET_MIDNIGHT) {
        InsightsTagsScreen(
          uiState = state,
          onBack = {},
          onSelectRange = {},
          onSelectTag = {},
        )
      }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("tags_mood_distribution_card"))
    composeRule.onNodeWithTag("tag_mood_insufficient_data").performScrollTo().assertIsDisplayed()
  }
}
