package com.example.clickerapp.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
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
        val current = getState() ?: GameStateEntity().also { upsert(it) }
        if (current.points < cost) return false
        val updated = updateFn(current)
        upsert(updated)
        return true
    }
    
    @Transaction
    suspend fun buyWithCheck(
        costFn: (GameStateEntity) -> Long,
        updateFn: (GameStateEntity, Long) -> GameStateEntity
    ): Boolean {
        val current = getState() ?: GameStateEntity().also { upsert(it) }
        val cost = costFn(current) // Вычисляем стоимость внутри транзакции на основе актуального состояния
        if (current.points < cost) return false
        val updated = updateFn(current, cost)
        upsert(updated)
        return true
    }
    
    @Transaction
    suspend fun buyWithCheck(
        updateFn: (GameStateEntity) -> GameStateEntity
    ): Boolean {
        val current = getState() ?: GameStateEntity().also { upsert(it) }
        val updated = updateFn(current)
        // Проверяем, что состояние действительно изменилось (сравниваем ключевые поля)
        val changed = updated.points != current.points || 
                     updated.miningPower != current.miningPower ||
                     updated.tapPower != current.tapPower ||
                     updated.autoClickers != current.autoClickers ||
                     updated.autoPower != current.autoPower ||
                     updated.goatPenLevel != current.goatPenLevel ||
                     updated.goatFoodLevel != current.goatFoodLevel ||
                     updated.fridgeLevel != current.fridgeLevel ||
                     updated.printerLevel != current.printerLevel ||
                     updated.scannerLevel != current.scannerLevel ||
                     updated.printer3dLevel != current.printer3dLevel ||
                     updated.pointsMultiplier != current.pointsMultiplier ||
                     updated.autoClickerSpeed != current.autoClickerSpeed ||
                     updated.comboBonus != current.comboBonus ||
                     updated.offlineMultiplier != current.offlineMultiplier ||
                     updated.premiumUpgrade1 != current.premiumUpgrade1 ||
                     updated.premiumUpgrade2 != current.premiumUpgrade2 ||
                     updated.cryptoAmount != current.cryptoAmount ||
                     updated.hasSoldCrypto != current.hasSoldCrypto
        if (!changed) return false
        upsert(updated)
        return true
    }
}

