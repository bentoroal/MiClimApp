package cl.bentoroal.appcuidadodeplantas.utils

import cl.bentoroal.appcuidadodeplantas.model.ForecastItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ResumenClimatico(
    val fecha: LocalDate,
    val tempMin: Double?,
    val tempMax: Double?,
    val vientoMax: Double?
)

object ForecastUtils {

    fun obtenerResumenPara(fecha: LocalDate, bloques: List<ForecastItem>): ResumenClimatico {
        val bloquesDelDia = bloques.filter { item ->
            val localDate = Instant.ofEpochSecond(item.dt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            localDate == fecha
        }

        val tempMin = bloquesDelDia.minOfOrNull { it.main.temp_min }
        val tempMax = bloquesDelDia.maxOfOrNull { it.main.temp_max }
        val vientoMax = bloquesDelDia.maxOfOrNull { it.wind.speed }

        return ResumenClimatico(
            fecha = fecha,
            tempMin = tempMin,
            tempMax = tempMax,
            vientoMax = vientoMax
        )
    }
}