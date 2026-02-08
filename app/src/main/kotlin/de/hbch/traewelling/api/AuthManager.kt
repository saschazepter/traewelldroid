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
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

val STATE_LOCK = ReentrantLock()

class AuthManager private constructor(context: Context) {
    private val secureStorage: SecureStorage = SecureStorage(context.applicationContext)
    private val authService = AuthorizationService(context.applicationContext)

    @Volatile
    private var authState: AuthState = AuthState()

    val token get() = authState.accessToken

    init {
        readState()
    }

    fun readState() {
        STATE_LOCK.withLock {
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
        STATE_LOCK.withLock {
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

    private val refreshLock = ReentrantLock()
    private var refreshInFlight = false
    private val waiters = mutableListOf<Pair<(String?) -> Unit, (AuthorizationException?) -> Unit>>()

    @AnyThread
    fun getFreshAccessToken(
        callback: (String?) -> Unit,
        onError: (AuthorizationException?) -> Unit = {}
    ) {
        // Check if token is fresh
        val tokenNow = STATE_LOCK.withLock { authState.accessToken }
        if (isTokenFresh(tokenNow)) {
            callback(tokenNow)
            return
        }

        // start refresh or get informed when refresh is done
        refreshLock.withLock {
            // check token again (avoid race conditions)
            val tokenAgain = STATE_LOCK.withLock { authState.accessToken }
            if (isTokenFresh(tokenAgain)) {
                callback(tokenAgain)
                return
            }

            if (refreshInFlight) {
                waiters += callback to onError
                return
            } else {
                refreshInFlight = true
                waiters += callback to onError
            }
        }

        // only a single refresh can occur
        val currentState = STATE_LOCK.withLock { authState }
        val request = TokenRequest.Builder(
            SharedValues.AUTH_SERVICE_CONFIG,
            BuildConfig.OAUTH_CLIENT_ID
        )
            .setRefreshToken(currentState.refreshToken)
            .setGrantType(GrantTypeValues.REFRESH_TOKEN)
            .build()

        authService.performTokenRequest(request) { tokenResponse, ex ->
            // Update state and get waiters to notify
            val callbacksToNotify: List<Pair<(String?) -> Unit, (AuthorizationException?) -> Unit>> =
                refreshLock.withLock {
                    currentState.update(tokenResponse, ex)
                    writeState(currentState)

                    refreshInFlight = false
                    val copy = waiters.toList()
                    waiters.clear()
                    copy
                }

            // Notify waiters
            if (ex != null) {
                callbacksToNotify.forEach { (_, err) -> err(ex) }
            } else {
                val newToken = tokenResponse?.accessToken
                callbacksToNotify.forEach { (cb, _) -> cb(newToken) }
            }
        }
    }

    private fun isTokenFresh(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        return try {
            val jwt = JWT(token)
            val exp = jwt.expiresAt?.toInstant() ?: return false
            Duration.between(Instant.now(), exp) > Duration.ofMinutes(30)
        } catch (_: Exception) {
            false
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
