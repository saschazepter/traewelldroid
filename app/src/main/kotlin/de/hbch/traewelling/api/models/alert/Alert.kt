package de.hbch.traewelling.api.models.alert

import androidx.compose.ui.graphics.Color
import com.google.gson.annotations.SerializedName
import de.hbch.traewelling.R
import java.time.ZonedDateTime

data class Alert(
    val id: String,
    val type: AlertType,
    @SerializedName("active_from") val activeFrom: ZonedDateTime,
    @SerializedName("active_until") val activeUntil: ZonedDateTime?,
    val url: String?,
    val translations: List<AlertTranslation>?
)

data class AlertTranslation(
    val title: String,
    val url: String,
    val content: String,
    val locale: String
)

enum class AlertType {
    @SerializedName("info")
    INFO {
        override val icon = R.drawable.ic_report
    },
    @SerializedName("warning")
    WARNING {
        override val icon = R.drawable.ic_error
        override val color = Color(0xFFF57C00)
    },
    @SerializedName("danger")
    DANGER {
        override val icon = R.drawable.ic_cancel
        override val color = Color.Red
    },
    @SerializedName("success")
    SUCCESS {
        override val icon = R.drawable.ic_check
        override val color = Color.Green
    };

    abstract val icon: Int
    open val color: Color? = null
}
