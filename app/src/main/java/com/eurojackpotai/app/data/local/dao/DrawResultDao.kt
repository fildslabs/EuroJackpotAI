package com.eurojackpotai.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.eurojackpotai.app.data.local.entity.DrawResultEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DrawResultDao {

    @Query("SELECT * FROM draw_results ORDER BY drawDate DESC")
    fun observeAll(): Flow<List<DrawResultEntity>>

    @Query("SELECT * FROM draw_results ORDER BY drawDate DESC LIMIT 1")
    fun observeLatest(): Flow<DrawResultEntity?>

    @Query("SELECT * FROM draw_results ORDER BY drawDate DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<DrawResultEntity>

    @Query(
        "SELECT * FROM draw_results WHERE drawDate BETWEEN :start AND :end ORDER BY drawDate DESC"
    )
    suspend fun getForYear(start: LocalDate, end: LocalDate): List<DrawResultEntity>

    @Query("SELECT DISTINCT CAST(strftime('%Y', drawDate * 86400, 'unixepoch') AS INTEGER) FROM draw_results ORDER BY 1 DESC")
    suspend fun getAvailableYears(): List<Int>

    @Query("SELECT COUNT(*) FROM draw_results")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM draw_results WHERE isSampleData = 1")
    suspend fun sampleDataCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM draw_results WHERE drawDate = :date)")
    suspend fun existsForDate(date: LocalDate): Boolean

    /** Same as [existsForDate] but ignores sample rows — used to decide whether a
     *  freshly-synced real draw still needs inserting. Plain [existsForDate] would
     *  wrongly say "yes" when the bundled sample history (which is generated relative
     *  to today's date, see SampleDrawSeeder) happens to already have a row on the same
     *  calendar date as the real draw, silently blocking the real sync forever. */
    @Query("SELECT EXISTS(SELECT 1 FROM draw_results WHERE drawDate = :date AND isSampleData = 0)")
    suspend fun existsRealDataForDate(date: LocalDate): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(draws: List<DrawResultEntity>)

    /** Replaces the bundled sample rows once real data has been synced at least once. */
    @Query("DELETE FROM draw_results WHERE isSampleData = 1")
    suspend fun clearSampleData()
}
