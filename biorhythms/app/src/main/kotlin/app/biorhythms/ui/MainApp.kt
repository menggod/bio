package app.biorhythms.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.biorhythms.ui.screen.*
import app.biorhythms.ui.viewmodel.MainViewModel

enum class MainTab(val title: String, val icon: ImageVector) {
    CHART("节律", Icons.AutoMirrored.Filled.ShowChart),
    COMPASS("罗盘", Icons.Default.Explore),
    COMPAT("契合", Icons.Default.Favorite),
    NOTES("日记", Icons.Default.EditNote),
    PROFILES("档案", Icons.Default.People)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    viewModel: MainViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(MainTab.CHART) }
    var isPureChartPageOpen by remember { mutableStateOf(false) }
    var isInfoPageOpen by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (isPureChartPageOpen) {
        // 纯净单页面展示：只有曲线图
        BackHandler { isPureChartPageOpen = false }
        FullScreenChartScreen(
            viewModel = viewModel,
            uiState = uiState,
            onExit = { isPureChartPageOpen = false }
        )
    } else if (isInfoPageOpen) {
        // 科学科普指南页面
        BackHandler { isInfoPageOpen = false }
        InfoScreen(
            onBack = { isInfoPageOpen = false },
            modifier = modifier.fillMaxSize()
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    MainTab.values().forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) }
                        )
                    }
                }
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            when (selectedTab) {
                MainTab.CHART -> ChartScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    onNavigateToNotes = { selectedTab = MainTab.NOTES },
                    onOpenPureChart = { isPureChartPageOpen = true },
                    onOpenInfo = { isInfoPageOpen = true },
                    modifier = Modifier.padding(innerPadding)
                )
                MainTab.COMPASS -> LifeCompassScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    modifier = Modifier.padding(innerPadding)
                )
                MainTab.COMPAT -> CompatScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    modifier = Modifier.padding(innerPadding)
                )
                MainTab.NOTES -> NotesScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    modifier = Modifier.padding(innerPadding)
                )
                MainTab.PROFILES -> ProfilesScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    onOpenInfo = { isInfoPageOpen = true },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
