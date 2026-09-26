package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.LedgerEntryType
import com.example.data.OpticalCategory
import com.example.data.OpticalDatabase
import com.example.data.OpticalProduct
import com.example.data.OpticalRepository
import com.example.data.PaymentMethod
import com.example.data.RetailerShopProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Tariq Jaddah Optical", appName)
  }

  @Test
  fun `firestore offline persistence caches sales records and ledger updates when internet lost`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, OpticalDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val dao = db.opticalDao()
    val repository = OpticalRepository(dao, context)

    // Verify offline persistence is enabled by default with unlimited cache
    val initialSyncState = repository.cloudSyncManager.syncState.value
    assertTrue(initialSyncState.offlinePersistenceEnabled)
    assertTrue(initialSyncState.unlimitedCacheEnabled)

    // Simulate losing internet connection
    repository.cloudSyncManager.setSimulatedOfflineMode(true)
    delay(100)

    val product = repository.saveProduct(
      OpticalProduct(
        sku = "TJO-OFFLINE-01",
        name = "Offline Test Blue-Cut Lens",
        brand = "Tariq Jaddah",
        category = OpticalCategory.OPHTHALMIC_LENSES,
        wholesalePrice = 1500.0,
        unitCostPkr = 950.0,
        retailMsrp = 2400.0,
        minOrderQty = 1,
        currentStock = 30,
        lowStockThreshold = 10,
        primarySpec = "1.56 Blue-Cut HMC",
        secondarySpec = "Anti-Glare UV420",
        powerOrSizeRange = "SPH -1.00 | CYL -0.50",
        supplierName = "Tariq Optical Lab",
        leadTimeDays = 3
      )
    )

    val customer = RetailerShopProfile(
      id = 1,
      shopName = "Al-Noor Optical House",
      cityAndMarket = "Lahore Shah Alam Market",
      contactPerson = "Haji Imran",
      phoneNumber = "03001234567",
      email = "",
      accountTier = "Gold Partner",
      paymentTermsPreference = "Cash",
      creditLimitPkr = 300000.0,
      specialNotesAndPreferences = "Offline test customer"
    )
    repository.saveShopProfile(customer)

    // 1. Create a New Order while offline
    val order = repository.createRetailerOrderAndDeductStock(
      shopId = customer.id,
      retailerShopName = customer.shopName,
      retailerCity = customer.cityAndMarket,
      retailerContact = customer.phoneNumber,
      product = product,
      quantity = 4,
      customRxAndLabNotes = "[SPH -1.00 | CYL -0.50] Offline test order",
      paymentMethod = PaymentMethod.JAZZCASH,
      senderNumber = "03001234567",
      receiverNumber = "03176858707",
      paidAmountPkr = 4000.0
    )

    // 2. Create a Counter Sale while offline
    val counterSale = repository.createCounterSaleAndDeductStock(
      customerName = "Walk-in Buyer",
      customerPhone = "03219876543",
      product = product,
      quantity = 2,
      unitSalePricePkr = 1600.0,
      discountPkr = 200.0,
      customRxAndLabNotes = "[SPH -0.75 | CYL 0.00] Counter sale offline",
      paymentMethod = PaymentMethod.CASH,
      senderNumber = "03219876543",
      receiverNumber = "03087321947",
      paidAmountPkr = 3000.0
    )

    // 3. Record a manual Customer Khata Ledger update while offline
    repository.recordCustomerLedgerEntry(
      shop = customer,
      entryType = LedgerEntryType.CREDIT_PAYMENT,
      amountPkr = 2000.0,
      paymentMethod = PaymentMethod.EASYPAISA,
      senderNumber = "03001234567",
      receiverNumber = "03087321947",
      referenceCode = "KHATA-OFFLINE-99",
      notes = "Remaining balance paid while offline"
    )

    delay(150)
    val summary = repository.cloudSyncManager.loadAndVerifyFromLocalCache()
    val offlineState = repository.cloudSyncManager.syncState.value

    assertTrue(summary.contains("2 sales orders"))
    assertEquals(2, offlineState.cachedSalesRecordsCount)
    assertTrue(offlineState.cachedLedgerEntriesCount >= 5)
    assertTrue(offlineState.pendingOfflineWritesCount > 0)
    assertTrue(dao.getAllRetailerOrdersSync().any { it.orderNumber == order.orderNumber })
    assertTrue(dao.getAllRetailerOrdersSync().any { it.orderNumber == counterSale.orderNumber })
    assertTrue(dao.getAllLedgerEntriesSync().any { it.referenceCode == "KHATA-OFFLINE-99" })

    db.close()
  }
}

