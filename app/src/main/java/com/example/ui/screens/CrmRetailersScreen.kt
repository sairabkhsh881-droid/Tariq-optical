package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
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
import com.example.data.CustomerInteractionLog
import com.example.data.CustomerLedgerEntry
import com.example.data.DEALER_NUMBER_1
import com.example.data.DEALER_NUMBER_2
import com.example.data.InteractionType
import com.example.data.LedgerEntryType
import com.example.data.PaymentMethod
import com.example.data.RetailerOrder
import com.example.data.RetailerShopProfile
import com.example.data.TARIQ_MEHMOOD_RECEIVER_NUMBERS
import com.example.ui.LocalIsUrdu
import com.example.ui.WholesaleDashboardUiState
import com.example.ui.components.MetricStatCard
import com.example.ui.components.PaymentMethodBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatShortDate
import com.example.ui.theme.CriticalStockContainer
import com.example.ui.theme.CriticalStockRed
import com.example.ui.theme.PrecisionTeal
import com.example.ui.theme.StockHealthyContainer
import com.example.ui.theme.StockHealthyGreen
import com.example.ui.tr

@Composable
fun CrmRetailersScreen(
    uiState: WholesaleDashboardUiState,
    onSaveShopProfile: (RetailerShopProfile) -> Unit,
    onDeleteShopProfile: (RetailerShopProfile) -> Unit = {},
    onDeleteAllShopProfiles: () -> Unit = {},
    onRecordLedgerEntry: (
        shop: RetailerShopProfile,
        entryType: LedgerEntryType,
        amountPkr: Double,
        paymentMethod: PaymentMethod,
        senderNumber: String,
        receiverNumber: String,
        referenceCode: String,
        notes: String
    ) -> Unit,
    onLogInteraction: (RetailerShopProfile, InteractionType, String, String) -> Unit,
    onQuickOrderForShop: (RetailerShopProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedShopId by remember {
        mutableStateOf<Int?>(uiState.shopProfiles.firstOrNull()?.id)
    }
    var showAddOrEditShopDialog by remember { mutableStateOf(false) }
    var editingShopProfile by remember { mutableStateOf<RetailerShopProfile?>(null) }
    var deletingShopProfile by remember { mutableStateOf<RetailerShopProfile?>(null) }
    var showDeleteAllCustomersConfirm by remember { mutableStateOf(false) }
    var interactionTargetShop by remember { mutableStateOf<RetailerShopProfile?>(null) }
    var ledgerTargetShop by remember { mutableStateOf<RetailerShopProfile?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("crm_retailers_screen_list")
    ) {
        // Top KPI Row: Outstanding Customer Debt & Collected Payments
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MetricStatCard(
                    title = tr("OUTSTANDING DEBT (KHATA)", "کل بقایا ادھار (کھاتہ)"),
                    value = formatCurrency(uiState.totalCustomerDebtPkr),
                    subtitle = "${uiState.shopProfiles.size} ${tr("customers in ledger", "کسٹمرز کا کھاتہ")}",
                    icon = Icons.Default.ReceiptLong,
                    accentColor = if (uiState.totalCustomerDebtPkr > 0) CriticalStockRed else StockHealthyGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = tr("COLLECTED PAYMENTS", "کل وصول شدہ رقم"),
                    value = formatCurrency(uiState.totalLedgerCollectedPkr),
                    subtitle = tr("Cash, JazzCash & EasyPaisa", "نقد، جیز کیش اور ایزی پیسہ"),
                    icon = Icons.Default.Payments,
                    accentColor = PrecisionTeal,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Action Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(
                                "Your Customers & Khata Ledger",
                                "آپ کے کسٹمرز اور کھاتہ (Ledger)"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(
                                "Add your own customers/shops & track Cash, Debt, JazzCash & EasyPaisa",
                                "اپنے کسٹمرز شامل کریں اور نقد، ادھار، جیز کیش اور ایزی پیسہ کھاتہ چلائیں"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (uiState.shopProfiles.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { showDeleteAllCustomersConfirm = true },
                                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                    contentColor = CriticalStockRed
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("clear_all_customers_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete All Customers",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(tr("Clear All", "سب حذف"))
                            }
                        }
                        Button(
                            onClick = {
                                editingShopProfile = null
                                showAddOrEditShopDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("add_crm_shop_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddBusiness,
                                contentDescription = "Add Customer / Shop Profile",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("+ Add Customer", "+ نیا کسٹمر"))
                        }
                    }
                }

                // Official Dealer Payment & Khata Accounts Banner
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dealer_khata_numbers_banner")
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = tr(
                                "OFFICIAL DEALER KHATA & PAYMENT ACCOUNTS (TARIQ JADDAH OPTICAL):",
                                "آفیشل ڈیلر کھاتہ اور ادائیگی نمبرز (طارق جدہ آپٹیکل):"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = DEALER_NUMBER_1,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = DEALER_NUMBER_2,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.shopProfiles.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("empty_customers_card")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddBusiness,
                            contentDescription = "Empty Customers",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = tr(
                                "Your Customer List is Clean & Ready",
                                "آپ کی کسٹمر لسٹ صاف اور تیار ہے"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(
                                "All demo customers have been removed. Add your own retail customers and optical shops to manage their Khata ledger, orders, and payments.",
                                "تمام ڈیمو کسٹمرز ہٹا دیے گئے ہیں۔ اپنے کسٹمرز اور دکانیں شامل کریں اور ان کا کھاتہ اور آرڈرز چلائیں۔"
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                editingShopProfile = null
                                showAddOrEditShopDialog = true
                            },
                            modifier = Modifier.testTag("empty_state_add_customer_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("+ Add Your First Customer / Shop", "+ اپنا پہلا کسٹمر شامل کریں"))
                        }
                    }
                }
            }
        }

        // Shop Profile + Customer Ledger Cards
        items(uiState.shopProfiles, key = { it.id }) { shop ->
            val shopOrders = remember(uiState.allRetailerOrdersUnfiltered, shop.id, shop.shopName) {
                uiState.allRetailerOrdersUnfiltered.filter {
                    it.shopId == shop.id || it.retailerShopName.equals(shop.shopName, ignoreCase = true)
                }
            }
            val shopLedger = remember(uiState.ledgerEntries, shop.id, shop.shopName) {
                uiState.ledgerEntries.filter {
                    it.shopId == shop.id || it.shopName.equals(shop.shopName, ignoreCase = true)
                }
            }
            val shopInteractions = remember(uiState.interactionLogs, shop.id, shop.shopName) {
                uiState.interactionLogs.filter {
                    it.shopId == shop.id || it.shopName.equals(shop.shopName, ignoreCase = true)
                }
            }
            val isExpanded = expandedShopId == shop.id

            CrmRetailerProfileCard(
                shop = shop,
                shopOrders = shopOrders,
                shopLedger = shopLedger,
                shopInteractions = shopInteractions,
                isExpanded = isExpanded,
                onToggleExpand = {
                    expandedShopId = if (isExpanded) null else shop.id
                },
                onEditShop = {
                    editingShopProfile = shop
                    showAddOrEditShopDialog = true
                },
                onDeleteShop = {
                    deletingShopProfile = shop
                },
                onOpenLedgerDialog = {
                    ledgerTargetShop = shop
                },
                onLogInteraction = {
                    interactionTargetShop = shop
                },
                onQuickOrder = {
                    onQuickOrderForShop(shop)
                }
            )
        }
    }

    if (showAddOrEditShopDialog) {
        RetailerShopEditorDialog(
            initialShop = editingShopProfile,
            onDismiss = {
                showAddOrEditShopDialog = false
                editingShopProfile = null
            },
            onDelete = editingShopProfile?.let { existing ->
                {
                    onDeleteShopProfile(existing)
                    showAddOrEditShopDialog = false
                    editingShopProfile = null
                }
            },
            onSave = { saved ->
                onSaveShopProfile(saved)
                showAddOrEditShopDialog = false
                editingShopProfile = null
            }
        )
    }

    if (deletingShopProfile != null) {
        val target = deletingShopProfile!!
        AlertDialog(
            onDismissRequest = { deletingShopProfile = null },
            title = {
                Text(tr("Delete Customer Profile?", "کسٹمر پروفائل حذف کریں؟"))
            },
            text = {
                Text(
                    tr(
                        "Are you sure you want to delete '${target.shopName}' from your customer list?",
                        "کیا آپ واقعی '${target.shopName}' کو کسٹمر لسٹ سے حذف کرنا چاہتے ہیں؟"
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteShopProfile(target)
                        deletingShopProfile = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = CriticalStockRed
                    ),
                    modifier = Modifier.testTag("confirm_delete_customer_btn")
                ) {
                    Text(tr("Delete Customer", "حذف کریں"))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingShopProfile = null }) {
                    Text(tr("Cancel", "منسوخ"))
                }
            }
        )
    }

    if (showDeleteAllCustomersConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAllCustomersConfirm = false },
            title = {
                Text(tr("Clear All Customers?", "تمام کسٹمرز صاف کریں؟"))
            },
            text = {
                Text(
                    tr(
                        "This will delete all ${uiState.shopProfiles.size} customers from your CRM so you can add fresh customers.",
                        "یہ تمام ${uiState.shopProfiles.size} کسٹمرز کو حذف کر دے گا۔"
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAllShopProfiles()
                        showDeleteAllCustomersConfirm = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = CriticalStockRed
                    )
                ) {
                    Text(tr("Delete All Customers", "سب حذف کریں"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllCustomersConfirm = false }) {
                    Text(tr("Cancel", "منسوخ"))
                }
            }
        )
    }

    if (ledgerTargetShop != null) {
        val target = ledgerTargetShop!!
        RecordCustomerLedgerDialog(
            shop = target,
            onDismiss = { ledgerTargetShop = null },
            onSaveEntry = { entryType, amountPkr, method, senderNo, receiverNo, refCode, notes ->
                onRecordLedgerEntry(target, entryType, amountPkr, method, senderNo, receiverNo, refCode, notes)
                ledgerTargetShop = null
            }
        )
    }

    if (interactionTargetShop != null) {
        val target = interactionTargetShop!!
        LogCustomerInteractionDialog(
            shop = target,
            onDismiss = { interactionTargetShop = null },
            onConfirmLog = { type, summary, nextStep ->
                onLogInteraction(target, type, summary, nextStep)
                interactionTargetShop = null
            }
        )
    }
}

