package cl.bentoroal.appcuidadodeplantas.model

import com.google.gson.annotations.SerializedName

data class OpenMeteoResponse(
    val daily: DailyWeatherData,
    @SerializedName("current_weather")
    val currentWeather: CurrentWeatherData?
)

data class DailyWeatherData(
    val time: List<String>,

    @SerializedName("temperature_2m_min")
    val temperatureMin: List<Double>,

    @SerializedName("temperature_2m_max")
    val temperatureMax: List<Double>,

    @SerializedName("wind_speed_10m_max")
    val windSpeedMax: List<Double>,

    @SerializedName("weathercode")
    val weatherCode: List<Int>
)

data class CurrentWeatherData(
    val temperature: Double,
    val windspeed: Double,
    val weathercode: Int,
    val time: String
)
