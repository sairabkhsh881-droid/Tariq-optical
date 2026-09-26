package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.DEALER_NUMBER_1
import com.example.data.DEALER_NUMBER_2
import com.example.data.OpticalCategory
import com.example.data.OpticalProduct
import com.example.data.PaymentMethod
import com.example.data.RetailOrderStage
import com.example.data.RetailerOrder
import com.example.data.RetailerShopProfile
import com.example.data.TARIQ_MEHMOOD_RECEIVER_NUMBERS
import com.example.data.extractCylFromText
import com.example.data.extractSphFromText
import com.example.data.formatSphCylPowerTag
import com.example.data.mergeSphCylIntoNotes
import com.example.ui.LocalIsUrdu
import com.example.ui.WholesaleDashboardUiState
import com.example.ui.components.BarcodeLabelBadge
import com.example.ui.components.CategoryBadge
import com.example.ui.components.CompactBarcodePickerDialog
import com.example.ui.components.MetricStatCard
import com.example.ui.components.OpticalSphCylSelectorSection
import com.example.ui.components.OrderPipelineStepper
import com.example.ui.components.PaymentMethodBadge
import com.example.ui.components.SphCylPowerBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatShortDate
import com.example.ui.theme.CriticalStockRed
import com.example.ui.theme.StockHealthyGreen
import com.example.ui.tr
import java.util.Locale

@Composable
fun RetailOrdersScreen(
    uiState: WholesaleDashboardUiState,
    onSelectStageFilter: (RetailOrderStage?) -> Unit,
    onOpenCreateOrderDialog: () -> Unit,
    onOpenCounterSaleDialog: () -> Unit = {},
    onAdvanceOrderStage: (RetailerOrder) -> Unit,
    onOpenBarcodeScanner: () -> Unit = {},
    onOpenOrderReceipt: (RetailerOrder) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isUrdu = LocalIsUrdu.current
    // 0 = All, 1 = Counter Sales Only, 2 = Customer Orders Only
    var saleTypeFilter by remember { mutableIntStateOf(0) }

    val displayedOrders = remember(uiState.retailerOrders, saleTypeFilter) {
        when (saleTypeFilter) {
            1 -> uiState.retailerOrders.filter {
                it.orderNumber.startsWith("CS-", ignoreCase = true) ||
                    it.retailerShopName.contains("Counter Sale", ignoreCase = true) ||
                    it.customRxAndLabNotes.contains("[COUNTER SALE]", ignoreCase = true)
            }
            2 -> uiState.retailerOrders.filterNot {
                it.orderNumber.startsWith("CS-", ignoreCase = true) ||
                    it.retailerShopName.contains("Counter Sale", ignoreCase = true) ||
                    it.customRxAndLabNotes.contains("[COUNTER SALE]", ignoreCase = true)
            }
            else -> uiState.retailerOrders
        }
    }

    val counterSalesCount = remember(uiState.allRetailerOrdersUnfiltered) {
        uiState.allRetailerOrdersUnfiltered.count {
            it.orderNumber.startsWith("CS-", ignoreCase = true) ||
                it.retailerShopName.contains("Counter Sale", ignoreCase = true) ||
                it.customRxAndLabNotes.contains("[COUNTER SALE]", ignoreCase = true)
        }
    }

    val activeOrdersCount = remember(uiState.retailerOrders) {
        uiState.retailerOrders.count { it.stage != RetailOrderStage.DELIVERED }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("retail_orders_screen_list")
    ) {
        // KPI Summary Row in PKR
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MetricStatCard(
                    title = tr("Total Sales Revenue", "کل فروخت (آرڈرز + کاؤنٹر)"),
                    value = formatCurrency(uiState.activeRetailRevenue),
                    subtitle = "${uiState.allRetailerOrdersUnfiltered.size} ${tr("total bills ($counterSalesCount Counter)", "کل بل ($counterSalesCount کاؤنٹر سیل)")}",
                    icon = Icons.Default.Payments,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = tr("Active Orders", "جاری آرڈرز"),
                    value = "$activeOrdersCount Active",
                    subtitle = tr("Rx Lab, QC & Dispatch", "لیب، کوالٹی اور ڈسپیچ"),
                    icon = Icons.Default.LocalShipping,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Prominent New Order & Counter Sale Action Banner
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        text = tr(
                            "Create New Order or Instant Counter Sale (Per Piece • SPH / CYL)",
                            "نیا کسٹمر آرڈر یا فوری کاؤنٹر سیل درج کریں (فی پیس • SPH / CYL)"
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onOpenCreateOrderDialog,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_retail_order_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddShoppingCart,
                                contentDescription = "New Order",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tr("+ New Order", "+ نیا آرڈر"),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onOpenCounterSaleDialog,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0D9488),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("counter_sale_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PointOfSale,
                                contentDescription = "Counter Sale",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tr("Counter Sale", "کاؤنٹر سیل"),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        FilledTonalButton(
                            onClick = onOpenBarcodeScanner,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("orders_scan_barcode_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Barcode for Sale",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(tr("Scan", "اسکین"))
                        }
                    }
                }
            }
        }

        // Sale Type & Stage Filter Chips
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = saleTypeFilter == 0,
                        onClick = { saleTypeFilter = 0 },
                        label = { Text(tr("All (${uiState.retailerOrders.size})", "تمام بل")) },
                        modifier = Modifier.testTag("filter_sale_type_all")
                    )
                    FilterChip(
                        selected = saleTypeFilter == 1,
                        onClick = { saleTypeFilter = 1 },
                        label = { Text(tr("Counter Sales ($counterSalesCount)", "کاؤنٹر سیل ($counterSalesCount)")) },
                        modifier = Modifier.testTag("filter_sale_type_counter")
                    )
                    FilterChip(
                        selected = saleTypeFilter == 2,
                        onClick = { saleTypeFilter = 2 },
                        label = { Text(tr("Customer Orders", "کسٹمر آرڈرز")) },
                        modifier = Modifier.testTag("filter_sale_type_orders")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = uiState.selectedOrderStageFilter == null,
                            onClick = { onSelectStageFilter(null) },
                            label = { Text(tr("All Stages", "تمام مراحل")) },
                            modifier = Modifier.testTag("stage_filter_all")
                        )
                    }
                    items(RetailOrderStage.entries) { stage ->
                        FilterChip(
                            selected = uiState.selectedOrderStageFilter == stage,
                            onClick = {
                                onSelectStageFilter(
                                    if (uiState.selectedOrderStageFilter == stage) null else stage
                                )
                            },
                            label = { Text(if (isUrdu) stage.urduLabel else stage.label) },
                            modifier = Modifier.testTag("stage_filter_${stage.name.lowercase()}")
                        )
                    }
                }
            }
        }

        if (displayedOrders.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = "No Orders Yet",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = tr(
                                "No Orders or Counter Sales Recorded Yet",
                                "ابھی تک کوئی آرڈر یا کاؤنٹر سیل درج نہیں کی گئی"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tr(
                                "Tap '+ New Order' to create a customer order or 'Counter Sale' for an instant walk-in bill with SPH/CYL options.",
                                "نیا کسٹمر آرڈر بنانے کے لیے '+ نیا آرڈر' یا فوری نقد بل کے لیے 'کاؤنٹر سیل' پر کلک کریں۔"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onOpenCreateOrderDialog,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tr("+ New Order", "+ نیا آرڈر"))
                            }
                            Button(
                                onClick = onOpenCounterSaleDialog,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0D9488),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PointOfSale,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tr("Counter Sale", "کاؤنٹر سیل"))
                            }
                        }
                    }
                }
            }
        } else {
            items(
                items = displayedOrders,
                key = { it.id }
            ) { order ->
                RetailerOrderTrackingCard(
                    order = order,
                    onAdvanceStage = { onAdvanceOrderStage(order) },
                    onOpenReceipt = { onOpenOrderReceipt(order) }
                )
            }
        }
    }
}

