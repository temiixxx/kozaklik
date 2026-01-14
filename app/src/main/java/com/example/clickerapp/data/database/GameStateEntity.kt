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
    
    // Престиж система
    val prestigeLevel: Int = 0, // Уровень престижа
    val prestigePoints: Long = 0L, // Очки престижа (накоплены, но не потрачены)
    
    // Временные бусты
    val boostMultiplier: Int = 1, // Текущий множитель буста (1 = нет буста, 2 = x2, и т.д.)
    val boostEndTime: Long = 0L, // Время окончания буста (epoch ms)
    
    // Квесты
    val activeQuestType: String = "", // Тип активного квеста ("", "taps", "points", "upgrades")
    val activeQuestProgress: Long = 0L, // Прогресс активного квеста
    val activeQuestTarget: Long = 0L, // Цель активного квеста
    val activeQuestReward: Long = 0L, // Награда за квест
    val lastQuestResetTime: Long = 0L, // Время последнего сброса квеста
    
    // События
    val activeEventType: String = "", // Тип активного события ("", "double_day", "free_upgrades")
    val activeEventEndTime: Long = 0L, // Время окончания события
)

