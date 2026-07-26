package pt.trekio.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import pt.trekio.misc.Either
import pt.trekio.repos.UserRepository
import pt.trekio.services.FailingService
import pt.trekio.services.user.UserService
import pt.trekio.ui.theme.ThemeMode
import pt.trekio.ui.theme.TrekioAppTheme
import pt.trekio.ui.utils.GradientButton
import pt.trekio.ui.utils.SuccessAnimation
import pt.trekio.viewmodels.SettingsViewModel
import pt.trekio.viewmodels.states.TitleState
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.auth_title
import trekio.composeapp.generated.resources.dark_theme_icon
import trekio.composeapp.generated.resources.light_theme_icon
import trekio.composeapp.generated.resources.welcome_back_user_text

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleScreen(
    onAuthenticateClick: () -> Unit,
    onLoggedIn: () -> Unit,
    userRepo: UserRepository,
    userService: UserService,
    settingsVm: SettingsViewModel,
) {
    var checkingLogin by remember { mutableStateOf(true) }
    val theme by settingsVm.theme.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val icon =
                if (theme == ThemeMode.DARK || isSystemInDarkTheme()) {
                    painterResource(Res.drawable.dark_theme_icon)
                } else {
                    painterResource(Res.drawable.light_theme_icon)
                }
            Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(250.dp),
            )

            Spacer(modifier = Modifier.padding(top = 8.dp))

            Text(
                text = "Trekio",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.padding(top = 36.dp))

            if (!checkingLogin) {
                GradientButton(
                    onClick = onAuthenticateClick,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 100.dp),
                ) {
                    Text(stringResource(Res.string.auth_title))
                }
            }
        }
        if (checkingLogin) {
            LoggedAnimation(
                onLoggedIn,
                userRepo,
                userService,
            ) { checkingLogin = false }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun LightTitleScreenPreview() = TitleScreen({}, {}, FailingService, FailingService, SettingsViewModel(FailingService, FailingService))

@Preview(uiMode = UI_MODE_NIGHT_YES, showSystemUi = true)
@Composable
fun DarkTitleScreenPreview() =
    TrekioAppTheme(ThemeMode.DARK) {
        TitleScreen({}, {}, FailingService, FailingService, SettingsViewModel(FailingService, FailingService))
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoggedAnimation(
    onLogged: () -> Unit,
    userRepo: UserRepository,
    userService: UserService,
    onFinished: () -> Unit,
) {
    var loginState by remember { mutableStateOf<TitleState>(TitleState.Loading) }
    val scrimAlpha = remember { Animatable(0f) }
    var showAnimation by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scrimAlpha.animateTo(
            1f,
            tween(300, easing = FastOutSlowInEasing),
        )

        val stored = userRepo.getTokens() != null

        if (!stored) {
            loginState = TitleState.Failed
            onFinished()
            scrimAlpha.animateTo(
                0f,
                tween(300, easing = FastOutSlowInEasing),
            )
            return@LaunchedEffect
        }

        when (val res = userService.getSelfDetails()) {
            is Either.Success -> {
                showAnimation = true
                name = res.value.username
            }
            is Either.Failure -> {
                userRepo.clear()
                loginState = TitleState.Failed
                scrimAlpha.animateTo(
                    0f,
                    tween(300, easing = FastOutSlowInEasing),
                )
                onFinished()
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = scrimAlpha.value
                }.background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
    ) {
        AnimatedContent(
            targetState = loginState,
        ) { state ->
            when (state) {
                is TitleState.Loading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                else -> {}
            }
        }
    }

    if (showAnimation) {
        SuccessAnimation(
            onFinish = onLogged,
            text = stringResource(Res.string.welcome_back_user_text, name ?: ""),
        )
    }
}
