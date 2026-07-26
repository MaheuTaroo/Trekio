package pt.trekio.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
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
import pt.trekio.dto.UserDto
import pt.trekio.misc.Failure
import pt.trekio.misc.HaversineDistance
import pt.trekio.misc.Success
import pt.trekio.misc.WebSocketCommunicator
import pt.trekio.misc.showAlert
import pt.trekio.misc.toGeoPoint
import pt.trekio.misc.toGeographicPoint
import pt.trekio.repos.UserRepository
import pt.trekio.services.hikes.HikeService
import pt.trekio.services.user.UserService
import pt.trekio.viewmodels.states.HikeState
import kotlin.let
import kotlin.time.Duration.Companion.seconds

class HikingViewModel(
    service: HikeService,
    private val userService: UserService,
    userRepo: UserRepository,
    private val trail: TrailDto,
    val isFirstPoint: Boolean,
) : ViewModel() {
    companion object {
        fun getFactory(
            hikeService: HikeService,
            userService: UserService,
            userRepo: UserRepository,
            trail: TrailDto,
            isFirstPoint: Boolean,
        ) = viewModelFactory {
            initializer {
                HikingViewModel(hikeService, userService, userRepo, trail, isFirstPoint)
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

            comms.incoming.collect { msg ->
                Logger.i { "Arrived at collect:$msg" }
                try {
                    var checkpoint: HikerLocationAndCheckpointDto? = null
                    var notice: HikerLocationNoticeDto? = null
                    var details: HikeDto? = null

                    try {
                        checkpoint = parser.decodeFromString<HikerLocationAndCheckpointDto>(msg)
                        Logger.i { "Reached here" }
                    } catch (e: IllegalArgumentException) {
                        logger.e { "Hike error decoding HikerLocationAndCheckpointDto: $e" }
                        try {
                            notice = parser.decodeFromString<HikerLocationNoticeDto>(msg)
                        } catch (_: IllegalArgumentException) {
                            logger.e { "Hike error decoding HikerLocationNoticeDto: $e" }
                            details = parser.decodeFromString<HikeDto>(msg)
                        }
                    }

                    if (checkpoint != null) {
                        processCheckpoint(checkpoint)
                    }

                    if (notice != null) {
                        processNotice(notice)
                    }

                    Logger.i { "Details: $details" }
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
                if (dist < .001) {
                    return@launch
                }

                logger.i { "Detected current location $location, ${(dist * 1000).toInt()}m from previous location" }
                lastReportedLocation = location
                sendAction {
                    comms.sendLocation(location.toGeoPoint())
                }
            }
        }
    }

    fun processCheckpoint(lastCheckpoint: HikerLocationAndCheckpointDto) {
        logger.i { "Checkpoint: ${lastCheckpoint.uid};$id " }
        if (lastCheckpoint.uid == id) {
            lastCheckpoint.lastCheckpoint?.let { lc ->
                logger.i { "Receiving checkpoint $lc" }
                checkpoint = lc.toGeographicPoint()
            }
        }
    }

    suspend fun processNotice(notice: HikerLocationNoticeDto) {
        logger.i { "Arrived at notice with id: ${notice.id}" }
        mutex.withLock {
            notice.currentLocation?.let { geoPoint ->
                val isNewHiker = notice.id !in hikers
                val location = geoPoint.toGeographicPoint()
                hikers[notice.id] = location

                if (isNewHiker) {
                    fetchAndCacheUser(notice.id)
                }

                if (_hikerSelection.value.matchesHiker(notice.id)) {
                    updateSelectionLocation(location)
                }
            } ?: run {
                hikers.remove(notice.id)
                if (_hikerSelection.value.matchesHiker(notice.id)) {
                    _hikerSelection.value = HikerSelection.None
                }
            }
        }
    }

    suspend fun processDetails(details: HikeDto) {
        Logger.i { "Arrived at processDetails" }
        if (details.hiker == id) {
            details.finish?.let { finish ->
                _state.emit(HikeState.Details(details.start, finish, trail.distance))
            } ?: sendAction(true) {
                Logger.i { "Hike was not completely finished" }
                true
            }
        }
    }

    fun finish() {
        logger.i { "ACTION 3: FINISH" }
        sendAction(action = comms::finish)
    }

    fun cancel() {
        logger.i { "ACTION 4: CANCEL" }
        sendAction(isStopping = true, action = comms::cancel)
    }

    fun details() {
        logger.i { "ACTION 5: DETAILS" }
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

    val hikers: SnapshotStateMap<ULong, GeographicPoint> = mutableStateMapOf()

    private val _hikerSelection = MutableStateFlow<HikerSelection>(HikerSelection.None)
    val hikerSelection: StateFlow<HikerSelection> = _hikerSelection.asStateFlow()

    private val userCache = mutableMapOf<ULong, UserDto>()

    private fun HikerSelection.matchesHiker(id: ULong): Boolean =
        when (this) {
            is HikerSelection.Loading -> userId == id
            is HikerSelection.Loaded -> userId == id
            is HikerSelection.Error -> userId == id
            HikerSelection.None -> false
        }

    private fun updateSelectionLocation(location: GeographicPoint) {
        _hikerSelection.value =
            when (val current = _hikerSelection.value) {
                is HikerSelection.Loading -> current.copy(location = location)
                is HikerSelection.Loaded -> current.copy(location = location)
                is HikerSelection.Error -> current.copy(location = location)
                HikerSelection.None -> current
            }
    }

    private fun fetchAndCacheUser(userId: ULong) {
        viewModelScope.launch {
            when (val res = userService.getUserByIdentifier(userId.toString())) {
                is Success -> {
                    userCache[userId] = res.value
                    if (_hikerSelection.value.matchesHiker(userId)) {
                        val location = hikers[userId] ?: return@launch
                        _hikerSelection.value = HikerSelection.Loaded(userId, location, res.value)
                    }
                }
                is Failure -> {
                    logger.e { "Could not fetch user info for uid=$userId: ${res.message}" }
                    if (_hikerSelection.value.matchesHiker(userId)) {
                        val location = hikers[userId] ?: return@launch
                        _hikerSelection.value = HikerSelection.Error(userId, location, res.message)
                    }
                }
            }
        }
    }

    fun selectHiker(userId: ULong) {
        val current = _hikerSelection.value
        if (current.matchesHiker(userId)) {
            _hikerSelection.value = HikerSelection.None
            return
        }

        val location = hikers[userId] ?: return
        val cachedUser = userCache[userId]

        if (cachedUser != null) {
            _hikerSelection.value = HikerSelection.Loaded(userId, location, cachedUser)
        } else {
            // fallback, caso o fetch inicial (ao entrar o hiker) tenha falhado
            _hikerSelection.value = HikerSelection.Loading(userId, location)
            fetchAndCacheUser(userId)
        }
    }

    fun clearHikerSelection() {
        _hikerSelection.value = HikerSelection.None
    }
}

sealed interface HikerSelection {
    data object None : HikerSelection

    data class Loading(
        val userId: ULong,
        val location: GeographicPoint,
    ) : HikerSelection

    data class Loaded(
        val userId: ULong,
        val location: GeographicPoint,
        val user: UserDto,
    ) : HikerSelection

    data class Error(
        val userId: ULong,
        val location: GeographicPoint,
        val message: String,
    ) : HikerSelection
}
