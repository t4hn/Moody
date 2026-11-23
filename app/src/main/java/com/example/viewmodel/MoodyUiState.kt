package com.example.viewmodel

import com.example.model.DailyContext
import com.example.model.MoodEntry
import com.example.ui.theme.AppearanceMode
import com.example.ui.theme.MoodyPalette

data class QuickLogToastState(
  val entry: MoodEntry,
  val message: String,
)

data class ImportDialogState(
  val validEntries: List<MoodEntry>? = null,
  val validDailyContexts: List<DailyContext> = emptyList(),
  val errorMessage: String? = null,
)

data class MoodyUiState(
  val allEntries: List<MoodEntry> = emptyList(),
  val todayEntries: List<MoodEntry> = emptyList(),
  val dailyContexts: Map<String, DailyContext> = emptyMap(),
  val customQuickTimestamp: Long? = null,
  val lastLoggedPulseId: String? = null,
  val quickToast: QuickLogToastState? = null,
  val editingEntry: MoodEntry? = null,
  val suggestedTags: List<String> = emptyList(),
  val allUsedTags: List<String> = emptyList(),

  // Appearance
  val appearanceMode: AppearanceMode = AppearanceMode.DARK,
  val colorPalette: MoodyPalette = MoodyPalette.TEAL_OBSIDIAN,

  // Insights
  val stats: MoodStats = MoodStats(null, 0, 0, 0),
  val insightRange: InsightTimeRange = InsightTimeRange.WEEK,
  val lineGraphPoints: List<MoodGraphPoint> = emptyList(),
  val heatmapCells: List<HeatmapDayCell> = emptyList(),
  val selectedHeatmapDay: HeatmapDayCell? = null,
  val timeOfDayMode: TimeOfDayMode = TimeOfDayMode.PART_OF_DAY,
  val timeOfDayBuckets: List<TimeOfDayBucket> = emptyList(),
  val sleepAnalytics: SleepAnalyticsSummary = SleepAnalyticsSummary(),

  // History
  val searchQuery: String = "",
  val selectedFilterTags: Set<String> = emptySet(),
  val dateFilter: HistoryDateFilter = HistoryDateFilter.ALL,
  val customFilterDayMillis: Long? = null,
  val filteredHistoryEntries: List<MoodEntry> = emptyList(),

  // Settings / Data feedback
  val importDialogState: ImportDialogState? = null,
  val statusMessage: String? = null,
)
