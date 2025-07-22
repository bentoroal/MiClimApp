package cl.bentoroal.appcuidadodeplantas.model

data class ForecastResponse(
    val list: List<ForecastItem>
)

data class ForecastItem(
    val dt: Long,
    val main: MainWeatherData,
    val wind: WindData
)

data class MainWeatherData(
    val temp_min: Double,
    val temp_max: Double
)

data class WindData(
    val speed: Double
)