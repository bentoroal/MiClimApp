package cl.bentoroal.miclimapp.model

data class DailyForecast(
    val dayName: String,
    val iconResId: Int,
    val minTemp: Int,
    val maxTemp: Int,
    val maxWind: Int
)