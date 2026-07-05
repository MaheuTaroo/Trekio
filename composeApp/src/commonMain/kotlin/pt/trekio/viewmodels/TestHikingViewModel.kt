package pt.trekio.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.tiagopraia.kmp.mapbox.GeographicPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pt.trekio.dto.TrailDto
import pt.trekio.misc.Success
import pt.trekio.services.hikes.HikeService
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

    val path =
        listOf(GeographicPoint(trail.start.lat, trail.start.lon, trail.start.alt)) +
            trail.path.map { GeographicPoint(it.lat, it.lon, it.alt) } +
            GeographicPoint(trail.end.lat, trail.end.lon, trail.end.alt)

    init {
        viewModelScope.launch {
            val res = service.startHike(trail.id)
            if (res is Success) {
                try {
                    delay(30.seconds)
                } finally {
                    res.value.cancel()
                }
            }
        }
    }
}
