package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class OpticalCategory(
    val displayName: String,
    val urduName: String,
    val unitName: String = "Pieces",
    val urduUnitName: String = "پیس",
    val defaultThreshold: Int,
    val categoryNumberCode: String,
    val barcodePrefix: String
) {
    EYEGLASS_FRAMES("Eyeglass Frames", "عینک کے فریم", "Pieces", "پیس", 15, "CAT-10", "89640010"),
    OPHTHALMIC_LENSES("Ophthalmic Lenses", "نظر کے لینز", "Pieces", "پیس", 25, "CAT-20", "89640020"),
    CONTACT_LENSES("Contact Lenses (Eye Lenses)", "کانٹیکٹ آئی لینز", "Pieces", "پیس", 20, "CAT-30", "89640030")
}

const val DEALER_NUMBER_1 = "03176858707 Tariq Mehmood"
const val DEALER_NUMBER_2 = "03087321947 Tariq Mehmood"
val TARIQ_MEHMOOD_RECEIVER_NUMBERS = listOf(
    DEALER_NUMBER_1,
    DEALER_NUMBER_2
)

// Standard Optical Glasses Lenses SPH (Spherical) & CYL (Cylindrical) Diopter Presets
val OPTICAL_SPH_MINUS_OPTIONS = listOf(
    "0.00 (Plano)",
    "-0.25", "-0.50", "-0.75", "-1.00",
    "-1.25", "-1.50", "-1.75", "-2.00",
    "-2.25", "-2.50", "-2.75", "-3.00",
    "-3.25", "-3.50", "-3.75", "-4.00",
    "-4.50", "-5.00", "-5.50", "-6.00",
    "-7.00", "-8.00", "-10.00", "-12.00"
)

val OPTICAL_SPH_PLUS_OPTIONS = listOf(
    "0.00 (Plano)",
    "+0.25", "+0.50", "+0.75", "+1.00",
    "+1.25", "+1.50", "+1.75", "+2.00",
    "+2.25", "+2.50", "+2.75", "+3.00",
    "+3.25", "+3.50", "+4.00", "+4.50",
    "+5.00", "+6.00"
)

val OPTICAL_SPH_RANGE_OPTIONS = listOf(
    "0.00 (Plano)",
    "SPH -6.00 to +4.00",
    "SPH -8.00 to +4.00",
    "SPH -12.00 to +6.00",
    "SPH -0.50 to -8.00"
)

val OPTICAL_CYL_OPTIONS = listOf(
    "0.00 (Sph Only)",
    "-0.25", "-0.50", "-0.75", "-1.00",
    "-1.25", "-1.50", "-1.75", "-2.00",
    "-2.25", "-2.50", "-2.75", "-3.00",
    "-3.50", "-4.00",
    "+0.25", "+0.50", "+0.75", "+1.00",
    "+1.25", "+1.50", "+2.00",
    "CYL up to -2.00",
    "CYL up to -4.00"
)

fun formatSphCylPowerTag(sph: String, cyl: String): String {
    val cleanSph = sph.trim()
    val cleanCyl = cyl.trim()
    if (cleanSph.isEmpty() && cleanCyl.isEmpty()) return ""
    val sphPart = if (cleanSph.isNotEmpty()) {
        if (cleanSph.startsWith("SPH", ignoreCase = true)) cleanSph else "SPH $cleanSph"
    } else "SPH 0.00"
    val cylPart = if (cleanCyl.isNotEmpty()) {
        if (cleanCyl.startsWith("CYL", ignoreCase = true)) cleanCyl else "CYL $cleanCyl"
    } else "CYL 0.00"
    return "$sphPart / $cylPart"
}

fun mergeSphCylIntoNotes(baseText: String, sph: String, cyl: String): String {
    val powerTag = formatSphCylPowerTag(sph, cyl)
    if (powerTag.isEmpty()) return baseText.trim()
    val strippedBase = baseText
        .replace(Regex("""\[?SPH[^|\]]*(?:\||/)\s*CYL[^\]]*\]?\s*[•|—-]?\s*""", RegexOption.IGNORE_CASE), "")
        .trim()
    return if (strippedBase.isEmpty()) {
        "[$powerTag]"
    } else {
        "[$powerTag] • $strippedBase"
    }
}

