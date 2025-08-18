package cl.bentoroal.miclimapp.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WeatherWorkerScheduler {

    private const val MORNING_HOUR = 8
    private const val MORNING_MINUTE = 0
    private const val EVENING_HOUR = 20
    private const val EVENING_MINUTE = 0

    private const val WORK_TAG_MORNING = "WeatherWorker8AM"
    private const val WORK_TAG_EVENING = "WeatherWorker8PM"

    private fun calculateInitialDelay(targetHour: Int, targetMinute: Int): Long {
        val now = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (now.after(targetTime)) {
            targetTime.add(Calendar.DAY_OF_MONTH, 1)
        }
        val delay = targetTime.timeInMillis - now.timeInMillis
        Log.d("WeatherWorkerScheduler",
            "Calculated delay for $targetHour:$targetMinute is $delay ms (${delay / 60000} minutes)")
        return delay
    }

    fun scheduleDailyWorkers(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 1) Worker de la mañana (8:00 AM)
        val initialDelayMorning = calculateInitialDelay(MORNING_HOUR, MORNING_MINUTE)
        val morningRequest = PeriodicWorkRequestBuilder<WeatherWorker>(
            1, TimeUnit.DAYS,
            30, TimeUnit.MINUTES
        )
            .setInitialDelay(initialDelayMorning, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag(WORK_TAG_MORNING)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_TAG_MORNING,
            ExistingPeriodicWorkPolicy.KEEP, // en lugar de REPLACE
            morningRequest
        )

        Log.d("WeatherWorkerScheduler",
            "⏰ Scheduled $WORK_TAG_MORNING with initial delay ${initialDelayMorning / 60000} min")

        // 2) Worker de la noche (8:00 PM)
        val initialDelayEvening = calculateInitialDelay(EVENING_HOUR, EVENING_MINUTE)
        val eveningRequest = PeriodicWorkRequestBuilder<WeatherWorker>(
            1, TimeUnit.DAYS,
            30, TimeUnit.MINUTES
        )
            .setInitialDelay(initialDelayEvening, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag(WORK_TAG_EVENING)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_TAG_EVENING,
            ExistingPeriodicWorkPolicy.KEEP, // en lugar de REPLACE
            eveningRequest
        )

        Log.d("WeatherWorkerScheduler",
            "🌙 Scheduled $WORK_TAG_EVENING with initial delay ${initialDelayEvening / 60000} min")
    }
}
