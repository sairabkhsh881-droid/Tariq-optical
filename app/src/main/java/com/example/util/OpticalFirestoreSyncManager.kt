package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import com.example.data.CategoryThresholdConfig
import com.example.data.CustomerInteractionLog
import com.example.data.CustomerLedgerEntry
import com.example.data.InteractionType
import com.example.data.LedgerEntryType
import com.example.data.OpticalCategory
import com.example.data.OpticalDao
import com.example.data.OpticalProduct
import com.example.data.PaymentMethod
import com.example.data.RestockOrderStatus
import com.example.data.RestockPurchaseOrder
import com.example.data.RetailOrderStage
import com.example.data.RetailerOrder
import com.example.data.RetailerShopProfile
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class CloudSyncConnectionStatus(
    val labelEn: String,
    val labelUr: String
) {
    CONNECTED_AND_SYNCED(
        "Connected & Synced (Offline Cache Ready)",
        "کلاؤڈ سے منسلک اور آف لائن کیش تیار"
    ),
    SYNCING(
        "Syncing with Firebase Firestore...",
        "فائر بیس کلاؤڈ کے ساتھ سنک ہو رہا ہے..."
    ),
    WAITING_FOR_FIREBASE_CONFIG(
        "Local Cache Active (Add Firebase for Cloud Sync)",
        "لوکل آف لائن کیش فعال ہے (کلاؤڈ سیٹ اپ اختیاری)"
    ),
    OFFLINE_OR_ERROR(
        "Offline Persistence Active (Sales & Khata Cached Locally)",
        "آف لائن پرسیسٹنس فعال ہے (سیلز اور کھاتہ لوکل کیش میں محفوظ)"
    )
}

data class CloudSyncUiState(
    val status: CloudSyncConnectionStatus = CloudSyncConnectionStatus.WAITING_FOR_FIREBASE_CONFIG,
    val workspaceId: String = "tariq_jaddah_optical_1947",
    val autoBackupEnabled: Boolean = true,
    val realTimeMultiDeviceSyncEnabled: Boolean = true,
    val offlinePersistenceEnabled: Boolean = true,
    val unlimitedCacheEnabled: Boolean = true,
    val isNetworkOnline: Boolean = true,
    val simulatedOfflineMode: Boolean = false,
    val isServingFromLocalCache: Boolean = false,
    val cachedSalesRecordsCount: Int = 0,
    val cachedLedgerEntriesCount: Int = 0,
    val pendingOfflineSalesCount: Int = 0,
    val pendingOfflineLedgerCount: Int = 0,
    val pendingOfflineWritesCount: Int = 0,
    val lastOfflineCacheTimestampMs: Long = 0L,
    val lastSyncedEpochMs: Long = 0L,
    val lastSyncSummaryEn: String = "Firestore offline persistence enabled: sales records & Khata ledger cache locally.",
    val lastSyncSummaryUr: String = "فائر اسٹور آف لائن پرسیسٹنس فعال ہے: سیلز اور کھاتہ ریکارڈز لوکل کیش میں محفوظ رہتے ہیں۔",
    val lastSyncedDeviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
    val isDefaultFirebaseConfigured: Boolean = false,
    val customProjectId: String = "",
    val customAppId: String = "",
    val customApiKey: String = "",
    val syncedProductCount: Int = 0,
    val syncedOrderCount: Int = 0,
    val syncedShopCount: Int = 0,
    val syncedLedgerCount: Int = 0,
    val errorDetails: String? = null
)

