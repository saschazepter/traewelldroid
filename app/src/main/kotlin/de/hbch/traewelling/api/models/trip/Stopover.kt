package de.hbch.traewelling.api.models.trip

import de.hbch.traewelling.api.models.station.Station
import java.time.ZonedDateTime

data class Stopover(
    val station: Station,
    val arrivalPlanned: ZonedDateTime,
    val arrivalReal: ZonedDateTime?,
    val arrivalPlatformPlanned: String?,
    val arrivalPlatformReal: String?,
    val departurePlanned: ZonedDateTime,
    val departureReal: ZonedDateTime?,
    val departurePlatformPlanned: String?,
    val departurePlatformReal: String?,
    val platform: String?,
    val cancelled: Boolean
) {
    val isCancelled: Boolean
        // DB Rest also shows last stop as cancelled
        get() = cancelled && arrivalPlatformReal == null
}
