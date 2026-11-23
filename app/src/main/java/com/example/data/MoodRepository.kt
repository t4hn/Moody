package com.example.data

import com.example.model.MoodEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class MoodRepository(
  private val dao: MoodEntryDao,
  private val preferences: MoodyPreferences,
) {
  val allEntries: Flow<List<MoodEntry>> =
    dao.observeAllEntries().map { entities -> entities.map { it.toDomain() } }

  val demoEntryCount: Flow<Int> = dao.observeDemoCount()

  suspend fun seedDemoDataIfFirstLaunch() {
    val alreadySeeded = preferences.hasSeededDemo.first()
    if (!alreadySeeded) {
      if (dao.countEntries() == 0) {
        val seeds = DemoDataSeeder.createSeedEntries().map { it.toEntity() }
        dao.upsertAll(seeds)
      }
      preferences.markDemoSeeded()
    }
  }

  suspend fun restoreDemoData() {
    val seeds = DemoDataSeeder.createSeedEntries().map { it.toEntity() }
    dao.upsertAll(seeds)
    preferences.markDemoSeeded()
  }

  suspend fun upsertEntry(entry: MoodEntry) {
    dao.upsert(entry.copy(updatedAt = System.currentTimeMillis()).toEntity())
  }

  suspend fun deleteEntry(id: String) {
    dao.deleteById(id)
  }

  suspend fun clearDemoData() {
    dao.deleteDemoEntries()
  }

  suspend fun deleteAllEntries() {
    dao.deleteAll()
  }

  suspend fun mergeImportedEntries(entries: List<MoodEntry>) {
    dao.upsertAll(entries.map { it.toEntity() })
  }

  suspend fun replaceWithImportedEntries(entries: List<MoodEntry>) {
    dao.replaceAll(entries.map { it.toEntity() })
  }
}
