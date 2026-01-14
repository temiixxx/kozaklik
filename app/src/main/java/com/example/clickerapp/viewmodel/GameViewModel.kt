package com.example.clickerapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clickerapp.data.database.GameStateEntity
import com.example.clickerapp.data.repository.GameRepository
import com.example.clickerapp.data.repository.GameState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(
    private val repository: GameRepository,
) : ViewModel() {
    private val TAG = "GameViewModel"

    val state: StateFlow<GameState> =
        repository.state.stateIn(
            viewModelScope, 
            SharingStarted.WhileSubscribed(5_000), 
            GameState(
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
        )

    val achievements: StateFlow<Set<String>> =
        repository.achievements.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private var autoJob: Job? = null
    private var miningJob: Job? = null
    private var roomJob: Job? = null

        init {
            viewModelScope.launch {
                repository.getOrCreate()
            }
            viewModelScope.launch {
                repository.applyOfflineIncome(nowEpochMs = System.currentTimeMillis())
            }
            // Автоматически генерируем квест, если его нет
            viewModelScope.launch {
                val currentState = repository.getOrCreate()
                if (currentState.activeQuestType.isEmpty()) {
                    generateNewQuest()
                }
            }
        // Логирование обновлений состояния для отладки
        viewModelScope.launch {
            state.collect { gameState ->
                Log.d(TAG, "StateFlow updated - points=${gameState.points}, tapPower=${gameState.tapPower}, autoClickers=${gameState.autoClickers}, miningPower=${gameState.miningPower}")
            }
        }
        // Unlock achievements reactively (writes go to another table, so no infinite loop).
        viewModelScope.launch {
            combine(state, achievements) { s, unlocked -> s to unlocked }.collect { (s, unlocked) ->
                checkAndUnlock(s, unlocked)
            }
        }
        startAutoLoop()
        startMiningLoop()
        startRoomLoop()
    }

    fun onAppBackgrounded() {
        viewModelScope.launch { repository.markSeen(System.currentTimeMillis()) }
    }

    fun onAppForegrounded() {
        viewModelScope.launch { repository.applyOfflineIncome(System.currentTimeMillis()) }
    }

    fun tapGoat() {
        viewModelScope.launch {
            repository.updateStateAtomically { current ->
                val now = System.currentTimeMillis()
                
                // Проверяем и обновляем активные бусты/события
                val activeBoostMultiplier = if (now < current.boostEndTime) current.boostMultiplier else 1
                val updatedBoost = if (now >= current.boostEndTime && current.boostMultiplier > 1) {
                    current.copy(boostMultiplier = 1, boostEndTime = 0L)
                } else {
                    current
                }
                val updatedEvents = if (now >= current.activeEventEndTime && current.activeEventType.isNotEmpty()) {
                    updatedBoost.copy(activeEventType = "", activeEventEndTime = 0L)
                } else {
                    updatedBoost
                }
                
                // Вычисляем доход
                val timeSinceLastTap = now - updatedEvents.lastTapTime
                val comboMultiplier = if (timeSinceLastTap < 2000 && updatedEvents.lastTapTime > 0) {
                    (updatedEvents.comboBonus + 1).coerceAtMost(10)
                } else {
                    1
                }
                
                val penBonus = 1.0 + (updatedEvents.goatPenLevel * 0.2)
                val foodMultiplier = 1.0 + (updatedEvents.goatFoodLevel * 0.15)
                
                val baseGain = (updatedEvents.tapPower * penBonus).toLong()
                val multiplierGain = baseGain * updatedEvents.pointsMultiplier
                val foodGain = (multiplierGain * foodMultiplier).toLong()
                val comboGain = foodGain * comboMultiplier
                val boostGain = comboGain * activeBoostMultiplier
                
                // Применяем бонус события "Двойной день"
                val eventMultiplier = if (updatedEvents.activeEventType == "double_day" && now < updatedEvents.activeEventEndTime) {
                    2
                } else {
                    1
                }
                val finalGain = boostGain * eventMultiplier
                
                // Обновляем прогресс квеста "taps"
                val afterTapsQuest = if (updatedEvents.activeQuestType == "taps") {
                    val newProgress = updatedEvents.activeQuestProgress + 1
                    if (newProgress >= updatedEvents.activeQuestTarget) {
                        updatedEvents.copy(
                            activeQuestType = "",
                            activeQuestProgress = 0L,
                            activeQuestTarget = 0L,
                            activeQuestReward = 0L,
                            points = updatedEvents.points + finalGain + updatedEvents.activeQuestReward
                        )
                    } else {
                        updatedEvents.copy(
                            activeQuestProgress = newProgress,
                            points = updatedEvents.points + finalGain
                        )
                    }
                } else {
                    updatedEvents.copy(points = updatedEvents.points + finalGain)
                }
                
                // Обновляем прогресс квеста "points" (если есть)
                val afterPointsQuest = if (afterTapsQuest.activeQuestType == "points") {
                    val newProgress = afterTapsQuest.activeQuestProgress + finalGain
                    if (newProgress >= afterTapsQuest.activeQuestTarget) {
                        afterTapsQuest.copy(
                            activeQuestType = "",
                            activeQuestProgress = 0L,
                            activeQuestTarget = 0L,
                            activeQuestReward = 0L,
                            points = afterTapsQuest.points + afterTapsQuest.activeQuestReward
                        )
                    } else {
                        afterTapsQuest.copy(activeQuestProgress = newProgress)
                    }
                } else {
                    afterTapsQuest
                }
                
                val updatedQuest = afterPointsQuest
                
                updatedQuest.copy(
                    totalTaps = updatedQuest.totalTaps + 1,
                    lastTapTime = now,
                    boostMultiplier = activeBoostMultiplier,
                )
            }
        }
    }

    fun buyTapUpgrade() {
        viewModelScope.launch {
            val success = repository.buyWithCheck(
                costFn = { state -> tapUpgradeCost(state.tapPower) },
                updateFn = { state, cost ->
                    val updated = state.copy(
                        points = state.points - cost,
                        tapPower = state.tapPower + 1,
                    )
                    updateQuestProgress(updated, "upgrades", 1L)
                }
            )
            if (success) {
                Log.d(TAG, "buyTapUpgrade: Purchase successful")
            } else {
                Log.w(TAG, "buyTapUpgrade: Purchase failed")
            }
        }
    }
    
    /**
     * Обновляет прогресс квеста и возвращает обновленное состояние
     */
    private fun updateQuestProgress(
        state: GameStateEntity,
        questType: String,
        progressDelta: Long
    ): GameStateEntity {
        if (state.activeQuestType != questType || state.activeQuestTarget <= 0) {
            return state
        }
        val newProgress = state.activeQuestProgress + progressDelta
        return if (newProgress >= state.activeQuestTarget) {
            // Квест выполнен
            state.copy(
                activeQuestType = "",
                activeQuestProgress = 0L,
                activeQuestTarget = 0L,
                activeQuestReward = 0L,
                points = state.points + state.activeQuestReward
            )
        } else {
            state.copy(activeQuestProgress = newProgress)
        }
    }

    fun buyAutoClicker() {
        viewModelScope.launch {
            val success = repository.buyWithCheck(
                costFn = { state -> autoClickerCost(state.autoClickers) },
                updateFn = { state, cost ->
                    val updated = state.copy(
                        points = state.points - cost,
                        autoClickers = state.autoClickers + 1,
                    )
                    updateQuestProgress(updated, "upgrades", 1L)
                }
            )
            if (success) {
                Log.d(TAG, "buyAutoClicker: Purchase successful")
                startAutoLoop()
            } else {
                Log.w(TAG, "buyAutoClicker: Purchase failed")
            }
        }
    }

    fun buyAutoPower() {
        viewModelScope.launch {
            val success = repository.buyWithCheck(
                costFn = { state -> autoPowerCost(state.autoPower) },
                updateFn = { state, cost ->
                    val updated = state.copy(
                        points = state.points - cost,
                        autoPower = state.autoPower + 1,
                    )
                    updateQuestProgress(updated, "upgrades", 1L)
                }
            )
            if (success) {
                Log.d(TAG, "buyAutoPower: Purchase successful")
            } else {
                Log.w(TAG, "buyAutoPower: Purchase failed")
            }
        }
    }

    fun buyPointsMultiplier() {
        viewModelScope.launch {
            val success = repository.buyWithCheck(
                costFn = { state -> pointsMultiplierCost(state.pointsMultiplier) },
                updateFn = { state, cost ->
                    val updated = state.copy(
                        points = state.points - cost,
                        pointsMultiplier = state.pointsMultiplier + 1,
                    )
                    updateQuestProgress(updated, "upgrades", 1L)
                }
            )
            if (success) {
                Log.d(TAG, "buyPointsMultiplier: Purchase successful")
            } else {
                Log.w(TAG, "buyPointsMultiplier: Purchase failed")
            }
        }
    }

    fun buyAutoClickerSpeed() {
        viewModelScope.launch {
            val success = repository.buyWithCheck(
                costFn = { state -> autoClickerSpeedCost(state.autoClickerSpeed) },
                updateFn = { state, cost ->
                    val updated = state.copy(
                        points = state.points - cost,
                        autoClickerSpeed = state.autoClickerSpeed + 1,
                    )
                    updateQuestProgress(updated, "upgrades", 1L)
                }
            )
            if (success) {
                Log.d(TAG, "buyAutoClickerSpeed: Purchase successful")
                startAutoLoop() // Перезапускаем цикл с новой скоростью
            } else {
                Log.w(TAG, "buyAutoClickerSpeed: Purchase failed")
            }
        }
    }

    fun buyComboBonus() {
        viewModelScope.launch {
            val success = repository.buyWithCheck(
                costFn = { state -> comboBonusCost(state.comboBonus) },
                updateFn = { state, cost ->
                    val updated = state.copy(
                        points = state.points - cost,
                        comboBonus = state.comboBonus + 1,
                    )
                    updateQuestProgress(updated, "upgrades", 1L)
                }
            )
            if (success) {
                Log.d(TAG, "buyComboBonus: Purchase successful")
            } else {
                Log.w(TAG, "buyComboBonus: Purchase failed")
            }
        }
    }

    fun buyOfflineMultiplier() {
        viewModelScope.launch {
            val success = repository.buyWithCheck(
                costFn = { state -> offlineMultiplierCost(state.offlineMultiplier) },
                updateFn = { state, cost ->
                    val updated = state.copy(
                        points = state.points - cost,
                        offlineMultiplier = state.offlineMultiplier + 1,
                    )
                    updateQuestProgress(updated, "upgrades", 1L)
                }
            )
            if (success) {
                Log.d(TAG, "buyOfflineMultiplier: Purchase successful")
            } else {
                Log.w(TAG, "buyOfflineMultiplier: Purchase failed")
            }
        }
    }

    fun buyPremiumUpgrade1() {
        viewModelScope.launch {
            val success = repository.buyWithCheck(
                costFn = { state -> premiumUpgradeCost(state.premiumUpgrade1) },
                updateFn = { state, cost ->
                    val updated = state.copy(
                        points = state.points - cost,
                        premiumUpgrade1 = state.premiumUpgrade1 + 1,
                    )
                    updateQuestProgress(updated, "upgrades", 1L)
                }
            )
            if (success) {
                Log.d(TAG, "buyPremiumUpgrade1: Purchase successful")
            } else {
                Log.w(TAG, "buyPremiumUpgrade1: Purchase failed")
            }
        }
    }

    fun buyPremiumUpgrade2() {
        viewModelScope.launch {
            val success = repository.buyWithCheck(
                costFn = { state -> premiumUpgradeCost(state.premiumUpgrade2) },
                updateFn = { state, cost ->
                    val updated = state.copy(
                        points = state.points - cost,
                        premiumUpgrade2 = state.premiumUpgrade2 + 1,
                    )
                    updateQuestProgress(updated, "upgrades", 1L)
                }
            )
            if (success) {
                Log.d(TAG, "buyPremiumUpgrade2: Purchase successful")
            } else {
                Log.w(TAG, "buyPremiumUpgrade2: Purchase failed")
            }
        }
    }

    // Улучшения для козы
    fun buyGoatPen() {
        viewModelScope.launch {
            val success = repository.buyWithCheck(
                costFn = { state -> goatPenCost(state.goatPenLevel) },
                updateFn = { state, cost ->
                    val updated = state.copy(
                        points = state.points - cost,
                        goatPenLevel = state.goatPenLevel + 1,
                    )
                    updateQuestProgress(updated, "upgrades", 1L)
                }
            )
            if (success) {
                Log.d(TAG, "buyGoatPen: Purchase successful")
            } else {
                Log.w(TAG, "buyGoatPen: Purchase failed")
            }
        }
    }

    fun buyGoatFood() {
        viewModelScope.launch {
            val success = repository.buyWithCheck(
                costFn = { state -> goatFoodCost(state.goatFoodLevel) },
                updateFn = { state, cost ->
                    val updated = state.copy(
                        points = state.points - cost,
                        goatFoodLevel = state.goatFoodLevel + 1,
                    )
                    updateQuestProgress(updated, "upgrades", 1L)
                }
            )
            if (success) {
                Log.d(TAG, "buyGoatFood: Purchase successful")
            } else {
                Log.w(TAG, "buyGoatFood: Purchase failed")
            }
        }
    }

    // Оборудование коморки
    fun buyFridge() {
        viewModelScope.launch {
            val success = repository.buyWithCheck(
                costFn = { state -> fridgeCost(state.fridgeLevel) },
                updateFn = { state, cost ->
                    val updated = state.copy(
                        points = state.points - cost,
                        fridgeLevel = state.fridgeLevel + 1,
                    )
                    updateQuestProgress(updated, "upgrades", 1L)
                }
            )
            if (success) {
                Log.d(TAG, "buyFridge: Purchase successful")
            } else {
                Log.w(TAG, "buyFridge: Purchase failed")
            }
        }
    }

    fun buyPrinter() {
        viewModelScope.launch {
            val success = repository.buyWithCheck(
                costFn = { state -> printerCost(state.printerLevel) },
                updateFn = { state, cost ->
                    val updated = state.copy(
                        points = state.points - cost,
                        printerLevel = state.printerLevel + 1,
                    )
                    updateQuestProgress(updated, "upgrades", 1L)
                }
            )
            if (success) {
                Log.d(TAG, "buyPrinter: Purchase successful")
            } else {
                Log.w(TAG, "buyPrinter: Purchase failed")
            }
        }
    }

    fun buyScanner() {
        viewModelScope.launch {
            val success = repository.buyWithCheck(
                costFn = { state -> scannerCost(state.scannerLevel) },
                updateFn = { state, cost ->
                    val updated = state.copy(
                        points = state.points - cost,
                        scannerLevel = state.scannerLevel + 1,
                    )
                    updateQuestProgress(updated, "upgrades", 1L)
                }
            )
            if (success) {
                Log.d(TAG, "buyScanner: Purchase successful")
            } else {
                Log.w(TAG, "buyScanner: Purchase failed")
            }
        }
    }

    fun buyPrinter3d() {
        viewModelScope.launch {
            val success = repository.buyWithCheck(
                costFn = { state -> printer3dCost(state.printer3dLevel) },
                updateFn = { state, cost ->
                    val updated = state.copy(
                        points = state.points - cost,
                        printer3dLevel = state.printer3dLevel + 1,
                    )
                    updateQuestProgress(updated, "upgrades", 1L)
                }
            )
            if (success) {
                Log.d(TAG, "buyPrinter3d: Purchase successful")
            } else {
                Log.w(TAG, "buyPrinter3d: Purchase failed")
            }
        }
    }

    // Майнинг
    fun buyMiningPower() {
        viewModelScope.launch {
            val success = repository.buyWithCheck(
                costFn = { state -> miningPowerCost(state.miningPower) },
                updateFn = { state, cost ->
                    val updated = state.copy(
                        points = state.points - cost,
                        miningPower = state.miningPower + 1,
                    )
                    updateQuestProgress(updated, "upgrades", 1L)
                }
            )
            if (success) {
                Log.d(TAG, "buyMiningPower: Purchase successful")
            } else {
                Log.w(TAG, "buyMiningPower: Purchase failed")
            }
        }
    }

    fun sellCrypto() {
        viewModelScope.launch {
            repository.updateStateAtomically { current ->
                if (current.cryptoAmount <= 0) {
                    current // Возвращаем без изменений
                } else {
                    val now = System.currentTimeMillis()
                    val eventMultiplier = if (current.activeEventType == "double_day" && now < current.activeEventEndTime) 2 else 1
                    val pointsGain = current.cryptoAmount * 100L * eventMultiplier
                    current.copy(
                        points = current.points + pointsGain,
                        cryptoAmount = 0L,
                        hasSoldCrypto = true,
                    )
                }
            }
        }
    }
    
    // Система бустов
    fun activateBoost(multiplier: Int, durationMinutes: Int) {
        viewModelScope.launch {
            repository.updateStateAtomically { current ->
                val now = System.currentTimeMillis()
                val endTime = now + (durationMinutes * 60 * 1000L)
                current.copy(
                    boostMultiplier = multiplier,
                    boostEndTime = endTime
                )
            }
        }
    }
    
    // Система престижа
    fun performPrestige() {
        viewModelScope.launch {
            repository.updateStateAtomically { current ->
                if (current.points < 1_000_000L) {
                    return@updateStateAtomically current // Нужно минимум 1M для престижа
                }
                // Вычисляем очки престижа: 1 престиж за каждые 1M очков
                val prestigeEarned = current.points / 1_000_000L
                val newPrestigePoints = current.prestigePoints + prestigeEarned
                
                // Сбрасываем прогресс, но сохраняем престиж
                GameStateEntity(
                    id = 0,
                    points = 0L,
                    tapPower = 1,
                    autoClickers = 0,
                    autoPower = 1,
                    totalTaps = 0L,
                    lastSeenEpochMs = current.lastSeenEpochMs,
                    pointsMultiplier = 1,
                    autoClickerSpeed = 1,
                    comboBonus = 0,
                    offlineMultiplier = 1,
                    premiumUpgrade1 = 0,
                    premiumUpgrade2 = 0,
                    lastTapTime = 0L,
                    goatPenLevel = 0,
                    goatFoodLevel = 0,
                    fridgeLevel = 0,
                    printerLevel = 0,
                    scannerLevel = 0,
                    printer3dLevel = 0,
                    cryptoAmount = 0L,
                    miningPower = 0,
                    lastMiningTime = 0L,
                    hasSoldCrypto = false,
                    prestigeLevel = current.prestigeLevel + 1,
                    prestigePoints = newPrestigePoints,
                    boostMultiplier = 1,
                    boostEndTime = 0L,
                    activeQuestType = current.activeQuestType,
                    activeQuestProgress = current.activeQuestProgress,
                    activeQuestTarget = current.activeQuestTarget,
                    activeQuestReward = current.activeQuestReward,
                    lastQuestResetTime = current.lastQuestResetTime,
                    activeEventType = current.activeEventType,
                    activeEventEndTime = current.activeEventEndTime,
                )
            }
        }
    }
    
    // Использовать очки престижа для постоянного бонуса
    fun spendPrestigePoints(amount: Long): Boolean {
        viewModelScope.launch {
            repository.updateStateAtomically { current ->
                if (current.prestigePoints < amount) {
                    current
                } else {
                    // Можно потратить на постоянные бонусы (например, начать с большим tapPower)
                    current.copy(prestigePoints = current.prestigePoints - amount)
                }
            }
        }
        return false
    }
    
    // Система квестов
    fun generateNewQuest() {
        viewModelScope.launch {
            repository.updateStateAtomically { current ->
                val questTypes = listOf("taps", "points", "upgrades")
                val selectedType = questTypes.random()
                val (target, reward) = when (selectedType) {
                    "taps" -> 100L to 1000L
                    "points" -> 10000L to 5000L
                    "upgrades" -> 5L to 2000L
                    else -> 0L to 0L
                }
                current.copy(
                    activeQuestType = selectedType,
                    activeQuestProgress = 0L,
                    activeQuestTarget = target,
                    activeQuestReward = reward,
                    lastQuestResetTime = System.currentTimeMillis()
                )
            }
        }
    }
    
    // Система событий
    fun startEvent(eventType: String, durationMinutes: Int) {
        viewModelScope.launch {
            repository.updateStateAtomically { current ->
                val now = System.currentTimeMillis()
                val endTime = now + (durationMinutes * 60 * 1000L)
                current.copy(
                    activeEventType = eventType,
                    activeEventEndTime = endTime
                )
            }
        }
    }

    private fun startAutoLoop() {
        if (autoJob?.isActive == true) return
        autoJob = viewModelScope.launch {
            while (true) {
                var delayMs = 1000L
                repository.updateStateAtomically { current ->
                    val now = System.currentTimeMillis()
                    val activeBoost = if (now < current.boostEndTime) current.boostMultiplier else 1
                    val eventMultiplier = if (current.activeEventType == "double_day" && now < current.activeEventEndTime) 2 else 1
                    
                    delayMs = 1000L / current.autoClickerSpeed.coerceAtLeast(1)
                    val baseGain = (current.autoClickers * current.autoPower).toLong()
                    val multiplierGain = baseGain * current.pointsMultiplier
                    val boostGain = multiplierGain * activeBoost * eventMultiplier
                    
                    if (boostGain <= 0L) {
                        current
                    } else {
                        current.copy(points = current.points + boostGain, boostMultiplier = activeBoost)
                    }
                }
                delay(delayMs)
            }
        }
    }

    private fun startRoomLoop() {
        if (roomJob?.isActive == true) return
        roomJob = viewModelScope.launch {
            while (true) {
                delay(2000) // Оборудование коморки работает раз в 2 секунды
                repository.updateStateAtomically { current ->
                    val now = System.currentTimeMillis()
                    val activeBoost = if (now < current.boostEndTime) current.boostMultiplier else 1
                    val eventMultiplier = if (current.activeEventType == "double_day" && now < current.activeEventEndTime) 2 else 1
                    
                    val roomIncome = (
                        current.fridgeLevel * 10L +
                        current.printerLevel * 15L +
                        current.scannerLevel * 20L +
                        current.printer3dLevel * 50L
                    ) * activeBoost * eventMultiplier
                    
                    if (roomIncome > 0) {
                        current.copy(points = current.points + roomIncome, boostMultiplier = activeBoost)
                    } else {
                        current
                    }
                }
            }
        }
    }

    private fun startMiningLoop() {
        if (miningJob?.isActive == true) return
        miningJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                repository.updateStateAtomically { current ->
                    if (current.miningPower > 0) {
                        val now = System.currentTimeMillis()
                        val cryptoGain = current.miningPower.toLong()
                        current.copy(
                            cryptoAmount = current.cryptoAmount + cryptoGain,
                            lastMiningTime = now,
                        )
                    } else {
                        current // Возвращаем без изменений
                    }
                }
            }
        }
    }

    private suspend fun checkAndUnlock(state: GameState, unlocked: Set<String>) {
        val now = System.currentTimeMillis()

        fun unlock(id: String) {
            if (id in unlocked) return
            viewModelScope.launch { repository.unlockAchievement(id, now) }
        }

        // Тапы
        if (state.totalTaps >= 1) unlock(AchievementIds.FirstTap)
        if (state.totalTaps >= 100) unlock(AchievementIds.Taps100)
        if (state.totalTaps >= 1_000) unlock(AchievementIds.Taps1k)
        if (state.totalTaps >= 10_000) unlock(AchievementIds.Taps10k)
        if (state.totalTaps >= 100_000) unlock(AchievementIds.Taps100k)
        if (state.totalTaps >= 1_000_000) unlock(AchievementIds.Taps1m)
        
        // Очки
        if (state.points >= 1_000) unlock(AchievementIds.Points1k)
        if (state.points >= 100_000) unlock(AchievementIds.Points100k)
        if (state.points >= 1_000_000) unlock(AchievementIds.Points1m)
        if (state.points >= 10_000_000) unlock(AchievementIds.Points10m)
        if (state.points >= 100_000_000) unlock(AchievementIds.Points100m)
        if (state.points >= 1_000_000_000) unlock(AchievementIds.Points1b)
        
        // Сила тапа
        if (state.tapPower >= 10) unlock(AchievementIds.TapPower10)
        if (state.tapPower >= 50) unlock(AchievementIds.TapPower50)
        if (state.tapPower >= 100) unlock(AchievementIds.TapPower100)
        if (state.tapPower >= 500) unlock(AchievementIds.TapPower500)
        
        // Авто-кликеры
        if (state.autoClickers >= 10) unlock(AchievementIds.AutoClickers10)
        if (state.autoClickers >= 50) unlock(AchievementIds.AutoClickers50)
        if (state.autoClickers >= 100) unlock(AchievementIds.AutoClickers100)
        if (state.autoClickers >= 500) unlock(AchievementIds.AutoClickers500)
        
        // Авто-сила
        if (state.autoPower >= 5) unlock(AchievementIds.AutoPower5)
        if (state.autoPower >= 25) unlock(AchievementIds.AutoPower25)
        if (state.autoPower >= 100) unlock(AchievementIds.AutoPower100)
        
        // Множители
        if (state.pointsMultiplier >= 5) unlock(AchievementIds.Multiplier5x)
        if (state.pointsMultiplier >= 10) unlock(AchievementIds.Multiplier10x)
        if (state.pointsMultiplier >= 50) unlock(AchievementIds.Multiplier50x)
        
        // Скорость авто-кликеров
        if (state.autoClickerSpeed >= 5) unlock(AchievementIds.AutoSpeed5)
        if (state.autoClickerSpeed >= 10) unlock(AchievementIds.AutoSpeed10)
        if (state.autoClickerSpeed >= 20) unlock(AchievementIds.AutoSpeed20)
        
        // Комбо
        if (state.comboBonus >= 5) unlock(AchievementIds.Combo5)
        if (state.comboBonus >= 10) unlock(AchievementIds.Combo10)
        if (state.comboBonus >= 20) unlock(AchievementIds.ComboMaster)
        
        // Офлайн
        if (state.offlineMultiplier >= 5) unlock(AchievementIds.OfflineMultiplier5)
        if (state.offlineMultiplier >= 10) unlock(AchievementIds.OfflineMultiplier10)
        
        // Улучшения козы
        if (state.goatPenLevel >= 5) unlock(AchievementIds.GoatPen5)
        if (state.goatPenLevel >= 10) unlock(AchievementIds.GoatPen10)
        if (state.goatPenLevel >= 20) unlock(AchievementIds.GoatPen20)
        if (state.goatFoodLevel >= 5) unlock(AchievementIds.GoatFood5)
        if (state.goatFoodLevel >= 10) unlock(AchievementIds.GoatFood10)
        if (state.goatFoodLevel >= 20) unlock(AchievementIds.GoatFood20)
        if (state.goatPenLevel >= 10 && state.goatFoodLevel >= 10) unlock(AchievementIds.GoatMaster)
        
        // Коморка
        if (state.fridgeLevel >= 5) unlock(AchievementIds.Fridge5)
        if (state.fridgeLevel >= 10) unlock(AchievementIds.Fridge10)
        if (state.printerLevel >= 5) unlock(AchievementIds.Printer5)
        if (state.printerLevel >= 10) unlock(AchievementIds.Printer10)
        if (state.scannerLevel >= 5) unlock(AchievementIds.Scanner5)
        if (state.scannerLevel >= 10) unlock(AchievementIds.Scanner10)
        if (state.printer3dLevel >= 5) unlock(AchievementIds.Printer3d5)
        if (state.printer3dLevel >= 10) unlock(AchievementIds.Printer3d10)
        if (state.fridgeLevel >= 5 && state.printerLevel >= 5 && 
            state.scannerLevel >= 5 && state.printer3dLevel >= 5) unlock(AchievementIds.RoomMaster)
        
        // Майнинг
        if (state.miningPower >= 5) unlock(AchievementIds.MiningPower5)
        if (state.miningPower >= 10) unlock(AchievementIds.MiningPower10)
        if (state.miningPower >= 50) unlock(AchievementIds.MiningPower50)
        if (state.cryptoAmount >= 1_000) unlock(AchievementIds.Crypto1k)
        if (state.cryptoAmount >= 10_000) unlock(AchievementIds.Crypto10k)
        if (state.cryptoAmount >= 100_000) unlock(AchievementIds.Crypto100k)
        if (state.cryptoAmount >= 1_000_000) unlock(AchievementIds.CryptoMillionaire)
        if (state.hasSoldCrypto) unlock(AchievementIds.CryptoSold)
        
        // Премиум
        if (state.premiumUpgrade1 >= 1) unlock(AchievementIds.Premium1)
        if (state.premiumUpgrade2 >= 1) unlock(AchievementIds.Premium2)
        if (state.premiumUpgrade1 >= 1 && state.premiumUpgrade2 >= 1) unlock(AchievementIds.PremiumBoth)
        
        // Специальные
        if (state.autoClickerSpeed >= 10 && state.autoClickers >= 50) unlock(AchievementIds.SpeedDemon)
        if (state.points >= 1_000_000 && state.totalTaps >= 10_000) unlock(AchievementIds.Millionaire)
        if (state.points >= 1_000_000_000) unlock(AchievementIds.Billionaire)
        if (state.tapPower >= 10 && state.autoClickers >= 10 && 
            state.autoPower >= 10 && state.pointsMultiplier >= 10) unlock(AchievementIds.Perfectionist)
        if (state.goatPenLevel >= 1 && state.goatFoodLevel >= 1 && 
            state.fridgeLevel >= 1 && state.printerLevel >= 1 && 
            state.scannerLevel >= 1 && state.printer3dLevel >= 1 && 
            state.miningPower >= 1) unlock(AchievementIds.Collector)
    }
}

