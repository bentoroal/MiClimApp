package cl.bentoroal.appcuidadodeplantas.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.switchmaterial.SwitchMaterial
import cl.bentoroal.appcuidadodeplantas.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*

class SettingsFragment : Fragment() {

    companion object {
        private const val REQUEST_LOCATION = 1001
        private const val PREFS_NAME        = "clima_prefs"
        private const val KEY_AUTO_LOCATION = "auto_location"
        private const val KEY_MANUAL_LOCATION = "manual_location"
        private const val KEY_ALERTS_ENABLED = "alerts_enabled"
        private const val KEY_TEMP_MIN      = "temp_min_alert"
        private const val KEY_WIND_MAX      = "wind_max_alert"
        private const val KEY_SAVED_LAT     = "saved_lat"
        private const val KEY_SAVED_LON     = "saved_lon"
    }

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var autoComplete: AutoCompleteTextView
    private lateinit var autoAdapter: ArrayAdapter<String>
    private var geocoderResults: List<Address> = emptyList()
    private val geocoder by lazy { Geocoder(requireContext(), Locale.getDefault()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fusedClient = LocationServices.getFusedLocationProviderClient(requireContext())

        val switchLocation = view.findViewById<SwitchMaterial>(R.id.switchUbicacionAutomatica)
        autoComplete        = view.findViewById(R.id.autocompleteUbicacionManual)
        val switchAlerts = view.findViewById<SwitchMaterial>(R.id.switchActivarAlertas)
        val seekTemp        = view.findViewById<SeekBar>(R.id.seekBarTempMin)
        val seekWind        = view.findViewById<SeekBar>(R.id.seekBarWindMax)
        val txtTemp         = view.findViewById<TextView>(R.id.txtTempMinValor)
        val txtWind         = view.findViewById<TextView>(R.id.txtWindMaxValor)
        val btnGuardar      = view.findViewById<Button>(R.id.btnGuardarPreferencias)

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val autoEnabled = prefs.getBoolean(KEY_AUTO_LOCATION, true)
        switchLocation.isChecked = autoEnabled
        autoComplete.isEnabled = !autoEnabled

        autoAdapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf())
        autoComplete.setAdapter(autoAdapter)
        autoComplete.threshold = 1

        autoComplete.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = Unit
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.length >= autoComplete.threshold) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val results = fetchGeocoderAddresses(query)
                        geocoderResults = results
                        autoAdapter.clear()
                        autoAdapter.addAll(results.map { addr ->
                            addr.locality ?: addr.featureName ?: ""
                        })
                        autoAdapter.notifyDataSetChanged()
                    }
                }
            }
        })

        autoComplete.setOnItemClickListener { _, _, pos, _ ->
            geocoderResults.getOrNull(pos)?.let { addr ->
                prefs.edit {
                    putFloat(KEY_SAVED_LAT, addr.latitude.toFloat())
                    putFloat(KEY_SAVED_LON, addr.longitude.toFloat())
                }
            }
        }

        autoComplete.setText(prefs.getString(KEY_MANUAL_LOCATION, "") ?: "")
        switchAlerts.isChecked = prefs.getBoolean(KEY_ALERTS_ENABLED, true)
        seekTemp.isEnabled = switchAlerts.isChecked
        seekWind.isEnabled = switchAlerts.isChecked

        seekTemp.progress = prefs.getInt(KEY_TEMP_MIN, 0)
        seekWind.progress = prefs.getInt(KEY_WIND_MAX, 30)
        txtTemp.text = "${seekTemp.progress} °C"
        txtWind.text = "${seekWind.progress} km/h"

        switchLocation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) ensureLocationPermission()
            autoComplete.isEnabled = !isChecked
        }

        switchAlerts.setOnCheckedChangeListener { _, isChecked ->
            seekTemp.isEnabled = isChecked
            seekWind.isEnabled = isChecked
            txtTemp.isEnabled  = isChecked
            txtWind.isEnabled  = isChecked
        }

        seekTemp.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, value: Int, fromUser: Boolean) {
                txtTemp.text = "$value °C"
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        seekWind.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, value: Int, fromUser: Boolean) {
                txtWind.text = "$value km/h"
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        btnGuardar.setOnClickListener {
            prefs.edit {
                putBoolean(KEY_AUTO_LOCATION, switchLocation.isChecked)
                putString(KEY_MANUAL_LOCATION, autoComplete.text.toString())
                putBoolean(KEY_ALERTS_ENABLED, switchAlerts.isChecked)
                putInt(KEY_TEMP_MIN, seekTemp.progress)
                putInt(KEY_WIND_MAX, seekWind.progress)
            }
            Toast.makeText(requireContext(), "🌿 Ajustes guardados con éxito", Toast.LENGTH_SHORT).show()
            // Navegación: puedes usar findNavController().navigateUp() si estás usando Navigation
        }
    }

    private fun ensureLocationPermission() {
        val fine = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION

        if (ContextCompat.checkSelfPermission(requireContext(), fine) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), coarse) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(fine, coarse),
                REQUEST_LOCATION
            )
        } else {
            saveCurrentGpsLocation()
        }
    }

    private fun saveCurrentGpsLocation() {
        val fine = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION

        val hasFine = ContextCompat.checkSelfPermission(requireContext(), fine) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(requireContext(), coarse) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            try {
                fusedClient.lastLocation.addOnSuccessListener { loc ->
                    loc?.let {
                        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                            putFloat(KEY_SAVED_LAT, it.latitude.toFloat())
                            putFloat(KEY_SAVED_LON, it.longitude.toFloat())
                        }
                    }
                }
            } catch (e: SecurityException) {
                Toast.makeText(requireContext(), "Permiso denegado al intentar acceder a la ubicación", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Permisos de ubicación no disponibles", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun fetchGeocoderAddresses(query: String): List<Address> =
        withContext(Dispatchers.IO) {
            try {
                geocoder.getFromLocationName(query, 5) ?: emptyList()
            } catch (e: IOException) {
                emptyList()
            }
        }
}