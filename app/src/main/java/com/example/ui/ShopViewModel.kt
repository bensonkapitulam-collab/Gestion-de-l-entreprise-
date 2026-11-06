package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ArrivalEntity
import com.example.data.ArrivalInput
import com.example.data.InvoiceLineInput
import com.example.data.InvoiceWithLines
import com.example.data.ProductEntity
import com.example.data.ShopRepository
import com.example.util.ReportTableData
import com.example.util.currentMonthKey
import com.example.util.formatCurrency
import com.example.util.formatDisplayDate
import com.example.util.formatDisplayMonth
import com.example.util.last7DaysKeysAndLabels
import com.example.util.shiftDateKey
import com.example.util.shiftMonthKey
import com.example.util.todayDateKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class ShopTab(val label: String) {
    FACTURE("Facture"),
    ARRIVAGE("Nouvel arrivage"),
    STOCK("Stock"),
    HONNEUR("Client d’honneur"),
    JOUR("Gain par jour"),
    SEMAINE("Gain de la semaine"),
    MOIS("Gain du mois")
}

data class CartLineDraft(
    val uid: String = UUID.randomUUID().toString(),
    val qty: String = "",
    val designation: String = "",
    val unitPrice: String = ""
) {
    val lineTotal: Double
        get() = (qty.toIntOrNull() ?: 0) * (unitPrice.replace(',', '.').toDoubleOrNull() ?: 0.0)
}

data class ArrivalLineDraft(
    val uid: String = UUID.randomUUID().toString(),
    val name: String = "",
    val quality: String = "",
    val qty: String = "",
    val unitCost: String = "",
    val unitPrice: String = ""
) {
    val totalCost: Double
        get() = (qty.toIntOrNull() ?: 0) * (unitCost.replace(',', '.').toDoubleOrNull() ?: 0.0)
    val totalPrice: Double
        get() = (qty.toIntOrNull() ?: 0) * (unitPrice.replace(',', '.').toDoubleOrNull() ?: 0.0)
}

data class ClientHonorSummary(
    val name: String,
    val totalSpent: Double,
    val invoiceCount: Int
)

data class DayBarSummary(
    val dateKey: String,
    val label: String,
    val profit: Double,
    val revenue: Double,
    val invoiceCount: Int
)

data class MonthDaySummary(
    val dateKey: String,
    val displayDay: String,
    val invoiceCount: Int,
    val revenue: Double,
    val profit: Double
)