object AchievementIds {
    // Тапы
    const val FirstTap = "first_tap"
    const val Taps100 = "taps_100"
    const val Taps1k = "taps_1k"
    const val Taps10k = "taps_10k"
    const val Taps100k = "taps_100k"
    const val Taps1m = "taps_1m"
    
    // Очки
    const val Points1k = "points_1k"
    const val Points100k = "points_100k"
    const val Points1m = "points_1m"
    const val Points10m = "points_10m"
    const val Points100m = "points_100m"
    const val Points1b = "points_1b"
    
    // Сила тапа
    const val TapPower10 = "tap_power_10"
    const val TapPower50 = "tap_power_50"
    const val TapPower100 = "tap_power_100"
    const val TapPower500 = "tap_power_500"
    
    // Авто-кликеры
    const val AutoClickers10 = "auto_clickers_10"
    const val AutoClickers50 = "auto_clickers_50"
    const val AutoClickers100 = "auto_clickers_100"
    const val AutoClickers500 = "auto_clickers_500"
    
    // Авто-сила
    const val AutoPower5 = "auto_power_5"
    const val AutoPower25 = "auto_power_25"
    const val AutoPower100 = "auto_power_100"
    
    // Множители
    const val Multiplier5x = "multiplier_5x"
    const val Multiplier10x = "multiplier_10x"
    const val Multiplier50x = "multiplier_50x"
    
