package com.example.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quality: String = "",
    val qty: Int,
    val cost: Double,
    val price: Double
) {
    val displayName: String
        get() = if (quality.isNotBlank()) "$name ($quality)" else name
}

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,
    val client: String,
    val total: Double,
    val profit: Double,
    val notesText: String = ""
) {
    val notes: List<String>
        get() = if (notesText.isBlank()) emptyList() else notesText.split("\n").filter { it.isNotBlank() }
}

@Entity(
    tableName = "invoice_lines",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["invoiceId"])]
)
data class InvoiceLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val productId: Long?,
    val designation: String,
    val qty: Int,
    val unitPrice: Double,
    val unitCost: Double
)

data class InvoiceWithLines(
    @Embedded val invoice: InvoiceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val lines: List<InvoiceLineEntity>
)

@Entity(tableName = "arrivals")
data class ArrivalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,
    val productName: String,
    val quality: String = "",
    val qty: Int,
    val unitCost: Double
)