class OpticalFirestoreSyncManager(
    private val context: Context,
    private val dao: OpticalDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _syncState = MutableStateFlow(loadInitialState())
    val syncState: StateFlow<CloudSyncUiState> = _syncState.asStateFlow()

    private var firestoreInstance: FirebaseFirestore? = null
    private val activeListeners = mutableListOf<ListenerRegistration>()
    private var networkCallbackRegistered = false

    init {
        registerNetworkConnectivityMonitor()
        scope.launch {
            initializeFirestoreAndSyncIfAvailable()
        }
    }

    private fun checkSystemNetworkOnline(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val activeNet = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNet) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Throwable) {
            true
        }
    }

    fun isEffectiveOnline(): Boolean {
        val state = _syncState.value
        return !state.simulatedOfflineMode && checkSystemNetworkOnline()
    }

    private fun registerNetworkConnectivityMonitor() {
        if (networkCallbackRegistered) return
        runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    scope.launch {
                        handleConnectivityChange(systemOnline = true)
                    }
                }

                override fun onLost(network: Network) {
                    scope.launch {
                        handleConnectivityChange(systemOnline = false)
                    }
                }
            })
            networkCallbackRegistered = true
        }
    }

    private suspend fun handleConnectivityChange(systemOnline: Boolean) {
        val effectiveOnline = systemOnline && !_syncState.value.simulatedOfflineMode
        val db = firestoreInstance
        if (!effectiveOnline) {
            runCatching { db?.disableNetwork() }
            refreshLocalCountsOnly()
            _syncState.update {
                it.copy(
                    isNetworkOnline = false,
                    isServingFromLocalCache = true,
                    status = CloudSyncConnectionStatus.OFFLINE_OR_ERROR,
                    lastSyncSummaryEn = "Internet connection lost. Firestore offline persistence & local cache active (${it.cachedSalesRecordsCount} sales, ${it.cachedLedgerEntriesCount} Khata entries accessible).",
                    lastSyncSummaryUr = "انٹرنیٹ منقطع ہے۔ فائر اسٹور آف لائن کیش فعال ہے (${it.cachedSalesRecordsCount} سیلز، ${it.cachedLedgerEntriesCount} کھاتہ اندراجات دستیاب ہیں)۔",
                    errorDetails = null
                )
            }
        } else {
            runCatching { db?.enableNetwork()?.awaitTask() }
            _syncState.update {
                it.copy(
                    isNetworkOnline = true,
                    isServingFromLocalCache = false
                )
            }
            if (_syncState.value.pendingOfflineWritesCount > 0 || _syncState.value.autoBackupEnabled) {
                flushPendingOfflineQueueToCloud()
            } else {
                refreshLocalCountsOnly()
            }
        }
    }

    private fun getPendingSalesSet(): MutableSet<String> =
        (prefs.getStringSet(KEY_PENDING_SALES_IDS, emptySet()) ?: emptySet()).toMutableSet()

    private fun getPendingLedgerSet(): MutableSet<String> =
        (prefs.getStringSet(KEY_PENDING_LEDGER_IDS, emptySet()) ?: emptySet()).toMutableSet()

    private fun savePendingSets(salesIds: Set<String>, ledgerIds: Set<String>) {
        prefs.edit()
            .putStringSet(KEY_PENDING_SALES_IDS, salesIds)
            .putStringSet(KEY_PENDING_LEDGER_IDS, ledgerIds)
            .apply()
    }

    private fun loadInitialState(): CloudSyncUiState {
        val workspaceId = prefs.getString(KEY_WORKSPACE_ID, DEFAULT_WORKSPACE_ID)
            ?.takeIf { it.isNotBlank() } ?: DEFAULT_WORKSPACE_ID
        val autoBackup = prefs.getBoolean(KEY_AUTO_BACKUP, true)
        val realTimeSync = prefs.getBoolean(KEY_REALTIME_SYNC, true)
        val offlinePersistence = prefs.getBoolean(KEY_OFFLINE_PERSISTENCE, true)
        val unlimitedCache = prefs.getBoolean(KEY_UNLIMITED_CACHE, true)
        val simulatedOffline = prefs.getBoolean(KEY_SIMULATED_OFFLINE, false)
        val lastSyncMs = prefs.getLong(KEY_LAST_SYNC_MS, 0L)
        val lastCacheMs = prefs.getLong(KEY_LAST_OFFLINE_CACHE_MS, System.currentTimeMillis())
        val customProjectId = prefs.getString(KEY_CUSTOM_PROJECT_ID, "") ?: ""
        val customAppId = prefs.getString(KEY_CUSTOM_APP_ID, "") ?: ""
        val customApiKey = prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
        val pendingSales = getPendingSalesSet().size
        val pendingLedgers = getPendingLedgerSet().size

        val defaultConfigured = checkDefaultFirebaseAvailable()
        val hasConfig = defaultConfigured ||
            (customProjectId.isNotBlank() && customAppId.isNotBlank() && customApiKey.isNotBlank())
        val netOnline = !simulatedOffline && checkSystemNetworkOnline()

        return CloudSyncUiState(
            status = when {
                !netOnline -> CloudSyncConnectionStatus.OFFLINE_OR_ERROR
                hasConfig -> CloudSyncConnectionStatus.CONNECTED_AND_SYNCED
                else -> CloudSyncConnectionStatus.WAITING_FOR_FIREBASE_CONFIG
            },
            workspaceId = workspaceId,
            autoBackupEnabled = autoBackup,
            realTimeMultiDeviceSyncEnabled = realTimeSync,
            offlinePersistenceEnabled = offlinePersistence,
            unlimitedCacheEnabled = unlimitedCache,
            isNetworkOnline = netOnline,
            simulatedOfflineMode = simulatedOffline,
            isServingFromLocalCache = !netOnline,
            pendingOfflineSalesCount = pendingSales,
            pendingOfflineLedgerCount = pendingLedgers,
            pendingOfflineWritesCount = pendingSales + pendingLedgers,
            lastOfflineCacheTimestampMs = lastCacheMs,
            lastSyncedEpochMs = lastSyncMs,
            isDefaultFirebaseConfigured = defaultConfigured,
            customProjectId = customProjectId,
            customAppId = customAppId,
            customApiKey = customApiKey
        )
    }

    private fun checkDefaultFirebaseAvailable(): Boolean {
        return try {
            val existing = FirebaseApp.getApps(context)
            if (existing.any { it.name == FirebaseApp.DEFAULT_APP_NAME }) {
                true
            } else {
                FirebaseApp.initializeApp(context) != null
            }
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Configures FirebaseFirestore with PersistentCacheSettings (unlimited on-disk SQLite cache by default)
     * so that all sales records (RetailerOrder) and Khata ledger entries (CustomerLedgerEntry) remain
     * cached locally and accessible when internet connectivity is lost.
     */
    private fun applyFirestoreOfflinePersistenceSettings(db: FirebaseFirestore, state: CloudSyncUiState) {
        runCatching {
            val cacheSizeBytes = if (state.unlimitedCacheEnabled) {
                FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
            } else {
                100L * 1024L * 1024L // 100 MB persistent cache
            }

            val settingsBuilder = FirebaseFirestoreSettings.Builder()
            if (state.offlinePersistenceEnabled) {
                val persistentCacheSettings = PersistentCacheSettings.newBuilder()
                    .setSizeBytes(cacheSizeBytes)
                    .build()
                settingsBuilder.setLocalCacheSettings(persistentCacheSettings)
            } else {
                val memoryCacheSettings = MemoryCacheSettings.newBuilder().build()
                settingsBuilder.setLocalCacheSettings(memoryCacheSettings)
            }
            db.firestoreSettings = settingsBuilder.build()
        }.onFailure {
            // Fallback for older Firestore SDKs or if settings were already locked
            runCatching {
                @Suppress("DEPRECATION")
                val fallbackSettings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(state.offlinePersistenceEnabled)
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build()
                db.firestoreSettings = fallbackSettings
            }
        }
    }

    @Synchronized
    private fun getOrCreateFirestore(): FirebaseFirestore? {
        firestoreInstance?.let { return it }

        return try {
            val state = _syncState.value
            val app: FirebaseApp? = when {
                checkDefaultFirebaseAvailable() -> FirebaseApp.getInstance()
                state.customProjectId.isNotBlank() &&
                    state.customAppId.isNotBlank() &&
                    state.customApiKey.isNotBlank() -> {
                    val existingCustom = FirebaseApp.getApps(context)
                        .firstOrNull { it.name == CUSTOM_FIREBASE_APP_NAME }
                    existingCustom ?: FirebaseApp.initializeApp(
                        context,
                        FirebaseOptions.Builder()
                            .setProjectId(state.customProjectId.trim())
                            .setApplicationId(state.customAppId.trim())
                            .setApiKey(state.customApiKey.trim())
                            .build(),
                        CUSTOM_FIREBASE_APP_NAME
                    )
                }
                else -> null
            }

            if (app == null) return null

            val db = FirebaseFirestore.getInstance(app)
            applyFirestoreOfflinePersistenceSettings(db, state)

            if (!isEffectiveOnline()) {
                runCatching { db.disableNetwork() }
            } else {
                runCatching { db.enableNetwork() }
            }

            firestoreInstance = db
            db
        } catch (t: Throwable) {
            _syncState.update {
                it.copy(
                    status = CloudSyncConnectionStatus.OFFLINE_OR_ERROR,
                    isServingFromLocalCache = true,
                    errorDetails = t.localizedMessage ?: "Firestore offline cache active"
                )
            }
            null
        }
    }

    suspend fun initializeFirestoreAndSyncIfAvailable() {
        refreshLocalCountsOnly()
        val db = getOrCreateFirestore()
        val online = isEffectiveOnline()
        if (db == null) {
            _syncState.update {
                it.copy(
                    status = if (online) {
                        CloudSyncConnectionStatus.WAITING_FOR_FIREBASE_CONFIG
                    } else {
                        CloudSyncConnectionStatus.OFFLINE_OR_ERROR
                    },
                    isNetworkOnline = online,
                    isServingFromLocalCache = !online,
                    isDefaultFirebaseConfigured = checkDefaultFirebaseAvailable()
                )
            }
            return
        }

        _syncState.update {
            it.copy(
                isNetworkOnline = online,
                isServingFromLocalCache = !online,
                isDefaultFirebaseConfigured = checkDefaultFirebaseAvailable(),
                errorDetails = null
            )
        }

        // Always warm up & merge any cached sales records and ledger entries from Firestore local cache first
        warmUpSalesAndLedgerFromFirestoreLocalCache(db, _syncState.value.workspaceId)

        if (_syncState.value.realTimeMultiDeviceSyncEnabled) {
            attachRealTimeListeners(db, _syncState.value.workspaceId)
        }

        if (!online) {
            _syncState.update {
                it.copy(
                    status = CloudSyncConnectionStatus.OFFLINE_OR_ERROR,
                    isServingFromLocalCache = true,
                    lastSyncSummaryEn = "Offline Mode: Serving ${it.cachedSalesRecordsCount} sales records & ${it.cachedLedgerEntriesCount} Khata ledger entries from local persistent cache.",
                    lastSyncSummaryUr = "آف لائن موڈ: ${it.cachedSalesRecordsCount} سیلز ریکارڈز اور ${it.cachedLedgerEntriesCount} کھاتہ اندراجات لوکل کیش سے دستیاب ہیں۔"
                )
            }
            return
        }

        if (_syncState.value.autoBackupEnabled) {
            performFullCloudBackup(silent = true)
        } else {
            refreshLocalCountsOnly()
        }
    }

    /**
     * Reads sales records (SUB_RETAILER_ORDERS) and customer Khata ledger updates (SUB_LEDGER_ENTRIES)
     * directly from Firestore's on-disk PersistentCache (Source.CACHE) and merges them into Room
     * so they are immediately accessible even when the app starts with zero internet connection.
     */
    private suspend fun warmUpSalesAndLedgerFromFirestoreLocalCache(
        db: FirebaseFirestore,
        workspaceId: String
    ) {
        runCatching {
            val rootDoc = db.collection(COLLECTION_DEALERS).document(workspaceId)

            val cachedOrdersSnap = withTimeoutOrNull(1500L) {
                rootDoc.collection(SUB_RETAILER_ORDERS).get(Source.CACHE).awaitTask()
            }
            val cachedOrders = cachedOrdersSnap?.documents?.mapNotNull { it.toRetailerOrderOrNull() }.orEmpty()
            if (cachedOrders.isNotEmpty()) {
                dao.insertRetailerOrders(cachedOrders)
            }

            val cachedLedgersSnap = withTimeoutOrNull(1500L) {
                rootDoc.collection(SUB_LEDGER_ENTRIES).get(Source.CACHE).awaitTask()
            }
            val cachedLedgers = cachedLedgersSnap?.documents?.mapNotNull { it.toLedgerEntryOrNull() }.orEmpty()
            if (cachedLedgers.isNotEmpty()) {
                dao.insertLedgerEntries(cachedLedgers)
            }

            val cachedShopsSnap = withTimeoutOrNull(1500L) {
                rootDoc.collection(SUB_SHOP_PROFILES).get(Source.CACHE).awaitTask()
            }
            val cachedShops = cachedShopsSnap?.documents?.mapNotNull { it.toShopProfileOrNull() }.orEmpty()
            if (cachedShops.isNotEmpty()) {
                dao.insertShopProfiles(cachedShops)
            }

            val cachedProductsSnap = withTimeoutOrNull(1500L) {
                rootDoc.collection(SUB_PRODUCTS).get(Source.CACHE).awaitTask()
            }
            val cachedProducts = cachedProductsSnap?.documents?.mapNotNull { it.toOpticalProductOrNull() }.orEmpty()
            if (cachedProducts.isNotEmpty()) {
                dao.insertProducts(cachedProducts)
            }
        }
        refreshLocalCountsOnly()
    }

    private suspend fun refreshLocalCountsOnly() {
        val products = dao.getAllProductsSync()
        val orders = dao.getAllRetailerOrdersSync()
        val shops = dao.getAllShopProfilesSync()
        val ledgers = dao.getAllLedgerEntriesSync()
        val pendingSales = getPendingSalesSet().size
        val pendingLedgers = getPendingLedgerSet().size
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_LAST_OFFLINE_CACHE_MS, now).apply()

        _syncState.update {
            it.copy(
                syncedProductCount = products.size,
                syncedOrderCount = orders.size,
                syncedShopCount = shops.size,
                syncedLedgerCount = ledgers.size,
                cachedSalesRecordsCount = orders.size,
                cachedLedgerEntriesCount = ledgers.size,
                pendingOfflineSalesCount = pendingSales,
                pendingOfflineLedgerCount = pendingLedgers,
                pendingOfflineWritesCount = pendingSales + pendingLedgers,
                lastOfflineCacheTimestampMs = now
            )
        }
    }

    /**
     * Immediately caches a new or updated sales record (RetailerOrder) into Firestore's local
     * persistent cache and tracks pending offline writes if the internet connection is lost.
     */
    fun cacheSalesRecordOfflineFirst(order: RetailerOrder, changeDescriptionEn: String, changeDescriptionUr: String) {
        scope.launch {
            val online = isEffectiveOnline()
            val docId = order.orderNumber.ifBlank { "ORD_${order.id}" }

            if (!online) {
                val pendingSales = getPendingSalesSet().apply { add(docId) }
                val pendingLedgers = getPendingLedgerSet()
                savePendingSets(pendingSales, pendingLedgers)
            }

            val db = getOrCreateFirestore()
            if (db != null) {
                val workspaceId = _syncState.value.workspaceId
                val docRef = db.collection(COLLECTION_DEALERS)
                    .document(workspaceId)
                    .collection(SUB_RETAILER_ORDERS)
                    .document(docId)

                // Writes synchronously to Firestore local PersistentCacheSettings on disk,
                // and queues for automatic server delivery when internet is available.
                val writeTask = docRef.set(order.toFirestoreMap(), SetOptions.merge())
                writeTask.addOnSuccessListener {
                    scope.launch {
                        val pendingSales = getPendingSalesSet().apply { remove(docId) }
                        val pendingLedgers = getPendingLedgerSet()
                        savePendingSets(pendingSales, pendingLedgers)
                        refreshLocalCountsOnly()
                    }
                }
                if (online) {
                    withTimeoutOrNull(2000L) {
                        runCatching { writeTask.awaitTask() }
                    }
                }
            }

            refreshLocalCountsOnly()
            if (!online) {
                _syncState.update {
                    it.copy(
                        status = CloudSyncConnectionStatus.OFFLINE_OR_ERROR,
                        isNetworkOnline = false,
                        isServingFromLocalCache = true,
                        lastSyncSummaryEn = "Offline Cache: Sale ${order.orderNumber} saved locally (${it.cachedSalesRecordsCount} sales cached, ${it.pendingOfflineWritesCount} queued for cloud sync).",
                        lastSyncSummaryUr = "آف لائن کیش: سیل ${order.orderNumber} لوکل اسٹوریج میں محفوظ (${it.cachedSalesRecordsCount} سیلز کیش، کلاؤڈ سنک کے لیے قطار میں)۔",
                        errorDetails = null
                    )
                }
            } else if (_syncState.value.autoBackupEnabled) {
                performFullCloudBackup(
                    silent = true,
                    customSummaryEn = changeDescriptionEn,
                    customSummaryUr = changeDescriptionUr
                )
            }
        }
    }

    /**
     * Immediately caches a Customer Khata Ledger update (Debit Invoice or Credit Payment) into
     * Firestore's local persistent cache and tracks pending offline writes when offline.
     */
    fun cacheLedgerEntryOfflineFirst(entry: CustomerLedgerEntry, changeDescriptionEn: String, changeDescriptionUr: String) {
        scope.launch {
            val online = isEffectiveOnline()
            val docId = "LEDGER_${entry.id.takeIf { it != 0 } ?: entry.referenceCode}_${entry.timestampEpochMs}"

            if (!online) {
                val pendingSales = getPendingSalesSet()
                val pendingLedgers = getPendingLedgerSet().apply { add(docId) }
                savePendingSets(pendingSales, pendingLedgers)
            }

            val db = getOrCreateFirestore()
            if (db != null) {
                val workspaceId = _syncState.value.workspaceId
                val docRef = db.collection(COLLECTION_DEALERS)
                    .document(workspaceId)
                    .collection(SUB_LEDGER_ENTRIES)
                    .document(docId)

                // Writes immediately to Firestore local PersistentCacheSettings on disk
                val writeTask = docRef.set(entry.toFirestoreMap(), SetOptions.merge())
                writeTask.addOnSuccessListener {
                    scope.launch {
                        val pendingSales = getPendingSalesSet()
                        val pendingLedgers = getPendingLedgerSet().apply { remove(docId) }
                        savePendingSets(pendingSales, pendingLedgers)
                        refreshLocalCountsOnly()
                    }
                }
                if (online) {
                    withTimeoutOrNull(2000L) {
                        runCatching { writeTask.awaitTask() }
                    }
                }
            }

            refreshLocalCountsOnly()
            if (!online) {
                _syncState.update {
                    it.copy(
                        status = CloudSyncConnectionStatus.OFFLINE_OR_ERROR,
                        isNetworkOnline = false,
                        isServingFromLocalCache = true,
                        lastSyncSummaryEn = "Offline Cache: Khata entry ${entry.referenceCode} saved locally (${it.cachedLedgerEntriesCount} ledger entries cached, ${it.pendingOfflineWritesCount} queued).",
                        lastSyncSummaryUr = "آف لائن کیش: کھاتہ اندراج ${entry.referenceCode} لوکل کیش میں محفوظ (${it.cachedLedgerEntriesCount} کھاتہ اندراجات محفوظ)۔",
                        errorDetails = null
                    )
                }
            } else if (_syncState.value.autoBackupEnabled) {
                performFullCloudBackup(
                    silent = true,
                    customSummaryEn = changeDescriptionEn,
                    customSummaryUr = changeDescriptionUr
                )
            }
        }
    }

    fun setOfflinePersistenceEnabled(enabled: Boolean, unlimitedCache: Boolean = true) {
        prefs.edit()
            .putBoolean(KEY_OFFLINE_PERSISTENCE, enabled)
            .putBoolean(KEY_UNLIMITED_CACHE, unlimitedCache)
            .apply()
        _syncState.update {
            it.copy(
                offlinePersistenceEnabled = enabled,
                unlimitedCacheEnabled = unlimitedCache,
                lastSyncSummaryEn = if (enabled) {
                    "Firestore offline persistence enabled (Unlimited local cache for sales & Khata ledger)."
                } else {
                    "Firestore switched to memory-only cache (Room local database remains active)."
                },
                lastSyncSummaryUr = if (enabled) {
                    "فائر اسٹور آف لائن پرسیسٹنس فعال کر دی گئی (سیلز اور کھاتہ کے لیے لامحدود لوکل کیش)۔"
                } else {
                    "فائر اسٹور میموری کیش پر منتقل (لوکل روم ڈیٹا بیس فعال ہے)۔"
                }
            )
        }
    }

    /**
     * Allows toggling or testing Offline Mode (calls Firestore disableNetwork() / enableNetwork())
     * so the user can verify that sales records and ledger updates remain accessible and cache locally
     * when the internet connection is lost.
     */
    fun setSimulatedOfflineMode(offline: Boolean, onResultMessage: (String) -> Unit = {}) {
        prefs.edit().putBoolean(KEY_SIMULATED_OFFLINE, offline).apply()
        _syncState.update {
            it.copy(
                simulatedOfflineMode = offline
            )
        }
        scope.launch {
            handleConnectivityChange(systemOnline = checkSystemNetworkOnline())
            val state = _syncState.value
            if (offline) {
                onResultMessage(
                    "Offline Persistence Mode Active: ${state.cachedSalesRecordsCount} sales records & ${state.cachedLedgerEntriesCount} Khata ledger entries are cached locally and accessible offline."
                )
            } else {
                onResultMessage(
                    "Online Mode Restored: Flushing queued offline sales & Khata ledger updates to Firestore."
                )
            }
        }
    }

    /**
     * Explicitly reads and restores sales records and ledger updates from the local cache
     * (Firestore Source.CACHE + Room local storage) and verifies offline accessibility.
     */
    suspend fun loadAndVerifyFromLocalCache(): String {
        val db = getOrCreateFirestore()
        if (db != null) {
            warmUpSalesAndLedgerFromFirestoreLocalCache(db, _syncState.value.workspaceId)
        } else {
            refreshLocalCountsOnly()
        }
        val state = _syncState.value
        val summaryEn =
            "Verified Local Offline Cache: ${state.cachedSalesRecordsCount} sales orders & ${state.cachedLedgerEntriesCount} Khata ledger entries accessible offline (${state.pendingOfflineWritesCount} pending cloud sync)."
        val summaryUr =
            "لوکل آف لائن کیش تصدیق شدہ: ${state.cachedSalesRecordsCount} سیلز آرڈرز اور ${state.cachedLedgerEntriesCount} کھاتہ اندراجات آف لائن دستیاب ہیں۔"
        _syncState.update {
            it.copy(
                lastSyncSummaryEn = summaryEn,
                lastSyncSummaryUr = summaryUr,
                errorDetails = null
            )
        }
        return summaryEn
    }

    suspend fun flushPendingOfflineQueueToCloud(): Boolean {
        if (!isEffectiveOnline()) {
            refreshLocalCountsOnly()
            return false
        }
        val ok = performFullCloudBackup(
            silent = true,
            customSummaryEn = "Flushed offline cached sales records & Khata ledger updates to Firestore.",
            customSummaryUr = "آف لائن کیش شدہ سیلز ریکارڈز اور کھاتہ اپ ڈیٹس کلاؤڈ کے ساتھ سنک ہو گئیں۔"
        )
        if (ok) {
            savePendingSets(emptySet(), emptySet())
            refreshLocalCountsOnly()
        }
        return ok
    }

    fun updateCloudConfiguration(
        workspaceId: String,
        customProjectId: String,
        customAppId: String,
        customApiKey: String,
        onResultMessage: (String) -> Unit = {}
    ) {
        val cleanWorkspace = workspaceId.trim().ifEmpty { DEFAULT_WORKSPACE_ID }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")

        prefs.edit()
            .putString(KEY_WORKSPACE_ID, cleanWorkspace)
            .putString(KEY_CUSTOM_PROJECT_ID, customProjectId.trim())
            .putString(KEY_CUSTOM_APP_ID, customAppId.trim())
            .putString(KEY_CUSTOM_API_KEY, customApiKey.trim())
            .apply()

        detachRealTimeListeners()
        synchronized(this) {
            firestoreInstance = null
            runCatching {
                FirebaseApp.getApps(context)
                    .firstOrNull { it.name == CUSTOM_FIREBASE_APP_NAME }
                    ?.delete()
            }
        }

        _syncState.update {
            it.copy(
                workspaceId = cleanWorkspace,
                customProjectId = customProjectId.trim(),
                customAppId = customAppId.trim(),
                customApiKey = customApiKey.trim(),
                errorDetails = null
            )
        }

        scope.launch {
            val db = getOrCreateFirestore()
            if (db != null) {
                if (_syncState.value.realTimeMultiDeviceSyncEnabled) {
                    attachRealTimeListeners(db, cleanWorkspace)
                }
                val ok = performFullCloudBackup(silent = false)
                if (ok) {
                    onResultMessage("Connected to Firebase Firestore workspace '$cleanWorkspace' with Offline Persistence enabled.")
                }
            } else {
                refreshLocalCountsOnly()
                onResultMessage("Saved workspace '$cleanWorkspace'. Local offline persistence is active; add Firebase credentials to enable remote cloud sync.")
            }
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_BACKUP, enabled).apply()
        _syncState.update { it.copy(autoBackupEnabled = enabled) }
        if (enabled) {
            scope.launch { performFullCloudBackup(silent = true) }
        }
    }

    fun setRealTimeMultiDeviceSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REALTIME_SYNC, enabled).apply()
        _syncState.update { it.copy(realTimeMultiDeviceSyncEnabled = enabled) }
        val db = getOrCreateFirestore()
        if (enabled && db != null) {
            attachRealTimeListeners(db, _syncState.value.workspaceId)
        } else {
            detachRealTimeListeners()
        }
    }

    @Synchronized
    private fun detachRealTimeListeners() {
        activeListeners.forEach { runCatching { it.remove() } }
        activeListeners.clear()
    }

    @Synchronized
    private fun attachRealTimeListeners(db: FirebaseFirestore, workspaceId: String) {
        detachRealTimeListeners()
        val rootDoc = db.collection(COLLECTION_DEALERS).document(workspaceId)

        // 1. Products / Per-Piece Inventory Listener (with MetadataChanges.INCLUDE for offline cache)
        activeListeners += rootDoc.collection(SUB_PRODUCTS)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val remoteProducts = snapshot.documents.mapNotNull { it.toOpticalProductOrNull() }
                if (remoteProducts.isNotEmpty()) {
                    scope.launch {
                        dao.insertProducts(remoteProducts)
                        if (!snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites()) {
                            markSyncTimestamp(
                                en = "Live inventory update received from cloud (${remoteProducts.size} SKUs).",
                                ur = "کلاؤڈ سے لائیو اسٹاک اپ ڈیٹ موصول ہوئی (${remoteProducts.size} پراڈکٹس)۔"
                            )
                        } else {
                            refreshLocalCountsOnly()
                        }
                    }
                }
            }

        // 2. Category Thresholds Listener
        activeListeners += rootDoc.collection(SUB_CATEGORY_THRESHOLDS)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val remoteConfigs = snapshot.documents.mapNotNull { it.toCategoryThresholdOrNull() }
                if (remoteConfigs.isNotEmpty()) {
                    scope.launch {
                        dao.upsertCategoryThresholds(remoteConfigs)
                    }
                }
            }

        // 3. Retailer Orders (Sales Records & Counter Sales) Listener — serves from local cache when offline
        activeListeners += rootDoc.collection(SUB_RETAILER_ORDERS)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val remoteOrders = snapshot.documents.mapNotNull { it.toRetailerOrderOrNull() }
                val fromCache = snapshot.metadata.isFromCache
                val hasPending = snapshot.metadata.hasPendingWrites()
                if (remoteOrders.isNotEmpty()) {
                    scope.launch {
                        dao.insertRetailerOrders(remoteOrders)
                        if (!fromCache && !hasPending) {
                            markSyncTimestamp(
                                en = "Live sales orders synced across devices (${remoteOrders.size} orders).",
                                ur = "ملٹی ڈیوائس سیلز آرڈرز سنک ہو گئے (${remoteOrders.size} آرڈرز)۔"
                            )
                        } else {
                            refreshLocalCountsOnly()
                            _syncState.update {
                                it.copy(
                                    isServingFromLocalCache = fromCache,
                                    cachedSalesRecordsCount = maxOf(it.cachedSalesRecordsCount, remoteOrders.size)
                                )
                            }
                        }
                    }
                }
            }

        // 4. Supplier Restock POs Listener
        activeListeners += rootDoc.collection(SUB_RESTOCK_ORDERS)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val remotePos = snapshot.documents.mapNotNull { it.toRestockOrderOrNull() }
                if (remotePos.isNotEmpty()) {
                    scope.launch {
                        dao.insertRestockOrders(remotePos)
                    }
                }
            }

        // 5. CRM Shop Profiles Listener
        activeListeners += rootDoc.collection(SUB_SHOP_PROFILES)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val remoteShops = snapshot.documents.mapNotNull { it.toShopProfileOrNull() }
                if (remoteShops.isNotEmpty()) {
                    scope.launch {
                        dao.insertShopProfiles(remoteShops)
                    }
                }
            }

        // 6. Customer Khata Ledger Entries Listener — serves from local cache when offline
        activeListeners += rootDoc.collection(SUB_LEDGER_ENTRIES)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val remoteLedgers = snapshot.documents.mapNotNull { it.toLedgerEntryOrNull() }
                val fromCache = snapshot.metadata.isFromCache
                val hasPending = snapshot.metadata.hasPendingWrites()
                if (remoteLedgers.isNotEmpty()) {
                    scope.launch {
                        dao.insertLedgerEntries(remoteLedgers)
                        if (!fromCache && !hasPending) {
                            markSyncTimestamp(
                                en = "Live Khata ledger synced from cloud (${remoteLedgers.size} entries).",
                                ur = "کسٹمر کھاتہ لیجر کلاؤڈ سے سنک ہو گیا (${remoteLedgers.size} اندراجات)۔"
                            )
                        } else {
                            refreshLocalCountsOnly()
                            _syncState.update {
                                it.copy(
                                    isServingFromLocalCache = fromCache,
                                    cachedLedgerEntriesCount = maxOf(it.cachedLedgerEntriesCount, remoteLedgers.size)
                                )
                            }
                        }
                    }
                }
            }

        // 7. CRM Customer Interaction Logs Listener
        activeListeners += rootDoc.collection(SUB_INTERACTION_LOGS)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val remoteLogs = snapshot.documents.mapNotNull { it.toInteractionLogOrNull() }
                if (remoteLogs.isNotEmpty()) {
                    scope.launch {
                        dao.insertInteractionLogs(remoteLogs)
                    }
                }
            }
    }

    /**
     * Triggered automatically after any local write in OpticalRepository when autoBackupEnabled = true.
     */
    fun triggerAutomaticCloudBackup(changeDescriptionEn: String, changeDescriptionUr: String) {
        if (!_syncState.value.autoBackupEnabled) {
            scope.launch { refreshLocalCountsOnly() }
            return
        }
        scope.launch {
            performFullCloudBackup(
                silent = true,
                customSummaryEn = changeDescriptionEn,
                customSummaryUr = changeDescriptionUr
            )
        }
    }

    /**
     * Pushes all local inventory products, category thresholds, retailer sales orders,
     * supplier POs, retail shop profiles, and customer Khata ledger entries to Firebase Firestore.
     * When the internet connection is lost, batch.commit() immediately writes to Firestore's local
     * PersistentCacheSettings and queues the write for automatic sync when connectivity returns.
     */
    suspend fun performFullCloudBackup(
        silent: Boolean = false,
        customSummaryEn: String? = null,
        customSummaryUr: String? = null
    ): Boolean {
        val online = isEffectiveOnline()
        val db = getOrCreateFirestore()
        if (db == null) {
            refreshLocalCountsOnly()
            _syncState.update {
                it.copy(
                    status = if (online) {
                        CloudSyncConnectionStatus.WAITING_FOR_FIREBASE_CONFIG
                    } else {
                        CloudSyncConnectionStatus.OFFLINE_OR_ERROR
                    },
                    isNetworkOnline = online,
                    isServingFromLocalCache = !online,
                    lastSyncSummaryEn = customSummaryEn
                        ?: "Cached locally (${it.cachedSalesRecordsCount} sales orders, ${it.cachedLedgerEntriesCount} Khata ledger entries accessible offline).",
                    lastSyncSummaryUr = customSummaryUr
                        ?: "لوکل کیش میں محفوظ (${it.cachedSalesRecordsCount} سیلز آرڈرز، ${it.cachedLedgerEntriesCount} کھاتہ اندراجات آف لائن دستیاب)۔",
                    errorDetails = if (silent) null else "Local offline persistence is active. Add Firebase Project credentials to also sync to remote cloud."
                )
            }
            return false
        }

        _syncState.update {
            it.copy(
                status = CloudSyncConnectionStatus.SYNCING,
                isNetworkOnline = online,
                errorDetails = null
            )
        }

        return try {
            val workspaceId = _syncState.value.workspaceId
            val rootDoc = db.collection(COLLECTION_DEALERS).document(workspaceId)

            val products = dao.getAllProductsSync()
            val thresholds = dao.getAllCategoryThresholdsSync()
            val orders = dao.getAllRetailerOrdersSync()
            val restockPos = dao.getAllRestockOrdersSync()
            val shops = dao.getAllShopProfilesSync()
            val ledgers = dao.getAllLedgerEntriesSync()
            val interactions = dao.getAllInteractionLogsSync()

            val batch = db.batch()

            products.forEach { prod ->
                val docRef = rootDoc.collection(SUB_PRODUCTS).document(prod.sku.ifBlank { "PROD_${prod.id}" })
                batch.set(docRef, prod.toFirestoreMap(), SetOptions.merge())
            }

            thresholds.forEach { cfg ->
                val docRef = rootDoc.collection(SUB_CATEGORY_THRESHOLDS).document(cfg.category.name)
                batch.set(docRef, cfg.toFirestoreMap(), SetOptions.merge())
            }

            orders.forEach { order ->
                val docRef = rootDoc.collection(SUB_RETAILER_ORDERS)
                    .document(order.orderNumber.ifBlank { "ORD_${order.id}" })
                batch.set(docRef, order.toFirestoreMap(), SetOptions.merge())
            }

            restockPos.forEach { po ->
                val docRef = rootDoc.collection(SUB_RESTOCK_ORDERS)
                    .document(po.poNumber.ifBlank { "PO_${po.id}" })
                batch.set(docRef, po.toFirestoreMap(), SetOptions.merge())
            }

            shops.forEach { shop ->
                val docRef = rootDoc.collection(SUB_SHOP_PROFILES).document("SHOP_${shop.id}")
                batch.set(docRef, shop.toFirestoreMap(), SetOptions.merge())
            }

            ledgers.forEach { entry ->
                val docRef = rootDoc.collection(SUB_LEDGER_ENTRIES)
                    .document("LEDGER_${entry.id}_${entry.timestampEpochMs}")
                batch.set(docRef, entry.toFirestoreMap(), SetOptions.merge())
            }

            interactions.forEach { log ->
                val docRef = rootDoc.collection(SUB_INTERACTION_LOGS)
                    .document("LOG_${log.id}_${log.timestampEpochMs}")
                batch.set(docRef, log.toFirestoreMap(), SetOptions.merge())
            }

            val now = System.currentTimeMillis()
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
            val totalPieces = products.sumOf { it.currentStock }
            val totalSalesRevenuePkr = orders.sumOf { it.totalAmount }
            val totalDebitPkr = ledgers.filter { it.entryType == LedgerEntryType.DEBIT_INVOICE }.sumOf { it.amountPkr }
            val totalCreditPkr = ledgers.filter { it.entryType == LedgerEntryType.CREDIT_PAYMENT }.sumOf { it.amountPkr }

            val manifestMap = mapOf(
                "workspaceId" to workspaceId,
                "dealerName" to "Tariq Jaddah Optical",
                "lastBackupEpochMs" to now,
                "deviceName" to deviceName,
                "totalProducts" to products.size,
                "totalStockPieces" to totalPieces,
                "totalOrders" to orders.size,
                "totalSalesRevenuePkr" to totalSalesRevenuePkr,
                "totalShops" to shops.size,
                "totalLedgerEntries" to ledgers.size,
                "netReceivableUdhaarPkr" to (totalDebitPkr - totalCreditPkr).coerceAtLeast(0.0)
            )
            batch.set(rootDoc, manifestMap, SetOptions.merge())

            // Enqueue commit into Firestore PersistentCacheSettings immediately.
            // If offline, batch.commit() writes to the local disk cache right away and resolves when back online.
            val commitTask = batch.commit()
            commitTask.addOnSuccessListener {
                scope.launch {
                    val syncedAt = System.currentTimeMillis()
                    prefs.edit().putLong(KEY_LAST_SYNC_MS, syncedAt).apply()
                    savePendingSets(emptySet(), emptySet())
                    refreshLocalCountsOnly()
                    _syncState.update {
                        it.copy(
                            status = CloudSyncConnectionStatus.CONNECTED_AND_SYNCED,
                            isNetworkOnline = true,
                            isServingFromLocalCache = false,
                            lastSyncedEpochMs = syncedAt,
                            lastSyncedDeviceName = deviceName,
                            errorDetails = null
                        )
                    }
                }
            }

            val completedOnline = if (online) {
                withTimeoutOrNull(3000L) {
                    commitTask.awaitTask()
                    true
                } ?: false
            } else {
                false
            }

            refreshLocalCountsOnly()

            if (completedOnline) {
                prefs.edit().putLong(KEY_LAST_SYNC_MS, now).apply()
                savePendingSets(emptySet(), emptySet())
                _syncState.update {
                    it.copy(
                        status = CloudSyncConnectionStatus.CONNECTED_AND_SYNCED,
                        isNetworkOnline = true,
                        isServingFromLocalCache = false,
                        lastSyncedEpochMs = now,
                        lastSyncedDeviceName = deviceName,
                        lastSyncSummaryEn = customSummaryEn
                            ?: "Synced & cached ${orders.size} sales records, ${ledgers.size} Khata entries, ${shops.size} shops & ${products.size} SKUs ($totalPieces pcs).",
                        lastSyncSummaryUr = customSummaryUr
                            ?: "${orders.size} سیلز آرڈرز، ${ledgers.size} کھاتہ اندراجات اور ${products.size} پراڈکٹس کلاؤڈ اور لوکل کیش میں محفوظ ہو گئیں۔",
                        syncedProductCount = products.size,
                        syncedOrderCount = orders.size,
                        syncedShopCount = shops.size,
                        syncedLedgerCount = ledgers.size,
                        cachedSalesRecordsCount = orders.size,
                        cachedLedgerEntriesCount = ledgers.size,
                        pendingOfflineSalesCount = 0,
                        pendingOfflineLedgerCount = 0,
                        pendingOfflineWritesCount = 0,
                        errorDetails = null
                    )
                }
                true
            } else {
                // Written to Firestore local persistent cache; queued for server sync when internet returns
                _syncState.update {
                    it.copy(
                        status = CloudSyncConnectionStatus.OFFLINE_OR_ERROR,
                        isNetworkOnline = false,
                        isServingFromLocalCache = true,
                        lastSyncSummaryEn = customSummaryEn
                            ?: "Cached locally in Firestore Offline Persistence (${orders.size} sales records & ${ledgers.size} Khata ledger entries queued for automatic sync).",
                        lastSyncSummaryUr = customSummaryUr
                            ?: "فائر اسٹور آف لائن کیش میں محفوظ (${orders.size} سیلز ریکارڈز اور ${ledgers.size} کھاتہ اندراجات انٹرنیٹ بحال ہونے پر خودکار سنک ہوں گے)۔",
                        cachedSalesRecordsCount = orders.size,
                        cachedLedgerEntriesCount = ledgers.size,
                        errorDetails = null
                    )
                }
                true
            }
        } catch (t: Throwable) {
            refreshLocalCountsOnly()
            _syncState.update {
                it.copy(
                    status = CloudSyncConnectionStatus.OFFLINE_OR_ERROR,
                    isServingFromLocalCache = true,
                    errorDetails = t.localizedMessage ?: "Saved in local offline cache; queued for cloud sync."
                )
            }
            false
        }
    }

    /**
     * Helper that fetches a collection from Firestore (Source.DEFAULT) when online,
     * and automatically falls back to Firestore's on-disk cache (Source.CACHE) when offline
     * or if the network request fails/times out.
     */
    private suspend fun fetchCollectionWithOfflineCacheFallback(
        db: FirebaseFirestore,
        workspaceId: String,
        subCollection: String
    ): QuerySnapshot? {
        val colRef = db.collection(COLLECTION_DEALERS).document(workspaceId).collection(subCollection)
        val online = isEffectiveOnline()
        if (online) {
            val remoteSnap = runCatching {
                withTimeoutOrNull(4000L) {
                    colRef.get(Source.DEFAULT).awaitTask()
                }
            }.getOrNull()
            if (remoteSnap != null) return remoteSnap
        }
        return runCatching {
            colRef.get(Source.CACHE).awaitTask()
        }.getOrNull()
    }

    /**
     * Pulls all collections from Firebase Firestore for the current workspaceId (automatically
     * falling back to Firestore's on-disk Source.CACHE when internet connection is lost) and merges
     * them into the local Room database so sales records and ledger updates remain accessible.
     */
    suspend fun pullAndRestoreFromCloud(): Boolean {
        val db = getOrCreateFirestore()
        if (db == null) {
            refreshLocalCountsOnly()
            val state = _syncState.value
            _syncState.update {
                it.copy(
                    status = CloudSyncConnectionStatus.WAITING_FOR_FIREBASE_CONFIG,
                    lastSyncSummaryEn = "Local Cache Active: ${state.cachedSalesRecordsCount} sales records & ${state.cachedLedgerEntriesCount} Khata ledger entries available offline.",
                    lastSyncSummaryUr = "لوکل کیش فعال ہے: ${state.cachedSalesRecordsCount} سیلز ریکارڈز اور ${state.cachedLedgerEntriesCount} کھاتہ اندراجات آف لائن دستیاب ہیں۔",
                    errorDetails = null
                )
            }
            return true
        }

        _syncState.update {
            it.copy(
                status = CloudSyncConnectionStatus.SYNCING,
                errorDetails = null
            )
        }

        return try {
            val workspaceId = _syncState.value.workspaceId
            var servedFromCache = !isEffectiveOnline()

            val prodSnap = fetchCollectionWithOfflineCacheFallback(db, workspaceId, SUB_PRODUCTS)
            if (prodSnap?.metadata?.isFromCache == true) servedFromCache = true
            val remoteProducts = prodSnap?.documents?.mapNotNull { it.toOpticalProductOrNull() }.orEmpty()
            if (remoteProducts.isNotEmpty()) {
                dao.insertProducts(remoteProducts)
            }

            val threshSnap = fetchCollectionWithOfflineCacheFallback(db, workspaceId, SUB_CATEGORY_THRESHOLDS)
            val remoteThresholds = threshSnap?.documents?.mapNotNull { it.toCategoryThresholdOrNull() }.orEmpty()
            if (remoteThresholds.isNotEmpty()) {
                dao.upsertCategoryThresholds(remoteThresholds)
            }

            val orderSnap = fetchCollectionWithOfflineCacheFallback(db, workspaceId, SUB_RETAILER_ORDERS)
            if (orderSnap?.metadata?.isFromCache == true) servedFromCache = true
            val remoteOrders = orderSnap?.documents?.mapNotNull { it.toRetailerOrderOrNull() }.orEmpty()
            if (remoteOrders.isNotEmpty()) {
                dao.insertRetailerOrders(remoteOrders)
            }

            val poSnap = fetchCollectionWithOfflineCacheFallback(db, workspaceId, SUB_RESTOCK_ORDERS)
            val remotePos = poSnap?.documents?.mapNotNull { it.toRestockOrderOrNull() }.orEmpty()
            if (remotePos.isNotEmpty()) {
                dao.insertRestockOrders(remotePos)
            }

            val shopSnap = fetchCollectionWithOfflineCacheFallback(db, workspaceId, SUB_SHOP_PROFILES)
            val remoteShops = shopSnap?.documents?.mapNotNull { it.toShopProfileOrNull() }.orEmpty()
            if (remoteShops.isNotEmpty()) {
                dao.insertShopProfiles(remoteShops)
            }

            val ledgerSnap = fetchCollectionWithOfflineCacheFallback(db, workspaceId, SUB_LEDGER_ENTRIES)
            if (ledgerSnap?.metadata?.isFromCache == true) servedFromCache = true
            val remoteLedgers = ledgerSnap?.documents?.mapNotNull { it.toLedgerEntryOrNull() }.orEmpty()
            if (remoteLedgers.isNotEmpty()) {
                dao.insertLedgerEntries(remoteLedgers)
            }

            val logSnap = fetchCollectionWithOfflineCacheFallback(db, workspaceId, SUB_INTERACTION_LOGS)
            val remoteLogs = logSnap?.documents?.mapNotNull { it.toInteractionLogOrNull() }.orEmpty()
            if (remoteLogs.isNotEmpty()) {
                dao.insertInteractionLogs(remoteLogs)
            }

            refreshLocalCountsOnly()
            val now = System.currentTimeMillis()
            prefs.edit().putLong(KEY_LAST_SYNC_MS, now).apply()
            val latestState = _syncState.value

            _syncState.update {
                it.copy(
                    status = if (servedFromCache) {
                        CloudSyncConnectionStatus.OFFLINE_OR_ERROR
                    } else {
                        CloudSyncConnectionStatus.CONNECTED_AND_SYNCED
                    },
                    isServingFromLocalCache = servedFromCache,
                    lastSyncedEpochMs = now,
                    lastSyncSummaryEn = if (servedFromCache) {
                        "Loaded from Firestore Local Offline Cache: ${latestState.cachedSalesRecordsCount} sales records & ${latestState.cachedLedgerEntriesCount} Khata ledger entries accessible offline."
                    } else {
                        "Restored ${latestState.cachedSalesRecordsCount} sales orders, ${latestState.cachedLedgerEntriesCount} Khata entries, ${latestState.syncedShopCount} shops & ${latestState.syncedProductCount} SKUs from Firestore."
                    },
                    lastSyncSummaryUr = if (servedFromCache) {
                        "فائر اسٹور لوکل آف لائن کیش سے ${latestState.cachedSalesRecordsCount} سیلز ریکارڈز اور ${latestState.cachedLedgerEntriesCount} کھاتہ اندراجات بحال ہو گئے۔"
                    } else {
                        "فائر بیس کلاؤڈ سے ${latestState.cachedSalesRecordsCount} آرڈرز، ${latestState.cachedLedgerEntriesCount} کھاتہ اندراجات اور ${latestState.syncedProductCount} پراڈکٹس بحال ہو گئیں۔"
                    },
                    errorDetails = null
                )
            }
            true
        } catch (t: Throwable) {
            refreshLocalCountsOnly()
            _syncState.update {
                it.copy(
                    status = CloudSyncConnectionStatus.OFFLINE_OR_ERROR,
                    isServingFromLocalCache = true,
                    errorDetails = t.localizedMessage ?: "Serving sales & ledger from local offline cache."
                )
            }
            false
        }
    }

    private suspend fun markSyncTimestamp(en: String, ur: String) {
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_LAST_SYNC_MS, now).apply()
        refreshLocalCountsOnly()
        _syncState.update {
            it.copy(
                status = CloudSyncConnectionStatus.CONNECTED_AND_SYNCED,
                isServingFromLocalCache = false,
                lastSyncedEpochMs = now,
                lastSyncSummaryEn = en,
                lastSyncSummaryUr = ur,
                errorDetails = null
            )
        }
    }

    companion object {
        private const val PREFS_NAME = "tariq_jaddah_firestore_sync_prefs_v7"
        private const val KEY_WORKSPACE_ID = "workspace_id"
        private const val KEY_AUTO_BACKUP = "auto_backup_enabled"
        private const val KEY_REALTIME_SYNC = "realtime_sync_enabled"
        private const val KEY_OFFLINE_PERSISTENCE = "offline_persistence_enabled"
        private const val KEY_UNLIMITED_CACHE = "unlimited_cache_enabled"
        private const val KEY_SIMULATED_OFFLINE = "simulated_offline_mode"
        private const val KEY_PENDING_SALES_IDS = "pending_offline_sales_ids"
        private const val KEY_PENDING_LEDGER_IDS = "pending_offline_ledger_ids"
        private const val KEY_LAST_OFFLINE_CACHE_MS = "last_offline_cache_epoch_ms"
        private const val KEY_LAST_SYNC_MS = "last_sync_epoch_ms"
        private const val KEY_CUSTOM_PROJECT_ID = "custom_firebase_project_id"
        private const val KEY_CUSTOM_APP_ID = "custom_firebase_app_id"
        private const val KEY_CUSTOM_API_KEY = "custom_firebase_api_key"

        private const val DEFAULT_WORKSPACE_ID = "tariq_jaddah_optical_clean_v7"
        private const val CUSTOM_FIREBASE_APP_NAME = "TariqJaddahOpticalCustomFirebase"

        private const val COLLECTION_DEALERS = "dealers"
        private const val SUB_PRODUCTS = "products"
        private const val SUB_CATEGORY_THRESHOLDS = "category_thresholds"
        private const val SUB_RETAILER_ORDERS = "retailer_orders"
        private const val SUB_RESTOCK_ORDERS = "restock_orders"
        private const val SUB_SHOP_PROFILES = "shop_profiles"
        private const val SUB_LEDGER_ENTRIES = "ledger_entries"
        private const val SUB_INTERACTION_LOGS = "interaction_logs"
    }
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { exc ->
        if (cont.isActive) cont.resumeWithException(exc)
    }
}

