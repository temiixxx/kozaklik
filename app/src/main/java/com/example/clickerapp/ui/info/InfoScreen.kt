package com.example.clickerapp.ui.info

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.clickerapp.R
import com.example.clickerapp.viewmodel.GameViewModel
import com.example.clickerapp.viewmodel.AchievementIds

@Composable
fun InfoScreen(
    gameViewModel: GameViewModel,
) {
    val state by gameViewModel.state.collectAsStateWithLifecycle()
    val unlocked by gameViewModel.achievements.collectAsStateWithLifecycle()
    var tabIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.info_title),
            style = MaterialTheme.typography.headlineMedium
        )

        TabRow(selectedTabIndex = tabIndex) {
            Tab(
                selected = tabIndex == 0,
                onClick = { tabIndex = 0 },
                text = { Text(stringResource(R.string.info_about)) },
            )
            Tab(
                selected = tabIndex == 1,
                onClick = { tabIndex = 1 },
                text = { Text(stringResource(R.string.info_tutorial)) },
            )
            Tab(
                selected = tabIndex == 2,
                onClick = { tabIndex = 2 },
                text = { Text(stringResource(R.string.info_stats)) },
            )
            Tab(
                selected = tabIndex == 3,
                onClick = { tabIndex = 3 },
                text = { Text(stringResource(R.string.info_achievements)) },
            )
        }

        when (tabIndex) {
            0 -> AboutTab()
            1 -> TutorialTab()
            2 -> StatsTab(
                points = state.points,
                totalTaps = state.totalTaps,
                tapPower = state.tapPower,
                autoClickers = state.autoClickers,
            )
            else -> AchievementsTab(
                unlocked = unlocked,
                state = state,
            )
        }
    }
}

@Composable
private fun AboutTab() {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.info_made_by),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(10.dp))

            val url = "https://kamorka.online/"
            Text(
                text = stringResource(R.string.info_kamorka),
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Открыть сайт в браузере: $url",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .padding(0.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Если кнопка «Только в приложении» открыта — сайт грузится прямо внутри приложения.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            )

            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }
            ) {
                Text("Открыть в браузере")
            }
        }
    }
}

@Composable
private fun TutorialTab() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Правила", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "1) Тапай козу — получай очки.\n" +
                    "2) Прокачивай «Силу тапа» — каждый тап приносит больше.\n" +
                    "3) Покупай авто‑кликеры — они приносят очки сами каждую секунду.\n" +
                    "4) Чем дальше — тем быстрее рост.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun StatsTab(
    points: Long,
    totalTaps: Long,
    tapPower: Int,
    autoClickers: Int,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Статистика игрока", style = MaterialTheme.typography.titleLarge)
            Text("Очки: $points")
            Text("Всего тапов: $totalTaps")
            Text("Сила тапа: $tapPower")
            Text("Авто‑кликеры: $autoClickers")
        }
    }
}

