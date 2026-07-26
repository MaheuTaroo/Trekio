package pt.trekio.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.trekio.dto.StatisticsDto
import pt.trekio.misc.Failure
import pt.trekio.misc.HikeInfo
import pt.trekio.misc.Success
import pt.trekio.repos.UserRepository
import pt.trekio.services.hikes.HikeService
import pt.trekio.services.trails.TrailService
import pt.trekio.services.user.UserService
import pt.trekio.viewmodels.states.UserProfileState

class UserProfileViewModel(
    private val userService: UserService,
    private val trailService: TrailService,
    private val hikeService: HikeService,
    private val userRepo: UserRepository,
) : ViewModel() {
    companion object {
        fun getFactory(
            userService: UserService,
            trailService: TrailService,
            hikeService: HikeService,
            repo: UserRepository,
        ) = viewModelFactory {
            initializer {
                UserProfileViewModel(
                    userService,
                    trailService,
                    hikeService,
                    repo,
                )
            }
        }

        private const val TAG = "UserProfileViewModel"

        private fun embedErrorMessage(msg: String) = "User Profile Details failure: $msg"
    }

    private val _state by lazy {
        MutableStateFlow<UserProfileState>(UserProfileState.Idle)
    }
    val state = _state.asStateFlow()

    var statistics by mutableStateOf<StatisticsDto?>(null)
    private var _hikes = MutableStateFlow<List<HikeInfo>>(emptyList())
    val hikes: StateFlow<List<HikeInfo>> = _hikes.asStateFlow()

    var page by mutableStateOf(0uL)
        private set

    init {
        _state.value = UserProfileState.Loading
        viewModelScope.launch {
            val user = userRepo.getOwnDetails()
            if (user == null) {
                val message = "Could not find own statistics"
                Logger.e(tag = TAG) { embedErrorMessage(message) }
                _state.emit(UserProfileState.Error(message))
                return@launch
            }
            val statRes = userService.getStatsOf(user.id)
            if (statRes is Failure) {
                Logger.e(tag = TAG) { embedErrorMessage(statRes.message) }
                _state.emit(UserProfileState.Error(statRes.message))
                return@launch
            }
            val stats = (statRes as Success).value
            statistics = stats.copy(totalTime = stats.totalTime / 1000)

            fetchHikePage()
        }
    }

    var canIncrementPage by mutableStateOf(false)

    var canDecrementPage by mutableStateOf(page != 0uL)

    private suspend fun fetchHikePage() {
        _state.emit(UserProfileState.FetchingHikes)
        val hikes = hikeService.getMyFinishedHikes(page)
        if (hikes is Failure) {
            Logger.e(tag = TAG) { embedErrorMessage("could not fetch hikes - ${hikes.message}") }
            _state.emit(UserProfileState.Error(hikes.message))
            return
        }

        val data = (hikes as Success).value
        val infoList = mutableListOf<HikeInfo>()
        for (hike in data.hikes) {
            if (hike.finish == null) {
                Logger.e(tag = TAG) { embedErrorMessage("hike with ID = ${hike.id} is not marked as finished") }
                _state.emit(UserProfileState.Error("One of the hikes is not finished yet"))
                return
            }

            val trail = trailService.getTrailDetails(hike.trail)
            if (trail is Failure) {
                _state.emit(UserProfileState.Error(trail.message))
                return
            }

            val trailData = (trail as Success).value
            infoList.add(
                HikeInfo(
                    hike.id,
                    trailData.name,
                    trailData.distance,
                    trailData.difficulty,
                    (hike.finish!! - hike.start) / 1000,
                ),
            )
        }
        _hikes.emit(infoList)
        Logger.i(tag = TAG) { "data.hasMore = ${data.hasMore}, page = $page" }
        canIncrementPage = data.hasMore
        canDecrementPage = page != 0uL
        _state.emit(UserProfileState.Success)
    }

    fun getNextPage() {
        if (!canIncrementPage) {
            return
        }

        page++
        viewModelScope.launch {
            fetchHikePage()
        }
    }

    fun getPreviousPage() {
        if (!canDecrementPage) {
            return
        }

        page--
        viewModelScope.launch {
            fetchHikePage()
        }
    }
}
