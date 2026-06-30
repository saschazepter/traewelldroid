package de.hbch.traewelling

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.android.material.color.DynamicColors
import de.hbch.traewelling.api.AuthManager
import de.hbch.traewelling.api.TraewellingApi
import de.hbch.traewelling.api.interceptors.CoilInterceptor
import de.hbch.traewelling.logging.Logger
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

