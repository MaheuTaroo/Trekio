package pt.trekio.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import co.touchlab.kermit.Logger
import io.github.tiagopraia.kmp.mapbox.GeographicPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pt.trekio.dto.TrailDto
import pt.trekio.misc.Failure
import pt.trekio.misc.HaversineDistance
import pt.trekio.misc.Success
import pt.trekio.misc.WebSocketCommunicator
import pt.trekio.misc.toGeoPoint
import pt.trekio.services.hikes.HikeService
import pt.trekio.viewmodels.states.HikeState
import kotlin.time.Duration.Companion.seconds

class HikingViewModel(
    service: HikeService,
    trail: TrailDto,
) : ViewModel() {
    companion object {
        fun getFactory(
            hikeService: HikeService,
            trail: TrailDto,
        ) = viewModelFactory {
            initializer {
                HikingViewModel(hikeService, trail)
            }
        }

        val logger = Logger.withTag("HikingViewModel")
    }

    private lateinit var comms: WebSocketCommunicator

    private var lastReportedLocation: GeographicPoint? = null

    private val mutex = Mutex()

    private var _state = MutableStateFlow<HikeState>(HikeState.Loading)
    val state: StateFlow<HikeState> = _state.asStateFlow()

    val path =
        listOf(GeographicPoint(trail.start.lat, trail.start.lon, trail.start.alt)) +
            trail.path.map { GeographicPoint(it.lat, it.lon, it.alt) } +
            GeographicPoint(trail.end.lat, trail.end.lon, trail.end.alt)

    init {
        viewModelScope.launch {
            logger.i { "Starting hike..." }
            val res = service.startHike(trail.id)

            if (res is Failure) {
                logger.e { "Hike error: ${res.message}" }
                showErrorAndStop(res.message)
                return@launch
            }

            val tmp = (res as Success).value
            if (tmp.isClosed()) {
                logger.e { "Tunnel got closed" }
                showErrorAndStop("Communication tunnel was closed")
                return@launch
            }

            logger.i { "Hike started, changing state..." }
            _state.emit(HikeState.Hiking)
            comms = tmp
        }
    }

    private suspend fun CoroutineScope.showErrorAndStop(msg: String) {
        _state.emit(HikeState.Error(msg))
        delay(5.seconds)
        _state.emit(HikeState.Stopped)
        cancel()
    }

    private fun sendAction(action: suspend () -> Boolean) {
        viewModelScope.launch {
            val succeeded = action()
            if (!succeeded && _state.value is HikeState.Hiking) {
                comms.cancel()
                showErrorAndStop("Communication channel has unexpectedly closed")
            }
        }
    }

    fun reportLocation(location: GeographicPoint) {
        viewModelScope.launch {
            mutex.withLock {
                if (lastReportedLocation == null) {
                    logger.i { "Sending first location..." }
                    lastReportedLocation = location
                    sendAction { comms.sendLocation(location.toGeoPoint()) }
                    return@launch
                }

                val dist = HaversineDistance.between(location.toGeoPoint(), lastReportedLocation!!.toGeoPoint())
                if (dist < .003) {
                    return@launch
                }

                logger.i { "Detected current location $location, ${(dist * 1000).toInt()}m from previous location" }
                lastReportedLocation = location
                sendAction { comms.sendLocation(location.toGeoPoint()) }
            }
        }
    }

    fun finish() = sendAction(comms::finish)

    fun cancel() = sendAction(comms::cancel)
}
