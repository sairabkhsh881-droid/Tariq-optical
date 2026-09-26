package com.example.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.LocalIsUrdu
import com.example.ui.theme.LowStockAmber
import com.example.ui.theme.StockHealthyGreen
import com.example.ui.tr
import com.example.util.CloudSyncConnectionStatus
import com.example.util.CloudSyncUiState
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun exportInstalledApkToDownloads(context: Context): String {
    return try {
        val sourceApk = File(context.applicationInfo.sourceDir)
        if (!sourceApk.exists()) {
            return "APK source not found on this runtime."
        }
        val fileName = "Tariq_Jaddah_Optical.apk"
        val sizeMb = String.format(Locale.US, "%.1f MB", sourceApk.length() / (1024.0 * 1024.0))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return "Could not create APK entry in Downloads."
            resolver.openOutputStream(uri)?.use { outStream ->
                FileInputStream(sourceApk).use { inStream ->
                    inStream.copyTo(outStream)
                }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)

            runCatching {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.android.package-archive"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(shareIntent, "Save / Share $fileName ($sizeMb)").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }

            "Saved inside Android Downloads/$fileName ($sizeMb). NOTE: In the AI Studio browser preview, this phone is a Cloud Emulator — to download the APK to your physical laptop or phone, click the Settings (⚙️) or GitHub icon at the very top-right of the AI Studio webpage (outside the phone screen)."
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsDir.mkdirs()
            val outFile = File(downloadsDir, fileName)
            FileInputStream(sourceApk).use { inStream ->
                FileOutputStream(outFile).use { outStream ->
                    inStream.copyTo(outStream)
                }
            }
            "Saved inside Android: ${outFile.absolutePath} ($sizeMb)"
        }
    } catch (t: Throwable) {
        "Note: To download the APK to your laptop or phone, use the AI Studio top-right Settings (⚙️) or GitHub button outside the phone screen (${t.localizedMessage ?: "ready"})."
    }
}

