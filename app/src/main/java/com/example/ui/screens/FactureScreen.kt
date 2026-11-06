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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Print
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.InvoiceWithLines
import com.example.data.ProductEntity
import com.example.ui.CartLineDraft
import com.example.ui.ShopUiState
import com.example.util.formatCurrency
import com.example.util.formatDisplayDate

@Composable
fun FactureScreen(
    state: ShopUiState,
    onClientNameChange: (String) -> Unit,
    onUpdateLine: (Int, String?, String?, String?) -> Unit,
    onPickProduct: (Int, ProductEntity) -> Unit,
    onAddLine: () -> Unit,
    onRemoveLine: (Int) -> Unit,
    onCancelEdit: () -> Unit,
    onValidateInvoice: () -> Unit,
    onEditInvoice: (InvoiceWithLines) -> Unit,
    onPrintInvoice: () -> Unit,
    onExportInvoiceJpeg: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!state.noticeMessage.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = state.noticeMessage,
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
                if (state.editingInvoiceId != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✏️ Modification de la facture N°${state.editingInvoiceId}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelLarge
                            )
                            TextButton(onClick = onCancelEdit) {
                                Text("Annuler")
                            }
                        }
                    }
                }

                Text(
                    text = "Maison La Grance divine",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Facture N°${state.nextInvoiceNumber} · ${formatDisplayDate(state.editingInvoiceDateKey)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.clientName,
                    onValueChange = onClientNameChange,
                    label = { Text("Nom du client") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("invoice_client_input")
                )

                Spacer(modifier = Modifier.height(4.dp))

                state.cartLines.forEachIndexed { index, line ->
                    CartLineRow(
                        index = index,
                        line = line,
                        products = state.products,
                        onUpdateLine = { q, d, pu -> onUpdateLine(index, q, d, pu) },
                        onPickProduct = { prod -> onPickProduct(index, prod) },
                        onRemove = { onRemoveLine(index) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onAddLine,
                        modifier = Modifier.testTag("add_invoice_line_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ajouter une ligne")
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(state.cartTotal),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("invoice_total_text")
                        )
                    }
                }

                if (state.stockWarnings.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        state.stockWarnings.forEach { warn ->
                            Text(
                                text = "⚠ $warn",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }

                // Signature line
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Column(
                        modifier = Modifier.width(160.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Signature",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onValidateInvoice,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("validate_invoice_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (state.editingInvoiceId != null) {
                                "Enregistrer les modifications"
                            } else {
                                "Valider la facture"
                            }
                        )
                    }

                    IconButton(
                        onClick = onPrintInvoice,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Imprimer la facture",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onExportInvoiceJpeg,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Enregistrer en JPEG",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Dernières factures
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
                    text = "Dernières factures",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val recent = state.invoices.take(20)
                if (recent.isEmpty()) {
                    Text(
                        text = "Aucune facture",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    recent.forEachIndexed { idx, item ->
                        if (idx > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "N°${item.invoice.id} · ${item.invoice.client}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${formatDisplayDate(item.invoice.dateKey)} · ${formatCurrency(item.invoice.total)}${if (item.invoice.notes.isNotEmpty()) " ⚠" else ""}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onEditInvoice(item) },
                                    modifier = Modifier.testTag("edit_invoice_${item.invoice.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Modifier")
                                }
                            }
                            if (item.invoice.notes.isNotEmpty()) {
                                item.invoice.notes.forEach { note ->
                                    Text(
                                        text = "⚠ $note",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartLineRow(
    index: Int,
    line: CartLineDraft,
    products: List<ProductEntity>,
    onUpdateLine: (String?, String?, String?) -> Unit,
    onPickProduct: (ProductEntity) -> Unit,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val matchingProducts = remember(line.designation, products) {
        if (line.designation.isBlank()) {
            products.take(8)
        } else {
            products.filter {
                it.displayName.contains(line.designation, ignoreCase = true)
            }.take(8)
        }
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
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = line.designation,
                        onValueChange = {
                            onUpdateLine(null, it, null)
                            expanded = it.isNotEmpty() && matchingProducts.isNotEmpty()
                        },
                        label = { Text("Désignation") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cart_designation_$index")
                    )
                    DropdownMenu(
                        expanded = expanded && matchingProducts.isNotEmpty(),
                        onDismissRequest = { expanded = false }
                    ) {
                        matchingProducts.forEach { prod ->
                            DropdownMenuItem(
                                text = {
                                    Text("${prod.displayName} — ${formatCurrency(prod.price)} (Stock: ${prod.qty})")
                                },
                                onClick = {
                                    onPickProduct(prod)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = line.qty,
                    onValueChange = { onUpdateLine(it, null, null) },
                    label = { Text("Qté") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("cart_qty_$index")
                )

                OutlinedTextField(
                    value = line.unitPrice,
                    onValueChange = { onUpdateLine(null, null, it) },
                    label = { Text("P.U (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("cart_pu_$index")
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "PT",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(line.lineTotal),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
