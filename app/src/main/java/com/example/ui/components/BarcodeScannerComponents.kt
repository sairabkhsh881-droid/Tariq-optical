package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.OpticalCategory
import com.example.data.OpticalProduct
import com.example.data.PaymentMethod
import com.example.data.RetailerShopProfile
import com.example.data.TARIQ_MEHMOOD_RECEIVER_NUMBERS
import com.example.data.extractCylFromText
import com.example.data.extractSphFromText
import com.example.data.mergeSphCylIntoNotes
import com.example.ui.LocalIsUrdu
import com.example.ui.theme.CriticalStockRed
import com.example.ui.theme.LowStockAmber
import com.example.ui.theme.LowStockAmberContainer
import com.example.ui.theme.StockHealthyGreen
import com.example.ui.tr
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import java.util.EnumMap
import java.util.concurrent.Executors

private enum class ScannedActionTab(val label: String, val urduLabel: String) {
    QUICK_SALE("Sale & Deduct Stock", "فوری فروخت اور اسٹاک کٹوتی"),
    ADJUST_STOCK("Adjust Stock (Pieces)", "اسٹاک کم یا زیادہ کریں")
}

/**
 * Compact 1D Barcode visual badge for product cards and scanner labels.
 */
@Composable
fun BarcodeLabelBadge(
    barcode: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cleanCode = barcode.ifBlank { "8964001000000" }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "Barcode",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = cleanCode,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Draws realistic 1D barcode bars deterministically from any barcode string.
 */
@Composable
fun BarcodeBarsGraphic(
    barcode: String,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF0F172A)
) {
    val code = barcode.ifBlank { "8964001009012" }
    Canvas(modifier = modifier) {
        val totalBars = code.length * 4 + 6
        val slotWidth = size.width / totalBars.coerceAtLeast(1)
        var x = slotWidth
        // Start guard bars
        drawRect(
            color = barColor,
            topLeft = Offset(x, 0f),
            size = ComposeSize(slotWidth * 0.7f, size.height)
        )
        x += slotWidth * 1.6f
        drawRect(
            color = barColor,
            topLeft = Offset(x, 0f),
            size = ComposeSize(slotWidth * 0.7f, size.height)
        )
        x += slotWidth * 1.5f

        code.forEachIndexed { index, ch ->
            val v = ch.code
            val w1 = if ((v + index) % 2 == 0) 0.75f else 1.35f
            val w2 = if ((v * 3 + index) % 3 == 0) 1.25f else 0.6f
            if (x + slotWidth * 3f < size.width) {
                drawRect(
                    color = barColor,
                    topLeft = Offset(x, 0f),
                    size = ComposeSize(slotWidth * w1, size.height * 0.92f)
                )
                x += slotWidth * 1.85f
                drawRect(
                    color = barColor,
                    topLeft = Offset(x, 0f),
                    size = ComposeSize(slotWidth * w2, size.height * 0.92f)
                )
                x += slotWidth * 1.95f
            }
        }

        // End guard bar
        if (x + slotWidth < size.width) {
            drawRect(
                color = barColor,
                topLeft = Offset(x, 0f),
                size = ComposeSize(slotWidth * 0.8f, size.height)
            )
        }
    }
}

/**
 * Full-screen/modal Camera Barcode Scanner for Quick Stock Lookup & Inventory Adjustment During Sales.
 */
