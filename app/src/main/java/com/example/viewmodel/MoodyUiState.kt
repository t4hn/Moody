package com.example.viewmodel

import com.example.model.MoodEntry

data class QuickLogToastState(
  val entry: MoodEntry,
  val message: String,
)

data class ImportDialogState(
  val validEntries: List<MoodEntry>? = null,
  val errorMessage: String? = null,
)

data class MoodyUiState(
  val allEntries: List<MoodEntry> = emptyList(),
  val todayEntries: List<MoodEntry> = emptyList(),
  val demoEntryCount: Int = 0,
  val customQuickTimestamp: Long? = null,
  val lastLoggedPulseId: String? = null,
  val quickToast: QuickLogToastState? = null,
  val editingEntry: MoodEntry? = null,
  val suggestedTags: List<String> = emptyList(),
  val allUsedTags: List<String> = emptyList(),

  // Insights
  val stats: MoodStats = MoodStats(null, 0, 0, 0),
  val insightRange: InsightTimeRange = InsightTimeRange.WEEK,
  val lineGraphPoints: List<MoodGraphPoint> = emptyList(),
  val heatmapCells: List<HeatmapDayCell> = emptyList(),
  val selectedHeatmapDay: HeatmapDayCell? = null,
  val timeOfDayMode: TimeOfDayMode = TimeOfDayMode.PART_OF_DAY,
  val timeOfDayBuckets: List<TimeOfDayBucket> = emptyList(),

  // History
  val searchQuery: String = "",
  val selectedFilterTags: Set<String> = emptySet(),
  val dateFilter: HistoryDateFilter = HistoryDateFilter.ALL,
  val customFilterDayMillis: Long? = null,
  val filteredHistoryEntries: List<MoodEntry> = emptyList(),

  // Data tab feedback
  val importDialogState: ImportDialogState? = null,
  val statusMessage: String? = null,
)
