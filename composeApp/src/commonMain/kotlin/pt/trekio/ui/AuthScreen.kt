package pt.trekio.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowLeft
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import pt.trekio.platform.OpenUrl
import pt.trekio.services.FailingService
import pt.trekio.ui.utils.Action
import pt.trekio.ui.utils.ContentWarning
import pt.trekio.ui.utils.ContentWarningButtons
import pt.trekio.ui.utils.ContentWarningDialog
import pt.trekio.ui.utils.CustomTextField
import pt.trekio.ui.utils.GradientButton
import pt.trekio.ui.utils.SuccessAnimation
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
import trekio.composeapp.generated.resources.new_password_text
import trekio.composeapp.generated.resources.new_username_text
import trekio.composeapp.generated.resources.oauth_leave_blank_text
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onBack: () -> Unit,
    onAuthSuccess: () -> Unit,
    vm: AuthViewModel,
    username: String? = null,
    new: Boolean? = null,
    error: String? = null,
) {
    val state by vm.state.collectAsState()
    val googleState by vm.googleState.collectAsState()

    var onRegister by remember { mutableStateOf(false) }
    var success by remember { mutableStateOf(false) }

    var oauth by remember { mutableStateOf(new == true) }
    var newUsername by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (new == false) success = true
    }

    LaunchedEffect(state) {
        if (state is AuthState.Success) {
            oauth = false
            success = true
        }
    }

    if (googleState != null) {
        OpenUrl(googleState!!)
        vm.cleanupGoogle()
    }

    val vmError = (state as? AuthState.OAuthError)?.message

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
                error = error,
            )
        }

        ConditionalComponents(
            success = success,
            onAuthSuccess = onAuthSuccess,
            onRegister = onRegister,
            onOAuth = oauth,
            onAuthChange = {
                oauth = false
                success = true
            },
            vm = vm,
            username = newUsername,
            onUsernameChange = { newUsername = it },
            password = password,
            onPasswordChange = { password = it },
            visible = visible,
            onVisibleChange = { visible = !visible },
            defaultUsername = username ?: "",
            error = vmError,
            isLoading = state == AuthState.Loading,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AuthScreenPreview() = AuthScreen({}, {}, AuthViewModel(FailingService))

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

@Preview(showBackground = true)
@Composable
fun PasswordCustomTextFieldPreview() =
    PasswordCustomTextField(
        value = "",
        onValueChange = {},
        label = Res.string.password_text,
        placeholder = Res.string.password_holder_text,
        leadingIcon = Icons.Default.Lock,
        visible = false,
        onVisibleChange = {},
    )

private fun enableButton(
    isLoading: Boolean,
    onRegister: Boolean,
    username: String,
    email: String,
    password: String,
    confirmPassword: String,
): Boolean =
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
        )

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
        enabled = enableButton(isLoading, onRegister, username, email, password, confirmPassword),
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

@Preview(showBackground = true)
@Composable
fun LoginSubmitButtonPreview() =
    AuthSubmitButton(
        onRegister = false,
        state = AuthState.Idle,
        vm = AuthViewModel(FailingService),
        username = "",
        email = "",
        password = "",
        confirmPassword = "",
    )

@Preview(showBackground = true)
@Composable
fun RegisterSubmitButtonPreview() =
    AuthSubmitButton(
        onRegister = true,
        state = AuthState.Idle,
        vm = AuthViewModel(FailingService),
        username = "A",
        email = "A",
        password = "A",
        confirmPassword = "A",
    )

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

@Preview(showBackground = true)
@Composable
fun LoginFieldsPreview() = AuthFields(false, AuthState.Idle, AuthViewModel(FailingService))

@Preview(showBackground = true)
@Composable
fun RegisterFieldsPreview() = AuthFields(true, AuthState.Idle, AuthViewModel(FailingService))

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

@Preview(showBackground = true)
@Composable
fun SwapLoginButtonPreview() = SwapAuthButton(false) {}

