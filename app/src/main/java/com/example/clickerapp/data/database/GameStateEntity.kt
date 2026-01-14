package com.example.clickerapp.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_state")
data class GameStateEntity(
    @PrimaryKey val id: Int = 0,
    val points: Long = 0L,
    val tapPower: Int = 1,
    val autoClickers: Int = 0,
    val autoPower: Int = 1,
    val totalTaps: Long = 0L,
    val lastSeenEpochMs: Long = 0L,
    // Новые улучшения
    val pointsMultiplier: Int = 1, // Множитель очков (x2, x3, x5...)
    val autoClickerSpeed: Int = 1, // Скорость авто-кликеров (1 = раз в секунду, 2 = раз в 0.5 сек...)
    val comboBonus: Int = 0, // Бонус за комбо (дополнительные очки за цепочку тапов)
    val offlineMultiplier: Int = 1, // Множитель офлайн-дохода
    val premiumUpgrade1: Int = 0, // Премиум улучшение 1 (уровень)
    val premiumUpgrade2: Int = 0, // Премиум улучшение 2 (уровень)
    val lastTapTime: Long = 0L, // Время последнего тапа для комбо
    // Улучшения для козы
    val goatPenLevel: Int = 0, // Уровень загона для козы (увеличивает базовый доход)
    val goatFoodLevel: Int = 0, // Уровень еды для козы (множитель очков)
    // Оборудование коморки
    val fridgeLevel: Int = 0, // Холодильник (пассивный доход)
    val printerLevel: Int = 0, // Принтер (пассивный доход)
    val scannerLevel: Int = 0, // Сканер (пассивный доход)
    val printer3dLevel: Int = 0, // 3D принтер (пассивный доход)
    // Майнинг
    val cryptoAmount: Long = 0L, // Количество крипты
    val miningPower: Int = 0, // Мощность майнинга (крипта в секунду)
    val lastMiningTime: Long = 0L, // Время последнего майнинга
    val hasSoldCrypto: Boolean = false, // Флаг продажи крипты (для достижения)
)

