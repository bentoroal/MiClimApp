package cl.bentoroal.appcuidadodeplantas.api // ajusta según tu paquete

import cl.bentoroal.appcuidadodeplantas.model.OpenMeteoResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {
    @GET("v1/forecast")
    suspend fun getDailyForecast(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("daily") daily: String = "temperature_2m_min,temperature_2m_max,wind_speed_10m_max",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoResponse
}