// --- Firestore Serialization / Deserialization Mappers ---

private fun OpticalProduct.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "sku" to sku,
    "barcode" to barcode,
    "name" to name,
    "brand" to brand,
    "category" to category.name,
    "wholesalePrice" to wholesalePrice,
    "unitCostPkr" to unitCostPkr,
    "retailMsrp" to retailMsrp,
    "minOrderQty" to minOrderQty,
    "currentStock" to currentStock,
    "lowStockThreshold" to lowStockThreshold,
    "primarySpec" to primarySpec,
    "secondarySpec" to secondarySpec,
    "powerOrSizeRange" to powerOrSizeRange,
    "supplierName" to supplierName,
    "leadTimeDays" to leadTimeDays,
    "updatedAtEpochMs" to System.currentTimeMillis()
)

private fun DocumentSnapshot.toOpticalProductOrNull(): OpticalProduct? = runCatching {
    val skuStr = getString("sku") ?: return null
    val catName = getString("category") ?: OpticalCategory.EYEGLASS_FRAMES.name
    val category = runCatching { OpticalCategory.valueOf(catName) }
        .getOrDefault(OpticalCategory.EYEGLASS_FRAMES)
    val wholesale = getDouble("wholesalePrice") ?: 0.0
    OpticalProduct(
        id = (getLong("id") ?: 0L).toInt(),
        sku = skuStr,
        barcode = getString("barcode") ?: "",
        name = getString("name") ?: skuStr,
        brand = getString("brand") ?: "Tariq Jaddah",
        category = category,
        wholesalePrice = wholesale,
        unitCostPkr = getDouble("unitCostPkr") ?: (wholesale * 0.65),
        retailMsrp = getDouble("retailMsrp") ?: (wholesale * 1.5),
        minOrderQty = (getLong("minOrderQty") ?: 1L).toInt(),
        currentStock = (getLong("currentStock") ?: 0L).toInt(),
        lowStockThreshold = (getLong("lowStockThreshold") ?: category.defaultThreshold.toLong()).toInt(),
        primarySpec = getString("primarySpec") ?: "",
        secondarySpec = getString("secondarySpec") ?: "",
        powerOrSizeRange = getString("powerOrSizeRange") ?: "",
        supplierName = getString("supplierName") ?: "",
        leadTimeDays = (getLong("leadTimeDays") ?: 5L).toInt()
    )
}.getOrNull()

