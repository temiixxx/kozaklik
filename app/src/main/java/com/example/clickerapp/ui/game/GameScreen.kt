package com.example.clickerapp.ui.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.clickerapp.R
import com.example.clickerapp.util.NumberFormatter
import com.example.clickerapp.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onOpenUpgrades: () -> Unit,
    onOpenRoom: () -> Unit,
    onOpenMining: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPrestigeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Индикаторы бустов и событий
        BoostAndEventIndicators(state = state)
        
        // Карточка активного квеста
        if (state.activeQuestType.isNotEmpty()) {
            QuestCard(state = state)
        }
        Text(
            text = stringResource(R.string.game_title),
            style = MaterialTheme.typography.headlineMedium
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AnimatedContent(
                        targetState = state.points,
                        label = "points",
                    ) { points ->
                        Text(
                            text = "${stringResource(R.string.game_points)}: ${NumberFormatter.format(points)}",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    Button(onClick = onOpenUpgrades) {
                        Text(stringResource(R.string.game_upgrades))
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${stringResource(R.string.game_tap_power)}: ${state.tapPower}")
                    Text("${stringResource(R.string.game_auto_clickers)}: ${state.autoClickers}")
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onOpenRoom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.room_button))
                    }
                    Button(
                        onClick = onOpenMining,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.mining_button))
                    }
                }
                
                // Кнопка престижа
                Spacer(Modifier.height(8.dp))
                PrestigeButton(
                    state = state,
                    onClick = { showPrestigeDialog = true }
                )
            }
        }

        GoatTapCard(
            onTap = { viewModel.tapGoat() },
        )

        Text(
            text = "Тапай козу, прокачивай силу тапа и покупай авто‑кликеры.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        )
    }
    
    // Диалог подтверждения престижа
    if (showPrestigeDialog) {
        PrestigeConfirmDialog(
            state = state,
            onConfirm = {
                viewModel.performPrestige()
                showPrestigeDialog = false
            },
            onDismiss = { showPrestigeDialog = false }
        )
    }
}

@Composable
private fun GoatTapCard(
    onTap: () -> Unit,
) {
    var tapped by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scale by animateFloatAsState(targetValue = if (tapped) 0.96f else 1f, label = "tapScale")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable {
                onTap()
                scope.launch {
                    tapped = true
                    delay(80)
                    tapped = false
                }
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.koza),
                contentDescription = "Goat",
                modifier = Modifier
                    .width(200.dp)
                    .aspectRatio(3f / 4f) // Вертикальная ориентация (3:4)
                    .clip(RoundedCornerShape(20.dp))
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "КОЗА",
                fontSize = 24.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.game_click),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun BoostAndEventIndicators(state: com.example.clickerapp.data.repository.GameState) {
    val now = System.currentTimeMillis()
    val hasActiveBoost = now < state.boostEndTime && state.boostMultiplier > 1
    val hasActiveEvent = now < state.activeEventEndTime && state.activeEventType.isNotEmpty()
    
    if (hasActiveBoost || hasActiveEvent) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (hasActiveBoost) {
                    val timeLeft = ((state.boostEndTime - now) / 1000).toInt()
                    val minutes = timeLeft / 60
                    val seconds = timeLeft % 60
                    Text(
                        text = stringResource(R.string.boost_active, state.boostMultiplier),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(R.string.boost_time_left, String.format("%d:%02d", minutes, seconds)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                if (hasActiveEvent) {
                    if (hasActiveBoost) Spacer(Modifier.height(8.dp))
                    val eventName = when (state.activeEventType) {
                        "double_day" -> stringResource(R.string.event_double_day)
                        "free_upgrades" -> stringResource(R.string.event_free_upgrades)
                        else -> state.activeEventType
                    }
                    val eventTimeLeft = ((state.activeEventEndTime - now) / 1000).toInt()
                    val eventMinutes = eventTimeLeft / 60
                    val eventSeconds = eventTimeLeft % 60
                    Text(
                        text = stringResource(R.string.event_active, eventName),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(R.string.boost_time_left, String.format("%d:%02d", eventMinutes, eventSeconds)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestCard(state: com.example.clickerapp.data.repository.GameState) {
    if (state.activeQuestType.isEmpty() || state.activeQuestTarget <= 0) return
    
    val progress = state.activeQuestProgress.toFloat() / state.activeQuestTarget.toFloat()
    val questDescription = when (state.activeQuestType) {
        "taps" -> stringResource(R.string.quest_taps, NumberFormatter.format(state.activeQuestTarget))
        "points" -> stringResource(R.string.quest_points, NumberFormatter.format(state.activeQuestTarget))
        "upgrades" -> stringResource(R.string.quest_upgrades, NumberFormatter.format(state.activeQuestTarget))
        else -> ""
    }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.quest_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = questDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.quest_progress,
                    NumberFormatter.format(state.activeQuestProgress),
                    NumberFormatter.format(state.activeQuestTarget)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.quest_reward, NumberFormatter.format(state.activeQuestReward)),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PrestigeButton(
    state: com.example.clickerapp.data.repository.GameState,
    onClick: () -> Unit
) {
    val canPrestige = state.points >= 1_000_000L
    val prestigeEarned = if (canPrestige) state.points / 1_000_000L else 0L
    
    Button(
        onClick = onClick,
        enabled = canPrestige,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.prestige_button),
                style = MaterialTheme.typography.titleMedium
            )
            if (canPrestige) {
                Text(
                    text = stringResource(R.string.prestige_earn, NumberFormatter.format(prestigeEarned)),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = stringResource(R.string.prestige_insufficient),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun PrestigeConfirmDialog(
    state: com.example.clickerapp.data.repository.GameState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val prestigeEarned = state.points / 1_000_000L
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.prestige_confirm_title))
        },
        text = {
            Text(stringResource(R.string.prestige_confirm_message, NumberFormatter.format(prestigeEarned)))
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.prestige_confirm_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.prestige_confirm_no))
            }
        }
    )
}
