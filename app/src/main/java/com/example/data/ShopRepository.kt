package com.example.data

import kotlinx.coroutines.flow.Flow

class ShopRepository(
    private val dao: ShopDao,
    private val passwordStore: PasswordStore
) {
    val products: Flow<List<ProductEntity>> = dao.getAllProducts()
    val invoices: Flow<List<InvoiceWithLines>> = dao.getAllInvoicesWithLines()
    val arrivals: Flow<List<ArrivalEntity>> = dao.getAllArrivals()
    val hasPassword: Flow<Boolean> = passwordStore.hasPasswordFlow

    suspend fun setPassword(password: String) = passwordStore.setPassword(password)

    suspend fun verifyPassword(password: String): Boolean = passwordStore.verifyPassword(password)

    suspend fun recordArrivals(dateKey: String, items: List<ArrivalInput>) {
        dao.recordArrivalsTransaction(dateKey, items)
    }

    suspend fun saveInvoice(
        editingInvoiceId: Long?,
        dateKey: String,
        client: String,
        lines: List<InvoiceLineInput>,
        notes: List<String>
    ): Long {
        return dao.saveInvoiceTransaction(
            editingInvoiceId = editingInvoiceId,
            dateKey = dateKey,
            client = client,
            lines = lines,
            notes = notes
        )
    }
}
