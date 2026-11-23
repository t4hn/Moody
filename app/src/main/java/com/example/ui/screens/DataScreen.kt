package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.MoodLevel
import com.example.ui.components.GlassCard
import com.example.ui.theme.AppearanceMode
import com.example.ui.theme.MoodColor4
import com.example.ui.theme.MoodyPalette
import com.example.ui.theme.MoodyTheme
import com.example.util.DateTimeUtils
import com.example.viewmodel.MoodyUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DataScreen(
  uiState: MoodyUiState,
  onSelectAppearanceMode: (AppearanceMode) -> Unit,
  onSelectColorPalette: (MoodyPalette) -> Unit,
  onBuildJsonExport: () -> String,
  onBuildCsvExport: () -> String,
  onHandleJsonImportRaw: (String) -> Unit,
  onHandleCsvImportRaw: (String) -> Unit,
  onConfirmImport: (replaceExisting: Boolean) -> Unit,
  onDismissImportDialog: () -> Unit,
  onDeleteAllEntries: () -> Unit,
  onShowStatus: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var showClearAllDialog by remember { mutableStateOf(false) }

  val exportJsonLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json"),
  ) { uri ->
    if (uri != null) {
      scope.launch {
        val result = withContext(Dispatchers.IO) {
          runCatching {
            val payload = onBuildJsonExport()
            context.contentResolver.openOutputStream(uri)?.use { stream ->
              stream.write(payload.toByteArray(Charsets.UTF_8))
            } ?: error("Unable to open output stream")
          }
        }
        result.onSuccess {
          onShowStatus("Exported ${uiState.allEntries.size} entries as JSON.")
        }.onFailure { err ->
          onShowStatus("JSON export failed: ${err.localizedMessage ?: "unknown error"}")
        }
      }
    }
  }

  val exportCsvLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("text/csv"),
  ) { uri ->
    if (uri != null) {
      scope.launch {
        val result = withContext(Dispatchers.IO) {
          runCatching {
            val payload = onBuildCsvExport()
            context.contentResolver.openOutputStream(uri)?.use { stream ->
              stream.write(payload.toByteArray(Charsets.UTF_8))
            } ?: error("Unable to open output stream")
          }
        }
        result.onSuccess {
          onShowStatus("Exported ${uiState.allEntries.size} entries as CSV.")
        }.onFailure { err ->
          onShowStatus("CSV export failed: ${err.localizedMessage ?: "unknown error"}")
        }
      }
    }
  }

  val importJsonLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      scope.launch {
        val readResult = withContext(Dispatchers.IO) {
          runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
              it.readText()
            } ?: error("Could not read selected file")
          }
        }
        readResult.onSuccess { rawText ->
          onHandleJsonImportRaw(rawText)
        }.onFailure { err ->
          onShowStatus("Failed to read file: ${err.localizedMessage ?: "unknown error"}")
        }
      }
    }
  }

  val importCsvLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      scope.launch {
        val readResult = withContext(Dispatchers.IO) {
          runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
              it.readText()
            } ?: error("Could not read selected file")
          }
        }
        readResult.onSuccess { rawText ->
          onHandleCsvImportRaw(rawText)
        }.onFailure { err ->
          onShowStatus("Failed to read CSV file: ${err.localizedMessage ?: "unknown error"}")
        }
      }
    }
  }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 640.dp),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
          Text(
            text = "SETTINGS",
            style = MaterialTheme.typography.labelSmall,
            color = colors.accentPrimary,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Appearance & Data",
            style = MaterialTheme.typography.displaySmall,
            color = colors.textPrimary,
          )
        }
      }

      // Status banner
      item {
        AnimatedVisibility(visible = uiState.statusMessage != null) {
          uiState.statusMessage?.let { message ->
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = colors.accentPrimary.copy(alpha = 0.14f),
              border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.4f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
              )
            }
          }
        }
      }

      // 1. Appearance & Colour Theme Settings Section
      item {
        GlassCard(glowColor = colors.accentPrimary, cornerRadius = 24.dp) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.Palette,
                contentDescription = null,
                tint = colors.accentPrimary,
                modifier = Modifier.size(20.dp),
              )
              Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
              )
            }

            // Appearance Mode selector (System Default / Light / Dark)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "Theme mode",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary,
              )
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(colors.chartTrack)
                  .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                AppearanceMode.entries.forEach { mode ->
                  val selected = uiState.appearanceMode == mode
                  val modeIcon = when (mode) {
                    AppearanceMode.SYSTEM -> Icons.Outlined.SettingsBrightness
                    AppearanceMode.LIGHT -> Icons.Outlined.LightMode
                    AppearanceMode.DARK -> Icons.Outlined.DarkMode
                  }
                  Row(
                    modifier = Modifier
                      .weight(1f)
                      .minimumInteractiveComponentSize()
                      .clip(RoundedCornerShape(10.dp))
                      .background(if (selected) colors.cardSurfaceElevated else Color.Transparent)
                      .clickable { onSelectAppearanceMode(mode) }
                      .padding(horizontal = 6.dp, vertical = 8.dp)
                      .testTag("appearance_mode_${mode.name.lowercase()}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                  ) {
                    Icon(
                      imageVector = modeIcon,
                      contentDescription = null,
                      tint = if (selected) colors.accentPrimary else colors.textSecondary,
                      modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                      text = if (mode == AppearanceMode.SYSTEM) "System" else mode.label,
                      style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                      ),
                      color = if (selected) colors.textPrimary else colors.textSecondary,
                      maxLines = 1,
                    )
                  }
                }
              }
            }

            HorizontalDivider(color = colors.border)

            // Colour Palette selector
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text(
                text = "Colour palette",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary,
              )
              MoodyPalette.entries.forEach { palette ->
                val selected = uiState.colorPalette == palette
                PaletteOptionRow(
                  palette = palette,
                  isDarkPreview = colors.isDark,
                  selected = selected,
                  onClick = { onSelectColorPalette(palette) },
                )
              }
            }
          }
        }
      }

      // 2. Journal Vault Summary Card
      item {
        val oldestTs = uiState.allEntries.minOfOrNull { it.timestamp }
        val newestTs = uiState.allEntries.maxOfOrNull { it.timestamp }
        val spanText = if (oldestTs != null && newestTs != null) {
          "${DateTimeUtils.formatShortDate(oldestTs)} – ${DateTimeUtils.formatShortDate(newestTs)}"
        } else {
          "No entries stored"
        }
        GlassCard(glowColor = colors.accentSecondary, cornerRadius = 24.dp) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
          ) {
            Text(
              text = "On-Device Storage",
              style = MaterialTheme.typography.titleLarge,
              color = colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Everything is auto-saved locally to your device.",
              style = MaterialTheme.typography.bodySmall,
              color = colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(32.dp),
            ) {
              VaultMetric(label = "Total entries", value = uiState.allEntries.size.toString())
              VaultMetric(label = "Unique tags", value = uiState.stats.distinctTagCount.toString())
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Span: $spanText",
              style = MaterialTheme.typography.labelSmall,
              color = colors.textMuted,
            )
          }
        }
      }

      // 3. Export Card
      item {
        GlassCard(cornerRadius = 24.dp) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "Export",
              style = MaterialTheme.typography.titleLarge,
              color = colors.textPrimary,
            )
            Text(
              text = "Save a complete JSON backup for re-importing later, or export CSV for spreadsheets.",
              style = MaterialTheme.typography.bodySmall,
              color = colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(2.dp))

            DataActionRow(
              icon = Icons.Outlined.Download,
              title = "Export JSON Backup",
              subtitle = "Full mood entries, tags, notes & contextual data",
              accent = colors.accentPrimary,
              enabled = uiState.allEntries.isNotEmpty(),
              onClick = {
                exportJsonLauncher.launch("moody-backup-${DateTimeUtils.formatFileDate()}.json")
              },
              testTag = "export_json_button",
            )

            DataActionRow(
              icon = Icons.Outlined.TableChart,
              title = "Export CSV",
              subtitle = "Entries with sleep, wellbeing & physical activity columns",
              accent = MoodColor4,
              enabled = uiState.allEntries.isNotEmpty(),
              onClick = {
                exportCsvLauncher.launch("moody-export-${DateTimeUtils.formatFileDate()}.csv")
              },
              testTag = "export_csv_button",
            )
          }
        }
      }

      // 4. Import Card
      item {
        GlassCard(cornerRadius = 24.dp) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "Import",
              style = MaterialTheme.typography.titleLarge,
              color = colors.textPrimary,
            )
            Text(
              text = "Load a MOODY JSON or CSV backup and choose to merge with or replace your current entries.",
              style = MaterialTheme.typography.bodySmall,
              color = colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(2.dp))

            DataActionRow(
              icon = Icons.Outlined.FileUpload,
              title = "Import from JSON",
              subtitle = "Validates JSON backup before merging or replacing",
              accent = colors.accentSecondary,
              enabled = true,
              onClick = {
                importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
              },
              testTag = "import_json_button",
            )

            DataActionRow(
              icon = Icons.Outlined.TableChart,
              title = "Import from CSV",
              subtitle = "Imports exported CSV logs with optional context fields",
              accent = colors.accentPrimary,
              enabled = true,
              onClick = {
                importCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
              },
              testTag = "import_csv_button",
            )
          }
        }
      }

      // 5. Data Management Card
      item {
        GlassCard(cornerRadius = 24.dp) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "Data Management",
              style = MaterialTheme.typography.titleLarge,
              color = colors.textPrimary,
            )

            DataActionRow(
              icon = Icons.Outlined.DeleteForever,
              title = "Delete all entries",
              subtitle = "Permanently wipe the local mood database",
              accent = colors.danger,
              enabled = uiState.allEntries.isNotEmpty(),
              onClick = { showClearAllDialog = true },
              testTag = "delete_all_button",
            )
          }
        }
      }

      // 6. Informational Mood Scale Card
      item {
        GlassCard(cornerRadius = 24.dp) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "Mood Scale",
              style = MaterialTheme.typography.titleLarge,
              color = colors.textPrimary,
            )
            Text(
              text = "Five-point scoring guide used across your timeline, charts, and heatmap.",
              style = MaterialTheme.typography.bodySmall,
              color = colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            MoodLevel.entries.forEach { level ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(level.color),
                )
                Text(
                  text = "${level.score}",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = colors.textPrimary,
                )
                Text(
                  text = level.scaleDescription,
                  style = MaterialTheme.typography.bodySmall,
                  color = colors.textSecondary,
                )
              }
            }
          }
        }
      }
    }
  }

  // Import validation / merge-or-replace dialog
  uiState.importDialogState?.let { dialogState ->
    if (dialogState.errorMessage != null) {
      AlertDialog(
        onDismissRequest = onDismissImportDialog,
        containerColor = colors.cardSurfaceElevated,
        title = { Text("Invalid Backup File", color = colors.danger) },
        text = { Text(dialogState.errorMessage, color = colors.textSecondary) },
        confirmButton = {
          TextButton(onClick = onDismissImportDialog) {
            Text("OK", color = colors.textPrimary)
          }
        },
      )
    } else if (dialogState.validEntries != null) {
      AlertDialog(
        onDismissRequest = onDismissImportDialog,
        containerColor = colors.cardSurfaceElevated,
        title = {
          Text("Import ${dialogState.validEntries.size} entries", color = colors.textPrimary)
        },
        text = {
          Text(
            text = "How would you like to import these entries into MOODY?",
            color = colors.textSecondary,
          )
        },
        confirmButton = {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = { onConfirmImport(true) },
              border = BorderStroke(1.dp, colors.danger.copy(alpha = 0.6f)),
              modifier = Modifier.testTag("import_replace_button"),
            ) {
              Text("Replace", color = colors.danger)
            }
            Button(
              onClick = { onConfirmImport(false) },
              colors = ButtonDefaults.buttonColors(
                containerColor = colors.accentPrimary,
                contentColor = colors.onAccentPrimary,
              ),
              modifier = Modifier.testTag("import_merge_button"),
            ) {
              Text("Merge")
            }
          }
        },
        dismissButton = {
          TextButton(onClick = onDismissImportDialog) {
            Text("Cancel", color = colors.textSecondary)
          }
        },
      )
    }
  }

  if (showClearAllDialog) {
    AlertDialog(
      onDismissRequest = { showClearAllDialog = false },
      containerColor = colors.cardSurfaceElevated,
      title = { Text("Delete all entries?", color = colors.textPrimary) },
      text = {
        Text(
          text = "This will permanently delete all ${uiState.allEntries.size} entries on this device. Consider exporting a JSON backup first.",
          color = colors.textSecondary,
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            showClearAllDialog = false
            onDeleteAllEntries()
          },
        ) {
          Text("Delete All", color = colors.danger)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearAllDialog = false }) {
          Text("Cancel", color = colors.textSecondary)
        }
      },
    )
  }
}

