package pt.trekio.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowLeft
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import pt.trekio.platform.OpenUrl
import pt.trekio.services.FailingService
import pt.trekio.ui.utils.CustomTextField
import pt.trekio.ui.utils.GradientButton
import pt.trekio.ui.utils.TopBarCreator
import pt.trekio.ui.utils.titleIntermediate
import pt.trekio.viewmodels.AuthViewModel
import pt.trekio.viewmodels.states.AuthState
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.auth_title
import trekio.composeapp.generated.resources.email_holder_text
import trekio.composeapp.generated.resources.email_text
import trekio.composeapp.generated.resources.google
import trekio.composeapp.generated.resources.google_icon
import trekio.composeapp.generated.resources.login_extended_text
import trekio.composeapp.generated.resources.login_text
import trekio.composeapp.generated.resources.on_login_success_text
import trekio.composeapp.generated.resources.on_register_success_text
import trekio.composeapp.generated.resources.or_create_account_text
import trekio.composeapp.generated.resources.or_log_account
import trekio.composeapp.generated.resources.password_confirmation_text
import trekio.composeapp.generated.resources.password_holder_text
import trekio.composeapp.generated.resources.password_text
import trekio.composeapp.generated.resources.register_text
import trekio.composeapp.generated.resources.sign_up_extended_text
import trekio.composeapp.generated.resources.switch_login_text
import trekio.composeapp.generated.resources.switch_sign_up_text
import trekio.composeapp.generated.resources.username_holder_text
import trekio.composeapp.generated.resources.username_text
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onBack: () -> Unit,
    onAuthSuccess: () -> Unit,
    vm: AuthViewModel,
) {
    val state by vm.state.collectAsState()
    val googleState by vm.googleState.collectAsState()

    var onRegister by remember { mutableStateOf(false) }
    var success by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is AuthState.Success) success = true
    }

    if (googleState != null) {
        OpenUrl(googleState!!)
        vm.cleanupGoogle()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        TopBarCreator(stringResource(Res.string.auth_title), onBack)

        AnimatedContent(
            targetState = onRegister,
            transitionSpec = {
                if (targetState) {
                    slideInHorizontally(
                        initialOffsetX = { it },
                    ) + fadeIn() togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { -it },
                        ) + fadeOut()
                } else {
                    slideInHorizontally(
                        initialOffsetX = { -it },
                    ) + fadeIn() togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { it },
                        ) + fadeOut()
                }.using(
                    SizeTransform(clip = false),
                )
            },
        ) { register ->
            AuthColumn(
                onRegister = register,
                onRegisterChanged = { onRegister = it },
                state = state,
                vm = vm,
            )
        }

        if (success) {
            AuthSuccessAnimation(onFinish = onAuthSuccess, onRegister = onRegister)
        }
    }
}

