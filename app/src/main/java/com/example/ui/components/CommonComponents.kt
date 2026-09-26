package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lens
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.OPTICAL_CYL_OPTIONS
import com.example.data.OPTICAL_SPH_MINUS_OPTIONS
import com.example.data.OPTICAL_SPH_PLUS_OPTIONS
import com.example.data.OPTICAL_SPH_RANGE_OPTIONS
import com.example.data.OpticalCategory
import com.example.data.OpticalProduct
import com.example.data.PaymentMethod
import com.example.data.RetailOrderStage
import com.example.data.StockPushAlert
import com.example.data.extractCylFromText
import com.example.data.extractSphCylBadgeFromText
import com.example.data.extractSphFromText
import com.example.data.formatSphCylPowerTag
import com.example.data.mergeSphCylIntoNotes
import com.example.ui.LocalIsUrdu
import com.example.ui.theme.CriticalStockContainer
import com.example.ui.theme.CriticalStockRed
import com.example.ui.theme.LowStockAmber
import com.example.ui.theme.LowStockAmberContainer
import com.example.ui.theme.StockHealthyContainer
import com.example.ui.theme.StockHealthyGreen
import com.example.ui.tr
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatCurrency(amount: Double): String {
    val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    return "PKR ${numberFormat.format(amount)}"
}

fun formatShortDate(epochMs: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
    return sdf.format(Date(epochMs))
}

@Composable
fun CategoryBadge(
    category: OpticalCategory,
    modifier: Modifier = Modifier
) {
    val isUrdu = LocalIsUrdu.current
    val (bgColor, fgColor, icon) = when (category) {
        OpticalCategory.EYEGLASS_FRAMES -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.Visibility
        )
        OpticalCategory.OPHTHALMIC_LENSES -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Default.Lens
        )
        OpticalCategory.CONTACT_LENSES -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            Icons.Default.RemoveRedEye
        )
    }

    Surface(
        color = bgColor,
        contentColor = fgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = category.displayName,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isUrdu) category.urduName else category.displayName,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun PaymentMethodBadge(
    method: PaymentMethod,
    modifier: Modifier = Modifier
) {
    val isUrdu = LocalIsUrdu.current
    val (bg, fg) = when (method) {
        PaymentMethod.CASH -> Pair(StockHealthyContainer, StockHealthyGreen)
        PaymentMethod.DEBT -> Pair(CriticalStockContainer, CriticalStockRed)
        PaymentMethod.JAZZCASH -> Pair(Color(0xFFFEE2E2), Color(0xFFB91C1C))
        PaymentMethod.EASYPAISA -> Pair(Color(0xFFD1FAE5), Color(0xFF047857))
    }

    Surface(
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = method.label,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isUrdu) method.urduLabel else method.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StockHealthBadge(
    currentStock: Int,
    threshold: Int,
    unitName: String = "Pieces",
    modifier: Modifier = Modifier
) {
    val isUrdu = LocalIsUrdu.current
    val isOut = currentStock == 0
    val isLow = currentStock <= threshold

    val bg = when {
        isOut -> CriticalStockContainer
        isLow -> LowStockAmberContainer
        else -> StockHealthyContainer
    }
    val fg = when {
        isOut -> CriticalStockRed
        isLow -> LowStockAmber
        else -> StockHealthyGreen
    }
    val label = if (isUrdu) {
        when {
            isOut -> "اسٹاک ختم (0 / کم از کم $threshold پیس)"
            isLow -> "کم اسٹاک: $currentStock پیس (حد ≤$threshold)"
            else -> "موجود اسٹاک: $currentStock پیس"
        }
    } else {
        when {
            isOut -> "OUT OF STOCK (0 / Min $threshold Pcs)"
            isLow -> "LOW STOCK: $currentStock Pieces (≤$threshold)"
            else -> "IN STOCK: $currentStock Pieces"
        }
    }

    Surface(
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = if (isLow) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = label,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LowStockThresholdAlertBanner(
    lowStockCount: Int,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = lowStockCount > 0) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = LowStockAmberContainer
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = modifier
                .fillMaxWidth()
                .border(1.5.dp, LowStockAmber.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .clickable(onClick = onActionClick)
                .testTag("low_stock_alert_banner")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(LowStockAmber)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Low Stock Push Alert",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(
                            "Push Alert: $lowStockCount Products Below Piece Threshold",
                            "پش الرٹ: $lowStockCount پراڈکٹس کا اسٹاک مقررہ حد سے کم ہے"
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF78350F),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(
                            "Tap to add new stock pieces or configure low-stock thresholds.",
                            "نیا اسٹاک (پیس) شامل کرنے یا حد تبدیل کرنے کے لیے کلک کریں۔"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF92400E)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Inspect Low Stock",
                    tint = Color(0xFF78350F)
                )
            }
        }
    }
}

