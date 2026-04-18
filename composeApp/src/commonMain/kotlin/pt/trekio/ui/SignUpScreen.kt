package pt.trekio.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import pt.trekio.ui.utils.GradientButton
import pt.trekio.ui.utils.TopBarCreator
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.email_text
import trekio.composeapp.generated.resources.google
import trekio.composeapp.generated.resources.google_icon
import trekio.composeapp.generated.resources.password_text
import trekio.composeapp.generated.resources.sign_up_google
import trekio.composeapp.generated.resources.sign_up_title
import trekio.composeapp.generated.resources.username_text

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onSignUp: () -> Unit,
    onGoogleSignUp: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    TopBarCreator(stringResource(Res.string.sign_up_title), onBack)
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
    ) {
        GradientButton(
            onClick = onGoogleSignUp,
            modifier = Modifier.width(230.dp),
        ) {
            Icon(
                painter = painterResource(Res.drawable.google_icon),
                contentDescription = stringResource(Res.string.google),
                modifier = Modifier.size(30.dp),
            )
            Spacer(Modifier.width(15.dp))
            Text(
                text = stringResource(Res.string.sign_up_google),
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
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                singleLine = true,
                label = { Text(stringResource(Res.string.username_text)) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.width(250.dp),
            )

            Spacer(modifier = Modifier.height(10.dp))

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

            Spacer(modifier = Modifier.height(18.dp))

            GradientButton(
                onClick = onSignUp,
                modifier = Modifier.width(120.dp),
                enabled = username.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
            ) {
                Text(
                    text = stringResource(Res.string.sign_up_title),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun SignUpScreenPreview() = SignUpScreen({}, {}, {})
