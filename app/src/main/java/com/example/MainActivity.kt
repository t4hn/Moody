package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Storage
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.EntryDetailSheet
import com.example.ui.components.FloatingMoodToast
import com.example.ui.screens.DataScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.CharcoalBase
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.MoodyTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.viewmodel.MoodyViewModel

const val ROUTE_TODAY = "today"
const val ROUTE_INSIGHTS = "insights"
const val ROUTE_HISTORY = "history"
const val ROUTE_DATA = "data"

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
  MoodyTab(ROUTE_DATA, "Data", Icons.Filled.Storage, Icons.Outlined.Storage),
)

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
    )
    setContent {
      MoodyTheme {
        MoodyApp()
      }
    }
  }
}

@Composable
fun MoodyApp(
  viewModel: MoodyViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = backStackEntry?.destination

  fun navigateToTab(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianBg),
  ) {
    val useRail = maxWidth >= 840.dp

    Scaffold(
      containerColor = ObsidianBg,
      contentWindowInsets = WindowInsets.safeDrawing,
      bottomBar = {
        if (!useRail) {
          Box {
            NavigationBar(
              containerColor = CharcoalBase.copy(alpha = 0.96f),
              contentColor = TextPrimary,
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
                    selectedIconColor = AccentPrimary,
                    selectedTextColor = TextPrimary,
                    indicatorColor = GlassSurfaceElevated,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                  ),
                  modifier = Modifier.testTag("nav_${tab.route}"),
                )
              }
            }
            HorizontalDivider(
              color = GlassBorder,
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
            containerColor = CharcoalBase,
            contentColor = TextPrimary,
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
                  selectedIconColor = AccentPrimary,
                  selectedTextColor = TextPrimary,
                  indicatorColor = GlassSurfaceElevated,
                  unselectedIconColor = TextMuted,
                  unselectedTextColor = TextMuted,
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
            composable(ROUTE_INSIGHTS) {
              InsightsScreen(
                uiState = uiState,
                onSelectRange = viewModel::setInsightRange,
                onSelectHeatmapDay = viewModel::selectHeatmapDay,
                onSelectTimeOfDayMode = viewModel::setTimeOfDayMode,
              )
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
            composable(ROUTE_DATA) {
              DataScreen(
                uiState = uiState,
                onBuildJsonExport = viewModel::buildJsonExport,
                onBuildCsvExport = viewModel::buildCsvExport,
                onHandleJsonImportRaw = viewModel::handleJsonImportRaw,
                onConfirmImport = viewModel::confirmImport,
                onDismissImportDialog = viewModel::dismissImportDialog,
                onClearDemoData = viewModel::clearDemoData,
                onRestoreDemoData = viewModel::restoreDemoData,
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
      onSave = viewModel::saveEditedEntry,
      onDelete = viewModel::deleteEntry,
      onDismiss = viewModel::closeEntryEditor,
    )
  }
}
