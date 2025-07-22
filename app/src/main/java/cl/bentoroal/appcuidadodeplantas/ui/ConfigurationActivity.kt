package cl.bentoroal.appcuidadodeplantas.ui

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cl.bentoroal.appcuidadodeplantas.R

class ConfigurationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuration)

        val seekTemp = findViewById<SeekBar>(R.id.seekBarTempMin)
        val seekWind = findViewById<SeekBar>(R.id.seekBarWindMax)
        val txtTemp = findViewById<TextView>(R.id.txtTempMinValor)
        val txtWind = findViewById<TextView>(R.id.txtWindMaxValor)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarUmbrales)

        val prefs = getSharedPreferences("clima_prefs", Context.MODE_PRIVATE)
        seekTemp.progress = prefs.getFloat("temp_min_alert", 0f).toInt()
        seekWind.progress = prefs.getFloat("wind_max_alert", 30f).toInt()

        txtTemp.text = "${seekTemp.progress} °C"
        txtWind.text = "${seekWind.progress} km/h"

        seekTemp.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                txtTemp.text = "$value °C"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        seekWind.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                txtWind.text = "$value km/h"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        btnGuardar.setOnClickListener {
            prefs.edit()
                .putFloat("temp_min_alert", seekTemp.progress.toFloat())
                .putFloat("wind_max_alert", seekWind.progress.toFloat())
                .apply()
            Toast.makeText(this, "🌿 Umbrales guardados con éxito", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}