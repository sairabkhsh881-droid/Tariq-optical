package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.OpticalDatabase
import com.example.data.OpticalProduct
import com.example.data.OpticalRepository
import com.example.data.RetailerOrder
import com.example.data.RetailerShopProfile
import com.example.ui.LocalIsUrdu
import com.example.ui.OpticalViewModel
import com.example.ui.WholesaleTab
import com.example.ui.components.AddNewStockDialog
import com.example.ui.components.BarcodeScannerLookupAndSaleDialog
import com.example.ui.components.CloudSyncAndBackupDialog
import com.example.ui.components.OfflinePersistenceStatusBanner
import com.example.ui.components.OrderReceiptPrintAndSaveDialog
import com.example.ui.components.ProductBarcodeGeneratorDialog
import com.example.ui.components.PushNotificationCenterDialog
import com.example.ui.components.StatusFeedbackToast
import com.example.ui.components.TariqJaddahTopBarBrand
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.CreateCounterSaleDialog
import com.example.ui.screens.CreateRetailOrderDialog
import com.example.ui.screens.CrmRetailersScreen
import com.example.ui.screens.DealerLoginScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.ManualRestockOrderDialog
import com.example.ui.screens.ProductSkuEditorDialog
import com.example.ui.screens.RetailOrdersScreen
import com.example.ui.screens.SalesReportingScreen
import com.example.ui.theme.LowStockAmber
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StockHealthyGreen
import com.example.ui.tr
import com.example.util.CloudSyncConnectionStatus
import com.example.util.OpticalNotificationHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        OpticalNotificationHelper.ensureChannelCreated(applicationContext)

        val database = OpticalDatabase.getInstance(applicationContext)
        val repository = OpticalRepository(database.opticalDao(), applicationContext)

        setContent {
            MyApplicationTheme {
                val viewModel: OpticalViewModel = viewModel(
                    factory = OpticalViewModel.provideFactory(repository)
                )
                TariqJaddahOpticalApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TariqJaddahOpticalApp(viewModel: OpticalViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val isUrdu by viewModel.isUrduLanguage.collectAsStateWithLifecycle()
    val statusBannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val cloudSyncState by viewModel.cloudSyncState.collectAsStateWithLifecycle()

    // Request POST_NOTIFICATIONS permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !OpticalNotificationHelper.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Dealer Authentication Gate (Username: Tariq1947, Password: 1947)
    var isDealerAuthenticated by rememberSaveable { mutableStateOf(false) }

    // Shared dialog states
    var quickRetailOrderProduct by remember { mutableStateOf<OpticalProduct?>(null) }
    var quickRetailOrderShop by remember { mutableStateOf<RetailerShopProfile?>(null) }
    var showCreateRetailOrderModal by remember { mutableStateOf(false) }

    var quickCounterSaleProduct by remember { mutableStateOf<OpticalProduct?>(null) }
    var showCreateCounterSaleModal by remember { mutableStateOf(false) }

    var quickManualRestockProduct by remember { mutableStateOf<OpticalProduct?>(null) }
    var showManualRestockModal by remember { mutableStateOf(false) }

    var quickAddStockProduct by remember { mutableStateOf<OpticalProduct?>(null) }
    var showAddNewStockModal by remember { mutableStateOf(false) }

    var showPushCenterModal by remember { mutableStateOf(false) }

    var showBarcodeScannerModal by remember { mutableStateOf(false) }
    var scannerInitialCode by remember { mutableStateOf("") }
    var scannedBarcodeForNewProduct by remember { mutableStateOf<String?>(null) }

    var showBarcodeGeneratorModal by remember { mutableStateOf(false) }
    var barcodeGeneratorInitialProduct by remember { mutableStateOf<OpticalProduct?>(null) }
    var receiptOrderForPrintOrSave by remember { mutableStateOf<RetailerOrder?>(null) }
    var showCloudSyncModal by remember { mutableStateOf(false) }

    // BackHandler returns to CATALOG from secondary tabs when logged in
    BackHandler(enabled = isDealerAuthenticated && selectedTab != WholesaleTab.CATALOG) {
        viewModel.selectTab(WholesaleTab.CATALOG)
    }

    CompositionLocalProvider(LocalIsUrdu provides isUrdu) {
        if (!isDealerAuthenticated) {
            DealerLoginScreen(
                onLoginSuccess = {
                    isDealerAuthenticated = true
                    viewModel.notifyStatus(
                        if (isUrdu) {
                            "خوش آمدید! طارق جدہ آپٹیکل ڈیلر پورٹل کھل گیا ہے۔"
                        } else {
                            "Welcome, Tariq1947! Tariq Jaddah Optical portal unlocked."
                        }
                    )
                },
                onToggleLanguage = { viewModel.toggleUrduLanguage() }
            )
            return@CompositionLocalProvider
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 680.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    Column {
                        TopAppBar(
                            title = {
                                TariqJaddahTopBarBrand()
                            },
                            actions = {
                                // Firebase Firestore Cloud Sync & Save to GitHub Button
                                IconButton(
                                    onClick = { showCloudSyncModal = true },
                                    modifier = Modifier.testTag("top_bar_cloud_sync_button")
                                ) {
                                    val cloudIcon = when (cloudSyncState.status) {
                                        CloudSyncConnectionStatus.CONNECTED_AND_SYNCED -> Icons.Default.CloudDone
                                        CloudSyncConnectionStatus.SYNCING -> Icons.Default.CloudSync
                                        CloudSyncConnectionStatus.WAITING_FOR_FIREBASE_CONFIG -> Icons.Default.CloudUpload
                                        CloudSyncConnectionStatus.OFFLINE_OR_ERROR -> Icons.Default.CloudOff
                                    }
                                    val cloudTint = when (cloudSyncState.status) {
                                        CloudSyncConnectionStatus.CONNECTED_AND_SYNCED -> StockHealthyGreen
                                        CloudSyncConnectionStatus.SYNCING -> MaterialTheme.colorScheme.primary
                                        CloudSyncConnectionStatus.WAITING_FOR_FIREBASE_CONFIG -> LowStockAmber
                                        CloudSyncConnectionStatus.OFFLINE_OR_ERROR -> MaterialTheme.colorScheme.error
                                    }
                                    Icon(
                                        imageVector = cloudIcon,
                                        contentDescription = "Save Project to GitHub & Cloud Sync",
                                        tint = cloudTint
                                    )
                                }

                                // Camera Barcode Scanner Button (Quick Stock Lookup & Sale Adjustment)
                                FilledTonalButton(
                                    onClick = {
                                        scannerInitialCode = ""
                                        showBarcodeScannerModal = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .testTag("top_bar_barcode_scanner_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan Product Barcode",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = tr("Scan", "اسکین"),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Urdu / English Language Toggle Button
                                FilledTonalButton(
                                    onClick = { viewModel.toggleUrduLanguage() },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .testTag("language_toggle_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Translate,
                                        contentDescription = "Switch Urdu / English",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isUrdu) "English" else "اردو",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Push Notification Center Bell Button
                                IconButton(
                                    onClick = {
                                        viewModel.markAllPushAlertsRead()
                                        showPushCenterModal = true
                                    },
                                    modifier = Modifier.testTag("top_bar_push_alerts_button")
                                ) {
                                    BadgedBox(
                                        badge = {
                                            val badgeCount = maxOf(
                                                uiState.unreadPushAlertCount,
                                                uiState.lowStockProducts.size
                                            )
                                            if (badgeCount > 0) {
                                                Badge(containerColor = LowStockAmber) {
                                                    Text("$badgeCount")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.lowStockProducts.isNotEmpty()) {
                                                Icons.Default.NotificationsActive
                                            } else {
                                                Icons.Default.Notifications
                                            },
                                            contentDescription = "Low-Stock Push Notification Center",
                                            tint = if (uiState.lowStockProducts.isNotEmpty()) {
                                                LowStockAmber
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }

                                // Logout / Lock Portal Button
                                IconButton(
                                    onClick = {
                                        isDealerAuthenticated = false
                                    },
                                    modifier = Modifier.testTag("top_bar_logout_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Log Out of Dealer Portal",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        // Persistent Quick-Action Bar: New Order, Counter Sale, + Add Product, Save to GitHub
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        quickRetailOrderProduct = null
                                        quickRetailOrderShop = null
                                        showCreateRetailOrderModal = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("global_new_order_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddShoppingCart,
                                        contentDescription = "New Order",
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = tr("New Order", "نیا آرڈر"),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }

                                Button(
                                    onClick = {
                                        quickCounterSaleProduct = null
                                        showCreateCounterSaleModal = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0D9488),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("global_counter_sale_btn")
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
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }

                                FilledTonalButton(
                                    onClick = {
                                        scannedBarcodeForNewProduct = ""
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(0.95f)
                                        .testTag("global_add_product_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Product",
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = tr("+ Product", "+ پراڈکٹ"),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }

                                FilledTonalButton(
                                    onClick = { showCloudSyncModal = true },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("global_github_save_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "Save Project to GitHub",
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = tr("GitHub", "GitHub"),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    if (!isExpandedScreen) {
                        NavigationBar {
                            WholesaleTab.entries.forEach { tab ->
                                val selected = selectedTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        TabIconWithBadge(
                                            tab = tab,
                                            lowStockCount = uiState.lowStockProducts.size,
                                            openPoCount = uiState.pendingSupplierPoCount
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = if (isUrdu) tab.urduTitle else tab.title,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedScreen) {
                        NavigationRail(
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            WholesaleTab.entries.forEach { tab ->
                                val selected = selectedTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        TabIconWithBadge(
                                            tab = tab,
                                            lowStockCount = uiState.lowStockProducts.size,
                                            openPoCount = uiState.pendingSupplierPoCount
                                        )
                                    },
                                    label = { Text(if (isUrdu) tab.urduTitle else tab.title) },
                                    modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            StatusFeedbackToast(
                                message = statusBannerMessage,
                                onDismiss = { viewModel.dismissBannerMessage() }
                            )

                            val showOfflineBanner = selectedTab == WholesaleTab.RETAIL_ORDERS ||
                                selectedTab == WholesaleTab.CRM_SHOPS ||
                                selectedTab == WholesaleTab.SALES_REPORTS ||
                                !cloudSyncState.isNetworkOnline ||
                                cloudSyncState.simulatedOfflineMode ||
                                cloudSyncState.pendingOfflineWritesCount > 0

                            if (showOfflineBanner) {
                                OfflinePersistenceStatusBanner(
                                    cloudState = cloudSyncState,
                                    cachedSalesCount = maxOf(
                                        cloudSyncState.cachedSalesRecordsCount,
                                        uiState.allRetailerOrdersUnfiltered.size
                                    ),
                                    cachedLedgerCount = maxOf(
                                        cloudSyncState.cachedLedgerEntriesCount,
                                        uiState.ledgerEntries.size
                                    ),
                                    onOpenOfflineCacheSettings = { showCloudSyncModal = true },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.background,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                when (selectedTab) {
                                    WholesaleTab.CATALOG -> {
                                        CatalogScreen(
                                            uiState = uiState,
                                            onSearchChange = viewModel::updateSearchQuery,
                                            onCategorySelect = viewModel::selectCategory,
                                            onJumpToLowStock = viewModel::jumpToLowStockAlerts,
                                            onSaveProduct = viewModel::saveOrUpdateProduct,
                                            onDeleteProduct = viewModel::deleteProduct,
                                            onDeleteAllProducts = viewModel::deleteAllProducts,
                                            onOpenAddStockDialog = { product ->
                                                quickAddStockProduct = product
                                                showAddNewStockModal = true
                                            },
                                            onQuickRetailOrder = { product ->
                                                quickRetailOrderProduct = product
                                                quickRetailOrderShop = null
                                                showCreateRetailOrderModal = true
                                            },
                                            onQuickCounterSale = { product ->
                                                quickCounterSaleProduct = product
                                                showCreateCounterSaleModal = true
                                            },
                                            onQuickRestockPo = { product ->
                                                quickManualRestockProduct = product
                                                showManualRestockModal = true
                                            },
                                            onOpenBarcodeScanner = { initialCode ->
                                                scannerInitialCode = initialCode ?: ""
                                                showBarcodeScannerModal = true
                                            },
                                            onOpenBarcodeGenerator = { product ->
                                                barcodeGeneratorInitialProduct = product
                                                showBarcodeGeneratorModal = true
                                            }
                                        )
                                    }

                                    WholesaleTab.INVENTORY -> {
                                        InventoryScreen(
                                            uiState = uiState,
                                            onToggleLowStockOnly = viewModel::toggleLowStockOnly,
                                            onCategorySelect = viewModel::selectCategory,
                                            onAdjustStockAndThreshold = viewModel::adjustProductStockAndThreshold,
                                            onOpenAddNewStock = { product ->
                                                quickAddStockProduct = product
                                                showAddNewStockModal = true
                                            },
                                            onUpdateCategoryThreshold = viewModel::updateCategoryThreshold,
                                            onTriggerPushBroadcast = viewModel::triggerLowStockPushBroadcast,
                                            onPlaceManualRestock = { product ->
                                                quickManualRestockProduct = product
                                                showManualRestockModal = true
                                            },
                                            onBatchRestockAllLow = { lowProducts ->
                                                viewModel.placeBatchRestockForLowStockItems(lowProducts)
                                            },
                                            onCreateRestockOrder = { prod, qty, notes ->
                                                viewModel.placeManualRestockOrder(prod, qty, notes)
                                            },
                                            onAdvancePoStatus = viewModel::advanceRestockOrderStatus,
                                            onOpenBarcodeScanner = { initialCode ->
                                                scannerInitialCode = initialCode ?: ""
                                                showBarcodeScannerModal = true
                                            }
                                        )
                                    }

                                    WholesaleTab.RETAIL_ORDERS -> {
                                        RetailOrdersScreen(
                                            uiState = uiState,
                                            onSelectStageFilter = viewModel::selectOrderStageFilter,
                                            onOpenCreateOrderDialog = {
                                                quickRetailOrderProduct = null
                                                quickRetailOrderShop = null
                                                showCreateRetailOrderModal = true
                                            },
                                            onOpenCounterSaleDialog = {
                                                quickCounterSaleProduct = null
                                                showCreateCounterSaleModal = true
                                            },
                                            onAdvanceOrderStage = viewModel::advanceRetailerOrderStage,
                                            onOpenBarcodeScanner = {
                                                scannerInitialCode = ""
                                                showBarcodeScannerModal = true
                                            },
                                            onOpenOrderReceipt = { order ->
                                                receiptOrderForPrintOrSave = order
                                            }
                                        )
                                    }

                                    WholesaleTab.SALES_REPORTS -> {
                                        SalesReportingScreen(
                                            uiState = uiState,
                                            onSelectDateRange = viewModel::selectReportDateRange,
                                            onSelectCategory = viewModel::selectReportCategoryFilter,
                                            onSelectProduct = viewModel::selectReportProductFilter,
                                            onGenerateCsv = viewModel::buildSalesReportCsv,
                                            onNotifyStatus = viewModel::notifyStatus,
                                            onOpenOrderReceipt = { order ->
                                                receiptOrderForPrintOrSave = order
                                            },
                                            onJumpToLowStock = viewModel::jumpToLowStockAlerts
                                        )
                                    }

                                    WholesaleTab.CRM_SHOPS -> {
                                        CrmRetailersScreen(
                                            uiState = uiState,
                                            onSaveShopProfile = viewModel::saveOrUpdateShopProfile,
                                            onDeleteShopProfile = viewModel::deleteShopProfile,
                                            onDeleteAllShopProfiles = viewModel::deleteAllShopProfiles,
                                            onRecordLedgerEntry = viewModel::recordCustomerLedgerEntry,
                                            onLogInteraction = viewModel::logCustomerInteraction,
                                            onQuickOrderForShop = { shop ->
                                                quickRetailOrderShop = shop
                                                quickRetailOrderProduct = null
                                                showCreateRetailOrderModal = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Shared Dialog: Add New Stock (Pieces)
        if (showAddNewStockModal) {
            AddNewStockDialog(
                products = uiState.allProducts,
                preselectedProduct = quickAddStockProduct,
                onDismiss = {
                    showAddNewStockModal = false
                    quickAddStockProduct = null
                },
                onOpenNewProductDialog = {
                    scannedBarcodeForNewProduct = ""
                },
                onConfirmAddStock = { product, addedPieces, updatedWholesalePkr, updatedCostPkr, batchNotes ->
                    viewModel.addNewStockPieces(
                        product = product,
                        addedPieces = addedPieces,
                        updatedWholesalePricePkr = updatedWholesalePkr,
                        updatedUnitCostPkr = updatedCostPkr,
                        batchNotes = batchNotes
                    )
                    showAddNewStockModal = false
                    quickAddStockProduct = null
                }
            )
        }

        // Shared Dialog: Create Retailer Order (New Order - Sold Per Piece + Payment Methods + Sender/Receiver)
        if (showCreateRetailOrderModal) {
            CreateRetailOrderDialog(
                products = uiState.allProducts,
                shopProfiles = uiState.shopProfiles,
                preselectedProduct = quickRetailOrderProduct,
                preselectedShop = quickRetailOrderShop,
                onDismiss = {
                    showCreateRetailOrderModal = false
                    quickRetailOrderProduct = null
                    quickRetailOrderShop = null
                },
                onCreateOrder = { shopId, shop, city, contact, product, qty, rxNotes, paymentMethod, senderNo, receiverNo, paidAmt ->
                    viewModel.createRetailerOrder(
                        shopId = shopId,
                        retailerShopName = shop,
                        retailerCity = city,
                        retailerContact = contact,
                        product = product,
                        quantity = qty,
                        customRxAndLabNotes = rxNotes,
                        paymentMethod = paymentMethod,
                        senderNumber = senderNo,
                        receiverNumber = receiverNo,
                        paidAmountPkr = paidAmt
                    )
                    showCreateRetailOrderModal = false
                    quickRetailOrderProduct = null
                    quickRetailOrderShop = null
                    viewModel.selectTab(WholesaleTab.RETAIL_ORDERS)
                }
            )
        }

        // Shared Dialog: Instant Counter Sale (Walk-In / Cash Counter POS with SPH/CYL & Instant Receipt)
        if (showCreateCounterSaleModal) {
            CreateCounterSaleDialog(
                products = uiState.allProducts,
                preselectedProduct = quickCounterSaleProduct,
                onDismiss = {
                    showCreateCounterSaleModal = false
                    quickCounterSaleProduct = null
                },
                onConfirmCounterSale = { customerName, customerPhone, product, qty, unitPricePkr, discountPkr, rxAndLabNotes, paymentMethod, senderNo, receiverNo, paidAmt ->
                    showCreateCounterSaleModal = false
                    quickCounterSaleProduct = null
                    viewModel.createCounterSale(
                        customerName = customerName,
                        customerPhone = customerPhone,
                        product = product,
                        quantity = qty,
                        unitSalePricePkr = unitPricePkr,
                        discountPkr = discountPkr,
                        customRxAndLabNotes = rxAndLabNotes,
                        paymentMethod = paymentMethod,
                        senderNumber = senderNo,
                        receiverNumber = receiverNo,
                        paidAmountPkr = paidAmt,
                        onCounterSaleCreated = { createdOrder ->
                            receiptOrderForPrintOrSave = createdOrder
                            viewModel.selectTab(WholesaleTab.RETAIL_ORDERS)
                        }
                    )
                }
            )
        }

        // Shared Dialog: Manual Supplier Restock PO (Pieces)
        if (showManualRestockModal) {
            ManualRestockOrderDialog(
                products = uiState.allProducts,
                preselectedProduct = quickManualRestockProduct,
                onDismiss = {
                    showManualRestockModal = false
                    quickManualRestockProduct = null
                },
                onOpenNewProductDialog = {
                    scannedBarcodeForNewProduct = ""
                },
                onConfirmRestock = { product, qty, notes ->
                    viewModel.placeManualRestockOrder(
                        product = product,
                        orderQuantity = qty,
                        manualNotes = notes
                    )
                    showManualRestockModal = false
                    quickManualRestockProduct = null
                }
            )
        }

        // Push Notification Center Modal
        if (showPushCenterModal) {
            PushNotificationCenterDialog(
                alerts = uiState.pushAlerts,
                lowStockCount = uiState.lowStockProducts.size,
                onTriggerPushNow = viewModel::triggerLowStockPushBroadcast,
                onClearHistory = viewModel::clearAllPushAlerts,
                onJumpToInventory = {
                    viewModel.selectTab(WholesaleTab.INVENTORY)
                },
                onDismiss = { showPushCenterModal = false }
            )
        }

        // Camera Barcode Scanner & Quick Stock Lookup / Sale Adjustment Modal
        if (showBarcodeScannerModal) {
            BarcodeScannerLookupAndSaleDialog(
                products = uiState.allProducts,
                shopProfiles = uiState.shopProfiles,
                initialScannedCode = scannerInitialCode,
                onDismiss = {
                    showBarcodeScannerModal = false
                    scannerInitialCode = ""
                },
                onQuickAdjustStock = { product, newStock, newThreshold ->
                    viewModel.adjustProductStockAndThreshold(product, newStock, newThreshold)
                },
                onOpenAddStockDialog = { product ->
                    showBarcodeScannerModal = false
                    quickAddStockProduct = product
                    showAddNewStockModal = true
                },
                onCompleteQuickSale = { shopId, shopName, city, contact, product, qty, notes, method, sender, receiver, paid ->
                    viewModel.createRetailerOrder(
                        shopId = shopId,
                        retailerShopName = shopName,
                        retailerCity = city,
                        retailerContact = contact,
                        product = product,
                        quantity = qty,
                        customRxAndLabNotes = notes,
                        paymentMethod = method,
                        senderNumber = sender,
                        receiverNumber = receiver,
                        paidAmountPkr = paid
                    )
                },
                onOpenFullSaleDialog = { product ->
                    showBarcodeScannerModal = false
                    quickRetailOrderProduct = product
                    quickRetailOrderShop = null
                    showCreateRetailOrderModal = true
                },
                onCreateNewProductWithBarcode = { rawBarcode ->
                    showBarcodeScannerModal = false
                    scannedBarcodeForNewProduct = rawBarcode
                }
            )
        }

        // Register New Product from Scanned Unregistered Barcode
        if (scannedBarcodeForNewProduct != null) {
            ProductSkuEditorDialog(
                initialProduct = null,
                categoryThresholds = uiState.categoryThresholds,
                initialBarcode = scannedBarcodeForNewProduct ?: "",
                allProducts = uiState.allProducts,
                onDismiss = { scannedBarcodeForNewProduct = null },
                onSave = { newProd ->
                    viewModel.saveOrUpdateProduct(newProd)
                    scannedBarcodeForNewProduct = null
                }
            )
        }

        // Printable Category Barcode Generator Dialog
        if (showBarcodeGeneratorModal) {
            ProductBarcodeGeneratorDialog(
                products = uiState.allProducts,
                initialProduct = barcodeGeneratorInitialProduct,
                onSaveUpdatedProductBarcode = { updatedProd ->
                    viewModel.saveOrUpdateProduct(updatedProd)
                },
                onNotifyStatus = viewModel::notifyStatus,
                onDismiss = {
                    showBarcodeGeneratorModal = false
                    barcodeGeneratorInitialProduct = null
                }
            )
        }

        // Receipt 🧾 Print / Save as PDF / Save as JPG Dialog
        receiptOrderForPrintOrSave?.let { orderToPrint ->
            OrderReceiptPrintAndSaveDialog(
                order = orderToPrint,
                onDismiss = { receiptOrderForPrintOrSave = null },
                onNotifyStatus = viewModel::notifyStatus
            )
        }

        // Firebase Firestore Cloud Sync & Multi-Device Backup Dialog
        if (showCloudSyncModal) {
            CloudSyncAndBackupDialog(
                cloudState = cloudSyncState,
                totalProductsCount = uiState.allProducts.size,
                totalPiecesInStock = uiState.totalUnitsInStock,
                totalShopProfilesCount = uiState.shopProfiles.size,
                totalLedgerEntriesCount = uiState.ledgerEntries.size,
                totalSalesOrdersCount = uiState.allRetailerOrdersUnfiltered.size,
                onBackupNow = { viewModel.backupAllToCloud() },
                onRestoreFromCloud = { viewModel.restoreAllFromCloud() },
                onToggleAutoBackup = { viewModel.setCloudAutoBackup(it) },
                onToggleRealTimeSync = { viewModel.setCloudRealTimeSync(it) },
                onToggleOfflinePersistence = { viewModel.setFirestoreOfflinePersistence(it) },
                onToggleSimulatedOfflineMode = { viewModel.setSimulatedOfflineMode(it) },
                onVerifyLocalCache = { viewModel.verifyAndLoadFromLocalCache() },
                onFlushOfflineQueue = { viewModel.flushOfflineQueueToCloud() },
                onSaveFirebaseConfig = { workspaceId, projectId, appId, apiKey ->
                    viewModel.updateCloudSyncConfig(workspaceId, projectId, appId, apiKey)
                },
                onDismiss = { showCloudSyncModal = false }
            )
        }
    }
}

@Composable
private fun TabIconWithBadge(
    tab: WholesaleTab,
    lowStockCount: Int,
    openPoCount: Int
) {
    val icon: ImageVector = when (tab) {
        WholesaleTab.CATALOG -> Icons.Default.MenuBook
        WholesaleTab.INVENTORY -> Icons.Default.Inventory2
        WholesaleTab.RETAIL_ORDERS -> Icons.Default.ShoppingBag
        WholesaleTab.SALES_REPORTS -> Icons.Default.Analytics
        WholesaleTab.CRM_SHOPS -> Icons.Default.ReceiptLong
    }

    val badgeNumber = when (tab) {
        WholesaleTab.INVENTORY -> lowStockCount + openPoCount
        else -> 0
    }

    if (badgeNumber > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = LowStockAmber
                ) {
                    Text(badgeNumber.toString())
                }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tab.title
            )
        }
    } else {
        Icon(
            imageVector = icon,
            contentDescription = tab.title
        )
    }
}
