package com.eurojackpotai.app.presentation.components

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.eurojackpotai.app.domain.model.SupportedCurrency

/** Row of currency-code chips — the one currency picker used on both Home (jackpot)
 *  and Results (per-country prize amounts), backed by the same CurrencyRepository
 *  singleton so a choice made on either screen applies to both (see AppContainer).
 *  Inline rather than a Settings-screen dropdown, since no Settings screen exists yet
 *  (see README's "What's deferred to a later phase"). */
@Composable
fun CurrencyPicker(selected: SupportedCurrency, onSelect: (SupportedCurrency) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(SupportedCurrency.entries) { currency ->
            FilterChip(
                selected = currency == selected,
                onClick = { onSelect(currency) },
                label = { Text(currency.code) }
            )
        }
    }
}
