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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.example.data.CategoryThresholdConfig
import com.example.data.OpticalCategory
import com.example.data.OpticalProduct
import com.example.data.RestockPurchaseOrder
import com.example.ui.LocalIsUrdu
import com.example.ui.WholesaleDashboardUiState
import com.example.ui.components.BarcodeLabelBadge
import com.example.ui.components.CategoryBadge
import com.example.ui.components.MetricStatCard
import com.example.ui.components.SphCylPowerBadge
import com.example.ui.components.StockHealthBadge
import com.example.ui.components.formatCurrency
import com.example.ui.theme.CriticalStockRed
import com.example.ui.theme.LowStockAmber
import com.example.ui.theme.LowStockAmberContainer
import com.example.ui.theme.StockHealthyGreen
import com.example.ui.tr

private enum class StockTabMode(val label: String, val urduLabel: String) {
    WAREHOUSE_STOCK("Warehouse Stock (Pieces)", "گودام اسٹاک (پیس)"),
    PUSH_THRESHOLDS("Push Thresholds", "پش الرٹ حد"),
    SUPPLIER_POS("Supplier POs", "سپلائر آرڈرز")
}

@Composable
fun InventoryScreen(
    uiState: WholesaleDashboardUiState,
    onToggleLowStockOnly: (Boolean) -> Unit,
    onCategorySelect: (OpticalCategory?) -> Unit,
    onAdjustStockAndThreshold: (OpticalProduct, Int, Int) -> Unit,
    onOpenAddNewStock: (OpticalProduct?) -> Unit,
    onUpdateCategoryThreshold: (OpticalCategory, Int, Boolean, Boolean) -> Unit,
    onTriggerPushBroadcast: () -> Unit,
    onPlaceManualRestock: (OpticalProduct) -> Unit,
    onBatchRestockAllLow: (List<OpticalProduct>) -> Unit,
    onCreateRestockOrder: (OpticalProduct, Int, String) -> Unit,
    onAdvancePoStatus: (RestockPurchaseOrder) -> Unit,
    onOpenBarcodeScanner: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isUrdu = LocalIsUrdu.current
    var activeSubTab by remember { mutableStateOf(StockTabMode.WAREHOUSE_STOCK) }
    var thresholdEditingProduct by remember { mutableStateOf<OpticalProduct?>(null) }
    var editingCategoryThreshold by remember { mutableStateOf<OpticalCategory?>(null) }

    val displayedProducts = remember(
        uiState.allProducts,
        uiState.selectedCategory,
        uiState.showOnlyLowStockInInventory
    ) {
        uiState.allProducts.filter { p ->
            val catMatch = uiState.selectedCategory == null || p.category == uiState.selectedCategory
            val lowMatch = !uiState.showOnlyLowStockInInventory || p.isLowStock
            catMatch && lowMatch
        }.sortedWith(
            compareByDescending<OpticalProduct> { it.isCriticalOut }
                .thenByDescending { it.isLowStock }
                .thenBy { it.currentStock }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Sub-mode switcher row
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                StockTabMode.entries.forEach { mode ->
                    val selected = activeSubTab == mode
                    FilterChip(
                        selected = selected,
                        onClick = { activeSubTab = mode },
                        label = {
                            Text(
                                text = if (isUrdu) mode.urduLabel else mode.label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier.testTag("stock_subtab_${mode.name.lowercase()}")
                    )
                }
            }
        }

        when (activeSubTab) {
            StockTabMode.WAREHOUSE_STOCK -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("inventory_screen_list")
                ) {
                    // KPI Metrics Row in PKR & Pieces
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MetricStatCard(
                                    title = tr("STOCK VALUE (PKR)", "اسٹاک کی مالیت"),
                                    value = formatCurrency(uiState.totalStockValueWholesale),
                                    subtitle = "${uiState.totalUnitsInStock} ${tr("Pieces in warehouse", "پیس گودام میں موجود")}",
                                    icon = Icons.Default.Savings,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricStatCard(
                                    title = tr("LOW STOCK ALERTS", "کم اسٹاک الرٹس"),
                                    value = "${uiState.lowStockProducts.size} SKUs",
                                    subtitle = if (uiState.lowStockProducts.isEmpty()) {
                                        tr("All above piece threshold", "تمام اسٹاک پورا ہے")
                                    } else {
                                        tr("Push alert active", "پش الرٹ فعال ہے")
                                    },
                                    icon = Icons.Default.WarningAmber,
                                    accentColor = if (uiState.lowStockProducts.isEmpty()) StockHealthyGreen else LowStockAmber,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Prominent Add New Stock (Pieces) & Scan Barcode Row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { onOpenBarcodeScanner(null) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("inventory_scan_barcode_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan Barcode",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tr("Scan Barcode", "بارکوڈ اسکین"),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = { onOpenAddNewStock(null) },
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("inventory_add_new_stock_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add New Stock Pieces",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tr(
                                            "+ Add Stock (Pieces)",
                                            "+ نیا اسٹاک (پیس)"
                                        ),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Batch Manual Reorder Card when items are below threshold
                    if (uiState.lowStockProducts.isNotEmpty()) {
                        item {
                            LowStockManualActionCard(
                                lowStockProducts = uiState.lowStockProducts,
                                onBatchRestockAll = { onBatchRestockAllLow(uiState.lowStockProducts) },
                                onConfigureThresholds = { activeSubTab = StockTabMode.PUSH_THRESHOLDS },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    // Filter Bar: Low-Stock Only Switch + Category Filter Chips
                    item {
                        InventoryFilterHeader(
                            showOnlyLowStock = uiState.showOnlyLowStockInInventory,
                            selectedCategory = uiState.selectedCategory,
                            lowStockCount = uiState.lowStockProducts.size,
                            onToggleLowStock = onToggleLowStockOnly,
                            onSelectCategory = onCategorySelect,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    if (displayedProducts.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Stock Healthy",
                                        tint = StockHealthyGreen,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = tr(
                                            "All filtered SKUs are above their piece thresholds!",
                                            "تمام پراڈکٹس کا اسٹاک مقررہ پیس حد سے زیادہ ہے!"
                                        ),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    if (uiState.showOnlyLowStockInInventory) {
                                        OutlinedButton(onClick = { onToggleLowStockOnly(false) }) {
                                            Text(tr("Show All Stock Items", "تمام اسٹاک دکھائیں"))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(
                            items = displayedProducts,
                            key = { it.id }
                        ) { product ->
                            InventoryStockControlCard(
                                product = product,
                                onStepStock = { delta ->
                                    val updated = (product.currentStock + delta).coerceAtLeast(0)
                                    onAdjustStockAndThreshold(product, updated, product.lowStockThreshold)
                                },
                                onAddNewStockPieces = { onOpenAddNewStock(product) },
                                onOpenThresholdEditor = { thresholdEditingProduct = product },
                                onPlaceManualOrder = { onPlaceManualRestock(product) },
                                onScanOrLookupBarcode = { onOpenBarcodeScanner(product.barcode) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }

            StockTabMode.PUSH_THRESHOLDS -> {
                PushThresholdConfigurationPanel(
                    uiState = uiState,
                    onEditCategoryThreshold = { cat -> editingCategoryThreshold = cat },
                    onEditProductThreshold = { prod -> thresholdEditingProduct = prod },
                    onTriggerPushBroadcast = onTriggerPushBroadcast
                )
            }

            StockTabMode.SUPPLIER_POS -> {
                RestockOrdersScreen(
                    uiState = uiState,
                    preselectedProductForPo = null,
                    onConsumePreselectedProduct = {},
                    onCreateRestockOrder = onCreateRestockOrder,
                    onAdvancePoStatus = onAdvancePoStatus,
                    onBatchRestockLowItems = onBatchRestockAllLow
                )
            }
        }
    }

    if (thresholdEditingProduct != null) {
        val product = thresholdEditingProduct!!
        ThresholdAndStockEditorDialog(
            product = product,
            onDismiss = { thresholdEditingProduct = null },
            onConfirm = { newStock, newThreshold ->
                onAdjustStockAndThreshold(product, newStock, newThreshold)
                thresholdEditingProduct = null
            }
        )
    }

    if (editingCategoryThreshold != null) {
        val cat = editingCategoryThreshold!!
        val currentConfig = uiState.categoryThresholds.find { it.category == cat }
        CategoryThresholdEditorDialog(
            category = cat,
            currentThreshold = currentConfig?.thresholdUnits ?: cat.defaultThreshold,
            currentPushEnabled = currentConfig?.pushAlertsEnabled ?: true,
            onDismiss = { editingCategoryThreshold = null },
            onSave = { newThresh, pushEnabled, applyToAll ->
                onUpdateCategoryThreshold(cat, newThresh, pushEnabled, applyToAll)
                editingCategoryThreshold = null
            }
        )
    }
}

@Composable
private fun PushThresholdConfigurationPanel(
    uiState: WholesaleDashboardUiState,
    onEditCategoryThreshold: (OpticalCategory) -> Unit,
    onEditProductThreshold: (OpticalProduct) -> Unit,
    onTriggerPushBroadcast: () -> Unit
) {
    val isUrdu = LocalIsUrdu.current
    LazyColumn(
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("push_thresholds_list")
    ) {
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Push Notification Engine",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = tr(
                                    "Low-Stock Push Notification Engine",
                                    "کم اسٹاک پش نوٹیفکیشن سسٹم (پیس)"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = tr(
                            "Configure minimum piece thresholds per optical category or per individual SKU. Whenever stock pieces drop at or below threshold, an Android push notification is immediately sent.",
                            "ہر کیٹیگری یا انفرادی پراڈکٹ کے لیے کم از کم پیس کی حد مقرر کریں۔ جب اسٹاک اس حد سے کم ہوگا تو فوری موبائل پش الرٹ ملے گا۔"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onTriggerPushBroadcast,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("broadcast_low_stock_push_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Test Push",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            tr(
                                "Send Push Alert Now (${uiState.lowStockProducts.size} Low SKUs)",
                                "فوری پش نوٹیفکیشن بھیجیں (${uiState.lowStockProducts.size} کم اسٹاک)"
                            )
                        )
                    }
                }
            }
        }

        // Category-Level Thresholds
        item {
            Text(
                text = tr("1. Category-Level Threshold Rules (Pieces)", "1. کیٹیگری کے لحاظ سے اسٹاک الرٹ کی حد (پیس)"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(OpticalCategory.entries) { category ->
            val config: CategoryThresholdConfig? =
                uiState.categoryThresholds.find { it.category == category }
            val thresholdVal = config?.thresholdUnits ?: category.defaultThreshold
            val pushEnabled = config?.pushAlertsEnabled ?: true
            val catProducts = uiState.allProducts.filter { it.category == category }
            val lowInCat = catProducts.count { it.isLowStock }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_threshold_card_${category.name.lowercase()}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CategoryBadge(category = category)
                        Surface(
                            color = if (pushEnabled) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (pushEnabled) tr("Push Alerts ON", "پش الرٹس فعال") else tr("Push Muted", "پش الرٹس بند"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = tr(
                                    "Category Default: ≤ $thresholdVal Pieces",
                                    "کیٹیگری الرٹ حد: ≤ $thresholdVal پیس"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${catProducts.size} SKUs • $lowInCat below threshold",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = { onEditCategoryThreshold(category) },
                            modifier = Modifier.testTag("edit_cat_threshold_${category.name.lowercase()}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Configure Category Threshold",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("Configure", "ترتیب دیں"))
                        }
                    }
                }
            }
        }

        // Per-Product Thresholds
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tr("2. Individual Product Thresholds (Per Piece)", "2. انفرادی پراڈکٹ کی اسٹاک الرٹ حد (پیس)"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(uiState.allProducts, key = { it.id }) { prod ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${prod.sku} • ${prod.name}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(
                                "Stock: ${prod.currentStock} Pieces | Alert Threshold: ≤ ${prod.lowStockThreshold} Pieces",
                                "موجودہ اسٹاک: ${prod.currentStock} پیس | الرٹ حد: ≤ ${prod.lowStockThreshold} پیس"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (prod.isLowStock) LowStockAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (prod.isLowStock) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    OutlinedButton(
                        onClick = { onEditProductThreshold(prod) }
                    ) {
                        Text(tr("Set Limit", "حد بدلنے"))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryThresholdEditorDialog(
    category: OpticalCategory,
    currentThreshold: Int,
    currentPushEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (Int, Boolean, Boolean) -> Unit
) {
    val isUrdu = LocalIsUrdu.current
    var thresholdText by remember { mutableStateOf(currentThreshold.toString()) }
    var pushEnabled by remember { mutableStateOf(currentPushEnabled) }
    var applyToAllInCat by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isUrdu) "${category.urduName} الرٹ حد" else "${category.displayName} Threshold",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = tr(
                        "Set the low-stock push alert threshold (in Pieces) for ${category.displayName}.",
                        "${category.urduName} کے لیے کم اسٹاک الرٹ کی حد (پیس میں) مقرر کریں۔"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { thresholdText = it },
                    label = {
                        Text(
                            tr(
                                "Category Alert Threshold (Pieces)",
                                "کم از کم پیس کی حد (Pieces)"
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_category_threshold")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tr("Enable Push Alerts for Category", "اس کیٹیگری کے پش الرٹس فعال رکھیں"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = pushEnabled,
                        onCheckedChange = { pushEnabled = it }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = applyToAllInCat,
                        onCheckedChange = { applyToAllInCat = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr(
                            "Apply this threshold to all existing SKUs in ${category.displayName} and trigger push check now",
                            "یہ حد اس کیٹیگری کی تمام موجودہ پراڈکٹس پر لاگو کریں"
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = thresholdText.toIntOrNull()?.coerceAtLeast(1) ?: category.defaultThreshold
                    onSave(parsed, pushEnabled, applyToAllInCat)
                },
                modifier = Modifier.testTag("save_cat_threshold_btn")
            ) {
                Text(tr("Save Category Rule", "محفوظ کریں"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cancel", "منسوخ"))
            }
        }
    )
}

@Composable
private fun LowStockManualActionCard(
    lowStockProducts: List<OpticalProduct>,
    onBatchRestockAll: () -> Unit,
    onConfigureThresholds: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = LowStockAmberContainer),
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, LowStockAmber, RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = "Low Stock Warning",
                    tint = Color(0xFF92400E),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(
                            "${lowStockProducts.size} Products Below Piece Threshold",
                            "${lowStockProducts.size} پراڈکٹس کا اسٹاک مقررہ پیس حد سے کم ہے"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF78350F)
                    )
                    Text(
                        text = tr(
                            "Push notifications active. Place manual POs or configure piece thresholds.",
                            "پش الرٹس فعال ہیں۔ سپلائر آرڈر دیں یا اسٹاک حد تبدیل کریں۔"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF92400E)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onBatchRestockAll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB45309),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("batch_manual_restock_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = "Place Manual Orders",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tr("Order All Low SKUs", "کم اسٹاک کا آرڈر دیں"))
                }
                OutlinedButton(
                    onClick = onConfigureThresholds,
                    modifier = Modifier.testTag("jump_to_thresholds_btn")
                ) {
                    Text(tr("Thresholds", "اسٹاک حد"))
                }
            }
        }
    }
}

@Composable
private fun InventoryFilterHeader(
    showOnlyLowStock: Boolean,
    selectedCategory: OpticalCategory?,
    lowStockCount: Int,
    onToggleLowStock: (Boolean) -> Unit,
    onSelectCategory: (OpticalCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrdu = LocalIsUrdu.current
    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(14.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = "Low Stock Filter",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = tr(
                                "Show Only Low-Stock SKUs ($lowStockCount)",
                                "صرف کم اسٹاک والی پراڈکٹس دکھائیں ($lowStockCount)"
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = tr(
                                "Filter items where Pieces ≤ Low-Stock Threshold",
                                "وہ آئٹمز جن کے پیس مقررہ حد سے کم ہیں"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = showOnlyLowStock,
                    onCheckedChange = onToggleLowStock,
                    modifier = Modifier.testTag("switch_low_stock_only")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onSelectCategory(null) },
                    label = { Text(tr("All Categories", "تمام کیٹیگریز")) }
                )
            }
            items(OpticalCategory.entries) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = {
                        onSelectCategory(if (selectedCategory == cat) null else cat)
                    },
                    label = { Text(if (isUrdu) cat.urduName else cat.displayName) }
                )
            }
        }
    }
}

@Composable
private fun InventoryStockControlCard(
    product: OpticalProduct,
    onStepStock: (Int) -> Unit,
    onAddNewStockPieces: () -> Unit,
    onOpenThresholdEditor: () -> Unit,
    onPlaceManualOrder: () -> Unit,
    onScanOrLookupBarcode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val stockRatio = remember(product.currentStock, product.lowStockThreshold) {
        val targetHealthy = (product.lowStockThreshold * 2.5f).coerceAtLeast(10f)
        (product.currentStock.toFloat() / targetHealthy).coerceIn(0.04f, 1f)
    }

    val barColor = when {
        product.isCriticalOut -> CriticalStockRed
        product.isLowStock -> LowStockAmber
        else -> StockHealthyGreen
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (product.isLowStock) 1.5.dp else 0.5.dp,
                color = if (product.isLowStock) LowStockAmber else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("inventory_card_${product.sku}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryBadge(category = product.category)
                    Text(
                        text = product.sku,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                StockHealthBadge(
                    currentStock = product.currentStock,
                    threshold = product.lowStockThreshold,
                    unitName = "Pieces"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            BarcodeLabelBadge(
                barcode = product.barcode,
                onClick = onScanOrLookupBarcode
            )
            if (product.powerOrSizeRange.contains("SPH", ignoreCase = true) ||
                product.powerOrSizeRange.contains("CYL", ignoreCase = true)
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                SphCylPowerBadge(rawText = product.powerOrSizeRange)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${product.primarySpec} • ${tr("Sale", "فروخت")}: ${formatCurrency(product.wholesalePrice)}/Pc • ${tr("Value", "مالیت")}: ${formatCurrency(product.currentStock * product.wholesalePrice)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Visual Progress Bar of Stock vs Threshold (Strictly Pieces)
            Column {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tr(
                            "On-Hand: ${product.currentStock} Pieces (پیس)",
                            "موجودہ اسٹاک: ${product.currentStock} پیس"
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(
                            "Push Alert Threshold: ≤ ${product.lowStockThreshold} Pieces",
                            "کم اسٹاک الرٹ حد: ≤ ${product.lowStockThreshold} پیس"
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (product.isLowStock) LowStockAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (product.isLowStock) FontWeight.Bold else FontWeight.Normal
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { stockRatio },
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(10.dp))

            // Quick Stock Stepper + Add Stock Button + Threshold Config
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = { onStepStock(-1) },
                        enabled = product.currentStock > 0,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("decrement_stock_${product.sku}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Remove 1 Piece",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${product.currentStock} Pcs",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }

                    FilledTonalIconButton(
                        onClick = { onStepStock(+1) },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("increment_stock_${product.sku}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add 1 Piece",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = onAddNewStockPieces,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("add_stock_card_btn_${product.sku}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add New Stock Pieces",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(tr("Add Stock", "نیا اسٹاک"))
                    }

                    OutlinedButton(
                        onClick = onOpenThresholdEditor,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("config_threshold_${product.sku}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Configure low-stock threshold",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(tr("Limit", "حد"))
                    }
                }
            }

            if (product.isLowStock) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onPlaceManualOrder,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LowStockAmber,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_order_low_${product.sku}")
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = "Manual Order",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        tr(
                            "Low Stock: Place Supplier PO (+${product.suggestedRestockQty} Pieces)",
                            "کم اسٹاک: سپلائر آرڈر دیں (+${product.suggestedRestockQty} پیس)"
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ThresholdAndStockEditorDialog(
    product: OpticalProduct,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var stockInput by remember { mutableStateOf(product.currentStock.toString()) }
    var thresholdInput by remember { mutableStateOf(product.lowStockThreshold.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = tr(
                    "Stock & Push Alert Threshold (${product.sku})",
                    "اسٹاک اور پش الرٹ کی حد (${product.sku})"
                ),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = tr(
                        "When physical stock (in Pieces) drops to or below the Low-Stock Threshold, Tariq Jaddah Optical dispatches a push notification and flags the item.",
                        "جب موجودہ پیس مقررہ حد سے کم ہوں گے تو طارق جدہ آپٹیکل فوری پش الرٹ بھیجے گا۔"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = stockInput,
                    onValueChange = { stockInput = it },
                    label = {
                        Text(
                            tr(
                                "Current Warehouse Stock (Pieces)",
                                "موجودہ گودام اسٹاک (پیس)"
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_stock_input")
                )

                OutlinedTextField(
                    value = thresholdInput,
                    onValueChange = { thresholdInput = it },
                    label = {
                        Text(
                            tr(
                                "Low-Stock Push Alert Threshold (Pieces)",
                                "کم اسٹاک پش الرٹ کی حد (پیس)"
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_threshold_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newStock = stockInput.toIntOrNull()?.coerceAtLeast(0) ?: product.currentStock
                    val newThreshold = thresholdInput.toIntOrNull()?.coerceAtLeast(1) ?: product.lowStockThreshold
                    onConfirm(newStock, newThreshold)
                },
                modifier = Modifier.testTag("confirm_threshold_save")
            ) {
                Text(tr("Save Threshold", "محفوظ کریں"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cancel", "منسوخ"))
            }
        }
    )
}
