package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.OpticalCategory
import com.example.data.OpticalProduct
import com.example.data.RestockOrderStatus
import com.example.data.RestockPurchaseOrder
import com.example.data.extractCylFromText
import com.example.data.extractSphFromText
import com.example.data.mergeSphCylIntoNotes
import com.example.ui.LocalIsUrdu
import com.example.ui.WholesaleDashboardUiState
import com.example.ui.components.CategoryBadge
import com.example.ui.components.MetricStatCard
import com.example.ui.components.OpticalSphCylSelectorSection
import com.example.ui.components.SphCylPowerBadge
import com.example.ui.components.formatCurrency
import com.example.ui.theme.LowStockAmber
import com.example.ui.theme.LowStockAmberContainer
import com.example.ui.theme.PrecisionTeal
import com.example.ui.theme.StockHealthyContainer
import com.example.ui.theme.StockHealthyGreen
import com.example.ui.tr

@Composable
fun RestockOrdersScreen(
    uiState: WholesaleDashboardUiState,
    preselectedProductForPo: OpticalProduct? = null,
    onConsumePreselectedProduct: () -> Unit = {},
    onCreateRestockOrder: (OpticalProduct, Int, String) -> Unit,
    onAdvancePoStatus: (RestockPurchaseOrder) -> Unit,
    onBatchRestockLowItems: (List<OpticalProduct>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreatePoDialog by remember { mutableStateOf(false) }
    var dialogTargetProduct by remember { mutableStateOf<OpticalProduct?>(null) }

    LaunchedEffect(preselectedProductForPo) {
        if (preselectedProductForPo != null) {
            dialogTargetProduct = preselectedProductForPo
            showCreatePoDialog = true
            onConsumePreselectedProduct()
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("restock_orders_screen_list")
    ) {
        // Top KPI Row
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MetricStatCard(
                    title = tr("Low-Stock SKUs", "کم اسٹاک آئٹمز"),
                    value = "${uiState.lowStockProducts.size} Alerts",
                    subtitle = tr("Need manual restock (Pieces)", "مزید پیس منگوانے کی ضرورت"),
                    icon = Icons.Default.Warning,
                    accentColor = LowStockAmber,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = tr("Active Lab POs", "سپلائر آرڈرز"),
                    value = "${uiState.pendingSupplierPoCount} Open",
                    subtitle = "${uiState.restockOrders.size} ${tr("total POs logged", "کل سپلائر آرڈرز")}",
                    icon = Icons.Default.Factory,
                    accentColor = PrecisionTeal,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Low-Stock Threshold Quick-Order Carousel
        if (uiState.lowStockProducts.isNotEmpty()) {
            item {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = tr(
                                "SKUs Below Piece Threshold — Place Manual PO",
                                "مقررہ پیس حد سے کم اسٹاک — سپلائر آرڈر دیں"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { onBatchRestockLowItems(uiState.lowStockProducts) }
                        ) {
                            Text(tr("Order All", "سب منگوائیں"))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.lowStockProducts, key = { it.id }) { lowSku ->
                            LowStockQuickOrderCard(
                                product = lowSku,
                                onPlaceOrderClick = {
                                    dialogTargetProduct = lowSku
                                    showCreatePoDialog = true
                                }
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StockHealthyContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr(
                                    "All Optical SKUs Above Piece Threshold",
                                    "تمام آپٹیکل پراڈکٹس کا اسٹاک پورا ہے"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF065F46),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "You can still place a manual supplier PO (in Pieces) at any time.",
                                    "آپ کسی بھی وقت سپلائر کو مزید پیس کا آرڈر دے سکتے ہیں۔"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF047857)
                            )
                        }
                        Button(
                            onClick = {
                                dialogTargetProduct = null
                                showCreatePoDialog = true
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(tr("New PO", "نیا آرڈر"))
                        }
                    }
                }
            }
        }

        // Header for Supplier Purchase Orders List
        item {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = tr("Manual Supplier Purchase Orders (Pieces)", "سپلائر پرچیز آرڈرز (پیس)"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        dialogTargetProduct = null
                        showCreatePoDialog = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("new_manual_po_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = "New Manual Supplier Order",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tr("Place Manual PO", "سپلائر آرڈر دیں"))
                }
            }
        }

        // Purchase Orders List
        items(uiState.restockOrders, key = { it.id }) { po ->
            RestockPurchaseOrderCard(
                po = po,
                onAdvanceStatus = { onAdvancePoStatus(po) }
            )
        }
    }

    if (showCreatePoDialog) {
        ManualRestockOrderDialog(
            products = uiState.allProducts,
            preselectedProduct = dialogTargetProduct,
            onDismiss = {
                showCreatePoDialog = false
                dialogTargetProduct = null
            },
            onConfirmRestock = { prod, qty, notes ->
                onCreateRestockOrder(prod, qty, notes)
                showCreatePoDialog = false
                dialogTargetProduct = null
            }
        )
    }
}

@Composable
private fun LowStockQuickOrderCard(
    product: OpticalProduct,
    onPlaceOrderClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LowStockAmberContainer),
        modifier = Modifier
            .width(265.dp)
            .border(1.5.dp, LowStockAmber.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .testTag("low_stock_quick_card_${product.sku}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = product.sku,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF78350F),
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = LowStockAmber,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${product.currentStock} / Min ${product.lowStockThreshold} Pcs",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF451A03),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = product.supplierName,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF78350F),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onPlaceOrderClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF78350F),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_manual_order_${product.sku}")
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = "Place Manual Order",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(tr("Order +${product.suggestedRestockQty} Pieces", "+${product.suggestedRestockQty} پیس منگوائیں"))
            }
        }
    }
}

