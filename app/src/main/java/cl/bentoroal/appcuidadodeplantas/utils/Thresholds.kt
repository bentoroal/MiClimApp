package cl.bentoroal.appcuidadodeplantas.utils

import android.content.Context

object Thresholds {
    private const val PREF_NAME = "clima_prefs"
    private const val KEY_TEMP_MIN = "temp_min_alert"
    private const val KEY_WIND_MAX = "wind_max_alert"

    fun getMinTemp(context: Context): Float {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_TEMP_MIN, 0f)
    }

    fun getMaxWind(context: Context): Float {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_WIND_MAX, 30f)
    }
}