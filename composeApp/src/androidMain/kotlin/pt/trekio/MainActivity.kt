package pt.trekio

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.core.net.toUri
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.Protocol
import pt.trekio.misc.ApiRoutes.DeepLink
import pt.trekio.misc.Failure
import pt.trekio.misc.OAuthDeepLinkBus
import pt.trekio.misc.OAuthDeepLinkEvent
import pt.trekio.misc.Routes
import pt.trekio.misc.Routes.BASE_URL
import pt.trekio.platform.AppEnvironment
import pt.trekio.services.hikes.HikeHttpService
import pt.trekio.services.trails.TrailHttpService
import pt.trekio.services.user.UserHttpService

private val Context.userDataStore by preferencesDataStore(name = DATASTORE_FILENAME)

class MainActivity : ComponentActivity() {
    private lateinit var userService: UserHttpService
    private lateinit var userRepo: UserDataRepository

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        userRepo = UserDataRepository(userDataStore)
        val httpClient =
            HttpClient(OkHttp) {
                val prettyButLaxJson =
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    }

                engine {
                    config {
                        protocols(listOf(Protocol.HTTP_1_1))
                    }
                }

                defaultRequest {
                    url(BASE_URL)
                    header("ngrok-skip-browser-warning", "true") // NGROK THINGS
                }
                install(ContentNegotiation) { json(prettyButLaxJson) }
                install(WebSockets) {
                    contentConverter = KotlinxWebsocketSerializationConverter(prettyButLaxJson)
                    pingIntervalMillis = 5_000
                }
                install(Logging) {
                    logger =
                        object : io.ktor.client.plugins.logging.Logger {
                            override fun log(message: String) {
                                Logger.i(message)
                            }
                        }
                    level = LogLevel.ALL
                }
            }
        userService = UserHttpService(userRepo, httpClient)
        val trailService = TrailHttpService(userRepo, httpClient)
        val hikeService = HikeHttpService(userRepo, httpClient)

        val intent = intent
        Logger.i { "OnCreate intent value: $intent" }
        handleDeepLink(intent)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        val expectedUri = DeepLink.path.toUri()

        Logger.i { "URI from handleDeepLink: $uri" }
        if (!uri.matches(expectedUri)) return

        val error = uri.getQueryParameter(Routes.ERROR)
        if (error != null) {
            lifecycleScope.launch { OAuthDeepLinkBus.emit(OAuthDeepLinkEvent.Error(error)) }
            return
        }

        val code = uri.getQueryParameter(Routes.CODE) ?: return
        val email = uri.getQueryParameter(Routes.EMAIL) ?: return
        val username = uri.getQueryParameter(Routes.USERNAME) ?: return
        val new = uri.getQueryParameter(Routes.NEW) ?: return

        lifecycleScope.launch {
            val res = userService.googleCallback(code, email, username)
            val event =
                if (res is Failure) {
                    OAuthDeepLinkEvent.Error(res.message)
                } else {
                    OAuthDeepLinkEvent.Success(username, new.toBoolean())
                }
            OAuthDeepLinkBus.emit(event)
        }
    }

    private fun Uri.matches(expected: Uri) = scheme == expected.scheme && host == expected.host && path == expected.path
}

/*
@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
 */
