package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AppearanceMode
import com.example.ui.theme.MoodyPalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.moodyDataStore: DataStore<Preferences> by preferencesDataStore(name = "moody_prefs")

class MoodyPreferences(private val context: Context) {

  private object Keys {
    val APPEARANCE_MODE = stringPreferencesKey("appearance_mode")
    val COLOR_PALETTE = stringPreferencesKey("color_palette")
  }

  val appearanceMode: Flow<AppearanceMode> =
    context.moodyDataStore.data.map { prefs ->
      AppearanceMode.fromNameOrDefault(prefs[Keys.APPEARANCE_MODE])
    }

  val colorPalette: Flow<MoodyPalette> =
    context.moodyDataStore.data.map { prefs ->
      MoodyPalette.fromNameOrDefault(prefs[Keys.COLOR_PALETTE])
    }

  suspend fun setAppearanceMode(mode: AppearanceMode) {
    context.moodyDataStore.edit { prefs ->
      prefs[Keys.APPEARANCE_MODE] = mode.name
    }
  }

  suspend fun setColorPalette(palette: MoodyPalette) {
    context.moodyDataStore.edit { prefs ->
      prefs[Keys.COLOR_PALETTE] = palette.name
    }
  }
}
