package de.hbch.traewelling.api.models.trip

import com.google.gson.annotations.SerializedName

data class DeparturePage(
    @SerializedName("data") val data: List<Departure>,
    @SerializedName("meta") val meta: DepartureMeta
)
