package cl.bentoroal.miclimapp.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import cl.bentoroal.miclimapp.api.RetrofitInstance
import cl.bentoroal.miclimapp.databinding.FragmentMiClimaBinding
import cl.bentoroal.miclimapp.ui.adapters.ForecastAdapter
import cl.bentoroal.miclimapp.utils.WeatherUtils
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

        // Inicializar RecyclerView
        forecastAdapter = ForecastAdapter(emptyList())
        binding.rvForecast.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = forecastAdapter
        }

        // Cargar clima al entrar por primera vez
        reloadWeather()
    }

    override fun onResume() {
        super.onResume()
        // Reintenta cargar cada vez que se vuelve al fragmento
        reloadWeather()
    }

    fun reloadWeather() {
        viewLifecycleOwner.lifecycleScope.launch {
            val safeBinding = _binding ?: return@launch

            val prefs = requireContext()
                .getSharedPreferences("clima_prefs", Context.MODE_PRIVATE)
            val lat = prefs.getFloat("saved_lat", -999f).toDouble()
            val lon = prefs.getFloat("saved_lon", -999f).toDouble()

            try {
                val respForecasts = RetrofitInstance.api.getDailyForecast(lat, lon)
                val respCurrent = RetrofitInstance.api.getCurrentWeather(lat, lon)

                val forecasts = WeatherUtils.toDailyForecasts(respForecasts.daily)
                val current = WeatherUtils.toCurrentWeather(respCurrent.currentWeather)

                val hoy = forecasts.firstOrNull()
                val dayName = LocalDate.now()
                    .dayOfWeek
                    .getDisplayName(TextStyle.FULL, Locale("es", "ES"))
                    .replaceFirstChar { it.uppercase() }

                safeBinding.txtTituloClimaHoy.text = " $dayName"

                if (hoy != null) {
                    safeBinding.txtTempActual.text = "${current.temperature.toInt()}°C"
                    safeBinding.imgClimaActualIcon.apply {
                        setImageResource(current.iconResId)
                        visibility = View.VISIBLE
                    }

                    safeBinding.txtTempMin.text = "Min: ${hoy.minTemp.toInt()}°C"
                    safeBinding.txtTempMax.text = "Max: ${hoy.maxTemp.toInt()}°C"
                    safeBinding.txtVientoMax.text = "Viento: ${hoy.maxWind.toInt()} km/h"

                    safeBinding.cardClimaActual.apply {
                        alpha = 0f
                        scaleX = 0.9f
                        scaleY = 0.9f
                    }.animate()
                        .alpha(1f).scaleX(1f).scaleY(1f)
                        .setDuration(500)
                        .start()
                } else {
                    safeBinding.txtTempActual.text = "Datos no disponibles"
                }

                forecastAdapter.update(forecasts.take(7))

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
