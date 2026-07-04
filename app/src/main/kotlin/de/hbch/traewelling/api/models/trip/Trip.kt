package de.hbch.traewelling.api.models.trip

import com.google.gson.annotations.SerializedName
import de.hbch.traewelling.api.models.station.Station

data class Trip(
    @SerializedName("id") val id: Int,
    @SerializedName("category") val category: ProductType?,
    @SerializedName("mode") val travelType: MotisTravelType?,
    @SerializedName("lineName") val lineName: String,
    @SerializedName("origin") val origin: Station,
    @SerializedName("destination") val destination: Station,
    @SerializedName("stopovers") var stopovers: List<Stopover>,
    @SerializedName("number") val lineId: String,
    @SerializedName("routeColor") val lineColor: String,
    val dataSource: DataSource?
)

data class DataSource(
    val id: String,
    val attribution: String
)
