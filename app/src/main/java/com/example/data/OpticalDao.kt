package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OpticalDao {
    // --- Products / Digital Catalog / Inventory (Managed per Piece) ---
    @Query("SELECT * FROM optical_products ORDER BY (currentStock <= lowStockThreshold) DESC, category ASC, name ASC")
    fun getAllProducts(): Flow<List<OpticalProduct>>

    @Query("SELECT * FROM optical_products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Int): OpticalProduct?

    @Query("SELECT * FROM optical_products WHERE LOWER(barcode) = LOWER(:code) OR LOWER(sku) = LOWER(:code) LIMIT 1")
    suspend fun getProductByBarcodeOrSku(code: String): OpticalProduct?

    @Query("SELECT * FROM optical_products WHERE category = :category")
    suspend fun getProductsByCategorySync(category: OpticalCategory): List<OpticalProduct>

    @Query("SELECT COUNT(*) FROM optical_products")
    suspend fun getProductCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: OpticalProduct): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<OpticalProduct>)

    @Update
    suspend fun updateProduct(product: OpticalProduct)

    @Query("UPDATE optical_products SET currentStock = :newStock, lowStockThreshold = :newThreshold WHERE id = :productId")
    suspend fun updateStockAndThreshold(productId: Int, newStock: Int, newThreshold: Int)

    @Query("UPDATE optical_products SET lowStockThreshold = :newThreshold WHERE category = :category")
    suspend fun applyThresholdToCategoryProducts(category: OpticalCategory, newThreshold: Int)

    @Query("DELETE FROM optical_products WHERE id = :productId")
    suspend fun deleteProduct(productId: Int)

    @Query("DELETE FROM optical_products")
    suspend fun deleteAllProducts()

    // --- Category Threshold Configurations ---
    @Query("SELECT * FROM category_threshold_configs")
    fun getAllCategoryThresholds(): Flow<List<CategoryThresholdConfig>>

    @Query("SELECT COUNT(*) FROM category_threshold_configs")
    suspend fun getCategoryThresholdCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategoryThreshold(config: CategoryThresholdConfig)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategoryThresholds(configs: List<CategoryThresholdConfig>)

    // --- Push Notification Alert Logs ---
    @Query("SELECT * FROM stock_push_alerts ORDER BY createdAtEpochMs DESC")
    fun getAllPushAlerts(): Flow<List<StockPushAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPushAlert(alert: StockPushAlert): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPushAlerts(alerts: List<StockPushAlert>)

    @Query("UPDATE stock_push_alerts SET isRead = 1")
    suspend fun markAllPushAlertsRead()

    @Query("DELETE FROM stock_push_alerts")
    suspend fun clearAllPushAlerts()

    // --- Retailer Custom Orders (Sales & Fulfillment) ---
    @Query("SELECT * FROM retailer_orders ORDER BY createdAtEpochMs DESC")
    fun getAllRetailerOrders(): Flow<List<RetailerOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRetailerOrder(order: RetailerOrder): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRetailerOrders(orders: List<RetailerOrder>)

    @Query("UPDATE retailer_orders SET stage = :newStage WHERE id = :orderId")
    suspend fun updateRetailerOrderStage(orderId: Int, newStage: RetailOrderStage)

    @Query("DELETE FROM retailer_orders WHERE id = :orderId")
    suspend fun deleteRetailerOrder(orderId: Int)

    // --- Manual Supplier Restock Purchase Orders ---
    @Query("SELECT * FROM restock_purchase_orders ORDER BY createdAtEpochMs DESC")
    fun getAllRestockOrders(): Flow<List<RestockPurchaseOrder>>

    @Query("SELECT * FROM restock_purchase_orders WHERE id = :poId LIMIT 1")
    suspend fun getRestockOrderById(poId: Int): RestockPurchaseOrder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRestockOrder(po: RestockPurchaseOrder): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRestockOrders(pos: List<RestockPurchaseOrder>)

    @Query("UPDATE restock_purchase_orders SET status = :newStatus WHERE id = :poId")
    suspend fun updateRestockOrderStatus(poId: Int, newStatus: RestockOrderStatus)

    // --- CRM: Retailer Shop Profiles ---
    @Query("SELECT * FROM retailer_shop_profiles ORDER BY shopName ASC")
    fun getAllShopProfiles(): Flow<List<RetailerShopProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShopProfile(profile: RetailerShopProfile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShopProfiles(profiles: List<RetailerShopProfile>)

    @Update
    suspend fun updateShopProfile(profile: RetailerShopProfile)

    @Query("DELETE FROM retailer_shop_profiles WHERE id = :shopId")
    suspend fun deleteShopProfile(shopId: Int)

    @Query("DELETE FROM retailer_shop_profiles")
    suspend fun deleteAllShopProfiles()

    @Query("DELETE FROM customer_ledger_entries WHERE shopId = :shopId")
    suspend fun deleteLedgerEntriesForShop(shopId: Int)

    @Query("DELETE FROM customer_interaction_logs WHERE shopId = :shopId")
    suspend fun deleteInteractionLogsForShop(shopId: Int)

    // --- Customer Ledger (Khata) Entries ---
    @Query("SELECT * FROM customer_ledger_entries ORDER BY timestampEpochMs DESC")
    fun getAllLedgerEntries(): Flow<List<CustomerLedgerEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntry(entry: CustomerLedgerEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntries(entries: List<CustomerLedgerEntry>)

    // --- CRM: Customer Interaction Logs ---
    @Query("SELECT * FROM customer_interaction_logs ORDER BY timestampEpochMs DESC")
    fun getAllInteractionLogs(): Flow<List<CustomerInteractionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteractionLog(log: CustomerInteractionLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteractionLogs(logs: List<CustomerInteractionLog>)

    // --- One-Shot Snapshot Queries for Firebase Firestore Cloud Sync & Auto-Backup ---
    @Query("SELECT * FROM optical_products")
    suspend fun getAllProductsSync(): List<OpticalProduct>

    @Query("SELECT * FROM category_threshold_configs")
    suspend fun getAllCategoryThresholdsSync(): List<CategoryThresholdConfig>

    @Query("SELECT * FROM retailer_orders")
    suspend fun getAllRetailerOrdersSync(): List<RetailerOrder>

    @Query("SELECT * FROM restock_purchase_orders")
    suspend fun getAllRestockOrdersSync(): List<RestockPurchaseOrder>

    @Query("SELECT * FROM retailer_shop_profiles")
    suspend fun getAllShopProfilesSync(): List<RetailerShopProfile>

    @Query("SELECT * FROM customer_ledger_entries")
    suspend fun getAllLedgerEntriesSync(): List<CustomerLedgerEntry>

    @Query("SELECT * FROM customer_interaction_logs")
    suspend fun getAllInteractionLogsSync(): List<CustomerInteractionLog>

    @Query("SELECT * FROM stock_push_alerts")
    suspend fun getAllPushAlertsSync(): List<StockPushAlert>
}
