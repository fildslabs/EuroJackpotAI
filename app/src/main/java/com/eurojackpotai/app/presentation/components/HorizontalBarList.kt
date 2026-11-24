package com.eurojackpotai.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A minimal, dependency-free bar chart: each row is a label, a proportional bar drawn
 * with a plain Box (no Canvas needed at this scale), and the raw value. Used for every
 * chart on the Statistics screen so they all share one visual language.
 */
@Composable
fun HorizontalBarList(
    items: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
    barColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    val maxValue = (items.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (label, value) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.width(56.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = (value.toFloat() / maxValue).coerceIn(0.03f, 1f))
                            .height(16.dp)
                            .background(barColor, RoundedCornerShape(4.dp))
                    )
                }
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.width(32.dp)
                )
            }
        }
    }
}
