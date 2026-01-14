package com.example.clickerapp.ui.room

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
import com.example.clickerapp.viewmodel.fridgeCost
import com.example.clickerapp.viewmodel.printerCost
import com.example.clickerapp.viewmodel.printer3dCost
import com.example.clickerapp.viewmodel.scannerCost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.room_title)) },
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
            Text(
                text = stringResource(R.string.room_description),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val fridgeCost = fridgeCost(state.fridgeLevel)
            EquipmentCard(
                title = stringResource(R.string.room_fridge),
                description = stringResource(R.string.room_fridge_desc, state.fridgeLevel),
                income = "${state.fridgeLevel * 10} очков/2сек",
                subtitle = "${stringResource(R.string.cost)}: ${NumberFormatter.format(fridgeCost)}",
                canBuy = state.points >= fridgeCost,
                onBuy = { viewModel.buyFridge() },
            )

            val printerCost = printerCost(state.printerLevel)
            EquipmentCard(
                title = stringResource(R.string.room_printer),
                description = stringResource(R.string.room_printer_desc, state.printerLevel),
                income = "${state.printerLevel * 15} очков/2сек",
                subtitle = "${stringResource(R.string.cost)}: ${NumberFormatter.format(printerCost)}",
                canBuy = state.points >= printerCost,
                onBuy = { viewModel.buyPrinter() },
            )

            val scannerCost = scannerCost(state.scannerLevel)
            EquipmentCard(
                title = stringResource(R.string.room_scanner),
                description = stringResource(R.string.room_scanner_desc, state.scannerLevel),
                income = "${state.scannerLevel * 20} очков/2сек",
                subtitle = "${stringResource(R.string.cost)}: ${NumberFormatter.format(scannerCost)}",
                canBuy = state.points >= scannerCost,
                onBuy = { viewModel.buyScanner() },
            )

            val printer3dCost = printer3dCost(state.printer3dLevel)
            EquipmentCard(
                title = stringResource(R.string.room_printer3d),
                description = stringResource(R.string.room_printer3d_desc, state.printer3dLevel),
                income = "${state.printer3dLevel * 50} очков/2сек",
                subtitle = "${stringResource(R.string.cost)}: ${NumberFormatter.format(printer3dCost)}",
                canBuy = state.points >= printer3dCost,
                onBuy = { viewModel.buyPrinter3d() },
            )
        }
    }
}

@Composable
private fun EquipmentCard(
    title: String,
    description: String,
    income: String,
    subtitle: String,
    canBuy: Boolean,
    onBuy: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(Modifier.height(4.dp))
            Text("Доход: $income", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = onBuy, enabled = canBuy) {
                    Text("Купить")
                }
            }
        }
    }
}
