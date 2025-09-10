package de.hbch.traewelling.push

import de.hbch.traewelling.BuildConfig
import org.unifiedpush.android.embedded_fcm_distributor.EmbeddedDistributorReceiver
import org.unifiedpush.android.embedded_fcm_distributor.Gateway

class EmbeddedFcmDistributor : EmbeddedDistributorReceiver() {
    override val gateway = object : Gateway {
        override val vapid = BuildConfig.UP_FCM_VAPID
        override fun getEndpoint(token: String) = "${BuildConfig.UP_FCM_PROXY}?t=$token"
    }
}