@Composable
fun AddNewStockDialog(
    products: List<OpticalProduct>,
    preselectedProduct: OpticalProduct?,
    onDismiss: () -> Unit,
    onOpenNewProductDialog: () -> Unit = {},
    onConfirmAddStock: (
        product: OpticalProduct,
        addedPieces: Int,
        updatedWholesalePricePkr: Double,
        updatedUnitCostPkr: Double,
        batchNotes: String
    ) -> Unit
) {
    if (products.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = tr("Add Your First Product First", "پہلے اپنی پراڈکٹ شامل کریں"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = tr(
                        "Your product catalog is currently empty. Add your own optical frames, lenses, or contact lenses first, or create a New Order / Counter Sale to add products directly.",
                        "آپ کا کیٹلاگ اس وقت خالی ہے۔ اسٹاک بڑھانے سے پہلے اپنی نئی پراڈکٹ شامل کریں۔"
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

    val initialProd = preselectedProduct ?: products.first()
    var selectedProduct by remember { mutableStateOf(initialProd) }
    var piecesToAddInput by remember { mutableStateOf("25") }
    var wholesalePriceInput by remember { mutableStateOf(initialProd.wholesalePrice.toLong().toString()) }
    var unitCostInput by remember { mutableStateOf(initialProd.unitCostPkr.toLong().toString()) }
    var sphPowerInput by remember {
        mutableStateOf(
            extractSphFromText(initialProd.powerOrSizeRange).ifEmpty {
                if (initialProd.category != OpticalCategory.EYEGLASS_FRAMES) "-1.00" else ""
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
    var batchNotes by remember {
        mutableStateOf("New stock shipment added to Tariq Jaddah Optical warehouse (Pieces).")
    }

    val addedPieces = (piecesToAddInput.toIntOrNull() ?: 1).coerceAtLeast(1)
    val newTotalPieces = selectedProduct.currentStock + addedPieces
    val updatedWPrice = wholesalePriceInput.toDoubleOrNull() ?: selectedProduct.wholesalePrice
    val updatedCost = unitCostInput.toDoubleOrNull() ?: selectedProduct.unitCostPkr

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = tr(
                    "Add New Stock (Pieces & SPH/CYL)",
                    "نیا اسٹاک شامل کریں (پیس اور SPH/CYL)"
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
                    text = tr("Select Product SKU:", "پراڈکٹ منتخب کریں:"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(products, key = { it.id }) { prod ->
                        FilterChip(
                            selected = selectedProduct.id == prod.id,
                            onClick = {
                                selectedProduct = prod
                                wholesalePriceInput = prod.wholesalePrice.toLong().toString()
                                unitCostInput = prod.unitCostPkr.toLong().toString()
                                sphPowerInput = extractSphFromText(prod.powerOrSizeRange).ifEmpty {
                                    if (prod.category != OpticalCategory.EYEGLASS_FRAMES) "-1.00" else ""
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

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
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
                            text = tr(
                                "Current Stock: ${selectedProduct.currentStock} Pieces → After Adding: $newTotalPieces Pieces",
                                "موجودہ اسٹاک: ${selectedProduct.currentStock} پیس ← نیا کل اسٹاک: $newTotalPieces پیس"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                OpticalSphCylSelectorSection(
                    sphValue = sphPowerInput,
                    cylValue = cylPowerInput,
                    onSphChange = { sphPowerInput = it },
                    onCylChange = { cylPowerInput = it },
                    contextLabelEn = "Restock Lens SPH / CYL Power Options",
                    contextLabelUr = "نئے اسٹاک کے لیے لینز SPH / CYL نمبر"
                )

                OutlinedTextField(
                    value = piecesToAddInput,
                    onValueChange = { piecesToAddInput = it },
                    label = {
                        Text(
                            tr(
                                "New Stock Quantity to Add (Pieces)",
                                "شامل کرنے کے لیے نئے پیس کی تعداد (Pieces)"
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_add_stock_pieces")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = wholesalePriceInput,
                        onValueChange = { wholesalePriceInput = it },
                        label = { Text(tr("Sale Price/Piece (PKR)", "فروخت قیمت فی پیس")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unitCostInput,
                        onValueChange = { unitCostInput = it },
                        label = { Text(tr("Cost/Piece (PKR)", "خرید قیمت فی پیس")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = batchNotes,
                    onValueChange = { batchNotes = it },
                    label = { Text(tr("Supplier / Stock Batch Notes", "سپلائر / اسٹاک نوٹس")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mergedNotes = mergeSphCylIntoNotes(batchNotes, sphPowerInput, cylPowerInput)
                    val updatedSpecRange = if (sphPowerInput.isNotBlank() || cylPowerInput.isNotBlank()) {
                        formatSphCylPowerTag(sphPowerInput, cylPowerInput)
                    } else {
                        selectedProduct.powerOrSizeRange
                    }
                    onConfirmAddStock(
                        selectedProduct.copy(powerOrSizeRange = updatedSpecRange),
                        addedPieces,
                        updatedWPrice,
                        updatedCost,
                        mergedNotes
                    )
                },
                modifier = Modifier.testTag("confirm_add_new_stock_btn")
            ) {
                Text(tr("Add +$addedPieces Pieces to Stock", "+$addedPieces پیس اسٹاک میں شامل کریں"))
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
fun OpticalSphCylSelectorSection(
    sphValue: String,
    cylValue: String,
    onSphChange: (String) -> Unit,
    onCylChange: (String) -> Unit,
    contextLabelEn: String = "Optical Glasses Lenses SPH / CYL Options",
    contextLabelUr: String = "نظر کے لینز SPH / CYL پاور آپشنز",
    modifier: Modifier = Modifier
) {
    var sphMode by remember {
        mutableStateOf(
            when {
                sphValue.startsWith("+") -> 1
                sphValue.contains("to", ignoreCase = true) -> 2
                else -> 0
            }
        )
    }

    val currentSphOptions = when (sphMode) {
        1 -> OPTICAL_SPH_PLUS_OPTIONS
        2 -> OPTICAL_SPH_RANGE_OPTIONS
        else -> OPTICAL_SPH_MINUS_OPTIONS
    }

    val powerSummary = formatSphCylPowerTag(sphValue, cylValue)

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("optical_sph_cyl_selector_section")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lens,
                        contentDescription = "Lens SPH CYL",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = tr(contextLabelEn, contextLabelUr),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (powerSummary.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = powerSummary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // SPH Sign / Range Mode Filter Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = sphMode == 0,
                    onClick = { sphMode = 0 },
                    label = { Text(tr("Minus (-) SPH", "مائنس (-) SPH")) },
                    modifier = Modifier.testTag("sph_mode_minus_chip")
                )
                FilterChip(
                    selected = sphMode == 1,
                    onClick = { sphMode = 1 },
                    label = { Text(tr("Plus (+) SPH", "پلس (+) SPH")) },
                    modifier = Modifier.testTag("sph_mode_plus_chip")
                )
                FilterChip(
                    selected = sphMode == 2,
                    onClick = { sphMode = 2 },
                    label = { Text(tr("Plano / Range", "سادہ / رینج")) },
                    modifier = Modifier.testTag("sph_mode_range_chip")
                )
            }

            Text(
                text = tr(
                    "1. Select SPH (Spherical Power / سفیرکل نمبر):",
                    "1. سفیرکل پاور (SPH) منتخب کریں:"
                ),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(currentSphOptions) { sphOpt ->
                    FilterChip(
                        selected = sphValue.equals(sphOpt, ignoreCase = true),
                        onClick = { onSphChange(sphOpt) },
                        label = { Text("SPH $sphOpt") },
                        modifier = Modifier.testTag("sph_option_chip_$sphOpt")
                    )
                }
            }

            Text(
                text = tr(
                    "2. Select CYL (Cylindrical Power / سلنڈر نمبر):",
                    "2. سلنڈر پاور (CYL) منتخب کریں:"
                ),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(OPTICAL_CYL_OPTIONS) { cylOpt ->
                    FilterChip(
                        selected = cylValue.equals(cylOpt, ignoreCase = true),
                        onClick = { onCylChange(cylOpt) },
                        label = { Text("CYL $cylOpt") },
                        modifier = Modifier.testTag("cyl_option_chip_$cylOpt")
                    )
                }
            }

            // Direct Custom SPH & CYL Input Fields
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = sphValue,
                    onValueChange = onSphChange,
                    label = { Text(tr("SPH Power (e.g. -1.50)", "SPH نمبر")) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_sph_power")
                )
                OutlinedTextField(
                    value = cylValue,
                    onValueChange = onCylChange,
                    label = { Text(tr("CYL Power (e.g. -0.75)", "CYL نمبر")) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_cyl_power")
                )
                if (sphValue.isNotBlank() || cylValue.isNotBlank()) {
                    IconButton(
                        onClick = {
                            onSphChange("")
                            onCylChange("")
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear SPH/CYL",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SphCylPowerBadge(
    rawText: String,
    modifier: Modifier = Modifier
) {
    val badgeText = extractSphCylBadgeFromText(rawText) ?: return
    Surface(
        color = Color(0xFFE0F2FE),
        contentColor = Color(0xFF0369A1),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.border(1.dp, Color(0xFF7DD3FC), RoundedCornerShape(8.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lens,
                contentDescription = "SPH CYL Power",
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PushNotificationCenterDialog(
    alerts: List<StockPushAlert>,
    lowStockCount: Int,
    onTriggerPushNow: () -> Unit,
    onClearHistory: () -> Unit,
    onJumpToInventory: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Push Alerts",
                    tint = LowStockAmber
                )
                Text(
                    text = tr("Low-Stock Push Notification Center", "کم اسٹاک پش الرٹ سینٹر"),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = tr(
                        "Real-time push alerts are dispatched whenever any product's piece count drops at or below its configured threshold.",
                        "جب کسی بھی پراڈکٹ کے پیس مقررہ حد سے کم ہوتے ہیں تو فوری پش نوٹیفکیشن بھیجا جاتا ہے۔"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onTriggerPushNow,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("send_test_push_btn")
                    ) {
                        Text(tr("Send Push Alert ($lowStockCount Low)", "پش الرٹ بھیجیں ($lowStockCount کم)"))
                    }
                    if (alerts.isNotEmpty()) {
                        OutlinedButton(onClick = onClearHistory) {
                            Text(tr("Clear", "صاف کریں"))
                        }
                    }
                }

                HorizontalDivider()

                if (alerts.isEmpty()) {
                    Text(
                        text = tr("No push notifications logged yet.", "ابھی تک کوئی الرٹ موجود نہیں۔"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                    ) {
                        items(alerts, key = { it.id }) { alert ->
                            Surface(
                                color = LowStockAmberContainer.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "${alert.productSku} • ${alert.category.displayName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF78350F),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Stock: ${alert.currentStock} Pcs (≤${alert.threshold})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CriticalStockRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = alert.productName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFF451A03)
                                    )
                                    Text(
                                        text = alert.triggerReason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF78350F)
                                    )
                                    Text(
                                        text = formatShortDate(alert.createdAtEpochMs),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onJumpToInventory()
                }
            ) {
                Text(tr("Configure Thresholds", "اسٹاک حد سیٹ کریں"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Close", "بند کریں"))
            }
        }
    )
}

@Composable
fun StatusFeedbackToast(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = message != null) {
        if (message != null) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 6.dp,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("status_feedback_banner")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Update Notification",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss notification",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun OrderPipelineStepper(
    currentStage: RetailOrderStage,
    modifier: Modifier = Modifier
) {
    val isUrdu = LocalIsUrdu.current
    val stages = RetailOrderStage.entries
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            stages.forEachIndexed { index, stage ->
                val isCompleted = index <= currentStage.stepIndex
                val isCurrent = index == currentStage.stepIndex
                val dotColor = when {
                    isCurrent -> MaterialTheme.colorScheme.secondary
                    isCompleted -> StockHealthyGreen
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(if (isCurrent) 24.dp else 18.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCompleted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (index < stages.lastIndex) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .padding(horizontal = 4.dp)
                            .background(
                                if (index < currentStage.stepIndex) StockHealthyGreen
                                else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isUrdu) "مرحلہ: ${currentStage.urduLabel}" else "Stage: ${currentStage.label}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isUrdu) "مرحلہ ${currentStage.stepIndex + 1} از ${stages.size}" else "Step ${currentStage.stepIndex + 1} of ${stages.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Professional Brand Logo Emblem & Lockup containing the app name "TARIQ JADDAH OPTICAL".
 */
@Composable
fun TariqJaddahBrandLogoBanner(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Surface(
        color = Color(0xFF0A192F),
        contentColor = Color.White,
        shape = RoundedCornerShape(if (compact) 14.dp else 18.dp),
        modifier = modifier
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFD97706),
                        Color(0xFF38BDF8),
                        Color(0xFFF59E0B)
                    )
                ),
                shape = RoundedCornerShape(if (compact) 14.dp else 18.dp)
            )
            .testTag("tariq_jaddah_professional_logo")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
            modifier = Modifier.padding(
                horizontal = if (compact) 12.dp else 16.dp,
                vertical = if (compact) 8.dp else 12.dp
            )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(if (compact) 46.dp else 64.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFFBBF24), Color(0xFF38BDF8))
                        ),
                        shape = CircleShape
                    )
                    .background(Color(0xFF0A192F))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_tariq_jaddah_logo),
                    contentDescription = "Tariq Jaddah Optical Official Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(if (compact) 42.dp else 60.dp)
                        .clip(CircleShape)
                )
            }

            Column(modifier = Modifier.weight(1f, fill = false)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                        contentColor = Color(0xFFFBBF24),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = tr("OFFICIAL BRAND • EST. 1947", "آفیشل برانڈ • طارق جدہ آپٹیکل"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tr("TARIQ JADDAH OPTICAL", "طارق جدہ آپٹیکل"),
                    style = if (compact) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tr(
                        "Wholesale & Counter Optical Dealer • Frames, SPH/CYL Lenses & Khata",
                        "ہول سیل اور کاؤنٹر سیل ڈیلر • فریم، نظر کے لینز اور کھاتہ سسٹم"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF7DD3FC),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TariqJaddahTopBarBrand(
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.testTag("top_bar_brand_logo")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFBBF24), Color(0xFF38BDF8))
                    ),
                    shape = CircleShape
                )
                .background(Color(0xFF0A192F))
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_tariq_jaddah_logo),
                contentDescription = "Tariq Jaddah Optical Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(35.dp)
                    .clip(CircleShape)
            )
        }
        Column {
            Text(
                text = tr("Tariq Jaddah Optical", "طارق جدہ آپٹیکل"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = tr(
                    "Orders • Counter Sale • Per-Piece Stock & Khata",
                    "نیو آرڈر • کاؤنٹر سیل • اسٹاک اور کھاتہ سسٹم"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

