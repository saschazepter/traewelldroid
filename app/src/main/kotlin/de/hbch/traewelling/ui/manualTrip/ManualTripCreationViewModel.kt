package de.hbch.traewelling.ui.manualTrip

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import de.hbch.traewelling.TraewelldroidApplication
import de.hbch.traewelling.api.models.polyline.Feature
import de.hbch.traewelling.api.models.trip.CreateManualTripRequest
import de.hbch.traewelling.api.models.trip.Operator
import de.hbch.traewelling.api.models.trip.PreviewManualTripPolylineRequest
import de.hbch.traewelling.api.models.trip.Trip

class ManualTripCreationViewModel(application: Application): AndroidViewModel(application) {
    private val traewellingApi = (application as TraewelldroidApplication).traewellingApi

    suspend fun getOperators(query: String): List<Operator>? {
        val response = traewellingApi.checkInService.getOperators(query)

        if (response.isSuccessful) {
            return response.body()?.data
        }
        return null
    }

    suspend fun requestPolylinePreview(
        request: PreviewManualTripPolylineRequest
    ): Feature? {
        val response = traewellingApi.travelService.previewPolylineForManualTrip(request)

        if (response.isSuccessful) {
            return response.body()?.data
        }

        return null
    }

    suspend fun createManualTrip(request: CreateManualTripRequest): Trip? {
        val response = traewellingApi.travelService.createManualTrip(request)
        return response.body()?.data
    }
}