fun extractSphFromText(text: String): String {
    val match = Regex("""SPH\s*:?\s*([^|/•\]]+)""", RegexOption.IGNORE_CASE).find(text)
    return match?.groupValues?.get(1)?.trim() ?: ""
}

fun extractCylFromText(text: String): String {
    val match = Regex("""CYL\s*:?\s*([^|/•\]]+)""", RegexOption.IGNORE_CASE).find(text)
    return match?.groupValues?.get(1)?.trim() ?: ""
}

fun extractSphCylBadgeFromText(text: String): String? {
    val bracketMatch = Regex("""\[(SPH[^\]]*CYL[^\]]*)]""", RegexOption.IGNORE_CASE).find(text)
    if (bracketMatch != null) return bracketMatch.groupValues[1].trim()
    val sph = extractSphFromText(text)
    val cyl = extractCylFromText(text)
    if (sph.isNotEmpty() || cyl.isNotEmpty()) {
        return formatSphCylPowerTag(sph, cyl)
    }
    return null
}

enum class PaymentMethod(
    val label: String,
    val urduLabel: String,
    val defaultReceiverNo: String
) {
    CASH("Cash", "نقد (Cash)", "03176858707 Tariq Mehmood (Cash)"),
    DEBT("Debt (Udhaar)", "ادھار / کھاتہ (Debt)", "03176858707 / 03087321947 Tariq Mehmood"),
    JAZZCASH("JazzCash", "جیز کیش (JazzCash)", "03176858707 Tariq Mehmood"),
    EASYPAISA("EasyPaisa", "ایزی پیسہ (EasyPaisa)", "03087321947 Tariq Mehmood")
}

enum class LedgerEntryType(
    val label: String,
    val urduLabel: String
) {
    DEBIT_INVOICE("Order Invoice / Debt (+)", "بل / ادھار اضافہ (+)"),
    CREDIT_PAYMENT("Payment Received (-)", "رقم وصولی (-)")
}

enum class RetailOrderStage(val label: String, val urduLabel: String, val stepIndex: Int) {
    RECEIVED("Order Received", "آرڈر موصول", 0),
    LAB_PROCESSING("Rx Lab / Picking", "لیب تیاری", 1),
    QUALITY_CHECK("Optical QC", "کوالٹی چیک", 2),
    DISPATCHED("Dispatched", "روانہ شدہ", 3),
    DELIVERED("Delivered", "پہنچا دیا گیا", 4);

    fun nextStage(): RetailOrderStage? = entries.find { it.stepIndex == this.stepIndex + 1 }
}

enum class RestockOrderStatus(val label: String, val urduLabel: String) {
    ORDERED("PO Placed", "آرڈر دیا گیا"),
    IN_TRANSIT("In Transit from Lab", "راستے میں"),
    RECEIVED_INTO_STOCK("Received (+Pieces Added)", "موصول (+پیس شامل)")
}

enum class InteractionType(val label: String, val urduLabel: String) {
    PHONE_CALL("Phone Call", "فون کال"),
    SHOWROOM_VISIT("Showroom Visit", "شوروم وزٹ"),
    WHATSAPP_FOLLOWUP("WhatsApp / Catalog Share", "واٹس ایپ رابطہ"),
    PAYMENT_COLLECTION("Payment / Ledger Settlement", "کھاتہ وصولی"),
    SAMPLE_DISPATCH("Sample Frame Tray Dispatch", "سیمپل ٹرے روانگی")
}

