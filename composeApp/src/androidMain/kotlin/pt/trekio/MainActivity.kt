package pt.trekio

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.datastore.preferences.preferencesDataStore
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import pt.trekio.misc.Routes.BASE_URL
import pt.trekio.platform.AppEnvironment
import pt.trekio.services.hikes.HikeHttpService
import pt.trekio.services.trails.TrailHttpService
import pt.trekio.services.user.UserHttpService

class MainActivity : ComponentActivity() {
    val Context.userDataStore by preferencesDataStore(name = DATASTORE_FILENAME)

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val userRepo = UserDataRepository(userDataStore)
        val httpClient =
            HttpClient {
                val prettyButLaxJson =
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    }

                defaultRequest {
                    url(BASE_URL)
                }
                install(ContentNegotiation) { json(prettyButLaxJson) }
                install(WebSockets) {
                    contentConverter = KotlinxWebsocketSerializationConverter(prettyButLaxJson)
                    pingIntervalMillis = 5_000
                }
            }
        val userService = UserHttpService(userRepo, httpClient)
        val trailService = TrailHttpService(userRepo, httpClient)
        val hikeService = HikeHttpService(userRepo, httpClient)

        setContent {
            AppEnvironment {
                Scaffold(
                    contentWindowInsets = WindowInsets.systemBars,
                ) {
                    App(userService, trailService, hikeService, userRepo)
                }
            }
        }
    }
}

/*
@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
 */