@Composable
fun CloudSyncAndBackupDialog(
    cloudState: CloudSyncUiState,
    totalProductsCount: Int,
    totalPiecesInStock: Int,
    totalShopProfilesCount: Int,
    totalLedgerEntriesCount: Int,
    totalSalesOrdersCount: Int,
    onBackupNow: () -> Unit,
    onRestoreFromCloud: () -> Unit,
    onToggleAutoBackup: (Boolean) -> Unit,
    onToggleRealTimeSync: (Boolean) -> Unit,
    onToggleOfflinePersistence: (Boolean) -> Unit = {},
    onToggleSimulatedOfflineMode: (Boolean) -> Unit = {},
    onVerifyLocalCache: () -> Unit = {},
    onFlushOfflineQueue: () -> Unit = {},
    onSaveFirebaseConfig: (workspaceId: String, projectId: String, appId: String, apiKey: String) -> Unit,
    onDismiss: () -> Unit
) {
    val isUrdu = LocalIsUrdu.current
    val context = LocalContext.current
    var directApkExportStatus by remember { mutableStateOf<String?>(null) }

    var workspaceIdInput by remember(cloudState.workspaceId) {
        mutableStateOf(cloudState.workspaceId)
    }
    var projectIdInput by remember(cloudState.customProjectId) {
        mutableStateOf(cloudState.customProjectId)
    }
    var appIdInput by remember(cloudState.customAppId) {
        mutableStateOf(cloudState.customAppId)
    }
    var apiKeyInput by remember(cloudState.customApiKey) {
        mutableStateOf(cloudState.customApiKey)
    }
    var showCredentialsForm by remember {
        mutableStateOf(cloudState.status == CloudSyncConnectionStatus.WAITING_FOR_FIREBASE_CONFIG)
    }

    val statusColor = when (cloudState.status) {
        CloudSyncConnectionStatus.CONNECTED_AND_SYNCED -> StockHealthyGreen
        CloudSyncConnectionStatus.SYNCING -> MaterialTheme.colorScheme.primary
        CloudSyncConnectionStatus.WAITING_FOR_FIREBASE_CONFIG -> LowStockAmber
        CloudSyncConnectionStatus.OFFLINE_OR_ERROR -> MaterialTheme.colorScheme.error
    }

    val statusIcon = when (cloudState.status) {
        CloudSyncConnectionStatus.CONNECTED_AND_SYNCED -> Icons.Default.CloudDone
        CloudSyncConnectionStatus.SYNCING -> Icons.Default.CloudSync
        CloudSyncConnectionStatus.WAITING_FOR_FIREBASE_CONFIG -> Icons.Default.CloudUpload
        CloudSyncConnectionStatus.OFFLINE_OR_ERROR -> Icons.Default.CloudOff
    }

    val formattedLastSync = remember(cloudState.lastSyncedEpochMs) {
        if (cloudState.lastSyncedEpochMs <= 0L) {
            null
        } else {
            SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.US)
                .format(Date(cloudState.lastSyncedEpochMs))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 14.dp)
                .testTag("cloud_sync_dialog")
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = statusColor.copy(alpha = 0.16f),
                            contentColor = statusColor,
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (cloudState.status == CloudSyncConnectionStatus.SYNCING) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.5.dp,
                                        color = statusColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = statusIcon,
                                        contentDescription = "Cloud Sync Status",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                        Column {
                            Text(
                                text = tr(
                                    "Save Project to GitHub & Cloud Sync",
                                    "پروجیکٹ گٹ ہب (GitHub) اور کلاؤڈ بیک اپ"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "GitHub Repository Export • Multi-Device Inventory, Khata & Sales Backup",
                                    "گٹ ہب ریپوزٹری ایکسپورٹ • ملٹی ڈیوائس اسٹاک، کھاتہ اور سیلز بیک اپ"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_cloud_sync_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Cloud Sync Dialog"
                        )
                    }
                }

                // Save Project to GitHub Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_to_github_card")
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = tr(
                                    "SAVE PROJECT TO GITHUB REPOSITORY",
                                    "پروجیکٹ کو گٹ ہب (GitHub) پر محفوظ کریں"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF38BDF8)
                            )
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "READY FOR GITHUB PUSH",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4ADE80),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = tr(
                                "Your Tariq Jaddah Optical project (with custom logo, clean user-owned catalog & customer database, New Order, Counter Sale, SPH/CYL options, Recharts-style analytics, and Khata ledger) is saved and ready to push to GitHub:\n• In the AI Studio top-right toolbar / Settings menu, click 'Push to GitHub' or 'Export to GitHub' (or 'Download ZIP') to save the complete Android codebase directly to your GitHub account.",
                                "آپ کا طارق جدہ آپٹیکل پروجیکٹ گٹ ہب (GitHub) پر محفوظ کرنے کے لیے مکمل تیار ہے:\n• اے آئی اسٹوڈیو کے اوپری دائیں مینو (Settings / GitHub) سے 'Push to GitHub' یا 'Export ZIP' پر کلک کر کے اپنے گٹ ہب اکاؤنٹ میں محفوظ کریں۔"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0)
                        )
                        Button(
                            onClick = onBackupNow,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0284C7),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_project_github_sync_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Save Project & Sync",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                tr(
                                    "Save Project State & Sync Backup Now",
                                    "پروجیکٹ اسٹیٹ اور بیک اپ ابھی محفوظ کریں"
                                ),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                directApkExportStatus = exportInstalledApkToDownloads(context)
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0D9488),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_direct_apk_no_usb_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Download Direct APK (No USB)",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                tr(
                                    "Export Direct APK to Downloads (No USB Cable)",
                                    "ڈائریکٹ APK فائل ڈاؤن لوڈز میں محفوظ کریں (بغیر USB)"
                                ),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!directApkExportStatus.isNullOrBlank()) {
                            Text(
                                text = directApkExportStatus!!,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4ADE80),
                                modifier = Modifier.testTag("direct_apk_export_status_text")
                            )
                        }
                    }
                }

                // Live Connection Status Banner Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = statusColor.copy(alpha = 0.11f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isUrdu) cloudState.status.labelUr else cloudState.status.labelEn,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }

                        Text(
                            text = if (isUrdu) cloudState.lastSyncSummaryUr else cloudState.lastSyncSummaryEn,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = tr(
                                    "Workspace ID: ${cloudState.workspaceId}",
                                    "ورک اسپیس آئی ڈی: ${cloudState.workspaceId}"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (formattedLastSync != null) {
                                    tr("Last Sync: $formattedLastSync", "آخری سنک: $formattedLastSync")
                                } else {
                                    tr("Not synced yet", "ابھی سنک نہیں ہوا")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!cloudState.errorDetails.isNullOrBlank()) {
                            Text(
                                text = cloudState.errorDetails,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Synced Data Summary Grid (Inventory, Customer Khata Ledgers, Sales Reports)
                Text(
                    text = tr(
                        "Cloud-Protected Modules Across Devices:",
                        "تمام آلات پر کلاؤڈ سے محفوظ شدہ ریکارڈز:"
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CloudModuleStatCard(
                        title = tr("Inventory (Pieces)", "اسٹاک (پیس)"),
                        value = "$totalProductsCount SKUs",
                        subtitle = tr("$totalPiecesInStock Pieces", "$totalPiecesInStock پیس"),
                        icon = Icons.Default.Inventory2,
                        accent = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f)
                    )
                    CloudModuleStatCard(
                        title = tr("Customer Khata", "کسٹمر کھاتہ"),
                        value = "$totalShopProfilesCount Shops",
                        subtitle = tr("$totalLedgerEntriesCount Entries", "$totalLedgerEntriesCount اندراجات"),
                        icon = Icons.Default.ReceiptLong,
                        accent = Color(0xFF0D9488),
                        modifier = Modifier.weight(1f)
                    )
                    CloudModuleStatCard(
                        title = tr("Sales Reports", "سیلز رپورٹس"),
                        value = "$totalSalesOrdersCount Orders",
                        subtitle = tr("PDF & Receipts", "رسیدیں اور PDF"),
                        icon = Icons.Default.Devices,
                        accent = Color(0xFFD97706),
                        modifier = Modifier.weight(1f)
                    )
                }

                // One-Tap Manual Backup & Restore Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onBackupNow,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cloud_backup_now_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Backup Now",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr("Backup Now", "ابھی بیک اپ کریں"),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledTonalButton(
                        onClick = onRestoreFromCloud,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cloud_restore_now_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Restore from Cloud",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr("Sync / Restore", "کلاؤڈ سے بحال کریں"),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Firestore Offline Persistence & Local Cache Card (Sales Records & Khata Ledger)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (cloudState.isServingFromLocalCache || !cloudState.isNetworkOnline) {
                            LowStockAmber.copy(alpha = 0.14f)
                        } else {
                            StockHealthyGreen.copy(alpha = 0.11f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("firestore_offline_persistence_card")
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
                                    imageVector = if (cloudState.isNetworkOnline && !cloudState.simulatedOfflineMode) {
                                        Icons.Default.CloudDone
                                    } else {
                                        Icons.Default.CloudOff
                                    },
                                    contentDescription = "Firestore Offline Persistence",
                                    tint = if (cloudState.isNetworkOnline && !cloudState.simulatedOfflineMode) {
                                        StockHealthyGreen
                                    } else {
                                        LowStockAmber
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = tr(
                                        "Firestore Offline Persistence & Local Cache",
                                        "فائر اسٹور آف لائن پرسیسٹنس اور لوکل کیش"
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Surface(
                                color = if (cloudState.offlinePersistenceEnabled) {
                                    StockHealthyGreen.copy(alpha = 0.18f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (cloudState.offlinePersistenceEnabled) {
                                        if (cloudState.isNetworkOnline && !cloudState.simulatedOfflineMode) {
                                            tr("CACHE ACTIVE • ONLINE", "کیش فعال • آن لائن")
                                        } else {
                                            tr("SERVING OFFLINE CACHE", "آف لائن کیش فعال")
                                        }
                                    } else {
                                        tr("MEMORY CACHE", "میموری کیش")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (cloudState.offlinePersistenceEnabled) {
                                        StockHealthyGreen
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = tr(
                                "Configured with PersistentCacheSettings (CACHE_SIZE_UNLIMITED) + Source.CACHE fallback. Sales records (New Orders & Counter Sales) and Customer Khata ledger updates cache locally on disk and remain 100% accessible when internet connection is lost, then sync automatically when back online.",
                                "انٹرنیٹ منقطع ہونے کی صورت میں تمام سیلز ریکارڈز (آرڈرز اور کاؤنٹر سیل) اور کسٹمر کھاتہ (Ledger) اپ ڈیٹس لوکل کیش میں محفوظ رہتے ہیں اور بغیر انٹرنیٹ کے مکمل دستیاب ہوتے ہیں۔"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val cachedSales = maxOf(cloudState.cachedSalesRecordsCount, totalSalesOrdersCount)
                            val cachedLedgers = maxOf(cloudState.cachedLedgerEntriesCount, totalLedgerEntriesCount)
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = tr("Cached Sales", "محفوظ سیلز"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$cachedSales Records",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0284C7),
                                        modifier = Modifier.testTag("cached_sales_records_count_text")
                                    )
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = tr("Cached Ledger", "محفوظ کھاتہ"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$cachedLedgers Entries",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0D9488),
                                        modifier = Modifier.testTag("cached_ledger_entries_count_text")
                                    )
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = tr("Offline Queue", "آف لائن قطار"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${cloudState.pendingOfflineWritesCount} Queued",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (cloudState.pendingOfflineWritesCount > 0) {
                                            LowStockAmber
                                        } else {
                                            StockHealthyGreen
                                        },
                                        modifier = Modifier.testTag("pending_offline_writes_count_text")
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(
                                        "Enable Firestore Persistent Local Cache (Unlimited)",
                                        "فائر اسٹور لوکل آف لائن پرسیسٹنس کیش (لامحدود)"
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr(
                                        "Keeps sales invoices, receipts & Khata ledger entries cached on device",
                                        "سیلز بل، رسیدیں اور کھاتہ اندراجات ہمیشہ ڈیوائس پر کیش رکھتا ہے"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = cloudState.offlinePersistenceEnabled,
                                onCheckedChange = onToggleOfflinePersistence,
                                modifier = Modifier.testTag("offline_persistence_switch")
                            )
                        }

                        HorizontalDivider()

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(
                                        "Offline Mode (Test Local Cache When Connection Lost)",
                                        "آف لائن موڈ (انٹرنیٹ بند ہونے پر لوکل کیش ٹیسٹ کریں)"
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr(
                                        "Disables Firestore network so new sales & Khata updates queue locally until reconnected",
                                        "نئے سیلز آرڈرز اور کھاتہ اپ ڈیٹس کو لوکل کیش میں محفوظ کر کے بعد میں سنک کرتا ہے"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = cloudState.simulatedOfflineMode,
                                onCheckedChange = onToggleSimulatedOfflineMode,
                                modifier = Modifier.testTag("simulate_offline_mode_switch")
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalButton(
                                onClick = onVerifyLocalCache,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("verify_local_cache_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Verify Local Cache",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr("Load Local Cache", "لوکل کیش چیک کریں"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (cloudState.pendingOfflineWritesCount > 0 || cloudState.simulatedOfflineMode) {
                                Button(
                                    onClick = onFlushOfflineQueue,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("flush_offline_queue_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "Flush Offline Queue",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tr("Sync Queued (${cloudState.pendingOfflineWritesCount})", "قطار سنک کریں"),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Automatic Backup & Multi-Device Real-Time Sync Toggles
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(
                                        "Automatic Cloud Backup on Every Change",
                                        "ہر تبدیلی پر خودکار کلاؤڈ بیک اپ"
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr(
                                        "Automatically backs up new stock, orders & Khata payments to Firestore",
                                        "نیا اسٹاک، آرڈرز اور کھاتہ ادائیگیاں خودکار طور پر کلاؤڈ پر محفوظ ہوں گی"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = cloudState.autoBackupEnabled,
                                onCheckedChange = onToggleAutoBackup,
                                modifier = Modifier.testTag("cloud_auto_backup_switch")
                            )
                        }

                        HorizontalDivider()

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(
                                        "Real-Time Multi-Device Synchronization",
                                        "ملٹی ڈیوائس لائیو سنکرونائزیشن"
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr(
                                        "Instantly syncs changes made on other phones or tablets using the same Workspace ID",
                                        "ایک ہی ورک اسپیس آئی ڈی استعمال کرنے والے تمام موبائل اور ٹیبلٹس پر لائیو سنک"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = cloudState.realTimeMultiDeviceSyncEnabled,
                                onCheckedChange = onToggleRealTimeSync,
                                modifier = Modifier.testTag("cloud_realtime_sync_switch")
                            )
                        }
                    }
                }

                // Firebase Workspace & Project Configuration Section
                OutlinedButton(
                    onClick = { showCredentialsForm = !showCredentialsForm },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("toggle_firebase_config_form_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Firebase Project Settings",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showCredentialsForm) {
                            tr("Hide Firebase Project & Workspace Settings", "فائر بیس سیٹنگز چھپائیں")
                        } else {
                            tr("Configure Firebase Project & Multi-Device Workspace ID", "فائر بیس پروجیکٹ اور ورک اسپیس سیٹنگز")
                        }
                    )
                }

                AnimatedVisibility(visible = showCredentialsForm) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = tr(
                                    "Multi-Device Workspace & Firebase Firestore Setup",
                                    "ملٹی ڈیوائس ورک اسپیس اور فائر بیس سیٹ اپ"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = if (cloudState.isDefaultFirebaseConfigured) {
                                    tr(
                                        "✓ Default google-services.json detected. All devices using the same Workspace ID below will share inventory, Khata ledgers & sales reports.",
                                        "✓ آپ کی google-services.json فائل منسلک ہے۔ یکساں ورک اسپیس آئی ڈی استعمال کرنے والے تمام آلات آپس میں سنک رہیں گے۔"
                                    )
                                } else {
                                    tr(
                                        "You can either place your Firebase 'google-services.json' inside the 'app/' folder OR enter your Firebase Project ID, Android App ID, and Web API Key below to connect right now:",
                                        "آپ اپنی 'google-services.json' فائل شامل کر سکتے ہیں یا نیچے اپنے فائر بیس پروجیکٹ کی معلومات درج کر کے فوری کلاؤڈ سنک چلا سکتے ہیں:"
                                    )
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = workspaceIdInput,
                                onValueChange = { workspaceIdInput = it },
                                label = {
                                    Text(tr("Dealer Shared Workspace ID", "مشترکہ ڈیلر ورک اسپیس آئی ڈی"))
                                },
                                placeholder = { Text("tariq_jaddah_optical_1947") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cloud_workspace_id_input")
                            )

                            if (!cloudState.isDefaultFirebaseConfigured) {
                                OutlinedTextField(
                                    value = projectIdInput,
                                    onValueChange = { projectIdInput = it },
                                    label = {
                                        Text(tr("Firebase Project ID (e.g. tariq-optical-db)", "فائر بیس پروجیکٹ آئی ڈی"))
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("cloud_project_id_input")
                                )

                                OutlinedTextField(
                                    value = appIdInput,
                                    onValueChange = { appIdInput = it },
                                    label = {
                                        Text(tr("Firebase Android App ID (1:...:android:...)", "فائر بیس اینڈرائیڈ ایپ آئی ڈی"))
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("cloud_app_id_input")
                                )

                                OutlinedTextField(
                                    value = apiKeyInput,
                                    onValueChange = { apiKeyInput = it },
                                    label = {
                                        Text(tr("Firebase Web API Key (AIza...)", "فائر بیس ویب اے پی آئی کی (API Key)"))
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("cloud_api_key_input")
                                )
                            }

                            Button(
                                onClick = {
                                    onSaveFirebaseConfig(
                                        workspaceIdInput,
                                        projectIdInput,
                                        appIdInput,
                                        apiKeyInput
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_cloud_config_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Save & Connect",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    tr(
                                        "Save Configuration & Sync with Firestore",
                                        "سیٹنگز محفوظ کریں اور کلاؤڈ سنک چلائیں"
                                    ),
                                    fontWeight = FontWeight.Bold
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
private fun CloudModuleStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = accent.copy(alpha = 0.1f)
        ),
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun OfflinePersistenceStatusBanner(
    cloudState: CloudSyncUiState,
    cachedSalesCount: Int,
    cachedLedgerCount: Int,
    onOpenOfflineCacheSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOffline = !cloudState.isNetworkOnline || cloudState.simulatedOfflineMode || cloudState.isServingFromLocalCache
    val accentColor = if (isOffline) LowStockAmber else StockHealthyGreen

    Surface(
        color = accentColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("offline_persistence_status_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isOffline) Icons.Default.CloudOff else Icons.Default.CloudDone,
                    contentDescription = "Firestore Offline Persistence",
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = if (isOffline) {
                            tr(
                                "Offline Persistence Active • Serving Local Cache ($cachedSalesCount Sales, $cachedLedgerCount Khata)",
                                "آف لائن کیش فعال • لوکل اسٹوریج سے دستیاب ($cachedSalesCount سیلز، $cachedLedgerCount کھاتہ)"
                            )
                        } else {
                            tr(
                                "Firestore Offline Persistence Active ($cachedSalesCount Sales & $cachedLedgerCount Khata Cached Locally)",
                                "فائر اسٹور آف لائن پرسیسٹنس فعال ($cachedSalesCount سیلز اور $cachedLedgerCount کھاتہ لوکل کیش میں محفوظ)"
                            )
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (cloudState.pendingOfflineWritesCount > 0) {
                        Text(
                            text = tr(
                                "${cloudState.pendingOfflineWritesCount} offline record(s) queued locally — will auto-sync when connection returns",
                                "${cloudState.pendingOfflineWritesCount} آف لائن ریکارڈز لوکل کیش میں محفوظ ہیں — انٹرنیٹ آنے پر خودکار سنک ہوں گے"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = LowStockAmber
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onOpenOfflineCacheSettings,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.testTag("open_offline_cache_settings_btn")
            ) {
                Text(
                    text = tr("Cache Info", "کیش سیٹنگز"),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

