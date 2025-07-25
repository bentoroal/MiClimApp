package cl.bentoroal.appcuidadodeplantas.utils

import cl.bentoroal.appcuidadodeplantas.model.DailyWeatherData
import java.time.LocalDate

data class ResumenClimatico(
    val fecha: LocalDate,
    val tempMin: Double?,
    val tempMax: Double?,
    val vientoMax: Double?
)

object ForecastUtils {

    fun obtenerResumenPara(index: Int, daily: DailyWeatherData): ResumenClimatico {
        val fecha = LocalDate.parse(daily.time[index])
        return ResumenClimatico(
            fecha = fecha,
            tempMin = daily.temperature_2m_min.getOrNull(index),
            tempMax = daily.temperature_2m_max.getOrNull(index),
            vientoMax = daily.wind_speed_10m_max.getOrNull(index)
        )
    }}