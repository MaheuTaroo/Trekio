package pt.trekio.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.jordond.compass.Location
import dev.jordond.compass.geolocation.Geolocator
import dev.jordond.compass.geolocation.mobile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class MapTestViewModel(trackUser: Boolean) : ViewModel() {
    companion object {
        fun getFactory(trackUser: Boolean) =
            viewModelFactory {
                initializer {
                    MapTestViewModel(trackUser)
                }
            }

        private val locator = Geolocator.mobile()
    }

    val coordinates: Flow<Location>

    init {
        if (trackUser) {
            viewModelScope.launch {
                locator.startTracking()
            }
            coordinates = locator.locationUpdates
        }
    }
}