    // Скорость авто-кликеров
    const val AutoSpeed5 = "auto_speed_5"
    const val AutoSpeed10 = "auto_speed_10"
    const val AutoSpeed20 = "auto_speed_20"
    
    // Комбо
    const val Combo5 = "combo_5"
    const val Combo10 = "combo_10"
    const val ComboMaster = "combo_master"
    
    // Офлайн
    const val OfflineMultiplier5 = "offline_multiplier_5"
    const val OfflineMultiplier10 = "offline_multiplier_10"
    
    // Улучшения козы
    const val GoatPen5 = "goat_pen_5"
    const val GoatPen10 = "goat_pen_10"
    const val GoatPen20 = "goat_pen_20"
    const val GoatFood5 = "goat_food_5"
    const val GoatFood10 = "goat_food_10"
    const val GoatFood20 = "goat_food_20"
    const val GoatMaster = "goat_master" // Загон 10+ и еда 10+
    
    // Коморка
    const val Fridge5 = "fridge_5"
    const val Fridge10 = "fridge_10"
    const val Printer5 = "printer_5"
    const val Printer10 = "printer_10"
    const val Scanner5 = "scanner_5"
    const val Scanner10 = "scanner_10"
    const val Printer3d5 = "printer_3d_5"
    const val Printer3d10 = "printer_3d_10"
    const val RoomMaster = "room_master" // Все оборудование 5+
    