@Entity(tableName = "optical_products")
data class OpticalProduct(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sku: String,
    val barcode: String = "",   // EAN-13 / Code-128 Product Barcode
    val name: String,
    val brand: String,
    val category: OpticalCategory,
    val wholesalePrice: Double, // Per Piece in PKR
    val unitCostPkr: Double = (wholesalePrice * 0.65), // Per Piece Landed Cost in PKR
    val retailMsrp: Double,     // Per Piece Retail Price in PKR
    val minOrderQty: Int,       // Minimum Pieces per order
    val currentStock: Int,      // Managed strictly in Pieces
    val lowStockThreshold: Int, // Threshold in Pieces
    val primarySpec: String,
    val secondarySpec: String,
    val powerOrSizeRange: String,
    val supplierName: String,
    val leadTimeDays: Int = 5
) {
    val effectiveBarcode: String
        get() = barcode.ifBlank { sku }
    val isLowStock: Boolean
        get() = currentStock <= lowStockThreshold

    val isCriticalOut: Boolean
        get() = currentStock == 0

    val suggestedRestockQty: Int
        get() = maxOf(lowStockThreshold * 2 - currentStock, minOrderQty * 3, 20)

    val unitProfitPkr: Double
        get() = (wholesalePrice - unitCostPkr).coerceAtLeast(0.0)

    val profitMarginPercent: Double
        get() = if (wholesalePrice > 0) ((wholesalePrice - unitCostPkr) / wholesalePrice) * 100.0 else 0.0
}

@Entity(tableName = "category_threshold_configs")
data class CategoryThresholdConfig(
    @PrimaryKey val category: OpticalCategory,
    val thresholdUnits: Int,
    val pushAlertsEnabled: Boolean = true,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "stock_push_alerts")
data class StockPushAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val productSku: String,
    val productName: String,
    val category: OpticalCategory,
    val currentStock: Int,
    val threshold: Int,
    val triggerReason: String,
    val isRead: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "retailer_orders")
data class RetailerOrder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderNumber: String,
    val shopId: Int = 0,
    val retailerShopName: String,
    val retailerCity: String,
    val retailerContact: String,
    val productId: Int,
    val productSku: String,
    val productName: String,
    val category: OpticalCategory,
    val quantity: Int,              // Strictly in Pieces
    val unitWholesalePrice: Double, // Per Piece in PKR
    val unitCostPkr: Double,        // Per Piece in PKR
    val totalAmount: Double,        // Total PKR
    val customRxAndLabNotes: String,
    val paymentTerms: String,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val senderNumber: String = "",
    val receiverNumber: String = "",
    val paidAmountPkr: Double = totalAmount,
    val debtAmountPkr: Double = 0.0,
    val stage: RetailOrderStage,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    val totalCostPkr: Double
        get() = unitCostPkr * quantity

    val grossProfitPkr: Double
        get() = totalAmount - totalCostPkr

    val profitMarginPercent: Double
        get() = if (totalAmount > 0.0) (grossProfitPkr / totalAmount) * 100.0 else 0.0
}

@Entity(tableName = "restock_purchase_orders")
data class RestockPurchaseOrder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val poNumber: String,
    val productId: Int,
    val productSku: String,
    val productName: String,
    val category: OpticalCategory,
    val supplierName: String,
    val orderQuantity: Int,        // Pieces
    val estimatedUnitCost: Double, // Per Piece in PKR
    val totalCost: Double,         // Total PKR
    val manualNotes: String,
    val status: RestockOrderStatus,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "retailer_shop_profiles")
data class RetailerShopProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shopName: String,
    val cityAndMarket: String,
    val contactPerson: String,
    val phoneNumber: String,
    val email: String,
    val accountTier: String,
    val paymentTermsPreference: String,
    val creditLimitPkr: Double,
    val specialNotesAndPreferences: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "customer_ledger_entries")
data class CustomerLedgerEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shopId: Int,
    val shopName: String,
    val entryType: LedgerEntryType,
    val amountPkr: Double,
    val paymentMethod: PaymentMethod,
    val senderNumber: String,
    val receiverNumber: String,
    val referenceCode: String,
    val notes: String,
    val timestampEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "customer_interaction_logs")
data class CustomerInteractionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shopId: Int,
    val shopName: String,
    val interactionType: InteractionType,
    val summaryNotes: String,
    val nextActionNote: String,
    val timestampEpochMs: Long = System.currentTimeMillis()
)
