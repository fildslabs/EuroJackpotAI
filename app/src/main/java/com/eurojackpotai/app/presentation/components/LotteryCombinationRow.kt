package com.eurojackpotai.app.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Renders "MAIN MAIN MAIN MAIN MAIN | EURO EURO" as the spec's example rows show.
 *
 *  5 main + 2 euro balls at the default [ballSize] don't always fit a narrow phone's
 *  width once a card's own padding is subtracted — reported directly as numbers
 *  getting cut off depending on screen size. Callers in a tight spot should pass a
 *  smaller [ballSize] (see the "Latest Draw"/My Numbers call sites), but as a last
 *  resort this is horizontally scrollable so an unexpectedly-narrow width (or a
 *  Design that uses a system font-scale affecting the surrounding layout) degrades to
 *  "swipe to see the rest" rather than silently clipping a ball off invisibly. */
@Composable
fun LotteryCombinationRow(
    mainNumbers: List<Int>,
    euroNumbers: List<Int>,
    ballSize: Dp = 40.dp,
    onNumberClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        mainNumbers.forEach { number ->
            NumberBall(
                number = number,
                kind = BallKind.MAIN,
                size = ballSize,
                onClick = onNumberClick?.let { { it(number) } }
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        HorizontalDivider(
            modifier = Modifier
                .width(1.dp)
                .height(ballSize)
        )
        Spacer(modifier = Modifier.width(4.dp))
        euroNumbers.forEach { number ->
            NumberBall(
                number = number,
                kind = BallKind.EURO,
                size = ballSize,
                onClick = onNumberClick?.let { { it(number) } }
            )
        }
    }
}
