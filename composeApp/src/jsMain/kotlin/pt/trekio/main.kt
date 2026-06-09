package pt.trekio

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.lifecycle.viewmodel.compose.viewModel
import co.touchlab.kermit.Logger
import pt.trekio.ui.MapScreen
import pt.trekio.viewmodels.MapViewModel

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        val vm = viewModel<MapViewModel>(factory = MapViewModel.getFactory())
        MapScreen(
            vm,
            { Logger.i("Profile") },
            { Logger.i("Trails") },
        )
    }
//    val dataStore = createDataStore()
//    val userRepo = UserDataRepo(dataStore)
//    val httpClient =
//        HttpClient(Js) {
//            defaultRequest {
//                url(BASE_URL)
//            }
//            install(ContentNegotiation) { json() }
//        }
//    val userService = UserHttpService(httpClient, userRepo)
//
//    ComposeViewport(document.body!!) {
//        App(userService)
//    }
}
