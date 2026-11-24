package com.eurojackpotai.app.data.local

import com.eurojackpotai.app.domain.model.DrawResult
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

/**
 * Generates a deterministic, clearly-fake draw history so the app has something to
 * show Statistics/Generate against on first launch and while offline, *before* the
 * user has configured a real LOTTO OpenAPI key (see LottoOpenApiRemoteDataSource).
 *
 * This is intentionally NOT scraped or hand-copied real EuroJackpot history: doing that
 * accurately for 2012-onward is out of scope for this phase and getting even one digit
 * wrong in a "past winning numbers" app is far worse than clearly labelling sample data
 * as sample data (see DrawResultEntity.isSampleData, surfaced in the UI as a
 * "SAMPLE DATA" badge). Every row produced here has isSampleData = true and a fixed
 * random seed, so re-installing the app always reproduces the exact same sample set.
 */
object SampleDrawSeeder {
    private const val SEED = 20260101L

    fun generate(drawCount: Int = 150, endDate: LocalDate = LocalDate.now()): List<DrawResult> {
        val random = Random(SEED)
        val dates = pastDrawDates(drawCount, endDate)
        return dates.map { date ->
            DrawResult(
                date = date,
                mainNumbers = randomDistinct(random, 1..50, 5),
                euroNumbers = randomDistinct(random, 1..12, 2),
                jackpotEur = null,
                isSampleData = true
            )
        }
    }

    /** Walks backward from [endDate] collecting the [count] most recent Tuesdays/Fridays. */
    private fun pastDrawDates(count: Int, endDate: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var cursor = endDate
        while (dates.size < count) {
            if (cursor.dayOfWeek == DayOfWeek.TUESDAY || cursor.dayOfWeek == DayOfWeek.FRIDAY) {
                dates += cursor
            }
            cursor = cursor.minusDays(1)
        }
        return dates
    }

    private fun randomDistinct(random: Random, range: IntRange, count: Int): List<Int> {
        val pool = range.toMutableList()
        val picked = mutableListOf<Int>()
        repeat(count) {
            val index = random.nextInt(pool.size)
            picked += pool.removeAt(index)
        }
        return picked.sorted()
    }
}
