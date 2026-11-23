package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.MoodEntry
import com.example.ui.components.GlassCard
import com.example.ui.components.MoodEntryCard
import com.example.ui.components.MoodyDatePickerDialog
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderBright
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.moodColorInterpolated
import com.example.util.DateTimeUtils
import com.example.viewmodel.HistoryDateFilter
import com.example.viewmodel.MoodAnalytics
import com.example.viewmodel.MoodyUiState

@Composable
fun HistoryScreen(
  uiState: MoodyUiState,
  onSearchQueryChange: (String) -> Unit,
  onToggleFilterTag: (String) -> Unit,
  onSelectDateFilter: (HistoryDateFilter, Long?) -> Unit,
  onClearFilters: () -> Unit,
  onOpenEntry: (MoodEntry) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showDayFilterPicker by remember { mutableStateOf(false) }

  val groupedEntries = remember(uiState.filteredHistoryEntries) {
    uiState.filteredHistoryEntries
      .groupBy { DateTimeUtils.dayKey(it.timestamp) }
      .toSortedMap(compareByDescending { it })
  }

  val hasActiveFilters = uiState.searchQuery.isNotBlank() ||
    uiState.selectedFilterTags.isNotEmpty() ||
    uiState.dateFilter != HistoryDateFilter.ALL

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 640.dp),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
          Text(
            text = "HISTORY",
            style = MaterialTheme.typography.labelSmall,
            color = AccentPrimary,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
          ) {
            Text(
              text = "All Entries",
              style = MaterialTheme.typography.displaySmall,
              color = TextPrimary,
            )
            Text(
              text = "${uiState.filteredHistoryEntries.size} shown",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary,
            )
          }
        }
      }

      // Search Bar
      item {
        OutlinedTextField(
          value = uiState.searchQuery,
          onValueChange = onSearchQueryChange,
          placeholder = { Text("Search notes, tags, or moods...", color = TextMuted) },
          leadingIcon = {
            Icon(
              imageVector = Icons.Outlined.Search,
              contentDescription = "Search entries",
              tint = TextSecondary,
            )
          },
          trailingIcon = {
            if (uiState.searchQuery.isNotEmpty()) {
              IconButton(onClick = { onSearchQueryChange("") }) {
                Icon(
                  imageVector = Icons.Outlined.Close,
                  contentDescription = "Clear search",
                  tint = TextSecondary,
                )
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(18.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentPrimary.copy(alpha = 0.65f),
            unfocusedBorderColor = GlassBorderBright,
            focusedContainerColor = GlassSurface,
            unfocusedContainerColor = GlassSurface,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = AccentPrimary,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("history_search_input"),
        )
      }

      // Date filter chips
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          HistoryDateFilter.entries.forEach { filter ->
            val selected = uiState.dateFilter == filter
            val label = if (filter == HistoryDateFilter.CUSTOM_DAY && selected && uiState.customFilterDayMillis != null) {
              DateTimeUtils.formatShortDate(uiState.customFilterDayMillis)
            } else {
              filter.label
            }
            FilterChip(
              selected = selected,
              onClick = {
                if (filter == HistoryDateFilter.CUSTOM_DAY) {
                  showDayFilterPicker = true
                } else {
                  onSelectDateFilter(filter, null)
                }
              },
              label = { Text(label, style = MaterialTheme.typography.labelMedium) },
              leadingIcon = if (filter == HistoryDateFilter.CUSTOM_DAY) {
                {
                  Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                  )
                }
              } else {
                null
              },
              colors = FilterChipDefaults.filterChipColors(
                containerColor = GlassSurface,
                selectedContainerColor = AccentPrimary.copy(alpha = 0.20f),
                labelColor = TextSecondary,
                selectedLabelColor = AccentPrimary,
                iconColor = TextSecondary,
                selectedLeadingIconColor = AccentPrimary,
              ),
              border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor = GlassBorder,
                selectedBorderColor = AccentPrimary.copy(alpha = 0.5f),
              ),
              shape = RoundedCornerShape(12.dp),
            )
          }
        }
      }

      // Tag filter chips
      if (uiState.allUsedTags.isNotEmpty()) {
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            uiState.allUsedTags.forEach { tag ->
              val selected = tag in uiState.selectedFilterTags
              FilterChip(
                selected = selected,
                onClick = { onToggleFilterTag(tag) },
                label = { Text("#$tag", style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                  containerColor = GlassSurface,
                  selectedContainerColor = GlassSurfaceElevated,
                  labelColor = TextMuted,
                  selectedLabelColor = TextPrimary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = selected,
                  borderColor = GlassBorder,
                  selectedBorderColor = TextPrimary.copy(alpha = 0.45f),
                ),
                shape = RoundedCornerShape(12.dp),
              )
            }
            if (hasActiveFilters) {
              TextButton(onClick = onClearFilters) {
                Text("Reset filters", color = AccentPrimary)
              }
            }
          }
        }
      } else if (hasActiveFilters) {
        item {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onClearFilters) {
              Text("Reset filters", color = AccentPrimary)
            }
          }
        }
      }

      if (groupedEntries.isEmpty()) {
        item {
          GlassCard(cornerRadius = 24.dp) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 36.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .background(GlassSurfaceElevated, CircleShape),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Outlined.SearchOff,
                  contentDescription = null,
                  tint = TextSecondary,
                )
              }
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = if (uiState.allEntries.isEmpty()) "Your journal is empty" else "No matching entries",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = if (uiState.allEntries.isEmpty()) {
                  "Tap a mood face on Today to start building your timeline."
                } else {
                  "Try adjusting your search query, tags, or date filter."
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
              )
              if (hasActiveFilters) {
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onClearFilters) {
                  Text("Clear all filters", color = AccentPrimary)
                }
              }
            }
          }
        }
      } else {
        groupedEntries.forEach { (dayStart, dayEntries) ->
          item(key = "header_$dayStart") {
            val dayAvg = dayEntries.map { it.mood }.average().toFloat()
            val dayColor = moodColorInterpolated(dayAvg)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 4.dp, end = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = DateTimeUtils.formatRelativeDay(dayStart),
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
              )
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = dayColor.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, dayColor.copy(alpha = 0.35f)),
              ) {
                Text(
                  text = "Avg ${MoodAnalytics.formatScore(dayAvg)}",
                  style = MaterialTheme.typography.labelSmall,
                  color = dayColor,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
              }
            }
          }

          items(
            items = dayEntries,
            key = { it.id },
          ) { entry ->
            MoodEntryCard(
              entry = entry,
              showDateInTimestamp = false,
              onClick = { onOpenEntry(entry) },
            )
          }
        }
      }
    }
  }

  if (showDayFilterPicker) {
    MoodyDatePickerDialog(
      initialTimestampMillis = uiState.customFilterDayMillis ?: System.currentTimeMillis(),
      onConfirmDateUtcMillis = { utcMillis ->
        val localDay = DateTimeUtils.combineDateAndTime(utcMillis, 12, 0)
        onSelectDateFilter(HistoryDateFilter.CUSTOM_DAY, localDay)
        showDayFilterPicker = false
      },
      onDismiss = { showDayFilterPicker = false },
    )
  }
}
