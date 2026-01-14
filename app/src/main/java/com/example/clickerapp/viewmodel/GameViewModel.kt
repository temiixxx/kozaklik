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
        repository.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameState(0, 1, 0, 1, 0, 1, 1, 0, 1, 0, 0, 0L, 0, 0, 0, 0, 0, 0, 0L, 0, 0L))

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
            val current = repository.getOrCreate()
            val now = System.currentTimeMillis()
            val timeSinceLastTap = now - current.lastTapTime
            val comboMultiplier = if (timeSinceLastTap < 2000 && current.lastTapTime > 0) {
                // Комбо: если тапнули в течение 2 секунд после предыдущего тапа
                (current.comboBonus + 1).coerceAtMost(10) // Максимум x10 комбо
            } else {
                1
            }
            
            // Улучшения козы: загон даёт базовый бонус, еда - множитель
            val penBonus = 1.0 + (current.goatPenLevel * 0.2) // +20% за уровень загона
            val foodMultiplier = 1.0 + (current.goatFoodLevel * 0.15) // +15% за уровень еды
            
            val baseGain = (current.tapPower * penBonus).toLong()
            val multiplierGain = baseGain * current.pointsMultiplier
            val foodGain = (multiplierGain * foodMultiplier).toLong()
            val comboGain = foodGain * comboMultiplier
            val finalGain = comboGain
            
            val next = current.copy(
                points = current.points + finalGain,
                totalTaps = current.totalTaps + 1,
                lastTapTime = now,
            )
            repository.save(next)
        }
    }

    fun buyTapUpgrade() {
        viewModelScope.launch {
            val success = repository.buyWithCheck(
                costFn = { state -> tapUpgradeCost(state.tapPower) },
                updateFn = { state, cost ->
                    state.copy(
                        points = state.points - cost,
                        tapPower = state.tapPower + 1,
                    )
                }
            )
            if (success) {
                Log.d(TAG, "buyTapUpgrade: Purchase successful")
            } else {
                Log.w(TAG, "buyTapUpgrade: Purchase failed")
            }
        }
    }

    fun buyAutoClicker() {
        viewModelScope.launch {
            val success = repository.buyWithCheck(
                costFn = { state -> autoClickerCost(state.autoClickers) },
                updateFn = { state, cost ->
                    state.copy(
                        points = state.points - cost,
                        autoClickers = state.autoClickers + 1,
                    )
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
                    state.copy(
                        points = state.points - cost,
                        autoPower = state.autoPower + 1,
                    )
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
                    state.copy(
                        points = state.points - cost,
                        pointsMultiplier = state.pointsMultiplier + 1,
                    )
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
                    state.copy(
                        points = state.points - cost,
                        autoClickerSpeed = state.autoClickerSpeed + 1,
                    )
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
                    state.copy(
                        points = state.points - cost,
                        comboBonus = state.comboBonus + 1,
                    )
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
                    state.copy(
                        points = state.points - cost,
                        offlineMultiplier = state.offlineMultiplier + 1,
                    )
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
                    state.copy(
                        points = state.points - cost,
                        premiumUpgrade1 = state.premiumUpgrade1 + 1,
                    )
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
                    state.copy(
                        points = state.points - cost,
                        premiumUpgrade2 = state.premiumUpgrade2 + 1,
                    )
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
                    state.copy(
                        points = state.points - cost,
                        goatPenLevel = state.goatPenLevel + 1,
                    )
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
                    state.copy(
                        points = state.points - cost,
                        goatFoodLevel = state.goatFoodLevel + 1,
                    )
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
                    state.copy(
                        points = state.points - cost,
                        fridgeLevel = state.fridgeLevel + 1,
                    )
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
                    state.copy(
                        points = state.points - cost,
                        printerLevel = state.printerLevel + 1,
                    )
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
                    state.copy(
                        points = state.points - cost,
                        scannerLevel = state.scannerLevel + 1,
                    )
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
                    state.copy(
                        points = state.points - cost,
                        printer3dLevel = state.printer3dLevel + 1,
                    )
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
                    state.copy(
                        points = state.points - cost,
                        miningPower = state.miningPower + 1,
                    )
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
            val current = repository.getOrCreate()
            if (current.cryptoAmount <= 0) return@launch
            // Продаём крипту по курсу: 1 крипта = 100 очков
            val pointsGain = current.cryptoAmount * 100L
            val next = current.copy(
                points = current.points + pointsGain,
                cryptoAmount = 0L,
                hasSoldCrypto = true,
            )
            repository.save(next)
        }
    }

    private fun startAutoLoop() {
        if (autoJob?.isActive == true) return
        autoJob = viewModelScope.launch {
            while (true) {
                val current = repository.getOrCreate()
                val delayMs = 1000L / current.autoClickerSpeed.coerceAtLeast(1)
                delay(delayMs)
                val baseGain = (current.autoClickers * current.autoPower).toLong()
                val multiplierGain = baseGain * current.pointsMultiplier
                if (multiplierGain <= 0L) continue
                repository.save(current.copy(points = current.points + multiplierGain))
            }
        }
    }

    private fun startRoomLoop() {
        if (roomJob?.isActive == true) return
        roomJob = viewModelScope.launch {
            while (true) {
                delay(2000) // Оборудование коморки работает раз в 2 секунды
                val current = repository.getOrCreate()
                val roomIncome = (
                    current.fridgeLevel * 10L +
                    current.printerLevel * 15L +
                    current.scannerLevel * 20L +
                    current.printer3dLevel * 50L
                )
                if (roomIncome > 0) {
                    repository.save(current.copy(points = current.points + roomIncome))
                }
            }
        }
    }

    private fun startMiningLoop() {
        if (miningJob?.isActive == true) return
        miningJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = repository.getOrCreate()
                if (current.miningPower > 0) {
                    val now = System.currentTimeMillis()
                    val cryptoGain = current.miningPower.toLong()
                    repository.save(current.copy(
                        cryptoAmount = current.cryptoAmount + cryptoGain,
                        lastMiningTime = now,
                    ))
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

fun tapUpgradeCost(currentTapPower: Int): Long =
    (30L * currentTapPower.toLong() * currentTapPower.toLong()).coerceAtLeast(30L)

fun autoClickerCost(currentAutoClickers: Int): Long {
    val n = (currentAutoClickers + 1L)
    return 150L * n * n
}

fun autoPowerCost(currentAutoPower: Int): Long =
    (500L * currentAutoPower.toLong() * currentAutoPower.toLong()).coerceAtLeast(500L)

fun pointsMultiplierCost(currentLevel: Int): Long {
    // Множитель очков: x2, x3, x5, x10... Стоимость растёт экспоненциально
    val multiplier = when (currentLevel) {
        0 -> 2
        1 -> 3
        2 -> 5
        else -> 10
    }
    return (1000L * multiplier * (currentLevel + 1) * (currentLevel + 1))
}

fun autoClickerSpeedCost(currentSpeed: Int): Long {
    // Ускорение авто-кликеров: каждое улучшение удваивает скорость
    return (2000L * (currentSpeed + 1) * (currentSpeed + 1) * (currentSpeed + 1))
}

fun comboBonusCost(currentBonus: Int): Long {
    // Бонус за комбо: увеличивает максимальный множитель комбо
    return (1500L * (currentBonus + 1) * (currentBonus + 1))
}

fun offlineMultiplierCost(currentMultiplier: Int): Long {
    // Множитель офлайн-дохода: удваивает доход за время вне игры
    return (3000L * (currentMultiplier + 1) * (currentMultiplier + 1) * (currentMultiplier + 1))
}

fun premiumUpgradeCost(currentLevel: Int): Long {
    // Премиум улучшения: очень дорогие, но мощные (увеличивают все параметры)
    return (10000L * (currentLevel + 1) * (currentLevel + 1) * (currentLevel + 1) * (currentLevel + 1))
}

// Улучшения для козы
fun goatPenCost(level: Int): Long = (500L * (level + 1) * (level + 1))
fun goatFoodCost(level: Int): Long = (800L * (level + 1) * (level + 1))

// Оборудование коморки
fun fridgeCost(level: Int): Long = (2000L * (level + 1) * (level + 1))
fun printerCost(level: Int): Long = (3000L * (level + 1) * (level + 1))
fun scannerCost(level: Int): Long = (4000L * (level + 1) * (level + 1))
fun printer3dCost(level: Int): Long = (10000L * (level + 1) * (level + 1))

// Майнинг
fun miningPowerCost(power: Int): Long = (5000L * (power + 1) * (power + 1) * (power + 1))
