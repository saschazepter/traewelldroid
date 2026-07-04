package de.hbch.traewelling.api.models.trip

import com.google.gson.annotations.SerializedName
import de.hbch.traewelling.R

data class Line(
    @SerializedName("type") val type: String,
    @SerializedName("id") val id: String,
    @SerializedName("fahrtNr") val journeyNumber: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("product") val product: ProductType?,
    @SerializedName("mode") val travelType: MotisTravelType?,
    @SerializedName("color") val lineColor: String?,
    @SerializedName("textColor") val textColor: String?
) {
    val safeProductType get() = product ?: ProductType.UNKNOWN
}

@Suppress("unused")
enum class ProductType {
    @SerializedName("all")
    ALL {
        override val selectable = false
    },
    @SerializedName("ferry")
    FERRY {
        override val icon = R.drawable.ic_ferry
        override val text = R.string.product_type_ferry
    },
    @SerializedName("taxi")
    TAXI {
        override val icon = R.drawable.ic_taxi
        override val text = R.string.product_type_taxi
    },
    @SerializedName("bus")
    BUS {
        override val icon = R.drawable.ic_bus
        override val text = R.string.product_type_bus
    },
    @SerializedName("suburban")
    SUBURBAN {
        override val icon = R.drawable.ic_suburban
        override val text = R.string.product_type_suburban
    },
    @SerializedName("subway")
    SUBWAY {
        override val icon = R.drawable.ic_subway
        override val text = R.string.product_type_subway
    },
    @SerializedName("tram")
    TRAM {
        override val icon = R.drawable.ic_tram
        override val text = R.string.product_type_tram
    },
    // RE, RB, RS
    @SerializedName("regional")
    REGIONAL {
        override val text = R.string.product_type_regional
    },
    // IRE, IR
    @SerializedName("regionalExp")
    REGIONAL_EXPRESS {
        override val text = R.string.product_type_regional_express
    },
    // ICE, ECE
    @SerializedName("nationalExpress")
    NATIONAL_EXPRESS {
        override val text = R.string.product_type_national_express
    },
    // IC, EC
    @SerializedName("national")
    NATIONAL {
        override val text = R.string.product_type_national
    },
    @SerializedName("plane")
    PLANE {
        override val icon = R.drawable.ic_plane
        override val text = R.string.product_type_plane
    },
    @SerializedName("freightTrain")
    FREIGHT_TRAIN {
        override val icon = R.drawable.ic_package
        override val text = R.string.product_type_freight_train
    },
    LONG_DISTANCE {
        override val text = R.string.product_type_national_express
        override val selectable = false
    },
    UNKNOWN {
        override val icon = R.drawable.ic_unknown
        override val text = R.string.unknown
        override val selectable = false
    };

    open val icon = R.drawable.ic_train
    open val text = R.string.product_type_bus
    open val selectable = true
}
