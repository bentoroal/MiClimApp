package cl.bentoroal.appcuidadodeplantas.api // ajusta según tu paquete

import cl.bentoroal.appcuidadodeplantas.model.OpenMeteoResponse
import retrofit2.http.GET
import retrofit2.http.Query
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private fun List<String>.toDayNames(locale: Locale): List<String> {
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    return map { dateStr ->
        LocalDate.parse(dateStr, fmt)
            .dayOfWeek
            .getDisplayName(TextStyle.FULL, locale)
            .replaceFirstChar { it.uppercase(locale) }
    }
}

interface WeatherService {
    @GET("v1/forecast")
    suspend fun getDailyForecast(
        @Query("latitude")  lat: Double,
        @Query("longitude") lon: Double,
        @Query("daily")
        daily: String = "temperature_2m_min,temperature_2m_max,wind_speed_10m_max,weathercode",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoResponse

    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current_weather") currentWeather: Boolean = true
    ): OpenMeteoResponse
}
