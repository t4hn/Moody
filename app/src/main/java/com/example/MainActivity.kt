package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.EntryDetailSheet
import com.example.ui.components.FloatingMoodToast
import com.example.ui.screens.DataScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.InsightsCategory
import com.example.ui.screens.InsightsHabitsScreen
import com.example.ui.screens.InsightsMoodScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.InsightsSleepScreen
import com.example.ui.screens.InsightsWellbeingScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.AppearanceMode
import com.example.ui.theme.MoodyTheme
import com.example.viewmodel.MoodyUiState
import com.example.viewmodel.MoodyViewModel

const val ROUTE_TODAY = "today"
const val ROUTE_INSIGHTS = "insights"
const val ROUTE_INSIGHTS_OVERVIEW = "insights/overview"
const val ROUTE_INSIGHTS_MOOD = "insights/mood"
const val ROUTE_INSIGHTS_WELLBEING = "insights/wellbeing"
const val ROUTE_INSIGHTS_SLEEP = "insights/sleep"
const val ROUTE_INSIGHTS_HABITS = "insights/habits"
const val ROUTE_HISTORY = "history"
const val ROUTE_SETTINGS = "settings"

private data class MoodyTab(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
)

private val moodyTabs = listOf(
  MoodyTab(ROUTE_TODAY, "Today", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline),
  MoodyTab(ROUTE_INSIGHTS, "Insights", Icons.Filled.AutoGraph, Icons.Outlined.AutoGraph),
  MoodyTab(ROUTE_HISTORY, "History", Icons.Filled.History, Icons.Outlined.History),
  MoodyTab(ROUTE_SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
    )
    setContent {
      MoodyApp()
    }
  }
}

@Composable
fun MoodyApp(
  viewModel: MoodyViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val isDark = when (uiState.appearanceMode) {
    AppearanceMode.SYSTEM -> isSystemInDarkTheme()
    AppearanceMode.LIGHT -> false
    AppearanceMode.DARK -> true
  }

  val activity = LocalContext.current as? ComponentActivity
  DisposableEffect(activity, isDark) {
    activity?.enableEdgeToEdge(
      statusBarStyle = if (isDark) {
        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
      } else {
        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
      },
      navigationBarStyle = if (isDark) {
        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
      } else {
        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
      },
    )
    onDispose {}
  }

  MoodyTheme(
    appearanceMode = uiState.appearanceMode,
    palette = uiState.colorPalette,
  ) {
    MoodyAppScaffold(
      uiState = uiState,
      viewModel = viewModel,
    )
  }
}

