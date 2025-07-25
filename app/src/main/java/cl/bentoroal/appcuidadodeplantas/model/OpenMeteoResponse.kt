package cl.bentoroal.appcuidadodeplantas.model

data class OpenMeteoResponse(
    val daily: DailyWeatherData,
    val hourly: HourlyWeatherData
)

data class DailyWeatherData(
    val time: List<String>,
    val temperature_2m_min: List<Double>,
    val temperature_2m_max: List<Double>,
    val wind_speed_10m_max: List<Double>
)

data class HourlyWeatherData(
    val time: List<String>,
    val temperature_2m: List<Double>
)