package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.ads.AdManager
import com.example.data.language.LanguageManager
import com.example.ui.screens.AddEditWorkerScreen
import com.example.ui.screens.AdvanceManagerScreen
import com.example.ui.screens.DailyAttendanceScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MusterCardScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SalaryReportScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WorkersListScreen
import com.example.ui.theme.HajriCardTheme
import com.example.ui.viewmodel.HajriViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        LanguageManager.initialize(this)
        AdManager.initialize(this)
        setContent {
            val viewModel: HajriViewModel = viewModel()
            val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
            val baseContext = LocalContext.current
            val localizedContext = remember(currentLanguage.code) {
                LanguageManager.getLocalizedContext(baseContext, currentLanguage.code)
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration
            ) {
                HajriCardTheme {
                    HajriAppRoot(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun HajriAppRoot(viewModel: HajriViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    if (!userProfile.isLoggedIn) {
        LoginScreen(
            viewModel = viewModel,
            onLoginSuccess = {}
        )
    } else {
        HajriAppMain(viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HajriAppMain(viewModel: HajriViewModel) {
    var currentScreen by remember { mutableStateOf<String>("home") }
    var editWorkerId by remember { mutableStateOf<Long?>(null) }
    var screenHistory by remember { mutableStateOf(listOf("home")) }

    fun navigateTo(route: String, workerId: Long? = null) {
        if (workerId != null) {
            editWorkerId = workerId
        }
        if (currentScreen != route) {
            screenHistory = screenHistory + route
            currentScreen = route
        }
    }

    fun navigateBack() {
        if (screenHistory.size > 1) {
            val updated = screenHistory.dropLast(1)
            screenHistory = updated
            currentScreen = updated.last()
        } else if (currentScreen != "home") {
            currentScreen = "home"
            screenHistory = listOf("home")
        }
    }

    BackHandler(enabled = currentScreen != "home") {
        navigateBack()
    }

    val currentTitle = when (currentScreen) {
        "home" -> stringResource(R.string.title_home)
        "all_workers" -> stringResource(R.string.title_workers)
        "daily_attendance" -> stringResource(R.string.title_attendance)
        "muster_card" -> stringResource(R.string.title_muster)
        "advance" -> stringResource(R.string.title_advance)
        "salary" -> stringResource(R.string.title_salary)
        "reports" -> stringResource(R.string.title_reports)
        "settings" -> stringResource(R.string.title_settings)
        "add_worker" -> stringResource(R.string.title_add_worker)
        "edit_worker" -> stringResource(R.string.title_edit_worker)
        else -> stringResource(R.string.title_home)
    }

    val isTopLevel = currentScreen in listOf("home", "all_workers", "daily_attendance", "muster_card", "salary")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = currentTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    if (!isTopLevel || currentScreen != "home") {
                        IconButton(
                            onClick = { navigateBack() },
                            modifier = Modifier.testTag("top_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.common_back),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                actions = {
                    if (currentScreen != "settings") {
                        IconButton(
                            onClick = { navigateTo("settings") },
                            modifier = Modifier.testTag("top_settings_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.nav_settings),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val navTabs = listOf(
                    Triple("home", stringResource(R.string.nav_home), Icons.Default.Home),
                    Triple("all_workers", stringResource(R.string.nav_workers), Icons.Default.Group),
                    Triple("daily_attendance", stringResource(R.string.nav_attendance), Icons.Default.CalendarMonth),
                    Triple("muster_card", stringResource(R.string.nav_muster), Icons.Default.ReceiptLong),
                    Triple("salary", stringResource(R.string.nav_salary), Icons.Default.Payments)
                )

                navTabs.forEach { (route, label, icon) ->
                    val isSelected = currentScreen == route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentScreen != route) {
                                navigateTo(route)
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_$route")
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            "home" -> HomeScreen(
                viewModel = viewModel,
                onNavigateTo = { route -> navigateTo(route) },
                onSelectWorkerForMuster = { workerId ->
                    viewModel.setSelectedWorkerId(workerId)
                    navigateTo("muster_card")
                },
                modifier = Modifier.padding(innerPadding)
            )
            "all_workers" -> WorkersListScreen(
                viewModel = viewModel,
                onNavigateToAddWorker = { navigateTo("add_worker") },
                onNavigateToEditWorker = { workerId ->
                    navigateTo("edit_worker", workerId)
                },
                onOpenMusterCard = { workerId ->
                    viewModel.setSelectedWorkerId(workerId)
                    navigateTo("muster_card")
                },
                modifier = Modifier.padding(innerPadding)
            )
            "daily_attendance" -> DailyAttendanceScreen(
                viewModel = viewModel,
                onOpenMusterCard = { workerId ->
                    viewModel.setSelectedWorkerId(workerId)
                    navigateTo("muster_card")
                },
                modifier = Modifier.padding(innerPadding)
            )
            "muster_card" -> MusterCardScreen(
                viewModel = viewModel,
                onNavigateToWorkerEdit = { workerId ->
                    navigateTo("edit_worker", workerId)
                },
                modifier = Modifier.padding(innerPadding)
            )
            "advance" -> AdvanceManagerScreen(
                viewModel = viewModel,
                onOpenMusterCard = { workerId ->
                    viewModel.setSelectedWorkerId(workerId)
                    navigateTo("muster_card")
                },
                modifier = Modifier.padding(innerPadding)
            )
            "salary" -> SalaryReportScreen(
                viewModel = viewModel,
                onOpenMusterCard = { workerId ->
                    viewModel.setSelectedWorkerId(workerId)
                    navigateTo("muster_card")
                },
                modifier = Modifier.padding(innerPadding)
            )
            "reports" -> ReportsScreen(
                viewModel = viewModel,
                onOpenMusterCard = { workerId ->
                    viewModel.setSelectedWorkerId(workerId)
                    navigateTo("muster_card")
                },
                modifier = Modifier.padding(innerPadding)
            )
            "settings" -> SettingsScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            "add_worker" -> AddEditWorkerScreen(
                viewModel = viewModel,
                workerId = null,
                onNavigateBack = { navigateBack() },
                modifier = Modifier.padding(innerPadding)
            )
            "edit_worker" -> AddEditWorkerScreen(
                viewModel = viewModel,
                workerId = editWorkerId,
                onNavigateBack = { navigateBack() },
                modifier = Modifier.padding(innerPadding)
            )
            else -> HomeScreen(
                viewModel = viewModel,
                onNavigateTo = { route -> navigateTo(route) },
                onSelectWorkerForMuster = { workerId ->
                    viewModel.setSelectedWorkerId(workerId)
                    navigateTo("muster_card")
                },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
