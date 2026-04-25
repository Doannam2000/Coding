package com.nantcompany.clipy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nantcompany.clipy.model.AppLanguage
import com.nantcompany.clipy.model.CropRatio
import com.nantcompany.clipy.model.Mp4Quality
import com.nantcompany.clipy.model.OutputFormat
import com.nantcompany.clipy.model.OutputResolution
import com.nantcompany.clipy.model.SaveBehavior
import com.nantcompany.clipy.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "clipy_preferences")

class PreferenceRepository(private val context: Context) {
  val preferences: Flow<UserPreferences> =
    context.dataStore.data.map { prefs ->
      UserPreferences(
        languageCode = prefs[LANGUAGE] ?: AppLanguage.English.code,
        defaultGifFps = prefs[GIF_FPS] ?: 18,
        defaultGifResolution = prefs[GIF_RESOLUTION] ?: "720p",
        defaultMp4Quality = Mp4Quality.valueOf(prefs[MP4_QUALITY] ?: Mp4Quality.Balanced.name),
        defaultOutputFormat = OutputFormat.valueOf(prefs[OUTPUT_FORMAT] ?: OutputFormat.MP4.name),
        defaultOutputResolution = OutputResolution.valueOf(prefs[OUTPUT_RESOLUTION] ?: OutputResolution.P1080.name),
        defaultOutputFps = prefs[OUTPUT_FPS] ?: 30,
        defaultMuteEnabled = prefs[MUTE] ?: false,
        defaultCropRatio = CropRatio.valueOf(prefs[CROP_RATIO] ?: CropRatio.Story.name),
        saveBehavior = SaveBehavior.valueOf(prefs[SAVE_BEHAVIOR] ?: SaveBehavior.AppFolder.name),
        onboardingCompleted = prefs[ONBOARDING] ?: false,
      )
    }

  suspend fun setOnboardingCompleted(completed: Boolean) {
    context.dataStore.edit { it[ONBOARDING] = completed }
  }

  suspend fun setLanguage(language: AppLanguage) {
    context.dataStore.edit { it[LANGUAGE] = language.code }
  }

  suspend fun updateDefaults(
    gifFps: Int,
    gifResolution: String,
    quality: Mp4Quality,
    outputFormat: OutputFormat,
    outputResolution: OutputResolution,
    outputFps: Int,
    cropRatio: CropRatio,
    saveBehavior: SaveBehavior,
    defaultMuteEnabled: Boolean,
  ) {
    context.dataStore.edit {
      it[GIF_FPS] = gifFps
      it[GIF_RESOLUTION] = gifResolution
      it[MP4_QUALITY] = quality.name
      it[OUTPUT_FORMAT] = outputFormat.name
      it[OUTPUT_RESOLUTION] = outputResolution.name
      it[OUTPUT_FPS] = outputFps
      it[CROP_RATIO] = cropRatio.name
      it[SAVE_BEHAVIOR] = saveBehavior.name
      it[MUTE] = defaultMuteEnabled
    }
  }

  companion object {
    private val LANGUAGE = stringPreferencesKey("language")
    private val GIF_FPS = intPreferencesKey("gif_fps")
    private val GIF_RESOLUTION = stringPreferencesKey("gif_resolution")
    private val MP4_QUALITY = stringPreferencesKey("mp4_quality")
    private val OUTPUT_FORMAT = stringPreferencesKey("output_format")
    private val OUTPUT_RESOLUTION = stringPreferencesKey("output_resolution")
    private val OUTPUT_FPS = intPreferencesKey("output_fps")
    private val MUTE = booleanPreferencesKey("default_mute")
    private val CROP_RATIO = stringPreferencesKey("crop_ratio")
    private val SAVE_BEHAVIOR = stringPreferencesKey("save_behavior")
    private val ONBOARDING = booleanPreferencesKey("onboarding_completed")
  }
}
