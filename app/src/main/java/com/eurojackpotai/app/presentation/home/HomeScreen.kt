package com.eurojackpotai.app.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eurojackpotai.app.EuroJackpotApp
import com.eurojackpotai.app.R
import com.eurojackpotai.app.core.GenericViewModelFactory
import com.eurojackpotai.app.core.util.CurrencyFormat
import com.eurojackpotai.app.core.util.DateFormatters
import com.eurojackpotai.app.domain.model.SupportedCurrency
import com.eurojackpotai.app.presentation.components.CurrencyPicker
import com.eurojackpotai.app.presentation.components.LotteryCombinationRow
import com.eurojackpotai.app.presentation.components.SectionCard
import com.eurojackpotai.app.presentation.components.SingleLineFitText
import com.eurojackpotai.app.presentation.navigation.BottomNavItem

@Composable
fun HomeScreen(onNavigateToTab: (BottomNavItem) -> Unit) {
    val app = LocalContext.current.applicationContext as EuroJackpotApp
    val viewModel: HomeViewModel = viewModel(
        factory = GenericViewModelFactory {
            HomeViewModel(app.container.lotteryRepository, app.container.savedNumberRepository, app.container.currencyRepository)
        }
    )
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            SectionCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.home_jackpot_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SingleLineFitText(
                        text = state.nextJackpotAmount?.let { formatJackpot(it, state.nextJackpotCurrency) }
                            ?: stringResource(R.string.home_jackpot_unavailable),
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    if (state.selectedCurrency != SupportedCurrency.PLN) {
                        state.nextJackpotAmount?.let { amount ->
                            val converted = CurrencyFormat.convertAndFormat(
                                amountPln = amount.toDouble(),
                                currency = state.selectedCurrency,
                                ratesPerPln = state.currencyRatesPerPln
                            )
                            Text(
                                text = converted?.let { stringResource(R.string.currency_converted_format, it) }
                                    ?: stringResource(R.string.currency_unavailable),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.padding(top = 8.dp))
                    CurrencyPicker(
                        selected = state.selectedCurrency,
                        onSelect = viewModel::selectCurrency
                    )
                    Spacer(modifier = Modifier.padding(top = 12.dp))
                    Text(text = stringResource(R.string.home_next_draw), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(
                            R.string.home_countdown_format,
                            state.countdownDays,
                            state.countdownHours,
                            state.countdownMinutes
                        ),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
        }

        item {
            SectionCard(
                title = stringResource(R.string.home_recent_result),
                titleAction = {
                    IconButton(
                        onClick = { viewModel.refreshLatestResult() },
                        enabled = !state.isRefreshingResult
                    ) {
                        if (state.isRefreshingResult) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.home_refresh_result))
                        }
                    }
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.latestDraw?.let { draw ->
                        Text(text = DateFormatters.formatDisplay(draw.date), style = MaterialTheme.typography.bodyMedium)
                        LotteryCombinationRow(mainNumbers = draw.sortedMain, euroNumbers = draw.sortedEuro, ballSize = 32.dp)
                        if (state.isSampleData) {
                            Text(
                                text = stringResource(R.string.common_sample_data_notice),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        // Connection lost (or never established): the draw above is
                        // still whatever was last saved locally, never blanked out —
                        // this just makes that fact visible instead of silent.
                        if (state.isOffline) {
                            Text(
                                text = stringResource(R.string.common_offline_notice),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        state.lastSyncTime?.let { syncTime ->
                            Text(
                                text = stringResource(R.string.common_last_updated, DateFormatters.formatDateTime(syncTime)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { onNavigateToTab(BottomNavItem.RESULTS) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.home_view_result))
                    }
                }
            }
        }

        item {
            Button(
                onClick = { onNavigateToTab(BottomNavItem.GENERATE) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.home_generate_numbers), style = MaterialTheme.typography.titleMedium)
            }
        }

        item {
            SummaryRow(
                label = stringResource(R.string.home_section_my_numbers),
                value = if (state.savedNumbersCount == 0) {
                    stringResource(R.string.home_no_saved_numbers)
                } else {
                    state.savedNumbersCount.toString()
                },
                onClick = { onNavigateToTab(BottomNavItem.MY_NUMBERS) }
            )
        }
        item {
            SummaryRow(
                label = stringResource(R.string.home_section_statistics),
                value = stringResource(R.string.nav_statistics),
                onClick = { onNavigateToTab(BottomNavItem.STATISTICS) }
            )
        }
        item {
            SummaryRow(
                label = stringResource(R.string.home_section_recent_results),
                value = stringResource(R.string.nav_results),
                onClick = { onNavigateToTab(BottomNavItem.RESULTS) }
            )
        }
    }
}

/** Renders the *native* amount as returned by the remote source — LOTTO OpenAPI
 *  reports PLN (see LottoOpenApiRemoteDataSource), so this normally shows "1,234 PLN"
 *  now rather than the "€1,234" EUR formatting this function keeps for other sources.
 *  The user's selected display currency (see CurrencyPicker) is shown as a separate
 *  converted line underneath, never by guessing a symbol for whatever currency code
 *  the API happens to report. */
private fun formatJackpot(amount: Long, currency: String?): String =
    CurrencyFormat.formatNative(amount, currency)

@Composable
private fun SummaryRow(label: String, value: String, onClick: () -> Unit) {
    SectionCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            Text(text = value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
