package pt.trekio.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pt.trekio.services.FailingService
import pt.trekio.ui.utils.DataCard
import pt.trekio.ui.utils.GradientButton
import pt.trekio.ui.utils.TopBarCreator
import pt.trekio.viewmodels.UserProfileState
import pt.trekio.viewmodels.UserProfileViewModel
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.confirm_delete_button
import trekio.composeapp.generated.resources.confirm_phrase
import trekio.composeapp.generated.resources.delete_button
import trekio.composeapp.generated.resources.email_text
import trekio.composeapp.generated.resources.input_phrase
import trekio.composeapp.generated.resources.secure_delete_account
import trekio.composeapp.generated.resources.total_km_text
import trekio.composeapp.generated.resources.total_time_spent
import trekio.composeapp.generated.resources.total_trails_text
import trekio.composeapp.generated.resources.user_profile_title
import trekio.composeapp.generated.resources.username_text

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    onBack: () -> Unit,
    onDelete: () -> Unit,
    vm: UserProfileViewModel,
    // onUpdate: () -> Unit,
) {
    val scroll = rememberScrollState()

    var openDelete by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    // var showPasswordConfirmation by remember { mutableStateOf(false) }
    // var openUpdate by remember { mutableStateOf(false) }
    // var usernameUpdate by remember { mutableStateOf("") }
    // var passwordUpdate by remember { mutableStateOf("") }
    // var confirmPassword by remember { mutableStateOf("") }

    val state by vm.state.collectAsState()

    LaunchedEffect(state) {
        if (state is UserProfileState.Success) onDelete()
    }

    val isLoading = state is UserProfileState.Loading
    val error = (state as? UserProfileState.Error)?.message

    TopBarCreator(stringResource(Res.string.user_profile_title), onBack)

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(top = 100.dp).verticalScroll(scroll),
    ) {
        DataCard(Res.string.username_text, "")

        Spacer(Modifier.height(20.dp))

        DataCard(Res.string.email_text, "")

        Spacer(Modifier.height(20.dp))

        DataCard(Res.string.total_trails_text, "")

        Spacer(Modifier.height(20.dp))

        DataCard(Res.string.total_km_text, "")

        Spacer(Modifier.height(20.dp))

        DataCard(Res.string.total_time_spent, "")

        Spacer(Modifier.height(30.dp))
        /*
        GradientButton(
            onClick = {
                openUpdate = !openUpdate
                showPasswordConfirmation = false
            },
            modifier = Modifier.width(200.dp),
        ) {
            Text(
                text = stringResource(Res.string.update_button),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        AnimatedVisibility(openUpdate) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(
                    modifier = Modifier.height(20.dp),
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(250.dp).height(225.dp).border(2.dp, Color.Gray, RoundedCornerShape(15.dp)),
                ) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = usernameUpdate,
                        onValueChange = { usernameUpdate = it },
                        label = { Text(stringResource(Res.string.username_text)) },
                        modifier = Modifier.width(200.dp),
                    )
                    Spacer(Modifier.height(15.dp))
                    OutlinedTextField(
                        value = passwordUpdate,
                        onValueChange = { passwordUpdate = it },
                        label = { Text(stringResource(Res.string.password_text)) },
                        modifier = Modifier.width(200.dp),
                    )
                    Spacer(Modifier.height(15.dp))
                    GradientButton(
                        onClick = { showPasswordConfirmation = true },
                        modifier = Modifier.width(125.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.update_title),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(showPasswordConfirmation) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(20.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(250.dp).height(150.dp).border(2.dp, Color.Gray, RoundedCornerShape(15.dp)),
                ) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text(stringResource(Res.string.your_password_text)) },
                        modifier = Modifier.width(200.dp),
                    )
                    Spacer(Modifier.height(15.dp))
                    GradientButton(
                        onClick = onUpdate,
                        modifier = Modifier.width(125.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.confirm_text),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }


        Spacer(Modifier.height(30.dp))
         */
        GradientButton(
            onClick = { openDelete = !openDelete },
            modifier = Modifier.width(200.dp),
            gradientColors =
                listOf(
                    Color(0xFFAA0000),
                    Color(0xFFD76464),
                    Color(0xFFAA0000),
                ),
        ) {
            Text(
                text = stringResource(Res.string.delete_button),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        AnimatedVisibility(openDelete) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(20.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(250.dp).border(2.dp, Color(0xFFAA0000), RoundedCornerShape(15.dp)),
                ) {
                    Spacer(Modifier.height(10.dp))

                    Text(
                        stringResource(Res.string.secure_delete_account),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(200.dp),
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        singleLine = true,
                        label = { Text(stringResource(Res.string.input_phrase)) },
                        modifier = Modifier.width(200.dp),
                    )

                    Spacer(Modifier.height(20.dp))

                    GradientButton(
                        onClick = { vm.delete() },
                        modifier = Modifier.width(100.dp),
                        gradientColors =
                            listOf(
                                Color(0xFFAA0000),
                                Color(0xFFD76464),
                                Color(0xFFAA0000),
                            ),
                        enabled = inputText == stringResource(Res.string.confirm_phrase) && !isLoading,
                    ) {
                        Text(
                            text = stringResource(Res.string.confirm_delete_button),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    if (error != null) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(200.dp),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun UserProfileScreenPreview() = UserProfileScreen({}, {}, UserProfileViewModel(FailingService))
