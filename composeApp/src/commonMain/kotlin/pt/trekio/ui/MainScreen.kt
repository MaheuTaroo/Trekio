package pt.trekio.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jordond.compass.Location
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.painterResource
import pt.trekio.ui.utils.GradientButton
import pt.trekio.viewmodels.MapScreenViewModel
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.logout_icon
import trekio.composeapp.generated.resources.settings_icon
import trekio.composeapp.generated.resources.three_points_icon
import trekio.composeapp.generated.resources.user_icon

@Composable
fun MainScreen(
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onMapTest: () -> Unit,
    onTrailsClick: () -> Unit,
    mapViewModel: MapScreenViewModel,
) {
    var showProfileMenu by remember { mutableStateOf(false) }
    var showChatDialog by remember { mutableStateOf(false) }

    val isOverlayVisible = showProfileMenu || showChatDialog

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxSize().weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .border(1.dp, Color.Black, CircleShape)
                            .clickable { showProfileMenu = true },
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.three_points_icon),
                        contentDescription = "",
                    )
                }
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .weight(6f)
                        .background(Color(0xFFEDEDED), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                MapScreen(mapViewModel)
            }

            Row(
                modifier = Modifier.fillMaxSize().weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                GradientButton(
                    onClick = onTrailsClick,
                    modifier = Modifier.width(120.dp),
                ) {
                    Text("Trails")
                }
                GradientButton(
                    onClick = onMapTest,
                    modifier = Modifier.width(120.dp),
                ) {
                    Text("Map Test")
                }
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .background(Color.LightGray, CircleShape)
                            .clickable { showChatDialog = true },
                )
            }
        }
        if (isOverlayVisible) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable {
                            showProfileMenu = false
                            showChatDialog = false
                        },
            )
        }
        AnimatedVisibility(enter = fadeIn(), visible = showProfileMenu) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(top = 60.dp, end = 16.dp),
                contentAlignment = Alignment.TopEnd,
            ) {
                Column(
                    modifier =
                        Modifier
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.dp, Color.Black, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                            .width(140.dp)
                            .height(120.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.user_icon),
                            contentDescription = "",
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "Player Profile",
                            modifier =
                                Modifier
                                    .clickable {
                                        showProfileMenu = false
                                        onProfileClick()
                                    }.padding(8.dp),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.settings_icon),
                            contentDescription = "",
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "Settings",
                            modifier =
                                Modifier
                                    .clickable {
                                        showProfileMenu = false
                                        onSettingsClick()
                                    }.padding(8.dp),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.logout_icon),
                            contentDescription = "",
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "Sign Out",
                            modifier =
                                Modifier
                                    .clickable {
                                        showProfileMenu = false
                                        // * TODO Sign out
                                    }.padding(8.dp),
                        )
                    }
                }
            }
        }
        AnimatedVisibility(visible = showChatDialog) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp, end = 16.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Column(
                    modifier =
                        Modifier
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                            .width(260.dp),
                ) {
                    Text(
                        "Hey (username)! How can I help you today?",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier =
                            Modifier
                                .background(Color(0xFFF2F2F2), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                                .height(40.dp),
                    ) {
                        Text("Type any message here...")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun MainScreenPreview() = MainScreen(
    {},
    {},
    {},
    {},
    viewModel<MapScreenViewModel>(factory = MapScreenViewModel.getFactory(false))
)
