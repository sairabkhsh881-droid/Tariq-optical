package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.OpticalCategory
import com.example.data.RetailerOrder
import com.example.ui.CategorySalesSummary
import com.example.ui.LocalIsUrdu
import com.example.ui.ProductSalesSummary
import com.example.ui.SalesDateRangeFilter
import com.example.ui.WholesaleDashboardUiState
import com.example.ui.components.CategoryBadge
import com.example.ui.components.MetricStatCard
import com.example.ui.components.PaymentMethodBadge
import com.example.ui.components.VisualSummaryDashboardSection
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatShortDate
import com.example.ui.theme.LowStockAmber
import com.example.ui.theme.PrecisionTeal
import com.example.ui.theme.StockHealthyGreen
import com.example.ui.tr
import com.example.util.OpticalPrintAndExportHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SalesReportingScreen(
    uiState: WholesaleDashboardUiState,
    onSelectDateRange: (SalesDateRangeFilter) -> Unit,
    onSelectCategory: (OpticalCategory?) -> Unit,
    onSelectProduct: (Int?) -> Unit,
    onGenerateCsv: () -> String = { "" },
    onNotifyStatus: (String) -> Unit,
    onOpenOrderReceipt: (RetailerOrder) -> Unit = {},
    onJumpToLowStock: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isUrdu = LocalIsUrdu.current
    var showPdfPreviewModal by remember { mutableStateOf(false) }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    OpticalPrintAndExportHelper.writeSalesReportPdf(uiState, out)
                }
                onNotifyStatus(
                    if (isUrdu) {
                        "سیلز رپورٹ کامیابی سے PDF میں محفوظ ہو گئی (${uiState.reportFilteredOrders.size} آرڈرز)۔"
                    } else {
                        "Sales report saved as PDF (${uiState.reportFilteredOrders.size} orders)."
                    }
                )
            }.onFailure {
                onNotifyStatus("Could not write PDF file: ${it.localizedMessage}")
            }
        }
    }

    val availableProductsForFilter = remember(uiState.allProducts, uiState.reportCategoryFilter) {
        if (uiState.reportCategoryFilter == null) {
            uiState.allProducts
        } else {
            uiState.allProducts.filter { it.category == uiState.reportCategoryFilter }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("sales_reporting_screen_list")
    ) {
        // Header + CSV Export Action Bar
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = "Sales Analytics",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Column {
                                Text(
                                    text = tr(
                                        "Tariq Jaddah Optical Sales Analytics (PKR)",
                                        "طارق جدہ آپٹیکل سیلز رپورٹس (PKR)"
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = tr(
                                        "Filter by Date Range, Category & Product • All Items Sold Per Piece",
                                        "تاریخ، کیٹیگری اور پراڈکٹ کے لحاظ سے رپورٹ • تمام حساب فی پیس"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                savePdfLauncher.launch("TariqJaddahOptical_SalesReport_PKR_$stamp.pdf")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_pdf_report_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Save PDF Report",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("Save PDF Report", "PDF رپورٹ محفوظ کریں"))
                        }

                        FilledTonalButton(
                            onClick = {
                                showPdfPreviewModal = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("preview_print_pdf_report_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Preview & Print PDF",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("Preview / Print PDF", "PDF دیکھیں / پرنٹ"))
                        }
                    }
                }
            }
        }

        // Filter 1: Date Range
        item {
            Column {
                Text(
                    text = tr("1. Filter by Date Range", "1. تاریخ کے لحاظ سے فلٹر کریں"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SalesDateRangeFilter.entries) { range ->
                        FilterChip(
                            selected = uiState.reportDateRange == range,
                            onClick = { onSelectDateRange(range) },
                            label = { Text(if (isUrdu) range.urduLabel else range.label) },
                            modifier = Modifier.testTag("report_date_${range.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Filter 2: Product Category
        item {
            Column {
                Text(
                    text = tr("2. Filter by Product Category", "2. پراڈکٹ کیٹیگری کے لحاظ سے فلٹر"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = uiState.reportCategoryFilter == null,
                            onClick = { onSelectCategory(null) },
                            label = { Text(tr("All Categories", "تمام کیٹیگریز")) },
                            modifier = Modifier.testTag("report_cat_all")
                        )
                    }
                    items(OpticalCategory.entries) { cat ->
                        FilterChip(
                            selected = uiState.reportCategoryFilter == cat,
                            onClick = {
                                onSelectCategory(if (uiState.reportCategoryFilter == cat) null else cat)
                            },
                            label = { Text(if (isUrdu) cat.urduName else cat.displayName) },
                            modifier = Modifier.testTag("report_cat_${cat.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Filter 3: Individual Product SKU
        item {
            Column {
                Text(
                    text = tr("3. Filter by Individual Product SKU", "3. انفرادی پراڈکٹ کوڈ کے لحاظ سے فلٹر"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = uiState.reportProductIdFilter == null,
                            onClick = { onSelectProduct(null) },
                            label = { Text(tr("All SKUs (${availableProductsForFilter.size})", "تمام پراڈکٹس (${availableProductsForFilter.size})")) },
                            modifier = Modifier.testTag("report_sku_all")
                        )
                    }
                    items(availableProductsForFilter, key = { it.id }) { prod ->
                        FilterChip(
                            selected = uiState.reportProductIdFilter == prod.id,
                            onClick = {
                                onSelectProduct(
                                    if (uiState.reportProductIdFilter == prod.id) null else prod.id
                                )
                            },
                            label = { Text("${prod.sku} • ${prod.name}") },
                            modifier = Modifier.testTag("report_sku_${prod.sku}")
                        )
                    }
                }
            }
        }

        // 4 KPI Summary Cards: Total Revenue, Pieces Sold, Gross Profit, Profit Margin %
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetricStatCard(
                        title = tr("TOTAL REVENUE (PKR)", "کل فروخت (PKR)"),
                        value = formatCurrency(uiState.reportTotalRevenuePkr),
                        subtitle = "${uiState.reportFilteredOrders.size} ${tr("wholesale orders", "ہول سیل آرڈرز")}",
                        icon = Icons.Default.MonetizationOn,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = tr("PIECES SOLD", "فروخت شدہ پیس"),
                        value = "${uiState.reportTotalUnitsSold} Pcs",
                        subtitle = tr("Sold strictly per piece", "صرف پیس میں فروخت"),
                        icon = Icons.Default.Inventory2,
                        accentColor = PrecisionTeal,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetricStatCard(
                        title = tr("GROSS PROFIT (PKR)", "خالص منافع (PKR)"),
                        value = formatCurrency(uiState.reportTotalProfitPkr),
                        subtitle = "${tr("Cost", "لاگت")}: ${formatCurrency(uiState.reportTotalCostPkr)}",
                        icon = Icons.Default.TrendingUp,
                        accentColor = StockHealthyGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = tr("PROFIT MARGIN", "منافع کی شرح"),
                        value = "${String.format(Locale.US, "%.1f", uiState.reportProfitMarginPercent)}%",
                        subtitle = tr("Net wholesale markup", "ہول سیل منافع فیصد"),
                        icon = Icons.Default.Percent,
                        accentColor = LowStockAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Recharts-Style Visual Summary Dashboard (Monthly Revenue Trends, Top Categories in PKR & Actionable Business Insights)
        item {
            VisualSummaryDashboardSection(
                uiState = uiState,
                onSelectCategoryFilter = onSelectCategory,
                onJumpToLowStock = onJumpToLowStock
            )
        }

        // Breakdown by Product Category
        item {
            Text(
                text = tr("Sales Breakdown by Product Category (Pieces)", "کیٹیگری کے لحاظ سے فروخت کی تفصیل (پیس)"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(uiState.categorySalesSummaries, key = { it.category.name }) { catSummary ->
            CategorySalesReportCard(
                summary = catSummary,
                totalReportRevenue = uiState.reportTotalRevenuePkr
            )
        }

        // Breakdown by Individual Product SKU
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tr(
                    "Performance by Individual Product (${uiState.productSalesSummaries.size} SKUs)",
                    "انفرادی پراڈکٹ کے لحاظ سے کارکردگی (${uiState.productSalesSummaries.size})"
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (uiState.productSalesSummaries.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tr(
                            "No sales transactions match the selected date range and filters.",
                            "منتخب کردہ تاریخ اور فلٹر میں کوئی فروخت موجود نہیں۔"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(uiState.productSalesSummaries, key = { it.productId }) { prodSummary ->
                ProductSalesReportRow(summary = prodSummary)
            }
        }

        // Filtered Order Transactions List
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tr(
                    "Filtered Order Transactions (${uiState.reportFilteredOrders.size})",
                    "فلٹر شدہ آرڈر ٹرانزیکشنز (${uiState.reportFilteredOrders.size})"
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(uiState.reportFilteredOrders, key = { it.id }) { order ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${order.orderNumber} • ${order.retailerShopName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        PaymentMethodBadge(method = order.paymentMethod)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${order.productSku} — ${order.productName} (${order.quantity} ${tr("Pieces", "پیس")})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${tr("Sender", "بھیجنے والا")}: ${order.senderNumber.ifEmpty { "N/A" }} • ${tr("Receiver", "وصول کرنے والا")}: ${order.receiverNumber.ifEmpty { "N/A" }} • ${formatShortDate(order.createdAtEpochMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "${tr("Revenue", "آمدنی")}: ${formatCurrency(order.totalAmount)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${tr("Profit", "منافع")}: ${formatCurrency(order.grossProfitPkr)} (${String.format(Locale.US, "%.1f", order.profitMarginPercent)}%)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = StockHealthyGreen
                            )
                        }
                        OutlinedButton(
                            onClick = { onOpenOrderReceipt(order) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("report_order_receipt_btn_${order.orderNumber}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "Receipt",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(tr("Receipt 🧾", "رسید 🧾"))
                        }
                    }
                }
            }
        }
    }

    if (showPdfPreviewModal) {
        PdfSalesReportPreviewDialog(
            uiState = uiState,
            context = context,
            onSavePdfClick = {
                val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                savePdfLauncher.launch("TariqJaddahOptical_SalesReport_PKR_$stamp.pdf")
            },
            onDismiss = { showPdfPreviewModal = false }
        )
    }
}

@Composable
private fun CategorySalesReportCard(
    summary: CategorySalesSummary,
    totalReportRevenue: Double
) {
    val shareRatio = if (totalReportRevenue > 0.0) {
        (summary.revenuePkr / totalReportRevenue).toFloat().coerceIn(0f, 1f)
    } else 0f

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                CategoryBadge(category = summary.category)
                Text(
                    text = "${summary.orderCount} ${tr("orders", "آرڈرز")} • ${summary.unitsSold} ${tr("Pieces sold", "پیس فروخت")}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = tr("REVENUE (PKR)", "فروخت (PKR)"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(summary.revenuePkr),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = tr("GROSS PROFIT & MARGIN", "منافع اور شرح"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${formatCurrency(summary.profitPkr)} (${String.format(Locale.US, "%.1f", summary.marginPercent)}%)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = StockHealthyGreen
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { shareRatio },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun ProductSalesReportRow(summary: ProductSalesSummary) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${summary.sku} • ${summary.productName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${summary.category.displayName} • ${summary.orderCount} ${tr("orders", "آرڈرز")} • ${summary.unitsSold} ${tr("Pieces sold", "پیس فروخت")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(summary.revenuePkr),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${tr("Profit", "منافع")}: ${formatCurrency(summary.profitPkr)} (${String.format(Locale.US, "%.1f", summary.marginPercent)}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = StockHealthyGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfSalesReportPreviewDialog(
    uiState: WholesaleDashboardUiState,
    context: Context,
    onSavePdfClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val pdfPageBitmap = remember(uiState) {
        OpticalPrintAndExportHelper.createSalesReportPageBitmap(uiState)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 14.dp)
                .testTag("pdf_sales_report_preview_dialog")
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF Sales Report",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = tr(
                                    "PDF Sales Report Preview & Print (PKR)",
                                    "سیلز رپورٹ PDF پری ویو اور پرنٹ (PKR)"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "Tariq Jaddah Optical • Revenue, Pieces Sold & Profit Margins",
                                    "طارق جدہ آپٹیکل • کل فروخت، پیس اور منافع کی مکمل رپورٹ"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onSavePdfClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_save_sales_pdf_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Save PDF",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Save as PDF File", "PDF فائل محفوظ کریں"))
                    }

                    FilledTonalButton(
                        onClick = {
                            OpticalPrintAndExportHelper.printSalesReportPdf(context, uiState)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_print_sales_pdf_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print PDF",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Print PDF", "پرنٹ کریں"))
                    }
                }

                HorizontalDivider()

                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                ) {
                    Image(
                        bitmap = pdfPageBitmap.asImageBitmap(),
                        contentDescription = "PDF Sales Report Page Preview",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}
