package cl.bentoroal.miclimapp.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class WorkerLauncherReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val hora = intent.getIntExtra("hora", -1)

        val request = OneTimeWorkRequestBuilder<WeatherWorker>()
            .setInputData(workDataOf("hora" to hora))
            .build()

        WorkManager.getInstance(context).enqueue(request)
        Log.d("WorkerLauncherReceiver", "Lanzando WeatherWorker para hora: $hora")
    }
}