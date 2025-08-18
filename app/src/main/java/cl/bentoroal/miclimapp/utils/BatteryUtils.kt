package cl.bentoroal.miclimapp.utils

import android.content.Context
import android.os.Build
import android.os.PowerManager

// Funcion si detecta si esta activado el ahorro de energia, que impide mostrar notificaciones diarias de clima
fun isBatteryOptimizationEnabled(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return !pm.isIgnoringBatteryOptimizations(context.packageName)
    }
    return false // versiones < Android 6 no tienen Doze
}
