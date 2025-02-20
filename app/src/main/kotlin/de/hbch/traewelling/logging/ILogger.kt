package de.hbch.traewelling.logging

import android.app.Application

interface ILogger {
    fun initialize(application: Application)
    fun captureException(t: Throwable)
    fun captureMessage(message: String, additionalInfo: Map<String, String>)
}