private fun CategoryThresholdConfig.toFirestoreMap(): Map<String, Any> = mapOf(
    "category" to category.name,
    "thresholdUnits" to thresholdUnits,
    "pushAlertsEnabled" to pushAlertsEnabled,
    "updatedAtEpochMs" to updatedAtEpochMs
)

private fun DocumentSnapshot.toCategoryThresholdOrNull(): CategoryThresholdConfig? = runCatching {
    val catName = getString("category") ?: return null
    val category = OpticalCategory.valueOf(catName)
    CategoryThresholdConfig(
        category = category,
        thresholdUnits = (getLong("thresholdUnits") ?: category.defaultThreshold.toLong()).toInt(),
        pushAlertsEnabled = getBoolean("pushAlertsEnabled") ?: true,
        updatedAtEpochMs = getLong("updatedAtEpochMs") ?: System.currentTimeMillis()
    )
}.getOrNull()

private fun RetailerOrder.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "orderNumber" to orderNumber,
    "shopId" to shopId,
    "retailerShopName" to retailerShopName,
    "retailerCity" to retailerCity,
    "retailerContact" to retailerContact,
    "productId" to productId,
    "productSku" to productSku,
    "productName" to productName,
    "category" to category.name,
    "quantity" to quantity,
    "unitWholesalePrice" to unitWholesalePrice,
    "unitCostPkr" to unitCostPkr,
    "totalAmount" to totalAmount,
    "customRxAndLabNotes" to customRxAndLabNotes,
    "paymentTerms" to paymentTerms,
    "paymentMethod" to paymentMethod.name,
    "senderNumber" to senderNumber,
    "receiverNumber" to receiverNumber,
    "paidAmountPkr" to paidAmountPkr,
    "debtAmountPkr" to debtAmountPkr,
    "stage" to stage.name,
    "createdAtEpochMs" to createdAtEpochMs
)

