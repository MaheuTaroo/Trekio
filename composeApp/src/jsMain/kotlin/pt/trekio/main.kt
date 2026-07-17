package pt.trekio

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.lifecycle.viewmodel.compose.viewModel
import co.touchlab.kermit.Logger
import pt.trekio.repos.SettingsRepository
import pt.trekio.services.FailingService
import pt.trekio.ui.MapScreen
import pt.trekio.viewmodels.MapViewModel
import pt.trekio.viewmodels.SettingsViewModel

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        // TODO: mudar para serviços corretos; atualmente só está assim para compilar
        val vm = viewModel<MapViewModel>(factory = MapViewModel.getFactory(FailingService))
        val settingsRepo = SettingsRepository()
        val settingsVm =
            viewModel<SettingsViewModel>(
                factory = SettingsViewModel.getFactory(settingsRepo, FailingService),
            )
        MapScreen(
            vm,
            { Logger.i("Profile") },
            { Logger.i("Trails") },
            {},
            {},
            settingsVm,
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
