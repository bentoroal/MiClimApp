package cl.bentoroal.appcuidadodeplantas

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import cl.bentoroal.appcuidadodeplantas.worker.WeatherWorker
import java.util.concurrent.TimeUnit
import androidx.navigation.ui.setupWithNavController

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 🔄 Conectar navegación inferior con fragments
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as androidx.navigation.fragment.NavHostFragment

        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setupWithNavController(navController)

        // 🌀 Iniciar el worker que consulta clima cada 6 horas
        scheduleWeatherWorker()
    }

    private fun scheduleWeatherWorker() {
        val request = PeriodicWorkRequestBuilder<WeatherWorker>(
            6, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "WeatherMonitor",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}