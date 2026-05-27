package de.hbch.traewelling.api.interceptors

import de.hbch.traewelling.events.PrivacyPolicyNotAcceptedEvent
import okhttp3.Interceptor
import okhttp3.Response
import org.greenrobot.eventbus.EventBus

class PrivacyPolicyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code == 406) {
            EventBus.getDefault().post(PrivacyPolicyNotAcceptedEvent())
        }

        return response
    }
}