    // Майнинг
    const val MiningPower5 = "mining_power_5"
    const val MiningPower10 = "mining_power_10"
    const val MiningPower50 = "mining_power_50"
    const val Crypto1k = "crypto_1k"
    const val Crypto10k = "crypto_10k"
    const val Crypto100k = "crypto_100k"
    const val CryptoMillionaire = "crypto_millionaire" // 1M крипты
    const val CryptoSold = "crypto_sold" // Продал крипту хотя бы раз
    
    // Премиум
    const val Premium1 = "premium_1"
    const val Premium2 = "premium_2"
    const val PremiumBoth = "premium_both" // Оба премиум улучшения
    
    // Специальные
    const val SpeedDemon = "speed_demon" // Авто-скорость 10+ и авто-кликеры 50+
    const val Millionaire = "millionaire" // 1M очков и 10K тапов
    const val Billionaire = "billionaire" // 1B очков
    const val Perfectionist = "perfectionist" // Все базовые улучшения на 10+
    const val Collector = "collector" // Все виды улучшений куплены хотя бы раз
}

/**
 * Формула стоимости с экспоненциальным ростом после уровня 25.
 * До 25 уровня - линейный рост, после - экспоненциальный.
 */
private fun costFormula(baseCost: Long, linearGrowth: Long, level: Int, exponentialThreshold: Int = 25): Long {
    return if (level < exponentialThreshold) {
        baseCost + linearGrowth * level.toLong()
    } else {
        val linearPart = baseCost + linearGrowth * exponentialThreshold.toLong()
        val exponentialLevel = level - exponentialThreshold
        val multiplier = 1.15.pow(exponentialLevel) // 15% рост за уровень
        (linearPart * multiplier).toLong()
    }
}

