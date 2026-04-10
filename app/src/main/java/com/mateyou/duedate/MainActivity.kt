@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.mateyou.duedate

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mateyou.duedate.data.DateFilter
import com.mateyou.duedate.data.DueDate
import com.mateyou.duedate.data.getDynamicTitle
import com.mateyou.duedate.data.isOverdue
import com.mateyou.duedate.ui.theme.DueDateTheme
import com.mateyou.duedate.ui.theme.SuccessGreenDark
import com.mateyou.duedate.ui.theme.SuccessGreenLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private var navigateToState = mutableStateOf<String?>(null)
    private var monthOffsetState = mutableIntStateOf(0)
    private var billFilterState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        SmsDiagnosticLogger.init(this)
        
        // Handle navigation from Widget
        navigateToState.value = intent.getStringExtra("navigate_to")
        monthOffsetState.intValue = intent.getIntExtra("month_offset", 0)
        billFilterState.value = intent.getStringExtra("BILL_FILTER")

        setContent {
            val viewModel: DueDateViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val isMaterialYouEnabled by viewModel.isMaterialYouEnabled.collectAsState()
            val biometricLockEnabled by viewModel.biometricLockEnabled.collectAsState()
            
            var isUnlocked by rememberSaveable { mutableStateOf(!biometricLockEnabled) }

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            DueDateTheme(
                darkTheme = darkTheme,
                dynamicColor = isMaterialYouEnabled
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isUnlocked) {
                        AppContent(
                            viewModel = viewModel, 
                            initialNav = navigateToState.value,
                            initialMonthOffset = monthOffsetState.intValue,
                            initialBillFilter = billFilterState.value
                        ) { 
                            navigateToState.value = null 
                            monthOffsetState.intValue = 0
                            billFilterState.value = null
                        }
                    } else {
                        LockScreen(onUnlock = { isUnlocked = true })
                    }
                }
            }
            
            // Re-lock when biometric setting is enabled if not already unlocked
            LaunchedEffect(biometricLockEnabled) {
                if (biometricLockEnabled) {
                    isUnlocked = false
                } else {
                    isUnlocked = true
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navigateToState.value = intent?.getStringExtra("navigate_to")
        monthOffsetState.intValue = intent?.getIntExtra("month_offset", 0) ?: 0
        billFilterState.value = intent?.getStringExtra("BILL_FILTER")
    }
}

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    val context = LocalContext.current as FragmentActivity
    val executor = ContextCompat.getMainExecutor(context)
    val biometricPrompt = BiometricPrompt(context, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            onUnlock()
        }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            // Handle error (e.g. user cancelled)
        }
    })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Authenticate to Unlock")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        .build()

    LaunchedEffect(Unit) {
        biometricPrompt.authenticate(promptInfo)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                modifier = Modifier
                    .size(64.dp)
                    .clickable { biometricPrompt.authenticate(promptInfo) },
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("DueDate is Locked", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = { biometricPrompt.authenticate(promptInfo) }) {
                Text("Tap to Unlock")
            }
        }
    }
}

