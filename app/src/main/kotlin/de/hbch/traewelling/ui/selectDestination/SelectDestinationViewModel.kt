package de.hbch.traewelling.ui.selectDestination

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import de.hbch.traewelling.TraewelldroidApplication
import de.hbch.traewelling.api.models.Data
import de.hbch.traewelling.api.models.trip.Trip
import de.hbch.traewelling.logging.Logger
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SelectDestinationViewModel(application: Application) : AndroidViewModel(application) {
    private val traewellingApi = (application as TraewelldroidApplication).traewellingApi

    fun getTrip(
        tripId: String,
        lineName: String,
        successfulCallback: (Trip) -> Unit,
        failureCallback: (String?) -> Unit
    ) {
        traewellingApi.travelService.getTrip(tripId, lineName)
            .enqueue(object: Callback<Data<Trip>> {
                override fun onResponse(
                    call: Call<Data<Trip>>,
                    response: Response<Data<Trip>>
                ) {
                    if (response.isSuccessful) {
                        val trip = response.body()?.data
                        if (trip != null) {
                            successfulCallback(trip)
                            return
                        }
                    }
                    failureCallback(response.errorBody()?.string())
                }
                override fun onFailure(call: Call<Data<Trip>>, t: Throwable) {
                    Logger.captureException(t)
                    failureCallback(t.message)
                }
            })
    }
}
