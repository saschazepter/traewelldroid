package de.hbch.traewelling.api.models.trip

import com.google.gson.annotations.SerializedName

data class Operator(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)
