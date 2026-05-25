package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NowPlayingMiniAndFullPlayer
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MusicPlayerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MusicPlayerViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val accentColor by viewModel.accentColor.collectAsState()

            MyApplicationTheme(
                themeMode = themeMode,
                accentColor = accentColor
            ) {
                MainLayout(viewModel = viewModel, accentColor = accentColor)
            }
        }
    }
}

@Composable
fun MainLayout(
    viewModel: MusicPlayerViewModel,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    var playerExpanded by rememberSaveable { mutableStateOf(false) }
    var currentScreenTab by rememberSaveable { mutableStateOf(0) } // 0 = Library, 1 = Equalizer, 2 = Settings

    val currentTrack by viewModel.currentTrack.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (!playerExpanded) {
                Column {
                    // Minimised persistent bottom player bar pops up only when a song is loaded
                    if (currentTrack != null) {
                        NowPlayingMiniAndFullPlayer(
                            viewModel = viewModel,
                            expanded = false,
                            onExpandChanged = { playerExpanded = it }
                        )
                    }

                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("app_navigation_bar")
                    ) {
                        NavigationBarItem(
                            selected = currentScreenTab == 0,
                            onClick = { currentScreenTab = 0 },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.LibraryMusic,
                                    contentDescription = "Library section",
                                    tint = if (currentScreenTab == 0) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = { Text("Library", style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_item_library")
                        )

                        NavigationBarItem(
                            selected = currentScreenTab == 1,
                            onClick = { currentScreenTab = 1 },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Equalizer,
                                    contentDescription = "Equalizer adjustments",
                                    tint = if (currentScreenTab == 1) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = { Text("Equalizer", style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_item_equalizer")
                        )

                        NavigationBarItem(
                            selected = currentScreenTab == 2,
                            onClick = { currentScreenTab = 2 },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Cosmetics settings",
                                    tint = if (currentScreenTab == 2) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = { Text("Settings", style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_item_settings")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Animated transitions between tabs
            AnimatedContent(
                targetState = currentScreenTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ScreenSwitchAnimation"
            ) { targetTab ->
                when (targetTab) {
                    0 -> LibraryScreen(viewModel = viewModel)
                    1 -> EqualizerScreen(viewModel = viewModel)
                    2 -> SettingsScreen(viewModel = viewModel)
                }
            }

            // Expanded Full immersive Player sheet container overlaid above the tabs
            if (playerExpanded && currentTrack != null) {
                NowPlayingMiniAndFullPlayer(
                    viewModel = viewModel,
                    expanded = true,
                    onExpandChanged = { playerExpanded = it }
                )
            }
        }
    }
}

