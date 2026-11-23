package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [
    MoodEntryEntity::class,
    DailyContextEntity::class,
  ],
  version = 2,
  exportSchema = false,
)
abstract class MoodyDatabase : RoomDatabase() {
  abstract fun moodEntryDao(): MoodEntryDao
  abstract fun dailyContextDao(): DailyContextDao

  companion object {
    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        // Safely migrate mood_entries from v1 to v2:
        // - Remove seeded demo entries (isDemo = 1) while preserving all genuine user entries (isDemo = 0).
        // - Drop the isDemo column via table rebuild (SQLite < 3.35 compatibility across API 24+).
        // - Add nullable contextual columns (energyLevel, stressLevel, anxietyLevel, physicalActivity).
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `mood_entries_new` (
            `id` TEXT NOT NULL,
            `mood` INTEGER NOT NULL,
            `timestamp` INTEGER NOT NULL,
            `note` TEXT NOT NULL,
            `tagsJson` TEXT NOT NULL,
            `createdAt` INTEGER NOT NULL,
            `updatedAt` INTEGER NOT NULL,
            `energyLevel` INTEGER,
            `stressLevel` INTEGER,
            `anxietyLevel` INTEGER,
            `physicalActivity` TEXT,
            PRIMARY KEY(`id`)
          )
          """.trimIndent(),
        )
        db.execSQL(
          """
          INSERT INTO `mood_entries_new` (
            `id`, `mood`, `timestamp`, `note`, `tagsJson`, `createdAt`, `updatedAt`,
            `energyLevel`, `stressLevel`, `anxietyLevel`, `physicalActivity`
          )
          SELECT
            `id`, `mood`, `timestamp`, `note`, `tagsJson`, `createdAt`, `updatedAt`,
            NULL, NULL, NULL, NULL
          FROM `mood_entries`
          WHERE `isDemo` = 0
          """.trimIndent(),
        )
        db.execSQL("DROP TABLE `mood_entries`")
        db.execSQL("ALTER TABLE `mood_entries_new` RENAME TO `mood_entries`")

        // Create daily_context table for daily contextual measurements (sleep duration & quality).
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `daily_context` (
            `dayKey` TEXT NOT NULL,
            `sleepDurationMinutes` INTEGER,
            `sleepQuality` INTEGER,
            `updatedAt` INTEGER NOT NULL,
            PRIMARY KEY(`dayKey`)
          )
          """.trimIndent(),
        )
      }
    }

    @Volatile
    private var INSTANCE: MoodyDatabase? = null

    fun getInstance(context: Context): MoodyDatabase =
      INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          MoodyDatabase::class.java,
          "moody_journal.db",
        )
          .addMigrations(MIGRATION_1_2)
          .build()
          .also { INSTANCE = it }
      }
  }
}
