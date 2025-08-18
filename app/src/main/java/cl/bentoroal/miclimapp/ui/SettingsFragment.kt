package cl.bentoroal.miclimapp.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.switchmaterial.SwitchMaterial
import cl.bentoroal.miclimapp.BuildConfig
import cl.bentoroal.miclimapp.R
import cl.bentoroal.miclimapp.model.Comuna
import cl.bentoroal.miclimapp.utils.*
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*

class SettingsFragment : Fragment() {

    companion object {
        private const val REQUEST_LOCATION = 1001
    }

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var autoComplete: AutoCompleteTextView
    private lateinit var autoAdapter: ArrayAdapter<String>
    private lateinit var todasLasComunas: List<Comuna>
    private lateinit var comunasMapByName: Map<String, Comuna>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        todasLasComunas = WeatherUtils.cargarComunasDesdeAssets(requireContext())
        comunasMapByName = todasLasComunas.associateBy { it.nombre.normalizeForSearch() }
        Log.d("SettingsFragment", "Comunas cargadas: ${todasLasComunas.size}")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Referencias a vistas
        autoComplete = view.findViewById(R.id.autocompleteUbicacionManual)
        val switchLocation = view.findViewById<SwitchMaterial>(R.id.switchUbicacionAutomatica)
        val switchAlerts = view.findViewById<SwitchMaterial>(R.id.switchActivarAlertas)
        val seekTemp = view.findViewById<SeekBar>(R.id.seekBarTempMin)
        val seekWind = view.findViewById<SeekBar>(R.id.seekBarWindMax)
        val txtTemp = view.findViewById<TextView>(R.id.txtTempMinValor)
        val txtWind = view.findViewById<TextView>(R.id.txtWindMaxValor)
        val btnGuardar = view.findViewById<Button>(R.id.btnGuardarPreferencias)
        val txtHistorial = view.findViewById<TextView>(R.id.txtUltimaNotificacion)

        val prefs = requireContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Carga inicial de preferencias
        val autoEnabled = prefs.getBoolean(KEY_AUTO_LOCATION, true)
        switchLocation.isChecked = autoEnabled
        autoComplete.isEnabled = !autoEnabled
        autoComplete.setText(prefs.getString(KEY_MANUAL_LOCATION, ""))

        switchAlerts.isChecked = prefs.getBoolean(KEY_ALERTS_ENABLED, true)
        seekTemp.isEnabled = switchAlerts.isChecked
        seekWind.isEnabled = switchAlerts.isChecked
        seekTemp.progress = prefs.getFloat(KEY_TEMP_MIN, 0f).toInt()
        seekWind.progress = prefs.getFloat(KEY_WIND_MAX, 30f).toInt()
        txtTemp.text = "${seekTemp.progress} °C"
        txtWind.text = "${seekWind.progress} km/h"