private fun DocumentSnapshot.toRetailerOrderOrNull(): RetailerOrder? = runCatching {
    val orderNo = getString("orderNumber") ?: return null
    val cat = runCatching { OpticalCategory.valueOf(getString("category") ?: "") }
        .getOrDefault(OpticalCategory.EYEGLASS_FRAMES)
    val payMethod = runCatching { PaymentMethod.valueOf(getString("paymentMethod") ?: "") }
        .getOrDefault(PaymentMethod.CASH)
    val stage = runCatching { RetailOrderStage.valueOf(getString("stage") ?: "") }
        .getOrDefault(RetailOrderStage.RECEIVED)
    val totalAmt = getDouble("totalAmount") ?: 0.0
    RetailerOrder(
        id = (getLong("id") ?: 0L).toInt(),
        orderNumber = orderNo,
        shopId = (getLong("shopId") ?: 0L).toInt(),
        retailerShopName = getString("retailerShopName") ?: "",
        retailerCity = getString("retailerCity") ?: "",
        retailerContact = getString("retailerContact") ?: "",
        productId = (getLong("productId") ?: 0L).toInt(),
        productSku = getString("productSku") ?: "",
        productName = getString("productName") ?: "",
        category = cat,
        quantity = (getLong("quantity") ?: 1L).toInt(),
        unitWholesalePrice = getDouble("unitWholesalePrice") ?: 0.0,
        unitCostPkr = getDouble("unitCostPkr") ?: 0.0,
        totalAmount = totalAmt,
        customRxAndLabNotes = getString("customRxAndLabNotes") ?: "",
        paymentTerms = getString("paymentTerms") ?: payMethod.label,
        paymentMethod = payMethod,
        senderNumber = getString("senderNumber") ?: "",
        receiverNumber = getString("receiverNumber") ?: "",
        paidAmountPkr = getDouble("paidAmountPkr") ?: totalAmt,
        debtAmountPkr = getDouble("debtAmountPkr") ?: 0.0,
        stage = stage,
        createdAtEpochMs = getLong("createdAtEpochMs") ?: System.currentTimeMillis()
    )
}.getOrNull()