@Composable
private fun RetailerOrderTrackingCard(
    order: RetailerOrder,
    onAdvanceStage: () -> Unit,
    onOpenReceipt: () -> Unit = {}
) {
    val isUrdu = LocalIsUrdu.current
    val nextStage = order.stage.nextStage()
    val isCounterSale = order.orderNumber.startsWith("CS-", ignoreCase = true) ||
        order.retailerShopName.contains("Counter Sale", ignoreCase = true) ||
        order.customRxAndLabNotes.contains("[COUNTER SALE]", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("retail_order_card_${order.orderNumber}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Order Number + Counter Sale / Order Badge + Category + Payment Method Badge
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = if (isCounterSale) Color(0xFF0D9488) else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isCounterSale) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isCounterSale) "⚡ ${order.orderNumber} (COUNTER)" else order.orderNumber,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    CategoryBadge(category = order.category)
                }

                PaymentMethodBadge(method = order.paymentMethod)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Retailer Shop Name & Contact
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.retailerShopName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "City",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = order.retailerCity,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Contact",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = order.retailerContact,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatShortDate(order.createdAtEpochMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(order.totalAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${order.quantity} ${tr("Pieces", "پیس")} × ${formatCurrency(order.unitWholesalePrice)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${tr("Profit", "منافع")}: ${formatCurrency(order.grossProfitPkr)} (${String.format(Locale.US, "%.0f", order.profitMarginPercent)}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = StockHealthyGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Payment Sender & Receiver + Paid vs Debt Summary Box
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${tr("Paid", "وصول شدہ")}: ${formatCurrency(order.paidAmountPkr)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = StockHealthyGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (order.debtAmountPkr > 0) {
                                "${tr("Ledger Debt (Udhaar)", "بقایا ادھار کھاتہ")}: ${formatCurrency(order.debtAmountPkr)}"
                            } else {
                                tr("Fully Settled (0 Debt)", "مکمل ادا شدہ")
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (order.debtAmountPkr > 0) CriticalStockRed else StockHealthyGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${tr("Sender No", "بھیجنے والا نمبر")}: ${order.senderNumber.ifEmpty { "N/A" }}  •  ${tr("Receiver No", "وصول کرنے والا نمبر")}: ${order.receiverNumber.ifEmpty { DEALER_NUMBER_1 }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${tr("Receipt / Khata Nos", "رسید اور کھاتہ نمبرز")}: $DEALER_NUMBER_1  •  $DEALER_NUMBER_2",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Ordered SKU & Custom Rx Lab Notes Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(12.dp)
            ) {
                Text(
                    text = "${order.productSku} — ${order.productName} (${order.quantity} ${tr("Pieces", "پیس")})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                SphCylPowerBadge(
                    rawText = order.customRxAndLabNotes,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "${tr("Custom Rx / Lab Spec", "لیب اور نمبر تفصیل")}: ${order.customRxAndLabNotes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5-Stage Pipeline Stepper
            OrderPipelineStepper(currentStage = order.stage)

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(10.dp))

            if (nextStage != null) {
                Button(
                    onClick = onAdvanceStage,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("advance_order_${order.orderNumber}")
                ) {
                    Text(
                        if (isUrdu) "اگلے مرحلے پر بھیجیں: ${nextStage.urduLabel}"
                        else "Advance Order to: ${nextStage.label}"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Advance stage",
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Order Delivered",
                        tint = StockHealthyGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCounterSale) {
                            tr("Completed Counter Sale (Delivered Immediately)", "کاؤنٹر سیل مکمل اور ڈیلیور شدہ")
                        } else {
                            tr("Delivered & Signed by Customer", "کسٹمر کو مکمل ڈیلیور ہو گیا")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = StockHealthyGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onOpenReceipt,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("receipt_button_${order.orderNumber}")
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = "Print or Save Receipt",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    tr(
                        "Receipt 🧾 Print / Save as PDF or JPG",
                        "رسید 🧾 پرنٹ کریں / PDF یا JPG میں محفوظ کریں"
                    )
                )
            }
        }
    }
}

@Composable
fun CreateRetailOrderDialog(
    products: List<OpticalProduct>,
    shopProfiles: List<RetailerShopProfile>,
    preselectedProduct: OpticalProduct?,
    preselectedShop: RetailerShopProfile? = null,
    onDismiss: () -> Unit,
    onCreateOrder: (
        shopId: Int,
        shopName: String,
        city: String,
        contact: String,
        product: OpticalProduct,
        qty: Int,
        rxNotes: String,
        paymentMethod: PaymentMethod,
        senderNumber: String,
        receiverNumber: String,
        paidAmountPkr: Double
    ) -> Unit
) {
    val isUrdu = LocalIsUrdu.current
    var useCustomNewProduct by remember { mutableStateOf(products.isEmpty()) }
    var selectedProduct by remember(products, preselectedProduct) {
        mutableStateOf(
            preselectedProduct ?: products.firstOrNull() ?: OpticalProduct(
                id = 0,
                sku = "LNS-${(100..999).random()}",
                barcode = "",
                name = "",
                brand = "Tariq Jaddah Optical",
                category = OpticalCategory.OPHTHALMIC_LENSES,
                wholesalePrice = 1500.0,
                unitCostPkr = 950.0,
                retailMsrp = 3000.0,
                minOrderQty = 1,
                currentStock = 50,
                lowStockThreshold = 15,
                primarySpec = "Single Piece Optical Item",
                secondarySpec = "Standard Multi-Coated",
                powerOrSizeRange = "SPH -1.00 / CYL -0.50",
                supplierName = "Tariq Jaddah Optical",
                leadTimeDays = 2
            )
        )
    }

    // Inline New Product state when catalog is empty or user clicks "+ New Product"
    var newProdCategory by remember { mutableStateOf(OpticalCategory.OPHTHALMIC_LENSES) }
    var newProdName by remember { mutableStateOf("") }
    var newProdSku by remember { mutableStateOf("TJO-${(100..999).random()}") }
    var newProdSalePriceText by remember { mutableStateOf("1500") }
    var newProdCostPriceText by remember { mutableStateOf("950") }
    var newProdInitialStockText by remember { mutableStateOf("50") }

    var showCameraScannerForSale by remember { mutableStateOf(false) }
    var scannedBannerNote by remember { mutableStateOf<String?>(null) }
    var selectedShop by remember {
        mutableStateOf(preselectedShop ?: shopProfiles.firstOrNull())
    }

    var shopName by remember {
        mutableStateOf(selectedShop?.shopName ?: "")
    }
    var city by remember {
        mutableStateOf(selectedShop?.cityAndMarket ?: "")
    }
    var contact by remember {
        mutableStateOf(selectedShop?.phoneNumber ?: "")
    }
    var quantityText by remember {
        mutableStateOf(
            if (products.isEmpty()) "1" else selectedProduct.minOrderQty.coerceAtLeast(1).toString()
        )
    }
    var sphPowerInput by remember {
        mutableStateOf(
            extractSphFromText(selectedProduct.powerOrSizeRange).ifEmpty {
                if (selectedProduct.category != OpticalCategory.EYEGLASS_FRAMES) "-1.50" else ""
            }
        )
    }
    var cylPowerInput by remember {
        mutableStateOf(
            extractCylFromText(selectedProduct.powerOrSizeRange).ifEmpty {
                if (selectedProduct.category != OpticalCategory.EYEGLASS_FRAMES) "-0.50" else ""
            }
        )
    }
    var rxNotes by remember {
        mutableStateOf("Per-Piece Order")
    }

    var selectedPaymentMethod by remember {
        mutableStateOf(PaymentMethod.CASH)
    }
    var senderNumber by remember {
        mutableStateOf(selectedShop?.phoneNumber ?: "")
    }
    var receiverNumber by remember {
        mutableStateOf(PaymentMethod.CASH.defaultReceiverNo)
    }

    val effectiveUnitPrice = if (useCustomNewProduct || products.isEmpty()) {
        (newProdSalePriceText.toDoubleOrNull() ?: 1500.0).coerceAtLeast(1.0)
    } else {
        selectedProduct.wholesalePrice
    }
    val effectiveUnitCost = if (useCustomNewProduct || products.isEmpty()) {
        (newProdCostPriceText.toDoubleOrNull() ?: (effectiveUnitPrice * 0.65)).coerceAtLeast(0.0)
    } else {
        selectedProduct.unitCostPkr
    }

    val parsedQty = (quantityText.toIntOrNull() ?: 1).coerceAtLeast(1)
    val orderTotal = parsedQty * effectiveUnitPrice

    var paidAmountText by remember(selectedProduct.id, effectiveUnitPrice, quantityText, selectedPaymentMethod) {
        mutableStateOf(
            if (selectedPaymentMethod == PaymentMethod.DEBT) "0"
            else orderTotal.toLong().toString()
        )
    }

    val paidAmountPkr = (paidAmountText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, orderTotal)
    val remainingDebtPkr = (orderTotal - paidAmountPkr).coerceAtLeast(0.0)
    val orderProfit = (effectiveUnitPrice - effectiveUnitCost) * parsedQty

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddShoppingCart,
                    contentDescription = "New Order",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = tr(
                        "New Order (Per Piece & SPH/CYL)",
                        "نیا آرڈر (فروخت فی پیس اور SPH/CYL)"
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Customer Selection or New Customer Entry
                Text(
                    text = tr(
                        "1. Select Saved Customer or Enter New Customer Below:",
                        "1. محفوظ کسٹمر منتخب کریں یا نیا کسٹمر لکھیں:"
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (shopProfiles.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedShop == null,
                                onClick = {
                                    selectedShop = null
                                    shopName = ""
                                    city = ""
                                    contact = ""
                                    senderNumber = ""
                                },
                                label = { Text(tr("+ New Customer", "+ نیا کسٹمر")) }
                            )
                        }
                        items(shopProfiles, key = { it.id }) { shop ->
                            FilterChip(
                                selected = selectedShop?.id == shop.id,
                                onClick = {
                                    selectedShop = shop
                                    shopName = shop.shopName
                                    city = shop.cityAndMarket
                                    contact = shop.phoneNumber
                                    senderNumber = shop.phoneNumber
                                },
                                label = { Text(shop.shopName) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text(tr("Customer / Optical Shop Name *", "کسٹمر / دکان کا نام *")) },
                    placeholder = { Text(tr("Enter customer or shop name", "کسٹمر یا دکان کا نام لکھیں")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_retail_shop_name")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text(tr("City / Area", "شہر / علاقہ")) },
                        placeholder = { Text(tr("e.g. Lahore", "مثلاً لاہور")) },
                        singleLine = true,
                        modifier = Modifier.weight(0.5f)
                    )
                    OutlinedTextField(
                        value = contact,
                        onValueChange = {
                            contact = it
                            if (senderNumber.isBlank()) senderNumber = it
                        },
                        label = { Text(tr("Customer Phone", "موبائل نمبر")) },
                        placeholder = { Text("03XX-XXXXXXX") },
                        singleLine = true,
                        modifier = Modifier.weight(0.5f)
                    )
                }

                HorizontalDivider()

                // 2. Product Selection or Inline New Product Entry
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tr(
                            "2. Select Product or Enter New Product:",
                            "2. پراڈکٹ منتخب کریں یا نئی پراڈکٹ درج کریں:"
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    if (products.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = { showCameraScannerForSale = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("order_dialog_scan_barcode_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Barcode",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tr("Scan", "اسکین"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (products.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = useCustomNewProduct,
                                onClick = { useCustomNewProduct = true },
                                label = { Text(tr("+ New Product", "+ نئی پراڈکٹ")) }
                            )
                        }
                        items(products, key = { it.id }) { prod ->
                            FilterChip(
                                selected = !useCustomNewProduct && selectedProduct.id == prod.id,
                                onClick = {
                                    useCustomNewProduct = false
                                    selectedProduct = prod
                                    quantityText = prod.minOrderQty.coerceAtLeast(1).toString()
                                    sphPowerInput = extractSphFromText(prod.powerOrSizeRange).ifEmpty {
                                        if (prod.category != OpticalCategory.EYEGLASS_FRAMES) "-1.50" else ""
                                    }
                                    cylPowerInput = extractCylFromText(prod.powerOrSizeRange).ifEmpty {
                                        if (prod.category != OpticalCategory.EYEGLASS_FRAMES) "-0.50" else ""
                                    }
                                },
                                label = {
                                    Text("${prod.sku} (${prod.currentStock} Pcs)")
                                }
                            )
                        }
                    }
                }

                if (useCustomNewProduct || products.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = tr(
                                    "Enter Product Details (Auto-saved to your Catalog):",
                                    "نئی پراڈکٹ کی تفصیل درج کریں (کیٹلاگ میں خودکار محفوظ ہوگی):"
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(OpticalCategory.entries) { cat ->
                                    FilterChip(
                                        selected = newProdCategory == cat,
                                        onClick = { newProdCategory = cat },
                                        label = { Text(if (isUrdu) cat.urduName else cat.displayName) }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = newProdName,
                                    onValueChange = { newProdName = it },
                                    label = { Text(tr("Product Name *", "پراڈکٹ کا نام *")) },
                                    placeholder = { Text(tr("e.g. Blue-Cut Lens / Frame", "مثلاً بلیو کٹ لینز")) },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.65f)
                                )
                                OutlinedTextField(
                                    value = newProdSku,
                                    onValueChange = { newProdSku = it },
                                    label = { Text(tr("SKU Code", "کوڈ")) },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.35f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = newProdSalePriceText,
                                    onValueChange = { newProdSalePriceText = it },
                                    label = { Text(tr("Price / Piece (PKR)", "فروخت قیمت فی پیس")) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = newProdCostPriceText,
                                    onValueChange = { newProdCostPriceText = it },
                                    label = { Text(tr("Cost / Piece (PKR)", "لاگت فی پیس")) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = newProdInitialStockText,
                                    onValueChange = { newProdInitialStockText = it },
                                    label = { Text(tr("Stock Pcs", "اسٹاک پیس")) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(0.8f)
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = selectedProduct.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                BarcodeLabelBadge(barcode = selectedProduct.barcode)
                            }
                            Text(
                                text = tr(
                                    "Price: ${formatCurrency(selectedProduct.wholesalePrice)} / Piece • Available: ${selectedProduct.currentStock} Pieces",
                                    "قیمت: ${formatCurrency(selectedProduct.wholesalePrice)} فی پیس • موجودہ اسٹاک: ${selectedProduct.currentStock} پیس"
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (scannedBannerNote != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = scannedBannerNote!!,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = {
                        Text(
                            tr(
                                "Order Quantity (Strictly in Pieces / پیس)",
                                "آرڈر کی تعداد (صرف پیس میں)"
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_retail_order_qty")
                )

                OpticalSphCylSelectorSection(
                    sphValue = sphPowerInput,
                    cylValue = cylPowerInput,
                    onSphChange = { sphPowerInput = it },
                    onCylChange = { cylPowerInput = it },
                    contextLabelEn = "Sales Order Lens SPH / CYL Power Options",
                    contextLabelUr = "فروخت آرڈر کے لیے لینز SPH / CYL نمبر"
                )

                // Payment Methods Section: Cash, Debt, JazzCash, EasyPaisa
                Text(
                    text = tr(
                        "Payment Method (Cash, Debt, JazzCash, EasyPaisa):",
                        "ادائیگی کا طریقہ (نقد، ادھار، جیز کیش، ایزی پیسہ):"
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(PaymentMethod.entries) { method ->
                        FilterChip(
                            selected = selectedPaymentMethod == method,
                            onClick = {
                                selectedPaymentMethod = method
                                receiverNumber = method.defaultReceiverNo
                                paidAmountText = if (method == PaymentMethod.DEBT) {
                                    "0"
                                } else {
                                    orderTotal.toLong().toString()
                                }
                            },
                            label = {
                                Text(if (isUrdu) method.urduLabel else method.label)
                            },
                            modifier = Modifier.testTag("payment_method_chip_${method.name.lowercase()}")
                        )
                    }
                }

                // Sender Number & Receiver Number
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = senderNumber,
                        onValueChange = { senderNumber = it },
                        label = {
                            Text(
                                tr(
                                    "Sender Number (Customer)",
                                    "بھیجنے والا نمبر (Sender)"
                                )
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_sender_number")
                    )
                    OutlinedTextField(
                        value = receiverNumber,
                        onValueChange = { receiverNumber = it },
                        label = {
                            Text(
                                tr(
                                    "Receiver Number (Tariq Jaddah)",
                                    "وصول کرنے والا نمبر (Receiver)"
                                )
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_receiver_number")
                    )
                }

                Text(
                    text = tr(
                        "Select Receiver Account for Receipt & Khata (Tariq Mehmood):",
                        "رسید اور کھاتہ کے لیے وصول کرنے والا نمبر منتخب کریں (طارق محمود):"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TARIQ_MEHMOOD_RECEIVER_NUMBERS) { accNo ->
                        FilterChip(
                            selected = receiverNumber == accNo,
                            onClick = { receiverNumber = accNo },
                            label = { Text(accNo, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = paidAmountText,
                    onValueChange = { paidAmountText = it },
                    label = {
                        Text(
                            tr(
                                "Paid Amount Now (PKR) — Rest goes to Customer Ledger",
                                "وصول شدہ رقم (PKR) — بقایا رقم کھاتہ میں درج ہوگی"
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_paid_amount_pkr")
                )

                OutlinedTextField(
                    value = rxNotes,
                    onValueChange = { rxNotes = it },
                    label = { Text(tr("Custom Rx Powers / Lab Instructions", "لیب اور نمبر کی تفصیلات")) },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_retail_rx_notes")
                )

                // Summary of Invoice, Paid & Ledger Debt
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text(
                            text = "${tr("Invoice Total", "کل بل")}: ${formatCurrency(orderTotal)} ($parsedQty ${tr("Pieces", "پیس")})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${tr("Paid via", "وصول شدہ")} ${selectedPaymentMethod.label}: ${formatCurrency(paidAmountPkr)} • ${tr("Added to Debt Ledger", "بقایا ادھار کھاتہ")}: ${formatCurrency(remainingDebtPkr)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (remainingDebtPkr > 0) CriticalStockRed else StockHealthyGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${tr("Estimated Dealer Profit", "متوقع منافع")}: ${formatCurrency(orderProfit)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = StockHealthyGreen
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalRxAndSphCyl = mergeSphCylIntoNotes(
                        rxNotes.trim().ifEmpty { "Standard Wholesale Piece Dispatch" },
                        sphPowerInput,
                        cylPowerInput
                    )
                    val productToOrder = if (useCustomNewProduct || products.isEmpty()) {
                        val initStock = (newProdInitialStockText.toIntOrNull() ?: 50).coerceAtLeast(parsedQty)
                        OpticalProduct(
                            id = 0,
                            sku = newProdSku.trim().ifEmpty { "TJO-${(100..999).random()}" },
                            barcode = "",
                            name = newProdName.trim().ifEmpty {
                                "${newProdCategory.displayName} (${formatSphCylPowerTag(sphPowerInput, cylPowerInput).ifEmpty { "Standard" }})"
                            },
                            brand = "Tariq Jaddah Optical",
                            category = newProdCategory,
                            wholesalePrice = effectiveUnitPrice,
                            unitCostPkr = effectiveUnitCost,
                            retailMsrp = effectiveUnitPrice * 1.8,
                            minOrderQty = 1,
                            currentStock = initStock,
                            lowStockThreshold = 10,
                            primarySpec = "Single Piece Optical Item",
                            secondarySpec = "Standard Optical Finish",
                            powerOrSizeRange = formatSphCylPowerTag(sphPowerInput, cylPowerInput).ifEmpty { "Standard" },
                            supplierName = "Tariq Jaddah Optical",
                            leadTimeDays = 2
                        )
                    } else {
                        selectedProduct
                    }

                    onCreateOrder(
                        selectedShop?.id ?: 0,
                        shopName.trim().ifEmpty { "Optical Customer" },
                        city.trim().ifEmpty { "Pakistan" },
                        contact.trim().ifEmpty { "Customer" },
                        productToOrder,
                        parsedQty,
                        finalRxAndSphCyl,
                        selectedPaymentMethod,
                        senderNumber.trim().ifEmpty { contact.trim().ifEmpty { "Customer" } },
                        receiverNumber.trim().ifEmpty { selectedPaymentMethod.defaultReceiverNo },
                        paidAmountPkr
                    )
                },
                modifier = Modifier.testTag("confirm_create_retail_order_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(tr("Confirm New Order & Ledger", "نیا آرڈر اور کھاتہ محفوظ کریں"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cancel", "منسوخ"))
            }
        }
    )

    if (showCameraScannerForSale && products.isNotEmpty()) {
        CompactBarcodePickerDialog(
            products = products,
            title = tr(
                "Scan Product Barcode for Order",
                "آرڈر کے لیے پراڈکٹ بارکوڈ اسکین کریں"
            ),
            onDismiss = { showCameraScannerForSale = false },
            onBarcodeScanned = { scannedCode ->
                val matched = products.firstOrNull {
                    it.barcode.equals(scannedCode, ignoreCase = true) ||
                        it.sku.equals(scannedCode, ignoreCase = true)
                } ?: products.firstOrNull {
                    it.barcode.contains(scannedCode, ignoreCase = true) ||
                        it.sku.contains(scannedCode, ignoreCase = true)
                }
                if (matched != null) {
                    useCustomNewProduct = false
                    if (selectedProduct.id == matched.id) {
                        val nextQty = ((quantityText.toIntOrNull() ?: matched.minOrderQty) + 1)
                        quantityText = nextQty.toString()
                        scannedBannerNote =
                            "Scanned ${matched.barcode} (${matched.sku}): Incremented sale quantity to $nextQty Pieces!"
                    } else {
                        selectedProduct = matched
                        quantityText = matched.minOrderQty.toString()
                        scannedBannerNote =
                            "Scanned ${matched.barcode}: Selected ${matched.sku} (${matched.currentStock} Pieces in stock)"
                    }
                } else {
                    scannedBannerNote = "No warehouse SKU matched barcode '$scannedCode'"
                }
                showCameraScannerForSale = false
            }
        )
    }
}

/**
 * Dedicated Instant Walk-in / Over-the-Counter POS Sale Dialog (Counter Sale / کاؤنٹر سیل).
 * Works with existing products or allows entering a new product inline, includes SPH/CYL lens
 * power options, discount calculation, and immediately generates a completed sale + receipt.
 */
@Composable
fun CreateCounterSaleDialog(
    products: List<OpticalProduct>,
    preselectedProduct: OpticalProduct? = null,
    onDismiss: () -> Unit,
    onConfirmCounterSale: (
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
    ) -> Unit
) {
    val isUrdu = LocalIsUrdu.current
    var useCustomNewProduct by remember { mutableStateOf(products.isEmpty()) }
    var selectedProduct by remember(products, preselectedProduct) {
        mutableStateOf(
            preselectedProduct ?: products.firstOrNull() ?: OpticalProduct(
                id = 0,
                sku = "CS-ITEM-${(100..999).random()}",
                barcode = "",
                name = "",
                brand = "Tariq Jaddah Optical",
                category = OpticalCategory.OPHTHALMIC_LENSES,
                wholesalePrice = 1200.0,
                unitCostPkr = 750.0,
                retailMsrp = 2400.0,
                minOrderQty = 1,
                currentStock = 50,
                lowStockThreshold = 10,
                primarySpec = "Counter Sale Optical Piece",
                secondarySpec = "Standard Coating",
                powerOrSizeRange = "SPH -1.00 / CYL -0.50",
                supplierName = "Tariq Jaddah Optical",
                leadTimeDays = 1
            )
        )
    }

    // Inline quick product fields when catalog is empty or "+ New Product" is selected
    var newProdCategory by remember { mutableStateOf(OpticalCategory.OPHTHALMIC_LENSES) }
    var newProdName by remember { mutableStateOf("") }
    var newProdSku by remember { mutableStateOf("CS-${(100..999).random()}") }
    var newProdCostPriceText by remember { mutableStateOf("750") }
    var newProdInitialStockText by remember { mutableStateOf("50") }

    var customerName by remember { mutableStateOf("Counter Sale (Walk-in)") }
    var customerPhone by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    var unitPriceText by remember(selectedProduct.id, useCustomNewProduct) {
        mutableStateOf(
            if (useCustomNewProduct || products.isEmpty()) "1200"
            else selectedProduct.wholesalePrice.toLong().toString()
        )
    }
    var discountText by remember { mutableStateOf("0") }

    var sphPowerInput by remember(selectedProduct.id) {
        mutableStateOf(
            extractSphFromText(selectedProduct.powerOrSizeRange).ifEmpty {
                if (selectedProduct.category != OpticalCategory.EYEGLASS_FRAMES) "-1.00" else ""
            }
        )
    }
    var cylPowerInput by remember(selectedProduct.id) {
        mutableStateOf(
            extractCylFromText(selectedProduct.powerOrSizeRange).ifEmpty {
                if (selectedProduct.category != OpticalCategory.EYEGLASS_FRAMES) "0.00 (Sph Only)" else ""
            }
        )
    }
    var counterNotes by remember {
        mutableStateOf("Instant Over-the-Counter Sale")
    }

    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var senderNumber by remember { mutableStateOf("Counter Walk-in") }
    var receiverNumber by remember { mutableStateOf(PaymentMethod.CASH.defaultReceiverNo) }
    var showCameraScanner by remember { mutableStateOf(false) }

    val parsedQty = (quantityText.toIntOrNull() ?: 1).coerceAtLeast(1)
    val unitPrice = (unitPriceText.toDoubleOrNull() ?: selectedProduct.wholesalePrice).coerceAtLeast(1.0)
    val discountPkr = (discountText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    val grossTotal = parsedQty * unitPrice
    val netTotal = (grossTotal - discountPkr).coerceAtLeast(0.0)

    var paidAmountText by remember(netTotal, selectedPaymentMethod) {
        mutableStateOf(
            if (selectedPaymentMethod == PaymentMethod.DEBT) "0"
            else netTotal.toLong().toString()
        )
    }
    val paidAmountPkr = (paidAmountText.toDoubleOrNull() ?: netTotal).coerceIn(0.0, netTotal)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PointOfSale,
                    contentDescription = "Counter Sale",
                    tint = Color(0xFF0D9488)
                )
                Text(
                    text = tr(
                        "Counter Sale (Instant POS Bill • SPH/CYL)",
                        "کاؤنٹر سیل (فوری نقد بل • SPH/CYL)"
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    color = Color(0xFF0D9488).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tr(
                            "⚡ Instant Walk-in Counter Sale: Deducts stock pieces immediately, marks sale as Delivered, and opens the Printable Receipt Bill (🧾).",
                            "⚡ فوری کاؤنٹر سیل: اسٹاک سے پیس منہا کر کے بل کو مکمل کرتی ہے اور فوری پرنٹ ایبل رسید (🧾) کھولتی ہے۔"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF0F766E),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                // Walk-in Customer Name & Optional Phone
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text(tr("Walk-in Buyer Name", "گاہک کا نام (کاؤنٹر)")) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.55f)
                            .testTag("counter_sale_customer_name")
                    )
                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = {
                            customerPhone = it
                            if (it.isNotBlank()) senderNumber = it
                        },
                        label = { Text(tr("Phone (Optional)", "فون نمبر")) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.45f)
                            .testTag("counter_sale_customer_phone")
                    )
                }

                // Product Picker or Quick New Product
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tr("Select Product or Enter Item:", "پراڈکٹ منتخب کریں یا نئی لکھیں:"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (products.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = { showCameraScanner = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(tr("Scan Barcode", "بارکوڈ اسکین"), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (products.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = useCustomNewProduct,
                                onClick = { useCustomNewProduct = true },
                                label = { Text(tr("+ New Item", "+ نئی آئٹم")) }
                            )
                        }
                        items(products, key = { it.id }) { prod ->
                            FilterChip(
                                selected = !useCustomNewProduct && selectedProduct.id == prod.id,
                                onClick = {
                                    useCustomNewProduct = false
                                    selectedProduct = prod
                                    unitPriceText = prod.wholesalePrice.toLong().toString()
                                    sphPowerInput = extractSphFromText(prod.powerOrSizeRange).ifEmpty {
                                        if (prod.category != OpticalCategory.EYEGLASS_FRAMES) "-1.00" else ""
                                    }
                                    cylPowerInput = extractCylFromText(prod.powerOrSizeRange).ifEmpty {
                                        if (prod.category != OpticalCategory.EYEGLASS_FRAMES) "0.00 (Sph Only)" else ""
                                    }
                                },
                                label = { Text("${prod.sku} (${prod.currentStock} Pcs)") }
                            )
                        }
                    }
                }

                if (useCustomNewProduct || products.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text(
                                text = tr(
                                    "Quick New Item (Also saves to your Catalog):",
                                    "فوری نئی آئٹم (آپ کے کیٹلاگ میں بھی محفوظ ہوگی):"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(OpticalCategory.entries) { cat ->
                                    FilterChip(
                                        selected = newProdCategory == cat,
                                        onClick = { newProdCategory = cat },
                                        label = { Text(if (isUrdu) cat.urduName else cat.displayName) }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = newProdName,
                                    onValueChange = { newProdName = it },
                                    label = { Text(tr("Item Name *", "آئٹم کا نام *")) },
                                    placeholder = { Text(tr("e.g. Blue-Cut Lens / Frame", "مثلاً بلیو کٹ لینز")) },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.65f)
                                )
                                OutlinedTextField(
                                    value = newProdSku,
                                    onValueChange = { newProdSku = it },
                                    label = { Text(tr("SKU", "کوڈ")) },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.35f)
                                )
                            }
                        }
                    }
                }

                // Quantity, Unit Price, and Discount Row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text(tr("Qty (Pieces)", "تعداد (پیس)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.33f)
                            .testTag("counter_sale_qty_input")
                    )
                    OutlinedTextField(
                        value = unitPriceText,
                        onValueChange = { unitPriceText = it },
                        label = { Text(tr("Price/Pc (PKR)", "قیمت فی پیس")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.34f)
                            .testTag("counter_sale_price_input")
                    )
                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { discountText = it },
                        label = { Text(tr("Discount (PKR)", "رعایت (PKR)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.33f)
                            .testTag("counter_sale_discount_input")
                    )
                }

                // Optical Lens SPH / CYL Power Selector
                OpticalSphCylSelectorSection(
                    sphValue = sphPowerInput,
                    cylValue = cylPowerInput,
                    onSphChange = { sphPowerInput = it },
                    onCylChange = { cylPowerInput = it },
                    contextLabelEn = "Counter Sale Lens SPH / CYL Power Options",
                    contextLabelUr = "کاؤنٹر سیل کے لیے لینز SPH / CYL نمبر"
                )

                // Payment Method Chips
                Text(
                    text = tr("Counter Payment Method:", "کاؤنٹر ادائیگی کا طریقہ:"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(PaymentMethod.entries) { method ->
                        FilterChip(
                            selected = selectedPaymentMethod == method,
                            onClick = {
                                selectedPaymentMethod = method
                                receiverNumber = method.defaultReceiverNo
                                paidAmountText = if (method == PaymentMethod.DEBT) "0" else netTotal.toLong().toString()
                            },
                            label = { Text(if (isUrdu) method.urduLabel else method.label) }
                        )
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TARIQ_MEHMOOD_RECEIVER_NUMBERS) { accNo ->
                        FilterChip(
                            selected = receiverNumber == accNo,
                            onClick = { receiverNumber = accNo },
                            label = { Text(accNo, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = counterNotes,
                    onValueChange = { counterNotes = it },
                    label = { Text(tr("Counter Bill Notes / Rx Details", "کاؤنٹر بل نوٹس")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Net Total Summary Box
                Surface(
                    color = Color(0xFF0D9488).copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = tr("Net Counter Bill Total:", "کل کاؤنٹر بل:"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatCurrency(netTotal),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F766E)
                            )
                        }
                        Text(
                            text = "$parsedQty ${tr("Pieces", "پیس")} × ${formatCurrency(unitPrice)}" +
                                if (discountPkr > 0) " (− PKR ${discountPkr.toLong()} Discount)" else "",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalRxNotes = mergeSphCylIntoNotes(
                        counterNotes.trim().ifEmpty { "Walk-in Counter Sale" },
                        sphPowerInput,
                        cylPowerInput
                    )
                    val productForCounterSale = if (useCustomNewProduct || products.isEmpty()) {
                        val initStock = (newProdInitialStockText.toIntOrNull() ?: 50).coerceAtLeast(parsedQty)
                        val costPkr = (newProdCostPriceText.toDoubleOrNull() ?: (unitPrice * 0.65)).coerceAtLeast(0.0)
                        OpticalProduct(
                            id = 0,
                            sku = newProdSku.trim().ifEmpty { "CS-${(100..999).random()}" },
                            barcode = "",
                            name = newProdName.trim().ifEmpty {
                                "${newProdCategory.displayName} (${formatSphCylPowerTag(sphPowerInput, cylPowerInput).ifEmpty { "Counter Item" }})"
                            },
                            brand = "Tariq Jaddah Optical",
                            category = newProdCategory,
                            wholesalePrice = unitPrice,
                            unitCostPkr = costPkr,
                            retailMsrp = unitPrice * 1.5,
                            minOrderQty = 1,
                            currentStock = initStock,
                            lowStockThreshold = 10,
                            primarySpec = "Counter & Wholesale Optical Piece",
                            secondarySpec = "Standard Finish",
                            powerOrSizeRange = formatSphCylPowerTag(sphPowerInput, cylPowerInput).ifEmpty { "Standard" },
                            supplierName = "Tariq Jaddah Optical",
                            leadTimeDays = 1
                        )
                    } else {
                        selectedProduct
                    }

                    onConfirmCounterSale(
                        customerName.trim().ifEmpty { "Counter Sale (Walk-in)" },
                        customerPhone.trim(),
                        productForCounterSale,
                        parsedQty,
                        unitPrice,
                        discountPkr,
                        finalRxNotes,
                        selectedPaymentMethod,
                        senderNumber.trim().ifEmpty { "Counter Customer" },
                        receiverNumber.trim().ifEmpty { selectedPaymentMethod.defaultReceiverNo },
                        paidAmountPkr
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0D9488),
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_counter_sale_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(tr("Complete Counter Sale & Receipt 🧾", "کاؤنٹر سیل مکمل کریں اور رسید نکالیں 🧾"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cancel", "منسوخ"))
            }
        }
    )

    if (showCameraScanner && products.isNotEmpty()) {
        CompactBarcodePickerDialog(
            products = products,
            title = tr("Scan Barcode for Counter Sale", "کاؤنٹر سیل کے لیے بارکوڈ اسکین کریں"),
            onDismiss = { showCameraScanner = false },
            onBarcodeScanned = { scannedCode ->
                val matched = products.firstOrNull {
                    it.barcode.equals(scannedCode, ignoreCase = true) ||
                        it.sku.equals(scannedCode, ignoreCase = true)
                }
                if (matched != null) {
                    useCustomNewProduct = false
                    selectedProduct = matched
                    unitPriceText = matched.wholesalePrice.toLong().toString()
                }
                showCameraScanner = false
            }
        )
    }
}
