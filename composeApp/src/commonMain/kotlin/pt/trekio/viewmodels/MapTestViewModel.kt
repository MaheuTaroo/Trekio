package pt.trekio.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import pt.trekio.dto.TrailPointDto
import pt.trekio.misc.GeoPoint
import pt.trekio.services.user.UserService
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class MapTestViewModel : ViewModel() {
    companion object {
        fun getFactory() =
            viewModelFactory {
                initializer {
                    MapTestViewModel()
                }
            }
    }

    val coordinates = flow {
        emit("{\"type\": \"Point\", \"coordinates\":[0.0, 0.0, 0.0]}")

        while (true) {
            delay(2500.milliseconds)
            emit(
                "{\"type\": \"Point\", \"coordinates\":[" +
                        "${Random.nextDouble(-180.0, 180.0)}," +
                        "${Random.nextDouble(-90.0, 90.0)}," +
                        "${Random.nextDouble(150.0)}]}"
            )
        }
    }
}