private fun Double.pow(exp: Int): Double {
    var result = 1.0
    repeat(exp) { result *= this }
    return result
}

fun tapUpgradeCost(currentTapPower: Int): Long =
    costFormula(10L, 5L, currentTapPower, 25)

fun autoClickerCost(currentAutoClickers: Int): Long {
    return costFormula(50L, 25L, currentAutoClickers, 25)
}

fun autoPowerCost(currentAutoPower: Int): Long =
    costFormula(20L, 10L, currentAutoPower, 25)

fun pointsMultiplierCost(currentLevel: Int): Long {
    // Множитель очков: x2, x3, x5, x10... Стоимость растёт экспоненциально после 20
    return costFormula(200L, 150L, currentLevel, 20)
}

fun autoClickerSpeedCost(currentSpeed: Int): Long {
    // Ускорение авто-кликеров: каждое улучшение удваивает скорость
    return costFormula(300L, 200L, currentSpeed, 20)
}

fun comboBonusCost(currentBonus: Int): Long {
    // Бонус за комбо: увеличивает максимальный множитель комбо
    return costFormula(250L, 150L, currentBonus, 20)
}

fun offlineMultiplierCost(currentMultiplier: Int): Long {
    // Множитель офлайн-дохода: удваивает доход за время вне игры
    return costFormula(400L, 250L, currentMultiplier, 20)
}

fun premiumUpgradeCost(currentLevel: Int): Long {
    // Премиум улучшения: очень дорогие, но мощные (увеличивают все параметры)
    return costFormula(1000L, 500L, currentLevel, 15)
}

// Улучшения для козы
fun goatPenCost(level: Int): Long = costFormula(100L, 50L, level, 30)
fun goatFoodCost(level: Int): Long = costFormula(150L, 75L, level, 30)

// Оборудование коморки
fun fridgeCost(level: Int): Long = costFormula(300L, 150L, level, 25)
fun printerCost(level: Int): Long = costFormula(400L, 200L, level, 25)
fun scannerCost(level: Int): Long = costFormula(500L, 250L, level, 25)
fun printer3dCost(level: Int): Long = costFormula(800L, 400L, level, 20)

// Майнинг
fun miningPowerCost(power: Int): Long = costFormula(600L, 300L, power, 25)
