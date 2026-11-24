package com.eurojackpotai.app.domain.usecase

import com.eurojackpotai.app.core.util.NumberValidation
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * One printed board ("1:", "2:", ...) on a scanned ticket — its own 5 main numbers and
 * (when found) its own 2 Euro numbers. A ticket can print more than one of these (a
 * "multi-board" ticket, buying several combinations on one slip); see
 * [ParseTicketTextUseCase] for how every board's own numbers are extracted, not just
 * the first.
 */
data class ParsedBoard(
    val mainNumbers: List<Int> = emptyList(),
    val euroNumbers: List<Int> = emptyList()
)

/**
 * What could be confidently read off a EuroJackpot Poland ticket (Totalizator
 * Sportowy's retail-terminal slip — the operator behind LOTTO OpenAPI). Every field is
 * nullable/possibly empty — OCR on a photographed ticket is never certain — so the
 * caller ([com.eurojackpotai.app.presentation.mynumbers.MyNumbersViewModel.startScan])
 * always shows this back to the user as a pre-filled, *editable* draft in
 * AddNumberDialog and never saves it directly. Same "never show fabricated data"
 * principle this app already applies everywhere else (see PrizeBreakdown, README).
 *
 * Ticket layout verified against a real purchased ticket (photo + owner-confirmed
 * numbers, 2026-09-28): printed text included
 * "27.04.26  19:49:11" (purchase time), "2 z 12   7 12" (Euro numbers section),
 * "5 z 50   1: 07 09 11 23 30" (main numbers, board 1), and
 * "DATA LOSOWANIA: 28.04.26  20:15" (the draw this ticket is actually for — one day
 * after purchase). The owner confirmed the real numbers as main [7, 9, 11, 23, 30],
 * Euro [7, 12] — note the last main number reads as "30", easy to misread as "50" at a
 * glance (why this whole feature never trusts its own OCR output without the user
 * reviewing it first).
 *
 * Second real ticket (photo + owner-confirmed numbers, 2026-10-14) revealed a *second*
 * printed layout: a 2-board ticket where "5 z 50" and "2 z 12" print once, side by
 * side, as a shared header row for BOTH boards —
 * e.g. "5 z 50                    2 z 12" — followed by one row per board carrying
 * that board's main numbers AND its own Euro numbers together, e.g.
 * "1: 06 14 23 36 45              2 12" then "2: 07 17 22 29 45              1 7".
 * Owner-confirmed board 1: main [6, 14, 23, 36, 45], Euro [2, 12]; board 2: main
 * [7, 17, 22, 29, 45], Euro [1, 7]. Because OCR reads a ticket roughly top row by top
 * row, a photo of this layout tends to produce recognized text where "5 z 50" and
 * "2 z 12" land right next to each other — with each board's actual numbers only
 * appearing on its own row below — which broke the original "5 z 50"-anchored
 * main-number regex (it only ever looked for a board's numbers directly after
 * "5 z 50", true on the first ticket's layout but not this one), and originally meant
 * only board 1 was ever extracted at all (board 2+ were just flagged as present). See
 * [mainBoardRegex] and [extractEuroNumbers]'s doc comments for both fixes — every
 * board on the ticket is now parsed, not only the first.
 *
 * This app has no verified source mapping a physical ticket to LOTTO's own official
 * draw/round numbering (same reasoning as [com.eurojackpotai.app.domain.model.SavedNumber.drawRoundLabel]'s
 * doc comment) — this ticket format prints a draw *date*, not a round number, so that's
 * what this parser extracts and what gets saved to [com.eurojackpotai.app.domain.model.SavedNumber.targetDrawDate].
 *
 * Scoped specifically to this one ticket format (Poland, Totalizator Sportowy retail
 * terminal slip) — a screenshot of an online purchase confirmation, or a paper ticket
 * from a different country's EuroJackpot operator, prints differently and this parser
 * will simply fail to find a match for some/all fields (never guesses a fallback).
 */
data class ParsedTicket(
    // One entry per board found on the ticket, in the order they're printed. Empty
    // when nothing could be read at all.
    val boards: List<ParsedBoard> = emptyList(),
    val drawDate: LocalDate? = null,
    // Whatever raw reference text was found (the terminal number printed as
    // "TERM. 20006931") — surfaced to the user as a note only, never treated as an
    // official round number (see the class doc comment above). One ticket, so this
    // (unlike the numbers above) is never per-board.
    val ticketReference: String? = null
) {
    // Convenience accessors for the common case (most tickets print exactly one
    // board) and for callers that only care about "the first thing recognized" —
    // e.g. what to pre-fill before the user has picked a board to review.
    val mainNumbers: List<Int> get() = boards.firstOrNull()?.mainNumbers.orEmpty()
    val euroNumbers: List<Int> get() = boards.firstOrNull()?.euroNumbers.orEmpty()
    val additionalBoardsDetected: Boolean get() = boards.size > 1
}

object ParseTicketTextUseCase {