@Composable
private fun AchievementsTab(
    unlocked: Set<String>,
    state: com.example.clickerapp.data.repository.GameState,
) {
    val categories = listOf(
        AchievementCategory(
            "Тапы",
            listOf(
                AchievementDef(AchievementIds.FirstTap, "Первый тап", "Тапни козу хотя бы один раз.", progress = if (state.totalTaps >= 1) 1f else 0f),
                AchievementDef(AchievementIds.Taps100, "Сотка", "Сделай 100 тапов.", progress = (state.totalTaps.coerceAtMost(100).toFloat() / 100f)),
                AchievementDef(AchievementIds.Taps1k, "Тысяча тапов", "Сделай 1 000 тапов.", progress = (state.totalTaps.coerceAtMost(1_000).toFloat() / 1_000f)),
                AchievementDef(AchievementIds.Taps10k, "Десять тысяч", "Сделай 10 000 тапов.", progress = (state.totalTaps.coerceAtMost(10_000).toFloat() / 10_000f)),
                AchievementDef(AchievementIds.Taps100k, "Сто тысяч", "Сделай 100 000 тапов.", progress = (state.totalTaps.coerceAtMost(100_000).toFloat() / 100_000f)),
                AchievementDef(AchievementIds.Taps1m, "Миллионер тапов", "Сделай 1 000 000 тапов!", progress = (state.totalTaps.coerceAtMost(1_000_000).toFloat() / 1_000_000f)),
            )
        ),
        AchievementCategory(
            "Очки",
            listOf(
                AchievementDef(AchievementIds.Points1k, "Тысяча очков", "Набери 1 000 очков.", progress = (state.points.coerceAtMost(1_000).toFloat() / 1_000f)),
                AchievementDef(AchievementIds.Points100k, "Легенда", "Набери 100 000 очков.", progress = (state.points.coerceAtMost(100_000).toFloat() / 100_000f)),
                AchievementDef(AchievementIds.Points1m, "Миллионер", "Набери 1 000 000 очков.", progress = (state.points.coerceAtMost(1_000_000).toFloat() / 1_000_000f)),
                AchievementDef(AchievementIds.Points10m, "Десять миллионов", "Набери 10 000 000 очков.", progress = (state.points.coerceAtMost(10_000_000).toFloat() / 10_000_000f)),
                AchievementDef(AchievementIds.Points100m, "Сто миллионов", "Набери 100 000 000 очков.", progress = (state.points.coerceAtMost(100_000_000).toFloat() / 100_000_000f)),
                AchievementDef(AchievementIds.Points1b, "Миллиардер", "Набери 1 000 000 000 очков!", progress = (state.points.coerceAtMost(1_000_000_000).toFloat() / 1_000_000_000f)),
            )
        ),
        AchievementCategory(
            "Сила тапа",
            listOf(
                AchievementDef(AchievementIds.TapPower10, "Копыто-10", "Прокачай силу тапа до 10.", progress = (state.tapPower.coerceAtMost(10).toFloat() / 10f)),
                AchievementDef(AchievementIds.TapPower50, "Копыто-50", "Прокачай силу тапа до 50.", progress = (state.tapPower.coerceAtMost(50).toFloat() / 50f)),
                AchievementDef(AchievementIds.TapPower100, "Копыто-100", "Прокачай силу тапа до 100.", progress = (state.tapPower.coerceAtMost(100).toFloat() / 100f)),
                AchievementDef(AchievementIds.TapPower500, "Копыто-500", "Прокачай силу тапа до 500!", progress = (state.tapPower.coerceAtMost(500).toFloat() / 500f)),
            )
        ),
        AchievementCategory(
            "Авто-кликеры",
            listOf(
                AchievementDef(AchievementIds.AutoClickers10, "Стадо", "Купи 10 авто‑кликеров.", progress = (state.autoClickers.coerceAtMost(10).toFloat() / 10f)),
                AchievementDef(AchievementIds.AutoClickers50, "Большое стадо", "Купи 50 авто‑кликеров.", progress = (state.autoClickers.coerceAtMost(50).toFloat() / 50f)),
                AchievementDef(AchievementIds.AutoClickers100, "Огромное стадо", "Купи 100 авто‑кликеров.", progress = (state.autoClickers.coerceAtMost(100).toFloat() / 100f)),
                AchievementDef(AchievementIds.AutoClickers500, "Армия коз", "Купи 500 авто‑кликеров!", progress = (state.autoClickers.coerceAtMost(500).toFloat() / 500f)),
            )
        ),
        AchievementCategory(
            "Авто-сила",
            listOf(
                AchievementDef(AchievementIds.AutoPower5, "Авто‑мощь", "Прокачай авто‑силу до 5.", progress = (state.autoPower.coerceAtMost(5).toFloat() / 5f)),
                AchievementDef(AchievementIds.AutoPower25, "Авто‑сила", "Прокачай авто‑силу до 25.", progress = (state.autoPower.coerceAtMost(25).toFloat() / 25f)),
                AchievementDef(AchievementIds.AutoPower100, "Авто‑легенда", "Прокачай авто‑силу до 100!", progress = (state.autoPower.coerceAtMost(100).toFloat() / 100f)),
            )
        ),
        AchievementCategory(
            "Множители",
            listOf(
                AchievementDef(AchievementIds.Multiplier5x, "Множитель x5", "Прокачай множитель очков до x5.", progress = (state.pointsMultiplier.coerceAtMost(5).toFloat() / 5f)),
                AchievementDef(AchievementIds.Multiplier10x, "Множитель x10", "Прокачай множитель очков до x10.", progress = (state.pointsMultiplier.coerceAtMost(10).toFloat() / 10f)),
                AchievementDef(AchievementIds.Multiplier50x, "Множитель x50", "Прокачай множитель очков до x50!", progress = (state.pointsMultiplier.coerceAtMost(50).toFloat() / 50f)),
            )
        ),
        AchievementCategory(
            "Скорость",
            listOf(
                AchievementDef(AchievementIds.AutoSpeed5, "Быстрый", "Прокачай скорость авто‑кликеров до 5.", progress = (state.autoClickerSpeed.coerceAtMost(5).toFloat() / 5f)),
                AchievementDef(AchievementIds.AutoSpeed10, "Очень быстрый", "Прокачай скорость авто‑кликеров до 10.", progress = (state.autoClickerSpeed.coerceAtMost(10).toFloat() / 10f)),
                AchievementDef(AchievementIds.AutoSpeed20, "Молниеносный", "Прокачай скорость авто‑кликеров до 20!", progress = (state.autoClickerSpeed.coerceAtMost(20).toFloat() / 20f)),
            )
        ),
        AchievementCategory(
            "Комбо",
            listOf(
                AchievementDef(AchievementIds.Combo5, "Комбо x5", "Прокачай комбо‑бонус до 5.", progress = (state.comboBonus.coerceAtMost(5).toFloat() / 5f)),
                AchievementDef(AchievementIds.Combo10, "Комбо x10", "Прокачай комбо‑бонус до 10.", progress = (state.comboBonus.coerceAtMost(10).toFloat() / 10f)),
                AchievementDef(AchievementIds.ComboMaster, "Мастер комбо", "Прокачай комбо‑бонус до 20!", progress = (state.comboBonus.coerceAtMost(20).toFloat() / 20f)),
            )
        ),
        AchievementCategory(
            "Офлайн",
            listOf(
                AchievementDef(AchievementIds.OfflineMultiplier5, "Офлайн x5", "Прокачай офлайн‑множитель до 5.", progress = (state.offlineMultiplier.coerceAtMost(5).toFloat() / 5f)),
                AchievementDef(AchievementIds.OfflineMultiplier10, "Офлайн x10", "Прокачай офлайн‑множитель до 10!", progress = (state.offlineMultiplier.coerceAtMost(10).toFloat() / 10f)),
            )
        ),
        AchievementCategory(
            "Улучшения козы",
            listOf(
                AchievementDef(AchievementIds.GoatPen5, "Загон 5", "Прокачай загон до 5 уровня.", progress = (state.goatPenLevel.coerceAtMost(5).toFloat() / 5f)),
                AchievementDef(AchievementIds.GoatPen10, "Загон 10", "Прокачай загон до 10 уровня.", progress = (state.goatPenLevel.coerceAtMost(10).toFloat() / 10f)),
                AchievementDef(AchievementIds.GoatPen20, "Загон 20", "Прокачай загон до 20 уровня!", progress = (state.goatPenLevel.coerceAtMost(20).toFloat() / 20f)),
                AchievementDef(AchievementIds.GoatFood5, "Еда 5", "Прокачай еду до 5 уровня.", progress = (state.goatFoodLevel.coerceAtMost(5).toFloat() / 5f)),
                AchievementDef(AchievementIds.GoatFood10, "Еда 10", "Прокачай еду до 10 уровня.", progress = (state.goatFoodLevel.coerceAtMost(10).toFloat() / 10f)),
                AchievementDef(AchievementIds.GoatFood20, "Еда 20", "Прокачай еду до 20 уровня!", progress = (state.goatFoodLevel.coerceAtMost(20).toFloat() / 20f)),
                AchievementDef(AchievementIds.GoatMaster, "Мастер козы", "Прокачай загон и еду до 10+ уровня!", progress = ((minOf(state.goatPenLevel, 10) + minOf(state.goatFoodLevel, 10)).toFloat() / 20f)),
            )
        ),
        AchievementCategory(
            "Коморка",
            listOf(
                AchievementDef(AchievementIds.Fridge5, "Холодильник 5", "Прокачай холодильник до 5 уровня.", progress = (state.fridgeLevel.coerceAtMost(5).toFloat() / 5f)),
                AchievementDef(AchievementIds.Fridge10, "Холодильник 10", "Прокачай холодильник до 10 уровня!", progress = (state.fridgeLevel.coerceAtMost(10).toFloat() / 10f)),
                AchievementDef(AchievementIds.Printer5, "Принтер 5", "Прокачай принтер до 5 уровня.", progress = (state.printerLevel.coerceAtMost(5).toFloat() / 5f)),
                AchievementDef(AchievementIds.Printer10, "Принтер 10", "Прокачай принтер до 10 уровня!", progress = (state.printerLevel.coerceAtMost(10).toFloat() / 10f)),
                AchievementDef(AchievementIds.Scanner5, "Сканер 5", "Прокачай сканер до 5 уровня.", progress = (state.scannerLevel.coerceAtMost(5).toFloat() / 5f)),
                AchievementDef(AchievementIds.Scanner10, "Сканер 10", "Прокачай сканер до 10 уровня!", progress = (state.scannerLevel.coerceAtMost(10).toFloat() / 10f)),
                AchievementDef(AchievementIds.Printer3d5, "3D принтер 5", "Прокачай 3D принтер до 5 уровня.", progress = (state.printer3dLevel.coerceAtMost(5).toFloat() / 5f)),
                AchievementDef(AchievementIds.Printer3d10, "3D принтер 10", "Прокачай 3D принтер до 10 уровня!", progress = (state.printer3dLevel.coerceAtMost(10).toFloat() / 10f)),
                AchievementDef(AchievementIds.RoomMaster, "Мастер коморки", "Прокачай всё оборудование до 5+ уровня!", progress = ((minOf(state.fridgeLevel, 5) + minOf(state.printerLevel, 5) + minOf(state.scannerLevel, 5) + minOf(state.printer3dLevel, 5)).toFloat() / 20f)),
            )
        ),
        AchievementCategory(
            "Майнинг",
            listOf(
                AchievementDef(AchievementIds.MiningPower5, "Майнинг 5", "Прокачай мощность майнинга до 5.", progress = (state.miningPower.coerceAtMost(5).toFloat() / 5f)),
                AchievementDef(AchievementIds.MiningPower10, "Майнинг 10", "Прокачай мощность майнинга до 10.", progress = (state.miningPower.coerceAtMost(10).toFloat() / 10f)),
                AchievementDef(AchievementIds.MiningPower50, "Майнинг 50", "Прокачай мощность майнинга до 50!", progress = (state.miningPower.coerceAtMost(50).toFloat() / 50f)),
                AchievementDef(AchievementIds.Crypto1k, "1K крипты", "Намайнь 1 000 крипты.", progress = (state.cryptoAmount.coerceAtMost(1_000).toFloat() / 1_000f)),
                AchievementDef(AchievementIds.Crypto10k, "10K крипты", "Намайнь 10 000 крипты.", progress = (state.cryptoAmount.coerceAtMost(10_000).toFloat() / 10_000f)),
                AchievementDef(AchievementIds.Crypto100k, "100K крипты", "Намайнь 100 000 крипты.", progress = (state.cryptoAmount.coerceAtMost(100_000).toFloat() / 100_000f)),
                AchievementDef(AchievementIds.CryptoMillionaire, "Крипто-миллионер", "Намайнь 1 000 000 крипты!", progress = (state.cryptoAmount.coerceAtMost(1_000_000).toFloat() / 1_000_000f)),
                AchievementDef(AchievementIds.CryptoSold, "Продавец", "Продай крипту хотя бы раз.", progress = if (state.hasSoldCrypto) 1f else 0f),
            )
        ),
        AchievementCategory(
            "Премиум",
            listOf(
                AchievementDef(AchievementIds.Premium1, "Премиум 1", "Купи первое премиум улучшение.", progress = if (state.premiumUpgrade1 >= 1) 1f else 0f),
                AchievementDef(AchievementIds.Premium2, "Премиум 2", "Купи второе премиум улучшение.", progress = if (state.premiumUpgrade2 >= 1) 1f else 0f),
                AchievementDef(AchievementIds.PremiumBoth, "Премиум мастер", "Купи оба премиум улучшения!", progress = if (state.premiumUpgrade1 >= 1 && state.premiumUpgrade2 >= 1) 1f else 0f),
            )
        ),
        AchievementCategory(
            "Специальные",
            listOf(
                AchievementDef(AchievementIds.SpeedDemon, "Демон скорости", "Авто‑скорость 10+ и 50+ авто‑кликеров!", progress = ((minOf(state.autoClickerSpeed, 10) + minOf(state.autoClickers, 50)).toFloat() / 60f)),
                AchievementDef(AchievementIds.Millionaire, "Миллионер", "1M очков и 10K тапов!", progress = ((minOf(state.points, 1_000_000).toFloat() / 1_000_000f + minOf(state.totalTaps, 10_000).toFloat() / 10_000f) / 2f)),
                AchievementDef(AchievementIds.Billionaire, "Миллиардер", "Набери 1 миллиард очков!", progress = (state.points.coerceAtMost(1_000_000_000).toFloat() / 1_000_000_000f)),
                AchievementDef(AchievementIds.Perfectionist, "Перфекционист", "Все базовые улучшения на 10+!", progress = ((minOf(state.tapPower, 10) + minOf(state.autoClickers, 10) + minOf(state.autoPower, 10) + minOf(state.pointsMultiplier, 10)).toFloat() / 40f)),
                AchievementDef(AchievementIds.Collector, "Коллекционер", "Купи все виды улучшений хотя бы раз!", progress = ((if (state.goatPenLevel >= 1) 1 else 0) + (if (state.goatFoodLevel >= 1) 1 else 0) + (if (state.fridgeLevel >= 1) 1 else 0) + (if (state.printerLevel >= 1) 1 else 0) + (if (state.scannerLevel >= 1) 1 else 0) + (if (state.printer3dLevel >= 1) 1 else 0) + (if (state.miningPower >= 1) 1 else 0)).toFloat() / 7f),
            )
        ),
    )

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Достижения", style = MaterialTheme.typography.titleLarge)
            val totalAchievements = categories.sumOf { it.achievements.size }
            val unlockedCount = categories.sumOf { cat -> cat.achievements.count { it.id in unlocked } }
            Text(
                "Открыто: $unlockedCount / $totalAchievements",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            
            categories.forEach { category ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        category.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    category.achievements.forEach { def ->
                        val isUnlocked = def.id in unlocked
                        Column(
                            modifier = Modifier.padding(start = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        def.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        def.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(if (isUnlocked) "✓" else "🔒")
                            }
                            if (!isUnlocked) {
                                LinearProgressIndicator(progress = def.progress.coerceIn(0f, 1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class AchievementCategory(
    val name: String,
    val achievements: List<AchievementDef>,
)

private data class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val progress: Float,
)

