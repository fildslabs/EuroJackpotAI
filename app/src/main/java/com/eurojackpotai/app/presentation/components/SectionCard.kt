package com.eurojackpotai.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** The one card style used across all five screens — rounded corners, generous
 * padding, an optional title (with an optional trailing action next to it, e.g. a
 * refresh button) — so "card-based UI" (spec section 4) means one component, not five
 * slightly-different reimplementations. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    titleAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (title != null) {
                if (titleAction != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = title, style = MaterialTheme.typography.titleMedium)
                        titleAction()
                    }
                } else {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.padding(top = 6.dp))
            }
            content()
        }
    }
}
