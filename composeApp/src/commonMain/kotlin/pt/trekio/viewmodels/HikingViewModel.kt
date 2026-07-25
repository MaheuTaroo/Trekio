package pt.trekio.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import co.touchlab.kermit.Logger
import io.github.tiagopraia.kmp.mapbox.GeographicPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import pt.trekio.dto.ErrorMessage
import pt.trekio.dto.HikeDto
import pt.trekio.dto.HikerLocationAndCheckpointDto
import pt.trekio.dto.HikerLocationNoticeDto
import pt.trekio.dto.TrailDto
import pt.trekio.misc.Failure
import pt.trekio.misc.HaversineDistance
import pt.trekio.misc.Success
import pt.trekio.misc.WebSocketCommunicator
import pt.trekio.misc.showAlert
import pt.trekio.misc.toGeoPoint
import pt.trekio.misc.toGeographicPoint
import pt.trekio.repos.UserRepository
import pt.trekio.services.hikes.HikeService
import pt.trekio.viewmodels.states.HikeState
import kotlin.let
import kotlin.time.Duration.Companion.seconds

class HikingViewModel(
    service: HikeService,
    userRepo: UserRepository,
    private val trail: TrailDto,
    val isFirstPoint: Boolean,
) : ViewModel() {
    companion object {
        fun getFactory(
            hikeService: HikeService,
            userRepo: UserRepository,
            trail: TrailDto,
            isFirstPoint: Boolean,
        ) = viewModelFactory {
            initializer {
                HikingViewModel(hikeService, userRepo, trail, isFirstPoint)
            }
        }

        private val logger = Logger.withTag("HikingViewModel")

        private val parser = Json { isLenient = true }
    }

    // Independent of viewModelScope so it survives its cancellation.
    // SupervisorJob so a cleanup failure can't cascade into anything else.
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var comms: WebSocketCommunicator
    var lastReportedLocation: GeographicPoint? = null
    private val mutex = Mutex()

    var hikers by mutableStateOf(mutableMapOf<ULong, GeographicPoint>())
        private set

    var id = 0UL

    var checkpoint by mutableStateOf<GeographicPoint?>(null)
        private set

    private var _state = MutableStateFlow<HikeState>(HikeState.Loading)
    val state: StateFlow<HikeState> = _state.asStateFlow()

    val path =
        listOf(GeographicPoint(trail.start.lat, trail.start.lon, trail.start.alt)) +
            trail.path.map { GeographicPoint(it.lat, it.lon, it.alt) } +
            GeographicPoint(trail.end.lat, trail.end.lon, trail.end.alt)

    init {
        viewModelScope.launch {
            userRepo.getOwnDetails()?.let { id = it.id }

            logger.i { "Starting hike..." }
            val res = service.startHike(trail.id, isFirstPoint)

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
            comms = tmp
            _state.emit(HikeState.Hiking)

            tmp.incoming.collect { msg ->
                try {
                    var checkpoint: HikerLocationAndCheckpointDto? = null
                    var notice: HikerLocationNoticeDto? = null
                    var details: HikeDto? = null

                    try {
                        checkpoint = parser.decodeFromString<HikerLocationAndCheckpointDto>(msg)
                    } catch (e: IllegalArgumentException) {
                        logger.e(tag = "HikingVM") { "Hike error decoding HikerLocationAndCheckpointDto: $e" }
                        try {
                            notice = parser.decodeFromString<HikerLocationNoticeDto>(msg)
                        } catch (_: IllegalArgumentException) {
                            logger.e(tag = "HikingVM") { "Hike error decoding HikerLocationNoticeDto: $e" }
                            details = parser.decodeFromString<HikeDto>(msg)
                        }
                    }

                    if (checkpoint != null) {
                        processCheckpoint(checkpoint)
                    }

                    if (notice != null) {
                        processNotice(notice)
                    }

                    if (details != null) {
                        processDetails(details)
                    }
                } catch (_: Throwable) {
                    if (comms.isClosed()) {
                        showErrorAndStop(comms.closeReason ?: "an unknown error occurred")
                        return@collect
                    }
                    try {
                        showAlert(parser.decodeFromString<ErrorMessage>(msg).error)
                    } catch (t: Throwable) {
                        logger.e(t) {
                            "Couldn't act upon most recent frame: ${t.message ?: "an unknown error appeared" }"
                        }
                    }
                }
            }
        }

        addCloseable(
            object : AutoCloseable {
                override fun close() {
                    cleanupScope.launch {
                        logger.i { "Cleaning up..." }
                        try {
                            if (!::comms.isInitialized) {
                                return@launch
                            }

                            if (!comms.isClosed()) {
                                comms.cancel()
                                logger.i { "Communication channel issued a cancellation command" }
                            } else {
                                logger.i { "Thankfully, the communication tunnel was already closed" }
                            }
                        } catch (t: Throwable) {
                            logger.e(t) {
                                "Could not close the communication channel: ${t.message ?: "an unknown error occurred"}"
                            }
                        } finally {
                            cleanupScope.cancel()
                        }
                    }
                }
            },
        )
    }

    private suspend fun showErrorAndStop(msg: String) {
        _state.emit(HikeState.Error(msg))
        delay(5.seconds)
        _state.emit(HikeState.Stopped)
    }

    private fun sendAction(
        isStopping: Boolean = false,
        action: suspend () -> Boolean,
    ) {
        viewModelScope.launch {
            if (isStopping) {
                _state.emit(HikeState.Stopping)
            }

            val succeeded = action()
            if (!succeeded && _state.value is HikeState.Hiking) {
                comms.cancel()
                showErrorAndStop("Communication channel has unexpectedly closed")
            } else if (isStopping) {
                _state.emit(HikeState.Stopped)
            }
        }
    }

    fun reportLocation(location: GeographicPoint) {
        if (!::comms.isInitialized) {
            return
        }
        viewModelScope.launch {
            mutex.withLock {
                if (lastReportedLocation == null) {
                    logger.i { "Sending first location..." }
                    lastReportedLocation = location
                    sendAction {
                        logger.i { "ACTION 1: $location" }
                        comms.sendLocation(location.toGeoPoint())
                    }
                    return@launch
                }

                val dist = HaversineDistance.between(location.toGeoPoint(), lastReportedLocation!!.toGeoPoint())
                if (dist < .003) {
                    return@launch
                }

                logger.i { "Detected current location $location, ${(dist * 1000).toInt()}m from previous location" }
                lastReportedLocation = location
                sendAction {
                    logger.i { "ACTION 2: $location" }
                    comms.sendLocation(location.toGeoPoint())
                }
            }
        }
    }

    fun processCheckpoint(lastCheckpoint: HikerLocationAndCheckpointDto) {
        if (lastCheckpoint.uid == id) {
            lastCheckpoint.lastCheckpoint?.let { lc ->
                checkpoint = lc.toGeographicPoint()
            }
        }
    }

    suspend fun processNotice(notice: HikerLocationNoticeDto) {
        notice.currentLocation?.let {
            mutex.withLock {
                hikers[notice.id] = it.toGeographicPoint()
            }
        } ?: hikers.remove(notice.id)
    }

    suspend fun processDetails(details: HikeDto) {
        if (details.hiker == id) {
            details.finish?.let { finish ->
                _state.emit(HikeState.Details(details.start, finish, trail.distance))
            } ?: sendAction(true) {
                Logger.i(tag = "HikingViewModel") { "Hike was not completely finished" }
                true
            }
        }
    }

    fun finish() {
        logger.i(tag = "HikingViewModel") { "ACTION 3: FINISH" }
        sendAction(action = comms::finish)
    }

    fun cancel() {
        logger.i(tag = "HikingViewModel") { "ACTION 4: CANCEL" }
        sendAction(isStopping = true, action = comms::cancel)
    }

    fun details() {
        logger.i(tag = "HikingViewModel") { "ACTION 5: DETAILS" }
        sendAction(isStopping = true) { true }
    }

    fun goBackToHike() {
        viewModelScope.launch {
            _state.emit(HikeState.Hiking)
        }
    }

    fun suggestCancellation() {
        viewModelScope.launch {
            _state.emit(HikeState.AboutToCancel)
        }
    }

    fun suggestFinishing() {
        viewModelScope.launch {
            _state.emit(HikeState.AboutToFinish)
        }
    }
}
