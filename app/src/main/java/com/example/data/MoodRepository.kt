package com.example.data

import com.example.model.DailyContext
import com.example.model.MoodEntry
import com.example.ui.theme.AppearanceMode
import com.example.ui.theme.MoodyPalette
import com.example.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class MoodRepository(
  private val dao: MoodEntryDao,
  private val dailyContextDao: DailyContextDao,
  private val preferences: MoodyPreferences,
) {
  val dailyContexts: Flow<Map<String, DailyContext>> =
    dailyContextDao.observeAll().map { entities ->
      entities.associate { it.dayKey to it.toDomain() }
    }

  val allEntries: Flow<List<MoodEntry>> =
    combine(
      dao.observeAllEntries(),
      dailyContexts,
    ) { entities, contextMap ->
      entities.map { entity ->
        val dayKey = DateTimeUtils.formatFileDate(entity.timestamp)
        entity.toDomain(dailyContext = contextMap[dayKey])
      }
    }

  val appearanceMode: Flow<AppearanceMode> = preferences.appearanceMode
  val colorPalette: Flow<MoodyPalette> = preferences.colorPalette

  suspend fun setAppearanceMode(mode: AppearanceMode) {
    preferences.setAppearanceMode(mode)
  }

  suspend fun setColorPalette(palette: MoodyPalette) {
    preferences.setColorPalette(palette)
  }

  suspend fun upsertEntry(entry: MoodEntry) {
    dao.upsert(entry.copy(updatedAt = System.currentTimeMillis()).toEntity())
  }

  suspend fun saveEditedEntry(
    entry: MoodEntry,
    updateDailySleep: Boolean,
  ) {
    val now = System.currentTimeMillis()
    dao.upsert(entry.copy(updatedAt = now).toEntity())
    if (updateDailySleep) {
      val dayKey = DateTimeUtils.formatFileDate(entry.timestamp)
      if (entry.sleepDurationMinutes == null && entry.sleepQuality == null) {
        dailyContextDao.deleteByDayKey(dayKey)
      } else {
        dailyContextDao.upsert(
          DailyContext(
            dayKey = dayKey,
            sleepDurationMinutes = entry.sleepDurationMinutes,
            sleepQuality = entry.sleepQuality,
            updatedAt = now,
          ).toEntity(),
        )
      }
    }
  }

  suspend fun deleteEntry(id: String) {
    dao.deleteById(id)
  }

  suspend fun deleteAllEntries() {
    dao.deleteAll()
    dailyContextDao.deleteAll()
  }

  suspend fun mergeImportedEntries(
    entries: List<MoodEntry>,
    importedDailyContexts: List<DailyContext>,
  ) {
    dao.upsertAll(entries.map { it.toEntity() })
    if (importedDailyContexts.isNotEmpty()) {
      val existingMap = dailyContextDao.getAllOnce().associateBy { it.dayKey }
      val merged = importedDailyContexts.mapNotNull { incoming ->
        if (!incoming.hasAnyData) return@mapNotNull null
        val existing = existingMap[incoming.dayKey]
        DailyContext(
          dayKey = incoming.dayKey,
          sleepDurationMinutes = incoming.sleepDurationMinutes ?: existing?.sleepDurationMinutes,
          sleepQuality = incoming.sleepQuality ?: existing?.sleepQuality,
          updatedAt = System.currentTimeMillis(),
        ).toEntity()
      }
      if (merged.isNotEmpty()) {
        dailyContextDao.upsertAll(merged)
      }
    }
  }

  suspend fun replaceWithImportedEntries(
    entries: List<MoodEntry>,
    importedDailyContexts: List<DailyContext>,
  ) {
    dao.replaceAll(entries.map { it.toEntity() })
    dailyContextDao.replaceAll(
      importedDailyContexts.filter { it.hasAnyData }.map { it.toEntity() },
    )
  }
}
