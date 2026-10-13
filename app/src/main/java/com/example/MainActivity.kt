package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.i18n.AppLanguage
import com.example.i18n.Translations
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.dialogs.PinDialog
import com.example.ui.screens.*
import com.example.ui.theme.AttendanceTrackerTheme
import com.example.ui.theme.TealPrimary

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val layoutDirection = if (uiState.language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr

            AttendanceTrackerTheme(darkTheme = uiState.isDarkMode) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    AttendanceApp(
                        viewModel = viewModel,
                        uiState = uiState
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceApp(
    viewModel: MainViewModel,
    uiState: com.example.ui.MainUiState
) {
    val lang = uiState.language
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingScreen by remember { mutableStateOf<AppScreen?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }

    // Handle feedback snackbars
    LaunchedEffect(uiState.feedbackMessage, uiState.errorMessage) {
        val msg = uiState.errorMessage ?: uiState.feedbackMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    // BackHandler to navigate back to TODAY
    BackHandler(enabled = uiState.currentScreen != AppScreen.TODAY) {
        viewModel.setScreen(AppScreen.TODAY)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TealPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = Translations.get("app_title", lang),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            Text(
                                text = Translations.get("app_subtitle", lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                actions = {
                    // Always-visible Language Toggle Button in Header
                    FilledTonalButton(
                        onClick = { viewModel.toggleLanguage() },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("language_toggle_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Translations.get("toggle_language", lang),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Dark / Light Mode Toggle
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (uiState.isDarkMode) Translations.get("light_mode", lang) else Translations.get("dark_mode", lang)
                        )
                    }

                    // PIN Lock status button (if enabled)
                    if (uiState.settings.pinProtectionEnabled) {
                        IconButton(
                            onClick = {
                                if (uiState.isManagerUnlocked) {
                                    viewModel.lockManager()
                                } else {
                                    showPinDialog = true
                                }
                            },
                            modifier = Modifier.testTag("pin_lock_status_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.isManagerUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (uiState.isManagerUnlocked) TealPrimary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                val screens = listOf(
                    Triple(AppScreen.TODAY, Icons.Default.Today, "today"),
                    Triple(AppScreen.KIOSK, Icons.Default.MeetingRoom, "kiosk"),
                    Triple(AppScreen.EMPLOYEES, Icons.Default.People, "employees"),
                    Triple(AppScreen.REPORTS, Icons.Default.BarChart, "reports"),
                    Triple(AppScreen.LEAVES, Icons.Default.BeachAccess, "leaves"),
                    Triple(AppScreen.SETTINGS, Icons.Default.Settings, "settings")
                )

                screens.forEach { (screen, icon, labelKey) ->
                    val isSelected = uiState.currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (uiState.settings.pinProtectionEnabled &&
                                !uiState.isManagerUnlocked &&
                                screen != AppScreen.KIOSK
                            ) {
                                pendingScreen = screen
                                showPinDialog = true
                            } else {
                                viewModel.setScreen(screen)
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = Translations.get(labelKey, lang)
                            )
                        },
                        label = {
                            Text(
                                text = Translations.get(labelKey, lang),
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = uiState.currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { target ->
                when (target) {
                    AppScreen.TODAY -> TodayAttendanceScreen(viewModel = viewModel, uiState = uiState)
                    AppScreen.KIOSK -> KioskScreen(viewModel = viewModel, uiState = uiState)
                    AppScreen.EMPLOYEES -> EmployeesScreen(viewModel = viewModel, uiState = uiState)
                    AppScreen.REPORTS -> ReportsScreen(viewModel = viewModel, uiState = uiState)
                    AppScreen.LEAVES -> LeavesScreen(viewModel = viewModel, uiState = uiState)
                    AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel, uiState = uiState)
                }
            }
        }
    }

    if (showPinDialog) {
        PinDialog(
            language = lang,
            onVerify = { pin -> viewModel.unlockManager(pin) },
            onSuccess = {
                showPinDialog = false
                pendingScreen?.let {
                    viewModel.setScreen(it)
                    pendingScreen = null
                }
            },
            onDismiss = {
                showPinDialog = false
                pendingScreen = null
            }
        )
    }
}
