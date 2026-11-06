package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class ArrivalInput(
    val name: String,
    val quality: String,
    val qty: Int,
    val unitCost: Double,
    val unitPrice: Double
)

data class InvoiceLineInput(
    val productId: Long?,
    val designation: String,
    val qty: Int,
    val unitPrice: Double,
    val unitCost: Double
)

@Dao
interface ShopDao {
    @Query("SELECT * FROM products ORDER BY id ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products")
    suspend fun getAllProductsOnce(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): ProductEntity?

    @Transaction
    @Query("SELECT * FROM invoices ORDER BY id DESC")
    fun getAllInvoicesWithLines(): Flow<List<InvoiceWithLines>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceWithLinesById(id: Long): InvoiceWithLines?

    @Query("SELECT * FROM arrivals ORDER BY id DESC")
    fun getAllArrivals(): Flow<List<ArrivalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceLines(lines: List<InvoiceLineEntity>)

    @Query("DELETE FROM invoice_lines WHERE invoiceId = :invoiceId")
    suspend fun deleteLinesForInvoice(invoiceId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArrival(arrival: ArrivalEntity)

    @Transaction
    suspend fun recordArrivalsTransaction(dateKey: String, items: List<ArrivalInput>) {
        val existingProducts = getAllProductsOnce().toMutableList()
        for (item in items) {
            val match = existingProducts.find {
                it.name.equals(item.name, ignoreCase = true) &&
                    it.quality.equals(item.quality, ignoreCase = true)
            }
            if (match == null) {
                val newProd = ProductEntity(
                    name = item.name,
                    quality = item.quality,
                    qty = item.qty,
                    cost = item.unitCost,
                    price = item.unitPrice
                )
                val newId = insertProduct(newProd)
                existingProducts.add(newProd.copy(id = newId))
            } else {
                val updated = match.copy(
                    qty = match.qty + item.qty,
                    cost = item.unitCost,
                    price = item.unitPrice
                )
                updateProduct(updated)
                val idx = existingProducts.indexOfFirst { it.id == match.id }
                if (idx >= 0) existingProducts[idx] = updated
            }
            insertArrival(
                ArrivalEntity(
                    dateKey = dateKey,
                    productName = item.name,
                    quality = item.quality,
                    qty = item.qty,
                    unitCost = item.unitCost
                )
            )
        }
    }

    @Transaction
    suspend fun saveInvoiceTransaction(
        editingInvoiceId: Long?,
        dateKey: String,
        client: String,
        lines: List<InvoiceLineInput>,
        notes: List<String>
    ): Long {
        val allProducts = getAllProductsOnce().associateBy { it.id }.toMutableMap()

        // If editing an existing invoice, restore its previous stock quantities first
        if (editingInvoiceId != null) {
            val existing = getInvoiceWithLinesById(editingInvoiceId)
            if (existing != null) {
                for (oldLine in existing.lines) {
                    val prod = oldLine.productId?.let { allProducts[it] }
                        ?: allProducts.values.find {
                            it.displayName.equals(oldLine.designation, ignoreCase = true) ||
                                (it.quality.isBlank() && it.name.equals(oldLine.designation, ignoreCase = true))
                        }
                    if (prod != null) {
                        val restored = prod.copy(qty = prod.qty + oldLine.qty)
                        updateProduct(restored)
                        allProducts[restored.id] = restored
                    }
                }
                deleteLinesForInvoice(editingInvoiceId)
            }
        }

        // Deduct new quantities
        for (line in lines) {
            val prod = line.productId?.let { allProducts[it] }
            if (prod != null) {
                val deducted = prod.copy(qty = (prod.qty - line.qty).coerceAtLeast(0))
                updateProduct(deducted)
                allProducts[deducted.id] = deducted
            }
        }

        val total = lines.sumOf { it.qty * it.unitPrice }
        val profit = lines.sumOf { it.qty * (it.unitPrice - it.unitCost) }
        val notesText = notes.joinToString("\n")

        val invoiceId = if (editingInvoiceId != null) {
            val existing = getInvoiceWithLinesById(editingInvoiceId)?.invoice
            val updatedInvoice = InvoiceEntity(
                id = editingInvoiceId,
                dateKey = existing?.dateKey ?: dateKey,
                client = client,
                total = total,
                profit = profit,
                notesText = notesText
            )
            updateInvoice(updatedInvoice)
            editingInvoiceId
        } else {
            insertInvoice(
                InvoiceEntity(
                    dateKey = dateKey,
                    client = client,
                    total = total,
                    profit = profit,
                    notesText = notesText
                )
            )
        }

        insertInvoiceLines(
            lines.map {
                InvoiceLineEntity(
                    invoiceId = invoiceId,
                    productId = it.productId,
                    designation = it.designation,
                    qty = it.qty,
                    unitPrice = it.unitPrice,
                    unitCost = it.unitCost
                )
            }
        )
        return invoiceId
    }
}