@Composable
private fun PaletteOptionRow(
  palette: MoodyPalette,
  isDarkPreview: Boolean,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val colors = MoodyTheme.colors
  val swatchBg = if (isDarkPreview) palette.previewDarkBg else palette.previewLightBg
  val swatchAccent = if (isDarkPreview) palette.previewAccentDark else palette.previewAccentLight

  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    color = if (selected) colors.accentPrimary.copy(alpha = 0.12f) else colors.cardSurfaceElevated.copy(alpha = 0.65f),
    border = BorderStroke(
      width = if (selected) 1.5.dp else 1.dp,
      color = if (selected) colors.accentPrimary.copy(alpha = 0.7f) else colors.border,
    ),
    modifier = Modifier
      .fillMaxWidth()
      .minimumInteractiveComponentSize()
      .testTag("palette_${palette.name.lowercase()}"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Two-tone swatch preview (background + accent dot)
      Box(
        modifier = Modifier
          .size(34.dp)
          .clip(CircleShape)
          .background(swatchBg)
          .border(1.5.dp, colors.borderBright, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(swatchAccent),
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = palette.title,
          style = MaterialTheme.typography.titleMedium,
          color = colors.textPrimary,
        )
        Text(
          text = palette.subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = colors.textSecondary,
        )
      }

      if (selected) {
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          imageVector = Icons.Outlined.Check,
          contentDescription = "Selected palette",
          tint = colors.accentPrimary,
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}

@Composable
private fun VaultMetric(
  label: String,
  value: String,
) {
  val colors = MoodyTheme.colors
  Column {
    Text(
      text = value,
      style = MaterialTheme.typography.headlineSmall,
      color = colors.textPrimary,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = colors.textMuted,
    )
  }
}

@Composable
private fun DataActionRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  accent: Color,
  enabled: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  val colors = MoodyTheme.colors
  Surface(
    onClick = onClick,
    enabled = enabled,
    shape = RoundedCornerShape(16.dp),
    color = colors.cardSurfaceElevated.copy(alpha = if (enabled) 0.75f else 0.35f),
    border = BorderStroke(1.dp, if (enabled) colors.borderBright else colors.border),
    modifier = Modifier
      .fillMaxWidth()
      .minimumInteractiveComponentSize()
      .testTag(testTag),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (enabled) accent else colors.textMuted,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = if (enabled) colors.textPrimary else colors.textMuted,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = if (enabled) colors.textSecondary else colors.textMuted,
        )
      }
    }
  }
}
