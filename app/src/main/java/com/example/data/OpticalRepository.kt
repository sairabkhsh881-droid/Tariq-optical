package com.example.data

import android.content.Context
import com.example.util.OpticalFirestoreSyncManager
import com.example.util.OpticalNotificationHelper
import kotlinx.coroutines.flow.Flow
import kotlin.math.max

class OpticalRepository(
    private val dao: OpticalDao,
    private val appContext: Context
) {
    val cloudSyncManager = OpticalFirestoreSyncManager(appContext, dao)

    val allProducts: Flow<List<OpticalProduct>> = dao.getAllProducts()
    val allCategoryThresholds: Flow<List<CategoryThresholdConfig>> = dao.getAllCategoryThresholds()
    val allPushAlerts: Flow<List<StockPushAlert>> = dao.getAllPushAlerts()
    val allRetailerOrders: Flow<List<RetailerOrder>> = dao.getAllRetailerOrders()
    val allRestockOrders: Flow<List<RestockPurchaseOrder>> = dao.getAllRestockOrders()
    val allShopProfiles: Flow<List<RetailerShopProfile>> = dao.getAllShopProfiles()
    val allLedgerEntries: Flow<List<CustomerLedgerEntry>> = dao.getAllLedgerEntries()
    val allInteractionLogs: Flow<List<CustomerInteractionLog>> = dao.getAllInteractionLogs()

    /**
     * Initializes default category threshold configurations only.
     * Pre-seeded products and customers have been removed so the dealer starts with a clean
     * catalog and customer list and can add their own products and customers.
     */
    suspend fun ensureSeedData() {
        if (dao.getCategoryThresholdCount() == 0) {
            val now = System.currentTimeMillis()
            val initialCategoryThresholds = listOf(
                CategoryThresholdConfig(
                    category = OpticalCategory.EYEGLASS_FRAMES,
                    thresholdUnits = 15,
                    pushAlertsEnabled = true,
                    updatedAtEpochMs = now
                ),
                CategoryThresholdConfig(
                    category = OpticalCategory.OPHTHALMIC_LENSES,
                    thresholdUnits = 25,
                    pushAlertsEnabled = true,
                    updatedAtEpochMs = now
                ),
                CategoryThresholdConfig(
                    category = OpticalCategory.CONTACT_LENSES,
                    thresholdUnits = 20,
                    pushAlertsEnabled = true,
                    updatedAtEpochMs = now
                )
            )
            dao.upsertCategoryThresholds(initialCategoryThresholds)
        }
        cloudSyncManager.initializeFirestoreAndSyncIfAvailable()
    }

    suspend fun saveProduct(product: OpticalProduct): OpticalProduct {
        val cleanBarcode = product.barcode.trim().ifEmpty {
            val base12 = product.category.barcodePrefix + (1000..9999).random().toString()
            val checkDigit = computeEan13CheckDigit(base12)
            base12 + checkDigit
        }
        val productWithBarcode = product.copy(barcode = cleanBarcode)
        val id = dao.insertProduct(productWithBarcode).toInt()
        val saved = productWithBarcode.copy(id = if (productWithBarcode.id != 0) productWithBarcode.id else id)
        if (saved.isLowStock) {
            recordAndDispatchLowStockPush(
                product = saved,
                reason = "SKU saved with stock (${saved.currentStock} Pieces) at/below threshold (${saved.lowStockThreshold} Pieces)."
            )
        }
        cloudSyncManager.triggerAutomaticCloudBackup(
            changeDescriptionEn = "Auto-backed up product ${saved.sku} (${saved.currentStock} Pieces) to Firestore.",
            changeDescriptionUr = "پراڈکٹ ${saved.sku} (${saved.currentStock} پیس) خودکار طور پر فائر بیس کلاؤڈ پر محفوظ ہو گئی۔"
        )
        return saved
    }

    suspend fun deleteProduct(product: OpticalProduct) {
        dao.deleteProduct(product.id)
        cloudSyncManager.triggerAutomaticCloudBackup(
            changeDescriptionEn = "Deleted product ${product.sku} from catalog.",
            changeDescriptionUr = "پراڈکٹ ${product.sku} کیٹلاگ سے حذف کر دی گئی۔"
        )
    }

    suspend fun deleteAllProducts() {
        dao.deleteAllProducts()
        cloudSyncManager.triggerAutomaticCloudBackup(
            changeDescriptionEn = "Cleared all products from catalog.",
            changeDescriptionUr = "تمام پراڈکٹس کیٹلاگ سے صاف کر دی گئیں۔"
        )
    }

    private fun computeEan13CheckDigit(first12Digits: String): Int {
        val digits = first12Digits.take(12).padEnd(12, '0').map { it.digitToIntOrNull() ?: 0 }
        val sum = digits.mapIndexed { idx, d -> if (idx % 2 == 0) d else d * 3 }.sum()
        return (10 - (sum % 10)) % 10
    }

    suspend fun addNewStockPieces(
        product: OpticalProduct,
        addedPieces: Int,
        updatedWholesalePricePkr: Double,
        updatedUnitCostPkr: Double,
        batchNotes: String
    ) {
        val cleanPieces = max(1, addedPieces)
        val newTotalStock = product.currentStock + cleanPieces
        val updatedProduct = product.copy(
            currentStock = newTotalStock,
            wholesalePrice = updatedWholesalePricePkr,
            unitCostPkr = updatedUnitCostPkr
        )
        dao.updateProduct(updatedProduct)

        val randomSuffix = (600..999).random()
        dao.insertRestockOrder(
            RestockPurchaseOrder(
                poNumber = "STK-ADD-$randomSuffix",
                productId = product.id,
                productSku = product.sku,
                productName = product.name,
                category = product.category,
                supplierName = product.supplierName,
                orderQuantity = cleanPieces,
                estimatedUnitCost = updatedUnitCostPkr,
                totalCost = updatedUnitCostPkr * cleanPieces,
                manualNotes = batchNotes.ifBlank {
                    "Direct Add New Stock: +$cleanPieces Pieces added to warehouse."
                },
                status = RestockOrderStatus.RECEIVED_INTO_STOCK
            )
        )
        cloudSyncManager.triggerAutomaticCloudBackup(
            changeDescriptionEn = "Auto-backed up +$cleanPieces Pieces added to ${product.sku} in Firestore.",
            changeDescriptionUr = "${product.sku} کے +$cleanPieces پیس کلاؤڈ اسٹاک میں اپ ڈیٹ کر دیے گئے۔"
        )
    }

    suspend fun updateStockAndThreshold(productId: Int, newStock: Int, newThreshold: Int) {
        val sanitizedStock = max(0, newStock)
        val sanitizedThreshold = max(1, newThreshold)
        dao.updateStockAndThreshold(
            productId = productId,
            newStock = sanitizedStock,
            newThreshold = sanitizedThreshold
        )
        val updatedProduct = dao.getProductById(productId)
        if (updatedProduct != null && updatedProduct.isLowStock) {
            recordAndDispatchLowStockPush(
                product = updatedProduct,
                reason = "Stock/threshold updated to $sanitizedStock Pieces (Threshold ≤ $sanitizedThreshold Pieces)."
            )
        }
        cloudSyncManager.triggerAutomaticCloudBackup(
            changeDescriptionEn = "Auto-backed up inventory count ($sanitizedStock Pieces) to Firestore.",
            changeDescriptionUr = "اسٹاک کی تعداد ($sanitizedStock پیس) کلاؤڈ پر محفوظ ہو گئی۔"
        )
    }

    suspend fun updateCategoryThresholdAndApply(
        category: OpticalCategory,
        newThreshold: Int,
        pushEnabled: Boolean,
        applyToAllProductsInCategory: Boolean
    ): Int {
        val sanitized = max(1, newThreshold)
        dao.upsertCategoryThreshold(
            CategoryThresholdConfig(
                category = category,
                thresholdUnits = sanitized,
                pushAlertsEnabled = pushEnabled,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
        var newlyLowCount = 0
        if (applyToAllProductsInCategory) {
            dao.applyThresholdToCategoryProducts(category, sanitized)
            val categoryProducts = dao.getProductsByCategorySync(category)
            val lowProducts = categoryProducts.filter { it.currentStock <= sanitized }
            newlyLowCount = lowProducts.size
            if (pushEnabled && lowProducts.isNotEmpty()) {
                lowProducts.forEach { prod ->
                    dao.insertPushAlert(
                        StockPushAlert(
                            productId = prod.id,
                            productSku = prod.sku,
                            productName = prod.name,
                            category = prod.category,
                            currentStock = prod.currentStock,
                            threshold = sanitized,
                            triggerReason = "Category '${category.displayName}' threshold updated to ≤ $sanitized Pieces."
                        )
                    )
                }
                OpticalNotificationHelper.dispatchCategoryThresholdPushNotification(
                    context = appContext,
                    categoryName = category.displayName,
                    newThreshold = sanitized,
                    affectedCount = lowProducts.size
                )
            }
        }
        cloudSyncManager.triggerAutomaticCloudBackup(
            changeDescriptionEn = "Auto-backed up ${category.displayName} threshold ($sanitized Pieces) to Firestore.",
            changeDescriptionUr = "${category.urduName} کا تھریشولڈ ($sanitized پیس) کلاؤڈ پر محفوظ ہو گیا۔"
        )
        return newlyLowCount
    }

    suspend fun triggerPushScanForAllLowStock(lowStockProducts: List<OpticalProduct>) {
        lowStockProducts.forEach { prod ->
            recordAndDispatchLowStockPush(
                product = prod,
                reason = "Dealer triggered active low-stock threshold push check."
            )
        }
    }

    suspend fun markAllPushAlertsRead() {
        dao.markAllPushAlertsRead()
    }

    suspend fun clearAllPushAlerts() {
        dao.clearAllPushAlerts()
    }

    private suspend fun recordAndDispatchLowStockPush(
        product: OpticalProduct,
        reason: String
    ) {
        dao.insertPushAlert(
            StockPushAlert(
                productId = product.id,
                productSku = product.sku,
                productName = product.name,
                category = product.category,
                currentStock = product.currentStock,
                threshold = product.lowStockThreshold,
                triggerReason = reason
            )
        )
        OpticalNotificationHelper.dispatchLowStockPushNotification(
            context = appContext,
            product = product,
            triggerReason = reason
        )
    }

    suspend fun createRetailerOrderAndDeductStock(
        shopId: Int,
        retailerShopName: String,
        retailerCity: String,
        retailerContact: String,
        product: OpticalProduct,
        quantity: Int,
        customRxAndLabNotes: String,
        paymentMethod: PaymentMethod,
        senderNumber: String,
        receiverNumber: String,
        paidAmountPkr: Double
    ): RetailerOrder {
        // If product is newly entered inline (id == 0), save it to catalog first
        val ensuredProduct = if (product.id == 0) {
            saveProduct(product.copy(currentStock = max(product.currentStock, quantity)))
        } else {
            product
        }

        // If shopId == 0 and customer name is not a generic walk-in, auto-save customer profile if not already present
        val cleanShopName = retailerShopName.trim().ifEmpty { "Optical Customer" }
        val resolvedShopId = if (shopId != 0) {
            shopId
        } else if (!cleanShopName.contains("Counter Sale", ignoreCase = true) &&
            !cleanShopName.contains("Walk-in", ignoreCase = true)
        ) {
            val existingShop = dao.getAllShopProfilesSync().firstOrNull {
                it.shopName.equals(cleanShopName, ignoreCase = true)
            }
            existingShop?.id ?: dao.insertShopProfile(
                RetailerShopProfile(
                    shopName = cleanShopName,
                    cityAndMarket = retailerCity.trim().ifEmpty { "Pakistan" },
                    contactPerson = cleanShopName,
                    phoneNumber = retailerContact.trim().ifEmpty { senderNumber.trim() },
                    email = "",
                    accountTier = "Regular Account",
                    paymentTermsPreference = paymentMethod.label,
                    creditLimitPkr = 300000.0,
                    specialNotesAndPreferences = "Auto-created from New Order (${ensuredProduct.category.displayName})"
                )
            ).toInt()
        } else {
            0
        }

        val randomSuffix = (100..999).random()
        val totalOrderAmount = ensuredProduct.wholesalePrice * quantity
        val actualPaid = paidAmountPkr.coerceIn(0.0, totalOrderAmount)
        val debtRemaining = (totalOrderAmount - actualPaid).coerceAtLeast(0.0)

        val order = RetailerOrder(
            orderNumber = "TJO-2026-$randomSuffix",
            shopId = resolvedShopId,
            retailerShopName = cleanShopName,
            retailerCity = retailerCity.trim().ifEmpty { "Pakistan" },
            retailerContact = retailerContact.trim().ifEmpty { "Customer" },
            productId = ensuredProduct.id,
            productSku = ensuredProduct.sku,
            productName = ensuredProduct.name,
            category = ensuredProduct.category,
            quantity = quantity,
            unitWholesalePrice = ensuredProduct.wholesalePrice,
            unitCostPkr = ensuredProduct.unitCostPkr,
            totalAmount = totalOrderAmount,
            customRxAndLabNotes = customRxAndLabNotes.trim(),
            paymentTerms = paymentMethod.label,
            paymentMethod = paymentMethod,
            senderNumber = senderNumber.trim(),
            receiverNumber = receiverNumber.trim(),
            paidAmountPkr = actualPaid,
            debtAmountPkr = debtRemaining,
            stage = RetailOrderStage.RECEIVED
        )
        val insertedId = dao.insertRetailerOrder(order).toInt()
        val savedOrder = order.copy(id = insertedId)

        // 1. Record Order Invoice (Debit) in Customer Ledger
        val now = System.currentTimeMillis()
        val debitEntry = CustomerLedgerEntry(
            shopId = resolvedShopId,
            shopName = cleanShopName,
            entryType = LedgerEntryType.DEBIT_INVOICE,
            amountPkr = totalOrderAmount,
            paymentMethod = paymentMethod,
            senderNumber = senderNumber.trim(),
            receiverNumber = receiverNumber.trim(),
            referenceCode = savedOrder.orderNumber,
            notes = "Order: $quantity Pieces × ${ensuredProduct.sku} @ PKR ${ensuredProduct.wholesalePrice.toLong()}/piece",
            timestampEpochMs = now
        )
        val debitId = dao.insertLedgerEntry(debitEntry).toInt()
        cloudSyncManager.cacheLedgerEntryOfflineFirst(
            entry = debitEntry.copy(id = debitId),
            changeDescriptionEn = "Cached Khata invoice ${savedOrder.orderNumber} for $cleanShopName.",
            changeDescriptionUr = "$cleanShopName کا کھاتہ بل ${savedOrder.orderNumber} محفوظ ہو گیا۔"
        )

        // 2. If any payment was made upfront, record Credit entry
        if (actualPaid > 0.0) {
            val creditEntry = CustomerLedgerEntry(
                shopId = resolvedShopId,
                shopName = cleanShopName,
                entryType = LedgerEntryType.CREDIT_PAYMENT,
                amountPkr = actualPaid,
                paymentMethod = paymentMethod,
                senderNumber = senderNumber.trim(),
                receiverNumber = receiverNumber.trim(),
                referenceCode = "${savedOrder.orderNumber}-PAY",
                notes = "Payment received via ${paymentMethod.label} (Order ${savedOrder.orderNumber})",
                timestampEpochMs = now + 100L
            )
            val creditId = dao.insertLedgerEntry(creditEntry).toInt()
            cloudSyncManager.cacheLedgerEntryOfflineFirst(
                entry = creditEntry.copy(id = creditId),
                changeDescriptionEn = "Cached Khata payment ${savedOrder.orderNumber}-PAY for $cleanShopName.",
                changeDescriptionUr = "$cleanShopName کی کھاتہ ادائیگی ${savedOrder.orderNumber}-PAY محفوظ ہو گئی۔"
            )
        }

        // Deduct pieces from stock
        val updatedStock = max(0, ensuredProduct.currentStock - quantity)
        dao.updateStockAndThreshold(ensuredProduct.id, updatedStock, ensuredProduct.lowStockThreshold)
        val updatedProduct = dao.getProductById(ensuredProduct.id)
        if (updatedProduct != null && updatedProduct.isLowStock) {
            recordAndDispatchLowStockPush(
                product = updatedProduct,
                reason = "Order ${savedOrder.orderNumber} for $cleanShopName deducted $quantity Pieces."
            )
        }
        cloudSyncManager.cacheSalesRecordOfflineFirst(
            order = savedOrder,
            changeDescriptionEn = "Auto-backed up & cached Order ${savedOrder.orderNumber}, Khata ledger & stock deduction.",
            changeDescriptionUr = "آرڈر ${savedOrder.orderNumber}، کھاتہ لیجر اور اسٹاک کی کمی لوکل کیش اور کلاؤڈ پر محفوظ ہو گئی۔"
        )
        return savedOrder
    }

    /**
     * Records an instant over-the-counter sale (Counter Sale / کاؤنٹر سیل).
     * Automatically marks the sale as DELIVERED, deducts stock, records ledger payment,
     * and returns the completed RetailerOrder for immediate receipt printing/export.
     */
    suspend fun createCounterSaleAndDeductStock(
        customerName: String,
        customerPhone: String,
        product: OpticalProduct,
        quantity: Int,
        unitSalePricePkr: Double,
        discountPkr: Double,
        customRxAndLabNotes: String,
        paymentMethod: PaymentMethod,
        senderNumber: String,
        receiverNumber: String,
        paidAmountPkr: Double
    ): RetailerOrder {
        val ensuredProduct = if (product.id == 0) {
            saveProduct(
                product.copy(
                    wholesalePrice = unitSalePricePkr,
                    currentStock = max(product.currentStock, quantity)
                )
            )
        } else {
            product
        }

        val randomSuffix = (100..999).random()
        val grossAmount = unitSalePricePkr * quantity
        val netTotalAmount = (grossAmount - discountPkr.coerceAtLeast(0.0)).coerceAtLeast(0.0)
        val effectiveUnitPrice = if (quantity > 0) netTotalAmount / quantity else unitSalePricePkr
        val actualPaid = paidAmountPkr.coerceIn(0.0, netTotalAmount)
        val debtRemaining = (netTotalAmount - actualPaid).coerceAtLeast(0.0)

        val cleanBuyerName = customerName.trim().ifEmpty { "Counter Sale (Walk-in)" }
        val displayBuyerName = if (cleanBuyerName.contains("Counter", ignoreCase = true)) {
            cleanBuyerName
        } else {
            "Counter Sale • $cleanBuyerName"
        }
        val cleanPhone = customerPhone.trim().ifEmpty { "Walk-in Counter" }

        val discountNote = if (discountPkr > 0) " [Discount: PKR ${discountPkr.toLong()}]" else ""
        val finalNotes = "[COUNTER SALE] ${customRxAndLabNotes.trim()}$discountNote".trim()

        val order = RetailerOrder(
            orderNumber = "CS-2026-$randomSuffix",
            shopId = 0,
            retailerShopName = displayBuyerName,
            retailerCity = "Counter Sale (Walk-in POS)",
            retailerContact = cleanPhone,
            productId = ensuredProduct.id,
            productSku = ensuredProduct.sku,
            productName = ensuredProduct.name,
            category = ensuredProduct.category,
            quantity = quantity,
            unitWholesalePrice = effectiveUnitPrice,
            unitCostPkr = ensuredProduct.unitCostPkr,
            totalAmount = netTotalAmount,
            customRxAndLabNotes = finalNotes,
            paymentTerms = "Counter Sale (${paymentMethod.label})",
            paymentMethod = paymentMethod,
            senderNumber = senderNumber.trim().ifEmpty { cleanPhone },
            receiverNumber = receiverNumber.trim().ifEmpty { paymentMethod.defaultReceiverNo },
            paidAmountPkr = actualPaid,
            debtAmountPkr = debtRemaining,
            stage = RetailOrderStage.DELIVERED
        )
        val insertedId = dao.insertRetailerOrder(order).toInt()
        val savedOrder = order.copy(id = insertedId)

        val now = System.currentTimeMillis()
        val counterDebit = CustomerLedgerEntry(
            shopId = 0,
            shopName = displayBuyerName,
            entryType = LedgerEntryType.DEBIT_INVOICE,
            amountPkr = netTotalAmount,
            paymentMethod = paymentMethod,
            senderNumber = senderNumber.trim().ifEmpty { cleanPhone },
            receiverNumber = receiverNumber.trim().ifEmpty { paymentMethod.defaultReceiverNo },
            referenceCode = savedOrder.orderNumber,
            notes = "Counter Sale: $quantity Pieces × ${ensuredProduct.sku} @ PKR ${effectiveUnitPrice.toLong()}/piece",
            timestampEpochMs = now
        )
        val counterDebitId = dao.insertLedgerEntry(counterDebit).toInt()
        cloudSyncManager.cacheLedgerEntryOfflineFirst(
            entry = counterDebit.copy(id = counterDebitId),
            changeDescriptionEn = "Cached Counter Sale invoice ${savedOrder.orderNumber}.",
            changeDescriptionUr = "کاؤنٹر سیل بل ${savedOrder.orderNumber} محفوظ ہو گیا۔"
        )

        if (actualPaid > 0.0) {
            val counterCredit = CustomerLedgerEntry(
                shopId = 0,
                shopName = displayBuyerName,
                entryType = LedgerEntryType.CREDIT_PAYMENT,
                amountPkr = actualPaid,
                paymentMethod = paymentMethod,
                senderNumber = senderNumber.trim().ifEmpty { cleanPhone },
                receiverNumber = receiverNumber.trim().ifEmpty { paymentMethod.defaultReceiverNo },
                referenceCode = "${savedOrder.orderNumber}-PAY",
                notes = "Counter Sale payment via ${paymentMethod.label} (${savedOrder.orderNumber})",
                timestampEpochMs = now + 100L
            )
            val counterCreditId = dao.insertLedgerEntry(counterCredit).toInt()
            cloudSyncManager.cacheLedgerEntryOfflineFirst(
                entry = counterCredit.copy(id = counterCreditId),
                changeDescriptionEn = "Cached Counter Sale payment ${savedOrder.orderNumber}-PAY.",
                changeDescriptionUr = "کاؤنٹر سیل ادائیگی ${savedOrder.orderNumber}-PAY محفوظ ہو گئی۔"
            )
        }

        val updatedStock = max(0, ensuredProduct.currentStock - quantity)
        dao.updateStockAndThreshold(ensuredProduct.id, updatedStock, ensuredProduct.lowStockThreshold)
        val updatedProduct = dao.getProductById(ensuredProduct.id)
        if (updatedProduct != null && updatedProduct.isLowStock) {
            recordAndDispatchLowStockPush(
                product = updatedProduct,
                reason = "Counter Sale ${savedOrder.orderNumber} deducted $quantity Pieces."
            )
        }
        cloudSyncManager.cacheSalesRecordOfflineFirst(
            order = savedOrder,
            changeDescriptionEn = "Auto-backed up & cached Counter Sale ${savedOrder.orderNumber} to Firestore.",
            changeDescriptionUr = "کاؤنٹر سیل ${savedOrder.orderNumber} لوکل کیش اور کلاؤڈ پر محفوظ ہو گئی۔"
        )
        return savedOrder
    }

    suspend fun advanceRetailerOrderStage(order: RetailerOrder) {
        val next = order.stage.nextStage() ?: return
        dao.updateRetailerOrderStage(order.id, next)
        cloudSyncManager.cacheSalesRecordOfflineFirst(
            order = order.copy(stage = next),
            changeDescriptionEn = "Auto-backed up & cached Order ${order.orderNumber} stage (${next.label}) to Firestore.",
            changeDescriptionUr = "آرڈر ${order.orderNumber} کا مرحلہ (${next.urduLabel}) لوکل کیش اور کلاؤڈ پر اپ ڈیٹ ہو گیا۔"
        )
    }

    suspend fun placeManualRestockOrder(
        product: OpticalProduct,
        orderQuantity: Int,
        manualNotes: String
    ) {
        val unitCost = product.unitCostPkr
        val randomSuffix = (502..999).random()
        val po = RestockPurchaseOrder(
            poNumber = "PO-TJO-$randomSuffix",
            productId = product.id,
            productSku = product.sku,
            productName = product.name,
            category = product.category,
            supplierName = product.supplierName,
            orderQuantity = orderQuantity,
            estimatedUnitCost = unitCost,
            totalCost = unitCost * orderQuantity,
            manualNotes = manualNotes.ifBlank {
                "Manual restock placed due to low-stock threshold (${product.currentStock}/${product.lowStockThreshold} Pieces remaining)."
            },
            status = RestockOrderStatus.ORDERED
        )
        dao.insertRestockOrder(po)
        cloudSyncManager.triggerAutomaticCloudBackup(
            changeDescriptionEn = "Auto-backed up Supplier PO ${po.poNumber} to Firestore.",
            changeDescriptionUr = "سپلائر آرڈر ${po.poNumber} کلاؤڈ پر محفوظ ہو گیا۔"
        )
    }

    suspend fun advanceRestockOrderStatus(po: RestockPurchaseOrder) {
        when (po.status) {
            RestockOrderStatus.ORDERED -> {
                dao.updateRestockOrderStatus(po.id, RestockOrderStatus.IN_TRANSIT)
            }
            RestockOrderStatus.IN_TRANSIT -> {
                dao.updateRestockOrderStatus(po.id, RestockOrderStatus.RECEIVED_INTO_STOCK)
                val product = dao.getProductById(po.productId)
                if (product != null) {
                    dao.updateStockAndThreshold(
                        productId = product.id,
                        newStock = product.currentStock + po.orderQuantity,
                        newThreshold = product.lowStockThreshold
                    )
                }
            }
            RestockOrderStatus.RECEIVED_INTO_STOCK -> Unit
        }
        cloudSyncManager.triggerAutomaticCloudBackup(
            changeDescriptionEn = "Auto-backed up PO ${po.poNumber} & stock arrival to Firestore.",
            changeDescriptionUr = "سپلائر آرڈر ${po.poNumber} اور اسٹاک آمد کلاؤڈ پر اپ ڈیٹ ہو گئی۔"
        )
    }

    // --- CRM & Customer Ledger (Khata) Operations ---
    suspend fun saveShopProfile(profile: RetailerShopProfile) {
        dao.insertShopProfile(profile)
        cloudSyncManager.triggerAutomaticCloudBackup(
            changeDescriptionEn = "Auto-backed up shop profile '${profile.shopName}' to Firestore.",
            changeDescriptionUr = "دکان پروفائل '${profile.shopName}' کلاؤڈ پر محفوظ ہو گئی۔"
        )
    }

    suspend fun deleteShopProfile(profile: RetailerShopProfile) {
        dao.deleteShopProfile(profile.id)
        dao.deleteLedgerEntriesForShop(profile.id)
        dao.deleteInteractionLogsForShop(profile.id)
        cloudSyncManager.triggerAutomaticCloudBackup(
            changeDescriptionEn = "Deleted customer profile '${profile.shopName}'.",
            changeDescriptionUr = "کسٹمر پروفائل '${profile.shopName}' حذف کر دی گئی۔"
        )
    }

    suspend fun deleteAllShopProfiles() {
        dao.deleteAllShopProfiles()
        cloudSyncManager.triggerAutomaticCloudBackup(
            changeDescriptionEn = "Cleared all customer profiles.",
            changeDescriptionUr = "تمام کسٹمر پروفائلز صاف کر دی گئیں۔"
        )
    }

    suspend fun recordCustomerLedgerEntry(
        shop: RetailerShopProfile,
        entryType: LedgerEntryType,
        amountPkr: Double,
        paymentMethod: PaymentMethod,
        senderNumber: String,
        receiverNumber: String,
        referenceCode: String,
        notes: String
    ) {
        val entry = CustomerLedgerEntry(
            shopId = shop.id,
            shopName = shop.shopName,
            entryType = entryType,
            amountPkr = amountPkr,
            paymentMethod = paymentMethod,
            senderNumber = senderNumber.trim(),
            receiverNumber = receiverNumber.trim(),
            referenceCode = referenceCode.trim().ifBlank { "KHATA-${(1000..9999).random()}" },
            notes = notes.trim(),
            timestampEpochMs = System.currentTimeMillis()
        )
        val entryId = dao.insertLedgerEntry(entry).toInt()
        cloudSyncManager.cacheLedgerEntryOfflineFirst(
            entry = entry.copy(id = entryId),
            changeDescriptionEn = "Auto-backed up & cached Khata ledger entry for ${shop.shopName} to Firestore.",
            changeDescriptionUr = "${shop.shopName} کا کھاتہ اندراج لوکل کیش اور کلاؤڈ پر محفوظ ہو گیا۔"
        )
    }

    suspend fun logCustomerInteraction(
        shopId: Int,
        shopName: String,
        interactionType: InteractionType,
        summaryNotes: String,
        nextActionNote: String
    ) {
        dao.insertInteractionLog(
            CustomerInteractionLog(
                shopId = shopId,
                shopName = shopName,
                interactionType = interactionType,
                summaryNotes = summaryNotes.trim(),
                nextActionNote = nextActionNote.trim(),
                timestampEpochMs = System.currentTimeMillis()
            )
        )
        cloudSyncManager.triggerAutomaticCloudBackup(
            changeDescriptionEn = "Auto-backed up CRM interaction for $shopName to Firestore.",
            changeDescriptionUr = "$shopName کا رابطہ ریکارڈ کلاؤڈ پر محفوظ ہو گیا۔"
        )
    }
}