@Composable
fun AppContent(
    viewModel: DueDateViewModel, 
    initialNav: String? = null, 
    initialMonthOffset: Int = 0, 
    initialBillFilter: String? = null,
    onNavConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
    val isXiaomiPromptCompleted by viewModel.isXiaomiPromptCompleted.collectAsState()
    
    val sharedPrefs = remember { context.getSharedPreferences("due_date_prefs", Context.MODE_PRIVATE) }
    var hasShownRecoveryPrompt by rememberSaveable { 
        mutableStateOf(sharedPrefs.getBoolean("has_shown_recovery_prompt", false)) 
    }
    
    fun hasSmsPermissions() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                             ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

    fun hasNotificationPermission() = if (Build.VERSION.SDK_INT >= 33) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else true

    var smsGranted by remember { mutableStateOf(hasSmsPermissions()) }
    var notificationsGranted by remember { mutableStateOf(hasNotificationPermission()) }
    
    // Track manual retry attempts to limit to exactly one extra chance from the screen
    var smsRetryCount by rememberSaveable { mutableStateOf(0) }
    var notificationsRetryCount by rememberSaveable { mutableStateOf(0) }
    
    // Temporary flags to know if the current request is a manual retry
    var isManualSmsRequest by remember { mutableStateOf(false) }
    var isManualNotificationRequest by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        val smsNow = hasSmsPermissions()
        val notificationsNow = hasNotificationPermission()
        
        if (!smsNow && isManualSmsRequest) {
            smsRetryCount++
        }
        if (!notificationsNow && isManualNotificationRequest) {
            notificationsRetryCount++
        }

        smsGranted = smsNow
        notificationsGranted = notificationsNow
        isManualSmsRequest = false
        isManualNotificationRequest = false
    }

    // Monitor for permission changes when returning from settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val smsNow = hasSmsPermissions()
                val notificationsNow = hasNotificationPermission()
                smsGranted = smsNow
                notificationsGranted = notificationsNow
                
                if (smsNow) smsRetryCount = 0
                if (notificationsNow) notificationsRetryCount = 0
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showRecoveryDialog by remember { mutableStateOf(false) }
    var showParseExistingDialog by remember { mutableStateOf(false) }

    // Logic to show recovery prompt after permissions are granted
    LaunchedEffect(isOnboardingCompleted, isXiaomiPromptCompleted, smsGranted, notificationsGranted) {
        val needsXiaomiPrompt = isXiaomiDevice() && !isXiaomiPromptCompleted
        if (isOnboardingCompleted && !needsXiaomiPrompt && smsGranted && notificationsGranted && !hasShownRecoveryPrompt) {
            showRecoveryDialog = true
        }
    }

    when {
        !isOnboardingCompleted -> {
            OnboardingScreen(onComplete = { 
                viewModel.completeOnboarding()
                val permissions = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                if (Build.VERSION.SDK_INT >= 33) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                launcher.launch(permissions.toTypedArray())
            })
        }
        !smsGranted -> {
            val showRationale = activity?.shouldShowRequestPermissionRationale(Manifest.permission.RECEIVE_SMS) == true || 
                                activity?.shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS) == true
            
            // Logic: Only lock if the user has explicitly tapped 'Grant Permission' on this screen once and failed.
            val isSmsLocked = smsRetryCount >= 1 || (!showRationale && smsRetryCount > 0)

            PermissionRequirementScreen(
                title = "SMS Permission Required",
                description = "DueDate needs SMS access to automatically detect bank alerts and extract your bill details for you. This permission is required to use the app.",
                svgRes = R.raw.permission_sms,
                buttonText = if (isSmsLocked) "Open Settings" else "Grant Permission",
                onButtonClick = {
                    if (isSmsLocked) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    } else {
                        isManualSmsRequest = true
                        launcher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
                    }
                }
            )
        }
        !notificationsGranted -> {
            val showRationale = Build.VERSION.SDK_INT >= 33 && activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true
            val isNotificationsLocked = notificationsRetryCount >= 1 || (!showRationale && notificationsRetryCount > 0)

            PermissionRequirementScreen(
                title = "Notification Permission Required",
                description = "DueDate needs Notification access to send you smart and timely reminders to pay your bills. This permission is required to ensure you never miss a payment.",
                svgRes = R.raw.permission_notifications,
                buttonText = if (isNotificationsLocked) "Open Settings" else "Grant Permission",
                onButtonClick = {
                    if (Build.VERSION.SDK_INT >= 33) {
                        if (isNotificationsLocked) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } else {
                            isManualNotificationRequest = true
                            launcher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                        }
                    }
                }
            )
        }
        isXiaomiDevice() && !isXiaomiPromptCompleted -> {
            XiaomiInstructionScreen(onComplete = { viewModel.completeXiaomiPrompt() })
        }
        else -> {
            MainNavigation(viewModel, initialNav, initialMonthOffset, initialBillFilter, onNavConsumed)
            
            if (showRecoveryDialog) {
                AlertDialog(
                    onDismissRequest = { 
                        showRecoveryDialog = false
                        hasShownRecoveryPrompt = true
                        sharedPrefs.edit().putBoolean("has_shown_recovery_prompt", true).apply()
                    },
                    title = { Text("Parse Existing Messages") },
                    text = { Text("Almost there. Upcoming SMS from your banks will be automatically parsed. Do you want to parse your existing messages to identify recent bills?") },
                    confirmButton = {
                        TextButton(onClick = {
                            showRecoveryDialog = false
                            showParseExistingDialog = true
                        }) { Text("Confirm") }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            showRecoveryDialog = false
                            hasShownRecoveryPrompt = true
                            sharedPrefs.edit().putBoolean("has_shown_recovery_prompt", true).apply()
                        }) { Text("Skip") }
                    }
                )
            }

            if (showParseExistingDialog) {
                ParseExistingSmsDialog(
                    onDismiss = { 
                        showParseExistingDialog = false
                        hasShownRecoveryPrompt = true
                        sharedPrefs.edit().putBoolean("has_shown_recovery_prompt", true).apply()
                    },
                    onParse = { count, days ->
                        viewModel.parseExistingSms(count, days)
                        showParseExistingDialog = false
                        hasShownRecoveryPrompt = true
                        sharedPrefs.edit().putBoolean("has_shown_recovery_prompt", true).apply()
                        Toast.makeText(context, "Scanning historical SMS...", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

fun Context.shouldShowRequestPermissionRationale(permission: String): Boolean {
    val activity = this as? ComponentActivity ?: return false
    return activity.shouldShowRequestPermissionRationale(permission)
}

sealed class Screen(val route: String, val label: String, val filledIcon: ImageVector, val outlinedIcon: ImageVector) {
    object Bills : Screen("bills", "Bills", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong)
    object Calendar : Screen("calendar", "Calendar", Icons.Default.DateRange, Icons.Outlined.DateRange)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings, Icons.Outlined.Settings)
}

enum class BillStatusFilter { TOTAL, DUE, LATE, PAID }

@Composable
fun MainNavigation(
    viewModel: DueDateViewModel = viewModel(), 
    initialNav: String? = null, 
    initialMonthOffset: Int = 0,
    initialBillFilter: String? = null,
    onNavConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val screens = listOf(Screen.Bills, Screen.Calendar, Screen.Settings)
    val pagerState = rememberPagerState(pageCount = { screens.size })
    val scope = rememberCoroutineScope()
    
    var selectedBillForMenu by remember { mutableStateOf<DueDate?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var billToRename by remember { mutableStateOf<DueDate?>(null) }
    var billForPartialPayment by remember { mutableStateOf<DueDate?>(null) }
    var billForHistory by remember { mutableStateOf<DueDate?>(null) }
    
    // Stable version for history animation
    var stableBillForHistory by remember { mutableStateOf<DueDate?>(null) }
    LaunchedEffect(billForHistory) {
        if (billForHistory != null) {
            stableBillForHistory = billForHistory
        }
    }

    var billToConfirmAction by remember { mutableStateOf<Pair<DueDate, String>?>(null) }
    var billToAnimateId by remember { mutableStateOf<Int?>(null) }
    
    var showArchiveFolder by remember { mutableStateOf(false) }
    var showTrashFolder by remember { mutableStateOf(false) }
    var showFrequencyScreen by remember { mutableStateOf(false) }
    var showActivityLog by remember { mutableStateOf(false) }
    var showBanksScreen by remember { mutableStateOf(false) }
    var showParsingScreen by remember { mutableStateOf(false) }
    var showConfigureTemplates by remember { mutableStateOf(false) }
    var showParserDiagnostics by remember { mutableStateOf(false) }
    var showSchedulerDiagnostics by remember { mutableStateOf(false) }
    var showParseSmsDialog by remember { mutableStateOf(false) }
    
    var initialConfigSms by remember { mutableStateOf<String?>(null) }
    var statusFilter by remember { mutableStateOf(BillStatusFilter.TOTAL) }
    var initialHomeScreenExpand by remember { mutableStateOf(false) }

    LaunchedEffect(initialNav, initialBillFilter) {
        if (initialNav == "calendar") {
            pagerState.scrollToPage(1)
            onNavConsumed()
        } else if (initialNav == "bills" && initialBillFilter != null) {
            pagerState.scrollToPage(0)
            statusFilter = try { BillStatusFilter.valueOf(initialBillFilter) } catch (e: Exception) { BillStatusFilter.TOTAL }
            onNavConsumed()
        }
    }

    LaunchedEffect(activity?.intent) {
        activity?.intent?.let { intent ->
            val destination = intent.getStringExtra("NAV_DESTINATION")
            if (destination != null) {
                intent.removeExtra("NAV_DESTINATION")
                when (destination) {
                    "bills" -> {
                        pagerState.scrollToPage(0)
                        val filter = intent.getStringExtra("BILL_FILTER")
                        statusFilter = if (filter != null) {
                            try { BillStatusFilter.valueOf(filter) } catch (e: Exception) { BillStatusFilter.TOTAL }
                        } else BillStatusFilter.TOTAL
                    }
                    "paid_bills" -> {
                        pagerState.scrollToPage(0)
                        statusFilter = BillStatusFilter.TOTAL
                        initialHomeScreenExpand = true
                    }
                    "trash" -> {
                        pagerState.scrollToPage(2)
                        showTrashFolder = true
                    }
                }
            }
        }
    }

    BackHandler(enabled = pagerState.currentPage != 0 || showArchiveFolder || showTrashFolder || showFrequencyScreen || showActivityLog || showBanksScreen || showParsingScreen || showConfigureTemplates || billForHistory != null) {
        scope.launch {
            if (showArchiveFolder) showArchiveFolder = false
            else if (showTrashFolder) showTrashFolder = false
            else if (showFrequencyScreen) showFrequencyScreen = false
            else if (showActivityLog) showActivityLog = false
            else if (showBanksScreen) showBanksScreen = false
            else if (showParsingScreen) showParsingScreen = false
            else if (showConfigureTemplates) { showConfigureTemplates = false; initialConfigSms = null }
            else if (billForHistory != null) billForHistory = null
            else pagerState.animateScrollToPage(0)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                screens.forEachIndexed { index, screen ->
                    val isSelected = pagerState.currentPage == index
                    NavigationBarItem(
                        icon = { Icon(imageVector = if (isSelected) screen.filledIcon else screen.outlinedIcon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = isSelected,
                        onClick = { 
                            scope.launch {
                                // Close all current overlays when any tab is tapped
                                showArchiveFolder = false
                                showTrashFolder = false
                                showFrequencyScreen = false
                                showBanksScreen = false
                                showParsingScreen = false
                                showConfigureTemplates = false
                                initialConfigSms = null
                                showActivityLog = false
                                billForHistory = null
                                
                                if (pagerState.currentPage != index) {
                                    delay(100) // Small delay for visual smoothness
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (screens[page]) {
                    Screen.Bills -> HomeScreen(
                        viewModel = viewModel, 
                        onBillLongClick = { selectedBillForMenu = it }, 
                        onBillClick = { billForHistory = it }, 
                        billToAnimateId = billToAnimateId,
                        initialStatusFilter = statusFilter,
                        initialExpanded = initialHomeScreenExpand
                    )
                    Screen.Calendar -> CalendarScreen(
                        viewModel = viewModel, 
                        onBillClick = { billForHistory = it },
                        initialMonthOffset = initialMonthOffset
                    )
                    Screen.Settings -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            SettingsScreen(
                                viewModel = viewModel,
                                onOpenArchive = { showArchiveFolder = true },
                                onOpenTrash = { showTrashFolder = true },
                                onOpenFrequency = { showFrequencyScreen = true },
                                onOpenActivityLog = { showActivityLog = true },
                                onOpenBanks = { showBanksScreen = true },
                                onOpenParsing = { showParsingScreen = true },
                                onOpenParserDiagnostics = { showParserDiagnostics = true },
                                onOpenSchedulerDiagnostics = { showSchedulerDiagnostics = true },
                                onOpenParseExistingSms = { showParseSmsDialog = true },
                                onOpenConfigureTemplates = { showConfigureTemplates = true }
                            )
                            
                            AnimatedVisibility(
                                visible = showFrequencyScreen,
                                enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)),
                                exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))
                            ) {
                                SwipeOverlay(onDismiss = { showFrequencyScreen = false }) {
                                    ReminderFrequencyScreen(viewModel, onBack = { showFrequencyScreen = false })
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showActivityLog, enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)), exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))) {
                SwipeOverlay(onDismiss = { showActivityLog = false }) {
                    ActivityLogScreen(onBack = { showActivityLog = false })
                }
            }

            AnimatedVisibility(visible = showBanksScreen, enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)), exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))) {
                SwipeOverlay(onDismiss = { showBanksScreen = false }) {
                    ManageBanksScreen(viewModel = viewModel, onBack = { showBanksScreen = false })
                }
            }

            AnimatedVisibility(visible = showParsingScreen, enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)), exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))) {
                SwipeOverlay(onDismiss = { showParsingScreen = false }) {
                    BillParsingScreen(
                        viewModel = viewModel, 
                        onBack = { showParsingScreen = false },
                        onConfigureSms = { sms ->
                            initialConfigSms = sms
                            showConfigureTemplates = true
                            showParsingScreen = false
                        }
                    )
                }
            }

            AnimatedVisibility(visible = showConfigureTemplates, enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)), exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))) {
                SwipeOverlay(onDismiss = { showConfigureTemplates = false; initialConfigSms = null }) {
                    ConfigureSmsTemplatesScreen(viewModel, initialSms = initialConfigSms, onBack = { showConfigureTemplates = false; initialConfigSms = null })
                }
            }
            
            AnimatedVisibility(visible = showArchiveFolder, enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)), exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))) {
                val archivedBills by viewModel.archivedDueDates.collectAsState(initial = emptyList())
                SwipeOverlay(onDismiss = { showArchiveFolder = false }) {
                    ArchiveTrashOverlay("Archive", archivedBills, viewModel, onBack = { showArchiveFolder = false }, onBillClick = { billForHistory = it })
                }
            }
            
            AnimatedVisibility(visible = showTrashFolder, enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)), exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))) {
                val deletedBills by viewModel.deletedDueDates.collectAsState(initial = emptyList())
                SwipeOverlay(onDismiss = { showTrashFolder = false }) {
                    ArchiveTrashOverlay("Trash", deletedBills, viewModel, onBack = { showTrashFolder = false })
                }
            }

            // History screen drawn on top of everything
            AnimatedVisibility(
                visible = billForHistory != null,
                enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.95f, animationSpec = tween(300))
            ) {
                stableBillForHistory?.let { bill ->
                    HistoryScreen(bill = bill, viewModel = viewModel, onBack = { billForHistory = null })
                }
            }

            if (selectedBillForMenu != null) {
                ModalBottomSheet(onDismissRequest = { selectedBillForMenu = null }, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
                    BillActionMenu(bill = selectedBillForMenu!!, onAction = { action ->
                        val bill = selectedBillForMenu!!
                        selectedBillForMenu = null
                        when (action) {
                            "history" -> billForHistory = bill
                            "partial" -> billForPartialPayment = bill
                            "rename" -> billToRename = bill
                            "archive" -> billToConfirmAction = bill to "archive"
                            "delete" -> billToConfirmAction = bill to "delete"
                        }
                    })
                }
            }

            if (billForPartialPayment != null) {
                PartialPaymentDialog(
                    bill = billForPartialPayment!!,
                    onDismiss = { billForPartialPayment = null },
                    onConfirm = { amount ->
                        viewModel.setPartialPayment(billForPartialPayment!!.id, amount)
                        billForPartialPayment = null
                    }
                )
            }

            if (billToRename != null) RenameDialog(bill = billToRename!!, onDismiss = { billToRename = null }, onConfirm = { newName -> viewModel.renameCard(billToRename!!.bankName, billToRename!!.cardName, billToRename!!.cardNumber, newName); billToRename = null })
            
            billToConfirmAction?.let { (bill, action) ->
                val title = when(action) { "archive" -> "Archive Bill?"; "delete" -> "Move to Trash?"; "permanent_delete" -> "Delete Permanently?"; else -> "" }
                val body = when(action) { "archive" -> "This bill will be moved to the archive."; "delete" -> "This bill will be moved to the trash."; "permanent_delete" -> "This entry cannot be retrieved once deleted permanently."; else -> "" }
                val confirmText = when(action) { "archive" -> "Archive"; "delete" -> "Trash"; "permanent_delete" -> "Delete"; else -> "Confirm" }
                AlertDialog(
                    onDismissRequest = { billToConfirmAction = null },
                    title = { Text(title) },
                    text = { Text(body) },
                    confirmButton = {
                        TextButton(onClick = {
                            scope.launch {
                                val id = bill.id
                                billToAnimateId = id
                                billToConfirmAction = null
                                delay(350)
                                when(action) { 
                                    "archive" -> viewModel.archive(id)
                                    "delete" -> viewModel.delete(id)
                                    "permanent_delete" -> viewModel.deletePermanently(id)
                                }
                                billToAnimateId = null
                            }
                        }) { Text(confirmText) }
                    },
                    dismissButton = { TextButton(onClick = { billToConfirmAction = null }) { Text("Cancel") } }
                )
            }

            if (showParseSmsDialog) {
                ParseExistingSmsDialog(
                    onDismiss = { showParseSmsDialog = false },
                    onParse = { count, days ->
                        viewModel.parseExistingSms(count, days)
                        showParseSmsDialog = false
                        Toast.makeText(context, "Scanning historical SMS...", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}



@Composable
fun HomeScreen(
    viewModel: DueDateViewModel, 
    onBillLongClick: (DueDate) -> Unit, 
    onBillClick: (DueDate) -> Unit, 
    billToAnimateId: Int? = null,
    initialStatusFilter: BillStatusFilter = BillStatusFilter.TOTAL,
    initialExpanded: Boolean = false
) {
    val dateFilteredDates by viewModel.filteredDueDates.collectAsState(initial = emptyList())
    val activeBanks by viewModel.activeBanks.collectAsState(initial = emptyList())

    val bankLogoMap = remember(activeBanks) {
        activeBanks.associateBy({ it.name.lowercase() }, { it.svgLogo })
    }

    val selectedDateFilter by viewModel.selectedFilter.collectAsState()
    var statusFilter by remember { mutableStateOf(initialStatusFilter) }
    var expanded by rememberSaveable { mutableStateOf(initialExpanded) }
    
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val successGreen = if (isSystemInDarkTheme()) SuccessGreenDark else SuccessGreenLight


    // Sync internal state if initial values change (from intent)
    LaunchedEffect(initialStatusFilter) { statusFilter = initialStatusFilter }
    LaunchedEffect(initialExpanded) { 
        expanded = initialExpanded
        if (initialExpanded) {
            delay(800) // Give time for list to populate and UI to settle
            val unpaidCount = dateFilteredDates.count { !it.isPaid }
            val headerIndex = 2 + unpaidCount // Index of PaidHeader: 0(Header) + 1(Summary) + unpaidCount
            if (headerIndex < listState.layoutInfo.totalItemsCount) {
                listState.animateScrollToItem(headerIndex)
            }
        }
    }

    var showFilterDialog by remember { mutableStateOf(false) }

    val finalFilteredDates = when (statusFilter) {
        BillStatusFilter.TOTAL -> dateFilteredDates
        BillStatusFilter.DUE -> dateFilteredDates.filter { !it.isPaid && !isOverdue(it.dueDate) }
        BillStatusFilter.LATE -> dateFilteredDates.filter { !it.isPaid && isOverdue(it.dueDate) }
        BillStatusFilter.PAID -> dateFilteredDates.filter { it.isPaid }
    }

    val filterSubtext = remember(finalFilteredDates.size, statusFilter) {
        val count = finalFilteredDates.size
        if (count == 0) (if (statusFilter == BillStatusFilter.TOTAL) "No bills found" else "No ${statusFilter.name.lowercase()} bills found")
        else (if (statusFilter == BillStatusFilter.TOTAL) "Showing all $count bills" else "Showing $count ${statusFilter.name.lowercase()} bills")
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow)), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionHeader("DueDate"); Spacer(modifier = Modifier.height(24.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(text = selectedDateFilter.getDynamicTitle(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); IconButton(onClick = { showFilterDialog = true }) { Icon(Icons.Default.FilterList, "Filter") } }; Text(filterSubtext, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryBox("Total", dateFilteredDates.size.toString(), Modifier.weight(1f), isSelected = statusFilter == BillStatusFilter.TOTAL, onClick = { statusFilter = BillStatusFilter.TOTAL })
                SummaryBox("Due", dateFilteredDates.count { !it.isPaid && !isOverdue(it.dueDate) }.toString(), Modifier.weight(1f), color = MaterialTheme.colorScheme.primary, isSelected = statusFilter == BillStatusFilter.DUE, onClick = { statusFilter = BillStatusFilter.DUE })
                SummaryBox("Late", dateFilteredDates.count { !it.isPaid && isOverdue(it.dueDate) }.toString(), Modifier.weight(1f), color = MaterialTheme.colorScheme.error, isSelected = statusFilter == BillStatusFilter.LATE, onClick = { statusFilter = BillStatusFilter.LATE })
                SummaryBox("Paid", dateFilteredDates.count { it.isPaid }.toString(), Modifier.weight(1f), color = successGreen, isSelected = statusFilter == BillStatusFilter.PAID, onClick = { statusFilter = BillStatusFilter.PAID })
            }
        }

        val unpaidBills = finalFilteredDates.filter { !it.isPaid }
        val paidBills = finalFilteredDates.filter { it.isPaid }

        items(unpaidBills, key = { it.id }) { dueDate ->
            val svgLogo = bankLogoMap[dueDate.cardName?.lowercase() ?: ""] ?: bankLogoMap[dueDate.bankName.lowercase()]
            AnimatedBillItem(
                dueDate = dueDate,
                billToAnimateId = billToAnimateId
            ) { isExiting, triggerExit ->
                BillCard(
                    dueDate = dueDate,
                    onTogglePaid = { triggerExit { viewModel.setPaidStatus(dueDate.id, true) } },
                    onLongClick = { onBillLongClick(dueDate) },
                    onClick = { onBillClick(dueDate) },
                    svgLogo = svgLogo
                )
            }
        }

        if (statusFilter == BillStatusFilter.TOTAL && paidBills.isNotEmpty()) {
            item(key = "PaidHeader") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expanded = !expanded
                            if (expanded) {
                                scope.launch {
                                    delay(450)
                                    val layoutInfo = listState.layoutInfo
                                    val headerItem = layoutInfo.visibleItemsInfo.find { it.key == "PaidHeader" }
                                    if (headerItem != null) {
                                        listState.animateScrollToItem(headerItem.index)
                                    }
                                }
                            }
                        }
                        .padding(top = 24.dp, bottom = 16.dp)
                ) {
                    Text("Paid Bills", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                }
            }

            if (expanded) {
                items(paidBills, key = { "paid_${it.id}" }) { bill ->
                    val svgLogo = bankLogoMap[bill.cardName?.lowercase() ?: ""] ?: bankLogoMap[bill.bankName.lowercase()]
                    AnimatedBillItem(
                        dueDate = bill,
                        billToAnimateId = billToAnimateId
                    ) { isExiting, triggerExit ->
                        BillCard(
                            dueDate = bill,
                            onTogglePaid = { triggerExit { viewModel.setPaidStatus(bill.id, false) } },
                            onLongClick = { onBillLongClick(bill) },
                            onClick = { onBillClick(bill) },
                            svgLogo = svgLogo
                        )
                    }
                }
            }
        } else if (statusFilter == BillStatusFilter.PAID) {
            items(paidBills, key = { it.id }) { dueDate ->
                val svgLogo = bankLogoMap[dueDate.cardName?.lowercase() ?: ""] ?: bankLogoMap[dueDate.bankName.lowercase()]
                AnimatedBillItem(
                    dueDate = dueDate,
                    billToAnimateId = billToAnimateId
                ) { isExiting, triggerExit ->
                    BillCard(
                        dueDate = dueDate,
                        onTogglePaid = { triggerExit { viewModel.setPaidStatus(dueDate.id, false) } },
                        onLongClick = { onBillLongClick(dueDate) },
                        onClick = { onBillClick(dueDate) },
                        svgLogo = svgLogo
                    )
                }
            }
        }

        if (finalFilteredDates.isEmpty()) item { Box(modifier = Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)); Spacer(Modifier.height(16.dp)); Text(filterSubtext, color = MaterialTheme.colorScheme.outline) } } }
    }
    if (showFilterDialog) FilterDialog(selectedFilter = selectedDateFilter, onDismiss = { showFilterDialog = false }, onFilterSelected = { viewModel.updateFilter(it); showFilterDialog = false })
}

