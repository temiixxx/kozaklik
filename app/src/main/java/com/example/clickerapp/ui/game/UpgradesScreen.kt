package com.example.clickerapp.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.clickerapp.R
import com.example.clickerapp.util.NumberFormatter
import com.example.clickerapp.viewmodel.GameViewModel
import com.example.clickerapp.viewmodel.autoClickerCost
import com.example.clickerapp.viewmodel.autoClickerSpeedCost
import com.example.clickerapp.viewmodel.autoPowerCost
import com.example.clickerapp.viewmodel.comboBonusCost
import com.example.clickerapp.viewmodel.offlineMultiplierCost
import com.example.clickerapp.viewmodel.pointsMultiplierCost
import com.example.clickerapp.viewmodel.premiumUpgradeCost
import com.example.clickerapp.viewmodel.tapUpgradeCost
import com.example.clickerapp.viewmodel.goatPenCost
import com.example.clickerapp.viewmodel.goatFoodCost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradesScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val tapCost = tapUpgradeCost(state.tapPower)
    val autoCost = autoClickerCost(state.autoClickers)
    val autoPowerCost = autoPowerCost(state.autoPower)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.upgrades_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Базовые улучшения
            CategoryHeader(stringResource(R.string.upgrade_category_basic))
            val tapCost = tapUpgradeCost(state.tapPower)
            UpgradeCard(
                title = stringResource(R.string.upgrade_tap_power),
                description = stringResource(R.string.upgrade_level, state.tapPower),
                subtitle = "${stringResource(R.string.cost)}: ${NumberFormatter.format(tapCost)}",
                canBuy = state.points >= tapCost,
                onBuy = { viewModel.buyTapUpgrade() },
            )
            val autoCost = autoClickerCost(state.autoClickers)
            UpgradeCard(
                title = stringResource(R.string.upgrade_auto_clicker),
                description = "${stringResource(R.string.upgrade_level, state.autoClickers)}",
                subtitle = "${stringResource(R.string.cost)}: ${NumberFormatter.format(autoCost)}",
                canBuy = state.points >= autoCost,
                onBuy = { viewModel.buyAutoClicker() },
            )
            val autoPowerCostVal = autoPowerCost(state.autoPower)
            UpgradeCard(
                title = stringResource(R.string.upgrade_auto_power),
                description = stringResource(R.string.upgrade_level, state.autoPower),
                subtitle = "${stringResource(R.string.cost)}: ${NumberFormatter.format(autoPowerCostVal)}",
                canBuy = state.points >= autoPowerCostVal,
                onBuy = { viewModel.buyAutoPower() },
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Улучшения для козы
            CategoryHeader(stringResource(R.string.upgrade_category_goat))
            
            val penCost = goatPenCost(state.goatPenLevel)
            UpgradeCard(
                title = stringResource(R.string.upgrade_goat_pen),
                description = stringResource(R.string.upgrade_goat_pen_desc, state.goatPenLevel + 1),
                subtitle = "${stringResource(R.string.cost)}: ${NumberFormatter.format(penCost)}",
                canBuy = state.points >= penCost,
                onBuy = { viewModel.buyGoatPen() },
            )

            val foodCost = goatFoodCost(state.goatFoodLevel)
            UpgradeCard(
                title = stringResource(R.string.upgrade_goat_food),
                description = stringResource(R.string.upgrade_goat_food_desc, state.goatFoodLevel + 1),
                subtitle = "${stringResource(R.string.cost)}: ${NumberFormatter.format(foodCost)}",
                canBuy = state.points >= foodCost,
                onBuy = { viewModel.buyGoatFood() },
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Множители
            CategoryHeader(stringResource(R.string.upgrade_category_multipliers))
            
            val pointsMultCost = pointsMultiplierCost(state.pointsMultiplier - 1)
            val pointsMultValue = when (state.pointsMultiplier) {
                1 -> 2
                2 -> 3
                3 -> 5
                else -> 10
            }
            UpgradeCard(
                title = stringResource(R.string.upgrade_points_multiplier),
                description = stringResource(R.string.upgrade_points_multiplier_desc, pointsMultValue),
                subtitle = "${stringResource(R.string.cost)}: ${NumberFormatter.format(pointsMultCost)}",
                canBuy = state.points >= pointsMultCost,
                onBuy = { viewModel.buyPointsMultiplier() },
            )

            val autoSpeedCost = autoClickerSpeedCost(state.autoClickerSpeed - 1)
            UpgradeCard(
                title = stringResource(R.string.upgrade_auto_speed),
                description = stringResource(R.string.upgrade_auto_speed_desc, state.autoClickerSpeed),
                subtitle = "${stringResource(R.string.cost)}: ${NumberFormatter.format(autoSpeedCost)}",
                canBuy = state.points >= autoSpeedCost,
                onBuy = { viewModel.buyAutoClickerSpeed() },
            )

            val comboCost = comboBonusCost(state.comboBonus)
            val maxCombo = (state.comboBonus + 1).coerceAtMost(10)
            UpgradeCard(
                title = stringResource(R.string.upgrade_combo_bonus),
                description = stringResource(R.string.upgrade_combo_bonus_desc, maxCombo),
                subtitle = "${stringResource(R.string.cost)}: ${NumberFormatter.format(comboCost)}",
                canBuy = state.points >= comboCost,
                onBuy = { viewModel.buyComboBonus() },
            )

            val offlineMultCost = offlineMultiplierCost(state.offlineMultiplier - 1)
            UpgradeCard(
                title = stringResource(R.string.upgrade_offline_multiplier),
                description = stringResource(R.string.upgrade_offline_multiplier_desc, state.offlineMultiplier),
                subtitle = "${stringResource(R.string.cost)}: ${NumberFormatter.format(offlineMultCost)}",
                canBuy = state.points >= offlineMultCost,
                onBuy = { viewModel.buyOfflineMultiplier() },
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Премиум
            CategoryHeader(stringResource(R.string.upgrade_category_premium))
            
            val premium1Cost = premiumUpgradeCost(state.premiumUpgrade1)
            val premium1Bonus = state.premiumUpgrade1 * 10
            UpgradeCard(
                title = stringResource(R.string.upgrade_premium_1),
                description = stringResource(R.string.upgrade_premium_1_desc, premium1Bonus),
                subtitle = "${stringResource(R.string.cost)}: ${NumberFormatter.format(premium1Cost)}",
                canBuy = state.points >= premium1Cost,
                onBuy = { viewModel.buyPremiumUpgrade1() },
            )

            val premium2Cost = premiumUpgradeCost(state.premiumUpgrade2)
            val premium2Bonus = state.premiumUpgrade2 * 15
            UpgradeCard(
                title = stringResource(R.string.upgrade_premium_2),
                description = stringResource(R.string.upgrade_premium_2_desc, premium2Bonus),
                subtitle = "${stringResource(R.string.cost)}: ${NumberFormatter.format(premium2Cost)}",
                canBuy = state.points >= premium2Cost,
                onBuy = { viewModel.buyPremiumUpgrade2() },
            )
        }
    }
}

@Composable
private fun CategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun UpgradeCard(
    title: String,
    description: String,
    subtitle: String,
    canBuy: Boolean,
    onBuy: () -> Unit,
) {
    Card(
        modifier = Modifier.animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedVisibility(visible = !canBuy) {
                    Text(
                        text = stringResource(R.string.not_enough_points),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(end = 12.dp),
                    )
                }
                Button(
                    onClick = onBuy,
                    enabled = canBuy,
                ) {
                    Text("Купить")
                }
            }
        }
    }
}

