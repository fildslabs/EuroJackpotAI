package com.eurojackpotai.app.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eurojackpotai.app.data.local.entity.DrawResultEntity
import com.eurojackpotai.app.data.local.entity.SavedNumberEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Instrumented (needs a device/emulator) — Room's query verifier and SQLite driver
 * aren't available on the plain JVM unit test runner. Covers product spec section 36
 * (DatabaseTest): save, update, delete, query for both tables.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var database: AppDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun savedNumber_saveThenQuery_returnsSameValues() = runBlocking {
        val dao = database.savedNumberDao()
        val entity = SavedNumberEntity(
            name = "Favorite",
            mainNumbersCsv = "7,18,23,41,47",
            euroNumbersCsv = "4,11",
            source = "MANUAL",
            generationMode = null,
            note = "birthday numbers",
            isActive = true,
            createdAt = LocalDateTime.of(2026, 1, 1, 12, 0)
        )

        val id = dao.insert(entity)
        val loaded = dao.getById(id)

        assertEquals("Favorite", loaded?.name)
        assertEquals("7,18,23,41,47", loaded?.mainNumbersCsv)
    }

    @Test
    fun savedNumber_update_persistsChange() = runBlocking {
        val dao = database.savedNumberDao()
        val id = dao.insert(
            SavedNumberEntity(
                name = "Original",
                mainNumbersCsv = "1,2,3,4,5",
                euroNumbersCsv = "1,2",
                source = "MANUAL",
                generationMode = null,
                note = "",
                isActive = true,
                createdAt = LocalDateTime.now()
            )
        )

        val loaded = dao.getById(id)!!
        dao.update(loaded.copy(name = "Renamed"))

        assertEquals("Renamed", dao.getById(id)?.name)
    }

    @Test
    fun savedNumber_delete_removesRow() = runBlocking {
        val dao = database.savedNumberDao()
        val id = dao.insert(
            SavedNumberEntity(
                name = "ToDelete",
                mainNumbersCsv = "1,2,3,4,5",
                euroNumbersCsv = "1,2",
                source = "MANUAL",
                generationMode = null,
                note = "",
                isActive = true,
                createdAt = LocalDateTime.now()
            )
        )

        dao.deleteById(id)

        assertNull(dao.getById(id))
    }

    @Test
    fun savedNumber_observeAll_reflectsInsertsInCreatedOrder() = runBlocking {
        val dao = database.savedNumberDao()
        dao.insert(baseSavedNumber("First", LocalDateTime.of(2026, 1, 1, 0, 0)))
        dao.insert(baseSavedNumber("Second", LocalDateTime.of(2026, 1, 2, 0, 0)))

        val all = dao.observeAll().first()

        assertEquals(2, all.size)
        assertEquals("Second", all.first().name) // newest first
    }

    @Test
    fun drawResult_insertAndQuery_worksAndIgnoresDuplicateDates() = runBlocking {
        val dao = database.drawResultDao()
        val date = LocalDate.of(2026, 1, 9)
        val draw = DrawResultEntity(
            drawDate = date,
            mainNumbersCsv = "4,25,30,32,33",
            euroNumbersCsv = "4,5",
            jackpotEur = null,
            isSampleData = false
        )

        dao.insertAll(listOf(draw))
        dao.insertAll(listOf(draw.copy(mainNumbersCsv = "1,2,3,4,5"))) // same date, should be ignored

        assertTrue(dao.existsForDate(date))
        assertEquals(1, dao.count())
        assertEquals("4,25,30,32,33", dao.getRecent(1).first().mainNumbersCsv)
    }

    @Test
    fun drawResult_clearSampleData_onlyRemovesSampleRows() = runBlocking {
        val dao = database.drawResultDao()
        dao.insertAll(
            listOf(
                DrawResultEntity(
                    drawDate = LocalDate.of(2026, 1, 2),
                    mainNumbersCsv = "1,2,3,4,5",
                    euroNumbersCsv = "1,2",
                    jackpotEur = null,
                    isSampleData = true
                ),
                DrawResultEntity(
                    drawDate = LocalDate.of(2026, 1, 9),
                    mainNumbersCsv = "6,7,8,9,10",
                    euroNumbersCsv = "3,4",
                    jackpotEur = null,
                    isSampleData = false
                )
            )
        )

        dao.clearSampleData()

        assertEquals(1, dao.count())
        assertEquals(0, dao.sampleDataCount())
    }

    private fun baseSavedNumber(name: String, createdAt: LocalDateTime) = SavedNumberEntity(
        name = name,
        mainNumbersCsv = "1,2,3,4,5",
        euroNumbersCsv = "1,2",
        source = "MANUAL",
        generationMode = null,
        note = "",
        isActive = true,
        createdAt = createdAt
    )
}
