package cl.bentoroal.miclimapp

import android.app.Application
import cl.bentoroal.miclimapp.worker.WeatherWorkerScheduler

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        WeatherWorkerScheduler.scheduleDailyWorkers(applicationContext)
    }
}