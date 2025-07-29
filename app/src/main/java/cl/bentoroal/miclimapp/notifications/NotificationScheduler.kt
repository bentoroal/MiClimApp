package cl.bentoroal.miclimapp.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import cl.bentoroal.miclimapp.worker.WorkerLauncherReceiver
import java.util.Calendar

object NotificationScheduler {

    // Configura el WeatherWorker para ejecutarse a las 08:00 y 20:00 todos los días
    fun scheduleDailyWeatherWorker(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val hours = listOf(8 to 1001, 20 to 1002) // hora, requestCode únicos por horario

        hours.forEach { (hour, requestCode) ->
            val intent = Intent(context, WorkerLauncherReceiver::class.java).apply {
                putExtra("hora", hour) // 8 o 20
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(Calendar.getInstance())) add(Calendar.DATE, 1) // evitar ejecución retroactiva
            }

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
    }
}
