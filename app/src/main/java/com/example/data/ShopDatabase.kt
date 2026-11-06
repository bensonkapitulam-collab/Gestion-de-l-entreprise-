package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProductEntity::class,
        InvoiceEntity::class,
        InvoiceLineEntity::class,
        ArrivalEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ShopDatabase : RoomDatabase() {
    abstract fun shopDao(): ShopDao

    companion object {
        @Volatile
        private var INSTANCE: ShopDatabase? = null

        fun getInstance(context: Context): ShopDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShopDatabase::class.java,
                    "gestion_boutique.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