data class ShopUiState(
    val isLocked: Boolean = true,
    val hasPassword: Boolean = false,
    val lockError: String? = null,
    val selectedTab: ShopTab = ShopTab.FACTURE,
    val products: List<ProductEntity> = emptyList(),
    val invoices: List<InvoiceWithLines> = emptyList(),
    val arrivals: List<ArrivalEntity> = emptyList(),
    // Facture
    val editingInvoiceId: Long? = null,
    val clientName: String = "",
    val cartLines: List<CartLineDraft> = List(5) { CartLineDraft() },
    val stockWarnings: List<String> = emptyList(),
    val noticeMessage: String? = null,
    val alertMessage: String? = null,
    // Arrivage
    val arrivalLines: List<ArrivalLineDraft> = List(3) { ArrivalLineDraft() },
    val arrivalError: String? = null,
    val arrivalNotice: String? = null,
    // Reports
    val selectedDateKey: String = todayDateKey(),
    val selectedMonthKey: String = currentMonthKey()
) {
    val cartTotal: Double
        get() = cartLines.sumOf { it.lineTotal }

    val nextInvoiceNumber: Long
        get() = editingInvoiceId ?: ((invoices.maxOfOrNull { it.invoice.id } ?: 0L) + 1L)

    val editingInvoiceDateKey: String
        get() = invoices.find { it.invoice.id == editingInvoiceId }?.invoice?.dateKey ?: todayDateKey()

    val totalArrivalCost: Double
        get() = arrivalLines.sumOf { it.totalCost }

    val totalArrivalPrice: Double
        get() = arrivalLines.sumOf { it.totalPrice }

    val totalStockValue: Double
        get() = products.sumOf { it.qty * it.cost }

    val lowStockCount: Int
        get() = products.count { it.qty <= 5 }

    val clientHonorList: List<ClientHonorSummary>
        get() {
            val map = linkedMapOf<String, ClientHonorSummary>()
            for (inv in invoices) {
                val rawName = inv.invoice.client.trim()
                val key = rawName.lowercase()
                val current = map[key]
                if (current == null) {
                    map[key] = ClientHonorSummary(rawName, inv.invoice.total, 1)
                } else {
                    map[key] = current.copy(
                        totalSpent = current.totalSpent + inv.invoice.total,
                        invoiceCount = current.invoiceCount + 1
                    )
                }
            }
            return map.values.sortedByDescending { it.totalSpent }.take(10)
        }

    val dayInvoices: List<InvoiceWithLines>
        get() = invoices.filter { it.invoice.dateKey == selectedDateKey }

    val dayProfit: Double
        get() = dayInvoices.sumOf { it.invoice.profit }

    val dayRevenue: Double
        get() = dayInvoices.sumOf { it.invoice.total }

    val weekDays: List<DayBarSummary>
        get() = last7DaysKeysAndLabels().map { (dKey, label) ->
            val dayInvs = invoices.filter { it.invoice.dateKey == dKey }
            DayBarSummary(
                dateKey = dKey,
                label = label,
                profit = dayInvs.sumOf { it.invoice.profit },
                revenue = dayInvs.sumOf { it.invoice.total },
                invoiceCount = dayInvs.size
            )
        }

    val weekProfit: Double
        get() = weekDays.sumOf { it.profit }

    val weekRevenue: Double
        get() = weekDays.sumOf { it.revenue }

    val monthInvoices: List<InvoiceWithLines>
        get() = invoices.filter { it.invoice.dateKey.startsWith(selectedMonthKey) }

    val monthProfit: Double
        get() = monthInvoices.sumOf { it.invoice.profit }

    val monthRevenue: Double
        get() = monthInvoices.sumOf { it.invoice.total }

    val monthDailyBreakdown: List<MonthDaySummary>
        get() {
            val grouped = monthInvoices.groupBy { it.invoice.dateKey }
            return grouped.keys.sorted().map { dKey ->
                val list = grouped[dKey].orEmpty()
                val parts = dKey.split("-")
                val dayLabel = if (parts.size == 3) "${parts[2]}/${parts[1]}" else dKey
                MonthDaySummary(
                    dateKey = dKey,
                    displayDay = dayLabel,
                    invoiceCount = list.size,
                    revenue = list.sumOf { it.invoice.total },
                    profit = list.sumOf { it.invoice.profit }
                )
            }
        }
}

class ShopViewModel(private val repository: ShopRepository) : ViewModel() {

    private val _localState = MutableStateFlow(ShopUiState())