@Preview
@Composable
fun AuthScreenPreview() = AuthScreen({}, {}, AuthViewModel(FailingService))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthColumn(
    onRegister: Boolean,
    onRegisterChanged: (Boolean) -> Unit,
    state: AuthState,
    vm: AuthViewModel,
) {
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(top = 130.dp),
    ) {
        Text(
            text = if (!onRegister) stringResource(Res.string.login_extended_text) else stringResource(Res.string.sign_up_extended_text),
            style = titleIntermediate,
        )

        Spacer(Modifier.height(30.dp))

        GradientButton(
            onClick = { vm.googleAuth() },
            modifier = Modifier.width(60.dp),
            shape = CircleShape,
        ) {
            Image(
                painter = painterResource(Res.drawable.google_icon),
                contentDescription = stringResource(Res.string.google),
                modifier = Modifier.size(30.dp),
            )
        }

        Spacer(Modifier.height(30.dp))

        Text(
            text = if (!onRegister) stringResource(Res.string.or_log_account) else stringResource(Res.string.or_create_account_text),
            style = titleIntermediate,
        )

        Spacer(Modifier.height(30.dp))

        AuthFields(onRegister, state, vm)

        Spacer(Modifier.height(30.dp))

        Text(
            text = if (!onRegister) stringResource(Res.string.switch_sign_up_text) else stringResource(Res.string.switch_login_text),
            style = titleIntermediate,
        )

        Spacer(Modifier.height(30.dp))

        SwapAuthButton(onRegister, onRegisterChanged)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthFields(
    onRegister: Boolean,
    state: AuthState,
    vm: AuthViewModel,
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf("") }
    var visibleConfirm by remember { mutableStateOf(false) }
    val error = (state as? AuthState.Error)?.message

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (onRegister) {
            CustomTextField(
                value = username,
                onValueChange = { username = it },
                label = Res.string.username_text,
                placeholder = Res.string.username_holder_text,
                modifier = Modifier.width(250.dp),
                leadingIcon = Icons.Default.Person,
            )

            Spacer(Modifier.height(15.dp))
        }

        CustomTextField(
            value = email,
            onValueChange = { email = it },
            label = Res.string.email_text,
            placeholder = Res.string.email_holder_text,
            modifier = Modifier.width(250.dp),
            leadingIcon = Icons.Default.Email,
        )

        Spacer(modifier = Modifier.height(15.dp))

        PasswordCustomTextField(
            value = password,
            onValueChange = { password = it },
            label = Res.string.password_text,
            placeholder = Res.string.password_holder_text,
            leadingIcon = Icons.Default.Lock,
            visible = visible,
            onVisibleChange = { visible = !visible },
        )

        if (onRegister) {
            Spacer(modifier = Modifier.height(15.dp))

            PasswordCustomTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = Res.string.password_confirmation_text,
                placeholder = Res.string.password_holder_text,
                leadingIcon = Icons.Default.Lock,
                visible = visibleConfirm,
                onVisibleChange = { visibleConfirm = !visibleConfirm },
            )
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.width(250.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        AuthSubmitButton(onRegister, state, vm, username, email, password, confirmPassword)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordCustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: StringResource,
    placeholder: StringResource,
    leadingIcon: ImageVector,
    visible: Boolean,
    onVisibleChange: () -> Unit,
) {
    CustomTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        visualTransformation =
            if (visible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
        trailingIcon = {
            IconButton(
                onClick = onVisibleChange,
                modifier = Modifier.size(20.dp),
            ) {
                Icon(
                    imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        },
        modifier = Modifier.width(250.dp),
        autoComplete = true,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthSubmitButton(
    onRegister: Boolean,
    state: AuthState,
    vm: AuthViewModel,
    username: String,
    email: String,
    password: String,
    confirmPassword: String,
) {
    val isLoading = state is AuthState.Loading

    GradientButton(
        onClick = {
            if (onRegister) {
                vm.register(username, email, password, confirmPassword)
            } else {
                vm.login(email, password)
            }
        },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 125.dp),
        enabled =
            !isLoading &&
                (
                    (
                        onRegister &&
                            username.isNotBlank() &&
                            email.isNotBlank() &&
                            password.isNotBlank() &&
                            confirmPassword.isNotBlank()
                    ) ||
                        (!onRegister && email.isNotBlank() && password.isNotBlank())
                ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(
                text = stringResource(if (onRegister) Res.string.register_text else Res.string.login_text),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwapAuthButton(
    onRegister: Boolean,
    onRegisterChanged: (Boolean) -> Unit,
) {
    GradientButton(
        onClick = { onRegisterChanged(!onRegister) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 125.dp),
    ) {
        if (onRegister) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowLeft,
                contentDescription = "Arrow",
            )

            Spacer(Modifier.width(5.dp))
        }

        Text(text = if (!onRegister) stringResource(Res.string.register_text) else stringResource(Res.string.login_text))

        if (!onRegister) {
            Spacer(Modifier.width(5.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowRight,
                contentDescription = "Arrow",
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthSuccessAnimation(
    onFinish: () -> Unit,
    onRegister: Boolean,
) {
    val scrimAlpha = remember { Animatable(0f) }
    val circleScale = remember { Animatable(0f) }
    val checkScale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scrimAlpha.animateTo(
            1f,
            tween(300, easing = FastOutSlowInEasing),
        )

        circleScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        )

        delay(100.milliseconds)

        checkScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        )

        delay(1.seconds)

        scrimAlpha.animateTo(
            0f,
            tween(300, easing = FastOutSlowInEasing),
        )

        onFinish()
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
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 100.dp)
                    .heightIn(min = 150.dp)
                    .scale(circleScale.value)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier.graphicsLayer {
                        scaleX = checkScale.value
                        scaleY = checkScale.value
                        transformOrigin = TransformOrigin.Center
                    },
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(
                                75.dp,
                            ).background(MaterialTheme.colorScheme.primary, CircleShape)
                            .border(2.dp, Color.White, CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(50.dp),
                    )
                }

                Spacer(Modifier.height(5.dp))

                Text(
                    text =
                        if (!onRegister) {
                            stringResource(
                                Res.string.on_login_success_text,
                            )
                        } else {
                            stringResource(Res.string.on_register_success_text)
                        },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
