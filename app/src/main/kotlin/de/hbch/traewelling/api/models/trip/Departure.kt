package de.hbch.traewelling.api.models.trip

import com.google.gson.annotations.SerializedName
import de.hbch.traewelling.api.models.station.Station
import java.time.ZonedDateTime

data class Departure(
    @SerializedName("tripId") val tripId: String,
    @SerializedName("when") val departure: ZonedDateTime?,
    @SerializedName("plannedWhen") val plannedDeparture: ZonedDateTime?,
    @SerializedName("platform") val platform: String?,
    @SerializedName("plannedPlatform") val plannedPlatform: String?,
    @SerializedName("direction") val direction: String?,
    @SerializedName("line") val line: Line?,
    @SerializedName("station") val station: Station?,
    @SerializedName("cancelled") val isCancelled: Boolean
)
