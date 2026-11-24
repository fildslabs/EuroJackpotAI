package com.eurojackpotai.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Covers [ParseTicketTextUseCase] against text shaped like what ML Kit actually returns
 * for two real, owner-confirmed Poland/Totalizator Sportowy EuroJackpot tickets (see the
 * [ParsedTicket] class doc comment for both): a single-board ticket (purchase 2026-04-27,
 * main [7, 9, 11, 23, 30], Euro [7, 12], draw date 2026-04-28) and a 2-board ticket with a
 * different, shared-header layout (purchase 2026-10-13, board 1 main [6, 14, 23, 36, 45],
 * Euro [2, 12], draw date 2026-10-13 — see [twoColumnHeaderTicketText] below). The exact
 * line breaks below are illustrative — normalization collapses all whitespace before
 * matching, so these tests don't depend on where ML Kit happened to split lines. */
class ParseTicketTextUseCaseTest {

    // A simplified but representative reconstruction of the recognized text blocks from
    // the reference ticket photo.
    private val referenceTicketText = """
        EUROJACKPOT
        27.04.26  19:49:11
        TERM. 20006931
        5 z 50   1: 07 09 11 23 30
        2 z 12   7 12
        DATA LOSOWANIA: 28.04.26  20:15
    """.trimIndent()

    @Test
    fun `parses main numbers, euro numbers, and draw date off the reference ticket`() {
        val parsed = ParseTicketTextUseCase(referenceTicketText)

        assertEquals(listOf(7, 9, 11, 23, 30), parsed.mainNumbers)
        assertEquals(listOf(7, 12), parsed.euroNumbers)
        assertEquals(LocalDate.of(2026, 4, 28), parsed.drawDate)
        assertEquals("20006931", parsed.ticketReference)
        assertFalse(parsed.additionalBoardsDetected)
    }

    @Test
    fun `is tolerant of OCR line breaks landing in different places`() {
        // Same content, but with the numbers split across lines the way a rotated or
        // tightly-cropped photo might come back from ML Kit.
        val jumbled = "EUROJACKPOT\n27.04.26\n19:49:11\nTERM.\n20006931\n5 z 50\n1: 07 09\n11 23 30\n" +
            "2 z 12\n7\n12\nDATA LOSOWANIA:\n28.04.26\n20:15"

        val parsed = ParseTicketTextUseCase(jumbled)

        assertEquals(listOf(7, 9, 11, 23, 30), parsed.mainNumbers)
        assertEquals(listOf(7, 12), parsed.euroNumbers)
        assertEquals(LocalDate.of(2026, 4, 28), parsed.drawDate)
    }

    @Test
    fun `extracts the main numbers for every board, not just the first`() {
        val twoBoards = referenceTicketText.replace(
            "5 z 50   1: 07 09 11 23 30",
            "5 z 50   1: 07 09 11 23 30\n5 z 50   2: 02 14 21 29 40"
        )

        val parsed = ParseTicketTextUseCase(twoBoards)

        assertEquals(2, parsed.boards.size)
        // mainNumbers (the convenience accessor) still reflects board 1...
        assertEquals(listOf(7, 9, 11, 23, 30), parsed.mainNumbers)
        // ...but board 2's own numbers are now extracted too, not just flagged as
        // present and dropped.
        assertEquals(listOf(2, 14, 21, 29, 40), parsed.boards[1].mainNumbers)
        assertTrue(parsed.additionalBoardsDetected)
    }

    @Test
    fun `unrecognizable text yields an all-empty result, never a guess`() {
        val parsed = ParseTicketTextUseCase("blurry photo, nothing legible")

        assertEquals(emptyList<Int>(), parsed.mainNumbers)
        assertEquals(emptyList<Int>(), parsed.euroNumbers)
        assertNull(parsed.drawDate)
        assertNull(parsed.ticketReference)
        assertFalse(parsed.additionalBoardsDetected)
    }

    @Test
    fun `a match with an invalid number count or range is discarded rather than half-applied`() {
        // "5 z 50" anchor present, but only 4 numbers follow instead of 5 — should not
        // partially match or throw.
        val malformed = "5 z 50   1: 07 09 11 23"
        val parsed = ParseTicketTextUseCase(malformed)

        assertEquals(emptyList<Int>(), parsed.mainNumbers)
    }

    // A second real, owner-confirmed ticket (photo, 2026-10-14) uses a different
    // layout: a 2-board ticket where "5 z 50" and "2 z 12" print once as a shared
    // header row for both boards, and each board's row then carries its own main
    // numbers AND its own Euro numbers together. Purchase 2026-10-13, board 1 main
    // [6, 14, 23, 36, 45], Euro [2, 12], board 2 main [7, 17, 22, 29, 45], Euro [1, 7],
    // draw date 2026-10-13, terminal reference 20024382. This is the layout that broke
    // the original "5 z 50"-anchored regex, and also motivated extracting every
    // board's own numbers instead of only the first (see the class doc comment on
    // [ParsedTicket] and on [ParseTicketTextUseCase.mainBoardRegex]).
    private val twoColumnHeaderTicketText = """
        EURO JACKPOT
        13.10.26  17:47:31
        5 z 50   2 z 12
        1: 06 14 23 36 45   2 12
        2: 07 17 22 29 45   1 7
        DATA LOSOWANIA: 13.10.26 20:15
        KOL. 04012244   TERM. 20024382
    """.trimIndent()

    @Test
    fun `parses main and euro numbers for both boards off the shared-header 2-board ticket layout`() {
        val parsed = ParseTicketTextUseCase(twoColumnHeaderTicketText)

        assertEquals(2, parsed.boards.size)
        assertEquals(listOf(6, 14, 23, 36, 45), parsed.boards[0].mainNumbers)
        assertEquals(listOf(2, 12), parsed.boards[0].euroNumbers)
        // Board 2 used to be only flagged as present, never extracted — this ticket
        // layout is exactly what motivated extracting every board's own numbers.
        assertEquals(listOf(7, 17, 22, 29, 45), parsed.boards[1].mainNumbers)
        assertEquals(listOf(1, 7), parsed.boards[1].euroNumbers)
        // Convenience accessors still mirror board 1, for callers that only care about
        // "the first thing recognized".
        assertEquals(listOf(6, 14, 23, 36, 45), parsed.mainNumbers)
        assertEquals(listOf(2, 12), parsed.euroNumbers)
        assertEquals(LocalDate.of(2026, 10, 13), parsed.drawDate)
        assertEquals("20024382", parsed.ticketReference)
        assertTrue(parsed.additionalBoardsDetected)
    }
}
