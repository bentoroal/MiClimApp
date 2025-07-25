package cl.bentoroal.appcuidadodeplantas.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import cl.bentoroal.appcuidadodeplantas.databinding.FragmentMiClimaBinding
import cl.bentoroal.appcuidadodeplantas.api.RetrofitInstance
import cl.bentoroal.appcuidadodeplantas.utils.ForecastUtils
import kotlinx.coroutines.launch

class MiClimaFragment : Fragment() {

    private var _binding: FragmentMiClimaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMiClimaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnConfigAlerts.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }
        cargarClimaActual()
    }

    private fun cargarClimaActual() {
        lifecycleScope.launch {
            try {
                val forecast = RetrofitInstance.api.getDailyForecast(-38.74, -72.59)
                val resumenHoy = ForecastUtils.obtenerResumenPara(0, forecast.daily)

                binding.txtClimaActualValores.text =
                    "🌡️ ${resumenHoy.tempMin}°C – ${resumenHoy.tempMax}°C\n💨 Viento: ${resumenHoy.vientoMax} km/h"

                binding.cardClimaActual.alpha = 0f
                binding.cardClimaActual.scaleX = 0.9f
                binding.cardClimaActual.scaleY = 0.9f
                binding.cardClimaActual.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .start()

            } catch (e: Exception) {
                e.printStackTrace()
                binding.txtClimaActualValores.text = "Error al cargar pronóstico"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}