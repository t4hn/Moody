package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.GlassCard
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.DangerRose
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderBright
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.MoodColor2
import com.example.ui.theme.MoodColor4
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.DateTimeUtils
import com.example.viewmodel.MoodyUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DataScreen(
  uiState: MoodyUiState,
  onBuildJsonExport: () -> String,
  onBuildCsvExport: () -> String,
  onHandleJsonImportRaw: (String) -> Unit,
  onConfirmImport: (replaceExisting: Boolean) -> Unit,
  onDismissImportDialog: () -> Unit,
  onClearDemoData: () -> Unit,
  onRestoreDemoData: () -> Unit,
  onDeleteAllEntries: () -> Unit,
  onShowStatus: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
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
            text = "DATA & VAULT",
            style = MaterialTheme.typography.labelSmall,
            color = AccentPrimary,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Local Backup",
            style = MaterialTheme.typography.displaySmall,
            color = TextPrimary,
          )
        }
      }

      // Status banner
      item {
        AnimatedVisibility(visible = uiState.statusMessage != null) {
          uiState.statusMessage?.let { message ->
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = AccentPrimary.copy(alpha = 0.14f),
              border = BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.4f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
              )
            }
          }
        }
      }

      // Vault Summary Card
      item {
        val oldestTs = uiState.allEntries.minOfOrNull { it.timestamp }
        val newestTs = uiState.allEntries.maxOfOrNull { it.timestamp }
        val spanText = if (oldestTs != null && newestTs != null) {
          "${DateTimeUtils.formatShortDate(oldestTs)} – ${DateTimeUtils.formatShortDate(newestTs)}"
        } else {
          "No entries stored"
        }
        GlassCard(glowColor = MoodColor2, cornerRadius = 24.dp) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
          ) {
            Text(
              text = "On-Device Storage",
              style = MaterialTheme.typography.titleLarge,
              color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Everything is auto-saved locally to your device.",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              VaultMetric(label = "Total entries", value = uiState.allEntries.size.toString())
              VaultMetric(label = "Demo entries", value = uiState.demoEntryCount.toString())
              VaultMetric(label = "Unique tags", value = uiState.stats.distinctTagCount.toString())
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Span: $spanText",
              style = MaterialTheme.typography.labelSmall,
              color = TextMuted,
            )
          }
        }
      }

      // Export Card
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
              color = TextPrimary,
            )
            Text(
              text = "Save a complete JSON backup for re-importing later, or export CSV for spreadsheets.",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(2.dp))

            DataActionRow(
              icon = Icons.Outlined.Download,
              title = "Export JSON Backup",
              subtitle = "Full mood entries, timestamps, tags & notes",
              accent = AccentPrimary,
              enabled = uiState.allEntries.isNotEmpty(),
              onClick = {
                exportJsonLauncher.launch("moody-backup-${DateTimeUtils.formatFileDate()}.json")
              },
              testTag = "export_json_button",
            )

            DataActionRow(
              icon = Icons.Outlined.TableChart,
              title = "Export CSV",
              subtitle = "Columns: timestamp, mood, tags, note",
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

      // Import Card
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
              color = TextPrimary,
            )
            Text(
              text = "Load a MOODY JSON backup and choose to merge with or replace your current entries.",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(2.dp))

            DataActionRow(
              icon = Icons.Outlined.FileUpload,
              title = "Import from JSON",
              subtitle = "Validates format before merging or replacing",
              accent = MoodColor2,
              enabled = true,
              onClick = {
                importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
              },
              testTag = "import_json_button",
            )
          }
        }
      }

      // Demo & Reset Card
      item {
        GlassCard(cornerRadius = 24.dp) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "Demo & Cleanup",
              style = MaterialTheme.typography.titleLarge,
              color = TextPrimary,
            )

            if (uiState.demoEntryCount > 0) {
              DataActionRow(
                icon = Icons.Outlined.AutoDelete,
                title = "Clear demo data",
                subtitle = "Remove ${uiState.demoEntryCount} seeded sample entries (keeps your own logs)",
                accent = MoodColor4,
                enabled = true,
                onClick = onClearDemoData,
                testTag = "clear_demo_button",
              )
            } else {
              DataActionRow(
                icon = Icons.Outlined.Restore,
                title = "Restore demo data",
                subtitle = "Add sample entries across recent days to preview charts",
                accent = AccentPrimary,
                enabled = true,
                onClick = onRestoreDemoData,
                testTag = "restore_demo_button",
              )
            }

            DataActionRow(
              icon = Icons.Outlined.DeleteForever,
              title = "Delete all entries",
              subtitle = "Permanently wipe the local mood database",
              accent = DangerRose,
              enabled = uiState.allEntries.isNotEmpty(),
              onClick = { showClearAllDialog = true },
              testTag = "delete_all_button",
            )
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
        containerColor = GlassSurfaceElevated,
        title = { Text("Invalid Backup File", color = DangerRose) },
        text = { Text(dialogState.errorMessage, color = TextSecondary) },
        confirmButton = {
          TextButton(onClick = onDismissImportDialog) {
            Text("OK", color = TextPrimary)
          }
        },
      )
    } else if (dialogState.validEntries != null) {
      AlertDialog(
        onDismissRequest = onDismissImportDialog,
        containerColor = GlassSurfaceElevated,
        title = {
          Text("Import ${dialogState.validEntries.size} entries", color = TextPrimary)
        },
        text = {
          Text(
            text = "How would you like to import these entries into MOODY?",
            color = TextSecondary,
          )
        },
        confirmButton = {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = { onConfirmImport(true) },
              border = BorderStroke(1.dp, DangerRose.copy(alpha = 0.6f)),
              modifier = Modifier.testTag("import_replace_button"),
            ) {
              Text("Replace", color = DangerRose)
            }
            Button(
              onClick = { onConfirmImport(false) },
              colors = ButtonDefaults.buttonColors(
                containerColor = AccentPrimary,
                contentColor = ObsidianBg,
              ),
              modifier = Modifier.testTag("import_merge_button"),
            ) {
              Text("Merge")
            }
          }
        },
        dismissButton = {
          TextButton(onClick = onDismissImportDialog) {
            Text("Cancel", color = TextSecondary)
          }
        },
      )
    }
  }

  if (showClearAllDialog) {
    AlertDialog(
      onDismissRequest = { showClearAllDialog = false },
      containerColor = GlassSurfaceElevated,
      title = { Text("Delete all entries?", color = TextPrimary) },
      text = {
        Text(
          text = "This will permanently delete all ${uiState.allEntries.size} entries on this device. Consider exporting a JSON backup first.",
          color = TextSecondary,
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            showClearAllDialog = false
            onDeleteAllEntries()
          },
        ) {
          Text("Delete All", color = DangerRose)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearAllDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      },
    )
  }
}

@Composable
private fun VaultMetric(
  label: String,
  value: String,
) {
  Column {
    Text(
      text = value,
      style = MaterialTheme.typography.headlineSmall,
      color = TextPrimary,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = TextMuted,
    )
  }
}

@Composable
private fun DataActionRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  accent: androidx.compose.ui.graphics.Color,
  enabled: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  Surface(
    onClick = onClick,
    enabled = enabled,
    shape = RoundedCornerShape(16.dp),
    color = GlassSurfaceElevated.copy(alpha = if (enabled) 0.75f else 0.35f),
    border = BorderStroke(1.dp, if (enabled) GlassBorderBright else GlassBorder),
    modifier = Modifier
      .fillMaxWidth()
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
        tint = if (enabled) accent else TextMuted,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = if (enabled) TextPrimary else TextMuted,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = if (enabled) TextSecondary else TextMuted,
        )
      }
    }
  }
}
