package pt.trekio

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import androidx.datastore.preferences.preferencesDataStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import pt.trekio.services.user.UserHttpService

class MainActivity : ComponentActivity() {
    val Context.userDataStore by preferencesDataStore(name = "user_prefs")

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val userRepo = UserDataRepo(userDataStore)
        val httpClient =
            HttpClient(OkHttp) {
                defaultRequest {
                    url("http://10.0.2.2:8080")
                }
                install(ContentNegotiation) { json() }
            }
        val userService = UserHttpService(httpClient, userRepo)

        setContent {
            Scaffold {
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