private fun RestockPurchaseOrder.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "poNumber" to poNumber,
    "productId" to productId,
    "productSku" to productSku,
    "productName" to productName,
    "category" to category.name,
    "supplierName" to supplierName,
    "orderQuantity" to orderQuantity,
    "estimatedUnitCost" to estimatedUnitCost,
    "totalCost" to totalCost,
    "manualNotes" to manualNotes,
    "status" to status.name,
    "createdAtEpochMs" to createdAtEpochMs
)

private fun DocumentSnapshot.toRestockOrderOrNull(): RestockPurchaseOrder? = runCatching {
    val poNo = getString("poNumber") ?: return null
    val cat = runCatching { OpticalCategory.valueOf(getString("category") ?: "") }
        .getOrDefault(OpticalCategory.EYEGLASS_FRAMES)
    val status = runCatching { RestockOrderStatus.valueOf(getString("status") ?: "") }
        .getOrDefault(RestockOrderStatus.ORDERED)
    RestockPurchaseOrder(
        id = (getLong("id") ?: 0L).toInt(),
        poNumber = poNo,
        productId = (getLong("productId") ?: 0L).toInt(),
        productSku = getString("productSku") ?: "",
        productName = getString("productName") ?: "",
        category = cat,
        supplierName = getString("supplierName") ?: "",
        orderQuantity = (getLong("orderQuantity") ?: 1L).toInt(),
        estimatedUnitCost = getDouble("estimatedUnitCost") ?: 0.0,
        totalCost = getDouble("totalCost") ?: 0.0,
        manualNotes = getString("manualNotes") ?: "",
        status = status,
        createdAtEpochMs = getLong("createdAtEpochMs") ?: System.currentTimeMillis()
    )
}.getOrNull()