@Composable
private fun CrmRetailerProfileCard(
    shop: RetailerShopProfile,
    shopOrders: List<RetailerOrder>,
    shopLedger: List<CustomerLedgerEntry>,
    shopInteractions: List<CustomerInteractionLog>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEditShop: () -> Unit,
    onDeleteShop: () -> Unit = {},
    onOpenLedgerDialog: () -> Unit,
    onLogInteraction: () -> Unit,
    onQuickOrder: () -> Unit
) {
    val isUrdu = LocalIsUrdu.current
    val totalDebits = remember(shopLedger) {
        shopLedger.filter { it.entryType == LedgerEntryType.DEBIT_INVOICE }.sumOf { it.amountPkr }
    }
    val totalCredits = remember(shopLedger) {
        shopLedger.filter { it.entryType == LedgerEntryType.CREDIT_PAYMENT }.sumOf { it.amountPkr }
    }
    val currentDebtBalance = (totalDebits - totalCredits).coerceAtLeast(0.0)
    val totalPiecesOrdered = remember(shopOrders) { shopOrders.sumOf { it.quantity } }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("crm_shop_card_${shop.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Tier Badge + Edit + Delete + Expand
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
                            text = shop.accountTier,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = if (currentDebtBalance > 0) CriticalStockContainer else StockHealthyContainer,
                        contentColor = if (currentDebtBalance > 0) CriticalStockRed else StockHealthyGreen,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (currentDebtBalance > 0) {
                                "${tr("Debt", "بقایا ادھار")}: ${formatCurrency(currentDebtBalance)}"
                            } else {
                                tr("Khata Settled (0 Debt)", "کھاتہ صاف ہے")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEditShop,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_crm_shop_${shop.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Shop Profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteShop,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_crm_shop_${shop.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Shop Profile",
                            tint = CriticalStockRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("toggle_expand_shop_${shop.id}")
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand Shop Ledger & Details"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Shop Name & Ledger Summary
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shop.shopName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Market",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = shop.cityAndMarket,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(currentDebtBalance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (currentDebtBalance > 0) CriticalStockRed else StockHealthyGreen
                    )
                    Text(
                        text = "${shopOrders.size} ${tr("orders", "آرڈرز")} • $totalPiecesOrdered ${tr("Pieces", "پیس")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contact Details Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Contact Person",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = shop.contactPerson,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Phone",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = shop.phoneNumber,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Customer Ledger Summary Bar (Total Billed vs Paid vs Balance)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = tr("TOTAL BILLED (+)", "کل بل (+)"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(totalDebits),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = tr("RECEIVED (-)", "کل وصولی (-)"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(totalCredits),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = StockHealthyGreen
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = tr("NET DEBT (KHATA)", "بقایا کھاتہ"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(currentDebtBalance),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (currentDebtBalance > 0) CriticalStockRed else StockHealthyGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Record Payment / Ledger Entry, New Order (Pieces), Log Interaction
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onOpenLedgerDialog,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("record_ledger_entry_btn_${shop.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "Customer Ledger Entry",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(tr("Ledger / Pay", "کھاتہ / وصولی"))
                }

                FilledTonalButton(
                    onClick = onQuickOrder,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("crm_new_order_shop_${shop.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = "New Order for Shop",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(tr("New Order", "نیا آرڈر"))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onLogInteraction,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("log_interaction_btn_${shop.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddComment,
                        contentDescription = "Log Interaction",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(tr("Log Interaction", "رابطہ درج کریں"))
                }

                OutlinedButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = "View Statement",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        if (isExpanded) tr("Hide Khata (${shopLedger.size})", "کھاتہ چھپائیں")
                        else tr("View Khata (${shopLedger.size})", "مکمل کھاتہ (${shopLedger.size})")
                    )
                }
            }

            // Expandable Customer Ledger Statement, Ordering History & Interaction Log
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    HorizontalDivider()

                    // Section 1: Customer Khata Ledger Entries (with Payment Method, Sender & Receiver)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "Customer Ledger",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = tr(
                                    "Customer Ledger (Khata Statement) (${shopLedger.size})",
                                    "کسٹمر کھاتہ اسٹیٹمنٹ (${shopLedger.size})"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = onOpenLedgerDialog) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(tr("Add Entry", "نیا اندراج"))
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = tr(
                                "Dealer Accounts: $DEALER_NUMBER_1  •  $DEALER_NUMBER_2",
                                "ڈیلر کھاتہ نمبرز: $DEALER_NUMBER_1  •  $DEALER_NUMBER_2"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    if (shopLedger.isEmpty()) {
                        Text(
                            text = tr("No ledger entries recorded yet.", "اس دکان کا کوئی کھاتہ اندراج موجود نہیں۔"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        shopLedger.forEach { entry ->
                            val isCredit = entry.entryType == LedgerEntryType.CREDIT_PAYMENT
                            Surface(
                                color = if (isCredit) {
                                    StockHealthyContainer.copy(alpha = 0.45f)
                                } else {
                                    CriticalStockContainer.copy(alpha = 0.45f)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 0.8.dp,
                                        color = if (isCredit) StockHealthyGreen.copy(alpha = 0.4f)
                                        else CriticalStockRed.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        PaymentMethodBadge(method = entry.paymentMethod)
                                        Text(
                                            text = (if (isCredit) "- " else "+ ") + formatCurrency(entry.amountPkr),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCredit) StockHealthyGreen else CriticalStockRed
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${if (isUrdu) entry.entryType.urduLabel else entry.entryType.label} • Ref: ${entry.referenceCode}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${tr("Sender No", "بھیجنے والا نمبر")}: ${entry.senderNumber.ifEmpty { "N/A" }}  →  ${tr("Receiver No", "وصول کرنے والا نمبر")}: ${entry.receiverNumber.ifEmpty { "N/A" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (entry.notes.isNotBlank()) {
                                        Text(
                                            text = entry.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = formatShortDate(entry.timestampEpochMs),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Section 2: Ordering History (in Pieces)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = "Ordering History",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = tr(
                                "Ordering History (${shopOrders.size} Orders • $totalPiecesOrdered Pieces)",
                                "آرڈر ہسٹری (${shopOrders.size} آرڈرز • $totalPiecesOrdered پیس)"
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    shopOrders.forEach { order ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${order.orderNumber} • ${order.productSku} (${order.quantity} ${tr("Pieces", "پیس")})",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${order.productName} • ${order.paymentMethod.label}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${formatShortDate(order.createdAtEpochMs)} • ${if (isUrdu) order.stage.urduLabel else order.stage.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Text(
                                    text = formatCurrency(order.totalAmount),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Section 3: Customer Interaction Log
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Interaction History",
                            tint = PrecisionTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = tr(
                                "Customer Interaction Log (${shopInteractions.size})",
                                "کسٹمر رابطہ ریکارڈ (${shopInteractions.size})"
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    shopInteractions.forEach { log ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (isUrdu) log.interactionType.urduLabel else log.interactionType.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = formatShortDate(log.timestampEpochMs),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = log.summaryNotes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (log.nextActionNote.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "${tr("Next Follow-up", "اگلا اقدام")}: ${log.nextActionNote}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordCustomerLedgerDialog(
    shop: RetailerShopProfile,
    onDismiss: () -> Unit,
    onSaveEntry: (
        entryType: LedgerEntryType,
        amountPkr: Double,
        paymentMethod: PaymentMethod,
        senderNumber: String,
        receiverNumber: String,
        referenceCode: String,
        notes: String
    ) -> Unit
) {
    val isUrdu = LocalIsUrdu.current
    var selectedEntryType by remember { mutableStateOf(LedgerEntryType.CREDIT_PAYMENT) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.JAZZCASH) }
    var amountInput by remember { mutableStateOf("25000") }
    var senderNumber by remember { mutableStateOf(shop.phoneNumber) }
    var receiverNumber by remember { mutableStateOf(PaymentMethod.JAZZCASH.defaultReceiverNo) }
    var referenceCode by remember { mutableStateOf("TID-${(100000..999999).random()}") }
    var notes by remember {
        mutableStateOf("Customer ledger payment received from ${shop.shopName}")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = tr(
                    "Customer Ledger Entry — ${shop.shopName}",
                    "کسٹمر کھاتہ اندراج — ${shop.shopName}"
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
                    text = tr("1. Ledger Transaction Type:", "1. کھاتہ اندراج کی قسم:"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(LedgerEntryType.entries) { entryType ->
                        FilterChip(
                            selected = selectedEntryType == entryType,
                            onClick = {
                                selectedEntryType = entryType
                                if (entryType == LedgerEntryType.DEBIT_INVOICE) {
                                    selectedMethod = PaymentMethod.DEBT
                                    receiverNumber = PaymentMethod.DEBT.defaultReceiverNo
                                }
                            },
                            label = {
                                Text(if (isUrdu) entryType.urduLabel else entryType.label)
                            }
                        )
                    }
                }

                Text(
                    text = tr(
                        "2. Payment Method (Cash, Debt, JazzCash, EasyPaisa):",
                        "2. ادائیگی کا طریقہ (نقد، ادھار، جیز کیش، ایزی پیسہ):"
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(PaymentMethod.entries) { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = {
                                selectedMethod = method
                                receiverNumber = method.defaultReceiverNo
                            },
                            label = {
                                Text(if (isUrdu) method.urduLabel else method.label)
                            },
                            modifier = Modifier.testTag("ledger_method_${method.name.lowercase()}")
                        )
                    }
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text(tr("Amount in PKR", "رقم (PKR)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_ledger_amount_pkr")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = senderNumber,
                        onValueChange = { senderNumber = it },
                        label = {
                            Text(
                                tr(
                                    "Sender Number",
                                    "بھیجنے والا نمبر (Sender)"
                                )
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_ledger_sender_no")
                    )
                    OutlinedTextField(
                        value = receiverNumber,
                        onValueChange = { receiverNumber = it },
                        label = {
                            Text(
                                tr(
                                    "Receiver Number",
                                    "وصول کرنے والا نمبر (Receiver)"
                                )
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_ledger_receiver_no")
                    )
                }

                Text(
                    text = tr(
                        "Select Receiver Account (Tariq Mehmood):",
                        "وصول کرنے والا نمبر منتخب کریں (طارق محمود):"
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
                    value = referenceCode,
                    onValueChange = { referenceCode = it },
                    label = { Text(tr("Receipt / Transaction ID (Ref)", "رسید / ٹرانزیکشن نمبر")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(tr("Ledger Notes / Remarks", "کھاتہ تفصیل / نوٹس")) },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = (amountInput.toDoubleOrNull() ?: 1000.0).coerceAtLeast(1.0)
                    onSaveEntry(
                        selectedEntryType,
                        amt,
                        selectedMethod,
                        senderNumber.trim().ifEmpty { shop.phoneNumber },
                        receiverNumber.trim().ifEmpty { selectedMethod.defaultReceiverNo },
                        referenceCode.trim().ifEmpty { "KHATA-REF" },
                        notes.trim().ifEmpty { "Recorded via Tariq Jaddah Optical Khata" }
                    )
                },
                modifier = Modifier.testTag("confirm_save_ledger_entry_btn")
            ) {
                Text(tr("Save to Customer Ledger", "کھاتہ میں محفوظ کریں"))
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
private fun RetailerShopEditorDialog(
    initialShop: RetailerShopProfile?,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onSave: (RetailerShopProfile) -> Unit
) {
    val isEditing = initialShop != null
    var shopName by remember { mutableStateOf(initialShop?.shopName ?: "") }
    var cityAndMarket by remember {
        mutableStateOf(initialShop?.cityAndMarket ?: "Lahore • Shah Alam Market")
    }
    var contactPerson by remember { mutableStateOf(initialShop?.contactPerson ?: "") }
    var phoneNumber by remember { mutableStateOf(initialShop?.phoneNumber ?: "0300-") }
    var email by remember { mutableStateOf(initialShop?.email ?: "orders@opticalshop.pk") }
    var accountTier by remember { mutableStateOf(initialShop?.accountTier ?: "Gold Partner") }
    var paymentTerms by remember {
        mutableStateOf(initialShop?.paymentTermsPreference ?: "JazzCash / Khata 15 Days")
    }
    var creditLimitText by remember {
        mutableStateOf(initialShop?.creditLimitPkr?.toLong()?.toString() ?: "400000")
    }
    var specialNotes by remember {
        mutableStateOf(
            initialShop?.specialNotesAndPreferences
                ?: "Orders frames & lenses per piece. Prefers JazzCash / EasyPaisa settlement."
        )
    }

    val tiers = listOf("Platinum Partner", "Gold Partner", "Silver Account")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) {
                    tr("Edit Retail Shop (${initialShop?.shopName})", "دکان پروفائل میں ترمیم کریں")
                } else {
                    tr("New Retail Shop Profile & Khata", "نئی ریٹیل شاپ اور کھاتہ بنائیں")
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
                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text(tr("Retail Shop Name", "دکان کا نام")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_crm_shop_name")
                )

                OutlinedTextField(
                    value = cityAndMarket,
                    onValueChange = { cityAndMarket = it },
                    label = { Text(tr("City & Optical Market", "شہر اور مارکیٹ")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_crm_city")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = contactPerson,
                        onValueChange = { contactPerson = it },
                        label = { Text(tr("Contact Person", "مالک / رابطہ کار")) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_crm_contact_person")
                    )
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text(tr("Phone / JazzCash No", "فون / جیز کیش نمبر")) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_crm_phone")
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(tr("Email Address", "ای میل")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = tr("Account Tier:", "اکاؤنٹ درجہ:"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(tiers) { tier ->
                        FilterChip(
                            selected = accountTier == tier,
                            onClick = { accountTier = tier },
                            label = { Text(tier) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = paymentTerms,
                        onValueChange = { paymentTerms = it },
                        label = { Text(tr("Payment Preference", "پسندیدہ ادائیگی")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = creditLimitText,
                        onValueChange = { creditLimitText = it },
                        label = { Text(tr("Credit Limit (PKR)", "ادھار کی حد (PKR)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = specialNotes,
                    onValueChange = { specialNotes = it },
                    label = { Text(tr("Special Notes & Optical Preferences", "خصوصی نوٹس اور ترجیحات")) },
                    minLines = 3,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_crm_special_notes")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limitPkr = creditLimitText.toDoubleOrNull()?.coerceAtLeast(10000.0) ?: 350000.0
                    onSave(
                        RetailerShopProfile(
                            id = initialShop?.id ?: 0,
                            shopName = shopName.trim().ifEmpty { "New Optical Shop" },
                            cityAndMarket = cityAndMarket.trim().ifEmpty { "Lahore" },
                            contactPerson = contactPerson.trim().ifEmpty { "Proprietor" },
                            phoneNumber = phoneNumber.trim().ifEmpty { "0300-0000000" },
                            email = email.trim().ifEmpty { "info@optical.pk" },
                            accountTier = accountTier,
                            paymentTermsPreference = paymentTerms.trim().ifEmpty { "Cash / JazzCash" },
                            creditLimitPkr = limitPkr,
                            specialNotesAndPreferences = specialNotes.trim()
                        )
                    )
                },
                modifier = Modifier.testTag("confirm_save_crm_shop_btn")
            ) {
                Text(if (isEditing) tr("Update Profile", "اپڈیٹ کریں") else tr("Create Profile", "محفوظ کریں"))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isEditing && onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = CriticalStockRed
                        )
                    ) {
                        Text(tr("Delete Customer", "حذف کریں"))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(tr("Cancel", "منسوخ"))
                }
            }
        }
    )
}

@Composable
private fun LogCustomerInteractionDialog(
    shop: RetailerShopProfile,
    onDismiss: () -> Unit,
    onConfirmLog: (InteractionType, String, String) -> Unit
) {
    val isUrdu = LocalIsUrdu.current
    var selectedType by remember { mutableStateOf(InteractionType.PHONE_CALL) }
    var summaryNotes by remember {
        mutableStateOf("Confirmed per-piece restock requirement and JazzCash ledger settlement.")
    }
    var nextAction by remember {
        mutableStateOf("Dispatch piece order before 5 PM.")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = tr("Log Interaction — ${shop.shopName}", "رابطہ ریکارڈ کریں — ${shop.shopName}"),
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
                    text = tr("Interaction Channel:", "رابطے کا ذریعہ:"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(InteractionType.entries) { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(if (isUrdu) type.urduLabel else type.label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = summaryNotes,
                    onValueChange = { summaryNotes = it },
                    label = { Text(tr("Interaction Summary / Discussion Notes", "گفتگو کی تفصیل")) },
                    minLines = 3,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_interaction_summary")
                )

                OutlinedTextField(
                    value = nextAction,
                    onValueChange = { nextAction = it },
                    label = { Text(tr("Next Action / Follow-up Reminder", "اگلا اقدام")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_interaction_next_action")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmLog(
                        selectedType,
                        summaryNotes.trim().ifEmpty { "Spoke with ${shop.contactPerson}." },
                        nextAction.trim()
                    )
                },
                modifier = Modifier.testTag("confirm_log_interaction_btn")
            ) {
                Text(tr("Save Interaction Log", "محفوظ کریں"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cancel", "منسوخ"))
            }
        }
    )
}
