package cl.bentoroal.miclimapp.model

import com.google.gson.annotations.SerializedName // Importante para mapear nombres diferentes

data class Comuna(
    @SerializedName("comuna_id") // Mapea "comuna_id" del JSON a comunaId
    val comunaId: Int,

    @SerializedName("region_id")
    val regionId: Int,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("latitud")
    val latitud: Double,

    @SerializedName("longitud")
    val longitud: Double
)