private fun RetailerShopProfile.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "shopName" to shopName,
    "cityAndMarket" to cityAndMarket,
    "contactPerson" to contactPerson,
    "phoneNumber" to phoneNumber,
    "email" to email,
    "accountTier" to accountTier,
    "paymentTermsPreference" to paymentTermsPreference,
    "creditLimitPkr" to creditLimitPkr,
    "specialNotesAndPreferences" to specialNotesAndPreferences,
    "createdAtEpochMs" to createdAtEpochMs
)

private fun DocumentSnapshot.toShopProfileOrNull(): RetailerShopProfile? = runCatching {
    val name = getString("shopName") ?: return null
    RetailerShopProfile(
        id = (getLong("id") ?: 0L).toInt(),
        shopName = name,
        cityAndMarket = getString("cityAndMarket") ?: "",
        contactPerson = getString("contactPerson") ?: "",
        phoneNumber = getString("phoneNumber") ?: "",
        email = getString("email") ?: "",
        accountTier = getString("accountTier") ?: "Gold Partner",
        paymentTermsPreference = getString("paymentTermsPreference") ?: "Cash",
        creditLimitPkr = getDouble("creditLimitPkr") ?: 250000.0,
        specialNotesAndPreferences = getString("specialNotesAndPreferences") ?: "",
        createdAtEpochMs = getLong("createdAtEpochMs") ?: System.currentTimeMillis()
    )
}.getOrNull()

