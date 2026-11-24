package com.eurojackpotai.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.eurojackpotai.app.data.local.entity.SavedNumberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedNumberDao {

    @Query("SELECT * FROM saved_numbers ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SavedNumberEntity>>

    @Query("SELECT * FROM saved_numbers WHERE id = :id")
    suspend fun getById(id: Long): SavedNumberEntity?

    @Insert
    suspend fun insert(entity: SavedNumberEntity): Long

    @Update
    suspend fun update(entity: SavedNumberEntity)

    @Query("DELETE FROM saved_numbers WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Bulk delete for the My Numbers multi-select "delete selected" action — one
     *  statement instead of looping [deleteById], so selecting dozens of saved
     *  combinations and deleting them is one DB write, not dozens. */
    @Query("DELETE FROM saved_numbers WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE saved_numbers SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: Long, isActive: Boolean)
}
