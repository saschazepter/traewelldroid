package de.hbch.traewelling.api.models.trip

import java.time.ZonedDateTime

data class CreateManualTripRequest(
    val category: ProductType,
    val lineName: String,
    val journeyNumber: Long?,
    val operatorId: String?,
    val originId: Int,
    val originDeparturePlanned: ZonedDateTime,
    val destinationId: Int,
    val destinationArrivalPlanned: ZonedDateTime,
    val stopovers: List<ManualTripStopover>?
)

data class ManualTripStopover(
    val stationId: Int,
    val arrival: ZonedDateTime?,
    val departure: ZonedDateTime?
)

data class PreviewManualTripPolylineRequest(
    val category: ProductType,
    val stationIds: List<Int>
)
