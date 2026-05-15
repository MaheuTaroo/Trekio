package pt.trekio.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import co.touchlab.kermit.Logger
import dev.jordond.compass.Location
import dev.jordond.compass.Priority
import dev.jordond.compass.geolocation.Geolocator
import dev.jordond.compass.geolocation.LocationRequest
import dev.jordond.compass.geolocation.TrackingStatus
import dev.jordond.compass.geolocation.mobile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class MapTestViewModel(trackUser: Boolean) : ViewModel() {

    private val locator = Geolocator.mobile()

    val coordinates: Flow<Location> = locator.locationUpdates

    init {
        if (trackUser) {
            viewModelScope.launch {
                val result = locator.startTracking(LocationRequest(Priority.HighAccuracy))
                Logger.i { "startTracking result: $result" }
            }

            viewModelScope.launch {
                locator.trackingStatus.collect { status ->
                    when (status) {
                        is TrackingStatus.Idle -> Logger.i { "Tracking: Idle" }
                        is TrackingStatus.Update -> Logger.i { "Tracking: ${status.location.coordinates}" }
                        is TrackingStatus.Error -> Logger.e { "Tracking error: ${status.cause}" }
                        is TrackingStatus.Tracking -> Logger.i { "Tracking iniciado, aguardando localização..." }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            locator.stopTracking()
        }
    }

    companion object {
        fun getFactory(trackUser: Boolean) = viewModelFactory {
            initializer {
                MapTestViewModel(trackUser)
            }
        }
    }
}