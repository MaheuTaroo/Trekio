package pt.trekio.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.tiagopraia.kmp.mapbox.GeographicPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.trekio.dto.TrailDto
import pt.trekio.misc.Failure
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.Success
import pt.trekio.services.hikes.HikeService
import pt.trekio.viewmodels.states.HikeState
import kotlin.time.Duration.Companion.seconds

class TestHikingViewModel(
    service: HikeService,
    trail: TrailDto,
) : ViewModel() {
    companion object {
        fun getFactory(
            hikeService: HikeService,
            trail: TrailDto,
        ) = viewModelFactory {
            initializer {
                TestHikingViewModel(hikeService, trail)
            }
        }
    }

    private var _state = MutableStateFlow<HikeState>(HikeState.Loading)
    val state: StateFlow<HikeState> = _state.asStateFlow()

    val path =
        listOf(GeographicPoint(trail.start.lat, trail.start.lon, trail.start.alt)) +
            trail.path.map { GeographicPoint(it.lat, it.lon, it.alt) } +
            GeographicPoint(trail.end.lat, trail.end.lon, trail.end.alt)

    init {
        viewModelScope.launch {
            val res = service.startHike(trail.id)

            if (res is Failure) {
                _state.emit(HikeState.Error(res.message))
                cancel()
                return@launch
            }

            _state.emit(HikeState.Hiking)
            val comm = (res as Success).value
            try {
                while (true) {
                    delay(3.seconds)
                    if (!comm.sendLocation(GeoPoint(.0, .0, .0))) {
                        break
                    }
                }
            } finally {
                _state.emit(HikeState.Stopped)
                cancel()
            }
        }
    }
}
