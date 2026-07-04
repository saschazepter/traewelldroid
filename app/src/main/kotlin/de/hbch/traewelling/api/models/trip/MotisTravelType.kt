package de.hbch.traewelling.api.models.trip

import com.google.gson.annotations.SerializedName
import de.hbch.traewelling.R

enum class MotisTravelType {
    @SerializedName("AERIAL_LIFT", alternate = ["AREAL_LIFT"])
    AERIAL_LIFT {
        override val icon = R.drawable.ic_aerial_lift
        override val string = R.string.product_type_aerial_lift
    },
    @SerializedName("AIRPLANE")
    AIRPLANE {
        override val icon = R.drawable.ic_plane
        override val string = R.string.product_type_plane
    },
    @SerializedName("BUS")
    BUS {
        override val icon = R.drawable.ic_bus
        override val string = R.string.product_type_bus
    },
    @SerializedName("COACH")
    COACH {
        override val icon = R.drawable.ic_coach_bus
        override val string = R.string.product_type_coach
    },
    @SerializedName("FERRY")
    FERRY {
        override val icon = R.drawable.ic_ferry
        override val string = R.string.product_type_ferry
    },
    @SerializedName("FUNICULAR")
    FUNICULAR {
        override val icon = R.drawable.ic_funicular
        override val string = R.string.product_type_funicular
    },
    @SerializedName("RAIL", alternate = ["HIGHSPEED_RAIL", "LONG_DISTANCE", "NIGHT_RAIL", "REGIONAL_FAST_RAIL", "REGIONAL_RAIL"])
    RAIL {
        override val icon = R.drawable.ic_train
        override val string = R.string.product_type_train
    },
    @SerializedName("SUBURBAN")
    SUBURBAN {
        override val icon = R.drawable.ic_suburban
        override val string = R.string.product_type_suburban
    },
    @SerializedName("SUBWAY")
    SUBWAY {
        override val icon = R.drawable.ic_subway
        override val string = R.string.product_type_subway
    },
    @SerializedName("TRAM")
    TRAM {
        override val icon = R.drawable.ic_tram
        override val string = R.string.product_type_tram
    };

    abstract val icon: Int
    abstract val string: Int
}