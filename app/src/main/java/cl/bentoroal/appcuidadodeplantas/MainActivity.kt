package cl.bentoroal.appcuidadodeplantas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.lifecycle.lifecycleScope
import cl.bentoroal.appcuidadodeplantas.api.RetrofitInstance
import cl.bentoroal.appcuidadodeplantas.ui.ConfigurationActivity
import cl.bentoroal.appcuidadodeplantas.worker.WeatherWorker
import cl.bentoroal.appcuidadodeplantas.utils.ForecastUtils
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scheduleWeatherWorker() // 🌤️ Encolamos el worker al iniciar

        val btnConfig = findViewById<Button>(R.id.btnConfiguration)
        btnConfig.setOnClickListener {
            val intent = Intent(this, ConfigurationActivity::class.java)
            startActivity(intent)
        }

        val cardResumen = findViewById<View>(R.id.cardResumenUmbrales)
        cardResumen.alpha = 0f
        cardResumen.translationY = 80f

        cardResumen.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(300)
            .start()

        val txtResumen = findViewById<TextView>(R.id.txtResumenUmbrales)
        txtResumen.alpha = 0f
        txtResumen.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(700)
            .start()
    }

    override fun onResume() {
        super.onResume()

        val prefs = getSharedPreferences("clima_prefs", MODE_PRIVATE)
        val temp = prefs.getFloat("temp_min_alert", 0f)
        val wind = prefs.getFloat("wind_max_alert", 30f)

        val txtResumen = findViewById<TextView>(R.id.txtResumenUmbrales)
        txtResumen.text = "🌡️ Alerta de helada: bajo ${temp} °C\n💨 Alerta de viento: sobre ${wind} km/h"

        val cardResumen = findViewById<View>(R.id.cardResumenUmbrales)
        cardResumen.scaleX = 0.9f
        cardResumen.scaleY = 0.9f
        cardResumen.alpha = 0f

        cardResumen.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(100)
            .start()

        cargarPronosticoManana()

    }

    private fun scheduleWeatherWorker() {
        val request = PeriodicWorkRequestBuilder<WeatherWorker>(
            6, TimeUnit.HOURS // ⏰ cada 6 horas
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "WeatherMonitor",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
    private fun cargarPronosticoManana() {
        lifecycleScope.launch {
            try {
                val forecast = RetrofitInstance.api.getForecast(
                    lat = -38.74,
                    lon = -72.59,
                    apiKey = getString(R.string.weather_api_key)
                )

                val bloques = forecast.list
                val hoy = LocalDate.now()
                val manana = hoy.plusDays(1)

                val resumenHoy = ForecastUtils.obtenerResumenPara(hoy, bloques)
                val resumenManana = ForecastUtils.obtenerResumenPara(manana, bloques)

                val txtHoy = findViewById<TextView>(R.id.txtPronosticoHoy)
                val txtManana = findViewById<TextView>(R.id.txtPronosticoManana)
                val card = findViewById<View>(R.id.cardPronosticoManana)

                txtHoy.text = "🌤️ Hoy (${resumenHoy.fecha}):\n🌡️ ${resumenHoy.tempMin}°C – ${resumenHoy.tempMax}°C\n💨 Viento: ${resumenHoy.vientoMax} km/h"
                txtManana.text = "☀️ Mañana (${resumenManana.fecha}):\n🌡️ ${resumenManana.tempMin}°C – ${resumenManana.tempMax}°C\n💨 Viento: ${resumenManana.vientoMax} km/h"

                card.visibility = View.VISIBLE
                card.alpha = 0f
                card.translationY = 40f
                card.animate().alpha(1f).translationY(0f).setDuration(600).start()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
