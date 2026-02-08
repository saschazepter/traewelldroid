package de.hbch.traewelling.api

import android.content.Context
import androidx.annotation.AnyThread
import com.auth0.android.jwt.JWT
import com.jcloquell.androidsecurestorage.SecureStorage
import de.hbch.traewelling.BuildConfig
import de.hbch.traewelling.shared.SharedValues
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationService
import net.openid.appauth.GrantTypeValues
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import org.json.JSONException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

val API_LOCK = ReentrantLock()

class AuthManager private constructor(context: Context) {
    private val secureStorage: SecureStorage = SecureStorage(context.applicationContext)
    private val lock = ReentrantLock()
    private val authService = AuthorizationService(context.applicationContext)

    @Volatile
    private var authState: AuthState = AuthState()

    val token get() = authState.accessToken

    init {
        readState()
    }

    fun readState() {
        lock.withLock {
            val stateString = secureStorage.getObject(SharedValues.SS_AUTH_STATE, String::class.java)
            if (stateString != null) {
                try {
                    authState = AuthState.jsonDeserialize(stateString)
                } catch (_: JSONException) {
                    authState = AuthState()
                }
            } else {
                // Migration from old JWT/Refresh token storage
                val jwt = secureStorage.getObject(SharedValues.SS_JWT, String::class.java)
                val refreshToken = secureStorage.getObject(SharedValues.SS_REFRESH_TOKEN, String::class.java)

                if (jwt != null && refreshToken != null) {
                    val newState = AuthState(SharedValues.AUTH_SERVICE_CONFIG)
                    val tokenRequest = TokenRequest.Builder(
                        SharedValues.AUTH_SERVICE_CONFIG,
                        BuildConfig.OAUTH_CLIENT_ID
                    )
                        .setGrantType(GrantTypeValues.REFRESH_TOKEN)
                        .setRefreshToken(refreshToken)
                        .build()
                    val tokenResponse = TokenResponse.Builder(tokenRequest)
                        .setAccessToken(jwt)
                        .setRefreshToken(refreshToken)
                        .build()
                    newState.update(tokenResponse, null)

                    writeState(newState)
                    secureStorage.removeObject(SharedValues.SS_JWT)
                    secureStorage.removeObject(SharedValues.SS_REFRESH_TOKEN)
                } else {
                    authState = AuthState()
                }
            }
        }
    }

    private fun writeState(state: AuthState?) {
        lock.withLock {
            authState = state ?: AuthState()
            val stateString = state?.jsonSerializeString()
            if (stateString != null) {
                secureStorage.storeObject(SharedValues.SS_AUTH_STATE, stateString)
            } else {
                secureStorage.removeObject(SharedValues.SS_AUTH_STATE)
            }
        }
    }

    fun replace(state: AuthState?) = writeState(state)

    fun logout() = replace(null)

    @AnyThread
    fun getFreshAccessToken(
        callback: (String?) -> Unit,
        onError: (AuthorizationException?) -> Unit = {}
    ) {
        val token = lock.withLock { authState }.accessToken
        try {
            val jwt = JWT(token ?: "")
            val eat = jwt.expiresAt?.toInstant() ?: Instant.MIN
            val now = Instant.now()
            val duration = Duration.between(now, eat)

            if (duration > Duration.ofMinutes(59)) {
                callback(token)
                return
            }
        } catch (_: Exception) {
            // ignored
        }

        API_LOCK.withLock {
            val currentState = lock.withLock { authState }
            val freshToken = currentState.accessToken
            try {
                val jwt = JWT(freshToken ?: "")

                val eat = jwt.expiresAt?.toInstant() ?: Instant.MIN
                val now = Instant.now()
                val duration = Duration.between(now, eat)

                if (duration > Duration.ofMinutes(59)) {
                    callback(freshToken)
                    return@withLock
                }
            } catch (_: Exception) {
                // ignored
            }

            val latch = CountDownLatch(1)
            var tokenResponseResult: TokenResponse? = null
            var exResult: AuthorizationException? = null

            authService.performTokenRequest(
                TokenRequest.Builder(
                    SharedValues.AUTH_SERVICE_CONFIG,
                    BuildConfig.OAUTH_CLIENT_ID
                )
                    .setRefreshToken(currentState.refreshToken)
                    .setGrantType(GrantTypeValues.REFRESH_TOKEN)
                    .build()
            ) { tokenResponse, ex ->
                tokenResponseResult = tokenResponse
                exResult = ex
                latch.countDown()
            }

            latch.await()

            currentState.update(tokenResponseResult, exResult)
            writeState(currentState)

            if (exResult != null) {
                onError(exResult)
            } else {
                callback(tokenResponseResult?.accessToken)
            }
        }
    }

    companion object {
        @Volatile private var INSTANCE: AuthManager? = null

        fun getInstance(context: Context): AuthManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager(context).also { INSTANCE = it }
            }
    }
}
