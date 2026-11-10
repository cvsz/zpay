package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AssetBalance::class, CryptoTransaction::class, PriceAlert::class, EncryptedKey::class, AITradingRule::class],
    version = 2,
    exportSchema = true
)
abstract class WalletDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao

    companion object {
        @Volatile
        private var INSTANCE: WalletDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Migration from v1 (Double-based financial fields) to v2 (Long minor units)
                // 1. Create new tables with v2 schema
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `asset_balances_new` (
                        `symbol` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `chain` TEXT NOT NULL,
                        `balanceMinorUnit` INTEGER NOT NULL,
                        `priceUsdMinorUnit` INTEGER NOT NULL,
                        `priceChange24h` REAL NOT NULL,
                        `lastUpdated` INTEGER NOT NULL
                    )
                """.trimIndent())

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `crypto_transactions_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `chain` TEXT NOT NULL,
                        `hash` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `fromAddress` TEXT NOT NULL,
                        `toAddress` TEXT NOT NULL,
                        `amountMinorUnit` INTEGER NOT NULL,
                        `feeUsdMinorUnit` INTEGER NOT NULL,
                        `status` TEXT NOT NULL DEFAULT 'PENDING',
                        `idempotencyKey` TEXT,
                        `timestamp` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())

                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_crypto_transactions_new_idempotencyKey` ON `crypto_transactions_new` (`idempotencyKey`)")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `price_alerts_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `symbol` TEXT NOT NULL,
                        `targetPriceMinorUnit` INTEGER NOT NULL,
                        `direction` TEXT NOT NULL,
                        `isTriggered` INTEGER NOT NULL DEFAULT 0,
                        `isCustom` INTEGER NOT NULL DEFAULT 1,
                        `dateCreated` INTEGER NOT NULL
                    )
                """.trimIndent())

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `encrypted_keys_new` (
                        `chain` TEXT NOT NULL PRIMARY KEY,
                        `address` TEXT NOT NULL,
                        `encryptedPrivateKey` TEXT NOT NULL,
                        `iv` TEXT NOT NULL,
                        `dateGenerated` INTEGER NOT NULL
                    )
                """.trimIndent())

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `ai_trading_rules_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `assetSymbol` TEXT NOT NULL,
                        `actionType` TEXT NOT NULL,
                        `triggerCondition` TEXT NOT NULL,
                        `targetThresholdMinorUnit` INTEGER NOT NULL,
                        `tradeAmountMinorUnit` INTEGER NOT NULL,
                        `isEnabled` INTEGER NOT NULL DEFAULT 1,
                        `requiresConfirmation` INTEGER NOT NULL DEFAULT 1,
                        `dateCreated` INTEGER NOT NULL,
                        `lastTriggered` INTEGER
                    )
                """.trimIndent())

                // 2. Copy data from old tables to new tables with conversion
                // AssetBalance: balance * 10^decimals, priceUsd * 100
                database.execSQL("""
                    INSERT INTO `asset_balances_new` (`symbol`, `name`, `chain`, `balanceMinorUnit`, `priceUsdMinorUnit`, `priceChange24h`, `lastUpdated`)
                    SELECT 
                        `symbol`, `name`, `chain`,
                        CAST(`balance` * CASE 
                            WHEN `symbol` = 'BTC' THEN 100000000
                            WHEN `symbol` = 'ETH' THEN 1000000000000000000
                            WHEN `symbol` = 'SOL' THEN 1000000000
                            WHEN `symbol` = 'BNB' THEN 100000000
                            ELSE 100
                        END AS INTEGER),
                        CAST(`priceUsd` * 100 AS INTEGER),
                        `priceChange24h`, `lastUpdated`
                    FROM `asset_balances`
                """.trimIndent())

                // CryptoTransaction: amount * 10^decimals, feeUsd * 100
                database.execSQL("""
                    INSERT INTO `crypto_transactions_new` (`id`, `chain`, `hash`, `type`, `fromAddress`, `toAddress`, `amountMinorUnit`, `feeUsdMinorUnit`, `status`, `timestamp`, `notes`)
                    SELECT 
                        `id`, `chain`, `hash`, `type`, `fromAddress`, `toAddress`,
                        CAST(`amount` * CASE 
                            WHEN `chain` = 'Bitcoin' THEN 100000000
                            WHEN `chain` = 'Ethereum' THEN 1000000000000000000
                            WHEN `chain` = 'Solana' THEN 1000000000
                            WHEN `chain` = 'BSC' THEN 100000000
                            ELSE 100
                        END AS INTEGER),
                        CAST(`feeUsd` * 100 AS INTEGER),
                        CASE 
                            WHEN `status` = 'Success' THEN 'SUCCESS'
                            WHEN `status` = 'Pending' THEN 'PENDING'
                            WHEN `status` = 'Failed' THEN 'FAILED'
                            ELSE 'PENDING'
                        END,
                        `timestamp`, `notes`
                    FROM `crypto_transactions`
                """.trimIndent())

                // PriceAlert: targetPrice * 100
                database.execSQL("""
                    INSERT INTO `price_alerts_new` (`id`, `symbol`, `targetPriceMinorUnit`, `direction`, `isTriggered`, `isCustom`, `dateCreated`)
                    SELECT `id`, `symbol`, CAST(`targetPrice` * 100 AS INTEGER), `direction`, `isTriggered`, `isCustom`, `dateCreated`
                    FROM `price_alerts`
                """.trimIndent())

                // EncryptedKey: no conversion needed
                database.execSQL("""
                    INSERT INTO `encrypted_keys_new` (`chain`, `address`, `encryptedPrivateKey`, `iv`, `dateGenerated`)
                    SELECT `chain`, `address`, `encryptedPrivateKey`, `iv`, `dateGenerated`
                    FROM `encrypted_keys`
                """.trimIndent())

                // 3. Drop old tables
                database.execSQL("DROP TABLE IF EXISTS `asset_balances`")
                database.execSQL("DROP TABLE IF EXISTS `crypto_transactions`")
                database.execSQL("DROP TABLE IF EXISTS `price_alerts`")
                database.execSQL("DROP TABLE IF EXISTS `encrypted_keys`")

                // 4. Rename new tables to final names
                database.execSQL("ALTER TABLE `asset_balances_new` RENAME TO `asset_balances`")
                database.execSQL("ALTER TABLE `crypto_transactions_new` RENAME TO `crypto_transactions`")
                database.execSQL("ALTER TABLE `price_alerts_new` RENAME TO `price_alerts`")
                database.execSQL("ALTER TABLE `encrypted_keys_new` RENAME TO `encrypted_keys`")
                database.execSQL("ALTER TABLE `ai_trading_rules_new` RENAME TO `ai_trading_rules`")
            }
        }

        fun getDatabase(context: Context): WalletDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WalletDatabase::class.java,
                    "apex_wallet_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
