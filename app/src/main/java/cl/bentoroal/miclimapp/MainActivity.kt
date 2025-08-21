package cl.bentoroal.miclimapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import cl.bentoroal.miclimapp.notifications.NotificationHelper
import cl.bentoroal.miclimapp.utils.KEY_SAVED_LAT
import cl.bentoroal.miclimapp.utils.KEY_SAVED_LON
import cl.bentoroal.miclimapp.utils.isBatteryOptimizationEnabled
import cl.bentoroal.miclimapp.worker.WeatherWorkerScheduler
import cl.bentoroal.miclimapp.ui.MiClimaFragment

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQ_CODE_ALL_PERMS = 2000
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    //Funcion para verificar si hay permiso de ubicacion otorgado
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // Muestra diálogo sólo una vez para pedir quitar optimización batería
    private fun checkBatteryOptimizationOnce() {
        val prefs = getSharedPreferences("clima_prefs", MODE_PRIVATE)
        val alreadyShown = prefs.getBoolean("battery_dialog_shown", false)

        if (!alreadyShown && isBatteryOptimizationEnabled(this)) {
            AlertDialog.Builder(this)
                .setTitle("Optimización de batería activada")
                .setMessage("Para recibir notificaciones diarias correctamente, sugerimos quitar la optimización de batería para esta app, o bien dejarla sin restricciones.")
                .setPositiveButton("Ir a ajustes") { _, _ ->
                    //Alerta extra para modelos con ajuste especial de bateria como xiaomi
                    //showBatterySettings(this)
                }
                .setNegativeButton("Ahora no", null)
                .show()

            prefs.edit { putBoolean("battery_dialog_shown", true) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        NotificationHelper.createChannelIfNeeded(this)
        WeatherWorkerScheduler.scheduleDailyWorkers(applicationContext)

        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.POST_NOTIFICATIONS
        }
        arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ).forEach { p ->
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) perms += p
        }

        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this, perms.toTypedArray(), REQ_CODE_ALL_PERMS)
        } else {
            checkLocationServices()
            fetchAndSaveLocation()
        }

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)!!
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setupWithNavController((navHost as NavHostFragment).navController)

        // Mostrar diálogo de batería solo una vez
        // checkBatteryOptimizationOnce()

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQ_CODE_ALL_PERMS) {
            val locationGranted = permissions.zip(grantResults.toList())
                .any { (perm, res) ->
                    (perm == Manifest.permission.ACCESS_FINE_LOCATION || perm == Manifest.permission.ACCESS_COARSE_LOCATION) &&
                            res == PackageManager.PERMISSION_GRANTED
                }

            if (locationGranted) {
                getSharedPreferences("clima_prefs", Context.MODE_PRIVATE)
                    .edit { putBoolean("permiso_denegado", false) }
                checkLocationServices()
                fetchAndSaveLocation()
            }
            else {
                handleLocationDenied()
            }
        }
    }

    private fun fetchAndSaveLocation() {
        if (!hasLocationPermission()) return

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val prefs = getSharedPreferences("clima_prefs", Context.MODE_PRIVATE)
                    prefs.edit {
                        putFloat(KEY_SAVED_LAT, location.latitude.toFloat())
                        putFloat(KEY_SAVED_LON, location.longitude.toFloat())
                    }

                    // 🔄 Forzar refresco del fragmento si está visible
                    val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                    val currentFragment = (navHost as? NavHostFragment)
                        ?.childFragmentManager
                        ?.fragments
                        ?.firstOrNull()

                    if (currentFragment is MiClimaFragment) {
                        currentFragment.reloadWeather()
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("MainActivity", "Permiso de ubicación no concedido al intentar obtener ubicación", e)
        }
    }




    private fun checkLocationServices() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!enabled) showLocationDisabledDialog()
    }

    private fun handleLocationDenied() {
        val prefs = getSharedPreferences("clima_prefs", Context.MODE_PRIVATE)

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            showLocationRationale()
        } else if (!prefs.getBoolean("permiso_denegado", false)) {
            showPermanentDenialSnackbar()
            prefs.edit { putBoolean("permiso_denegado", true) }
        }
    }


    private fun showLocationRationale() {
        AlertDialog.Builder(this)
            .setTitle("Permiso de ubicación")
            .setMessage("Necesitamos tu ubicación para mostrar el clima local automáticamente.")
            .setPositiveButton("Aceptar") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                    REQ_CODE_ALL_PERMS
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showPermanentDenialSnackbar() {
        Snackbar.make(findViewById(android.R.id.content), "Activa la ubicación en Ajustes para ver el clima de tu zona.", Snackbar.LENGTH_LONG)
            .setAction("Ajustes") {
                openAppSettings()
            }.show()
    }

    private fun showLocationDisabledDialog() {
        AlertDialog.Builder(this)
            .setTitle("Ubicación desactivada")
            .setMessage("La ubicación automática está desactivada. Puedes activarla o ingresar tu ubicación manualmente en los ajustes de la app.")
            .setPositiveButton("Ajustes ubicación") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNeutralButton("Ajustes de la app") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}
