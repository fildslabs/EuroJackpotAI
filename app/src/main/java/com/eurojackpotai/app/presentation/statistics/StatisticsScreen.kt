package com.eurojackpotai.app.presentation.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eurojackpotai.app.EuroJackpotApp
import com.eurojackpotai.app.R
import com.eurojackpotai.app.core.GenericViewModelFactory
import com.eurojackpotai.app.domain.model.LotteryStatistics
import com.eurojackpotai.app.presentation.components.BallKind
import com.eurojackpotai.app.presentation.components.HorizontalBarList
import com.eurojackpotai.app.presentation.components.NumberBall
import com.eurojackpotai.app.presentation.components.SectionCard

@Composable
fun StatisticsScreen() {
    val app = LocalContext.current.applicationContext as EuroJackpotApp
    val viewModel: StatisticsViewModel = viewModel(
        factory = GenericViewModelFactory {
            StatisticsViewModel(app.container.lotteryRepository, app.container.computeStatisticsUseCase)
        }
    )
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineMedium) }

        val stats = state.statistics
        if (stats == null) {
            item {
                SectionCard { Text(stringResource(R.string.stats_empty)) }
            }
            return@LazyColumn
        }

        item {
            Text(
                stringResource(R.string.stats_based_on, stats.drawsAnalyzed),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            SectionCard(title = stringResource(R.string.stats_hot_numbers)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BallRow(numbers = stats.hotMainNumbers.take(6).map { it.number }, kind = BallKind.HOT)
                    Text(
                        stringResource(R.string.hot_disclaimer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.stats_cold_numbers)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BallRow(numbers = stats.coldMainNumbers.take(6).map { it.number }, kind = BallKind.COLD)
                    Text(
                        stringResource(R.string.cold_disclaimer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.stats_frequency)) {
                HorizontalBarList(
                    items = stats.mainNumberFrequencies
                        .sortedByDescending { it.totalCount }
                        .take(15)
                        .map { it.number.toString().padStart(2, '0') to it.totalCount }
                )
            }
        }

        item {
            SectionCard(title = stringResource(R.string.stats_recent_frequency)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    RecentFrequencyColumn(title = stringResource(R.string.stats_recent_10), stats = stats) { it.last10Count }
                    RecentFrequencyColumn(title = stringResource(R.string.stats_recent_30), stats = stats) { it.last30Count }
                    RecentFrequencyColumn(title = stringResource(R.string.stats_recent_100), stats = stats) { it.last100Count }
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.stats_number_pairs)) {
                HorizontalBarList(
                    items = stats.topPairs.take(10).map { pair ->
                        "${pair.first.toString().padStart(2, '0')}+${pair.second.toString().padStart(2, '0')}" to pair.coOccurrenceCount
                    }
                )
            }
        }

        item {
            SectionCard(title = stringResource(R.string.stats_number_gaps)) {
                Column {
                    Text(
                        stringResource(R.string.stats_gap_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalBarList(
                        items = stats.mainNumberFrequencies
                            .sortedByDescending { it.drawsSinceLastSeen }
                            .take(10)
                            .map { it.number.toString().padStart(2, '0') to it.drawsSinceLastSeen }
                    )
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.stats_odd_even)) {
                HorizontalBarList(
                    items = stats.oddEvenSplits.map { "${it.oddCount}/${it.evenCount}" to it.occurrences }
                )
            }
        }

        item {
            SectionCard(title = stringResource(R.string.stats_distribution)) {
                HorizontalBarList(
                    items = stats.rangeDistribution.map { "${it.range.first}-${it.range.last}" to it.count }
                )
            }
        }

        item {
            SectionCard(title = stringResource(R.string.stats_sum_distribution)) {
                HorizontalBarList(
                    items = stats.sumDistribution.map { "${it.range.first}+" to it.count }
                )
            }
        }
    }
}

@Composable
private fun BallRow(numbers: List<Int>, kind: BallKind) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        numbers.forEach { NumberBall(number = it, kind = kind, size = 36.dp) }
    }
}

@Composable
private fun RecentFrequencyColumn(
    title: String,
    stats: LotteryStatistics,
    selector: (com.eurojackpotai.app.domain.model.NumberFrequency) -> Int
) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        HorizontalBarList(
            items = stats.mainNumberFrequencies
                .sortedByDescending(selector)
                .take(5)
                .map { it.number.toString().padStart(2, '0') to selector(it) }
        )
    }
}
