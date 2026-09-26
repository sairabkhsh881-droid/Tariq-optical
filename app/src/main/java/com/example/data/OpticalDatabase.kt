package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class OpticalTypeConverters {
    @TypeConverter
    fun fromCategory(value: OpticalCategory): String = value.name

    @TypeConverter
    fun toCategory(value: String): OpticalCategory = OpticalCategory.valueOf(value)

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod = PaymentMethod.valueOf(value)

    @TypeConverter
    fun fromLedgerEntryType(value: LedgerEntryType): String = value.name

    @TypeConverter
    fun toLedgerEntryType(value: String): LedgerEntryType = LedgerEntryType.valueOf(value)

    @TypeConverter
    fun fromRetailStage(value: RetailOrderStage): String = value.name

    @TypeConverter
    fun toRetailStage(value: String): RetailOrderStage = RetailOrderStage.valueOf(value)

    @TypeConverter
    fun fromRestockStatus(value: RestockOrderStatus): String = value.name

    @TypeConverter
    fun toRestockStatus(value: String): RestockOrderStatus = RestockOrderStatus.valueOf(value)

    @TypeConverter
    fun fromInteractionType(value: InteractionType): String = value.name

    @TypeConverter
    fun toInteractionType(value: String): InteractionType = InteractionType.valueOf(value)
}

@Database(
    entities = [
        OpticalProduct::class,
        CategoryThresholdConfig::class,
        StockPushAlert::class,
        RetailerOrder::class,
        RestockPurchaseOrder::class,
        RetailerShopProfile::class,
        CustomerLedgerEntry::class,
        CustomerInteractionLog::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(OpticalTypeConverters::class)
abstract class OpticalDatabase : RoomDatabase() {
    abstract fun opticalDao(): OpticalDao

    companion object {
        @Volatile
        private var INSTANCE: OpticalDatabase? = null

        fun getInstance(context: Context): OpticalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OpticalDatabase::class.java,
                    "tariq_jaddah_optical_clean_v7.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