    val uiState: StateFlow<ShopUiState> = combine(
        _localState,
        repository.hasPassword,
        repository.products,
        repository.invoices,
        repository.arrivals
    ) { local, hasPw, products, invoices, arrivals ->
        val warnings = computeStockWarnings(
            cartLines = local.cartLines,
            products = products,
            editingInvoice = invoices.find { it.invoice.id == local.editingInvoiceId }
        )
        local.copy(
            hasPassword = hasPw,
            products = products,
            invoices = invoices,
            arrivals = arrivals,
            stockWarnings = warnings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ShopUiState()
    )

    fun createPassword(password: String, confirm: String) {
        if (password.length < 6) {
            _localState.update { it.copy(lockError = "6 caractères minimum.") }
            return
        }
        if (password != confirm) {
            _localState.update { it.copy(lockError = "Les mots de passe diffèrent.") }
            return
        }
        viewModelScope.launch {
            repository.setPassword(password)
            _localState.update { it.copy(isLocked = false, lockError = null) }
        }
    }

    fun unlock(password: String) {
        viewModelScope.launch {
            val ok = repository.verifyPassword(password)
            if (ok) {
                _localState.update { it.copy(isLocked = false, lockError = null) }
            } else {
                _localState.update { it.copy(lockError = "Mot de passe incorrect.") }
            }
        }
    }

    fun lockApp() {
        _localState.update {
            it.copy(
                isLocked = true,
                lockError = null,
                cartLines = List(5) { CartLineDraft() },
                clientName = "",
                editingInvoiceId = null,
                noticeMessage = null
            )
        }
    }

    fun selectTab(tab: ShopTab) {
        _localState.update {
            it.copy(
                selectedTab = tab,
                noticeMessage = null,
                arrivalError = null,
                arrivalNotice = null
            )
        }
    }

    fun dismissAlert() {
        _localState.update { it.copy(alertMessage = null) }
    }

    // ---------- Facture ----------
    fun updateClientName(name: String) {
        _localState.update { it.copy(clientName = name) }
    }

    fun updateCartLine(index: Int, qty: String? = null, designation: String? = null, unitPrice: String? = null) {
        _localState.update { state ->
            val updated = state.cartLines.toMutableList()
            if (index in updated.indices) {
                val cur = updated[index]
                updated[index] = cur.copy(
                    qty = qty ?: cur.qty,
                    designation = designation ?: cur.designation,
                    unitPrice = unitPrice ?: cur.unitPrice
                )
            }
            state.copy(cartLines = updated)
        }
    }

    fun pickProductForCartLine(index: Int, product: ProductEntity) {
        _localState.update { state ->
            val updated = state.cartLines.toMutableList()
            if (index in updated.indices) {
                val cur = updated[index]
                val newPrice = if (cur.unitPrice.isBlank()) {
                    if (product.price % 1.0 == 0.0) product.price.toInt().toString() else product.price.toString()
                } else {
                    cur.unitPrice
                }
                updated[index] = cur.copy(
                    designation = product.displayName,
                    unitPrice = newPrice
                )
            }
            state.copy(cartLines = updated)
        }
    }

    fun addCartLine() {
        _localState.update { it.copy(cartLines = it.cartLines + CartLineDraft()) }
    }

    fun removeCartLine(index: Int) {
        _localState.update { state ->
            val updated = state.cartLines.toMutableList()
            if (index in updated.indices) {
                updated.removeAt(index)
            }
            if (updated.isEmpty()) updated.add(CartLineDraft())
            state.copy(cartLines = updated)
        }
    }

    fun cancelEditInvoice() {
        _localState.update {
            it.copy(
                editingInvoiceId = null,
                clientName = "",
                cartLines = List(5) { CartLineDraft() },
                noticeMessage = null
            )
        }
    }

    fun startEditInvoice(item: InvoiceWithLines) {
        val drafts = item.lines.map { line ->
            CartLineDraft(
                qty = line.qty.toString(),
                designation = line.designation,
                unitPrice = if (line.unitPrice % 1.0 == 0.0) line.unitPrice.toInt().toString() else line.unitPrice.toString()
            )
        }.ifEmpty { listOf(CartLineDraft()) }

        _localState.update {
            it.copy(
                selectedTab = ShopTab.FACTURE,
                editingInvoiceId = item.invoice.id,
                clientName = item.invoice.client,
                cartLines = drafts,
                noticeMessage = null
            )
        }
    }

    fun validateAndSaveInvoice() {
        val state = uiState.value
        val nonEmpty = state.cartLines.filter {
            it.designation.isNotBlank() || it.qty.isNotBlank() || it.unitPrice.isNotBlank()
        }
        if (nonEmpty.isEmpty()) {
            _localState.update { it.copy(alertMessage = "Remplissez au moins une ligne.") }
            return
        }
        val invalidLine = nonEmpty.any {
            it.designation.trim().isEmpty() || (it.qty.toIntOrNull() ?: 0) <= 0
        }
        if (invalidLine) {
            _localState.update {
                it.copy(alertMessage = "Chaque ligne doit avoir une quantité et une désignation.")
            }
            return
        }

        val editingInvoice = state.invoices.find { it.invoice.id == state.editingInvoiceId }
        val plan = planCart(nonEmpty, state.products, editingInvoice)
        if (plan.plannedLines.none { it.qty > 0 }) {
            val msg = plan.notes.joinToString("\n").ifBlank { "Stock fini." }
            _localState.update { it.copy(alertMessage = msg) }
            return
        }

        val finalLines = plan.plannedLines.filter { it.qty > 0 }.map {
            InvoiceLineInput(
                productId = it.product?.id,
                designation = it.designation,
                qty = it.qty,
                unitPrice = it.unitPrice,
                unitCost = it.product?.cost ?: 0.0
            )
        }
        val client = state.clientName.trim().ifBlank { "Client comptoir" }
        val dateKey = editingInvoice?.invoice?.dateKey ?: todayDateKey()
        val wasEditing = state.editingInvoiceId != null

        viewModelScope.launch {
            val savedId = repository.saveInvoice(
                editingInvoiceId = state.editingInvoiceId,
                dateKey = dateKey,
                client = client,
                lines = finalLines,
                notes = plan.notes
            )
            val notice = buildString {
                append("✅ Facture N°$savedId ${if (wasEditing) "modifiée." else "enregistrée."}")
                if (plan.notes.isNotEmpty()) {
                    append("\n")
                    append(plan.notes.joinToString("\n") { "⚠ $it" })
                }
            }
            _localState.update {
                it.copy(
                    editingInvoiceId = null,
                    clientName = "",
                    cartLines = List(5) { CartLineDraft() },
                    noticeMessage = notice
                )
            }
        }
    }

    // ---------- Arrivage ----------
    fun updateArrivalLine(
        index: Int,
        name: String? = null,
        quality: String? = null,
        qty: String? = null,
        unitCost: String? = null,
        unitPrice: String? = null
    ) {
        _localState.update { state ->
            val updated = state.arrivalLines.toMutableList()
            if (index in updated.indices) {
                val cur = updated[index]
                updated[index] = cur.copy(
                    name = name ?: cur.name,
                    quality = quality ?: cur.quality,
                    qty = qty ?: cur.qty,
                    unitCost = unitCost ?: cur.unitCost,
                    unitPrice = unitPrice ?: cur.unitPrice
                )
            }
            state.copy(arrivalLines = updated, arrivalError = null, arrivalNotice = null)
        }
    }

    fun addArrivalLine() {
        _localState.update { it.copy(arrivalLines = it.arrivalLines + ArrivalLineDraft()) }
    }

    fun removeArrivalLine(index: Int) {
        _localState.update { state ->
            val updated = state.arrivalLines.toMutableList()
            if (index in updated.indices) {
                updated.removeAt(index)
            }
            if (updated.isEmpty()) updated.add(ArrivalLineDraft())
            state.copy(arrivalLines = updated)
        }
    }

    fun saveArrivals() {
        val state = uiState.value
        val nonEmpty = state.arrivalLines.filter { it.name.isNotBlank() || it.qty.isNotBlank() }
        if (nonEmpty.isEmpty()) {
            _localState.update { it.copy(arrivalError = "Remplissez au moins une ligne.") }
            return
        }
        for (row in nonEmpty) {
            val n = row.name.trim()
            val q = row.qty.toIntOrNull() ?: 0
            val p = row.unitPrice.replace(',', '.').toDoubleOrNull()
            if (n.isEmpty() || q <= 0 || p == null || p < 0.0) {
                _localState.update {
                    it.copy(arrivalError = "Chaque ligne doit avoir un nom, une quantité et un prix de vente.")
                }
                return
            }
        }

        val inputs = nonEmpty.map { row ->
            ArrivalInput(
                name = row.name.trim(),
                quality = row.quality.trim(),
                qty = row.qty.toInt(),
                unitCost = row.unitCost.replace(',', '.').toDoubleOrNull() ?: 0.0,
                unitPrice = row.unitPrice.replace(',', '.').toDouble()
            )
        }

        viewModelScope.launch {
            repository.recordArrivals(todayDateKey(), inputs)
            _localState.update {
                it.copy(
                    arrivalLines = List(3) { ArrivalLineDraft() },
                    arrivalError = null,
                    arrivalNotice = "✅ Arrivage enregistré (${inputs.size} produit(s))."
                )
            }
        }
    }

    // ---------- Reports Date/Month ----------
    fun setSelectedDateKey(dateKey: String) {
        _localState.update { it.copy(selectedDateKey = dateKey) }
    }

    fun shiftSelectedDate(deltaDays: Int) {
        _localState.update { it.copy(selectedDateKey = shiftDateKey(it.selectedDateKey, deltaDays)) }
    }

    fun shiftSelectedMonth(deltaMonths: Int) {
        _localState.update { it.copy(selectedMonthKey = shiftMonthKey(it.selectedMonthKey, deltaMonths)) }
    }

    fun buildReportForTab(tab: ShopTab): ReportTableData {
        val s = uiState.value
        return when (tab) {
            ShopTab.STOCK -> ReportTableData(
                title = "Stock",
                subtitle = "Stock · ${formatDisplayDate(todayDateKey())}",
                kpis = listOf(
                    "Produits" to s.products.size.toString(),
                    "Valeur du stock" to formatCurrency(s.totalStockValue)
                ),
                headers = listOf("Produit", "Qualité", "Qté", "Achat", "Vente"),
                rows = s.products.map { p ->
                    listOf(
                        p.name,
                        p.quality.ifBlank { "—" },
                        if (p.qty <= 5) "${p.qty} ⚠" else p.qty.toString(),
                        formatCurrency(p.cost),
                        formatCurrency(p.price)
                    )
                }
            )
            ShopTab.JOUR -> ReportTableData(
                title = "Gain par jour",
                subtitle = "Gain par jour · ${formatDisplayDate(s.selectedDateKey)}",
                kpis = listOf(
                    "Gain net" to formatCurrency(s.dayProfit),
                    "Chiffre d’affaires" to formatCurrency(s.dayRevenue)
                ),
                headers = listOf("N°", "Client", "CA", "Gain"),
                rows = s.dayInvoices.map { inv ->
                    listOf(
                        "N°${inv.invoice.id}",
                        inv.invoice.client,
                        formatCurrency(inv.invoice.total),
                        formatCurrency(inv.invoice.profit)
                    )
                }
            )
            ShopTab.SEMAINE -> ReportTableData(
                title = "Gain de la semaine",
                subtitle = "Gain de la semaine · ${formatDisplayDate(todayDateKey())}",
                kpis = listOf(
                    "Gain net" to formatCurrency(s.weekProfit),
                    "Chiffre d’affaires" to formatCurrency(s.weekRevenue)
                ),
                headers = listOf("Jour", "Factures", "CA", "Gain"),
                rows = s.weekDays.map { d ->
                    listOf(
                        d.label,
                        d.invoiceCount.toString(),
                        formatCurrency(d.revenue),
                        formatCurrency(d.profit)
                    )
                }
            )
            ShopTab.MOIS -> ReportTableData(
                title = "Gain du mois",
                subtitle = "Gain du mois · ${formatDisplayMonth(s.selectedMonthKey)}",
                kpis = listOf(
                    "Gain net" to formatCurrency(s.monthProfit),
                    "Chiffre d’affaires" to formatCurrency(s.monthRevenue)
                ),
                headers = listOf("Jour", "Fact.", "CA", "Gain"),
                rows = s.monthDailyBreakdown.map { m ->
                    listOf(
                        m.displayDay,
                        m.invoiceCount.toString(),
                        formatCurrency(m.revenue),
                        formatCurrency(m.profit)
                    )
                }
            )
            ShopTab.HONNEUR -> ReportTableData(
                title = "Client d'honneur",
                subtitle = "Client d’honneur · ${formatDisplayDate(todayDateKey())}",
                headers = listOf("#", "Client", "Achats", "Total"),
                rows = s.clientHonorList.mapIndexed { idx, c ->
                    listOf(
                        (idx + 1).toString(),
                        c.name,
                        c.invoiceCount.toString(),
                        formatCurrency(c.totalSpent)
                    )
                }
            )
            else -> {
                val activeLines = s.cartLines.filter { it.designation.isNotBlank() || it.qty.isNotBlank() }
                ReportTableData(
                    title = "Facture N°${s.nextInvoiceNumber}",
                    subtitle = "Facture N°${s.nextInvoiceNumber} · ${formatDisplayDate(s.editingInvoiceDateKey)} · ${s.clientName.ifBlank { "Client comptoir" }}",
                    headers = listOf("Qté", "Désignation", "P.U", "PT"),
                    rows = activeLines.map { l ->
                        val q = l.qty.toIntOrNull() ?: 0
                        val pu = l.unitPrice.replace(',', '.').toDoubleOrNull() ?: 0.0
                        listOf(
                            q.toString(),
                            l.designation,
                            formatCurrency(pu),
                            formatCurrency(q * pu)
                        )
                    },
                    footerRow = listOf("", "", "Total", formatCurrency(s.cartTotal)),
                    showSignature = true
                )
            }
        }
    }

    private data class PlannedLine(
        val qty: Int,
        val designation: String,
        val unitPrice: Double,
        val product: ProductEntity?
    )

    private data class CartPlan(
        val plannedLines: List<PlannedLine>,
        val notes: List<String>
    )

    private fun matchProduct(designation: String, products: List<ProductEntity>): ProductEntity? {
        val trimmed = designation.trim()
        if (trimmed.isEmpty()) return null
        return products.find {
            it.displayName.equals(trimmed, ignoreCase = true) ||
                (it.quality.isBlank() && it.name.equals(trimmed, ignoreCase = true))
        }
    }

    private fun oldQtyForProduct(product: ProductEntity, editingInvoice: InvoiceWithLines?): Int {
        if (editingInvoice == null) return 0
        return editingInvoice.lines.filter { line ->
            if (line.productId != null) {
                line.productId == product.id
            } else {
                product.displayName.equals(line.designation, ignoreCase = true) ||
                    (product.quality.isBlank() && product.name.equals(line.designation, ignoreCase = true))
            }
        }.sumOf { it.qty }
    }

    private fun planCart(
        lines: List<CartLineDraft>,
        products: List<ProductEntity>,
        editingInvoice: InvoiceWithLines?
    ): CartPlan {
        val left = mutableMapOf<Long, Int>()
        val notes = mutableListOf<String>()
        val planned = lines.map { line ->
            val reqQ = line.qty.toIntOrNull() ?: 0
            val pu = line.unitPrice.replace(',', '.').toDoubleOrNull() ?: 0.0
            val desig = line.designation.trim()
            val prod = matchProduct(desig, products)
            if (prod == null) {
                PlannedLine(reqQ, desig, pu, null)
            } else {
                val avail = left.getOrPut(prod.id) { prod.qty + oldQtyForProduct(prod, editingInvoice) }
                val q = reqQ.coerceIn(0, avail)
                if (q < reqQ) {
                    if (q == 0) {
                        notes.add("Stock fini : $desig, ligne retirée")
                    } else {
                        notes.add("Stock insuffisant : $desig, quantité ramenée de $reqQ à $q")
                    }
                }
                val rem = avail - q
                left[prod.id] = rem
                if (q > 0 && rem == 0 && q == reqQ) {
                    notes.add("Stock épuisé : $desig")
                }
                PlannedLine(q, desig, pu, prod)
            }
        }
        return CartPlan(planned, notes)
    }

    private fun computeStockWarnings(
        cartLines: List<CartLineDraft>,
        products: List<ProductEntity>,
        editingInvoice: InvoiceWithLines?
    ): List<String> {
        val validDrafts = cartLines.filter {
            it.designation.isNotBlank() && (it.qty.toIntOrNull() ?: 0) > 0
        }
        if (validDrafts.isEmpty()) return emptyList()
        return planCart(validDrafts, products, editingInvoice).notes
    }
}

class ShopViewModelFactory(private val repository: ShopRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShopViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShopViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
