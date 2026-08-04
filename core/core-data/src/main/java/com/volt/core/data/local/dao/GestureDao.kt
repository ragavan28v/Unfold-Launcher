package com.volt.core.data.local.dao

import androidx.room.*
import com.volt.core.data.local.entity.GestureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GestureDao {
    @Query("SELECT * FROM gesture_bindings")
    fun observeAllBindings(): Flow<List<GestureEntity>>

    @Query("SELECT * FROM gesture_bindings WHERE gestureType = :gestureType")
    suspend fun getBinding(gestureType: String): GestureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBinding(binding: GestureEntity)

    @Query("DELETE FROM gesture_bindings")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bindings: List<GestureEntity>)
}
