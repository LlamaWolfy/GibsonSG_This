package com.pulsewave.visualizer.data

import android.content.Context
import com.pulsewave.visualizer.ui.settings.ColorTheme
import com.pulsewave.visualizer.ui.settings.VisualizerPreset
import com.pulsewave.visualizer.ui.settings.VisualizerSettings
import com.pulsewave.visualizer.ui.settings.VisualizerStyle
import org.json.JSONArray
import org.json.JSONObject

/** Persists named [VisualizerPreset]s to SharedPreferences as a JSON array. */
class PresetRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadPresets(): List<VisualizerPreset> {
        val raw = prefs.getString(KEY_PRESETS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i -> array.optJSONObject(i)?.toPresetOrNull() }
        }.getOrElse { emptyList() }
    }

    fun savePresets(presets: List<VisualizerPreset>) {
        val array = JSONArray()
        presets.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_PRESETS, array.toString()).apply()
    }

    private fun VisualizerPreset.toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("style", settings.style.name)
        put("colorTheme", settings.colorTheme.name)
        put("sensitivity", settings.sensitivity.toDouble())
        put("smoothing", settings.smoothing.toDouble())
        put("mirror", settings.mirror)
        put("density", settings.density.toDouble())
        put("beatFlash", settings.beatFlash)
        put("showAlbumArt", settings.showAlbumArt)
    }

    private fun JSONObject.toPresetOrNull(): VisualizerPreset? = runCatching {
        VisualizerPreset(
            name = getString("name"),
            settings = VisualizerSettings(
                style = VisualizerStyle.valueOf(getString("style")),
                colorTheme = ColorTheme.valueOf(getString("colorTheme")),
                sensitivity = optDouble("sensitivity", 1.0).toFloat(),
                smoothing = optDouble("smoothing", 0.55).toFloat(),
                mirror = optBoolean("mirror", true),
                density = optDouble("density", 1.0).toFloat(),
                beatFlash = optBoolean("beatFlash", true),
                showAlbumArt = optBoolean("showAlbumArt", true),
            ),
        )
    }.getOrNull()

    private companion object {
        const val PREFS_NAME = "pulsewave_presets"
        const val KEY_PRESETS = "presets"
    }
}
