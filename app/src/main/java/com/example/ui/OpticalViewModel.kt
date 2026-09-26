package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CategoryThresholdConfig
import com.example.data.CustomerInteractionLog
import com.example.data.CustomerLedgerEntry
import com.example.data.InteractionType
import com.example.data.LedgerEntryType
import com.example.data.OpticalCategory
import com.example.data.OpticalProduct
import com.example.data.OpticalRepository
import com.example.data.PaymentMethod
import com.example.data.RestockOrderStatus
import com.example.data.RestockPurchaseOrder
import com.example.data.RetailOrderStage
import com.example.data.RetailerOrder
import com.example.data.RetailerShopProfile
import com.example.data.StockPushAlert
import com.example.util.CloudSyncUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class WholesaleTab(val title: String, val urduTitle: String) {
    CATALOG("Catalog", "کیٹلاگ"),
    INVENTORY("Stock & POs", "اسٹاک"),
    RETAIL_ORDERS("Orders", "آرڈرز"),
    SALES_REPORTS("Reports", "رپورٹس"),
    CRM_SHOPS("Ledger & CRM", "کھاتہ / CRM")
}

enum class SalesDateRangeFilter(val label: String, val urduLabel: String, val daysBack: Int?) {
    LAST_7_DAYS("Last 7 Days", "آخری 7 دن", 7),
    LAST_30_DAYS("Last 30 Days", "آخری 30 دن", 30),
    LAST_90_DAYS("Last 90 Days", "آخری 90 دن", 90),
    ALL_TIME("All Time", "مکمل ریکارڈ", null)
}

data class CategorySalesSummary(
    val category: OpticalCategory,
    val orderCount: Int,
    val unitsSold: Int, // Pieces Sold
    val revenuePkr: Double,
    val costPkr: Double,
    val profitPkr: Double,
    val marginPercent: Double
)

data class ProductSalesSummary(
    val productId: Int,
    val sku: String,
    val productName: String,
    val category: OpticalCategory,
    val orderCount: Int,
    val unitsSold: Int, // Pieces Sold
    val revenuePkr: Double,
    val costPkr: Double,
    val profitPkr: Double,
    val marginPercent: Double
)

data class MonthlyRevenuePoint(
    val monthKey: String,
    val monthLabel: String,
    val urduMonthLabel: String,
    val revenuePkr: Double,
    val profitPkr: Double,
    val costPkr: Double,
    val ordersCount: Int,
    val piecesSold: Int,
    val framesRevenuePkr: Double,
    val ophthalmicRevenuePkr: Double,
    val contactLensesRevenuePkr: Double,
    val momGrowthPercent: Double
)

enum class InsightPriority {
    TOP_CATEGORY,
    REVENUE_MOMENTUM,
    RESTOCK_ACTION,
    KHATA_CASHFLOW
}

data class DealerBusinessInsight(
    val id: String,
    val priority: InsightPriority,
    val titleEn: String,
    val titleUr: String,
    val detailEn: String,
    val detailUr: String,
    val metricBadgeEn: String,
    val metricBadgeUr: String,
    val targetCategory: OpticalCategory? = null
)

data class WholesaleDashboardUiState(
    val allProducts: List<OpticalProduct> = emptyList(),
    val filteredCatalogProducts: List<OpticalProduct> = emptyList(),
    val lowStockProducts: List<OpticalProduct> = emptyList(),
    val categoryThresholds: List<CategoryThresholdConfig> = emptyList(),
    val pushAlerts: List<StockPushAlert> = emptyList(),
    val unreadPushAlertCount: Int = 0,
    val retailerOrders: List<RetailerOrder> = emptyList(),
    val allRetailerOrdersUnfiltered: List<RetailerOrder> = emptyList(),
    val restockOrders: List<RestockPurchaseOrder> = emptyList(),
    val shopProfiles: List<RetailerShopProfile> = emptyList(),
    val ledgerEntries: List<CustomerLedgerEntry> = emptyList(),
    val interactionLogs: List<CustomerInteractionLog> = emptyList(),
    val totalCustomerDebtPkr: Double = 0.0,
    val totalLedgerCollectedPkr: Double = 0.0,
    val searchQuery: String = "",
    val selectedCategory: OpticalCategory? = null,
    val showOnlyLowStockInInventory: Boolean = false,
    val selectedOrderStageFilter: RetailOrderStage? = null,
    val totalStockValueWholesale: Double = 0.0,
    val totalUnitsInStock: Int = 0, // Total Pieces in Stock
    val activeRetailRevenue: Double = 0.0,
    val pendingSupplierPoCount: Int = 0,
    // Sales Reporting State
    val reportDateRange: SalesDateRangeFilter = SalesDateRangeFilter.ALL_TIME,
    val reportCategoryFilter: OpticalCategory? = null,
    val reportProductIdFilter: Int? = null,
    val reportFilteredOrders: List<RetailerOrder> = emptyList(),
    val reportTotalRevenuePkr: Double = 0.0,
    val reportTotalCostPkr: Double = 0.0,
    val reportTotalProfitPkr: Double = 0.0,
    val reportProfitMarginPercent: Double = 0.0,
    val reportTotalUnitsSold: Int = 0, // Total Pieces Sold
    val categorySalesSummaries: List<CategorySalesSummary> = emptyList(),
    val productSalesSummaries: List<ProductSalesSummary> = emptyList(),
    val monthlyRevenueTrends: List<MonthlyRevenuePoint> = emptyList(),
    val dealerBusinessInsights: List<DealerBusinessInsight> = emptyList()
)

class OpticalViewModel(private val repository: OpticalRepository) : ViewModel() {

    private val _selectedTab = MutableStateFlow(WholesaleTab.CATALOG)
    val selectedTab: StateFlow<WholesaleTab> = _selectedTab.asStateFlow()

