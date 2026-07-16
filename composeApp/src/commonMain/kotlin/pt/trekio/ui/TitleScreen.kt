package pt.trekio.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import pt.trekio.misc.Either
import pt.trekio.repos.UserRepository
import pt.trekio.services.FailingService
import pt.trekio.services.user.UserService
import pt.trekio.ui.utils.GradientButton
import pt.trekio.ui.utils.titleIntermediate
import pt.trekio.viewmodels.states.TitleState
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.auth_title
import trekio.composeapp.generated.resources.welcome_back_user_text
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleScreen(
    onAuthenticateClick: () -> Unit,
    onLoggedIn: () -> Unit,
    userRepo: UserRepository,
    userService: UserService,
) {
    var checkingLogin by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "(Logo for application)",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.padding(top = 36.dp))

            Text(
                text = "(Some Intro text for the application info)",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.padding(top = 24.dp))

            Text(
                text = "(Developers maybe)",
                style = MaterialTheme.typography.bodyMedium,
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
fun TitleScreenPreview() = TitleScreen({}, {}, FailingService, FailingService)

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
    val circleScale = remember { Animatable(0f) }

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
                circleScale.animateTo(
                    1f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                )

                loginState = TitleState.Success(res.value.username)

                delay(1.seconds)

                circleScale.animateTo(
                    0f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                )

                delay(300.milliseconds)

                scrimAlpha.animateTo(
                    0f,
                    tween(300, easing = FastOutSlowInEasing),
                )

                onLogged()
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

                is TitleState.Success -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 100.dp)
                                    .heightIn(min = 50.dp)
                                    .scale(circleScale.value)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                        ) {
                            Text(
                                text = stringResource(Res.string.welcome_back_user_text, state.username),
                                style = titleIntermediate,
                                color = MaterialTheme.colorScheme.background,
                            )
                        }
                    }
                }

                is TitleState.Failed -> {}
            }
        }
    }
}
