package com.eurojackpotai.app.data.repository

import com.eurojackpotai.app.data.local.dao.SavedNumberDao
import com.eurojackpotai.app.data.local.entity.toDomain
import com.eurojackpotai.app.data.local.entity.toEntity
import com.eurojackpotai.app.domain.model.SavedNumber
import com.eurojackpotai.app.domain.repository.SavedNumberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SavedNumberRepositoryImpl(
    private val dao: SavedNumberDao
) : SavedNumberRepository {

    override fun observeAll(): Flow<List<SavedNumber>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): SavedNumber? = dao.getById(id)?.toDomain()

    override suspend fun save(number: SavedNumber): Long = dao.insert(number.toEntity())

    override suspend fun update(number: SavedNumber) = dao.update(number.toEntity())

    override suspend fun delete(id: Long) = dao.deleteById(id)

    override suspend fun deleteMany(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        dao.deleteByIds(ids.toList())
    }

    override suspend fun setActive(id: Long, isActive: Boolean) = dao.setActive(id, isActive)
}