    private val _isUrduLanguage = MutableStateFlow(false)
    val isUrduLanguage: StateFlow<Boolean> = _isUrduLanguage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<OpticalCategory?>(null)
    private val _showOnlyLowStock = MutableStateFlow(false)
    private val _selectedOrderStage = MutableStateFlow<RetailOrderStage?>(null)

    // Sales report filter states
    private val _reportDateRange = MutableStateFlow(SalesDateRangeFilter.ALL_TIME)
    private val _reportCategoryFilter = MutableStateFlow<OpticalCategory?>(null)
    private val _reportProductIdFilter = MutableStateFlow<Int?>(null)

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    val cloudSyncState: StateFlow<CloudSyncUiState> = repository.cloudSyncManager.syncState

    init {
        viewModelScope.launch {
            repository.ensureSeedData()
            repository.cloudSyncManager.initializeFirestoreAndSyncIfAvailable()
        }
    }

    private data class CoreDataBundle(
        val products: List<OpticalProduct>,
        val catThresholds: List<CategoryThresholdConfig>,
        val pushAlerts: List<StockPushAlert>,
        val retailOrders: List<RetailerOrder>,
        val restockOrders: List<RestockPurchaseOrder>,
        val shopProfiles: List<RetailerShopProfile>,
        val ledgerEntries: List<CustomerLedgerEntry>,
        val interactionLogs: List<CustomerInteractionLog>
    )

    private data class UiFilterBundle(
        val query: String,
        val category: OpticalCategory?,
        val lowOnly: Boolean,
        val stage: RetailOrderStage?,
        val reportDateRange: SalesDateRangeFilter,
        val reportCategory: OpticalCategory?,
        val reportProductId: Int?
    )

    private val coreDataFlow = combine(
        combine(
            repository.allProducts,
            repository.allCategoryThresholds,
            repository.allPushAlerts,
            repository.allRetailerOrders
        ) { products, thresholds, alerts, orders ->
            Quad(products, thresholds, alerts, orders)
        },
        combine(
            repository.allRestockOrders,
            repository.allShopProfiles,
            repository.allLedgerEntries,
            repository.allInteractionLogs
        ) { restock, shops, ledger, logs ->
            Quad(restock, shops, ledger, logs)
        }
    ) { firstQuad, secondQuad ->
        CoreDataBundle(
            products = firstQuad.first,
            catThresholds = firstQuad.second,
            pushAlerts = firstQuad.third,
            retailOrders = firstQuad.fourth,
            restockOrders = secondQuad.first,
            shopProfiles = secondQuad.second,
            ledgerEntries = secondQuad.third,
            interactionLogs = secondQuad.fourth
        )
    }

    private val filterFlow = combine(
        combine(_searchQuery, _selectedCategory, _showOnlyLowStock, _selectedOrderStage) { q, cat, low, stg ->
            Quad(q, cat, low, stg)
        },
        combine(_reportDateRange, _reportCategoryFilter, _reportProductIdFilter) { dr, rCat, rProd ->
            Triple(dr, rCat, rProd)
        }
    ) { first, second ->
        UiFilterBundle(
            query = first.first,
            category = first.second,
            lowOnly = first.third,
            stage = first.fourth,
            reportDateRange = second.first,
            reportCategory = second.second,
            reportProductId = second.third
        )
    }

