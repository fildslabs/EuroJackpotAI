package com.eurojackpotai.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.eurojackpotai.app.data.local.dao.DrawResultDao
import com.eurojackpotai.app.data.local.dao.SavedNumberDao
import com.eurojackpotai.app.data.local.entity.DrawResultEntity
import com.eurojackpotai.app.data.local.entity.SavedNumberEntity

// v2: SavedNumberEntity gained targetDrawDate/drawRoundLabel/ticketPhotoPath (My
// Numbers draw-round tagging + ticket photo attachment). The version number MUST bump
// whenever an @Entity's columns change — Room checks the on-device database's actual
// schema hash against what the compiled entities expect, and a mismatch at the same
// version number is a fatal crash on launch, not a warning (this is what caused the
// "reinstall and it crashes immediately" bug after the previous update). AppContainer's
// fallbackToDestructiveMigration() means this bump just wipes and recreates local data
// (saved numbers, cached draw history) rather than requiring a real migration — fine
// for this dev-phase app, but worth calling out to the user once before they update.
@Database(
    entities = [DrawResultEntity::class, SavedNumberEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun drawResultDao(): DrawResultDao
    abstract fun savedNumberDao(): SavedNumberDao

    companion object {
        const val DATABASE_NAME = "eurojackpot.db"
    }
}
