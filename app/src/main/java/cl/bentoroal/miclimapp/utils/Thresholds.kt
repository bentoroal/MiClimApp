package cl.bentoroal.miclimapp.utils

import android.content.Context
import androidx.core.content.edit

object Thresholds {
    private const val PREF_NAME = "clima_prefs"
    private const val KEY_TEMP_MIN = "temp_min_alert"
    private const val KEY_WIND_MAX = "wind_max_alert"

    fun getMinTemp(context: Context): Float {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return try {
            prefs.getFloat(KEY_TEMP_MIN, 0f)
        } catch (e: ClassCastException) {
            val intValue = prefs.getInt(KEY_TEMP_MIN, 0)
            // Opcional: migrar el valor correctamente
            prefs.edit { putFloat(KEY_TEMP_MIN, intValue.toFloat()) }
            intValue.toFloat()
        }
    }

    fun getMaxWind(context: Context): Float {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return try {
            prefs.getFloat(KEY_WIND_MAX, 30f)
        } catch (e: ClassCastException) {
            val intValue = prefs.getInt(KEY_WIND_MAX, 30)
            // Opcional: migrar el valor correctamente
            prefs.edit { putFloat(KEY_WIND_MAX, intValue.toFloat()) }
            intValue.toFloat()

        }
    }
}