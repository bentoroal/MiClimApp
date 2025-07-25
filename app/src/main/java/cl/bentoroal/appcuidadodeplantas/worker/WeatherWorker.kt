package cl.bentoroal.appcuidadodeplantas.worker

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cl.bentoroal.appcuidadodeplantas.api.RetrofitInstance
import cl.bentoroal.appcuidadodeplantas.notifications.NotificationHelper
import cl.bentoroal.appcuidadodeplantas.utils.Thresholds

class WeatherWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val PREFS_NAME    = "clima_prefs"
        private const val KEY_SAVED_LAT = "saved_lat"
        private const val KEY_SAVED_LON = "saved_lon"
    }

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        // Asegúrate de crear el canal de notificación
        NotificationHelper.createChannelIfNeeded(applicationContext)

        // 1. Lee las coords de SharedPreferences (o usa el default de Temuco)
        val prefs = applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultLat = -38.74f
        val defaultLon = -72.59f
        val lat = prefs.getFloat(KEY_SAVED_LAT, defaultLat).toDouble()
        val lon = prefs.getFloat(KEY_SAVED_LON, defaultLon).toDouble()

        // 2. Umbrales
        val tempMinAlert = Thresholds.getMinTemp(applicationContext)
        val windMaxAlert = Thresholds.getMaxWind(applicationContext)

        return try {
            // 3. Llamada a la API
            val response = RetrofitInstance.api.getDailyForecast(lat, lon)
            val todayIndex = 0

            val minTemp = response.daily.temperature_2m_min[todayIndex]
            val maxTemp = response.daily.temperature_2m_max[todayIndex]
            val maxWind = response.daily.wind_speed_10m_max[todayIndex]

            // 4. Construye alertas
            val alerts = mutableListOf<String>()
            if (minTemp <= tempMinAlert) {
                alerts.add("❄️ Helada posible: ${"%.1f".format(minTemp)} °C")
            }
            if (maxWind >= windMaxAlert) {
                alerts.add("🌬️ Viento fuerte: ${"%.1f".format(maxWind)} km/h")
            }

            // 5. Notificación
            val baseMessage = "🌤️ Clima para hoy: ${"%.1f".format(minTemp)}°C - ${"%.1f".format(maxTemp)}°C"
            val finalMessage = if (alerts.isNotEmpty()) alerts.joinToString("\n") else baseMessage
            NotificationHelper.showNotification(applicationContext, finalMessage)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}