private fun CustomerLedgerEntry.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "shopId" to shopId,
    "shopName" to shopName,
    "entryType" to entryType.name,
    "amountPkr" to amountPkr,
    "paymentMethod" to paymentMethod.name,
    "senderNumber" to senderNumber,
    "receiverNumber" to receiverNumber,
    "referenceCode" to referenceCode,
    "notes" to notes,
    "timestampEpochMs" to timestampEpochMs
)

private fun DocumentSnapshot.toLedgerEntryOrNull(): CustomerLedgerEntry? = runCatching {
    val shopName = getString("shopName") ?: return null
    val entryType = runCatching { LedgerEntryType.valueOf(getString("entryType") ?: "") }
        .getOrDefault(LedgerEntryType.DEBIT_INVOICE)
    val payMethod = runCatching { PaymentMethod.valueOf(getString("paymentMethod") ?: "") }
        .getOrDefault(PaymentMethod.CASH)
    CustomerLedgerEntry(
        id = (getLong("id") ?: 0L).toInt(),
        shopId = (getLong("shopId") ?: 0L).toInt(),
        shopName = shopName,
        entryType = entryType,
        amountPkr = getDouble("amountPkr") ?: 0.0,
        paymentMethod = payMethod,
        senderNumber = getString("senderNumber") ?: "",
        receiverNumber = getString("receiverNumber") ?: "",
        referenceCode = getString("referenceCode") ?: "",
        notes = getString("notes") ?: "",
        timestampEpochMs = getLong("timestampEpochMs") ?: System.currentTimeMillis()
    )
}.getOrNull()

private fun CustomerInteractionLog.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "shopId" to shopId,
    "shopName" to shopName,
    "interactionType" to interactionType.name,
    "summaryNotes" to summaryNotes,
    "nextActionNote" to nextActionNote,
    "timestampEpochMs" to timestampEpochMs
)

private fun DocumentSnapshot.toInteractionLogOrNull(): CustomerInteractionLog? = runCatching {
    val shopName = getString("shopName") ?: return null
    val type = runCatching { InteractionType.valueOf(getString("interactionType") ?: "") }
        .getOrDefault(InteractionType.PHONE_CALL)
    CustomerInteractionLog(
        id = (getLong("id") ?: 0L).toInt(),
        shopId = (getLong("shopId") ?: 0L).toInt(),
        shopName = shopName,
        interactionType = type,
        summaryNotes = getString("summaryNotes") ?: "",
        nextActionNote = getString("nextActionNote") ?: "",
        timestampEpochMs = getLong("timestampEpochMs") ?: System.currentTimeMillis()
    )
}.getOrNull()