@Composable
private fun MoodyAppScaffold(
  uiState: MoodyUiState,
  viewModel: MoodyViewModel,
) {
  val colors = MoodyTheme.colors
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = backStackEntry?.destination
  val insightsOverviewListState = rememberLazyListState()

  fun navigateToTab(route: String) {
    if (route == ROUTE_INSIGHTS &&
      currentDestination?.route?.startsWith("insights/") == true &&
      currentDestination.route != ROUTE_INSIGHTS_OVERVIEW
    ) {
      navController.popBackStack(ROUTE_INSIGHTS_OVERVIEW, inclusive = false)
      return
    }
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  fun navigateToInsightsCategory(category: InsightsCategory) {
    val targetRoute = when (category) {
      InsightsCategory.MOOD -> ROUTE_INSIGHTS_MOOD
      InsightsCategory.WELLBEING -> ROUTE_INSIGHTS_WELLBEING
      InsightsCategory.SLEEP -> ROUTE_INSIGHTS_SLEEP
      InsightsCategory.HABITS -> ROUTE_INSIGHTS_HABITS
    }
    navController.navigate(targetRoute) {
      launchSingleTop = true
    }
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.background),
  ) {
    val useRail = maxWidth >= 840.dp

    Scaffold(
      containerColor = colors.background,
      contentWindowInsets = WindowInsets.safeDrawing,
      bottomBar = {
        if (!useRail) {
          Box {
            NavigationBar(
              containerColor = colors.surfaceBase.copy(alpha = 0.96f),
              contentColor = colors.textPrimary,
              tonalElevation = 0.dp,
            ) {
              moodyTabs.forEach { tab ->
                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                NavigationBarItem(
                  selected = selected,
                  onClick = { navigateToTab(tab.route) },
                  icon = {
                    Icon(
                      imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                      contentDescription = tab.label,
                    )
                  },
                  label = {
                    Text(
                      text = tab.label,
                      style = MaterialTheme.typography.labelMedium,
                    )
                  },
                  colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.accentPrimary,
                    selectedTextColor = colors.textPrimary,
                    indicatorColor = colors.cardSurfaceElevated,
                    unselectedIconColor = colors.textMuted,
                    unselectedTextColor = colors.textMuted,
                  ),
                  modifier = Modifier.testTag("nav_${tab.route}"),
                )
              }
            }
            HorizontalDivider(
              color = colors.border,
              thickness = 1.dp,
              modifier = Modifier.align(Alignment.TopCenter),
            )
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        if (useRail) {
          NavigationRail(
            containerColor = colors.surfaceBase,
            contentColor = colors.textPrimary,
            modifier = Modifier.fillMaxHeight(),
          ) {
            moodyTabs.forEach { tab ->
              val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
              NavigationRailItem(
                selected = selected,
                onClick = { navigateToTab(tab.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.label,
                  )
                },
                label = {
                  Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelMedium,
                  )
                },
                colors = NavigationRailItemDefaults.colors(
                  selectedIconColor = colors.accentPrimary,
                  selectedTextColor = colors.textPrimary,
                  indicatorColor = colors.cardSurfaceElevated,
                  unselectedIconColor = colors.textMuted,
                  unselectedTextColor = colors.textMuted,
                ),
                modifier = Modifier.testTag("nav_${tab.route}"),
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
          NavHost(
            navController = navController,
            startDestination = ROUTE_TODAY,
            modifier = Modifier.fillMaxSize(),
          ) {
            composable(ROUTE_TODAY) {
              TodayScreen(
                uiState = uiState,
                onQuickLogMood = viewModel::quickLogMood,
                onSetCustomTimestamp = viewModel::setCustomQuickTimestamp,
                onOpenEntry = viewModel::openEntryEditor,
              )
            }
            navigation(
              startDestination = ROUTE_INSIGHTS_OVERVIEW,
              route = ROUTE_INSIGHTS,
            ) {
              composable(ROUTE_INSIGHTS_OVERVIEW) {
                InsightsScreen(
                  uiState = uiState,
                  onSelectRange = viewModel::setInsightRange,
                  onSelectCategory = ::navigateToInsightsCategory,
                  listState = insightsOverviewListState,
                )
              }
              composable(ROUTE_INSIGHTS_MOOD) {
                InsightsMoodScreen(
                  uiState = uiState,
                  onBack = { navController.popBackStack() },
                  onSelectRange = viewModel::setInsightRange,
                  onSelectHeatmapDay = viewModel::selectHeatmapDay,
                )
              }
              composable(ROUTE_INSIGHTS_WELLBEING) {
                InsightsWellbeingScreen(
                  uiState = uiState,
                  onBack = { navController.popBackStack() },
                  onSelectRange = viewModel::setInsightRange,
                )
              }
              composable(ROUTE_INSIGHTS_SLEEP) {
                InsightsSleepScreen(
                  uiState = uiState,
                  onBack = { navController.popBackStack() },
                  onSelectRange = viewModel::setInsightRange,
                )
              }
              composable(ROUTE_INSIGHTS_HABITS) {
                InsightsHabitsScreen(
                  uiState = uiState,
                  onBack = { navController.popBackStack() },
                  onSelectRange = viewModel::setInsightRange,
                  onSelectTimeOfDayMode = viewModel::setTimeOfDayMode,
                )
              }
            }
            composable(ROUTE_HISTORY) {
              HistoryScreen(
                uiState = uiState,
                onSearchQueryChange = viewModel::setSearchQuery,
                onToggleFilterTag = viewModel::toggleFilterTag,
                onSelectDateFilter = viewModel::setDateFilter,
                onClearFilters = viewModel::clearHistoryFilters,
                onOpenEntry = viewModel::openEntryEditor,
              )
            }
            composable(ROUTE_SETTINGS) {
              DataScreen(
                uiState = uiState,
                onSelectAppearanceMode = viewModel::setAppearanceMode,
                onSelectColorPalette = viewModel::setColorPalette,
                onBuildJsonExport = viewModel::buildJsonExport,
                onBuildCsvExport = viewModel::buildCsvExport,
                onHandleJsonImportRaw = viewModel::handleJsonImportRaw,
                onHandleCsvImportRaw = viewModel::handleCsvImportRaw,
                onConfirmImport = viewModel::confirmImport,
                onDismissImportDialog = viewModel::dismissImportDialog,
                onDeleteAllEntries = viewModel::deleteAllEntries,
                onShowStatus = viewModel::showStatus,
              )
            }
          }

          // Floating quick-log confirmation toast with "Add details" + "Undo"
          FloatingMoodToast(
            toastState = uiState.quickToast,
            onAddDetails = viewModel::openEntryDetailsFromToast,
            onUndo = viewModel::undoQuickToast,
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .widthIn(max = 560.dp)
              .padding(bottom = 14.dp),
          )
        }
      }
    }
  }

  // Entry detail & edit bottom sheet
  uiState.editingEntry?.let { entry ->
    EntryDetailSheet(
      entry = entry,
      suggestedTags = uiState.suggestedTags,
      dailyContexts = uiState.dailyContexts,
      onSave = viewModel::saveEditedEntry,
      onDelete = viewModel::deleteEntry,
      onDismiss = viewModel::closeEntryEditor,
    )
  }
}
