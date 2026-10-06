package de.hbch.traewelling

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.android.material.color.DynamicColors
import de.hbch.traewelling.api.AuthManager
import de.hbch.traewelling.api.TraewellingApi
import de.hbch.traewelling.api.interceptors.CoilInterceptor
import de.hbch.traewelling.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class TraewelldroidApplication : Application(), ImageLoaderFactory {

    lateinit var traewellingApi: TraewellingApi
        private set

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        AuthManager.getInstance(this)
        traewellingApi = TraewellingApi(this)

        Logger.getInstance().initialize(this)
        
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                filesDir.resolve("osmdroid").deleteRecursively()
                getExternalFilesDir(null)?.resolve("osmdroid")?.deleteRecursively()
            } catch (_: Exception) {
                // Ignore errors during cache cleanup
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(CoilInterceptor(AuthManager.getInstance(this)))
            .build()

        return ImageLoader.Builder(this)
            .callFactory(okHttpClient)
            .build()
    }
}

