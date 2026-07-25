package pt.trekio.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.trekio.misc.Failure
import pt.trekio.misc.Success
import pt.trekio.repos.UserRepository
import pt.trekio.services.trails.TrailService
import pt.trekio.viewmodels.states.TrailFetchState

class TrailFetchViewModel(
    private val userRepo: UserRepository,
    private val service: TrailService,
) : ViewModel() {
    companion object {
        fun getFactory(
            userRepo: UserRepository,
            service: TrailService,
        ) =
            viewModelFactory {
                initializer {
                    TrailFetchViewModel(userRepo, service)
                }
            }
    }

    private val _state by lazy {
        MutableStateFlow<TrailFetchState>(TrailFetchState.Idle)
    }
    val state = _state.asStateFlow()

    fun fetchPage(page: ULong = 0uL) {
        _state.value = TrailFetchState.Loading

        viewModelScope.launch {
            val res = service.getAllTrails(page)
            _state.value =
                if (res is Failure) {
                    TrailFetchState.Error(res.message)
                } else {
                    TrailFetchState.TrailsSuccess((res as Success).value.trails)
                }
        }
    }

    fun fetchTrailsByName(
        name: String,
        page: ULong = 0uL
    ) {
        _state.value = TrailFetchState.Loading
        viewModelScope.launch {
            val res = service.getAllTrails(page)
            _state.value =
                if (res is Failure) {
                    TrailFetchState.Error(res.message)
                } else {
                    TrailFetchState.TrailsSuccess((res as Success).value.trails.filter { it.name.contains(name, true) })
                }
        }
    }

    fun fetchPersonalTrails(page: ULong = 0uL) {
        _state.value = TrailFetchState.Loading
        viewModelScope.launch {
            val userId = userRepo.getOwnDetails()?.id
            if (userId == null) {
                _state.value = TrailFetchState.Error("User Not Found")
                return@launch
            }
            val res = service.getTrailsOf(userId, page)
            _state.value =
                if (res is Failure) {
                    TrailFetchState.Error(res.message)
                } else {
                    TrailFetchState.TrailsSuccess((res as Success).value.trails)
                }
        }
    }

    fun deleteTrail(trailId: ULong) {
        _state.value = TrailFetchState.Loading
        viewModelScope.launch {
            val res = service.deleteTrail(trailId)
            _state.value =
                if (res is Failure) {
                    TrailFetchState.Error(res.message)
                } else {
                    TrailFetchState.Success
                }
        }
    }

    fun updateTrail(trailId: ULong, name: String) {
        _state.value = TrailFetchState.Loading
        viewModelScope.launch {
            val res = service.updateTrail(trailId, name, null)
            _state.value =
                if (res is Failure) {
                    TrailFetchState.UpdateError(res.message)
                } else {
                    TrailFetchState.Success
                }
        }
    }
}
