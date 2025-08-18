package cl.bentoroal.miclimapp.worker

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cl.bentoroal.miclimapp.api.RetrofitInstance
import cl.bentoroal.miclimapp.notifications.NotificationHelper
import cl.bentoroal.miclimapp.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.UnknownHostException
import java.util.*

class WeatherWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "WeatherWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "🚀 doWork START (CoroutineWorker)")

        try {
            NotificationHelper.createChannelIfNeeded(applicationContext)

            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lat = prefs.getFloat(KEY_SAVED_LAT, -999f).toDouble()
            val lon = prefs.getFloat(KEY_SAVED_LON, -999f).toDouble()

            // Validación de coordenadas válidas
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                Log.e(TAG, "❌ Coordenadas inválidas: lat=$lat, lon=$lon. Cancelando worker.")
                return@withContext Result.failure()
            }

            val tempMinAlert = Thresholds.getMinTemp(applicationContext)
            val windMaxAlert = Thresholds.getMaxWind(applicationContext)

            Log.d(TAG, "🌍 Llamando API con lat=$lat, lon=$lon")
            val response = RetrofitInstance.api.getDailyForecast(lat, lon)
            val daily = response.daily

            val isMorningWorker = tags.contains("WeatherWorker8AM")
            val todayIndex = 0
            val tomorrowIndex = 1

            val index = if (isMorningWorker) todayIndex else tomorrowIndex

            // Validar índices para evitar errores
            if (index >= daily.temperatureMin.size ||
                index >= daily.temperatureMax.size ||
                index >= daily.windSpeedMax.size) {
                Log.e(TAG, "❌ Índice fuera de rango para los datos diarios: $index")
                return@withContext Result.failure()
            }

            // Mensaje base según el día
            val baseMessage = if (isMorningWorker) {
                "☀️ Hoy: Min %.1f°C - Max %.1f°C".format(
                    daily.temperatureMin[todayIndex],
                    daily.temperatureMax[todayIndex]
                )
            } else {
                "☀️ Mañana: Min %.1f°C - Max %.1f°C".format(
                    daily.temperatureMin[tomorrowIndex],
                    daily.temperatureMax[tomorrowIndex]
                )
            }

            val alerts = mutableListOf<String>()

            // Evaluar helada
            if (daily.temperatureMin[index] <= tempMinAlert)
                alerts.add("❄️ Helada posible: %.1f°C".format(daily.temperatureMin[index]))

            // Evaluar viento
            val vientoMaxDia = daily.windSpeedMax[index]
            Log.d(TAG, "💨 Viento máximo del día [$index]: $vientoMaxDia km/h")

            if (vientoMaxDia >= windMaxAlert)
                alerts.add("🌬️ Viento fuerte: %.1f km/h".format(vientoMaxDia))

            // Mensaje final
            val finalMessage = if (alerts.isNotEmpty()) {
                "⚠️ Alertas:\n${alerts.joinToString("\n")}\n\n📋 Pronóstico:\n$baseMessage"
            } else {
                "📋 Pronóstico:\n$baseMessage"
            }


            NotificationHelper.showNotification(applicationContext, finalMessage)

            val log = buildString {
                appendLine("🕒 ${Date()}")
                appendLine("🎯 Worker: ${tags.joinToString()}")
                appendLine("📍 Coordenadas: lat=$lat, lon=$lon")
                appendLine("💨 Viento: %.1f km/h".format(daily.windSpeedMax[index]))
                appendLine("🌡️ Temperaturas: min=%.1f°C, max=%.1f°C".format(daily.temperatureMin[index], daily.temperatureMax[index]))
                if (alerts.isNotEmpty()) {
                    appendLine("⚠️ Alertas:")
                    alerts.forEach { appendLine("- $it") }
                }
                appendLine("📋 Pronóstico mostrado:")
                appendLine(baseMessage)
            }

            prefs.edit {
                putString("weather_worker_log", log)
                putString("last_notification_message", finalMessage)
                putLong("last_notification_time", System.currentTimeMillis())
            }


            Log.d(TAG, "✅ Notificación enviada con éxito.")
            Result.success()

        } catch (e: UnknownHostException) {
            Log.w(TAG, "🌐 Sin conexión de red, se reintentará.", e)
            Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inesperado en doWork()", e)
            Result.failure()
        }
    }

}
