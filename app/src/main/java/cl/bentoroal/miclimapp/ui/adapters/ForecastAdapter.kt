package cl.bentoroal.miclimapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cl.bentoroal.miclimapp.model.DailyForecast
import cl.bentoroal.miclimapp.R

class ForecastAdapter(
    private var items: List<DailyForecast>
) : RecyclerView.Adapter<ForecastAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon  = view.findViewById<ImageView>(R.id.imgWeatherIcon)
        val day   = view.findViewById<TextView>(R.id.txtDayName)
        val temps = view.findViewById<TextView>(R.id.txtTemps)
        val wind  = view.findViewById<TextView>(R.id.txtWind)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_daily_forecast, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.icon.setImageResource(item.iconResId)
        holder.day.text   = item.dayName
        holder.temps.text = "${item.minTemp}°C – ${item.maxTemp}°C"
        holder.wind.text  = "💨 ${item.maxWind} km/h"
    }

    override fun getItemCount(): Int = items.size

    /**
     * Actualiza la lista y refresca la vista.
     * Puedes mejorar con DiffUtil para animaciones suaves.
     */
    fun update(newList: List<DailyForecast>) {
        items = newList
        notifyDataSetChanged()
    }
}