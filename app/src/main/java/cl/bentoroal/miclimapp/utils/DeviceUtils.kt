package cl.bentoroal.miclimapp.utils

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object DeviceUtils {

    fun showBatterySettings(context: Context) {
        val manufacturer = Build.MANUFACTURER.lowercase()

        val restrictedBrands = listOf("xiaomi", "huawei", "oppo", "vivo", "realme")

        if (restrictedBrands.any { manufacturer.contains(it) }) {
            AlertDialog.Builder(context)
                .setTitle("Permisos de batería")
                .setMessage(
                    "Para que la app funcione correctamente en segundo plano, " +
                            "por favor permite que la app no sea optimizada por la batería y habilita el inicio automático en los ajustes de tu teléfono."
                )
                .setPositiveButton("Abrir Ajustes") { _, _ ->
                    try {
                        val intent = Intent(Settings.ACTION_SETTINGS)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_SETTINGS)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } else {
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
        }
    }

}
