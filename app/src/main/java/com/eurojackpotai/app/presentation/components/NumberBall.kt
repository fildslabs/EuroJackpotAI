package com.eurojackpotai.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class BallKind { MAIN, EURO, HOT, COLD }

/**
 * The circular number chip used everywhere a EuroJackpot number is shown: draw results,
 * generated combinations, saved numbers and statistics lists. A single component keeps
 * that visual language consistent across all five screens.
 */
@Composable
fun NumberBall(
    number: Int,
    kind: BallKind = BallKind.MAIN,
    size: Dp = 40.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val (background, contentColor) = when (kind) {
        BallKind.MAIN -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        BallKind.EURO -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        BallKind.HOT -> Color(0xFFD9534F) to Color.White
        BallKind.COLD -> Color(0xFF4A90D9) to Color.White
    }

    var clickableModifier = modifier.size(size)
    if (onClick != null) clickableModifier = clickableModifier.clickable(onClick = onClick)

    Box(
        modifier = clickableModifier
            .background(background, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // The circle is a fixed dp size, but the label's font is sized in sp — on a
        // device/accessibility setting with a larger system font scale, titleMedium can
        // render wide enough that "07" wraps to "0"/"7" on two lines inside a still-tiny
        // 40dp circle, which reads as the number being cut off (reported directly: "화면
        // 사이즈에 따라 숫자가 잘려보여"). This is a small, fixed-size numeric badge, not
        // body copy, so its label intentionally ignores the system font-scale setting
        // (fontScale = 1f) and is forced to one line — every other label in the app still
        // scales normally with the user's accessibility font size.
        CompositionLocalProvider(
            LocalDensity provides Density(density = LocalDensity.current.density, fontScale = 1f)
        ) {
            Text(
                text = number.toString().padStart(2, '0'),
                color = contentColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }
    }
}