    val uiState: StateFlow<WholesaleDashboardUiState> = combine(
        coreDataFlow,
        filterFlow
    ) { data, filters ->
        val query = filters.query.trim().lowercase()
        val filteredProducts = data.products.filter { p ->
            val matchesCat = filters.category == null || p.category == filters.category
            val matchesQuery = query.isEmpty() ||
                p.name.lowercase().contains(query) ||
                p.sku.lowercase().contains(query) ||
                p.barcode.lowercase().contains(query) ||
                p.brand.lowercase().contains(query) ||
                p.primarySpec.lowercase().contains(query) ||
                p.secondarySpec.lowercase().contains(query)
            matchesCat && matchesQuery
        }

        val lowStockList = data.products.filter { it.isLowStock }

        val filteredRetailOrders = if (filters.stage == null) {
            data.retailOrders
        } else {
            data.retailOrders.filter { it.stage == filters.stage }
        }

        val totalWholesaleVal = data.products.sumOf { it.currentStock * it.wholesalePrice }
        val totalUnits = data.products.sumOf { it.currentStock }
        val retailRev = data.retailOrders.sumOf { it.totalAmount }
        val pendingPo = data.restockOrders.count { it.status != RestockOrderStatus.RECEIVED_INTO_STOCK }

        val totalDebit = data.ledgerEntries
            .filter { it.entryType == LedgerEntryType.DEBIT_INVOICE }
            .sumOf { it.amountPkr }
        val totalCredit = data.ledgerEntries
            .filter { it.entryType == LedgerEntryType.CREDIT_PAYMENT }
            .sumOf { it.amountPkr }
        val outstandingDebt = (totalDebit - totalCredit).coerceAtLeast(0.0)

        // --- Sales Reporting Calculations ---
        val now = System.currentTimeMillis()
        val cutoffMs = filters.reportDateRange.daysBack?.let { days ->
            now - days * 86_400_000L
        }

        val reportOrders = data.retailOrders.filter { order ->
            val matchesDate = cutoffMs == null || order.createdAtEpochMs >= cutoffMs
            val matchesCat = filters.reportCategory == null || order.category == filters.reportCategory
            val matchesProduct = filters.reportProductId == null || order.productId == filters.reportProductId
            matchesDate && matchesCat && matchesProduct
        }

        val repRevenue = reportOrders.sumOf { it.totalAmount }
        val repCost = reportOrders.sumOf { it.totalCostPkr }
        val repProfit = repRevenue - repCost
        val repMargin = if (repRevenue > 0.0) (repProfit / repRevenue) * 100.0 else 0.0
        val repUnits = reportOrders.sumOf { it.quantity }

        val catSummaries = OpticalCategory.entries.map { cat ->
            val catOrders = reportOrders.filter { it.category == cat }
            val cRev = catOrders.sumOf { it.totalAmount }
            val cCost = catOrders.sumOf { it.totalCostPkr }
            val cProf = cRev - cCost
            val cMargin = if (cRev > 0.0) (cProf / cRev) * 100.0 else 0.0
            CategorySalesSummary(
                category = cat,
                orderCount = catOrders.size,
                unitsSold = catOrders.sumOf { it.quantity },
                revenuePkr = cRev,
                costPkr = cCost,
                profitPkr = cProf,
                marginPercent = cMargin
            )
        }

        val prodSummaries = reportOrders
            .groupBy { it.productId }
            .map { (prodId, pOrders) ->
                val sample = pOrders.first()
                val pRev = pOrders.sumOf { it.totalAmount }
                val pCost = pOrders.sumOf { it.totalCostPkr }
                val pProf = pRev - pCost
                val pMarg = if (pRev > 0.0) (pProf / pRev) * 100.0 else 0.0
                ProductSalesSummary(
                    productId = prodId,
                    sku = sample.productSku,
                    productName = sample.productName,
                    category = sample.category,
                    orderCount = pOrders.size,
                    unitsSold = pOrders.sumOf { it.quantity },
                    revenuePkr = pRev,
                    costPkr = pCost,
                    profitPkr = pProf,
                    marginPercent = pMarg
                )
            }
            .sortedByDescending { it.revenuePkr }

        // --- 6-Month Revenue Trends (Recharts Data Series in PKR) ---
        val monthShortFmt = SimpleDateFormat("MMM", Locale.US)
        val monthKeyFmt = SimpleDateFormat("yyyy-MM", Locale.US)
        val urduMonths = mapOf(
            "Jan" to "جنوری",
            "Feb" to "فروری",
            "Mar" to "مارچ",
            "Apr" to "اپریل",
            "May" to "مئی",
            "Jun" to "جون",
            "Jul" to "جولائی",
            "Aug" to "اگست",
            "Sep" to "ستمبر",
            "Oct" to "اکتوبر",
            "Nov" to "نومبر",
            "Dec" to "دسمبر"
        )

        val ordersForTrend = data.retailOrders.filter { order ->
            val matchesCat = filters.reportCategory == null || order.category == filters.reportCategory
            val matchesProduct = filters.reportProductId == null || order.productId == filters.reportProductId
            matchesCat && matchesProduct
        }

        val rawMonthlyPoints = (5 downTo 0).map { monthsAgo ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MONTH, -monthsAgo)
            }
            val startMs = cal.timeInMillis
            val monthShort = monthShortFmt.format(cal.time)
            val mKey = monthKeyFmt.format(cal.time)
            cal.add(Calendar.MONTH, 1)
            val endMs = cal.timeInMillis

            val monthOrders = ordersForTrend.filter { it.createdAtEpochMs in startMs until endMs }
            val mRev = monthOrders.sumOf { it.totalAmount }
            val mCost = monthOrders.sumOf { it.totalCostPkr }
            val mProf = mRev - mCost
            val framesRev = monthOrders
                .filter { it.category == OpticalCategory.EYEGLASS_FRAMES }
                .sumOf { it.totalAmount }
            val ophRev = monthOrders
                .filter { it.category == OpticalCategory.OPHTHALMIC_LENSES }
                .sumOf { it.totalAmount }
            val clRev = monthOrders
                .filter { it.category == OpticalCategory.CONTACT_LENSES }
                .sumOf { it.totalAmount }

