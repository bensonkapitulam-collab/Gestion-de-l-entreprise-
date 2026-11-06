package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.ShopUiState
import com.example.util.formatCurrency
import com.example.util.formatDisplayDate
import com.example.util.formatDisplayMonth
import com.example.util.todayDateKey
import kotlin.math.abs

@Composable
fun ReportHeaderAndTools(
    title: String,
    onPrint: () -> Unit,
    onExportJpeg: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onPrint) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Imprimer")
            }
            OutlinedButton(onClick = onExportJpeg) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Enregistrer en JPEG")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Maison La Grance divine",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "$title · ${formatDisplayDate(todayDateKey())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun KpiPairRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = leftLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = leftValue,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rightLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = rightValue,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun StockScreen(
    state: ShopUiState,
    onPrint: () -> Unit,
    onExportJpeg: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(state.products, search) {
        if (search.isBlank()) state.products
        else state.products.filter {
            it.name.contains(search, ignoreCase = true) ||
                it.quality.contains(search, ignoreCase = true)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReportHeaderAndTools(title = "Stock", onPrint = onPrint, onExportJpeg = onExportJpeg)

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
                    text = "Stock",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                KpiPairRow(
                    leftLabel = "Produits",
                    leftValue = state.products.size.toString(),
                    rightLabel = "Valeur du stock",
                    rightValue = formatCurrency(state.totalStockValue)
                )
                if (state.lowStockCount > 0) {
                    Text(
                        text = "⚠ ${state.lowStockCount} produit(s) en stock faible (≤ 5)",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
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
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Rechercher un produit") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (filtered.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Stock vide",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    filtered.forEachIndexed { idx, prod ->
                        if (idx > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        }
                        val isLow = prod.qty <= 5
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = prod.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Qualité : ${prod.quality.ifBlank { "—" }} · Achat : ${formatCurrency(prod.cost)} · Vente : ${formatCurrency(prod.price)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (isLow) "${prod.qty} ⚠" else prod.qty.toString(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HonneurScreen(state: ShopUiState) {
    val list = state.clientHonorList
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (list.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "👑 Client d’honneur",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Aucune facture pour l’instant.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val top = list.first()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Client d’honneur",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = top.name,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${formatCurrency(top.totalSpent)} dépensés en ${top.invoiceCount} facture(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Classement",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    list.forEachIndexed { idx, c ->
                        if (idx > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "#${idx + 1}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(36.dp)
                                )
                                Column {
                                    Text(
                                        text = c.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${c.invoiceCount} achat(s)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = formatCurrency(c.totalSpent),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JourScreen(
    state: ShopUiState,
    onShiftDate: (Int) -> Unit,
    onPrint: () -> Unit,
    onExportJpeg: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReportHeaderAndTools(title = "Gain par jour", onPrint = onPrint, onExportJpeg = onExportJpeg)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gain par jour",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onShiftDate(-1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Jour précédent")
                        }
                        Text(
                            text = formatDisplayDate(state.selectedDateKey),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { onShiftDate(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Jour suivant")
                        }
                    }
                }

                KpiPairRow(
                    leftLabel = "Gain net",
                    leftValue = formatCurrency(state.dayProfit),
                    rightLabel = "Chiffre d’affaires",
                    rightValue = formatCurrency(state.dayRevenue)
                )

                Text(
                    text = "${state.dayInvoices.size} facture(s) ce jour",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.dayInvoices.isEmpty()) {
                    Text(
                        text = "Aucune vente ce jour",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.dayInvoices.forEachIndexed { idx, inv ->
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
                                Text(
                                    text = "N°${inv.invoice.id} · ${inv.invoice.client}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "CA : ${formatCurrency(inv.invoice.total)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Gain : ${formatCurrency(inv.invoice.profit)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SemaineScreen(
    state: ShopUiState,
    onPrint: () -> Unit,
    onExportJpeg: () -> Unit
) {
    val rows = state.weekDays
    val maxProfit = remember(rows) {
        rows.maxOfOrNull { abs(it.profit) }?.coerceAtLeast(1.0) ?: 1.0
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReportHeaderAndTools(title = "Gain de la semaine", onPrint = onPrint, onExportJpeg = onExportJpeg)

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
                    text = "Gain de la semaine",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                KpiPairRow(
                    leftLabel = "Gain net",
                    leftValue = formatCurrency(state.weekProfit),
                    rightLabel = "Chiffre d’affaires",
                    rightValue = formatCurrency(state.weekRevenue)
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "7 derniers jours",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                rows.forEach { day ->
                    val fraction by animateFloatAsState(
                        targetValue = (abs(day.profit) / maxProfit).toFloat().coerceIn(0.02f, 1f),
                        label = "bar"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${day.label} · ${day.invoiceCount} facture(s)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCurrency(day.profit),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoisScreen(
    state: ShopUiState,
    onShiftMonth: (Int) -> Unit,
    onPrint: () -> Unit,
    onExportJpeg: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReportHeaderAndTools(title = "Gain du mois", onPrint = onPrint, onExportJpeg = onExportJpeg)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gain du mois",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onShiftMonth(-1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mois précédent")
                        }
                        Text(
                            text = formatDisplayMonth(state.selectedMonthKey),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { onShiftMonth(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mois suivant")
                        }
                    }
                }

                KpiPairRow(
                    leftLabel = "Gain net",
                    leftValue = formatCurrency(state.monthProfit),
                    rightLabel = "Chiffre d’affaires",
                    rightValue = formatCurrency(state.monthRevenue)
                )

                Text(
                    text = "${state.monthInvoices.size} facture(s) · base de données : ${state.invoices.size} factures, ${state.products.size} produits, ${state.arrivals.size} arrivages enregistrés.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.monthDailyBreakdown.isEmpty()) {
                    Text(
                        text = "Aucune vente ce mois-ci",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.monthDailyBreakdown.forEachIndexed { idx, item ->
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
                            Column {
                                Text(
                                    text = "${item.displayDay} · ${item.invoiceCount} fact.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "CA : ${formatCurrency(item.revenue)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Gain : ${formatCurrency(item.profit)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
