package cl.bentoroal.appcuidadodeplantas.model

data class OneCallResponse(
    val daily: List<DailyForecast>
)

data class DailyForecast(
    val dt: Long,
    val temp: Temperature,
    val wind_speed: Double,
    val weather: List<WeatherInfo>
)

data class Temperature(
    val min: Double,
    val max: Double
)

data class WeatherInfo(
    val description: String,
    val icon: String
)