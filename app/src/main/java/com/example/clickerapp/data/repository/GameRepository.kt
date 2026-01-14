package com.example.clickerapp.data.repository

import android.util.Log
import com.example.clickerapp.data.database.AchievementDao
import com.example.clickerapp.data.database.AchievementEntity
import com.example.clickerapp.data.database.GameDao
import com.example.clickerapp.data.database.GameStateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class GameState(
    val points: Long,
    val tapPower: Int,
    val autoClickers: Int,
    val autoPower: Int,
    val totalTaps: Long,
    val pointsMultiplier: Int = 1,
    val autoClickerSpeed: Int = 1,
    val comboBonus: Int = 0,
    val offlineMultiplier: Int = 1,
    val premiumUpgrade1: Int = 0,
    val premiumUpgrade2: Int = 0,
    val lastTapTime: Long = 0L,
    val goatPenLevel: Int = 0,
    val goatFoodLevel: Int = 0,
    val fridgeLevel: Int = 0,
    val printerLevel: Int = 0,
    val scannerLevel: Int = 0,
    val printer3dLevel: Int = 0,
    val cryptoAmount: Long = 0L,
    val miningPower: Int = 0,
    val lastMiningTime: Long = 0L,
    val hasSoldCrypto: Boolean = false,
)

class GameRepository(
    private val dao: GameDao,
    private val achievementDao: AchievementDao,
) {
    private val TAG = "GameRepository"
    val state: Flow<GameState> =
        dao.observeState().map { it?.toDomain() ?: GameState(0, 1, 0, 1, 0, 1, 1, 0, 1, 0, 0, 0L, 0, 0, 0, 0, 0, 0, 0L, 0, 0L, false) }

    val achievements: Flow<Set<String>> =
        achievementDao.observeAll().map { list -> list.map { it.id }.toSet() }

    suspend fun getOrCreate(): GameStateEntity =
        dao.getState() ?: GameStateEntity().also { dao.upsert(it) }

    suspend fun save(state: GameStateEntity) {
        dao.upsert(state)
    }

    suspend fun unlockAchievement(id: String, unlockedAtEpochMs: Long) {
        achievementDao.insertIgnore(AchievementEntity(id = id, unlockedAtEpochMs = unlockedAtEpochMs))
    }

    /**
     * Начисляет offline-доход за прошедшее время.
     * Кап по времени защищает от “миллионов” после долгого отсутствия.
     */
    suspend fun applyOfflineIncome(nowEpochMs: Long, capSeconds: Long = 8 * 60 * 60) {
        val current = getOrCreate()
        val lastSeen = current.lastSeenEpochMs
        if (lastSeen <= 0L) {
            save(current.copy(lastSeenEpochMs = nowEpochMs))
            return
        }
        val deltaSec = ((nowEpochMs - lastSeen) / 1_000).coerceAtLeast(0)
        val effectiveSec = minOf(deltaSec, capSeconds)
        val basePerSec = (current.autoClickers * current.autoPower).toLong()
        val perSec = basePerSec * current.offlineMultiplier
        val gain = effectiveSec * perSec
        save(
            current.copy(
                points = current.points + gain,
                lastSeenEpochMs = nowEpochMs,
            )
        )
    }

    suspend fun markSeen(nowEpochMs: Long) {
        val current = getOrCreate()
        save(current.copy(lastSeenEpochMs = nowEpochMs))
    }
    
    /**
     * Атомарная покупка с проверкой стоимости.
     * Возвращает true, если покупка успешна, false если недостаточно очков.
     */
    suspend fun buyWithCheck(
        cost: Long,
        updateFn: (GameStateEntity) -> GameStateEntity
    ): Boolean {
        return try {
            Log.d(TAG, "buyWithCheck: Starting purchase with cost=$cost")
            val result = dao.buyWithCheck(cost, updateFn)
            if (result) {
                Log.d(TAG, "buyWithCheck: Purchase successful")
            } else {
                Log.w(TAG, "buyWithCheck: Purchase failed - insufficient points")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "buyWithCheck: Error during purchase", e)
            false
        }
    }
    
    /**
     * Атомарная покупка, где стоимость вычисляется внутри транзакции.
     * Возвращает true, если покупка успешна, false если недостаточно очков или состояние не изменилось.
     */
    suspend fun buyWithCheck(
        updateFn: (GameStateEntity) -> GameStateEntity
    ): Boolean {
        return try {
            Log.d(TAG, "buyWithCheck: Starting purchase with updateFn only")
            val result = dao.buyWithCheck(updateFn)
            if (result) {
                Log.d(TAG, "buyWithCheck: Purchase successful")
            } else {
                Log.w(TAG, "buyWithCheck: Purchase failed - state unchanged or insufficient points")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "buyWithCheck: Error during purchase", e)
            false
        }
    }
    
    /**
     * Атомарная покупка, где стоимость вычисляется внутри транзакции на основе актуального состояния.
     * Это гарантирует, что стоимость всегда вычисляется на основе текущего уровня улучшения.
     * Возвращает true, если покупка успешна, false если недостаточно очков.
     */
    suspend fun buyWithCheck(
        costFn: (GameStateEntity) -> Long,
        updateFn: (GameStateEntity, Long) -> GameStateEntity
    ): Boolean {
        return try {
            Log.d(TAG, "buyWithCheck: Starting purchase with costFn")
            val result = dao.buyWithCheck(costFn, updateFn)
            if (result) {
                Log.d(TAG, "buyWithCheck: Purchase successful")
            } else {
                Log.w(TAG, "buyWithCheck: Purchase failed - insufficient points")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "buyWithCheck: Error during purchase", e)
            false
        }
    }
}

private fun GameStateEntity.toDomain() = GameState(
    points = points,
    tapPower = tapPower,
    autoClickers = autoClickers,
    autoPower = autoPower,
    totalTaps = totalTaps,
    pointsMultiplier = pointsMultiplier,
    autoClickerSpeed = autoClickerSpeed,
    comboBonus = comboBonus,
    offlineMultiplier = offlineMultiplier,
    premiumUpgrade1 = premiumUpgrade1,
    premiumUpgrade2 = premiumUpgrade2,
    lastTapTime = lastTapTime,
    goatPenLevel = goatPenLevel,
    goatFoodLevel = goatFoodLevel,
    fridgeLevel = fridgeLevel,
    printerLevel = printerLevel,
    scannerLevel = scannerLevel,
    printer3dLevel = printer3dLevel,
    cryptoAmount = cryptoAmount,
    miningPower = miningPower,
    lastMiningTime = lastMiningTime,
    hasSoldCrypto = hasSoldCrypto,
)

