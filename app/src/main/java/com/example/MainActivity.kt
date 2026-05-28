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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NowPlayingMiniAndFullPlayer
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MusicPlayerViewModel
import com.example.util.neumorphic

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Pure, elegant solid layout background for pristine Neumorphic contrast

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
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
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .neumorphic(cornerRadius = 24.dp, elevation = 5.dp)
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
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Query music database",
                                    tint = if (currentScreenTab == 1) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = { Text("Search", style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_item_search")
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
                    1 -> SearchScreen(viewModel = viewModel)
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
}