@Composable
fun FilterDialog(selectedFilter: DateFilter, onDismiss: () -> Unit, onFilterSelected: (DateFilter) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Filter Bills") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                FilterChip(
                    label = "All Time",
                    selected = selectedFilter == DateFilter.ALL_TIME,
                    onClick = { onFilterSelected(DateFilter.ALL_TIME) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(label = "Last Year", selected = selectedFilter == DateFilter.LAST_YEAR, onClick = { onFilterSelected(DateFilter.LAST_YEAR) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(0.dp))
                    FilterChip(label = "This Year", selected = selectedFilter == DateFilter.THIS_YEAR, onClick = { onFilterSelected(DateFilter.THIS_YEAR) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(0.dp))
                    FilterChip(label = "Next Year", selected = selectedFilter == DateFilter.NEXT_YEAR, onClick = { onFilterSelected(DateFilter.NEXT_YEAR) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(0.dp))
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(label = "Last Quarter", selected = selectedFilter == DateFilter.LAST_QUARTER, onClick = { onFilterSelected(DateFilter.LAST_QUARTER) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(0.dp))
                    FilterChip(label = "This Quarter", selected = selectedFilter == DateFilter.THIS_QUARTER, onClick = { onFilterSelected(DateFilter.THIS_QUARTER) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(0.dp))
                    FilterChip(label = "Next Quarter", selected = selectedFilter == DateFilter.NEXT_QUARTER, onClick = { onFilterSelected(DateFilter.NEXT_QUARTER) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(0.dp))
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(label = "Last Month", selected = selectedFilter == DateFilter.LAST_MONTH, onClick = { onFilterSelected(DateFilter.LAST_MONTH) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(bottomStart = 28.dp))
                    FilterChip(label = "This Month", selected = selectedFilter == DateFilter.THIS_MONTH, onClick = { onFilterSelected(DateFilter.THIS_MONTH) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(0.dp))
                    FilterChip(label = "Next Month", selected = selectedFilter == DateFilter.NEXT_MONTH, onClick = { onFilterSelected(DateFilter.NEXT_MONTH) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(bottomEnd = 28.dp))
                }
            }
        }
    )
}

@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp)) { Surface(onClick = onClick, modifier = modifier.height(44.dp), shape = shape, color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent, border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))) { Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) { Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 1) } } }


@Composable
fun AnimatedBillItem(
    dueDate: DueDate,
    billToAnimateId: Int?,
    modifier: Modifier = Modifier,
    content: @Composable (isExiting: Boolean, triggerExit: (action: () -> Unit) -> Unit) -> Unit
) {
    var isLocalExiting by remember { mutableStateOf(false) }
    val isExiting = isLocalExiting || billToAnimateId == dueDate.id
    val scope = rememberCoroutineScope()

    AnimatedVisibility(
        visible = !isExiting,
        exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut(),
        modifier = modifier
    ) {
        content(isExiting) { action ->
            scope.launch {
                isLocalExiting = true
                delay(300)
                action()
            }
        }
    }
}
