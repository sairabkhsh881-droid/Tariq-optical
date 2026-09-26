package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.OpticalCategory
import com.example.data.OpticalProduct
import com.example.data.RetailerOrder
import com.example.ui.LocalIsUrdu
import com.example.ui.tr
import com.example.util.OpticalPrintAndExportHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dialog for Generating Barcodes by Category, Previewing Printable Sticker Labels,
 * Printing via Android PrintManager, and Saving as PDF or JPG.
 */
@Composable
fun ProductBarcodeGeneratorDialog(
    products: List<OpticalProduct>,
    initialProduct: OpticalProduct?,
    onSaveUpdatedProductBarcode: (OpticalProduct) -> Unit,
    onNotifyStatus: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (products.isEmpty()) {
        onDismiss()
        return
    }

    val context = LocalContext.current
    val isUrdu = LocalIsUrdu.current

    var selectedCategory by remember {
        mutableStateOf(initialProduct?.category ?: products.first().category)
    }
    val categoryProducts = remember(products, selectedCategory) {
        products.filter { it.category == selectedCategory }.ifEmpty { products }
    }
    var selectedProduct by remember(initialProduct) {
        mutableStateOf(initialProduct ?: categoryProducts.first())
    }
    var editableBarcode by remember(selectedProduct.id, selectedProduct.barcode) {
        mutableStateOf(
            selectedProduct.barcode.ifBlank {
                OpticalPrintAndExportHelper.generateBarcodeByCategory(
                    selectedProduct.category,
                    products
                )
            }
        )
    }
    var copiesPerProduct by remember { mutableIntStateOf(4) }
    var printWholeCategorySheet by remember { mutableStateOf(false) }

    val previewProduct = remember(selectedProduct, editableBarcode) {
        selectedProduct.copy(barcode = editableBarcode.trim().ifEmpty { selectedProduct.sku })
    }

    val labelPreviewBitmap = remember(previewProduct) {
        OpticalPrintAndExportHelper.createSingleProductBarcodeLabelBitmap(previewProduct)
    }

    val targetProductsForSheet = remember(
        printWholeCategorySheet,
        categoryProducts,
        previewProduct
    ) {
        if (printWholeCategorySheet) categoryProducts else listOf(previewProduct)
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    OpticalPrintAndExportHelper.writeBarcodeLabelsPdf(
                        productsToPrint = targetProductsForSheet,
                        copiesPerProduct = if (printWholeCategorySheet) 1 else copiesPerProduct,
                        outputStream = out
                    )
                }
                onNotifyStatus(
                    if (isUrdu) {
                        "پرنٹ ایبل بارکوڈ لیبلز PDF میں محفوظ ہو گئے۔"
                    } else {
                        "Saved printable Barcode Labels PDF (${targetProductsForSheet.size} SKUs)."
                    }
                )
            }.onFailure {
                onNotifyStatus("Failed to save Barcode PDF: ${it.localizedMessage}")
            }
        }
    }

    val saveJpgLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val bmp = OpticalPrintAndExportHelper.createBarcodeSheetBitmap(
                    productsToPrint = targetProductsForSheet,
                    copiesPerProduct = if (printWholeCategorySheet) 1 else copiesPerProduct
                )
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    OpticalPrintAndExportHelper.writeBitmapAsJpg(bmp, out)
                }
                onNotifyStatus(
                    if (isUrdu) {
                        "بارکوڈ لیبل شیٹ JPG تصویر میں محفوظ ہو گئی۔"
                    } else {
                        "Saved printable Barcode Label sheet as JPG image."
                    }
                )
            }.onFailure {
                onNotifyStatus("Failed to save Barcode JPG: ${it.localizedMessage}")
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
                .testTag("barcode_generator_dialog")
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Header
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
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Barcode Generator",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = tr(
                                    "Category Barcode Generator & Label Printer",
                                    "کیٹیگری بارکوڈ جنریٹر اور پرنٹ ایبل لیبلز"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "Numbered by Optical Category • Print or Save as PDF / JPG",
                                    "کیٹیگری کے لحاظ سے بارکوڈ نمبر • پرنٹ کریں یا PDF / JPG میں محفوظ کریں"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_barcode_generator_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Step 1: Select Category (Shows Category Barcode Prefix Numbering)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = tr(
                                "1. Select Optical Category (Category Barcode Numbering):",
                                "1. آپٹیکل کیٹیگری منتخب کریں (کیٹیگری کوڈ کے لحاظ سے بارکوڈ نمبر):"
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(OpticalCategory.entries) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = {
                                        selectedCategory = cat
                                        val firstInCat = products.firstOrNull { it.category == cat }
                                        if (firstInCat != null) {
                                            selectedProduct = firstInCat
                                            editableBarcode = firstInCat.barcode
                                        }
                                    },
                                    label = {
                                        val catTitle = if (isUrdu) cat.urduName else cat.displayName
                                        Text("${cat.categoryNumberCode} (${cat.barcodePrefix}•••) • $catTitle")
                                    },
                                    modifier = Modifier.testTag("barcode_gen_cat_${cat.name.lowercase()}")
                                )
                            }
                        }
                        Text(
                            text = tr(
                                "Active Prefix: ${selectedCategory.categoryNumberCode} → Starts with ${selectedCategory.barcodePrefix} + 4-digit item number + EAN-13 check digit",
                                "منتخب کیٹیگری کوڈ: ${selectedCategory.categoryNumberCode} ← بارکوڈ پری فکس: ${selectedCategory.barcodePrefix}"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Step 2: Select Product in Category
                Text(
                    text = tr(
                        "2. Select Product in ${selectedCategory.displayName}:",
                        "2. اس کیٹیگری میں سے پراڈکٹ منتخب کریں:"
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categoryProducts, key = { it.id }) { prod ->
                        FilterChip(
                            selected = selectedProduct.id == prod.id,
                            onClick = {
                                selectedProduct = prod
                                editableBarcode = prod.barcode
                            },
                            label = {
                                Text("${prod.sku} • ${prod.barcode}")
                            },
                            modifier = Modifier.testTag("barcode_gen_prod_${prod.sku}")
                        )
                    }
                }

                // Step 3: Generate / Customize Category Barcode Number & Save to Product
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = tr(
                                "3. Generate Barcode Number by Category (${selectedProduct.category.categoryNumberCode}):",
                                "3. کیٹیگری کے مطابق نیا بارکوڈ نمبر جنریٹ کریں:"
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = editableBarcode,
                                onValueChange = { editableBarcode = it },
                                label = {
                                    Text(
                                        tr(
                                            "Category Barcode (${selectedProduct.category.categoryNumberCode})",
                                            "بارکوڈ نمبر (${selectedProduct.category.categoryNumberCode})"
                                        )
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("barcode_gen_number_input")
                            )

                            FilledTonalButton(
                                onClick = {
                                    val generated = OpticalPrintAndExportHelper.generateBarcodeByCategory(
                                        category = selectedProduct.category,
                                        existingProducts = products,
                                        customSequence = (1000..9999).random()
                                    )
                                    editableBarcode = generated
                                },
                                modifier = Modifier.testTag("generate_category_barcode_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = "Generate by Category",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(tr("Generate", "نیا نمبر بنائیں"))
                            }
                        }

                        Button(
                            onClick = {
                                val updated = selectedProduct.copy(barcode = editableBarcode.trim())
                                selectedProduct = updated
                                onSaveUpdatedProductBarcode(updated)
                                onNotifyStatus(
                                    if (isUrdu) {
                                        "${updated.sku} کے لیے نیا بارکوڈ ${updated.barcode} محفوظ کر دیا گیا۔"
                                    } else {
                                        "Assigned category barcode ${updated.barcode} (${updated.category.categoryNumberCode}) to ${updated.sku}."
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_generated_barcode_to_product_btn")
                        ) {
                            Text(
                                tr(
                                    "Save Barcode ${editableBarcode.trim()} to ${selectedProduct.sku}",
                                    "یہ بارکوڈ ${selectedProduct.sku} کے ریکارڈ میں محفوظ کریں"
                                )
                            )
                        }
                    }
                }

                // Live Printable Sticker Label Preview
                Text(
                    text = tr(
                        "4. Printable Barcode Sticker Preview (Sold Per Piece in PKR):",
                        "4. پرنٹ ایبل بارکوڈ اسٹیکر کا نمونہ (فروخت فی پیس):"
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                ) {
                    Image(
                        bitmap = labelPreviewBitmap.asImageBitmap(),
                        contentDescription = "Printable Barcode Label Preview",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                // Sheet Layout Options: Copies per SKU or Print Entire Category
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = tr(
                                "Select Printable Label Sheet Layout:",
                                "پرنٹ شیٹ کے اسٹیکرز کی تعداد منتخب کریں:"
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf(1, 4, 8, 12)) { count ->
                                FilterChip(
                                    selected = !printWholeCategorySheet && copiesPerProduct == count,
                                    onClick = {
                                        printWholeCategorySheet = false
                                        copiesPerProduct = count
                                    },
                                    label = {
                                        Text(tr("$count Stickers", "$count اسٹیکرز"))
                                    }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = printWholeCategorySheet,
                                    onClick = { printWholeCategorySheet = true },
                                    label = {
                                        Text(
                                            tr(
                                                "All ${selectedCategory.categoryNumberCode} (${categoryProducts.size} SKUs)",
                                                "مکمل کیٹیگری (${categoryProducts.size} پراڈکٹس)"
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Print, Save as PDF, Save as JPG Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            OpticalPrintAndExportHelper.printBarcodeLabels(
                                context = context,
                                jobName = "TariqJaddah_Barcodes_${selectedProduct.sku}",
                                productsToPrint = targetProductsForSheet,
                                copiesPerProduct = if (printWholeCategorySheet) 1 else copiesPerProduct
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("print_barcode_labels_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print Barcode Labels",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Print Labels", "پرنٹ کریں"))
                    }

                    FilledTonalButton(
                        onClick = {
                            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                            savePdfLauncher.launch("TariqJaddah_Barcodes_${selectedProduct.sku}_$stamp.pdf")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_barcode_pdf_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Save Barcode PDF",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Save PDF", "PDF محفوظ کریں"))
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                            saveJpgLauncher.launch("TariqJaddah_Barcodes_${selectedProduct.sku}_$stamp.jpg")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_barcode_jpg_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Save Barcode JPG",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Save as JPG", "JPG فائل محفوظ کریں"))
                    }

                    OutlinedButton(
                        onClick = {
                            val bmp = OpticalPrintAndExportHelper.createBarcodeSheetBitmap(
                                productsToPrint = targetProductsForSheet,
                                copiesPerProduct = if (printWholeCategorySheet) 1 else copiesPerProduct
                            )
                            val savedName = OpticalPrintAndExportHelper.saveBitmapToGalleryAsJpg(
                                context = context,
                                bitmap = bmp,
                                fileNameWithoutExt = "TJO_Barcode_${selectedProduct.sku}_${System.currentTimeMillis()}"
                            )
                            if (savedName != null) {
                                onNotifyStatus(
                                    if (isUrdu) {
                                        "بارکوڈ لیبل گیلری (Pictures/TariqJaddahOptical) میں JPG محفوظ ہو گیا۔"
                                    } else {
                                        "Saved Barcode Label JPG to Gallery (Pictures/TariqJaddahOptical/$savedName)."
                                    }
                                )
                            } else {
                                onNotifyStatus("Could not save JPG to Gallery.")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gallery_barcode_jpg_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Save JPG to Gallery",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Save JPG to Gallery", "گیلری میں JPG"))
                    }
                }
            }
        }
    }
}

/**
 * Receipt 🧾 Preview, Print, Save as PDF & Save as JPG Dialog for any RetailerOrder.
 */
@Composable
fun OrderReceiptPrintAndSaveDialog(
    order: RetailerOrder,
    onNotifyStatus: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isUrdu = LocalIsUrdu.current
    val receiptBitmap = remember(order) {
        OpticalPrintAndExportHelper.createOrderReceiptBitmap(order)
    }

    val saveReceiptPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    OpticalPrintAndExportHelper.writeOrderReceiptPdf(order, out)
                }
                onNotifyStatus(
                    if (isUrdu) {
                        "رسید 🧾 ${order.orderNumber} کامیابی سے PDF میں محفوظ ہو گئی۔"
                    } else {
                        "Receipt 🧾 ${order.orderNumber} saved as PDF successfully."
                    }
                )
            }.onFailure {
                onNotifyStatus("Failed to save Receipt PDF: ${it.localizedMessage}")
            }
        }
    }

    val saveReceiptJpgLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    OpticalPrintAndExportHelper.writeBitmapAsJpg(receiptBitmap, out)
                }
                onNotifyStatus(
                    if (isUrdu) {
                        "رسید 🧾 ${order.orderNumber} کامیابی سے JPG تصویر میں محفوظ ہو گئی۔"
                    } else {
                        "Receipt 🧾 ${order.orderNumber} saved as JPG image successfully."
                    }
                )
            }.onFailure {
                onNotifyStatus("Failed to save Receipt JPG: ${it.localizedMessage}")
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
                .testTag("order_receipt_dialog")
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Header
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
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "Order Receipt",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = tr(
                                    "Receipt 🧾 ${order.orderNumber} • Print / Save PDF & JPG",
                                    "آفیشل رسید 🧾 ${order.orderNumber} • پرنٹ / PDF اور JPG"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "Tariq Jaddah Optical • ${order.retailerShopName} (${order.quantity} Pieces)",
                                    "طارق جدہ آپٹیکل • ${order.retailerShopName} (${order.quantity} پیس)"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "03176858707 Tariq Mehmood • 03087321947 Tariq Mehmood",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_receipt_dialog_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Receipt")
                    }
                }

                // Action Buttons Row 1: Print Receipt & Save as PDF
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            OpticalPrintAndExportHelper.printOrderReceipt(context, order)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("print_order_receipt_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print Receipt",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Print Receipt 🧾", "رسید پرنٹ کریں 🧾"))
                    }

                    FilledTonalButton(
                        onClick = {
                            saveReceiptPdfLauncher.launch("TariqJaddah_Receipt_${order.orderNumber}.pdf")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_receipt_pdf_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Save Receipt as PDF",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Save as PDF", "PDF محفوظ کریں"))
                    }
                }

                // Action Buttons Row 2: Save as JPG File & Quick Save JPG to Gallery
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            saveReceiptJpgLauncher.launch("TariqJaddah_Receipt_${order.orderNumber}.jpg")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_receipt_jpg_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Save Receipt as JPG",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Save as JPG", "JPG فائل محفوظ کریں"))
                    }

                    OutlinedButton(
                        onClick = {
                            val saved = OpticalPrintAndExportHelper.saveBitmapToGalleryAsJpg(
                                context = context,
                                bitmap = receiptBitmap,
                                fileNameWithoutExt = "TariqJaddah_Receipt_${order.orderNumber}"
                            )
                            if (saved != null) {
                                onNotifyStatus(
                                    if (isUrdu) {
                                        "رسید 🧾 گیلری (Pictures/TariqJaddahOptical) میں JPG محفوظ ہو گئی۔"
                                    } else {
                                        "Saved Receipt 🧾 JPG to Gallery (Pictures/TariqJaddahOptical/$saved)."
                                    }
                                )
                            } else {
                                onNotifyStatus("Could not save Receipt JPG to Gallery.")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gallery_save_receipt_jpg_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Save Receipt JPG to Gallery",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("JPG to Gallery", "گیلری میں JPG"))
                    }
                }

                HorizontalDivider()

                // High-Resolution Visual Receipt 🧾 Preview
                Card(
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                ) {
                    Image(
                        bitmap = receiptBitmap.asImageBitmap(),
                        contentDescription = "Receipt ${order.orderNumber} Preview",
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
