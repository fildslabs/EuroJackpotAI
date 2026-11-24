package com.eurojackpotai.app.presentation.generate

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eurojackpotai.app.EuroJackpotApp
import com.eurojackpotai.app.R
import com.eurojackpotai.app.core.GenericViewModelFactory
import com.eurojackpotai.app.domain.model.GeneratedCombination
import com.eurojackpotai.app.domain.model.GenerationMode
import com.eurojackpotai.app.domain.model.NumberExplanation
import com.eurojackpotai.app.domain.model.WeightIntensity
import com.eurojackpotai.app.presentation.components.LotteryCombinationRow
import com.eurojackpotai.app.presentation.components.SectionCard

@Composable
fun GenerateScreen() {
    val app = LocalContext.current.applicationContext as EuroJackpotApp
    val viewModel: GenerateViewModel = viewModel(
        factory = GenericViewModelFactory {
            GenerateViewModel(
                lotteryRepository = app.container.lotteryRepository,
                savedNumberRepository = app.container.savedNumberRepository,
                computeStatisticsUseCase = app.container.computeStatisticsUseCase,
                generateNumbersUseCase = app.container.generateNumbersUseCase
            )
        }
    )
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(stringResource(R.string.generate_title), style = MaterialTheme.typography.headlineMedium)
        }

        item {
            SectionCard(title = stringResource(R.string.generate_mode_label)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GenerationMode.entries.forEach { mode ->
                        ModeRow(
                            mode = mode,
                            isSelected = state.selectedMode == mode,
                            onClick = { viewModel.selectMode(mode) }
                        )
                    }
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.generate_games_label)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GenerateUiState.GAME_COUNT_OPTIONS) { count ->
                        FilterChip(
                            selected = state.selectedGameCount == count,
                            onClick = { viewModel.selectGameCount(count) },
                            label = { Text(count.toString()) }
                        )
                    }
                }
            }
        }

        if (isIntensityAdjustable(state.selectedMode)) {
            item {
                SectionCard(title = stringResource(R.string.generate_intensity_label)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WeightIntensity.entries.forEach { intensity ->
                            FilterChip(
                                selected = state.selectedIntensity == intensity,
                                onClick = { viewModel.selectIntensity(intensity) },
                                label = { Text(stringResource(intensityLabelRes(intensity))) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.generation_disclaimer),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                when (state.selectedMode) {
                    GenerationMode.HOT -> Text(
                        stringResource(R.string.hot_disclaimer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    GenerationMode.COLD -> Text(
                        stringResource(R.string.cold_disclaimer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    GenerationMode.AI_MIX -> Text(
                        stringResource(R.string.ai_mix_disclaimer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    GenerationMode.AVOID_POPULAR -> Text(
                        stringResource(R.string.avoid_popular_disclaimer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> Unit
                }
            }
        }

        item {
            Button(
                onClick = { viewModel.generate() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isGenerating
            ) {
                if (state.isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                }
                Text(
                    if (state.combinations.isEmpty()) {
                        stringResource(R.string.generate_button)
                    } else {
                        stringResource(R.string.generate_regenerate)
                    }
                )
            }
        }

        if (state.combinations.isNotEmpty()) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { viewModel.saveAllGenerated() }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.generate_save_all))
                    }
                    OutlinedButton(
                        onClick = { shareCombinations(context, state.combinations) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text(stringResource(R.string.generate_share))
                    }
                }
            }
            if (state.savedConfirmation) {
                item {
                    Text(
                        text = stringResource(R.string.common_save) + " ✓",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(2000)
                        viewModel.consumeSavedConfirmation()
                    }
                }
            }

            items(state.combinations) { combination ->
                CombinationCard(combination = combination, onNumberClick = { number ->
                    combination.explanations.find { it.number == number }?.let(viewModel::showExplanation)
                })
            }
        }
    }

    state.explanationForNumber?.let { explanation ->
        ExplanationDialog(explanation = explanation, onDismiss = { viewModel.dismissExplanation() })
    }
}

/** Hot / Cold / Hot+Cold are the only modes whose statistical bias strength the user
 * can tune — every other mode either ignores draw history or (Statistical Mix, AI Mix)
 * combines it with other signals in a way a single intensity knob wouldn't cleanly
 * describe. */
private fun isIntensityAdjustable(mode: GenerationMode): Boolean =
    mode == GenerationMode.HOT || mode == GenerationMode.COLD || mode == GenerationMode.HOT_COLD

private fun intensityLabelRes(intensity: WeightIntensity): Int = when (intensity) {
    WeightIntensity.WEAK -> R.string.intensity_weak
    WeightIntensity.NORMAL -> R.string.intensity_normal
    WeightIntensity.STRONG -> R.string.intensity_strong
}

@Composable
private fun ModeRow(mode: GenerationMode, isSelected: Boolean, onClick: () -> Unit) {
    val (titleRes, descRes) = modeStrings(mode)
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
                if (!mode.isAvailable) {
                    Text(
                        stringResource(R.string.generate_coming_soon),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.padding(top = 4.dp))
            Text(stringResource(descRes), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun modeStrings(mode: GenerationMode): Pair<Int, Int> = when (mode) {
    GenerationMode.RANDOM -> R.string.mode_random_title to R.string.mode_random_desc
    GenerationMode.BALANCED -> R.string.mode_balanced_title to R.string.mode_balanced_desc
    GenerationMode.HOT -> R.string.mode_hot_title to R.string.mode_hot_desc
    GenerationMode.COLD -> R.string.mode_cold_title to R.string.mode_cold_desc
    GenerationMode.HOT_COLD -> R.string.mode_hot_cold_title to R.string.mode_hot_cold_desc
    GenerationMode.STATISTICAL_MIX -> R.string.mode_statistical_mix_title to R.string.mode_statistical_mix_desc
    GenerationMode.AI_MIX -> R.string.mode_ai_mix_title to R.string.mode_ai_mix_desc
    GenerationMode.SMART_COVERAGE -> R.string.mode_smart_coverage_title to R.string.mode_smart_coverage_desc
    GenerationMode.AVOID_POPULAR -> R.string.mode_avoid_popular_title to R.string.mode_avoid_popular_desc
}

@Composable
private fun CombinationCard(combination: GeneratedCombination, onNumberClick: (Int) -> Unit) {
    SectionCard {
        LotteryCombinationRow(
            mainNumbers = combination.mainNumbers,
            euroNumbers = combination.euroNumbers,
            onNumberClick = if (combination.explanations.isNotEmpty()) onNumberClick else null
        )
    }
}

@Composable
private fun ExplanationDialog(explanation: NumberExplanation, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
        },
        title = { Text(stringResource(R.string.generate_explain_title) + " — ${explanation.number}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.explain_overall_frequency, explanation.overallFrequency))
                Text(stringResource(R.string.explain_recent_30, explanation.recent30Count))
                Text(stringResource(R.string.explain_last_drawn, explanation.drawsSinceLastSeen))
            }
        }
    )
}

private fun shareCombinations(context: android.content.Context, combinations: List<GeneratedCombination>) {
    val body = buildString {
        appendLine("EuroJackpot AI")
        appendLine("My ${combinations.size} Picks")
        combinations.forEach { combo ->
            appendLine("${combo.mainNumbers.joinToString(" ") { it.toString().padStart(2, '0') }} + " +
                combo.euroNumbers.joinToString(" ") { it.toString().padStart(2, '0') })
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