            MonthlyRevenuePoint(
                monthKey = mKey,
                monthLabel = monthShort,
                urduMonthLabel = urduMonths[monthShort] ?: monthShort,
                revenuePkr = mRev,
                profitPkr = mProf,
                costPkr = mCost,
                ordersCount = monthOrders.size,
                piecesSold = monthOrders.sumOf { it.quantity },
                framesRevenuePkr = framesRev,
                ophthalmicRevenuePkr = ophRev,
                contactLensesRevenuePkr = clRev,
                momGrowthPercent = 0.0
            )
        }

        val monthlyTrends = rawMonthlyPoints.mapIndexed { idx, pt ->
            val prevRev = if (idx > 0) rawMonthlyPoints[idx - 1].revenuePkr else 0.0
            val growth = if (prevRev > 0.0) {
                ((pt.revenuePkr - prevRev) / prevRev) * 100.0
            } else 0.0
            pt.copy(momGrowthPercent = growth)
        }

        // --- Actionable Dealer Business Insights (PKR) ---
        val topCategorySummary = catSummaries.maxByOrNull { it.revenuePkr }
        val topMarginCategory = catSummaries.maxByOrNull { it.marginPercent }
        val latestMonth = monthlyTrends.lastOrNull()
        val prevMonth = if (monthlyTrends.size >= 2) monthlyTrends[monthlyTrends.size - 2] else null
        val topProductSummary = prodSummaries.firstOrNull()

        val insights = buildList {
            if (topCategorySummary != null && topCategorySummary.revenuePkr > 0) {
                val sharePct = if (repRevenue > 0) (topCategorySummary.revenuePkr / repRevenue) * 100.0 else 0.0
                add(
                    DealerBusinessInsight(
                        id = "top_category_leader",
                        priority = InsightPriority.TOP_CATEGORY,
                        titleEn = "Top Revenue Driver: ${topCategorySummary.category.displayName}",
                        titleUr = "سب سے زیادہ آمدنی والی کیٹیگری: ${topCategorySummary.category.urduName}",
                        detailEn = "${topCategorySummary.category.displayName} generated PKR ${String.format(Locale.US, "%,.0f", topCategorySummary.revenuePkr)} (${String.format(Locale.US, "%.1f", sharePct)}% of sales across ${topCategorySummary.unitsSold} Pieces). Bundle with ${topMarginCategory?.category?.displayName ?: "frames"} (${String.format(Locale.US, "%.1f", topMarginCategory?.marginPercent ?: 35.0)}% margin) to maximize wholesale order value.",
                        detailUr = "${topCategorySummary.category.urduName} نے ${topCategorySummary.unitsSold} پیس کی فروخت سے PKR ${String.format(Locale.US, "%,.0f", topCategorySummary.revenuePkr)} (${String.format(Locale.US, "%.1f", sharePct)}%) کمائے۔ منافع بڑھانے کے لیے اسے دیگر کیٹیگریز کے ساتھ بنڈل کریں۔",
                        metricBadgeEn = "${String.format(Locale.US, "%.1f", sharePct)}% Share • ${String.format(Locale.US, "%.1f", topCategorySummary.marginPercent)}% Margin",
                        metricBadgeUr = "${String.format(Locale.US, "%.1f", sharePct)}% حصہ • ${String.format(Locale.US, "%.1f", topCategorySummary.marginPercent)}% منافع",
                        targetCategory = topCategorySummary.category
                    )
                )
            }

            if (latestMonth != null && prevMonth != null) {
                val growthSign = if (latestMonth.momGrowthPercent >= 0) "+" else ""
                val growthText = "$growthSign${String.format(Locale.US, "%.1f", latestMonth.momGrowthPercent)}%"
                add(
                    DealerBusinessInsight(
                        id = "monthly_momentum",
                        priority = InsightPriority.REVENUE_MOMENTUM,
                        titleEn = "Monthly Revenue Trend (${latestMonth.monthLabel} vs ${prevMonth.monthLabel}): $growthText",
                        titleUr = "ماہانہ فروخت کا رجحان (${latestMonth.urduMonthLabel} بمقابلہ ${prevMonth.urduMonthLabel}): $growthText",
                        detailEn = "${latestMonth.monthLabel} wholesale revenue reached PKR ${String.format(Locale.US, "%,.0f", latestMonth.revenuePkr)} (${latestMonth.piecesSold} Pieces sold) vs PKR ${String.format(Locale.US, "%,.0f", prevMonth.revenuePkr)} in ${prevMonth.monthLabel}. Best-selling SKU: ${topProductSummary?.sku ?: "LNS-167-BLC"} (${topProductSummary?.unitsSold ?: 0} Pieces).",
                        detailUr = "${latestMonth.urduMonthLabel} میں فروخت PKR ${String.format(Locale.US, "%,.0f", latestMonth.revenuePkr)} (${latestMonth.piecesSold} پیس) رہی جبکہ ${prevMonth.urduMonthLabel} میں PKR ${String.format(Locale.US, "%,.0f", prevMonth.revenuePkr)} تھی۔ سب سے زیادہ فروخت: ${topProductSummary?.sku ?: "LNS-167-BLC"}۔",
                        metricBadgeEn = "$growthText MoM • ${latestMonth.ordersCount} Orders",
                        metricBadgeUr = "$growthText ماہانہ • ${latestMonth.ordersCount} آرڈرز",
                        targetCategory = topProductSummary?.category
                    )
                )
            }

            if (lowStockList.isNotEmpty()) {
                val atRiskRevenuePkr = lowStockList.sumOf { it.suggestedRestockQty * it.wholesalePrice }
                val urgentSkus = lowStockList.take(3).joinToString(", ") { "${it.sku} (${it.currentStock} Pcs)" }
                add(
                    DealerBusinessInsight(
                        id = "restock_revenue_protection",
                        priority = InsightPriority.RESTOCK_ACTION,
                        titleEn = "Protect PKR ${String.format(Locale.US, "%,.0f", atRiskRevenuePkr)} in Upcoming Wholesale Demand",
                        titleUr = "ممکنہ فروخت (PKR ${String.format(Locale.US, "%,.0f", atRiskRevenuePkr)}) بچانے کے لیے فوری اسٹاک منگوائیں",
                        detailEn = "${lowStockList.size} high-velocity SKUs ($urgentSkus) are at or below threshold. Placing restock POs now prevents stockouts for repeat retail shop orders.",
                        detailUr = "${lowStockList.size} اہم پراڈکٹس ($urgentSkus) کم اسٹاک کی حد سے نیچے ہیں۔ ریٹیل شاپس کے آرڈرز متاثر ہونے سے بچانے کے لیے فوری سپلائر آرڈر دیں۔",
                        metricBadgeEn = "${lowStockList.size} Low-Stock SKUs",
                        metricBadgeUr = "${lowStockList.size} کم اسٹاک پراڈکٹس",
                        targetCategory = lowStockList.first().category
                    )
                )
            }

            val totalBilledLedger = totalDebit.coerceAtLeast(1.0)
            val collectionRate = ((totalCredit / totalBilledLedger) * 100.0).coerceIn(0.0, 100.0)
            add(
                DealerBusinessInsight(
                    id = "khata_recovery_insight",
                    priority = InsightPriority.KHATA_CASHFLOW,
                    titleEn = "Khata Collection Rate: ${String.format(Locale.US, "%.1f", collectionRate)}% Settled",
                    titleUr = "کھاتہ وصولی کی شرح: ${String.format(Locale.US, "%.1f", collectionRate)}% رقم وصول شدہ",
                    detailEn = "PKR ${String.format(Locale.US, "%,.0f", totalCredit)} collected against PKR ${String.format(Locale.US, "%,.0f", totalDebit)} billed. Follow up on PKR ${String.format(Locale.US, "%,.0f", outstandingDebt)} outstanding Udhaar via JazzCash (03176858707) or EasyPaisa (03087321947 Tariq Mehmood).",
                    detailUr = "کل بل PKR ${String.format(Locale.US, "%,.0f", totalDebit)} میں سے PKR ${String.format(Locale.US, "%,.0f", totalCredit)} وصول ہو چکے ہیں۔ بقایا ادھار PKR ${String.format(Locale.US, "%,.0f", outstandingDebt)} کی جیز کیش (03176858707) یا ایزی پیسہ (03087321947 طارق محمود) پر وصولی کریں۔",
                    metricBadgeEn = "PKR ${String.format(Locale.US, "%,.0f", outstandingDebt)} Udhaar",
                    metricBadgeUr = "PKR ${String.format(Locale.US, "%,.0f", outstandingDebt)} بقایا ادھار"
                )
            )
        }

        WholesaleDashboardUiState(
            allProducts = data.products,
            filteredCatalogProducts = filteredProducts,
            lowStockProducts = lowStockList,
            categoryThresholds = data.catThresholds,
            pushAlerts = data.pushAlerts,
            unreadPushAlertCount = data.pushAlerts.count { !it.isRead },
            retailerOrders = filteredRetailOrders,
            allRetailerOrdersUnfiltered = data.retailOrders,
            restockOrders = data.restockOrders,
            shopProfiles = data.shopProfiles,
            ledgerEntries = data.ledgerEntries,
            interactionLogs = data.interactionLogs,
            totalCustomerDebtPkr = outstandingDebt,
            totalLedgerCollectedPkr = totalCredit,
            searchQuery = filters.query,
            selectedCategory = filters.category,
            showOnlyLowStockInInventory = filters.lowOnly,
            selectedOrderStageFilter = filters.stage,
            totalStockValueWholesale = totalWholesaleVal,
            totalUnitsInStock = totalUnits,
            activeRetailRevenue = retailRev,
            pendingSupplierPoCount = pendingPo,
            reportDateRange = filters.reportDateRange,
            reportCategoryFilter = filters.reportCategory,
            reportProductIdFilter = filters.reportProductId,
            reportFilteredOrders = reportOrders,
            reportTotalRevenuePkr = repRevenue,
            reportTotalCostPkr = repCost,
            reportTotalProfitPkr = repProfit,
            reportProfitMarginPercent = repMargin,
            reportTotalUnitsSold = repUnits,
            categorySalesSummaries = catSummaries,
            productSalesSummaries = prodSummaries,
            monthlyRevenueTrends = monthlyTrends,
            dealerBusinessInsights = insights
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WholesaleDashboardUiState()
    )

    private data class Quad<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    fun selectTab(tab: WholesaleTab) {
        _selectedTab.value = tab
    }

    fun toggleUrduLanguage() {
        _isUrduLanguage.value = !_isUrduLanguage.value
        _statusBannerMessage.value = if (_isUrduLanguage.value) {
            "اردو زبان فعال کر دی گئی ہے (Urdu Language Enabled)"
        } else {
            "English language enabled"
        }
    }

    fun jumpToLowStockAlerts() {
        _showOnlyLowStock.value = true
        _selectedTab.value = WholesaleTab.INVENTORY
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: OpticalCategory?) {
        _selectedCategory.value = category
    }

    fun toggleLowStockOnly(lowOnly: Boolean) {
        _showOnlyLowStock.value = lowOnly
    }

    fun selectOrderStageFilter(stage: RetailOrderStage?) {
        _selectedOrderStage.value = stage
    }

    // --- Sales Report Filter Actions ---
    fun selectReportDateRange(range: SalesDateRangeFilter) {
        _reportDateRange.value = range
    }

    fun selectReportCategoryFilter(category: OpticalCategory?) {
        _reportCategoryFilter.value = category
        val currentProdId = _reportProductIdFilter.value
        if (category != null && currentProdId != null) {
            val prod = uiState.value.allProducts.find { it.id == currentProdId }
            if (prod != null && prod.category != category) {
                _reportProductIdFilter.value = null
            }
        }
    }

    fun selectReportProductFilter(productId: Int?) {
        _reportProductIdFilter.value = productId
    }

    fun buildSalesReportCsv(): String {
        val state = uiState.value
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val sb = StringBuilder()

        sb.appendLine("TARIQ JADDAH OPTICAL - B2B WHOLESALE SALES REPORT (PKR - PER PIECE)")
        sb.appendLine("Date Range Filter,${state.reportDateRange.label}")
        sb.appendLine("Category Filter,${state.reportCategoryFilter?.displayName ?: "All Categories"}")
        val selectedProductSku = state.allProducts.find { it.id == state.reportProductIdFilter }?.sku ?: "All SKUs"
        sb.appendLine("Product Filter,$selectedProductSku")
        sb.appendLine("Total Revenue (PKR),${state.reportTotalRevenuePkr.toLong()}")
        sb.appendLine("Total Landed Cost (PKR),${state.reportTotalCostPkr.toLong()}")
        sb.appendLine("Total Gross Profit (PKR),${state.reportTotalProfitPkr.toLong()}")
        sb.appendLine("Profit Margin (%),${String.format(Locale.US, "%.2f", state.reportProfitMarginPercent)}%")
        sb.appendLine("Total Pieces Sold,${state.reportTotalUnitsSold}")
        sb.appendLine()

        sb.appendLine("--- SUMMARY BY OPTICAL CATEGORY (PIECES) ---")
        sb.appendLine("Category,Orders,Pieces Sold,Revenue (PKR),Cost (PKR),Gross Profit (PKR),Margin (%)")
        state.categorySalesSummaries.forEach { cat ->
            sb.appendLine(
                "\"${cat.category.displayName}\",${cat.orderCount},${cat.unitsSold},${cat.revenuePkr.toLong()},${cat.costPkr.toLong()},${cat.profitPkr.toLong()},${String.format(Locale.US, "%.2f", cat.marginPercent)}%"
            )
        }
        sb.appendLine()

        sb.appendLine("--- SUMMARY BY INDIVIDUAL PRODUCT SKU (PIECES) ---")
        sb.appendLine("SKU,Product Name,Category,Orders,Pieces Sold,Revenue (PKR),Cost (PKR),Gross Profit (PKR),Margin (%)")
        state.productSalesSummaries.forEach { prod ->
            sb.appendLine(
                "\"${prod.sku}\",\"${prod.productName}\",\"${prod.category.displayName}\",${prod.orderCount},${prod.unitsSold},${prod.revenuePkr.toLong()},${prod.costPkr.toLong()},${prod.profitPkr.toLong()},${String.format(Locale.US, "%.2f", prod.marginPercent)}%"
            )
        }
        sb.appendLine()

        sb.appendLine("--- INDIVIDUAL RETAILER ORDER TRANSACTIONS (PER PIECE) ---")
        sb.appendLine("Order No,Date,Retailer Shop,City,SKU,Product Name,Category,Pieces,Unit Price (PKR),Total Revenue (PKR),Payment Method,Sender No,Receiver No,Gross Profit (PKR),Margin (%),Stage")
        state.reportFilteredOrders.forEach { order ->
            val dateStr = dateFormat.format(Date(order.createdAtEpochMs))
            sb.appendLine(
                "\"${order.orderNumber}\",\"$dateStr\",\"${order.retailerShopName}\",\"${order.retailerCity}\",\"${order.productSku}\",\"${order.productName}\",\"${order.category.displayName}\",${order.quantity},${order.unitWholesalePrice.toLong()},${order.totalAmount.toLong()},\"${order.paymentMethod.label}\",\"${order.senderNumber}\",\"${order.receiverNumber}\",${order.grossProfitPkr.toLong()},${String.format(Locale.US, "%.1f", order.profitMarginPercent)}%,\"${order.stage.label}\""
            )
        }
        return sb.toString()
    }

    fun notifyStatus(message: String) {
        _statusBannerMessage.value = message
    }

    fun findProductByScannedCode(rawCode: String): OpticalProduct? {
        val cleaned = rawCode.trim()
        if (cleaned.isEmpty()) return null
        val products = uiState.value.allProducts
        return products.firstOrNull {
            it.barcode.equals(cleaned, ignoreCase = true) ||
                it.sku.equals(cleaned, ignoreCase = true)
        } ?: products.firstOrNull {
            it.barcode.contains(cleaned, ignoreCase = true) ||
                it.sku.contains(cleaned, ignoreCase = true)
        }
    }

    fun dismissBannerMessage() {
        _statusBannerMessage.value = null
    }

    fun saveOrUpdateProduct(product: OpticalProduct) {
        viewModelScope.launch {
            repository.saveProduct(product)
            val lowNote = if (product.isLowStock) " • Low-stock push notification dispatched!" else ""
            _statusBannerMessage.value =
                "Saved Product ${product.sku} (PKR ${product.wholesalePrice.toLong()}/Piece)$lowNote"
        }
    }

    fun deleteProduct(product: OpticalProduct) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            _statusBannerMessage.value = if (_isUrduLanguage.value) {
                "پراڈکٹ ${product.sku} (${product.name}) کیٹلاگ سے حذف کر دی گئی۔"
            } else {
                "Deleted product ${product.sku} (${product.name}) from catalog."
            }
        }
    }

    fun deleteAllProducts() {
        viewModelScope.launch {
            repository.deleteAllProducts()
            _statusBannerMessage.value = if (_isUrduLanguage.value) {
                "تمام پراڈکٹس صاف کر دی گئیں۔ اب آپ اپنی پراڈکٹس شامل کر سکتے ہیں۔"
            } else {
                "All products deleted. You can now add your own products."
            }
        }
    }

    fun addNewStockPieces(
        product: OpticalProduct,
        addedPieces: Int,
        updatedWholesalePricePkr: Double,
        updatedUnitCostPkr: Double,
        batchNotes: String
    ) {
        viewModelScope.launch {
            repository.addNewStockPieces(
                product = product,
                addedPieces = addedPieces,
                updatedWholesalePricePkr = updatedWholesalePricePkr,
                updatedUnitCostPkr = updatedUnitCostPkr,
                batchNotes = batchNotes
            )
            val newTotal = product.currentStock + addedPieces
            _statusBannerMessage.value =
                "Added +$addedPieces Pieces to ${product.sku}. New Stock: $newTotal Pieces."
        }
    }

    fun adjustProductStockAndThreshold(product: OpticalProduct, newStock: Int, newThreshold: Int) {
        viewModelScope.launch {
            repository.updateStockAndThreshold(product.id, newStock, newThreshold)
            val alertStatus = if (newStock <= newThreshold) " • Push notification alert triggered!" else ""
            _statusBannerMessage.value =
                "Updated ${product.sku}: Stock = $newStock Pieces, Threshold ≤ $newThreshold Pieces$alertStatus"
        }
    }

    fun updateCategoryThreshold(
        category: OpticalCategory,
        newThreshold: Int,
        pushEnabled: Boolean,
        applyToAllProductsInCategory: Boolean
    ) {
        viewModelScope.launch {
            val lowCount = repository.updateCategoryThresholdAndApply(
                category = category,
                newThreshold = newThreshold,
                pushEnabled = pushEnabled,
                applyToAllProductsInCategory = applyToAllProductsInCategory
            )
            _statusBannerMessage.value = if (applyToAllProductsInCategory) {
                "Applied threshold ≤ $newThreshold Pieces to ${category.displayName} ($lowCount SKUs alerted via Push)."
            } else {
                "Saved default threshold ≤ $newThreshold Pieces for ${category.displayName}."
            }
        }
    }

    fun triggerLowStockPushBroadcast() {
        val lowList = uiState.value.lowStockProducts
        viewModelScope.launch {
            repository.triggerPushScanForAllLowStock(lowList)
            _statusBannerMessage.value =
                "Dispatched push notifications for ${lowList.size} SKUs below threshold."
        }
    }

    fun markAllPushAlertsRead() {
        viewModelScope.launch {
            repository.markAllPushAlertsRead()
        }
    }

    fun clearAllPushAlerts() {
        viewModelScope.launch {
            repository.clearAllPushAlerts()
            _statusBannerMessage.value = "Cleared push notification alert history."
        }
    }

    fun createRetailerOrder(
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
        paidAmountPkr: Double,
        onOrderCreated: (RetailerOrder) -> Unit = {}
    ) {
        viewModelScope.launch {
            val savedOrder = repository.createRetailerOrderAndDeductStock(
                shopId = shopId,
                retailerShopName = retailerShopName,
                retailerCity = retailerCity,
                retailerContact = retailerContact,
                product = product,
                quantity = quantity,
                customRxAndLabNotes = customRxAndLabNotes,
                paymentMethod = paymentMethod,
                senderNumber = senderNumber,
                receiverNumber = receiverNumber,
                paidAmountPkr = paidAmountPkr
            )
            val remaining = (product.currentStock - quantity).coerceAtLeast(0)
            val lowNotice = if (product.id != 0 && remaining <= product.lowStockThreshold) {
                " • Push notification sent ($remaining Pieces left ≤ ${product.lowStockThreshold})!"
            } else ""
            _statusBannerMessage.value =
                "New Order ${savedOrder.orderNumber} logged for $retailerShopName ($quantity Pieces × ${savedOrder.productSku} via ${paymentMethod.label})$lowNotice"
            onOrderCreated(savedOrder)
        }
    }

    fun createCounterSale(
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
        paidAmountPkr: Double,
        onCounterSaleCreated: (RetailerOrder) -> Unit = {}
    ) {
        viewModelScope.launch {
            val savedSale = repository.createCounterSaleAndDeductStock(
                customerName = customerName,
                customerPhone = customerPhone,
                product = product,
                quantity = quantity,
                unitSalePricePkr = unitSalePricePkr,
                discountPkr = discountPkr,
                customRxAndLabNotes = customRxAndLabNotes,
                paymentMethod = paymentMethod,
                senderNumber = senderNumber,
                receiverNumber = receiverNumber,
                paidAmountPkr = paidAmountPkr
            )
            _statusBannerMessage.value = if (_isUrduLanguage.value) {
                "کاؤنٹر سیل مکمل (${savedSale.orderNumber}): $quantity پیس × ${savedSale.productSku} — کل بل PKR ${savedSale.totalAmount.toLong()}"
            } else {
                "Counter Sale Completed (${savedSale.orderNumber}): $quantity Pcs × ${savedSale.productSku} — Total PKR ${savedSale.totalAmount.toLong()}"
            }
            onCounterSaleCreated(savedSale)
        }
    }

    fun advanceRetailerOrderStage(order: RetailerOrder) {
        viewModelScope.launch {
            val next = order.stage.nextStage() ?: return@launch
            repository.advanceRetailerOrderStage(order)
            _statusBannerMessage.value =
                "Order ${order.orderNumber} (${order.retailerShopName}) moved to ${next.label}."
        }
    }

    fun placeManualRestockOrder(
        product: OpticalProduct,
        orderQuantity: Int,
        manualNotes: String
    ) {
        viewModelScope.launch {
            repository.placeManualRestockOrder(product, orderQuantity, manualNotes)
            _statusBannerMessage.value =
                "Manual Supplier PO placed: $orderQuantity Pieces × ${product.sku} from ${product.supplierName}."
        }
    }

    fun placeBatchRestockForLowStockItems(lowStockProducts: List<OpticalProduct>) {
        if (lowStockProducts.isEmpty()) return
        viewModelScope.launch {
            lowStockProducts.forEach { product ->
                repository.placeManualRestockOrder(
                    product = product,
                    orderQuantity = product.suggestedRestockQty,
                    manualNotes = "Batch manual restock order for low-stock threshold (${product.currentStock}/${product.lowStockThreshold} Pieces)."
                )
            }
            _statusBannerMessage.value =
                "Placed ${lowStockProducts.size} manual supplier restock POs for all low-stock SKUs."
        }
    }

    fun advanceRestockOrderStatus(po: RestockPurchaseOrder) {
        viewModelScope.launch {
            repository.advanceRestockOrderStatus(po)
            val msg = if (po.status == RestockOrderStatus.IN_TRANSIT) {
                "PO ${po.poNumber} received! Added +${po.orderQuantity} Pieces to ${po.productSku} stock."
            } else {
                "PO ${po.poNumber} marked as In Transit from ${po.supplierName}."
            }
            _statusBannerMessage.value = msg
        }
    }

    // --- CRM & Customer Ledger (Khata) Actions ---
    fun saveOrUpdateShopProfile(profile: RetailerShopProfile) {
        viewModelScope.launch {
            repository.saveShopProfile(profile)
            _statusBannerMessage.value = "Saved Customer / Shop profile for ${profile.shopName} (${profile.cityAndMarket})."
        }
    }

    fun deleteShopProfile(profile: RetailerShopProfile) {
        viewModelScope.launch {
            repository.deleteShopProfile(profile)
            _statusBannerMessage.value = if (_isUrduLanguage.value) {
                "کسٹمر '${profile.shopName}' کا ریکارڈ حذف کر دیا گیا۔"
            } else {
                "Deleted customer '${profile.shopName}' from CRM & Khata."
            }
        }
    }

    fun deleteAllShopProfiles() {
        viewModelScope.launch {
            repository.deleteAllShopProfiles()
            _statusBannerMessage.value = if (_isUrduLanguage.value) {
                "تمام کسٹمرز صاف کر دیے گئے۔ اب آپ اپنے کسٹمرز شامل کر سکتے ہیں۔"
            } else {
                "All customers deleted. You can now add your own customers."
            }
        }
    }

    fun recordCustomerLedgerEntry(
        shop: RetailerShopProfile,
        entryType: LedgerEntryType,
        amountPkr: Double,
        paymentMethod: PaymentMethod,
        senderNumber: String,
        receiverNumber: String,
        referenceCode: String,
        notes: String
    ) {
        viewModelScope.launch {
            repository.recordCustomerLedgerEntry(
                shop = shop,
                entryType = entryType,
                amountPkr = amountPkr,
                paymentMethod = paymentMethod,
                senderNumber = senderNumber,
                receiverNumber = receiverNumber,
                referenceCode = referenceCode,
                notes = notes
            )
            _statusBannerMessage.value =
                "Ledger updated for ${shop.shopName}: ${entryType.label} PKR ${amountPkr.toLong()} (${paymentMethod.label})."
        }
    }

    fun logCustomerInteraction(
        shop: RetailerShopProfile,
        interactionType: InteractionType,
        summaryNotes: String,
        nextActionNote: String
    ) {
        viewModelScope.launch {
            repository.logCustomerInteraction(
                shopId = shop.id,
                shopName = shop.shopName,
                interactionType = interactionType,
                summaryNotes = summaryNotes,
                nextActionNote = nextActionNote
            )
            _statusBannerMessage.value =
                "Logged ${interactionType.label} interaction for ${shop.shopName}."
        }
    }

    // --- Firebase Firestore Cloud Sync, Offline Persistence & Auto-Backup Actions ---
    fun backupAllToCloud() {
        viewModelScope.launch {
            val ok = repository.cloudSyncManager.performFullCloudBackup(silent = false)
            val syncState = cloudSyncState.value
            _statusBannerMessage.value = if (ok) {
                if (_isUrduLanguage.value) {
                    "تمام اسٹاک، کسٹمر کھاتہ اور سیلز رپورٹس فائر بیس کلاؤڈ اور لوکل کیش میں محفوظ ہو گئیں۔"
                } else {
                    "All inventory, customer Khata ledgers & sales reports backed up & cached in Firestore."
                }
            } else {
                if (_isUrduLanguage.value) {
                    "لوکل آف لائن کیش فعال ہے (${syncState.cachedSalesRecordsCount} سیلز، ${syncState.cachedLedgerEntriesCount} کھاتہ اندراجات محفوظ)۔"
                } else {
                    "Local Offline Cache active (${syncState.cachedSalesRecordsCount} sales, ${syncState.cachedLedgerEntriesCount} Khata entries cached). Add Firebase credentials for remote sync."
                }
            }
        }
    }

    fun restoreAllFromCloud() {
        viewModelScope.launch {
            val ok = repository.cloudSyncManager.pullAndRestoreFromCloud()
            val syncState = cloudSyncState.value
            _statusBannerMessage.value = if (ok) {
                if (_isUrduLanguage.value) {
                    "فائر اسٹور (کلاؤڈ / لوکل آف لائن کیش) سے تمام اسٹاک، کھاتہ لیجر اور سیلز رپورٹس بحال کر لی گئیں۔"
                } else {
                    if (syncState.isServingFromLocalCache) {
                        "Restored ${syncState.cachedSalesRecordsCount} sales records & ${syncState.cachedLedgerEntriesCount} Khata ledger entries from Firestore local offline cache."
                    } else {
                        "Successfully synced & restored inventory, customer ledgers & sales reports from Firestore."
                    }
                }
            } else {
                if (_isUrduLanguage.value) {
                    "کلاؤڈ سے ڈیٹا بحال کرنے میں دشواری: لوکل آف لائن کیش فعال ہے۔"
                } else {
                    "Serving from local offline cache. Check your Firebase project configuration for remote sync."
                }
            }
        }
    }

    fun updateCloudSyncConfig(
        workspaceId: String,
        customProjectId: String,
        customAppId: String,
        customApiKey: String
    ) {
        repository.cloudSyncManager.updateCloudConfiguration(
            workspaceId = workspaceId,
            customProjectId = customProjectId,
            customAppId = customAppId,
            customApiKey = customApiKey,
            onResultMessage = { msg ->
                _statusBannerMessage.value = msg
            }
        )
    }

    fun setCloudAutoBackup(enabled: Boolean) {
        repository.cloudSyncManager.setAutoBackupEnabled(enabled)
    }

    fun setCloudRealTimeSync(enabled: Boolean) {
        repository.cloudSyncManager.setRealTimeMultiDeviceSyncEnabled(enabled)
    }

    fun setFirestoreOfflinePersistence(enabled: Boolean) {
        repository.cloudSyncManager.setOfflinePersistenceEnabled(enabled, unlimitedCache = true)
        _statusBannerMessage.value = if (enabled) {
            "Firestore Offline Persistence (Unlimited Local Cache) enabled for sales & Khata ledger."
        } else {
            "Firestore switched to memory cache (local Room persistence remains active)."
        }
    }

    fun setSimulatedOfflineMode(offline: Boolean) {
        repository.cloudSyncManager.setSimulatedOfflineMode(offline) { msg ->
            _statusBannerMessage.value = msg
        }
    }

    fun verifyAndLoadFromLocalCache() {
        viewModelScope.launch {
            val summary = repository.cloudSyncManager.loadAndVerifyFromLocalCache()
            _statusBannerMessage.value = summary
        }
    }

    fun flushOfflineQueueToCloud() {
        viewModelScope.launch {
            val ok = repository.cloudSyncManager.flushPendingOfflineQueueToCloud()
            _statusBannerMessage.value = if (ok) {
                "Flushed queued offline sales records & Khata ledger updates to Firestore."
            } else {
                val state = cloudSyncState.value
                "Sales (${state.cachedSalesRecordsCount}) & Khata (${state.cachedLedgerEntriesCount}) remain safely cached locally; will sync when online."
            }
        }
    }

    companion object {
        fun provideFactory(repository: OpticalRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OpticalViewModel(repository) as T
                }
            }
    }
}