@Composable
private fun RestockPurchaseOrderCard(
    po: RestockPurchaseOrder,
    onAdvanceStatus: () -> Unit
) {
    val isUrdu = LocalIsUrdu.current
    val (statusBg, statusFg) = when (po.status) {
        RestockOrderStatus.ORDERED -> Pair(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        RestockOrderStatus.IN_TRANSIT -> Pair(
            LowStockAmberContainer,
            Color(0xFF78350F)
        )
        RestockOrderStatus.RECEIVED_INTO_STOCK -> Pair(
            StockHealthyContainer,
            StockHealthyGreen
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("restock_po_card_${po.poNumber}")
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
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = po.poNumber,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    CategoryBadge(category = po.category)
                }

                Surface(
                    color = statusBg,
                    contentColor = statusFg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isUrdu) po.status.urduLabel else po.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${po.productSku} — ${po.productName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${tr("Supplier / Lab", "سپلائر / لیب")}: ${po.supplierName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "+${po.orderQuantity} ${tr("Pieces", "پیس")}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Est. ${formatCurrency(po.totalCost)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(10.dp)
            ) {
                SphCylPowerBadge(
                    rawText = po.manualNotes,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "${tr("PO Notes", "نوٹس")}: ${po.manualNotes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (po.status != RestockOrderStatus.RECEIVED_INTO_STOCK) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onAdvanceStatus,
                    colors = if (po.status == RestockOrderStatus.IN_TRANSIT) {
                        ButtonDefaults.buttonColors(
                            containerColor = StockHealthyGreen,
                            contentColor = Color.White
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("advance_po_${po.poNumber}")
                ) {
                    Icon(
                        imageVector = if (po.status == RestockOrderStatus.IN_TRANSIT) {
                            Icons.Default.Inventory
                        } else {
                            Icons.Default.LocalShipping
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (po.status == RestockOrderStatus.ORDERED) {
                            tr("Mark Shipment In Transit from Supplier", "سپلائر سے مال روانہ ہو چکا ہے")
                        } else {
                            tr(
                                "Receive Shipment Into Stock (+${po.orderQuantity} Pieces)",
                                "اسٹاک میں وصول کریں (+${po.orderQuantity} پیس شامل کریں)"
                            )
                        }
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Stock Replenished",
                        tint = StockHealthyGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr(
                            "Stock replenished (+${po.orderQuantity} Pieces added to warehouse inventory)",
                            "اسٹاک شامل ہو گیا (+${po.orderQuantity} پیس گودام میں جمع)"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = StockHealthyGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun ManualRestockOrderDialog(
    products: List<OpticalProduct>,
    preselectedProduct: OpticalProduct?,
    onDismiss: () -> Unit,
    onOpenNewProductDialog: () -> Unit = {},
    onConfirmRestock: (OpticalProduct, Int, String) -> Unit
) {
    if (products.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = tr("No Products in Catalog Yet", "کیٹلاگ میں ابھی کوئی پراڈکٹ نہیں ہے"),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = tr(
                        "Please add a product to your catalog first before creating a supplier restock purchase order.",
                        "سپلائر ری اسٹاک آرڈر بنانے سے پہلے کیٹلاگ میں اپنی پہلی پراڈکٹ شامل کریں۔"
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismiss()
                        onOpenNewProductDialog()
                    }
                ) {
                    Text(tr("+ Add New Product", "+ نئی پراڈکٹ شامل کریں"))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(tr("Close", "بند کریں"))
                }
            }
        )
        return
    }

    val initialProd = preselectedProduct ?: products.firstOrNull { it.isLowStock } ?: products.first()
    var selectedProduct by remember { mutableStateOf(initialProd) }
    var quantityInput by remember { mutableStateOf(initialProd.suggestedRestockQty.toString()) }
    var sphPowerInput by remember {
        mutableStateOf(
            extractSphFromText(initialProd.powerOrSizeRange).ifEmpty {
                if (initialProd.category != OpticalCategory.EYEGLASS_FRAMES) "-1.25" else ""
            }
        )
    }
    var cylPowerInput by remember {
        mutableStateOf(
            extractCylFromText(initialProd.powerOrSizeRange).ifEmpty {
                if (initialProd.category != OpticalCategory.EYEGLASS_FRAMES) "-0.50" else ""
            }
        )
    }
    var manualNotes by remember {
        mutableStateOf(
            "Manual restock order placed at stock ${initialProd.currentStock} Pieces (Threshold ≤ ${initialProd.lowStockThreshold} Pieces)."
        )
    }

    val qty = (quantityInput.toIntOrNull() ?: selectedProduct.suggestedRestockQty).coerceAtLeast(1)
    val estimatedUnitCost = selectedProduct.unitCostPkr
    val estimatedTotal = estimatedUnitCost * qty

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = tr(
                    "Place Manual Supplier Restock PO (Pieces & SPH/CYL)",
                    "سپلائر کو نیا اسٹاک آرڈر دیں (پیس اور SPH/CYL)"
                ),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = tr("Select Optical SKU to Restock:", "ری اسٹاک کے لیے پراڈکٹ منتخب کریں:"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(products, key = { it.id }) { prod ->
                        FilterChip(
                            selected = selectedProduct.id == prod.id,
                            onClick = {
                                selectedProduct = prod
                                quantityInput = prod.suggestedRestockQty.toString()
                                sphPowerInput = extractSphFromText(prod.powerOrSizeRange).ifEmpty {
                                    if (prod.category != OpticalCategory.EYEGLASS_FRAMES) "-1.25" else ""
                                }
                                cylPowerInput = extractCylFromText(prod.powerOrSizeRange).ifEmpty {
                                    if (prod.category != OpticalCategory.EYEGLASS_FRAMES) "-0.50" else ""
                                }
                                manualNotes =
                                    "Manual restock order placed at stock ${prod.currentStock} Pieces (Threshold ≤ ${prod.lowStockThreshold} Pieces)."
                            },
                            label = {
                                Text(
                                    text = if (prod.isLowStock) {
                                        "⚠️ ${prod.sku} (${prod.currentStock}/${prod.lowStockThreshold} Pcs)"
                                    } else {
                                        "${prod.sku} (${prod.currentStock} Pcs)"
                                    }
                                )
                            }
                        )
                    }
                }

                Surface(
                    color = if (selectedProduct.isLowStock) LowStockAmberContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "${selectedProduct.sku} — ${selectedProduct.name}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Supplier: ${selectedProduct.supplierName} • Lead Time: ${selectedProduct.leadTimeDays} days",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = tr(
                                "Current Stock: ${selectedProduct.currentStock} Pieces | Alert Threshold: ≤ ${selectedProduct.lowStockThreshold} Pieces",
                                "موجودہ اسٹاک: ${selectedProduct.currentStock} پیس | الرٹ حد: ≤ ${selectedProduct.lowStockThreshold} پیس"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                OpticalSphCylSelectorSection(
                    sphValue = sphPowerInput,
                    cylValue = cylPowerInput,
                    onSphChange = { sphPowerInput = it },
                    onCylChange = { cylPowerInput = it },
                    contextLabelEn = "Supplier Restock PO Lens SPH / CYL Options",
                    contextLabelUr = "سپلائر ری اسٹاک آرڈر کے لیے SPH / CYL نمبر"
                )

                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = { quantityInput = it },
                    label = { Text(tr("Manual Order Quantity (Pieces)", "منگوانے کے پیس کی تعداد (Pieces)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_manual_restock_qty")
                )

                OutlinedTextField(
                    value = manualNotes,
                    onValueChange = { manualNotes = it },
                    label = { Text(tr("Supplier PO Instructions / Batch Spec", "سپلائر آرڈر کی تفصیلات")) },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_manual_restock_notes")
                )

                Text(
                    text = tr(
                        "Estimated PO Cost: ${formatCurrency(estimatedTotal)} (${formatCurrency(estimatedUnitCost)} / Piece)",
                        "متوقع لاگت: ${formatCurrency(estimatedTotal)} (${formatCurrency(estimatedUnitCost)} فی پیس)"
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalNotes = mergeSphCylIntoNotes(manualNotes, sphPowerInput, cylPowerInput)
                    onConfirmRestock(selectedProduct, qty, finalNotes)
                },
                modifier = Modifier.testTag("confirm_manual_restock_btn")
            ) {
                Text(tr("Place Manual Supplier PO", "سپلائر آرڈر محفوظ کریں"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cancel", "منسوخ"))
            }
        }
    )
}
