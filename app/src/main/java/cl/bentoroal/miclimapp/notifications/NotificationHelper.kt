package cl.bentoroal.miclimapp.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import cl.bentoroal.miclimapp.R
import kotlin.random.Random

object NotificationHelper {

    const val CHANNEL_ID = "CLIMA_CHANNEL"

    fun createChannelIfNeeded(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alertas Climáticas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones de condiciones climáticas"
        }

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun showNotification(context: Context, message: String) {
        // ✅ Verifica permiso solo en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                Log.w("NotificationHelper", "❌ No se puede mostrar la notificación: falta POST_NOTIFICATIONS")
                return
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_weather)
            .setContentTitle("Alerta Climática")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(Random.nextInt(Int.MAX_VALUE), builder.build())
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "🚫 SecurityException al mostrar notificación", e)
        } catch (e: Exception) {
            Log.e("NotificationHelper", "🐛 Error inesperado al mostrar notificación", e)
        }
    }
}
