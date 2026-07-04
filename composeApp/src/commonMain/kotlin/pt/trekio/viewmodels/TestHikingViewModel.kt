package pt.trekio.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch
import pt.trekio.services.hikes.HikeService

class TestHikingViewModel(
    service: HikeService,
    trailId: ULong,
): ViewModel() {
    companion object {
        fun getFactory(hikeService: HikeService, trailId: ULong) =
            viewModelFactory {
                initializer {
                    TestHikingViewModel(hikeService, trailId)
                }
            }
    }

    init {
        viewModelScope.launch {
            service.startHike(trailId)
        }
    }
}
