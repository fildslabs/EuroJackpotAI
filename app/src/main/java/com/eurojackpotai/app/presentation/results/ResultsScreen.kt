package com.eurojackpotai.app.presentation.results

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eurojackpotai.app.EuroJackpotApp
import com.eurojackpotai.app.R
import com.eurojackpotai.app.core.GenericViewModelFactory
import com.eurojackpotai.app.core.util.CurrencyFormat
import com.eurojackpotai.app.core.util.DateFormatters
import com.eurojackpotai.app.domain.model.CountryPrize
import com.eurojackpotai.app.domain.model.DrawResult
import com.eurojackpotai.app.domain.model.PrizeBreakdownTier
import com.eurojackpotai.app.domain.model.PrizeTier
import com.eurojackpotai.app.domain.model.SupportedCurrency
import com.eurojackpotai.app.domain.model.TierPrizeAmount
import com.eurojackpotai.app.presentation.components.CurrencyPicker
import com.eurojackpotai.app.presentation.components.LotteryCombinationRow
import com.eurojackpotai.app.presentation.components.SectionCard

/** The app now gets its own per-country winner breakdown directly from LOTTO OpenAPI
 *  (see the "Winners by Country" section below, backed by
 *  LottoOpenApiRemoteDataSource.fetchPrizeBreakdown) — this external link is kept as a
 *  supplementary "full official results" fallback for anything that breakdown doesn't
 *  cover (lower prize tiers, city-level winner locations, older draws), since LOTTO
 *  OpenAPI itself has no city/place data for EuroJackpot at all (verified against its
 *  full spec — see PrizeBreakdown's doc comment).
 *
 *  NOTE: eurojackpot.org's own "/en/results" path 404s (that site has no English
 *  results section as of this writing), so euro-jackpot.net is used instead — it has a
 *  working English results page with a country/winner breakdown of its own. */
private const val OFFICIAL_RESULTS_URL = "https://www.euro-jackpot.net/en/results"

