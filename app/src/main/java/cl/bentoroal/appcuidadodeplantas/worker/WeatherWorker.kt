package cl.bentoroal.appcuidadodeplantas.worker

import android.content.Context
import android.annotation.SuppressLint
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cl.bentoroal.appcuidadodeplantas.R
import cl.bentoroal.appcuidadodeplantas.api.RetrofitInstance
import cl.bentoroal.appcuidadodeplantas.notifications.NotificationHelper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class WeatherWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    // Se utiliza SuppressLint para desactivar la advertencia "MissingPermission".
    // La verificación del permiso POST_NOTIFICATIONS ya se realiza dentro de NotificationHelper,
    // y el llamado a notify() está envuelto en try-catch. Esta anotación evita que Lint
    // marque el uso como inseguro, manteniendo el código limpio sin duplicar verificaciones.
    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        NotificationHelper.createChannelIfNeeded(applicationContext)

        val lat = -38.74   // Temuco 🌍
        val lon = -72.59
        val apiKey = applicationContext.getString(R.string.weather_api_key)

        return try {
            val response = RetrofitInstance.api.getForecast(lat, lon, apiKey = apiKey)
            val prefs = applicationContext.getSharedPreferences("clima_prefs", Context.MODE_PRIVATE)

            val tempMinAlert = prefs.getFloat("temp_min_alert", 0f).toDouble()
            val windMaxAlert = prefs.getFloat("wind_max_alert", 30f).toDouble()

            val todayBlocks = response.list.filter { item ->
                val localTime = Instant.ofEpochSecond(item.dt).atZone(ZoneId.systemDefault())
                localTime.toLocalDate() == LocalDate.now()
            }

            val minTemp = todayBlocks.minOfOrNull { it.main.temp_min }
            val maxTemp = todayBlocks.maxOfOrNull { it.main.temp_max }
            val maxWind = todayBlocks.maxOfOrNull { it.wind.speed }

            val alerts = mutableListOf<String>()

            if (minTemp != null && minTemp <= tempMinAlert) {
                alerts.add("❄️ Helada posible: ${minTemp} °C")
            }
            if (maxWind != null && maxWind >= windMaxAlert) {
                alerts.add("🌬️ Viento fuerte: ${maxWind} km/h")
            }

            val baseMessage = "🌤️ Clima para hoy: ${minTemp}°C - ${maxTemp}°C"
            val finalMessage = if (alerts.isNotEmpty()) alerts.joinToString("\n") else baseMessage

            NotificationHelper.showNotification(applicationContext, finalMessage)
            Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}