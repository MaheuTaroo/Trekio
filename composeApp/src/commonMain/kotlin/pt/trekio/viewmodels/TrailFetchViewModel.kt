package pt.trekio.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
                    TrailFetchState.Success((res as Success).value.trails)
                }
        }
    }
}
