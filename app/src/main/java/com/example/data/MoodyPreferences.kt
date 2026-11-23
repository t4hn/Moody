package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.moodyDataStore: DataStore<Preferences> by preferencesDataStore(name = "moody_prefs")

class MoodyPreferences(private val context: Context) {

  private object Keys {
    val DEMO_SEEDED = booleanPreferencesKey("demo_seeded")
  }

  val hasSeededDemo: Flow<Boolean> =
    context.moodyDataStore.data.map { prefs -> prefs[Keys.DEMO_SEEDED] ?: false }

  suspend fun markDemoSeeded() {
    context.moodyDataStore.edit { prefs ->
      prefs[Keys.DEMO_SEEDED] = true
    }
  }
}