@Composable
fun ResultsScreen() {
    val app = LocalContext.current.applicationContext as EuroJackpotApp
    val viewModel: ResultsViewModel = viewModel(
        factory = GenericViewModelFactory {
            ResultsViewModel(
                app.container.lotteryRepository,
                app.container.savedNumberRepository,
                app.container.compareNumbersUseCase,
                app.container.currencyRepository
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
        item { Text(stringResource(R.string.results_title), style = MaterialTheme.typography.headlineMedium) }

        item {
            SectionCard(title = stringResource(R.string.results_latest)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.latestDraw?.let { draw ->
                        Text(DateFormatters.formatDisplay(draw.date), style = MaterialTheme.typography.bodyMedium)
                        LotteryCombinationRow(mainNumbers = draw.sortedMain, euroNumbers = draw.sortedEuro, ballSize = 32.dp)
                        if (state.isSampleData) {
                            Text(
                                stringResource(R.string.common_sample_data_notice),
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
                    } ?: Text(stringResource(R.string.results_empty_history))
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(OFFICIAL_RESULTS_URL))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(stringResource(R.string.results_view_winner_info))
                    }
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.results_country_breakdown_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CurrencyPicker(selected = state.selectedCurrency, onSelect = viewModel::selectCurrency)
                    Button(
                        onClick = { viewModel.toggleCountryBreakdown() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.latestDraw != null
                    ) {
                        Text(stringResource(R.string.results_country_breakdown_button))
                    }
                    if (state.isCountryBreakdownExpanded) {
                        val tiers = state.countryBreakdown?.tiers.orEmpty()
                        when {
                            state.isCountryBreakdownLoading -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            }
                            tiers.isNotEmpty() -> {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    tiers.forEach { tier ->
                                        PrizeTierBreakdownRow(
                                            tier = tier,
                                            currency = state.selectedCurrency,
                                            ratesPerPln = state.currencyRatesPerPln
                                        )
                                    }
                                    Text(
                                        text = stringResource(R.string.results_country_no_city_data),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            state.countryBreakdownUnavailable -> {
                                Text(
                                    text = stringResource(R.string.results_country_breakdown_unavailable),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            else -> {
                                Text(
                                    text = stringResource(R.string.results_country_breakdown_empty),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.results_check_title)) {
                Column {
                    Button(onClick = { viewModel.toggleCheckExpanded() }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.results_check_my_numbers))
                    }
                    if (state.isCheckExpanded) {
                        Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (state.comparisons.isEmpty()) {
                                Text(stringResource(R.string.my_numbers_empty), style = MaterialTheme.typography.bodyMedium)
                            }
                            state.comparisons.forEach { comparison ->
                                ComparisonRow(
                                    comparison = comparison,
                                    tierPrizeAmounts = state.countryBreakdown?.tierPrizeAmounts.orEmpty(),
                                    isPrizeLookupLoading = state.isCountryBreakdownLoading &&
                                        comparison.result.prizeTier != null &&
                                        state.countryBreakdownLoadedForDate != state.latestDraw?.date,
                                    currency = state.selectedCurrency,
                                    ratesPerPln = state.currencyRatesPerPln
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.results_history)) {
                Column {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.availableYears) { year ->
                            FilterChip(
                                selected = state.selectedYear == year,
                                onClick = { viewModel.selectYear(year) },
                                label = { Text(year.toString()) }
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (state.drawsForSelectedYear.isEmpty()) {
                            Text(stringResource(R.string.results_empty_history))
                        }
                        state.drawsForSelectedYear.forEach { draw ->
                            HistoryRow(draw)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(draw: DrawResult) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(DateFormatters.formatDisplay(draw.date), style = MaterialTheme.typography.labelLarge)
        LotteryCombinationRow(
            mainNumbers = draw.sortedMain,
            euroNumbers = draw.sortedEuro,
            ballSize = 32.dp,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        HorizontalDivider()
    }
}

/** One prize tier's country breakdown — a header ("5 + 1 matches — 4 winners") plus
 *  one row per country (see [CountryPrizeRow]). */
@Composable
private fun PrizeTierBreakdownRow(tier: PrizeBreakdownTier, currency: SupportedCurrency, ratesPerPln: Map<String, Double>?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val tierLabel = PrizeTier.forRank(tier.degree)
        Text(
            text = if (tierLabel != null) {
                stringResource(R.string.results_country_breakdown_tier_label, tierLabel.mainMatches, tierLabel.euroMatches)
            } else {
                stringResource(R.string.results_country_breakdown_tier_fallback, tier.degree)
            },
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = stringResource(R.string.results_country_breakdown_winner_count, tier.totalWinners),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        tier.countries.forEach { country ->
            CountryPrizeRow(country = country, currency = currency, ratesPerPln = ratesPerPln)
        }
        HorizontalDivider()
    }
}

/** One country's row within a prize tier — country name, winner count, and the prize
 *  value per winner in both its native EUR amount and the user's selected currency
 *  (see CurrencyFormat.convertAndFormatFromEur). This figure is EUR, not PLN — see
 *  CountryPrize.prizeValueEur's doc comment for why a cross-border, per-country
 *  breakdown like this one has to be in EuroJackpot's shared settlement currency rather
 *  than each country's own money. No city-level detail is shown here or anywhere in
 *  this screen — see the results_country_no_city_data note rendered alongside the whole
 *  breakdown, and PrizeBreakdown's doc comment for why. */
@Composable
private fun CountryPrizeRow(country: CountryPrize, currency: SupportedCurrency, ratesPerPln: Map<String, Double>?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = stringResource(country.country.labelRes()), style = MaterialTheme.typography.bodyMedium)
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(R.string.results_country_breakdown_winner_count, country.winners),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = CurrencyFormat.formatNative(country.prizeValueEur.toLong(), "EUR"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (currency != SupportedCurrency.EUR) {
                CurrencyFormat.convertAndFormatFromEur(country.prizeValueEur, currency, ratesPerPln)?.let { converted ->
                    Text(
                        text = stringResource(R.string.currency_converted_format, converted),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonRow(
    comparison: SavedNumberComparison,
    tierPrizeAmounts: Map<Int, TierPrizeAmount>,
    isPrizeLookupLoading: Boolean,
    currency: SupportedCurrency,
    ratesPerPln: Map<String, Double>?
) {
    Column {
        Text(comparison.savedNumber.name, style = MaterialTheme.typography.titleMedium)
        LotteryCombinationRow(
            mainNumbers = comparison.savedNumber.sortedMain,
            euroNumbers = comparison.savedNumber.sortedEuro,
            ballSize = 32.dp,
            modifier = Modifier.padding(vertical = 6.dp)
        )
        Text(stringResource(R.string.results_matched_main, comparison.result.mainMatches))
        Text(stringResource(R.string.results_matched_euro, comparison.result.euroMatches))
        comparison.result.prizeTier?.let { tier ->
            Text(
                stringResource(R.string.results_prize_tier, "#${tier.rank}"),
                color = MaterialTheme.colorScheme.primary
            )
            PrizeWonAmount(
                tier = tier,
                tierPrizeAmounts = tierPrizeAmounts,
                isLoading = isPrizeLookupLoading,
                currency = currency,
                ratesPerPln = ratesPerPln
            )
        } ?: Text(stringResource(R.string.results_no_prize), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The PLN amount (+ converted currency) LOTTO Poland actually paid out for [tier] on
 *  this draw — see PrizeBreakdown.tierPrizeAmounts' doc comment for exactly what this
 *  figure is and its one open caveat (it's Poland's own domestic payout table, not
 *  necessarily the same number as the cross-border "Winners by Country" amount for the
 *  same tier). Three distinct states, never silently blank: still loading, loaded but
 *  nobody in Poland actually won this tier this draw (no real amount to report), or a
 *  real amount to show. */
@Composable
private fun PrizeWonAmount(
    tier: PrizeTier,
    tierPrizeAmounts: Map<Int, TierPrizeAmount>,
    isLoading: Boolean,
    currency: SupportedCurrency,
    ratesPerPln: Map<String, Double>?
) {
    if (isLoading) {
        Text(
            stringResource(R.string.results_prize_amount_loading),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val amount = tierPrizeAmounts[tier.rank]
    if (amount == null || amount.winnersInPoland <= 0) {
        Text(
            stringResource(R.string.results_prize_amount_unavailable),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Column {
        Text(
            text = stringResource(
                R.string.results_prize_amount_native,
                CurrencyFormat.formatNative(amount.prizeValuePln.toLong(), "PLN")
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        if (currency != SupportedCurrency.PLN) {
            CurrencyFormat.convertAndFormat(amount.prizeValuePln, currency, ratesPerPln)?.let { converted ->
                Text(
                    text = stringResource(R.string.currency_converted_format, converted),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