    // Anchors directly on a board's own index prefix ("1:", "2:", ...) immediately
    // followed by exactly 5 numbers, rather than on the decorative "5 z 50" header
    // text above it (what an earlier version did). That broke on the 2026-10-14
    // ticket, where "5 z 50" and "2 z 12" print together as one shared header row for
    // BOTH boards — leaving each board's actual numbers a full row away from "5 z 50",
    // separated by the other header's own text ("2 z 12"), which the old regex's small
    // bounded gap couldn't skip past (see the class doc comment). The "N:" prefix, by
    // contrast, sits directly against its own board's numbers on every layout seen so
    // far. findAll (not find) is used so every board's "N: ..." entry is matched, not
    // just the first — that's what lets every board (not only board 1) get its own
    // numbers extracted, and lets additionalBoardsDetected work without a separate
    // regex.
    private val mainBoardRegex =
        Regex("""(?<![\d.])(\d{1,2})\s*[:;.]\s*(\d{1,2}(?:\s+\d{1,2}){4})""")

    // Primary Euro-number strategy for board 1 only: anchored on the "2 z 12" header
    // text, for the layout where the Euro numbers print as their own short segment
    // shortly after that header (the 2026-09-28 reference ticket:
    // "...2 z 12   7 12..."). Tried first since it's the tighter, more specific match;
    // [extractEuroNumbers] falls back to a second, per-board strategy when this one
    // finds nothing (always the case for board 2+, since this header only ever prints
    // once per ticket).
    private val euroHeaderRegex =
        Regex("""2\s*z\s*12[^\d]{0,15}?(\d{1,2}\s+\d{1,2})(?=\D|$)""", RegexOption.IGNORE_CASE)
    private val euroFallbackRegex = Regex("""(?<![\d.])(\d{1,2})\s+(\d{1,2})(?![\d.])""")
    private val drawDateRegex =
        Regex("""DATA\s*LOSOWA\w{0,10}[^\d]{0,10}(\d{2}\.\d{2}\.\d{2})""", RegexOption.IGNORE_CASE)
    private val terminalRegex = Regex("""TERM\.?\s*(\d{4,10})""", RegexOption.IGNORE_CASE)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy")

    // Bound for extractEuroNumbers's fallback window when there's no following board
    // to mark where this board's row ends (always true for the last board on the
    // ticket) — generous enough to span "   2 12" but short enough to never reach into
    // an unrelated number printed elsewhere on the ticket.
    private const val FALLBACK_EURO_WINDOW = 20

    operator fun invoke(rawText: String): ParsedTicket {
        // ML Kit's Text.getText() already joins recognized lines with '\n'; collapsing
        // to single spaces makes every regex above tolerant of exactly where a line
        // break landed, which varies with photo angle/crop and isn't meaningful data.
        val normalized = rawText.replace(Regex("\\s+"), " ")

        val boardMatches = mainBoardRegex.findAll(normalized).toList()

        val boards = boardMatches.mapIndexed { index, match ->
            val main = match.groupValues[2]
                .trim()
                .split(Regex("\\s+"))
                .mapNotNull { it.toIntOrNull() }
                .takeIf { NumberValidation.validateMain(it).isEmpty() }
                .orEmpty()
            ParsedBoard(
                mainNumbers = main,
                euroNumbers = extractEuroNumbers(normalized, boardMatches, index)
            )
        }

        val drawDate = drawDateRegex.find(normalized)
            ?.groupValues?.get(1)
            ?.let { raw -> runCatching { LocalDate.parse(raw, dateFormatter) }.getOrNull() }

        val reference = terminalRegex.find(normalized)?.groupValues?.get(1)

        return ParsedTicket(
            boards = boards,
            drawDate = drawDate,
            ticketReference = reference
        )
    }

    /** For board 1, tries the "2 z 12"-header strategy ([euroHeaderRegex]) first. If
     *  that finds nothing — true for every board on the 2026-10-14 ticket's layout,
     *  where the header's own text sits between "2 z 12" and the actual digits, so it
     *  never matches at all — falls back to reading the first 2 standalone numbers
     *  that immediately follow this board's own main-number row, up to wherever the
     *  next board's row starts (or a short bounded window if this is the last board).
     *  That's exactly where that layout prints each board's own Euro numbers (e.g.
     *  "...1: 06 14 23 36 45   2 12   2: 07 17 22 29 45   1 7": "2 12" is board 1's,
     *  "1 7" is board 2's). */
    private fun extractEuroNumbers(normalized: String, boardMatches: List<MatchResult>, boardIndex: Int): List<Int> {
        if (boardIndex == 0) {
            euroHeaderRegex.find(normalized)
                ?.groupValues?.get(1)
                ?.trim()
                ?.split(Regex("\\s+"))
                ?.mapNotNull { it.toIntOrNull() }
                ?.takeIf { NumberValidation.validateEuro(it).isEmpty() }
                ?.let { return it }
        }

        val board = boardMatches.getOrNull(boardIndex) ?: return emptyList()
        val windowStart = board.range.last + 1
        val windowEnd = boardMatches.getOrNull(boardIndex + 1)?.range?.first
            ?.takeIf { it > windowStart }
            ?: (windowStart + FALLBACK_EURO_WINDOW).coerceAtMost(normalized.length)
        if (windowStart >= windowEnd) return emptyList()
        val window = normalized.substring(windowStart, windowEnd)

        return euroFallbackRegex.find(window)
            ?.let { listOfNotNull(it.groupValues[1].toIntOrNull(), it.groupValues[2].toIntOrNull()) }
            ?.takeIf { NumberValidation.validateEuro(it).isEmpty() }
            .orEmpty()
    }
}
