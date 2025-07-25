package cl.bentoroal.appcuidadodeplantas.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

suspend fun fetchGeocoderResults(
    context: Context,
    query: String
): List<String> = withContext(Dispatchers.IO) {
    val geocoder = Geocoder(context, Locale.getDefault())

    // getFromLocationName puede devolver null, así que lo convertimos en lista vacía si es null
    val addresses: List<Address> = geocoder
        .getFromLocationName(query, 5)
        .orEmpty()

    // Ahora sí podemos mapNotNull con seguridad
    addresses.mapNotNull { addr ->
        addr.locality ?: addr.featureName
    }
}