        // Configuración de auto-complete
        autoAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf()
        )
        autoComplete.setAdapter(autoAdapter)
        autoComplete.threshold = 1

        // Listeners y flujos de UI...

        switchLocation.setOnCheckedChangeListener { _, isChecked ->
            autoComplete.isEnabled = !isChecked
            if (isChecked) {
                ensureLocationPermission()
                autoComplete.setText("")
            }
        }

        autoComplete.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!autoComplete.isEnabled) return
                val query = s.toString().trim()
                if (query.length >= autoComplete.threshold) {
                    val suggestions = filtrarComunas(query)
                    autoAdapter.clear()
                    autoAdapter.addAll(suggestions)
                    autoAdapter.notifyDataSetChanged()
                    if (suggestions.isNotEmpty() && autoComplete.isFocused) {
                        autoComplete.showDropDown()
                    }
                } else {
                    autoAdapter.clear()
                    autoAdapter.notifyDataSetChanged()
                }
            }
        })

        autoComplete.setOnItemClickListener { _, _, position, _ ->
            autoAdapter.getItem(position)?.let { nombre ->
                val comuna = comunasMapByName[nombre.normalizeForSearch()]
                    ?: todasLasComunas.find { it.nombre.equals(nombre, true) }
                comuna?.let {
                    prefs.edit {
                        putString(KEY_MANUAL_LOCATION, it.nombre)
                        putFloat(KEY_SAVED_LAT, it.latitud.toFloat())
                        putFloat(KEY_SAVED_LON, it.longitud.toFloat())
                        putBoolean(KEY_AUTO_LOCATION, false)
                    }
                    switchLocation.isChecked = false
                    autoComplete.isEnabled = true
                    Toast.makeText(requireContext(), "Ubicación manual: ${it.nombre}", Toast.LENGTH_SHORT).show()
                    hideKeyboard()
                    autoComplete.clearFocus()
                }
            }
        }

        switchAlerts.setOnCheckedChangeListener { _, isChecked ->
            seekTemp.isEnabled = isChecked
            seekWind.isEnabled = isChecked
            txtTemp.isEnabled = isChecked
            txtWind.isEnabled = isChecked
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
                putBoolean(KEY_ALERTS_ENABLED, switchAlerts.isChecked)
                putFloat(KEY_TEMP_MIN, seekTemp.progress.toFloat())
                putFloat(KEY_WIND_MAX, seekWind.progress.toFloat())
                apply()
            }
            Toast.makeText(requireContext(), "🌿 Ajustes de alertas guardados", Toast.LENGTH_SHORT).show()
        }

        // ─── Historial de notificaciones (solo en DEBUG) ───
        if (BuildConfig.DEBUG) {
            txtHistorial.visibility = View.VISIBLE

            val lastMessage = prefs.getString("last_notification_message", null)
            val lastTime = prefs.getLong("last_notification_time", 0L)

            if (lastMessage != null && lastTime > 0) {
                val formattedTime = SimpleDateFormat(
                    "dd MMM yyyy HH:mm",
                    Locale("es", "ES")
                ).format(Date(lastTime))

                txtHistorial.text = """
                    🕒 Última notificación:
                    $formattedTime

                    $lastMessage
                """.trimIndent()
            } else {
                txtHistorial.text = "No se ha enviado ninguna notificación aún."
            }
        } else {
            txtHistorial.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                saveCurrentGpsLocation()
            } else {
                Toast.makeText(requireContext(), "Permiso de ubicación denegado.", Toast.LENGTH_SHORT).show()
                view?.findViewById<SwitchMaterial>(R.id.switchUbicacionAutomatica)?.isChecked = false
            }
        }
    }

    private fun filtrarComunas(query: String): List<String> {
        if (query.isBlank() || todasLasComunas.isEmpty()) return emptyList()
        val qNorm = query.normalizeForSearch()
        return todasLasComunas
            .filter { it.nombre.normalizeForSearch().contains(qNorm) }
            .map { it.nombre }
            .distinct()
            .take(10)
    }

    private fun String.normalizeForSearch(): String {
        val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
        return Regex("\\p{InCombiningDiacriticalMarks}+")
            .replace(normalized, "")
            .lowercase(Locale.getDefault())
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(autoComplete.windowToken, 0)
    }

    private fun ensureLocationPermission() {
        val fine = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
        if (ContextCompat.checkSelfPermission(requireContext(), fine) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), coarse) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(fine, coarse), REQUEST_LOCATION)
        } else {
            saveCurrentGpsLocation()
        }
    }

    private fun saveCurrentGpsLocation() {
        try {
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let {
                    requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                        putFloat(KEY_SAVED_LAT, it.latitude.toFloat())
                        putFloat(KEY_SAVED_LON, it.longitude.toFloat())
                        remove(KEY_MANUAL_LOCATION)
                    }
                    autoComplete.setText("")
                } ?: run {
                    Toast.makeText(requireContext(), "No se pudo obtener la ubicación actual.", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al obtener ubicación: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(requireContext(), "Error de seguridad al acceder a la ubicación.", Toast.LENGTH_SHORT).show()
        }
    }
}
