package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.ArrivalLineDraft
import com.example.ui.ShopUiState
import com.example.util.formatCurrency
import com.example.util.formatDisplayDate

@Composable
fun ArrivageScreen(
    state: ShopUiState,
    onUpdateLine: (Int, String?, String?, String?, String?, String?) -> Unit,
    onAddLine: () -> Unit,
    onRemoveLine: (Int) -> Unit,
    onSaveArrivals: () -> Unit
) {
    val existingNames = remember(state.products) {
        state.products.map { it.name }.distinct()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!state.arrivalNotice.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = state.arrivalNotice,
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Nouvel arrivage",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                state.arrivalLines.forEachIndexed { index, row ->
                    ArrivalLineCard(
                        index = index,
                        row = row,
                        existingNames = existingNames,
                        onUpdate = { n, ql, q, c, p -> onUpdateLine(index, n, ql, q, c, p) },
                        onRemove = { onRemoveLine(index) }
                    )
                }

                OutlinedButton(
                    onClick = onAddLine,
                    modifier = Modifier.testTag("add_arrival_line_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajouter une ligne")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Total achat",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(state.totalArrivalCost),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total vente",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(state.totalArrivalPrice),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (!state.arrivalError.isNullOrBlank()) {
                    Text(
                        text = state.arrivalError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Button(
                    onClick = onSaveArrivals,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_arrival_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Enregistrer l’arrivage")
                }
            }
        }

        // Derniers arrivages
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Derniers arrivages",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val recent = state.arrivals.take(8)
                if (recent.isEmpty()) {
                    Text(
                        text = "Aucun arrivage",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    recent.forEachIndexed { idx, arr ->
                        if (idx > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val title = if (arr.quality.isNotBlank()) {
                                    "${arr.productName} (${arr.quality})"
                                } else {
                                    arr.productName
                                }
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = formatDisplayDate(arr.dateKey),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "+${arr.qty}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = formatCurrency(arr.unitCost),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArrivalLineCard(
    index: Int,
    row: ArrivalLineDraft,
    existingNames: List<String>,
    onUpdate: (String?, String?, String?, String?, String?) -> Unit,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val filteredNames = remember(row.name, existingNames) {
        if (row.name.isBlank()) existingNames.take(6)
        else existingNames.filter { it.contains(row.name, ignoreCase = true) }.take(6)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1.2f)) {
                    OutlinedTextField(
                        value = row.name,
                        onValueChange = {
                            onUpdate(it, null, null, null, null)
                            expanded = it.isNotEmpty() && filteredNames.isNotEmpty()
                        },
                        label = { Text("Nom du produit") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("arrival_name_$index")
                    )
                    DropdownMenu(
                        expanded = expanded && filteredNames.isNotEmpty(),
                        onDismissRequest = { expanded = false }
                    ) {
                        filteredNames.forEach { n ->
                            DropdownMenuItem(
                                text = { Text(n) },
                                onClick = {
                                    onUpdate(n, null, null, null, null)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = row.quality,
                    onValueChange = { onUpdate(null, it, null, null, null) },
                    label = { Text("Qualité") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("arrival_quality_$index")
                )

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Supprimer la ligne",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = row.qty,
                    onValueChange = { onUpdate(null, null, it, null, null) },
                    label = { Text("Qté") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("arrival_qty_$index")
                )
                OutlinedTextField(
                    value = row.unitCost,
                    onValueChange = { onUpdate(null, null, null, it, null) },
                    label = { Text("P. Achat") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("arrival_cost_$index")
                )
                OutlinedTextField(
                    value = row.unitPrice,
                    onValueChange = { onUpdate(null, null, null, null, it) },
                    label = { Text("P. Vente") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("arrival_price_$index")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Achat total : ${formatCurrency(row.totalCost)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Vente totale : ${formatCurrency(row.totalPrice)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
