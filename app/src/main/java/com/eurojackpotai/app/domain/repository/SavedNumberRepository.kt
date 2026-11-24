package com.eurojackpotai.app.domain.repository

import com.eurojackpotai.app.domain.model.SavedNumber
import kotlinx.coroutines.flow.Flow

interface SavedNumberRepository {
    fun observeAll(): Flow<List<SavedNumber>>
    suspend fun getById(id: Long): SavedNumber?
    suspend fun save(number: SavedNumber): Long
    suspend fun update(number: SavedNumber)
    suspend fun delete(id: Long)
    suspend fun deleteMany(ids: Collection<Long>)
    suspend fun setActive(id: Long, isActive: Boolean)
}