@Composable
fun BarcodeScannerLookupAndSaleDialog(
    products: List<OpticalProduct>,
    shopProfiles: List<RetailerShopProfile>,
    initialScannedCode: String = "",
    onDismiss: () -> Unit,
    onQuickAdjustStock: (product: OpticalProduct, newStockPieces: Int, newThresholdPieces: Int) -> Unit,
    onOpenAddStockDialog: (OpticalProduct) -> Unit,
    onCompleteQuickSale: (
        shopId: Int,
        retailerShopName: String,
        retailerCity: String,
        retailerContact: String,
        product: OpticalProduct,
        quantityPieces: Int,
        customRxAndLabNotes: String,
        paymentMethod: PaymentMethod,
        senderNumber: String,
        receiverNumber: String,
        paidAmountPkr: Double
    ) -> Unit,
    onOpenFullSaleDialog: (OpticalProduct) -> Unit,
    onCreateNewProductWithBarcode: (String) -> Unit
) {
    val context = LocalContext.current
    val isUrdu = LocalIsUrdu.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var activeCode by remember {
        mutableStateOf(
            initialScannedCode.ifBlank {
                products.firstOrNull()?.barcode ?: ""
            }
        )
    }
    var manualBarcodeInput by remember { mutableStateOf(activeCode) }
    var isCameraActive by remember { mutableStateOf(true) }
    var torchEnabled by remember { mutableStateOf(false) }
    var selectedActionTab by remember { mutableStateOf(ScannedActionTab.QUICK_SALE) }
    var lastScanFeedback by remember { mutableStateOf<String?>(null) }

    // Dynamically resolve the product from `products` so real-time stock changes update immediately
    val matchedProduct: OpticalProduct? = remember(products, activeCode) {
        val clean = activeCode.trim()
        if (clean.isEmpty()) {
            null
        } else {
            products.firstOrNull {
                it.barcode.equals(clean, ignoreCase = true) ||
                    it.sku.equals(clean, ignoreCase = true)
            } ?: products.firstOrNull {
                it.barcode.contains(clean, ignoreCase = true) ||
                    it.sku.contains(clean, ignoreCase = true)
            }
        }
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
                .testTag("barcode_scanner_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Header Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Barcode Scanner",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = tr(
                                    "Camera Barcode Scanner & Quick Sale",
                                    "کیمرہ بارکوڈ اسکینر اور فوری فروخت / اسٹاک"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "Scan frame or lens barcode for instant stock lookup & piece adjustment",
                                    "فریم یا لینز کا بارکوڈ اسکین کر کے فوری اسٹاک دیکھیں اور فروخت کریں"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_barcode_scanner_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Scanner"
                        )
                    }
                }

                // Live CameraX Barcode Viewfinder Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0A192F)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (hasCameraPermission && isCameraActive) StockHealthyGreen
                                            else LowStockAmber
                                        )
                                )
                                Text(
                                    text = if (hasCameraPermission && isCameraActive) {
                                        tr(
                                            "LIVE CAMERA BARCODE SCANNER (EAN-13 / CODE-128 / QR)",
                                            "لائیو کیمرہ بارکوڈ اسکینر فعال ہے"
                                        )
                                    } else {
                                        tr(
                                            "CAMERA PAUSED / PERMISSION REQUIRED",
                                            "کیمرہ بند ہے یا اجازت درکار ہے"
                                        )
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (hasCameraPermission) {
                                    FilledTonalButton(
                                        onClick = { torchEnabled = !torchEnabled },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                            contentDescription = "Toggle Torch",
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (torchEnabled) tr("Torch ON", "فلیش آن") else tr("Torch", "فلیش"),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = { isCameraActive = !isCameraActive },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text(
                                            text = if (isCameraActive) tr("Pause", "روکیں") else tr("Resume", "چلائیں"),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (hasCameraPermission && isCameraActive) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(14.dp))
                            ) {
                                RealCameraXBarcodePreview(
                                    torchEnabled = torchEnabled,
                                    onBarcodeDetected = { detectedCode ->
                                        if (detectedCode.isNotBlank() && detectedCode != activeCode) {
                                            activeCode = detectedCode
                                            manualBarcodeInput = detectedCode
                                            lastScanFeedback = "Scanned Barcode: $detectedCode"
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Laser Reticle Overlay
                                ScannerLaserReticleOverlay(
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Bottom Overlay Hint
                                Box(
                                    contentAlignment = Alignment.BottomCenter,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                ) {
                                    Surface(
                                        color = Color(0xCC0F172A),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = tr(
                                                "Align product barcode inside frame for automatic detection",
                                                "پراڈکٹ کا بارکوڈ فریم کے اندر رکھیں، خودکار اسکین ہو جائے گا"
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFE2E8F0),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        } else if (!hasCameraPermission) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Camera Permission",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = tr(
                                        "Grant Camera Permission to scan physical product barcodes.",
                                        "بارکوڈ اسکین کرنے کے لیے کیمرے کی اجازت دیں۔"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    },
                                    modifier = Modifier.testTag("grant_camera_permission_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Enable Camera",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(tr("Enable Device Camera", "کیمرہ آن کریں"))
                                }
                            }
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                            ) {
                                Text(
                                    text = tr("Camera preview paused. Tap 'Resume' above.", "کیمرہ بند ہے، دوبارہ چلانے کے لیے اوپر کلک کریں۔"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }

                // Quick-Scan Warehouse Barcode Labels Strip (for instant testing & rapid counter tap)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = tr(
                            "Quick-Scan Product Barcode Labels (Tap any label or scan with camera):",
                            "پراڈکٹ بارکوڈ لیبلز (فوری اسکین یا چیک کرنے کے لیے کسی بھی بارکوڈ پر کلک کریں):"
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(products, key = { it.id }) { prod ->
                            val isSelected = matchedProduct?.id == prod.id
                            Surface(
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .width(168.dp)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        activeCode = prod.barcode.ifBlank { prod.sku }
                                        manualBarcodeInput = activeCode
                                        lastScanFeedback = "Scanned ${prod.sku} (${prod.barcode})"
                                    }
                                    .testTag("quick_scan_chip_${prod.sku}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = prod.sku,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${prod.currentStock} Pcs",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (prod.isLowStock) LowStockAmber else StockHealthyGreen
                                        )
                                    }
                                    BarcodeBarsGraphic(
                                        barcode = prod.barcode,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(22.dp)
                                    )
                                    Text(
                                        text = prod.barcode.ifBlank { prod.sku },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Manual Barcode / SKU Lookup Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = manualBarcodeInput,
                        onValueChange = {
                            manualBarcodeInput = it
                            activeCode = it
                        },
                        label = {
                            Text(
                                tr(
                                    "Scanned Barcode / SKU Lookup",
                                    "بارکوڈ نمبر یا پراڈکٹ کوڈ درج کریں"
                                )
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Lookup Barcode"
                            )
                        },
                        trailingIcon = {
                            if (manualBarcodeInput.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        manualBarcodeInput = ""
                                        activeCode = ""
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear barcode"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("barcode_manual_input")
                    )

                    Button(
                        onClick = {
                            activeCode = manualBarcodeInput.trim()
                            lastScanFeedback = "Looked up code: $activeCode"
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("barcode_lookup_btn")
                    ) {
                        Text(tr("Lookup", "تلاش کریں"))
                    }
                }

                AnimatedVisibility(visible = lastScanFeedback != null) {
                    if (lastScanFeedback != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Scan Status",
                                        tint = StockHealthyGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = lastScanFeedback!!,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                TextButton(onClick = { lastScanFeedback = null }) {
                                    Text(tr("OK", "ٹھیک ہے"))
                                }
                            }
                        }
                    }
                }

                // Scanned Product Result Section
                if (matchedProduct != null) {
                    ScannedProductStockAndSaleCard(
                        product = matchedProduct,
                        shopProfiles = shopProfiles,
                        selectedActionTab = selectedActionTab,
                        onSelectActionTab = { selectedActionTab = it },
                        onQuickAdjustStock = { newStock, newThreshold ->
                            onQuickAdjustStock(matchedProduct, newStock, newThreshold)
                            lastScanFeedback =
                                "Adjusted ${matchedProduct.sku} stock to $newStock Pieces (Threshold ≤ $newThreshold Pcs)"
                        },
                        onOpenAddStockDialog = {
                            onOpenAddStockDialog(matchedProduct)
                        },
                        onCompleteQuickSale = { shopId, shopName, city, contact, qty, notes, method, sender, receiver, paid ->
                            onCompleteQuickSale(
                                shopId,
                                shopName,
                                city,
                                contact,
                                matchedProduct,
                                qty,
                                notes,
                                method,
                                sender,
                                receiver,
                                paid
                            )
                            lastScanFeedback =
                                "Sale recorded: $qty Pieces of ${matchedProduct.sku} sold to $shopName (${method.label})"
                        },
                        onOpenFullSaleDialog = {
                            onOpenFullSaleDialog(matchedProduct)
                        }
                    )
                } else if (activeCode.isNotBlank()) {
                    // Unregistered Barcode Scanned Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = LowStockAmberContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("unregistered_barcode_card")
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = tr(
                                    "Barcode '$activeCode' Not Found in Warehouse",
                                    "بارکوڈ '$activeCode' گودام کے ریکارڈ میں موجود نہیں ہے"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F)
                            )
                            Text(
                                text = tr(
                                    "You can register this barcode as a new optical product in Tariq Jaddah Optical catalog.",
                                    "آپ اس بارکوڈ کے ساتھ نئی پراڈکٹ کیٹلاگ میں شامل کر سکتے ہیں۔"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF92400E)
                            )
                            Button(
                                onClick = { onCreateNewProductWithBarcode(activeCode.trim()) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_scanned_barcode_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New Product with Barcode",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    tr(
                                        "+ Add New Product with Barcode $activeCode",
                                        "+ بارکوڈ $activeCode کے ساتھ نئی پراڈکٹ بنائیں"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannedProductStockAndSaleCard(
    product: OpticalProduct,
    shopProfiles: List<RetailerShopProfile>,
    selectedActionTab: ScannedActionTab,
    onSelectActionTab: (ScannedActionTab) -> Unit,
    onQuickAdjustStock: (newStockPieces: Int, newThresholdPieces: Int) -> Unit,
    onOpenAddStockDialog: () -> Unit,
    onCompleteQuickSale: (
        shopId: Int,
        retailerShopName: String,
        retailerCity: String,
        retailerContact: String,
        quantityPieces: Int,
        customRxAndLabNotes: String,
        paymentMethod: PaymentMethod,
        senderNumber: String,
        receiverNumber: String,
        paidAmountPkr: Double
    ) -> Unit,
    onOpenFullSaleDialog: () -> Unit
) {
    val isUrdu = LocalIsUrdu.current

    // Quick Sale Form State (keyed by product.id so scanning a new item resets defaults)
    var saleQtyInput by remember(product.id) { mutableStateOf("5") }
    var sphPowerInput by remember(product.id) {
        mutableStateOf(
            extractSphFromText(product.powerOrSizeRange).ifEmpty {
                if (product.category != OpticalCategory.EYEGLASS_FRAMES) "-1.50" else ""
            }
        )
    }
    var cylPowerInput by remember(product.id) {
        mutableStateOf(
            extractCylFromText(product.powerOrSizeRange).ifEmpty {
                if (product.category != OpticalCategory.EYEGLASS_FRAMES) "-0.50" else ""
            }
        )
    }
    val initialShop = shopProfiles.firstOrNull()
    var selectedShopId by remember(product.id) { mutableIntStateOf(initialShop?.id ?: 0) }
    var shopName by remember(product.id) { mutableStateOf(initialShop?.shopName ?: "Apex Vision Boutique") }
    var shopCity by remember(product.id) { mutableStateOf(initialShop?.cityAndMarket ?: "Lahore • Mall Road") }
    var shopContact by remember(product.id) { mutableStateOf(initialShop?.phoneNumber ?: "0300-4128901") }
    var selectedPaymentMethod by remember(product.id) { mutableStateOf(PaymentMethod.CASH) }
    var senderNumber by remember(product.id) { mutableStateOf(initialShop?.phoneNumber ?: "0300-4128901") }
    var receiverNumber by remember(product.id) { mutableStateOf(PaymentMethod.CASH.defaultReceiverNo) }

    val saleQty = (saleQtyInput.toIntOrNull() ?: 1).coerceAtLeast(1)
    val totalSalePkr = saleQty * product.wholesalePrice
    val remainingStockAfterSale = (product.currentStock - saleQty).coerceAtLeast(0)

    var paidAmountInput by remember(product.id, saleQtyInput, selectedPaymentMethod) {
        mutableStateOf(
            if (selectedPaymentMethod == PaymentMethod.DEBT) "0"
            else totalSalePkr.toLong().toString()
        )
    }

    // Quick Inventory Adjustment State
    var exactStockInput by remember(product.id, product.currentStock) {
        mutableStateOf(product.currentStock.toString())
    }
    var exactThresholdInput by remember(product.id, product.lowStockThreshold) {
        mutableStateOf(product.lowStockThreshold.toString())
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = if (product.isLowStock) LowStockAmber else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("scanned_product_result_card")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            // Product Barcode & SKU Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryBadge(category = product.category)
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = product.sku,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                BarcodeLabelBadge(barcode = product.barcode)
            }

            // Product Name & Price per Piece
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${product.brand} • ${product.primarySpec}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(product.wholesalePrice),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = tr("per Piece (PKR)", "فی پیس قیمت"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Live Stock Health Badge (in Pieces)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                StockHealthBadge(
                    currentStock = product.currentStock,
                    threshold = product.lowStockThreshold,
                    unitName = "Pieces"
                )
                Text(
                    text = tr(
                        "Threshold ≤ ${product.lowStockThreshold} Pcs",
                        "کم اسٹاک حد ≤ ${product.lowStockThreshold} پیس"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            // Mode Switcher: Quick Sale & Deduct vs Quick Stock Adjustment
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ScannedActionTab.entries.forEach { tab ->
                    val selected = selectedActionTab == tab
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectActionTab(tab) },
                        label = {
                            Text(
                                text = if (isUrdu) tab.urduLabel else tab.label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (tab == ScannedActionTab.QUICK_SALE) {
                                    Icons.Default.AddShoppingCart
                                } else {
                                    Icons.Default.Tune
                                },
                                contentDescription = tab.label,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("scanner_mode_${tab.name.lowercase()}")
                    )
                }
            }

            when (selectedActionTab) {
                ScannedActionTab.QUICK_SALE -> {
                    // Quick Sale & Immediate Inventory Deduction During Sales
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = tr(
                                "1. Quantity to Sell (Pieces) — Auto-Deducts Warehouse Stock:",
                                "1. فروخت کرنے کے لیے پیس کی تعداد (اسٹاک سے خودکار کم ہو جائے گا):"
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    val next = (saleQty - 1).coerceAtLeast(1)
                                    saleQtyInput = next.toString()
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease Pieces")
                            }

                            OutlinedTextField(
                                value = saleQtyInput,
                                onValueChange = { saleQtyInput = it },
                                label = { Text(tr("Pieces (پیس)", "پیس کی تعداد")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("scanner_sale_qty_input")
                            )

                            FilledTonalIconButton(
                                onClick = {
                                    val next = saleQty + 1
                                    saleQtyInput = next.toString()
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase Pieces")
                            }
                        }

                        // Quick quantity chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(1, 5, 10, 20).forEach { preset ->
                                OutlinedButton(
                                    onClick = { saleQtyInput = preset.toString() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("$preset Pcs", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        // Stock Deduction & Total Bill Preview Banner
                        Surface(
                            color = if (remainingStockAfterSale <= product.lowStockThreshold) {
                                LowStockAmberContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = tr("Total Sale Bill:", "کل بل:"),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        text = "$saleQty Pcs × ${formatCurrency(product.wholesalePrice)} = ${formatCurrency(totalSalePkr)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = tr(
                                        "Stock Adjustment: ${product.currentStock} Pieces → $remainingStockAfterSale Pieces remaining",
                                        "اسٹاک ایڈجسٹمنٹ: ${product.currentStock} پیس ← فروخت کے بعد $remainingStockAfterSale پیس باقی"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (remainingStockAfterSale <= product.lowStockThreshold) {
                                        Color(0xFF78350F)
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    }
                                )
                            }
                        }

                        // Select Retailer Shop
                        OpticalSphCylSelectorSection(
                            sphValue = sphPowerInput,
                            cylValue = cylPowerInput,
                            onSphChange = { sphPowerInput = it },
                            onCylChange = { cylPowerInput = it },
                            contextLabelEn = "Scanned Sale Lens SPH / CYL Power Options",
                            contextLabelUr = "اسکین شدہ فروخت کے لیے SPH / CYL نمبر"
                        )

                        Text(
                            text = tr("2. Select Retailer Shop:", "2. ریٹیلر شاپ منتخب کریں:"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(shopProfiles, key = { it.id }) { profile ->
                                FilterChip(
                                    selected = selectedShopId == profile.id,
                                    onClick = {
                                        selectedShopId = profile.id
                                        shopName = profile.shopName
                                        shopCity = profile.cityAndMarket
                                        shopContact = profile.phoneNumber
                                        senderNumber = profile.phoneNumber
                                    },
                                    label = { Text(profile.shopName) }
                                )
                            }
                        }

                        // Select Payment Method (Cash, Debt, JazzCash, EasyPaisa)
                        Text(
                            text = tr("3. Payment Method & Numbers:", "3. ادائیگی کا طریقہ:"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(PaymentMethod.entries) { method ->
                                FilterChip(
                                    selected = selectedPaymentMethod == method,
                                    onClick = {
                                        selectedPaymentMethod = method
                                        receiverNumber = method.defaultReceiverNo
                                        paidAmountInput = if (method == PaymentMethod.DEBT) {
                                            "0"
                                        } else {
                                            totalSalePkr.toLong().toString()
                                        }
                                    },
                                    label = {
                                        Text(if (isUrdu) method.urduLabel else method.label)
                                    }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = senderNumber,
                                onValueChange = { senderNumber = it },
                                label = { Text(tr("Sender No", "بھیجنے والا نمبر")) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = receiverNumber,
                                onValueChange = { receiverNumber = it },
                                label = { Text(tr("Receiver No", "وصول کرنے والا نمبر")) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
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

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val paidVal = paidAmountInput.toDoubleOrNull() ?: totalSalePkr
                                    val saleNotes = mergeSphCylIntoNotes(
                                        "Scanned via Camera Barcode (${product.barcode})",
                                        sphPowerInput,
                                        cylPowerInput
                                    )
                                    onCompleteQuickSale(
                                        selectedShopId,
                                        shopName,
                                        shopCity,
                                        shopContact,
                                        saleQty,
                                        saleNotes,
                                        selectedPaymentMethod,
                                        senderNumber,
                                        receiverNumber,
                                        paidVal
                                    )
                                },
                                enabled = product.currentStock > 0,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("scanner_complete_quick_sale_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddShoppingCart,
                                    contentDescription = "Complete Sale",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    tr(
                                        "Sell $saleQty Pieces & Deduct Stock",
                                        "$saleQty پیس فروخت کریں اور اسٹاک کم کریں"
                                    )
                                )
                            }

                            OutlinedButton(
                                onClick = onOpenFullSaleDialog,
                                modifier = Modifier.testTag("scanner_open_full_order_btn")
                            ) {
                                Text(tr("Full Rx Form", "تفصیلی آرڈر"))
                            }
                        }
                    }
                }

                ScannedActionTab.ADJUST_STOCK -> {
                    // Quick Inventory Adjustment Mode (in Pieces)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OpticalSphCylSelectorSection(
                            sphValue = sphPowerInput,
                            cylValue = cylPowerInput,
                            onSphChange = { sphPowerInput = it },
                            onCylChange = { cylPowerInput = it },
                            contextLabelEn = "Scanned Restock Lens SPH / CYL Options",
                            contextLabelUr = "اسکین شدہ ری اسٹاک کے لیے SPH / CYL نمبر"
                        )

                        Text(
                            text = tr(
                                "Instant One-Tap Stock Adjustment (Pieces):",
                                "ایک کلک سے اسٹاک کم یا زیادہ کریں (پیس):"
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(-10, -5, -1).forEach { delta ->
                                OutlinedButton(
                                    onClick = {
                                        val updated = (product.currentStock + delta).coerceAtLeast(0)
                                        onQuickAdjustStock(updated, product.lowStockThreshold)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = CriticalStockRed
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("scanner_adj_minus_${-delta}")
                                ) {
                                    Text("$delta Pcs", fontWeight = FontWeight.Bold)
                                }
                            }
                            listOf(1, 5, 10, 25).forEach { delta ->
                                FilledTonalButton(
                                    onClick = {
                                        val updated = product.currentStock + delta
                                        onQuickAdjustStock(updated, product.lowStockThreshold)
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("scanner_adj_plus_$delta")
                                ) {
                                    Text("+$delta Pcs", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        HorizontalDivider()

                        Text(
                            text = tr(
                                "Set Exact Warehouse Piece Count & Low-Stock Threshold:",
                                "موجودہ اسٹاک (پیس) اور کم اسٹاک الرٹ کی حد مقرر کریں:"
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = exactStockInput,
                                onValueChange = { exactStockInput = it },
                                label = { Text(tr("Exact Stock (Pieces)", "موجودہ اسٹاک (پیس)")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("scanner_exact_stock_input")
                            )
                            OutlinedTextField(
                                value = exactThresholdInput,
                                onValueChange = { exactThresholdInput = it },
                                label = { Text(tr("Alert Threshold (Pcs)", "کم اسٹاک حد (پیس)")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("scanner_exact_threshold_input")
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val newStock = (exactStockInput.toIntOrNull() ?: product.currentStock).coerceAtLeast(0)
                                    val newThresh = (exactThresholdInput.toIntOrNull() ?: product.lowStockThreshold).coerceAtLeast(1)
                                    onQuickAdjustStock(newStock, newThresh)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("scanner_save_exact_stock_btn")
                            ) {
                                Text(tr("Save Stock Count", "اسٹاک محفوظ کریں"))
                            }

                            FilledTonalButton(
                                onClick = onOpenAddStockDialog,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = "Add Stock Batch",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(tr("+ New Shipment", "+ نیا اسٹاک"))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact Camera Barcode Picker Dialog used inside CreateRetailOrderDialog or ProductSkuEditorDialog
 * to quickly scan a product barcode with the camera.
 */
@Composable
fun CompactBarcodePickerDialog(
    products: List<OpticalProduct>,
    title: String,
    onDismiss: () -> Unit,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var manualCode by remember { mutableStateOf("") }
    var torchEnabled by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("compact_barcode_picker_dialog")
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(16.dp)
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
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Barcode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                    ) {
                        RealCameraXBarcodePreview(
                            torchEnabled = torchEnabled,
                            onBarcodeDetected = { code ->
                                if (code.isNotBlank()) {
                                    onBarcodeScanned(code)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        ScannerLaserReticleOverlay(modifier = Modifier.fillMaxSize())
                        IconButton(
                            onClick = { torchEnabled = !torchEnabled },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Flash",
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Enable Camera")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Grant Camera Permission", "کیمرے کی اجازت دیں"))
                    }
                }

                Text(
                    text = tr(
                        "Or tap a product barcode label below:",
                        "یا نیچے دیے گئے کسی پراڈکٹ بارکوڈ پر کلک کریں:"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(products, key = { it.id }) { prod ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                onBarcodeScanned(prod.barcode.ifBlank { prod.sku })
                            },
                            label = {
                                Text("${prod.sku} • ${prod.barcode} (${prod.currentStock} Pcs)")
                            },
                            modifier = Modifier.testTag("compact_scan_item_${prod.sku}")
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = manualCode,
                        onValueChange = { manualCode = it },
                        label = { Text(tr("Enter Barcode / SKU", "بارکوڈ درج کریں")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (manualCode.isNotBlank()) {
                                onBarcodeScanned(manualCode.trim())
                            }
                        }
                    ) {
                        Text(tr("Use", "منتخب کریں"))
                    }
                }
            }
        }
    }
}

/**
 * Animated laser reticle overlay for the CameraX barcode scanner.
 */
@Composable
private fun ScannerLaserReticleOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Canvas(modifier = modifier) {
        val frameWidth = size.width * 0.78f
        val frameHeight = size.height * 0.62f
        val left = (size.width - frameWidth) / 2f
        val top = (size.height - frameHeight) / 2f

        // Reticle border
        drawRoundRect(
            color = Color(0xFF38BDF8).copy(alpha = 0.85f),
            topLeft = Offset(left, top),
            size = ComposeSize(frameWidth, frameHeight),
            cornerRadius = CornerRadius(18f, 18f),
            style = Stroke(width = 3.5f)
        )

        // Animated laser line inside reticle
        val laserY = top + frameHeight * scanLineProgress
        drawLine(
            color = Color(0xFFEF4444),
            start = Offset(left + 12f, laserY),
            end = Offset(left + frameWidth - 12f, laserY),
            strokeWidth = 4f
        )
    }
}

/**
 * Real CameraX Preview + ZXing MultiFormatReader ImageAnalysis composable.
 */
@Composable
private fun RealCameraXBarcodePreview(
    torchEnabled: Boolean,
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(boundCamera, torchEnabled) {
        try {
            boundCamera?.cameraControl?.enableTorch(torchEnabled)
        } catch (_: Exception) {
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                analysisExecutor.shutdown()
            } catch (_: Exception) {
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    @Suppress("DEPRECATION")
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(
                                analysisExecutor,
                                ZxingCameraBarcodeAnalyzer { scannedValue ->
                                    previewView.post {
                                        onBarcodeDetected(scannedValue)
                                    }
                                }
                            )
                        }

                    val selector = when {
                        cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ->
                            CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ->
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        else -> CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    cameraProvider.unbindAll()
                    boundCamera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        imageAnalysis
                    )
                } catch (_: Exception) {
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}

/**
 * ZXing ImageAnalysis.Analyzer that decodes 1D and 2D barcodes from CameraX YUV_420_888 frames.
 * Supports both horizontal and 90-degree rotated orientations so 1D barcodes scan in portrait or landscape.
 */
private class ZxingCameraBarcodeAnalyzer(
    private val onBarcodeFound: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
            put(
                DecodeHintType.POSSIBLE_FORMATS,
                listOf(
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.EAN_8,
                    BarcodeFormat.UPC_A,
                    BarcodeFormat.UPC_E,
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.CODE_39,
                    BarcodeFormat.ITF,
                    BarcodeFormat.CODABAR,
                    BarcodeFormat.QR_CODE
                )
            )
            put(DecodeHintType.TRY_HARDER, true)
        }
        setHints(hints)
    }

    private var lastDetectedCode: String = ""
    private var lastDetectedTimestampMs: Long = 0L

    override fun analyze(image: ImageProxy) {
        try {
            val plane = image.planes.firstOrNull() ?: return
            val buffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride
            val width = image.width
            val height = image.height

            val yBytes = ByteArray(width * height)
            if (pixelStride == 1 && rowStride == width) {
                buffer.rewind()
                buffer.get(yBytes, 0, width * height)
            } else {
                val rowData = ByteArray(rowStride)
                buffer.rewind()
                for (row in 0 until height) {
                    val bytesToRead = if (row == height - 1) {
                        buffer.remaining().coerceAtMost(rowStride)
                    } else {
                        rowStride.coerceAtMost(buffer.remaining())
                    }
                    if (bytesToRead <= 0) break
                    buffer.get(rowData, 0, bytesToRead)
                    var colOffset = 0
                    val dstRowOffset = row * width
                    for (col in 0 until width) {
                        if (colOffset < bytesToRead) {
                            yBytes[dstRowOffset + col] = rowData[colOffset]
                        }
                        colOffset += pixelStride
                    }
                }
            }

            val decoded = tryDecodeLuminance(yBytes, width, height)
                ?: run {
                    // Rotate 90 degrees for portrait 1D barcode orientation
                    val rotated = ByteArray(width * height)
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            rotated[x * height + (height - y - 1)] = yBytes[y * width + x]
                        }
                    }
                    tryDecodeLuminance(rotated, height, width)
                }

            if (!decoded.isNullOrBlank()) {
                val now = System.currentTimeMillis()
                if (decoded != lastDetectedCode || (now - lastDetectedTimestampMs) > 1800L) {
                    lastDetectedCode = decoded
                    lastDetectedTimestampMs = now
                    onBarcodeFound(decoded)
                }
            }
        } catch (_: Exception) {
        } finally {
            image.close()
        }
    }

    private fun tryDecodeLuminance(data: ByteArray, width: Int, height: Int): String? {
        val source = PlanarYUVLuminanceSource(
            data,
            width,
            height,
            0,
            0,
            width,
            height,
            false
        )
        try {
            val result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
            if (!result.text.isNullOrBlank()) return result.text
        } catch (_: Exception) {
        } finally {
            reader.reset()
        }

        try {
            val result = reader.decodeWithState(BinaryBitmap(GlobalHistogramBinarizer(source)))
            if (!result.text.isNullOrBlank()) return result.text
        } catch (_: Exception) {
        } finally {
            reader.reset()
        }
        return null
    }
}
