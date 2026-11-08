package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AssetBalance::class, CryptoTransaction::class, PriceAlert::class, EncryptedKey::class, AITradingRule::class],
    version = 2,
    exportSchema = false
)
abstract class WalletDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao

    companion object {
        @Volatile
        private var INSTANCE: WalletDatabase? = null

        fun getDatabase(context: Context): WalletDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WalletDatabase::class.java,
                    "apex_wallet_database"
                )
                // SECURITY NOTE (P2): fallbackToDestructiveMigration(true) wipes all local wallet data
                // on any schema change. This is acceptable only for this demo. Before any production
                // release, replace with versioned Room migrations (Migration 1->2, 2->3, ...) and set
                // fallbackToDestructiveMigration(false) to avoid irreversible data loss.
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
