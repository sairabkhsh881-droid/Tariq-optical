package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CategoryThresholdConfig
import com.example.data.OpticalCategory
import com.example.data.OpticalProduct
import com.example.data.extractCylFromText
import com.example.data.extractSphFromText
import com.example.data.formatSphCylPowerTag
import com.example.ui.LocalIsUrdu
import com.example.ui.WholesaleDashboardUiState
import com.example.ui.components.BarcodeLabelBadge
import com.example.ui.components.CategoryBadge
import com.example.ui.components.CompactBarcodePickerDialog
import com.example.ui.components.LowStockThresholdAlertBanner
import com.example.ui.components.OpticalSphCylSelectorSection
import com.example.ui.components.SphCylPowerBadge
import com.example.ui.components.StockHealthBadge
import com.example.ui.components.TariqJaddahBrandLogoBanner
import com.example.ui.components.formatCurrency
import com.example.ui.theme.CriticalStockRed
import com.example.ui.theme.LowStockAmber
import com.example.ui.tr
import com.example.util.OpticalPrintAndExportHelper
import java.util.Locale

@Composable
fun CatalogScreen(
    uiState: WholesaleDashboardUiState,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (OpticalCategory?) -> Unit,
    onJumpToLowStock: () -> Unit,
    onSaveProduct: (OpticalProduct) -> Unit,
    onDeleteProduct: (OpticalProduct) -> Unit = {},
    onDeleteAllProducts: () -> Unit = {},
    onOpenAddStockDialog: (OpticalProduct?) -> Unit,
    onQuickRetailOrder: (OpticalProduct?) -> Unit,
    onQuickCounterSale: (OpticalProduct?) -> Unit = {},
    onQuickRestockPo: (OpticalProduct) -> Unit,
    onOpenBarcodeScanner: (String?) -> Unit = {},
    onOpenBarcodeGenerator: (OpticalProduct?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var editingProduct by remember { mutableStateOf<OpticalProduct?>(null) }
    var showNewProductDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("catalog_screen_list")
    ) {
        // Professional Brand Logo Banner containing the name of the app
        item {
            TariqJaddahBrandLogoBanner(
                compact = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Hero Banner
        item {
            CatalogHeroBanner(
                totalSkus = uiState.allProducts.size,
                totalUnits = uiState.totalUnitsInStock,
                totalValuation = uiState.totalStockValueWholesale,
                onAddSkuClick = { showNewProductDialog = true },
                onAddStockClick = { onOpenAddStockDialog(null) },
                onNewOrderClick = { onQuickRetailOrder(null) },
                onCounterSaleClick = { onQuickCounterSale(null) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        // Low-Stock Threshold Alert Banner
        if (uiState.lowStockProducts.isNotEmpty()) {
            item {
                LowStockThresholdAlertBanner(
                    lowStockCount = uiState.lowStockProducts.size,
                    onActionClick = onJumpToLowStock,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Category Showcase Cards
        item {
            CategoryShowcaseRow(
                selectedCategory = uiState.selectedCategory,
                allProducts = uiState.allProducts,
                onSelectCategory = onCategorySelect
            )
        }

        // Search & Filter Bar
        item {
            CatalogSearchAndFilterSection(
                query = uiState.searchQuery,
                selectedCategory = uiState.selectedCategory,
                resultCount = uiState.filteredCatalogProducts.size,
                onQueryChange = onSearchChange,
                onCategorySelect = onCategorySelect,
                onOpenNewProduct = { showNewProductDialog = true },
                onOpenAddStock = { onOpenAddStockDialog(null) },
                onOpenNewOrder = { onQuickRetailOrder(null) },
                onOpenCounterSale = { onQuickCounterSale(null) },
                onOpenBarcodeScanner = { onOpenBarcodeScanner(null) },
                onOpenBarcodeGenerator = { onOpenBarcodeGenerator(uiState.allProducts.firstOrNull()) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Empty State or Product List
        if (uiState.filteredCatalogProducts.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = "Add Your Own Products",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (uiState.allProducts.isEmpty()) {
                                tr(
                                    "Your Catalog is Ready — Add Your Own Optical Products",
                                    "آپ کا کیٹلاگ تیار ہے — اپنی آپٹیکل پراڈکٹس شامل کریں"
                                )
                            } else {
                                tr(
                                    "No optical products match your filter",
                                    "آپ کے فلٹر کے مطابق کوئی پراڈکٹ نہیں ملی"
                                )
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (uiState.allProducts.isEmpty()) {
                                tr(
                                    "All demo products have been removed. Tap '+ Add New Product' below to add your own frames, SPH/CYL optical lenses, or contact lenses, or start a New Order / Counter Sale.",
                                    "تمام پرانی پراڈکٹس ہٹا دی گئی ہیں۔ اپنے فریم، SPH/CYL نظر کے لینز اور کانٹیکٹ لینز شامل کرنے کے لیے نیچے '+ نئی پراڈکٹ' پر کلک کریں۔"
                                )
                            } else {
                                tr(
                                    "Try clearing the search query or selecting 'All Products'.",
                                    "تلاش صاف کریں یا 'تمام پراڈکٹس' منتخب کریں۔"
                                )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { showNewProductDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("empty_state_add_product_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(tr("+ Add Product", "+ نئی پراڈکٹ"))
                            }
                            Button(
                                onClick = { onQuickRetailOrder(null) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(tr("New Order", "نیا آرڈر"))
                            }
                            Button(
                                onClick = { onQuickCounterSale(null) },
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
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(tr("Counter Sale", "کاؤنٹر سیل"))
                            }
                        }
                        if (uiState.allProducts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    onSearchChange("")
                                    onCategorySelect(null)
                                }
                            ) {
                                Text(tr("Reset Catalog Filters", "فلٹرز ری سیٹ کریں"))
                            }
                        }
                    }
                }
            }
        } else {
            items(
                items = uiState.filteredCatalogProducts,
                key = { it.id }
            ) { product ->
                WholesaleProductCatalogCard(
                    product = product,
                    onEditProduct = { editingProduct = product },
                    onDeleteProduct = { onDeleteProduct(product) },
                    onAddStockPieces = { onOpenAddStockDialog(product) },
                    onCreateRetailOrder = { onQuickRetailOrder(product) },
                    onCreateCounterSale = { onQuickCounterSale(product) },
                    onPlaceManualRestock = { onQuickRestockPo(product) },
                    onScanOrLookupBarcode = { onOpenBarcodeScanner(product.barcode) },
                    onGenerateAndPrintBarcode = { onOpenBarcodeGenerator(product) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }

    if (showNewProductDialog || editingProduct != null) {
        ProductSkuEditorDialog(
            initialProduct = editingProduct,
            categoryThresholds = uiState.categoryThresholds,
            allProducts = uiState.allProducts,
            onDismiss = {
                showNewProductDialog = false
                editingProduct = null
            },
            onDelete = editingProduct?.let { prodToDelete ->
                {
                    onDeleteProduct(prodToDelete)
                    showNewProductDialog = false
                    editingProduct = null
                }
            },
            onSave = { saved ->
                onSaveProduct(saved)
                showNewProductDialog = false
                editingProduct = null
            }
        )
    }
}

@Composable
private fun CatalogHeroBanner(
    totalSkus: Int,
    totalUnits: Int,
    totalValuation: Double,
    onAddSkuClick: () -> Unit,
    onAddStockClick: () -> Unit,
    onNewOrderClick: () -> Unit = {},
    onCounterSaleClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(216.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_optical_banner),
                contentDescription = "Tariq Jaddah Optical Showroom",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xEE0A192F),
                                Color(0xCC0F2544),
                                Color(0x880A192F)
                            )
                        )
                    )
            )
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                        contentColor = Color(0xFF7DD3FC),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = tr(
                                "TARIQ JADDAH OPTICAL • SOLD PER PIECE (پیس)",
                                "طارق جدہ آپٹیکل • تمام مال فی پیس دستیاب ہے"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = onCounterSaleClick,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF0D9488),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("counter_sale_hero_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PointOfSale,
                                contentDescription = "Counter Sale",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tr("Counter Sale", "کاؤنٹر سیل"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        FilledTonalButton(
                            onClick = onAddSkuClick,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFFD97706),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_sku_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Product",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tr("New Product", "نئی پراڈکٹ"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = tr(
                            "Tariq Jaddah Optical Wholesale",
                            "طارق جدہ آپٹیکل ہول سیل ڈیلر"
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tr(
                            "Frames, Ophthalmic Lenses & Contact Eye Lenses — All Sold & Tracked Per Piece in PKR",
                            "عینک کے فریم، نظر کے لینز اور کانٹیکٹ آئی لینز — تمام حساب اور اسٹاک صرف پیس (Pieces) میں"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeroStatPill(label = tr("PRODUCTS", "پراڈکٹس"), value = "$totalSkus SKUs")
                    HeroStatPill(label = tr("STOCK PIECES", "کل اسٹاک پیس"), value = "$totalUnits Pcs")
                    HeroStatPill(label = tr("WHOLESALE VALUE", "ہول سیل مالیت"), value = formatCurrency(totalValuation))
                }
            }
        }
    }
}

@Composable
private fun HeroStatPill(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF94A3B8)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CategoryShowcaseRow(
    selectedCategory: OpticalCategory?,
    allProducts: List<OpticalProduct>,
    onSelectCategory: (OpticalCategory?) -> Unit
) {
    val isUrdu = LocalIsUrdu.current
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(OpticalCategory.entries) { category ->
            val count = allProducts.count { it.category == category }
            val totalPieces = allProducts.filter { it.category == category }.sumOf { it.currentStock }
            val lowCount = allProducts.count { it.category == category && it.isLowStock }
            val isSelected = selectedCategory == category
            val drawableRes = when (category) {
                OpticalCategory.EYEGLASS_FRAMES -> R.drawable.img_cat_frames
                OpticalCategory.OPHTHALMIC_LENSES -> R.drawable.img_cat_ophthalmic_lenses
                OpticalCategory.CONTACT_LENSES -> R.drawable.img_cat_contact_lenses
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp),
                modifier = Modifier
                    .width(235.dp)
                    .height(112.dp)
                    .border(
                        width = if (isSelected) 2.5.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        onSelectCategory(if (isSelected) null else category)
                    }
                    .testTag("category_card_${category.name.lowercase()}")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = drawableRes),
                        contentDescription = category.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x660F172A), Color(0xE60F172A))
                                )
                            )
                    )
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "$count SKUs • $totalPieces Pcs",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF93C5FD)
                            )
                            if (lowCount > 0) {
                                Surface(
                                    color = LowStockAmber,
                                    contentColor = Color.White,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "$lowCount Low",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = if (isUrdu) category.urduName else category.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isSelected) {
                                    tr("Filtered • Tap to show all", "فلٹر فعال • تمام دیکھنے کے لیے کلک کریں")
                                } else {
                                    tr("Sold Per Piece (پیس)", "فروخت فی پیس")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogSearchAndFilterSection(
    query: String,
    selectedCategory: OpticalCategory?,
    resultCount: Int,
    onQueryChange: (String) -> Unit,
    onCategorySelect: (OpticalCategory?) -> Unit,
    onOpenNewProduct: () -> Unit,
    onOpenAddStock: () -> Unit,
    onOpenNewOrder: () -> Unit = {},
    onOpenCounterSale: () -> Unit = {},
    onOpenBarcodeScanner: () -> Unit,
    onOpenBarcodeGenerator: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrdu = LocalIsUrdu.current
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    tr(
                        "Search SKU, barcode (8964...), frame model, lens index...",
                        "پراڈکٹ کوڈ، بارکوڈ نمبر، فریم ماڈل یا لینز تلاش کریں..."
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search catalog"
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenBarcodeScanner,
                        modifier = Modifier.testTag("search_bar_barcode_scan_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Barcode with Camera",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("catalog_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Row 1: New Order & Counter Sale (Primary Sales Options)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onOpenNewOrder,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("catalog_quick_new_order_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.AddShoppingCart,
                    contentDescription = "New Order",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tr("+ New Order", "+ نیا آرڈر"),
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onOpenCounterSale,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0D9488),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("catalog_quick_counter_sale_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.PointOfSale,
                    contentDescription = "Counter Sale",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tr("Counter Sale", "کاؤنٹر سیل"),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 2: Scan Barcode, New Product & Add New Stock
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onOpenBarcodeScanner,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("catalog_scan_barcode_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan Barcode",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(tr("Scan Barcode", "بارکوڈ اسکین"))
            }

            Button(
                onClick = onOpenNewProduct,
                modifier = Modifier
                    .weight(1f)
                    .testTag("quick_new_product_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Product",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(tr("+ New Product", "+ نئی پراڈکٹ"))
            }

            FilledTonalButton(
                onClick = onOpenAddStock,
                modifier = Modifier
                    .weight(1f)
                    .testTag("quick_add_stock_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = "Add New Stock",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(tr("+ Add Stock", "+ نیا اسٹاک"))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedButton(
            onClick = onOpenBarcodeGenerator,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("catalog_barcode_generator_studio_btn")
        ) {
            Icon(
                imageVector = Icons.Default.Print,
                contentDescription = "Category Barcode Generator & Printable Labels",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                tr(
                    "Barcode Generator by Category & Printable Labels (PDF / JPG / Print)",
                    "کیٹیگری کے مطابق بارکوڈ جنریٹر اور پرنٹ ایبل لیبلز"
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { onCategorySelect(null) },
                        label = { Text(tr("All Products", "تمام پراڈکٹس")) },
                        modifier = Modifier.testTag("filter_chip_all")
                    )
                }
                items(OpticalCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            onCategorySelect(if (selectedCategory == cat) null else cat)
                        },
                        label = { Text(if (isUrdu) cat.urduName else cat.displayName) },
                        modifier = Modifier.testTag("filter_chip_${cat.name.lowercase()}")
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$resultCount SKUs",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WholesaleProductCatalogCard(
    product: OpticalProduct,
    onEditProduct: () -> Unit,
    onDeleteProduct: () -> Unit = {},
    onAddStockPieces: () -> Unit,
    onCreateRetailOrder: () -> Unit,
    onCreateCounterSale: () -> Unit = {},
    onPlaceManualRestock: () -> Unit,
    onScanOrLookupBarcode: () -> Unit = {},
    onGenerateAndPrintBarcode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (product.isLowStock) 1.5.dp else 0.5.dp,
                color = if (product.isLowStock) LowStockAmber.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("catalog_product_card_${product.sku}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Category Badge + SKU + Edit & Delete Icons
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
                        color = MaterialTheme.colorScheme.surfaceVariant,
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEditProduct,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_product_${product.sku}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit ${product.name}",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteProduct,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_product_${product.sku}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete ${product.name}",
                            tint = CriticalStockRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Product Title & Brand
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${product.brand} • ${tr("Supplier", "سپلائر")}: ${product.supplierName} (${product.leadTimeDays}d)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Stock Health Badge + Barcode Badge (Strictly in Pieces)
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
                BarcodeLabelBadge(
                    barcode = product.barcode,
                    onClick = onScanOrLookupBarcode,
                    modifier = Modifier.testTag("product_barcode_badge_${product.sku}")
                )
            }

            if (product.powerOrSizeRange.contains("SPH", ignoreCase = true) ||
                product.powerOrSizeRange.contains("CYL", ignoreCase = true)
            ) {
                Spacer(modifier = Modifier.height(6.dp))
                SphCylPowerBadge(rawText = product.powerOrSizeRange)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Technical Optical Specs Grid
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    OpticalSpecRow(label = tr("Primary Spec", "بنیادی تفصیل"), value = product.primarySpec)
                    OpticalSpecRow(label = tr("Coating / Material", "میٹریل / کوٹنگ"), value = product.secondarySpec)
                    OpticalSpecRow(label = tr("Rx Range / Size", "سائز / نمبر رینج"), value = product.powerOrSizeRange)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Wholesale Pricing Row in PKR (Per Piece)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = tr("WHOLESALE / PIECE (PKR)", "ہول سیل قیمت فی پیس"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatCurrency(product.wholesalePrice),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tr("/ Piece", "/ فی پیس"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = tr(
                            "Cost: ${formatCurrency(product.unitCostPkr)}/Pc • Margin: ${String.format(Locale.US, "%.0f", product.profitMarginPercent)}%",
                            "لاگت: ${formatCurrency(product.unitCostPkr)}/پیس • منافع: ${String.format(Locale.US, "%.0f", product.profitMarginPercent)}%"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = tr(
                            "Min Order: ${product.minOrderQty} Pieces",
                            "کم از کم آرڈر: ${product.minOrderQty} پیس"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: New Order, Counter Sale, Add New Stock
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onCreateRetailOrder,
                    enabled = product.currentStock > 0,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sell_to_retailer_${product.sku}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = "New Order",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(tr("New Order", "نیا آرڈر"), style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onCreateCounterSale,
                    enabled = product.currentStock > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D9488),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("counter_sale_btn_${product.sku}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PointOfSale,
                        contentDescription = "Counter Sale",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(tr("Counter Sale", "کاؤنٹر سیل"), style = MaterialTheme.typography.labelMedium)
                }

                FilledTonalButton(
                    onClick = onAddStockPieces,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(0.95f)
                        .testTag("add_stock_btn_${product.sku}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = "Add Stock Pieces",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(tr("+ Stock", "+ اسٹاک"), style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
                onClick = onGenerateAndPrintBarcode,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("print_barcode_btn_${product.sku}")
            ) {
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = "Generate & Print Barcode",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    tr(
                        "Barcode (${product.category.categoryNumberCode}: ${product.barcode}) • Print / PDF / JPG",
                        "بارکوڈ پرنٹ کریں (${product.category.categoryNumberCode}: ${product.barcode}) • PDF / JPG"
                    )
                )
            }

            if (product.isLowStock) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onPlaceManualRestock,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = LowStockAmber
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("restock_po_${product.sku}")
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = "Place Manual Restock PO",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        tr(
                            "Low Stock Alert: Place Supplier PO (+${product.suggestedRestockQty} Pieces)",
                            "کم اسٹاک: سپلائر آرڈر دیں (+${product.suggestedRestockQty} پیس)"
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun OpticalSpecRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.38f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.62f)
        )
    }
}

@Composable
fun ProductSkuEditorDialog(
    initialProduct: OpticalProduct?,
    categoryThresholds: List<CategoryThresholdConfig>,
    initialBarcode: String = "",
    allProducts: List<OpticalProduct> = emptyList(),
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onSave: (OpticalProduct) -> Unit
) {
    val isEditing = initialProduct != null
    val isUrdu = LocalIsUrdu.current
    var selectedCategory by remember {
        mutableStateOf(initialProduct?.category ?: OpticalCategory.EYEGLASS_FRAMES)
    }
    val defaultCatThreshold = categoryThresholds
        .find { it.category == selectedCategory }
        ?.thresholdUnits ?: selectedCategory.defaultThreshold

    var sku by remember { mutableStateOf(initialProduct?.sku ?: "TJO-FRM-310") }
    var barcode by remember {
        mutableStateOf(
            initialProduct?.barcode?.ifBlank { initialBarcode }
                ?: initialBarcode.ifBlank { "8964001031005" }
        )
    }
    var showBarcodeCameraPicker by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var brand by remember { mutableStateOf(initialProduct?.brand ?: "Tariq Jaddah Optical") }
    var wholesalePrice by remember {
        mutableStateOf(initialProduct?.wholesalePrice?.toLong()?.toString() ?: "4800")
    }
    var unitCostPkr by remember {
        mutableStateOf(initialProduct?.unitCostPkr?.toLong()?.toString() ?: "2950")
    }
    var retailMsrp by remember {
        mutableStateOf(initialProduct?.retailMsrp?.toLong()?.toString() ?: "9500")
    }
    var minOrderQty by remember {
        mutableStateOf(initialProduct?.minOrderQty?.toString() ?: "5")
    }
    var currentStock by remember {
        mutableStateOf(initialProduct?.currentStock?.toString() ?: "40")
    }
    var lowStockThreshold by remember {
        mutableStateOf(initialProduct?.lowStockThreshold?.toString() ?: defaultCatThreshold.toString())
    }
    var primarySpec by remember {
        mutableStateOf(initialProduct?.primarySpec ?: "53-18-145mm • Full Rim Frame")
    }
    var secondarySpec by remember {
        mutableStateOf(initialProduct?.secondarySpec ?: "Beta-Titanium • Spring Hinge")
    }
    var sphPowerInput by remember {
        mutableStateOf(
            initialProduct?.let { extractSphFromText(it.powerOrSizeRange) }?.ifEmpty {
                if (initialProduct.category != OpticalCategory.EYEGLASS_FRAMES) "-1.00" else ""
            } ?: ""
        )
    }
    var cylPowerInput by remember {
        mutableStateOf(
            initialProduct?.let { extractCylFromText(it.powerOrSizeRange) }?.ifEmpty {
                if (initialProduct.category != OpticalCategory.EYEGLASS_FRAMES) "-0.50" else ""
            } ?: ""
        )
    }
    var powerOrSizeRange by remember {
        mutableStateOf(initialProduct?.powerOrSizeRange ?: "SPH -1.00 / CYL -0.50 | Assorted Range")
    }
    var supplierName by remember {
        mutableStateOf(initialProduct?.supplierName ?: "Tariq Jaddah Direct Import")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) {
                    tr("Edit Optical Product (${initialProduct?.sku})", "پراڈکٹ میں ترمیم کریں (${initialProduct?.sku})")
                } else {
                    tr("Add New Optical Product (Per Piece)", "نئی آپٹیکل پراڈکٹ شامل کریں (فی پیس)")
                },
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
                    text = tr("Product Category (All Sold Per Piece):", "پراڈکٹ کیٹیگری (فروخت فی پیس):"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(OpticalCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = cat
                                if (!isEditing) {
                                    val catDefault = categoryThresholds
                                        .find { it.category == cat }
                                        ?.thresholdUnits ?: cat.defaultThreshold
                                    lowStockThreshold = catDefault.toString()
                                    barcode = OpticalPrintAndExportHelper.generateBarcodeByCategory(
                                        category = cat,
                                        existingProducts = allProducts
                                    )
                                    if (cat != OpticalCategory.EYEGLASS_FRAMES && sphPowerInput.isBlank()) {
                                        sphPowerInput = "-1.00"
                                        cylPowerInput = "-0.50"
                                        powerOrSizeRange = formatSphCylPowerTag(sphPowerInput, cylPowerInput)
                                    }
                                }
                            },
                            label = {
                                val catTitle = if (isUrdu) cat.urduName else cat.displayName
                                Text("${cat.categoryNumberCode}: $catTitle")
                            }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text(tr("SKU Code", "پراڈکٹ کوڈ")) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.45f)
                            .testTag("input_sku_code")
                    )
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text(tr("Brand / Line", "برانڈ")) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.55f)
                            .testTag("input_brand")
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = {
                            Text(
                                tr(
                                    "Barcode by Category (${selectedCategory.categoryNumberCode})",
                                    "کیٹیگری بارکوڈ نمبر (${selectedCategory.categoryNumberCode})"
                                )
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_product_barcode")
                    )
                    FilledTonalButton(
                        onClick = {
                            barcode = OpticalPrintAndExportHelper.generateBarcodeByCategory(
                                category = selectedCategory,
                                existingProducts = allProducts,
                                customSequence = (1000..9999).random()
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("gen_barcode_by_cat_btn")
                    ) {
                        Text(tr("Gen ${selectedCategory.categoryNumberCode}", "نیا کوڈ"))
                    }
                    FilledTonalButton(
                        onClick = { showBarcodeCameraPicker = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("scan_barcode_for_sku_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Barcode",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(tr("Product Name / Model", "پراڈکٹ کا نام / ماڈل")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_product_name")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = wholesalePrice,
                        onValueChange = { wholesalePrice = it },
                        label = { Text(tr("Sale Price/Pc (PKR)", "فروخت قیمت فی پیس")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_wholesale_price")
                    )
                    OutlinedTextField(
                        value = unitCostPkr,
                        onValueChange = { unitCostPkr = it },
                        label = { Text(tr("Cost/Pc (PKR)", "خرید لاگت فی پیس")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_unit_cost")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = retailMsrp,
                        onValueChange = { retailMsrp = it },
                        label = { Text(tr("Retail MSRP/Pc (PKR)", "ریٹیل قیمت فی پیس")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minOrderQty,
                        onValueChange = { minOrderQty = it },
                        label = { Text(tr("Min Order (Pieces)", "کم از کم پیس")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentStock,
                        onValueChange = { currentStock = it },
                        label = { Text(tr("Stock (Pieces)", "موجودہ اسٹاک (پیس)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_current_stock")
                    )
                    OutlinedTextField(
                        value = lowStockThreshold,
                        onValueChange = { lowStockThreshold = it },
                        label = { Text(tr("Low Alert (Pieces)", "کم اسٹاک الرٹ (پیس)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_low_threshold")
                    )
                }

                OutlinedTextField(
                    value = primarySpec,
                    onValueChange = { primarySpec = it },
                    label = { Text(tr("Primary Optical Spec (Size / Index / Base Curve)", "سائز / انڈیکس")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = secondarySpec,
                    onValueChange = { secondarySpec = it },
                    label = { Text(tr("Material / Coating / Modality", "میٹریل / کوٹنگ")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OpticalSphCylSelectorSection(
                    sphValue = sphPowerInput,
                    cylValue = cylPowerInput,
                    onSphChange = { newSph ->
                        sphPowerInput = newSph
                        val tag = formatSphCylPowerTag(newSph, cylPowerInput)
                        if (tag.isNotEmpty()) {
                            powerOrSizeRange = tag
                        }
                    },
                    onCylChange = { newCyl ->
                        cylPowerInput = newCyl
                        val tag = formatSphCylPowerTag(sphPowerInput, newCyl)
                        if (tag.isNotEmpty()) {
                            powerOrSizeRange = tag
                        }
                    },
                    contextLabelEn = "New / Edit Item Lens SPH / CYL Power Options",
                    contextLabelUr = "نئی پراڈکٹ کا لینز SPH / CYL پاور نمبر"
                )

                OutlinedTextField(
                    value = powerOrSizeRange,
                    onValueChange = { powerOrSizeRange = it },
                    label = { Text(tr("SPH / CYL Power Range or Frame Size", "SPH / CYL پاور رینج یا سائز")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = supplierName,
                    onValueChange = { supplierName = it },
                    label = { Text(tr("Manufacturer / Optical Lab Supplier", "سپلائر / لیب کا نام")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val wPrice = wholesalePrice.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 3500.0
                    val cPrice = unitCostPkr.toDoubleOrNull()?.coerceAtLeast(1.0) ?: (wPrice * 0.62)
                    val msrp = retailMsrp.toDoubleOrNull()?.coerceAtLeast(wPrice) ?: (wPrice * 1.9)
                    val moq = minOrderQty.toIntOrNull()?.coerceAtLeast(1) ?: 4
                    val stock = currentStock.toIntOrNull()?.coerceAtLeast(0) ?: 0
                    val thresh = lowStockThreshold.toIntOrNull()?.coerceAtLeast(1) ?: 10

                    onSave(
                        OpticalProduct(
                            id = initialProduct?.id ?: 0,
                            sku = sku.trim().ifEmpty { "TJO-NEW-100" },
                            barcode = barcode.trim().ifEmpty { "8964001031005" },
                            name = name.trim().ifEmpty { "Optical Wholesale Item" },
                            brand = brand.trim().ifEmpty { "Tariq Jaddah Optical" },
                            category = selectedCategory,
                            wholesalePrice = wPrice,
                            unitCostPkr = cPrice,
                            retailMsrp = msrp,
                            minOrderQty = moq,
                            currentStock = stock,
                            lowStockThreshold = thresh,
                            primarySpec = primarySpec.trim().ifEmpty { "Standard Optical Spec" },
                            secondarySpec = secondarySpec.trim().ifEmpty { "Multi-Coated / Optical Grade" },
                            powerOrSizeRange = powerOrSizeRange.trim().ifEmpty { "Assorted Range" },
                            supplierName = supplierName.trim().ifEmpty { "Tariq Jaddah Lab Partner" }
                        )
                    )
                },
                modifier = Modifier.testTag("save_product_confirm_button")
            ) {
                Text(
                    if (isEditing) {
                        tr("Update Product", "پراڈکٹ اپڈیٹ کریں")
                    } else {
                        tr("Add Product to Catalog", "کیٹلاگ میں شامل کریں")
                    }
                )
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isEditing && onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = CriticalStockRed)
                    ) {
                        Text(tr("Delete Product", "حذف کریں"))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(tr("Cancel", "منسوخ"))
                }
            }
        }
    )

    if (showBarcodeCameraPicker) {
        CompactBarcodePickerDialog(
            products = allProducts,
            title = tr("Scan Product Barcode", "پراڈکٹ کا بارکوڈ اسکین کریں"),
            onDismiss = { showBarcodeCameraPicker = false },
            onBarcodeScanned = { scanned ->
                barcode = scanned
                showBarcodeCameraPicker = false
            }
        )
    }
}
