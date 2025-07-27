package cl.bentoroal.appcuidadodeplantas.ui

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
import cl.bentoroal.appcuidadodeplantas.R
import cl.bentoroal.appcuidadodeplantas.model.Comuna
import cl.bentoroal.appcuidadodeplantas.utils.WeatherUtils
import cl.bentoroal.appcuidadodeplantas.utils.PREFS_NAME
import cl.bentoroal.appcuidadodeplantas.utils.KEY_AUTO_LOCATION
import cl.bentoroal.appcuidadodeplantas.utils.KEY_MANUAL_LOCATION
import cl.bentoroal.appcuidadodeplantas.utils.KEY_ALERTS_ENABLED
import cl.bentoroal.appcuidadodeplantas.utils.KEY_TEMP_MIN
import cl.bentoroal.appcuidadodeplantas.utils.KEY_WIND_MAX
import cl.bentoroal.appcuidadodeplantas.utils.KEY_SAVED_LAT
import cl.bentoroal.appcuidadodeplantas.utils.KEY_SAVED_LON

import java.text.Normalizer
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
        // Cargar las comunas usando la función de WeatherUtils
        todasLasComunas = WeatherUtils.cargarComunasDesdeAssets(requireContext())
        comunasMapByName = todasLasComunas.associateBy { it.nombre.normalizeForSearch() } // Normaliza la clave si buscas con normalización
        Log.d("SettingsFragment", "Comunas cargadas: ${todasLasComunas.size}")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fusedClient = LocationServices.getFusedLocationProviderClient(requireContext())

        autoComplete = view.findViewById(R.id.autocompleteUbicacionManual)
        val switchLocation = view.findViewById<SwitchMaterial>(R.id.switchUbicacionAutomatica)
        val switchAlerts = view.findViewById<SwitchMaterial>(R.id.switchActivarAlertas)
        val seekTemp = view.findViewById<SeekBar>(R.id.seekBarTempMin)
        val seekWind = view.findViewById<SeekBar>(R.id.seekBarWindMax)
        val txtTemp = view.findViewById<TextView>(R.id.txtTempMinValor)
        val txtWind = view.findViewById<TextView>(R.id.txtWindMaxValor)
        val btnGuardar = view.findViewById<Button>(R.id.btnGuardarPreferencias)

        // Usa las constantes globales importadas
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Configuración del AutoCompleteTextView
        autoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, mutableListOf<String>())
        autoComplete.setAdapter(autoAdapter)
        autoComplete.threshold = 1

        // Cargar preferencias guardadas
        val autoEnabled = prefs.getBoolean(KEY_AUTO_LOCATION, true)
        switchLocation.isChecked = autoEnabled
        autoComplete.isEnabled = !autoEnabled
        autoComplete.setText(prefs.getString(KEY_MANUAL_LOCATION, ""))

        switchAlerts.isChecked = prefs.getBoolean(KEY_ALERTS_ENABLED, true)
        seekTemp.isEnabled = switchAlerts.isChecked
        seekWind.isEnabled = switchAlerts.isChecked
        seekTemp.progress = prefs.getInt(KEY_TEMP_MIN, 0)
        seekWind.progress = prefs.getInt(KEY_WIND_MAX, 30)
        txtTemp.text = "${seekTemp.progress} °C"
        txtWind.text = "${seekWind.progress} km/h"

        // Listeners

        switchLocation.setOnCheckedChangeListener { _, isChecked ->
            autoComplete.isEnabled = !isChecked
            if (isChecked) {
                ensureLocationPermission()
                autoComplete.setText("") // Limpiar texto manual
                // Opcional: Limpiar SharedPreferences de ubicación manual
                // prefs.edit {
                //    remove(KEY_SAVED_LAT)
                //    remove(KEY_SAVED_LON)
                //    remove(KEY_MANUAL_LOCATION)
                // }
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
            val nombreSeleccionado = autoAdapter.getItem(position)
            nombreSeleccionado?.let { nombreComuna ->
                val comunaSeleccionada = comunasMapByName[nombreComuna.normalizeForSearch()] ?: todasLasComunas.find { it.nombre.equals(nombreComuna, ignoreCase = true) }

                comunaSeleccionada?.let { comuna ->
                    prefs.edit {
                        putString(KEY_MANUAL_LOCATION, comuna.nombre)
                        putFloat(KEY_SAVED_LAT, comuna.latitud.toFloat())
                        putFloat(KEY_SAVED_LON, comuna.longitud.toFloat())
                        putBoolean(KEY_AUTO_LOCATION, false) // Desactivar auto al elegir manual
                    }
                    switchLocation.isChecked = false // Actualizar switch
                    autoComplete.isEnabled = true // Asegurar que siga habilitado si se seleccionó

                    Toast.makeText(requireContext(), "Ubicación manual: ${comuna.nombre}", Toast.LENGTH_SHORT).show()
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
                // La ubicación (auto/manual) y sus detalles ya se guardan en sus respectivos listeners
                putBoolean(KEY_ALERTS_ENABLED, switchAlerts.isChecked)
                putInt(KEY_TEMP_MIN, seekTemp.progress)
                putInt(KEY_WIND_MAX, seekWind.progress)
            }
            Toast.makeText(requireContext(), "🌿 Ajustes de alertas guardados", Toast.LENGTH_SHORT).show()
        }
    }

    private fun filtrarComunas(query: String): List<String> {
        if (query.isBlank() || !::todasLasComunas.isInitialized || todasLasComunas.isEmpty()) {
            return emptyList()
        }
        val queryNormalized = query.normalizeForSearch()
        return todasLasComunas
            .filter { comuna ->
                comuna.nombre.normalizeForSearch().contains(queryNormalized)
            }
            .map { it.nombre }
            .distinct()
            .take(10)
    }

    private fun String.normalizeForSearch(): String {
        val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
        return Regex("\\p{InCombiningDiacriticalMarks}+").replace(normalized, "").lowercase(Locale.getDefault())
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(autoComplete.windowToken, 0)
    }

    private fun ensureLocationPermission() {
        val fine = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
        if (ContextCompat.checkSelfPermission(requireContext(), fine) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), coarse) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(fine, coarse), REQUEST_LOCATION)
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
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Log.w("SettingsFragment", "saveCurrentGpsLocation llamado sin permisos, esto no debería pasar.")
                    return
                }
                fusedClient.lastLocation.addOnSuccessListener { loc ->
                    loc?.let {
                        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                            putFloat(KEY_SAVED_LAT, it.latitude.toFloat())
                            putFloat(KEY_SAVED_LON, it.longitude.toFloat())
                            remove(KEY_MANUAL_LOCATION) // Limpiar la ubicación manual guardada
                        }
                        autoComplete.setText("") // Limpiar el campo de texto
                        Log.d("SettingsFragment", "Ubicación GPS guardada. Manual borrada.")
                    } ?: run {
                        Log.w("SettingsFragment", "FusedLocationProviderClient.lastLocation devolvió null.")
                        Toast.makeText(requireContext(), "No se pudo obtener la ubicación actual.", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { e ->
                    Log.e("SettingsFragment", "Error al obtener última ubicación", e)
                    Toast.makeText(requireContext(), "Error al obtener ubicación: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: SecurityException) { // Aunque el check de arriba debería prevenir esto
                Log.e("SettingsFragment", "SecurityException en saveCurrentGpsLocation", e)
                Toast.makeText(requireContext(), "Error de seguridad al acceder a la ubicación.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Permisos de ubicación no disponibles.", Toast.LENGTH_SHORT).show()
        }
    }
    // No olvides manejar el resultado de requestPermissions en onRequestPermissionsResult
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permiso concedido
                saveCurrentGpsLocation()
            } else {
                // Permiso denegado
                Toast.makeText(requireContext(), "Permiso de ubicación denegado.", Toast.LENGTH_SHORT).show()
                // Quizás quieras desmarcar el switch de ubicación automática si el permiso es denegado
                view?.findViewById<SwitchMaterial>(R.id.switchUbicacionAutomatica)?.isChecked = false
            }
        }
    }
}
