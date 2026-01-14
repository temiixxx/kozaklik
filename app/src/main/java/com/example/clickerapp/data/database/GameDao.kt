package com.example.clickerapp.data.database

import android.util.Log
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    companion object {
        private const val TAG = "GameDao"
    }

    @Query("SELECT * FROM game_state WHERE id = 0 LIMIT 1")
    fun observeState(): Flow<GameStateEntity?>

    @Query("SELECT * FROM game_state WHERE id = 0 LIMIT 1")
    suspend fun getState(): GameStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: GameStateEntity)
    
    @Transaction
    suspend fun buyWithCheck(
        cost: Long,
        updateFn: (GameStateEntity) -> GameStateEntity
    ): Boolean {
        try {
            Log.d(TAG, "buyWithCheck(cost=$cost): Starting transaction")
            val current = getState() ?: run {
                Log.d(TAG, "buyWithCheck: Creating new state")
                val newState = GameStateEntity()
                upsert(newState)
                return@run newState
            }
            
            Log.d(TAG, "buyWithCheck: Current state - points=${current.points}, id=${current.id}")
            if (current.points < cost) {
                Log.w(TAG, "buyWithCheck: Insufficient points. Current: ${current.points}, Cost: $cost")
                return false
            }
            
            val updated = updateFn(current)
            // Проверяем, что очки действительно уменьшились
            if (updated.points > current.points) {
                Log.e(TAG, "buyWithCheck: Points increased instead of decreased! Current: ${current.points}, Updated: ${updated.points}")
                return false
            }
            
            Log.d(TAG, "buyWithCheck: Updated state - points=${updated.points}, id=${updated.id}")
            upsert(updated)
            Log.d(TAG, "buyWithCheck: Purchase successful")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "buyWithCheck: Exception during purchase", e)
            return false
        }
    }
    
    @Transaction
    suspend fun buyWithCheck(
        costFn: (GameStateEntity) -> Long,
        updateFn: (GameStateEntity, Long) -> GameStateEntity
    ): Boolean {
        try {
            Log.d(TAG, "buyWithCheck(costFn): Starting transaction")
            val current = getState() ?: run {
                Log.d(TAG, "buyWithCheck: Creating new state")
                val newState = GameStateEntity()
                upsert(newState)
                return@run newState
            }
            
            Log.d(TAG, "buyWithCheck: Current state - points=${current.points}, id=${current.id}")
            val cost = costFn(current)
            Log.d(TAG, "buyWithCheck: Calculated cost: $cost")
            
            if (current.points < cost) {
                Log.w(TAG, "buyWithCheck: Insufficient points. Current: ${current.points}, Cost: $cost")
                return false
            }
            
            val updated = updateFn(current, cost)
            // Проверяем, что очки действительно уменьшились
            if (updated.points != current.points - cost) {
                Log.e(TAG, "buyWithCheck: Points calculation error! Expected: ${current.points - cost}, Got: ${updated.points}")
                return false
            }
            
            Log.d(TAG, "buyWithCheck: Updated state - points=${updated.points}, id=${updated.id}")
            upsert(updated)
            Log.d(TAG, "buyWithCheck: Purchase successful")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "buyWithCheck: Exception during purchase", e)
            return false
        }
    }
    
    @Transaction
    suspend fun buyWithCheck(
        updateFn: (GameStateEntity) -> GameStateEntity
    ): Boolean {
        try {
            Log.d(TAG, "buyWithCheck(updateFn): Starting transaction")
            val current = getState() ?: run {
                Log.d(TAG, "buyWithCheck: Creating new state")
                val newState = GameStateEntity()
                upsert(newState)
                return@run newState
            }
            
            Log.d(TAG, "buyWithCheck: Current state - points=${current.points}, id=${current.id}")
            val updated = updateFn(current)
            
            // Простая проверка: состояние должно измениться (очки должны уменьшиться)
            if (updated.points >= current.points) {
                Log.w(TAG, "buyWithCheck: Points did not decrease. Current: ${current.points}, Updated: ${updated.points}")
                return false
            }
            
            Log.d(TAG, "buyWithCheck: Updated state - points=${updated.points}, id=${updated.id}")
            upsert(updated)
            Log.d(TAG, "buyWithCheck: Purchase successful")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "buyWithCheck: Exception during purchase", e)
            return false
        }
    }
    
    /**
     * Атомарно обновляет состояние (например, при тапе).
     * Гарантирует, что при быстрых тапах не будет потери очков из-за race condition.
     */
    @Transaction
    suspend fun updateStateAtomically(
        updateFn: (GameStateEntity) -> GameStateEntity
    ) {
        try {
            val current = getState() ?: run {
                val newState = GameStateEntity()
                upsert(newState)
                return@run newState
            }
            val updated = updateFn(current)
            upsert(updated)
        } catch (e: Exception) {
            Log.e(TAG, "updateStateAtomically: Exception", e)
            throw e
        }
    }
}

