package pt.trekio.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch
import pt.trekio.misc.Failure
import pt.trekio.misc.Success
import pt.trekio.services.trails.TrailService
import pt.trekio.viewmodels.states.TrailFetchState

class TrailFetchViewModel(
    private val service: TrailService,
) : ViewModel() {
    companion object {
        fun getFactory(service: TrailService) =
            viewModelFactory {
                initializer {
                    TrailFetchViewModel(service)
                }
            }
    }

    var state by mutableStateOf<TrailFetchState>(TrailFetchState.Idle)
        private set

    init {
        fetchPage()
    }

    fun fetchPage(page: ULong = 0uL) {
        state = TrailFetchState.Loading

        viewModelScope.launch {
            val res = service.getAllTrails(page)
            state =
                if (res is Failure) {
                    TrailFetchState.Error(res.message)
                } else {
                    TrailFetchState.Success((res as Success).value.trails)
                }
        }
    }
}
