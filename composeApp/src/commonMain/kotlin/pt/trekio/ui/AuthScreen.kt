package pt.trekio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import pt.trekio.platform.OpenUrl
import pt.trekio.services.FailingService
import pt.trekio.ui.utils.GradientButton
import pt.trekio.ui.utils.TopBarCreator
import pt.trekio.viewmodels.AuthViewModel
import pt.trekio.viewmodels.states.AuthState
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.auth_title
import trekio.composeapp.generated.resources.click_here_text
import trekio.composeapp.generated.resources.email_text
import trekio.composeapp.generated.resources.google
import trekio.composeapp.generated.resources.google_auth_text
import trekio.composeapp.generated.resources.google_icon
import trekio.composeapp.generated.resources.login_text
import trekio.composeapp.generated.resources.password_confirmation_text
import trekio.composeapp.generated.resources.password_text
import trekio.composeapp.generated.resources.register_text
import trekio.composeapp.generated.resources.switch_create_text
import trekio.composeapp.generated.resources.switch_login_text
import trekio.composeapp.generated.resources.username_text

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onBack: () -> Unit,
    onAuthSuccess: () -> Unit,
    vm: AuthViewModel,
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val state by vm.state.collectAsState()
    val googleState by vm.googleState.collectAsState()

    var onRegister by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is AuthState.Success) onAuthSuccess()
    }

    if (googleState != null) {
        OpenUrl(googleState!!)
        vm.cleanupGoogle()
    }

    val isLoading = state is AuthState.Loading
    val error = (state as? AuthState.Error)?.message

    TopBarCreator(stringResource(Res.string.auth_title), onBack)

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
    ) {
        GradientButton(
            onClick = { vm.googleAuth() },
            modifier = Modifier.width(250.dp),
        ) {
            Icon(
                painter = painterResource(Res.drawable.google_icon),
                contentDescription = stringResource(Res.string.google),
                modifier = Modifier.size(30.dp),
            )

            Spacer(Modifier.width(15.dp))

            Text(
                text = stringResource(Res.string.google_auth_text),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Spacer(Modifier.height(40.dp))

        Row(
            modifier = Modifier.fillMaxWidth(0.8f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Spacer(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline),
            )
            Text(
                text = "OR",
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline),
            )
        }

        Spacer(Modifier.height(40.dp))

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (onRegister) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    singleLine = true,
                    label = { Text(stringResource(Res.string.username_text)) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.width(250.dp),
                )

                Spacer(Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                singleLine = true,
                label = { Text(stringResource(Res.string.email_text)) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.width(250.dp),
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                singleLine = true,
                label = { Text(stringResource(Res.string.password_text)) },
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.width(250.dp),
            )

            if (onRegister) {
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    singleLine = true,
                    label = { Text(stringResource(Res.string.password_confirmation_text)) },
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.width(250.dp),
                )
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.width(250.dp),
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            GradientButton(
                onClick = {
                    if (onRegister) {
                        vm.register(username, email, password, confirmPassword)
                    } else {
                        vm.login(email, password)
                    }
                },
                modifier = Modifier.width(120.dp),
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

        Spacer(Modifier.height(40.dp))

        Row {
            Text(
                text = stringResource(if (onRegister) Res.string.switch_login_text else Res.string.switch_create_text),
                style = MaterialTheme.typography.bodyLarge,
            )

            Spacer(Modifier.width(5.dp))

            Text(
                text = stringResource(Res.string.click_here_text),
                color = Color.Blue,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.clickable(onClick = { onRegister = !onRegister }),
            )
        }
    }
}

@Preview
@Composable
fun AuthScreenPreview() = AuthScreen({}, {}, AuthViewModel(FailingService))
