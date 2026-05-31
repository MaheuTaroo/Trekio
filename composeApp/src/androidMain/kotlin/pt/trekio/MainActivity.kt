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
import io.ktor.serialization.kotlinx.json.json
import pt.trekio.misc.Routes.BASE_URL
import pt.trekio.services.user.UserHttpService

class MainActivity : ComponentActivity() {
    val Context.userDataStore by preferencesDataStore(name = "user_prefs")

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val userRepo = UserDataRepo(userDataStore)
        val httpClient =
            HttpClient {
                defaultRequest {
                    url(BASE_URL)
                }
                install(ContentNegotiation) { json() }
            }
        val userService = UserHttpService(httpClient, userRepo)

        setContent {
            Scaffold(
                contentWindowInsets = WindowInsets.systemBars,
            ) {
                App(userService)
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
