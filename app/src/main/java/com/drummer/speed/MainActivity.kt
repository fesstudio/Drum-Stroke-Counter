package com.drummer.speed

import android.Manifest
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.drummer.speed.data.model.SessionResult
import com.drummer.speed.navigation.AppNavigation
import com.drummer.speed.navigation.Screen
import com.drummer.speed.ui.components.*
import com.drummer.speed.ui.components.settings.SettingsDrawerContent
import com.drummer.speed.ui.screens.ResultSummaryScreen
import com.drummer.speed.ui.screens.SplashScreen
import com.drummer.speed.ui.theme.DrumStrokeCounterTheme
import com.drummer.speed.util.AudioConfig
import com.drummer.speed.util.TimeFormatter
import com.drummer.speed.viewmodel.DrumViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val sharedPrefs = remember { context.getSharedPreferences(AudioConfig.SHARED_PREFS_NAME, MODE_PRIVATE) }

            var showSplash by rememberSaveable { mutableStateOf(true) }
            val systemInDark = isSystemInDarkTheme()
            var isDarkMode by remember {
                mutableStateOf(sharedPrefs.getBoolean(AudioConfig.KEY_DARK_MODE, systemInDark))
            }
            var currentLanguage by remember {
                mutableStateOf(sharedPrefs.getString(AudioConfig.KEY_LANGUAGE, AudioConfig.DEFAULT_LANGUAGE) ?: AudioConfig.DEFAULT_LANGUAGE)
            }

            val viewModel: DrumViewModel = hiltViewModel()

            val localizedContext = remember(currentLanguage) {
                val locale = Locale(currentLanguage)
                Locale.setDefault(locale)
                val config = Configuration(context.resources.configuration)
                config.setLocale(locale)
                config.setLayoutDirection(locale)
                context.createConfigurationContext(config)
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalActivityResultRegistryOwner provides (this as ActivityResultRegistryOwner)
            ) {
                DrumStrokeCounterTheme(darkTheme = isDarkMode) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        if (showSplash) {
                            SplashScreen(
                                isDarkMode = isDarkMode,
                                onSplashFinished = { showSplash = false }
                            )
                        } else {
                            DrumCounterApp(
                                viewModel = viewModel,
                                isDarkMode = isDarkMode,
                                onThemeChange = {
                                    isDarkMode = it
                                    sharedPrefs.edit { putBoolean(AudioConfig.KEY_DARK_MODE, it) }
                                },
                                language = currentLanguage,
                                onLanguageChange = {
                                    currentLanguage = it
                                    sharedPrefs.edit { putString(AudioConfig.KEY_LANGUAGE, it) }
                                },
                                onExitApp = { this@MainActivity.finishAffinity() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrumCounterApp(
    viewModel: DrumViewModel,
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    onExitApp: () -> Unit = {}
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val uiState by viewModel.uiState.collectAsState()

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showAboutDialog by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var showResultSummary by remember { mutableStateOf(false) }
    var lastSessionResult by remember { mutableStateOf<SessionResult?>(null) }
    // Observe session result from ViewModel (when practice stops)
    LaunchedEffect(Unit) {
        snapshotFlow { viewModel.lastSessionResult }
            .collect { result ->
                if (result != null) {
                    lastSessionResult = result
                    showResultSummary = true
                }
            }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.RECORD_AUDIO)
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode.toInt() else packageInfo.versionCode
        viewModel.checkAppUpdate(currentVersionCode)
    }

    BackHandler {
        when {
            uiState.isRunning -> viewModel.stopPractice()
            showResultSummary -> showResultSummary = false
            drawerState.isOpen -> scope.launch { drawerState.close() }
            else -> showExitConfirmDialog = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !uiState.isRunning && !showResultSummary,
        drawerContent = {
            SettingsDrawerContent(
                language = language,
                onLanguageChange = onLanguageChange,
                isDarkMode = isDarkMode,
                onThemeChange = onThemeChange,
                onAboutClick = { showAboutDialog = true },
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        if (!uiState.isRunning && !showResultSummary) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    },
                    title = { Text(stringResource(R.string.app_name)) }
                )
            },
            bottomBar = {
                if (!uiState.isRunning && !showResultSummary) {
                    BottomNavigationBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (hasPermission) {
                    AppNavigation(
                        navController = navController,
                        viewModel = viewModel,
                        showResultSummary = showResultSummary,
                        onResultSummaryChange = { showResultSummary = it },
                        onLastResultChange = { lastSessionResult = it }
                    )
                } else {
                    PermissionRequiredScreen(
                        onRequestPermission = { launcher.launch(Manifest.permission.RECORD_AUDIO) }
                    )
                }

                if (showResultSummary && lastSessionResult != null) {
                    ResultSummaryScreen(
                        result = lastSessionResult!!,
                        onClose = { showResultSummary = false },
                        formatTime = { totalSeconds -> TimeFormatter.format(totalSeconds, context) }
                    )
                }

                if (uiState.isCountingDown && currentRoute == Screen.Practice.route) {
                    CountdownOverlay(uiState.countdownText)
                }

                if (showAboutDialog) {
                    key(language) {
                        AboutDialog(viewModel = viewModel, onDismiss = { showAboutDialog = false }, currentLanguage = language)
                    }
                }
                if (showExitConfirmDialog) {
                    ExitConfirmDialog(
                        onConfirm = onExitApp,
                        onDismiss = { showExitConfirmDialog = false }
                    )
                }
                if (viewModel.showUpdateDialog) AppUpdateDialog(viewModel) { viewModel.dismissUpdateDialog() }
                if (uiState.isCalibrating) {
                    CalibrationDialog(
                        step = uiState.calibrationStep,
                        progress = uiState.calibrationProgress,
                        hits = uiState.calibrationHits,
                        onDismiss = { viewModel.stopCalibration() },
                        onStart = { viewModel.startSmartCalibration() }
                    )
                }
            }
        }
    }
}
