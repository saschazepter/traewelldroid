package de.hbch.traewelling.api.interceptors

import de.hbch.traewelling.BuildConfig
import de.hbch.traewelling.api.AuthManager
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class CoilInterceptor(private val authManager: AuthManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBuilder = request.newBuilder()
            .addHeader(
                "User-Agent",
                "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}"
            )

        val host = request.url.host
        if (host.equals("traewelling.de", ignoreCase = true) || host.endsWith(".traewelling.de", ignoreCase = true)) {
            val tokenRef = AtomicReference(authManager.token)
            val latch = CountDownLatch(1)

            authManager.getFreshAccessToken(
                callback = { token ->
                    tokenRef.set(token)
                    latch.countDown()
                },
                onError = {
                    latch.countDown()
                }
            )

            val ok = latch.await(10, TimeUnit.SECONDS)
            if (!ok) {
                throw IOException("Timeout while waiting for fresh access token")
            }

            val token = tokenRef.get()
            if (!token.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}
