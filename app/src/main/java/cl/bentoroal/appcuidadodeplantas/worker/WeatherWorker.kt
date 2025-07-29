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
        // Crear el canal de notificación
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
            val tomorrowIndex = 1
            val todayIndex = 0
            // Variables para el dia actual:
            val minTempToday = response.daily.temperatureMin[todayIndex]
            val maxTempToday = response.daily.temperatureMax[todayIndex]
            val maxWindToday = response.daily.windSpeedMax[todayIndex]

            // Variables para el dia siguiente:
            val minTempTomorrow = response.daily.temperatureMin[tomorrowIndex]
            val maxTempTomorrow = response.daily.temperatureMax[tomorrowIndex]
            val maxWindTomorrow = response.daily.windSpeedMax[tomorrowIndex]

            // 4. Construye alertas
            val alerts = mutableListOf<String>()
            if (minTempTomorrow <= tempMinAlert) {
                alerts.add("❄️ Helada posible para mañana: ${"%.1f".format(minTempTomorrow)} °C")
            }
            if (maxWindTomorrow >= windMaxAlert) {
                alerts.add("🌬️ Mañana vientos de hasta: ${"%.1f".format(maxWindTomorrow)} km/h")
            }

            // 5. Notificación
            val baseMessageToday = "🌤️ Clima para hoy: ${"%.1f".format(minTempTomorrow)}°C - ${"%.1f".format(maxTempTomorrow)}°C"
            val finalMessageToday = if (alerts.isNotEmpty()) alerts.joinToString("\n") else baseMessageToday
            val baseMessageTomorrow = "🌞 Clima para mañana: ${"%.1f".format(minTempTomorrow)}°C - ${"%.1f".format(maxTempTomorrow)}°C"
            val finalMessageTomorrow = if (alerts.isNotEmpty()) alerts.joinToString("\n") else baseMessageTomorrow

            NotificationHelper.showNotification(applicationContext, finalMessageToday)
            NotificationHelper.showNotification(applicationContext, finalMessageTomorrow)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}