package cl.bentoroal.appcuidadodeplantas.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import cl.bentoroal.appcuidadodeplantas.api.RetrofitInstance
import cl.bentoroal.appcuidadodeplantas.databinding.FragmentMiClimaBinding
import cl.bentoroal.appcuidadodeplantas.ui.adapters.ForecastAdapter
import cl.bentoroal.appcuidadodeplantas.utils.WeatherUtils
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class MiClimaFragment : Fragment() {

    private var _binding: FragmentMiClimaBinding? = null
    private val binding get() = _binding!!

    private lateinit var forecastAdapter: ForecastAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMiClimaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) Inicializar RecyclerView
        forecastAdapter = ForecastAdapter(emptyList())
        binding.rvForecast.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = forecastAdapter
        }

        // 2) Cargar datos
        lifecycleScope.launch {
            try {
                val prefs = requireContext().getSharedPreferences("clima_prefs", Context.MODE_PRIVATE)
                val lat = prefs.getFloat("saved_lat", -38.74f).toDouble()
                val lon = prefs.getFloat("saved_lon", -72.59f).toDouble()
                val respForecasts = RetrofitInstance.api.getDailyForecast(lat, lon)
                val respCurrentWeather = RetrofitInstance.api.getCurrentWeather(lat, lon)
                val forecasts = WeatherUtils.toDailyForecasts(respForecasts.daily)
                val currentWeather = WeatherUtils.toCurrentWeather(respCurrentWeather.currentWeather)

                val hoy = forecasts.firstOrNull()

                val dayName = LocalDate.now()
                    .dayOfWeek
                    .getDisplayName(TextStyle.FULL, Locale("es", "ES"))
                    .replaceFirstChar { it.uppercase() }

                binding.txtTituloClimaHoy.text = " $dayName"

                if (hoy != null) {
                    // 1️⃣ Datos actuales
                    binding.txtTempActual.text = "${currentWeather.temperature.toInt()}°C"
                    binding.imgClimaActualIcon.setImageResource(currentWeather.iconResId)
                    binding.imgClimaActualIcon.setVisibility(View.VISIBLE)
                    // 2️⃣ Datos de hoy desde pronóstico
                    binding.txtTempMin.text = "Min: ${hoy.minTemp.toInt()}°C"
                    binding.txtTempMax.text = "Max: ${hoy.maxTemp.toInt()}°C"
                    binding.txtVientoMax.text = "Viento: ${hoy.maxWind.toInt()} km/h"

                    // 3️⃣ Animación suave
                    binding.cardClimaActual.apply {
                        alpha = 0f
                        scaleX = 0.9f
                        scaleY = 0.9f
                    }.animate()
                        .alpha(1f).scaleX(1f).scaleY(1f)
                        .setDuration(500)
                        .start()
                } else {
                    binding.txtTempActual.text = "Datos no disponibles"
                }

                // 4) Actualizar RecyclerView con los próximos 7 días
                forecastAdapter.update(forecasts.take(7))

            } catch (e: Exception) {
                e.printStackTrace()
                binding.txtTempActual.text = "Error al cargar pronóstico"
                binding.rvForecast.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}