@Preview(showBackground = true)
@Composable
fun SwapRegisterButtonPreview() = SwapAuthButton(true) {}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthColumn(
    onRegister: Boolean,
    onRegisterChanged: (Boolean) -> Unit,
    state: AuthState,
    vm: AuthViewModel,
    error: String?,
) {
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(top = 130.dp).verticalScroll(rememberScrollState()),
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

        if (error != null) {
            Spacer(Modifier.height(10.dp))

            Text(
                text = error,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
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

@Preview(showBackground = true)
@Composable
fun AuthColumnPreview() =
    AuthColumn(
        onRegister = false,
        onRegisterChanged = {},
        state = AuthState.Idle,
        vm = AuthViewModel(FailingService),
        error = "Error",
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OAuthContent(
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    visible: Boolean,
    onVisibleChange: () -> Unit,
) {
    CustomTextField(
        value = username,
        onValueChange = onUsernameChange,
        label = Res.string.new_username_text,
        placeholder = Res.string.username_holder_text,
        leadingIcon = Icons.Default.Person,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(10.dp))

    CustomTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = Res.string.new_password_text,
        placeholder = Res.string.password_holder_text,
        leadingIcon = Icons.Default.Lock,
        modifier = Modifier.fillMaxWidth(),
        autoComplete = true,
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
    )

    Spacer(Modifier.height(5.dp))

    Text(
        text = stringResource(Res.string.oauth_leave_blank_text),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(15.dp))
}

@Preview(showBackground = true)
@Composable
fun OAuthAccountWarningPreview() =
    ContentWarning(
        action = Action.OAuth,
        surface =
            lerp(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.primary,
                if (MaterialTheme.colorScheme.background.luminance() < 0.5f) 0.22f else 0.14f,
            ),
        color = MaterialTheme.colorScheme.primary,
        onSurface = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFE6EEF5) else Color(0xFF10233A),
        extraText = "Dummy",
    )

@Preview(showBackground = true)
@Composable
fun OAuthAccountButtonsPreview() =
    ContentWarningButtons(
        action = Action.OAuth,
        onDismiss = {},
        onDelete = {},
        isLoading = false,
        confirmed = true,
        gradient =
            listOf(
                lerp(MaterialTheme.colorScheme.primary, Color.White, 0.18f),
                MaterialTheme.colorScheme.primary,
                lerp(MaterialTheme.colorScheme.primary, Color.Black, 0.18f),
            ),
    )

@Preview(showBackground = true)
@Composable
fun OAuthAccountWarningDialogPreview() =
    ContentWarningDialog(
        action = Action.OAuth,
        isDanger = false,
        isLoading = false,
        error = null,
        onDismiss = {},
        onAction = {},
        content = { OAuthContent("", {}, "", {}, false, {}) },
        extraText = "Dummy",
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConditionalComponents(
    success: Boolean,
    onAuthSuccess: () -> Unit,
    onRegister: Boolean,
    onOAuth: Boolean,
    onAuthChange: () -> Unit,
    vm: AuthViewModel,
    defaultUsername: String,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    visible: Boolean,
    onVisibleChange: () -> Unit,
    error: String?,
    isLoading: Boolean,
) {
    if (success) {
        SuccessAnimation(
            onFinish = onAuthSuccess,
            text =
                if (!onRegister) {
                    stringResource(
                        Res.string.on_login_success_text,
                    )
                } else {
                    stringResource(Res.string.on_register_success_text)
                },
        )
    }

    if (onOAuth) {
        ContentWarningDialog(
            action = Action.OAuth,
            isDanger = false,
            isLoading = isLoading,
            onAction = {
                vm.updateUser(
                    username = username.ifBlank { null },
                    password = password.ifBlank { null },
                )
            },
            error = error,
            onDismiss = { onAuthChange() },
            extraText = defaultUsername,
            enabled = username != defaultUsername && (username.isNotBlank() || password.isNotBlank()),
        ) {
            OAuthContent(
                username = username,
                onUsernameChange = onUsernameChange,
                password = password,
                onPasswordChange = onPasswordChange,
                visible = visible,
                onVisibleChange = onVisibleChange,
            )
        }
    }
}
