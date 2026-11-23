package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [MoodEntryEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class MoodyDatabase : RoomDatabase() {
  abstract fun moodEntryDao(): MoodEntryDao

  companion object {
    @Volatile
    private var INSTANCE: MoodyDatabase? = null

    fun getInstance(context: Context): MoodyDatabase =
      INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          MoodyDatabase::class.java,
          "moody_journal.db",
        ).build().also { INSTANCE = it }
      }
  }
}
