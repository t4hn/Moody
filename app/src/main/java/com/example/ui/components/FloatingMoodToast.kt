package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.QuickLogToastState

@Composable
fun FloatingMoodToast(
  toastState: QuickLogToastState?,
  onAddDetails: () -> Unit,
  onUndo: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AnimatedVisibility(
    visible = toastState != null,
    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
    exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
    modifier = modifier,
  ) {
    if (toastState != null) {
      val moodColor = toastState.entry.moodLevel.color
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = GlassSurfaceElevated.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, moodColor.copy(alpha = 0.45f)),
        shadowElevation = 12.dp,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .testTag("quick_log_toast"),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
          ) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .background(moodColor, CircleShape),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = toastState.message,
              style = MaterialTheme.typography.bodyMedium,
              color = TextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
              onClick = onAddDetails,
              modifier = Modifier.testTag("toast_add_details"),
            ) {
              Text(
                text = "Add details",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = moodColor,
              )
            }
            TextButton(
              onClick = onUndo,
              modifier = Modifier.testTag("toast_undo"),
            ) {
              Text(
                text = "Undo",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
              )
            }
          }
        }
      }
    }
  }
}
