package cl.bentoroal.appcuidadodeplantas.utils

import android.content.Context
import cl.bentoroal.appcuidadodeplantas.R
import cl.bentoroal.appcuidadodeplantas.model.CurrentWeather
import cl.bentoroal.appcuidadodeplantas.model.DailyForecast
import cl.bentoroal.appcuidadodeplantas.model.DailyWeatherData
import cl.bentoroal.appcuidadodeplantas.model.CurrentWeatherData
import cl.bentoroal.appcuidadodeplantas.model.Comuna
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object WeatherUtils {

    // 1) Formatter para strings "yyyy-MM-dd"
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // 2) Mapeo completo a modelo UI
    fun toDailyForecasts(
        daily: DailyWeatherData,
        locale: Locale = Locale("es", "ES")
    ): List<DailyForecast> =
        (1 until daily.time.size).map { idx ->
            // 2.1) Fecha y nombre del día
            val date = LocalDate.parse(daily.time[idx], dateFormatter)
            val dayName = date.dayOfWeek
                .getDisplayName(TextStyle.FULL, locale)
                .replaceFirstChar { it.uppercase(locale) }

            // 2.2) Valores numéricos
            val minT = daily.temperatureMin.getOrNull(idx)?.roundToInt()?: 0
            val maxT = daily.temperatureMax.getOrNull(idx)?.roundToInt()?: 0
            val wind  = daily.windSpeedMax.getOrNull(idx)?.roundToInt()?: 0

            // 2.3) Icono según weatherCode
            val code = daily.weatherCode.getOrNull(idx) ?: -1
            val icon = when (code) {
                0               -> R.drawable.ic_sunny
                in 1..3         -> R.drawable.ic_cloudy
                45, 48          -> R.drawable.ic_foggy
                in 51..55, in 61..65, in 80..82 -> R.drawable.ic_rainy
                71,73,75        -> R.drawable.ic_snowy
                in 95..99       -> R.drawable.ic_stormy
                else            -> R.drawable.ic_unknown
            }

            DailyForecast(
                dayName   = dayName,
                iconResId = icon,
                minTemp   = minT,
                maxTemp   = maxT,
                maxWind   = wind
            )
        }

    fun toCurrentWeather(current: CurrentWeatherData?): CurrentWeather {
        // 1️⃣ Validación y valores seguros
        val temp = current?.temperature?.toFloat() ?: 0f
        val wind = current?.windspeed?.toFloat() ?: 0f
        val code = current?.weathercode ?: -1

        // 2️⃣ Icono visual según weatherCode
        val icon = when (code) {
            0               -> R.drawable.ic_sunny
            in 1..3         -> R.drawable.ic_cloudy
            45, 48          -> R.drawable.ic_foggy
            in 51..55, in 61..65, in 80..82 -> R.drawable.ic_rainy
            71, 73, 75      -> R.drawable.ic_snowy
            in 95..99       -> R.drawable.ic_stormy
            else            -> R.drawable.ic_unknown
        }

        // 3️⃣ Mapeo a modelo visual
        return CurrentWeather(
            temperature = temp,
            windSpeed   = wind,
            iconResId   = icon
        )
    }
    // 3) Extensiones de formateo (opcionales)
    fun Double?.toDegreeString(): String =
        this?.roundToInt()?.let { "$it°C" } ?: "--"

    fun Double?.toWindString(): String =
        this?.roundToInt()?.let { "$it km/h" } ?: "--"

    // 4) Funcion para crear objetos de comunas desde el json
    fun cargarComunasDesdeAssets(context: Context, fileName: String = "comunas_chile.json"): List<Comuna> {
        val jsonString: String
        try {
            jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace() // Maneja el error apropiadamente
            return emptyList()
        }
        val listComunaType = object : TypeToken<List<Comuna>>() {}.type
        return Gson().fromJson(jsonString, listComunaType) ?: emptyList() // Asegura que no sea null
    }
}