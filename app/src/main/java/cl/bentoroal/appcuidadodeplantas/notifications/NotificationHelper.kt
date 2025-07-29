package cl.bentoroal.appcuidadodeplantas.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlin.random.Random
import cl.bentoroal.appcuidadodeplantas.R

object NotificationHelper {

    const val CHANNEL_ID = "CLIMA_CHANNEL"

    fun createChannelIfNeeded(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alertas Climáticas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones de condiciones climaticas"
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showNotification(context: Context, message: String) {
        // 🌱 Verificar permiso solo en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                println("⚠️ No se puede mostrar notificación: falta permiso POST_NOTIFICATIONS.")
                return
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_weather)
            .setContentTitle("Alerta Climatica")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(Random.nextInt(Int.MAX_VALUE), builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
            println("🚫 No se pudo mostrar la notificación debido a un error de permisos.")
        } catch (e: Exception) {
            e.printStackTrace()
            println("🐛 Error inesperado al mostrar notificación.")
        }
    }
}