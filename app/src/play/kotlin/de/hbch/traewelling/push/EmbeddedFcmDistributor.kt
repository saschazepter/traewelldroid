package de.hbch.traewelling.push

import de.hbch.traewelling.BuildConfig
import org.unifiedpush.android.embedded_fcm_distributor.EmbeddedDistributorReceiver
import org.unifiedpush.android.embedded_fcm_distributor.Gateway

class EmbeddedFcmDistributor : EmbeddedDistributorReceiver() {
    override val gateway = object : Gateway {
        override val vapid: String = BuildConfig.FCM_VAPID

        override fun getEndpoint(token: String): String {
            return "${BuildConfig.UP_FCM_PROXY}?t=$token"
        }
    }
}
