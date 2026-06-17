package com.drummer.speed

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import android.os.Build
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.res.Configuration
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.hilt.navigation.compose.hiltViewModel
import com.drummer.speed.ui.theme.DrumStrokeCounterTheme
import com.drummer.speed.data.model.SessionResult
import com.drummer.speed.viewmodel.DrumViewModel
import com.drummer.speed.ui.screens.SplashScreen
import com.drummer.speed.ui.screens.PracticePage
import com.drummer.speed.ui.screens.HistoryPage
import com.drummer.speed.ui.screens.ResultSummaryScreen
import com.drummer.speed.ui.components.*
import com.drummer.speed.ui.screens.StatisticsPage
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.util.*
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val sharedPrefs = remember { context.getSharedPreferences("settings", MODE_PRIVATE) }
            
            var showSplash by rememberSaveable { mutableStateOf(true) }
            val systemInDark = isSystemInDarkTheme()
            var isDarkMode by remember { 
                mutableStateOf(sharedPrefs.getBoolean("is_dark_mode", systemInDark)) 
            }
            var currentLanguage by remember { 
                mutableStateOf(sharedPrefs.getString("language", "id") ?: "id") 
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
                LocalActivityResultRegistryOwner provides (context as ActivityResultRegistryOwner)
            ) {
                DrumStrokeCounterTheme(darkTheme = isDarkMode) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        if (showSplash) {
                            SplashScreen { showSplash = false }
                        } else {
                            DrumCounterScreen(
                                isDarkMode = isDarkMode,
                                onThemeChange = { 
                                    isDarkMode = it
                                    sharedPrefs.edit { putBoolean("is_dark_mode", it) }
                                },
                                language = currentLanguage,
                                onLanguageChange = { 
                                    currentLanguage = it
                                    sharedPrefs.edit { putString("language", it) }
                                },
                                viewModel = viewModel
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
fun DrumCounterScreen(
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    viewModel: DrumViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    var showAboutDialog by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { 3 })
    val selectedTab by remember { derivedStateOf { pagerState.currentPage } }
    var showResultSummary by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }
        viewModel.checkAppUpdate(currentVersionCode)
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = true) {
        when {
            viewModel.isRunning -> viewModel.stopPractice()
            showResultSummary -> showResultSummary = false
            selectedTab == 1 -> showExitConfirmDialog = true
            else -> showExitConfirmDialog = true
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !viewModel.isRunning && !showResultSummary,
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
                        if (!viewModel.isRunning && !showResultSummary) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    },
                    title = { Text(stringResource(R.string.app_name)) }
                )
            },
            bottomBar = {
                if (!viewModel.isRunning && !showResultSummary) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        modifier = Modifier.graphicsLayer {
                            shadowElevation = 10f
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            clip = true
                        }
                    ) {
                        val navItems = listOf(
                            Triple(Icons.Default.Timer, Icons.Default.Timer, R.string.practice),
                            Triple(Icons.Default.BarChart, Icons.Default.BarChart, R.string.statistics_tab),
                            Triple(Icons.Default.History, Icons.Default.History, R.string.history_tab)
                        )

                        navItems.forEachIndexed { index, item ->
                            val isSelected = selectedTab == index
                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1.2f else 1.0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "iconScale"
                            )

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                icon = {
                                    Icon(
                                        imageVector = item.first,
                                        contentDescription = null,
                                        modifier = Modifier.scale(scale),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(item.third),
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            if (hasPermission) {
                DrumCounterLogic(
                    innerPadding = innerPadding,
                    selectedTab = selectedTab,
                    pagerState = pagerState,
                    viewModel = viewModel,
                    showResultSummary = showResultSummary,
                    onResultSummaryChange = { showResultSummary = it },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            } else {
                PermissionRequiredScreen { launcher.launch(Manifest.permission.RECORD_AUDIO) }
            }

            if (showAboutDialog) {
                AboutDialog(viewModel = viewModel) { showAboutDialog = false }
            }

            if (showExitConfirmDialog) {
                ExitConfirmDialog(context) { showExitConfirmDialog = false }
            }

            if (viewModel.showUpdateDialog) {
                UpdateDialog(viewModel = viewModel) { viewModel.showUpdateDialog = false }
            }

            if (viewModel.isCalibrating) {
                CalibrationDialog(
                    step = viewModel.calibrationStep,
                    progress = viewModel.calibrationProgress,
                    hits = viewModel.calibrationHits,
                    onDismiss = { viewModel.stopCalibration() },
                    onStart = {
                        if (hasPermission) {
                            viewModel.startSmartCalibration()
                        } else {
                            launcher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DrumCounterLogic(
    innerPadding: PaddingValues, 
    selectedTab: Int,
    pagerState: androidx.compose.foundation.pager.PagerState,
    viewModel: DrumViewModel,
    showResultSummary: Boolean,
    onResultSummaryChange: (Boolean) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    
    val selectedIds = remember { mutableStateListOf<String>() }
    val isSelectionMode by remember { derivedStateOf { selectedIds.isNotEmpty() } }
    
    var showSelectionDeleteConfirm by remember { mutableStateOf(false) }
    var lastSessionResult by remember { mutableStateOf<SessionResult?>(null) }
    var sortType by remember { mutableStateOf("latest") }
    var showSortMenu by remember { mutableStateOf(false) }
    var revealedItemId by remember { mutableStateOf<String?>(null) }
    
    val isMetronomeEnabled by remember { derivedStateOf { viewModel.isMetronomeEnabled } }
    var showMetronomeWarning by remember { mutableStateOf(false) }
    
    val history by viewModel.history.collectAsState()

    // Reset selection when switching tabs
    LaunchedEffect(selectedTab) {
        selectedIds.clear()
        revealedItemId = null
    }

    val sortedHistory = remember(history, sortType) {
        when (sortType) {
            "latest" -> history.sortedByDescending { it.timestamp }
            "oldest" -> history.sortedBy { it.timestamp }
            "most" -> history.sortedByDescending { it.strokes }
            else -> history
        }
    }

    LaunchedEffect(isMetronomeEnabled) {
        if (isMetronomeEnabled) {
            showMetronomeWarning = true
            delay(3000)
            showMetronomeWarning = false
        } else {
            showMetronomeWarning = false
        }
    }

    fun formatTime(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (minutes > 0) String.format(Locale.getDefault(), "%d:%02d", minutes, seconds) else "$seconds ${context.getString(R.string.sec)}"
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .pointerInput(Unit) {
            detectTapGestures(onTap = { 
                focusManager.clearFocus() 
                selectedIds.clear()
            })
        }
        .pointerInput(Unit) {
            detectHorizontalDragGestures { change, dragAmount ->
                if (pagerState.currentPage == 0 && dragAmount > 40 && !viewModel.isRunning && !showResultSummary) {
                    onOpenDrawer()
                    change.consume()
                }
            }
        }
    ) {
        val isPracticePage by remember { derivedStateOf { pagerState.currentPage == 0 } }
        val isHistoryPage by remember { derivedStateOf { pagerState.currentPage == 2 } }
        AnimatedVisibility(
            visible = viewModel.isCountingDown && isPracticePage,
            enter = fadeIn() + scaleIn(initialScale = 0.5f),
            exit = fadeOut() + scaleOut(targetScale = 1.5f),
            modifier = Modifier.fillMaxSize().zIndex(10f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.alpha = 0.7f }
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = viewModel.countdownText,
                    fontSize = 120.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (showResultSummary && lastSessionResult != null) {
            ResultSummaryScreen(
                result = lastSessionResult!!,
                onClose = { onResultSummaryChange(false) },
                formatTime = ::formatTime
            )
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !viewModel.isRunning && !viewModel.isCountingDown,
                beyondViewportPageCount = 1
            ) { page ->
                if (page == 0) {
                    PracticePage(
                        isRunning = viewModel.isRunning,
                        strokeCount = viewModel.strokeCount,
                        timeLeft = viewModel.timeLeft,
                        timerSeconds = viewModel.timerSeconds,
                        timerInput = viewModel.timerInput,
                        onTimerInputChange = { viewModel.updateTimer(it) },
                        onTimerIncrement = { viewModel.incrementTimer(it) },
                        isMetronomeEnabled = viewModel.isMetronomeEnabled,
                        onMetronomeToggle = { viewModel.isMetronomeEnabled = it },
                        bpmInput = viewModel.bpmInput,
                        onBpmInputChange = { viewModel.updateBpm(it) },
                        onBpmIncrement = { viewModel.incrementBpm(it) },
                        sensitivity = viewModel.sensitivity,
                        onSensitivityChange = { viewModel.updateSensitivity(it) },
                        sensitivityInput = viewModel.sensitivityInput,
                        onSensitivityInputChange = { viewModel.updateSensitivityInput(it) },
                        onStartCalibration = { viewModel.isCalibrating = true },
                        isCountingDown = viewModel.isCountingDown,
                        onStart = {
                            if (!viewModel.isRunning) {
                                viewModel.startPractice(goText = context.getString(R.string.go)) { result ->
                                    lastSessionResult = result
                                    onResultSummaryChange(true)
                                }
                            } else { viewModel.stopPractice() }
                        },
                        onStop = { viewModel.stopPractice() },
                        onReset = { viewModel.resetPractice() },
                        formatTime = ::formatTime,
                        focusManager = focusManager
                    )
                } else if (page == 1) {
                    StatisticsPage(history = history)
                } else {
                    HistoryPage(
                        history = history,
                        sortedHistory = sortedHistory,
                        selectedIds = selectedIds,
                        isSelectionMode = isSelectionMode,
                        revealedItemId = revealedItemId,
                        onRevealedItemChange = { revealedItemId = it },
                        showSortMenu = showSortMenu,
                        onSortMenuToggle = { showSortMenu = it },
                        onSortTypeChange = { sortType = it },
                        onSelectAll = { selectedIds.clear(); selectedIds.addAll(history.map { it.id }) },
                        onClearSelection = { selectedIds.clear() },
                        onToggleSelection = { id -> if (selectedIds.contains(id)) selectedIds.remove(id) else selectedIds.add(id) },
                        onDelete = { viewModel.deleteResult(it) },
                    )
                }
            }
        }

        // Selection FAB
        if (isSelectionMode && isHistoryPage) {
            DeleteSelectionFAB(selectedIds.size) { showSelectionDeleteConfirm = true }
        }

        // Dialogs
        if (showSelectionDeleteConfirm) {
            SelectionDeleteConfirmDialog {
                viewModel.deleteSelectedResults(selectedIds.toList())
                selectedIds.clear()
                showSelectionDeleteConfirm = false
            }
        }

        // Metronome Warning
        AnimatedVisibility(
            visible = showMetronomeWarning,
            enter = fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp).padding(horizontal = 24.dp).zIndex(20f)
        ) {
            MetronomeWarningCard(stringResource(R.string.metronome_warning))
        }
    }
}

// --- Helper Composable Components ---

@Composable
fun SettingsDrawerContent(
    language: String,
    onLanguageChange: (String) -> Unit,
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onAboutClick: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    ModalDrawerSheet(
        modifier = Modifier.fillMaxWidth(0.75f),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Section: Appearance
            SettingSectionTitle(text = stringResource(R.string.general_section))
            
            DrawerItem(Icons.Default.Language, stringResource(R.string.language)) {
                Surface(
                    onClick = { onLanguageChange(if (language == "id") "en" else "id") },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = if (language == "id") "ID" else "EN",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            DrawerItem(Icons.Default.DarkMode, stringResource(R.string.dark_mode)) {
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { onThemeChange(it) },
                    thumbContent = {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            // Section: Community & Support
            SettingSectionTitle(text = stringResource(R.string.support_section))
            
            DrawerClickableItem(
                icon = Icons.Default.Favorite,
                label = stringResource(R.string.donate),
                iconColor = Color.Red
            ) {
                uriHandler.openUri("https://mez.ink/fes.studio/fes.studio")
                onCloseDrawer()
            }

            DrawerClickableItem(
                icon = Icons.Default.Share,
                label = stringResource(R.string.share_app),
                iconColor = MaterialTheme.colorScheme.primary
            ) {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "Check out this Drum Stroke Counter app: https://github.com/fesstudio/Drum-Stroke-Counter")
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(shareIntent)
                onCloseDrawer()
            }

            DrawerClickableItem(
                icon = Icons.Default.Info,
                label = stringResource(R.string.about),
                iconColor = MaterialTheme.colorScheme.secondary
            ) {
                onAboutClick()
                onCloseDrawer()
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettingSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
    )
}

@Composable
fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        content()
    }
}

@Composable
fun DrawerClickableItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    iconColor: Color = LocalContentColor.current,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconColor
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}
