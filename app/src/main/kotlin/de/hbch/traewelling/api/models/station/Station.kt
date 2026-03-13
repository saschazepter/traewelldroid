package de.hbch.traewelling.api.models.station

import com.google.gson.annotations.SerializedName

data class Station(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val areas: List<Area>?,
    val identifiers: List<StationIdentifier>?
) {
    val rl100 get() = identifiers?.firstOrNull { it.safeType == StationIdentifierType.DE_RL100 }?.identifier
    val evaIdentifier get() = identifiers?.firstOrNull { it.safeType == StationIdentifierType.DE_EVA_NR }?.identifier?.toLongOrNull()
}

data class Area(
    val name: String,
    val adminLevel: Int,
    val default: Boolean
)

enum class StationIdentifierType {
    @SerializedName("de_db_ril100")
    DE_RL100,
    @SerializedName("de_db_ibnr")
    DE_EVA_NR,
    UNKNOWN
}

data class StationIdentifier(
    val type: StationIdentifierType?,
    val identifier: String
) {
    val safeType get() = type ?: StationIdentifierType.UNKNOWN
}
