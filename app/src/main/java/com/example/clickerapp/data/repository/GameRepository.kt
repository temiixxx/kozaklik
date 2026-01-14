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
    
    // Престиж система
    val prestigeLevel: Int = 0,
    val prestigePoints: Long = 0L,
    
    // Временные бусты
    val boostMultiplier: Int = 1,
    val boostEndTime: Long = 0L,
    
    // Квесты
    val activeQuestType: String = "",
    val activeQuestProgress: Long = 0L,
    val activeQuestTarget: Long = 0L,
    val activeQuestReward: Long = 0L,
    val lastQuestResetTime: Long = 0L,
    
    // События
    val activeEventType: String = "",
    val activeEventEndTime: Long = 0L,
)

class GameRepository(
    private val dao: GameDao,
    private val achievementDao: AchievementDao,
) {
    private val TAG = "GameRepository"
    val state: Flow<GameState> =
        dao.observeState().map { entity ->
            val domain = entity?.toDomain() ?: GameState(
                points = 0, tapPower = 1, autoClickers = 0, autoPower = 1, totalTaps = 0,
                pointsMultiplier = 1, autoClickerSpeed = 1, comboBonus = 0, offlineMultiplier = 1,
                premiumUpgrade1 = 0, premiumUpgrade2 = 0, lastTapTime = 0L,
                goatPenLevel = 0, goatFoodLevel = 0,
                fridgeLevel = 0, printerLevel = 0, scannerLevel = 0, printer3dLevel = 0,
                cryptoAmount = 0L, miningPower = 0, lastMiningTime = 0L, hasSoldCrypto = false,
                prestigeLevel = 0, prestigePoints = 0L,
                boostMultiplier = 1, boostEndTime = 0L,
                activeQuestType = "", activeQuestProgress = 0L, activeQuestTarget = 0L,
                activeQuestReward = 0L, lastQuestResetTime = 0L,
                activeEventType = "", activeEventEndTime = 0L
            )
            Log.d(TAG, "State Flow updated - points=${domain.points}, tapPower=${domain.tapPower}, autoClickers=${domain.autoClickers}")
            domain
        }

    val achievements: Flow<Set<String>> =
        achievementDao.observeAll().map { list -> list.map { it.id }.toSet() }

    suspend fun getOrCreate(): GameStateEntity =
        dao.getState() ?: GameStateEntity().also { dao.upsert(it) }

    suspend fun save(state: GameStateEntity) {
        Log.d(TAG, "save: Saving state - points=${state.points}, id=${state.id}")
        dao.upsert(state)
        Log.d(TAG, "save: State saved successfully")
    }
    
    /**
     * Атомарно обновляет состояние (например, при тапе).
     * Гарантирует, что при быстрых тапах не будет потери очков из-за race condition.
     */
    suspend fun updateStateAtomically(
        updateFn: (GameStateEntity) -> GameStateEntity
    ) {
        dao.updateStateAtomically(updateFn)
    }

    suspend fun unlockAchievement(id: String, unlockedAtEpochMs: Long) {
        achievementDao.insertIgnore(AchievementEntity(id = id, unlockedAtEpochMs = unlockedAtEpochMs))
    }

    /**
     * Начисляет offline-доход за прошедшее время.
     * Кап по времени защищает от "миллионов" после долгого отсутствия.
     */
    suspend fun applyOfflineIncome(nowEpochMs: Long, capSeconds: Long = 8 * 60 * 60) {
        updateStateAtomically { current ->
            val lastSeen = current.lastSeenEpochMs
            if (lastSeen <= 0L) {
                current.copy(lastSeenEpochMs = nowEpochMs)
            } else {
                val deltaSec = ((nowEpochMs - lastSeen) / 1_000).coerceAtLeast(0)
                val effectiveSec = minOf(deltaSec, capSeconds)
                val basePerSec = (current.autoClickers * current.autoPower).toLong()
                val perSec = basePerSec * current.offlineMultiplier
                val gain = effectiveSec * perSec
                current.copy(
                    points = current.points + gain,
                    lastSeenEpochMs = nowEpochMs,
                )
            }
        }
    }

    suspend fun markSeen(nowEpochMs: Long) {
        updateStateAtomically { current ->
            current.copy(lastSeenEpochMs = nowEpochMs)
        }
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
    prestigeLevel = prestigeLevel,
    prestigePoints = prestigePoints,
    boostMultiplier = boostMultiplier,
    boostEndTime = boostEndTime,
    activeQuestType = activeQuestType,
    activeQuestProgress = activeQuestProgress,
    activeQuestTarget = activeQuestTarget,
    activeQuestReward = activeQuestReward,
    lastQuestResetTime = lastQuestResetTime,
    activeEventType = activeEventType,
    activeEventEndTime = activeEventEndTime,
)

