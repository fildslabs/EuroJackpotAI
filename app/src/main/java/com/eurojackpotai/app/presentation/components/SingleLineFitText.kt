package com.eurojackpotai.app.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * A single line of text that shrinks its own font size, in small steps, until it fits
 * the available width without wrapping — for a "hero number" label (the Home screen's
 * jackpot amount is the first user) whose *content* varies in length (a 10-digit PLN
 * amount vs. a much shorter EUR one) but whose *layout* should always read as one
 * confident line, never a value that happens to wrap onto a second line depending on
 * the device's screen size (reported directly: "메인화면의 당첨금도 화면 사이즈에 따라
 * 줄바꿈으로 보이는 부분").
 *
 * Plain [Text] can't do this on its own — a fixed [style] has no way to know its own
 * eventual width before layout happens, so this re-measures via [onTextLayout] and
 * steps the font size down whenever [androidx.compose.ui.text.TextLayoutResult.didOverflowWidth]
 * comes back true, converging after a couple of recompositions. [text] is watched so a
 * new, shorter value (switching from a PLN amount to a smaller converted currency, say)
 * starts back at [style]'s own size rather than staying stuck at a previous shrink.
 */
@Composable
fun SingleLineFitText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    minFontSize: TextUnit = 20.sp
) {
    var fontSize by remember(text, style) { mutableStateOf(style.fontSize) }
    Text(
        text = text,
        style = style.copy(fontSize = fontSize),
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
        onTextLayout = { result ->
            if (result.didOverflowWidth && fontSize.value > minFontSize.value) {
                fontSize = (fontSize.value * 0.92f).sp
            }
        }
    )
}
