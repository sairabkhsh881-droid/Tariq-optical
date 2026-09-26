package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import com.example.data.OpticalCategory
import com.example.data.OpticalProduct
import com.example.data.RetailerOrder
import com.example.ui.WholesaleDashboardUiState
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatShortDate
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object OpticalPrintAndExportHelper {

    /**
     * Generates a structured 13-digit EAN-13 barcode numbered by OpticalCategory:
     * - EYEGLASS_FRAMES (CAT-10): 89640010 + 4-digit category item sequence + 1 check digit
     * - OPHTHALMIC_LENSES (CAT-20): 89640020 + 4-digit category item sequence + 1 check digit
     * - CONTACT_LENSES (CAT-30): 89640030 + 4-digit category item sequence + 1 check digit
     */
    fun generateBarcodeByCategory(
        category: OpticalCategory,
        existingProducts: List<OpticalProduct>,
        customSequence: Int? = null
    ): String {
        val prefix = category.barcodePrefix // 8 digits e.g. 89640010
        val catCount = existingProducts.count { it.category == category }
        val seq = customSequence ?: ((catCount + 1) * 101 + (10..99).random()).coerceIn(1001, 9999)
        val seqStr = String.format(Locale.US, "%04d", seq % 10000)
        val first12 = (prefix + seqStr).take(12).padEnd(12, '0')
        val checkDigit = computeEan13CheckDigit(first12)
        return first12 + checkDigit
    }

    fun computeEan13CheckDigit(first12Digits: String): Int {
        val digits = first12Digits.take(12).padEnd(12, '0').map { it.digitToIntOrNull() ?: 0 }
        val sum = digits.mapIndexed { idx, d -> if (idx % 2 == 0) d else d * 3 }.sum()
        return (10 - (sum % 10)) % 10
    }

    /**
     * Renders a crisp 1D Code-128 Barcode Bitmap using ZXing MultiFormatWriter.
     */
    fun createZxingBarcodeBitmap(
        barcodeValue: String,
        widthPx: Int = 560,
        heightPx: Int = 130
    ): Bitmap {
        val clean = barcodeValue.trim().ifEmpty { "8964001009012" }
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        try {
            val bitMatrix = MultiFormatWriter().encode(
                clean,
                BarcodeFormat.CODE_128,
                widthPx,
                heightPx
            )
            val pixels = IntArray(widthPx * heightPx)
            for (y in 0 until heightPx) {
                val offset = y * widthPx
                for (x in 0 until widthPx) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.rgb(15, 23, 42) else Color.WHITE
                }
            }
            bitmap.setPixels(pixels, 0, widthPx, 0, 0, widthPx, heightPx)
        } catch (_: Exception) {
        }
        return bitmap
    }

    /**
     * Renders a single printable product barcode sticker label as a high-res Bitmap.
     */
    fun createSingleProductBarcodeLabelBitmap(
        product: OpticalProduct,
        widthPx: Int = 720,
        heightPx: Int = 380
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(RectF(8f, 8f, widthPx - 8f, heightPx - 8f), 20f, 20f, borderPaint)

        // Top Navy Banner
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 37, 68)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(14f, 14f, widthPx - 14f, 76f), 14f, 14f, headerPaint)

        val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("TARIQ JADDAH OPTICAL • طارق جدہ آپٹیکل", 28f, 52f, headerTitlePaint)

        // Category Number Badge on right of header
        val catBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(13, 148, 136)
            style = Paint.Style.FILL
        }
        val catRect = RectF(widthPx - 190f, 22f, widthPx - 24f, 68f)
        canvas.drawRoundRect(catRect, 10f, 10f, catBadgePaint)

        val catTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 19f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            product.category.categoryNumberCode,
            catRect.centerX(),
            catRect.centerY() + 7f,
            catTextPaint
        )

        // Product SKU, Category Name & Product Title
        val skuPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 64, 175)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            "${product.sku}  •  ${product.category.displayName} (${product.category.categoryNumberCode})",
            28f,
            112f,
            skuPaint
        )

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val trimmedName = if (product.name.length > 38) product.name.take(36) + "…" else product.name
        canvas.drawText(trimmedName, 28f, 146f, namePaint)

        val pricePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(5, 150, 105)
            textSize = 23f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            "Wholesale: ${formatCurrency(product.wholesalePrice)} / Piece (پیس)",
            28f,
            180f,
            pricePaint
        )

        // Draw 1D ZXing Barcode
        val codeStr = product.barcode.ifBlank { product.sku }
        val barcodeBmp = createZxingBarcodeBitmap(codeStr, widthPx - 96, 120)
        canvas.drawBitmap(barcodeBmp, 48f, 198f, null)

        // Draw human-readable Barcode Number with Category Prefix highlight
        val codeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 26f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.16f
        }
        canvas.drawText(codeStr, widthPx / 2f, 352f, codeTextPaint)

        return bitmap
    }

    /**
     * Renders a multi-label printable barcode sheet Bitmap (e.g. 1, 4, 8, or 12 labels on an A4 sheet).
     */
    fun createBarcodeSheetBitmap(
        productsToPrint: List<OpticalProduct>,
        copiesPerProduct: Int = 1
    ): Bitmap {
        val expandedLabels = productsToPrint.flatMap { prod ->
            List(copiesPerProduct.coerceIn(1, 24)) { prod }
        }.ifEmpty { return Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888) }

        val columns = if (expandedLabels.size == 1) 1 else 2
        val rows = ((expandedLabels.size + columns - 1) / columns).coerceAtLeast(1)

        val cellW = 720
        val cellH = 380
        val pad = 28
        val headerH = 110
        val sheetW = columns * cellW + (columns + 1) * pad
        val sheetH = headerH + rows * cellH + (rows + 1) * pad

        val sheetBitmap = Bitmap.createBitmap(sheetW, sheetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheetBitmap)
        canvas.drawColor(Color.WHITE)

        // Sheet Header
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 37, 68)
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            "TARIQ JADDAH OPTICAL — PRINTABLE PRODUCT BARCODE SHEET (PER PIECE)",
            pad.toFloat(),
            52f,
            titlePaint
        )

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 22f
        }
        val dateStamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
        canvas.drawText(
            "Category Numbering: CAT-10 (89640010•Frames) | CAT-20 (89640020•Ophthalmic) | CAT-30 (89640030•Contacts) • $dateStamp",
            pad.toFloat(),
            88f,
            subPaint
        )

        expandedLabels.forEachIndexed { idx, product ->
            val col = idx % columns
            val row = idx / columns
            val left = pad + col * (cellW + pad)
            val top = headerH + pad + row * (cellH + pad)
            val labelBmp = createSingleProductBarcodeLabelBitmap(product, cellW, cellH)
            canvas.drawBitmap(labelBmp, left.toFloat(), top.toFloat(), null)
        }

        return sheetBitmap
    }

    /**
     * Writes printable Barcode Labels to a PDF OutputStream.
     */
    fun writeBarcodeLabelsPdf(
        productsToPrint: List<OpticalProduct>,
        copiesPerProduct: Int,
        outputStream: OutputStream
    ) {
        val sheetBmp = createBarcodeSheetBitmap(productsToPrint, copiesPerProduct)
        val pdfDocument = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(sheetBmp.width, sheetBmp.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            page.canvas.drawBitmap(sheetBmp, 0f, 0f, null)
            pdfDocument.finishPage(page)
            pdfDocument.writeTo(outputStream)
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Sends Product Barcode Labels directly to Android's native PrintManager.
     */
    fun printBarcodeLabels(
        context: Context,
        jobName: String,
        productsToPrint: List<OpticalProduct>,
        copiesPerProduct: Int
    ) {
        val sheetBmp = createBarcodeSheetBitmap(productsToPrint, copiesPerProduct)
        printBitmapViaSystemPrintManager(context, jobName, sheetBmp)
    }

    /**
     * Renders a crisp Wholesale Order Receipt 🧾 (Invoice) as a high-res Bitmap for PDF, JPG, and Thermal/A4 Printing.
     */
    fun createOrderReceiptBitmap(order: RetailerOrder): Bitmap {
        val width = 920
        val height = 1510
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        // Outer Receipt Frame
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 37, 68)
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        canvas.drawRoundRect(RectF(20f, 20f, width - 20f, height - 20f), 24f, 24f, framePaint)

        // Top Header Block
        val topBannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 37, 68)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(32f, 32f, width - 32f, 246f), 18f, 18f, topBannerPaint)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("TARIQ JADDAH OPTICAL", width / 2f, 86f, brandPaint)

        val urduBrandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(125, 211, 252)
            textSize = 29f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("طارق جدہ آپٹیکل ہول سیل ڈیلر — آفیشل رسید 🧾", width / 2f, 130f, urduBrandPaint)

        val contactBannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(253, 224, 71)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "03176858707 Tariq Mehmood   •   03087321947 Tariq Mehmood",
            width / 2f,
            174f,
            contactBannerPaint
        )

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "Wholesale Eyeglass Frames, Ophthalmic & Contact Lenses • All Items Sold Per Piece (PKR)",
            width / 2f,
            216f,
            subtitlePaint
        )

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val rightValPaint = Paint(valuePaint).apply {
            textAlign = Paint.Align.RIGHT
        }
        val dashedLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            style = Paint.Style.STROKE
            strokeWidth = 3f
            pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
        }

        // Receipt Metadata Section
        var y = 294f
        canvas.drawText("RECEIPT / INVOICE NO:", 56f, y, labelPaint)
        canvas.drawText(order.orderNumber, width - 56f, y, rightValPaint)

        y += 42f
        canvas.drawText("DATE & TIME:", 56f, y, labelPaint)
        canvas.drawText(formatShortDate(order.createdAtEpochMs), width - 56f, y, rightValPaint)

        y += 42f
        canvas.drawText("ORDER PIPELINE STAGE:", 56f, y, labelPaint)
        canvas.drawText("${order.stage.label} (${order.stage.urduLabel})", width - 56f, y, rightValPaint)

        y += 30f
        canvas.drawLine(56f, y, width - 56f, y, dashedLinePaint)

        // Retailer Shop / Customer Details
        y += 44f
        val sectionHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(13, 148, 136)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("CUSTOMER / RETAILER SHOP DETAILS (گاہک کی تفصیل)", 56f, y, sectionHeaderPaint)

        y += 40f
        canvas.drawText("Shop Name:", 56f, y, labelPaint)
        canvas.drawText(order.retailerShopName, width - 56f, y, rightValPaint)

        y += 38f
        canvas.drawText("City & Market:", 56f, y, labelPaint)
        canvas.drawText(order.retailerCity, width - 56f, y, rightValPaint)

        y += 38f
        canvas.drawText("Buyer Contact:", 56f, y, labelPaint)
        canvas.drawText(order.retailerContact, width - 56f, y, rightValPaint)

        y += 30f
        canvas.drawLine(56f, y, width - 56f, y, dashedLinePaint)

        // Itemized Product Table (Sold Strictly Per Piece)
        y += 44f
        canvas.drawText("ITEMIZED OPTICAL SALE (فروخت شدہ مال — فی پیس)", 56f, y, sectionHeaderPaint)

        y += 24f
        val itemBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(52f, y, width - 52f, y + 210f), 16f, 16f, itemBoxPaint)

        val itemTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("${order.productSku} — ${order.productName}", 72f, y + 46f, itemTitlePaint)

        val itemSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 21f
        }
        canvas.drawText(
            "Category: ${order.category.displayName} (${order.category.categoryNumberCode}) • Unit: Sold Per Piece (پیس)",
            72f,
            y + 84f,
            itemSubPaint
        )

        val rxPreview = if (order.customRxAndLabNotes.length > 62) {
            order.customRxAndLabNotes.take(60) + "…"
        } else {
            order.customRxAndLabNotes
        }
        canvas.drawText("SPH/CYL & Rx Spec: $rxPreview", 72f, y + 122f, itemSubPaint)

        val qtyPricePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 64, 175)
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            "${order.quantity} Pieces (پیس)  ×  ${formatCurrency(order.unitWholesalePrice)} / Piece",
            72f,
            y + 174f,
            qtyPricePaint
        )

        val lineTotalPaint = Paint(qtyPricePaint).apply {
            textAlign = Paint.Align.RIGHT
            textSize = 28f
        }
        canvas.drawText(formatCurrency(order.totalAmount), width - 72f, y + 174f, lineTotalPaint)

        y += 240f
        canvas.drawLine(56f, y, width - 56f, y, dashedLinePaint)

        // Payment Method, Sender & Receiver Numbers, and Khata Balance
        y += 44f
        canvas.drawText("PAYMENT & KHATA LEDGER SUMMARY (ادائیگی اور کھاتہ تفصیل)", 56f, y, sectionHeaderPaint)

        y += 42f
        canvas.drawText("Payment Method:", 56f, y, labelPaint)
        canvas.drawText("${order.paymentMethod.label} (${order.paymentMethod.urduLabel})", width - 56f, y, rightValPaint)

        y += 38f
        canvas.drawText("Sender Number (Customer):", 56f, y, labelPaint)
        canvas.drawText(order.senderNumber.ifBlank { "N/A" }, width - 56f, y, rightValPaint)

        y += 38f
        canvas.drawText("Receiver Number (Tariq Jaddah):", 56f, y, labelPaint)
        canvas.drawText(order.receiverNumber.ifBlank { "03176858707 Tariq Mehmood" }, width - 56f, y, rightValPaint)

        y += 38f
        canvas.drawText("Dealer Receipt / Khata No 1:", 56f, y, labelPaint)
        canvas.drawText("03176858707 Tariq Mehmood", width - 56f, y, rightValPaint)

        y += 38f
        canvas.drawText("Dealer Receipt / Khata No 2:", 56f, y, labelPaint)
        canvas.drawText("03087321947 Tariq Mehmood", width - 56f, y, rightValPaint)

        y += 46f
        val totalBannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(239, 246, 255)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(52f, y - 30f, width - 52f, y + 125f), 14f, 14f, totalBannerPaint)

        val grandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 37, 68)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val grandRightPaint = Paint(grandPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("TOTAL INVOICE BILL (PKR):", 72f, y + 8f, grandPaint)
        canvas.drawText(formatCurrency(order.totalAmount), width - 72f, y + 8f, grandRightPaint)

        val paidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(5, 150, 105)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paidRightPaint = Paint(paidPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("AMOUNT PAID NOW:", 72f, y + 52f, paidPaint)
        canvas.drawText(formatCurrency(order.paidAmountPkr), width - 72f, y + 52f, paidRightPaint)

        val debtColor = if (order.debtAmountPkr > 0) Color.rgb(220, 38, 38) else Color.rgb(5, 150, 105)
        val debtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = debtColor
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val debtRightPaint = Paint(debtPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("BALANCE ON CUSTOMER KHATA (UDHAAR):", 72f, y + 96f, debtPaint)
        canvas.drawText(formatCurrency(order.debtAmountPkr), width - 72f, y + 96f, debtRightPaint)

        // Bottom Scannable Receipt Barcode & Stamp
        y += 150f
        val receiptBarcodeBmp = createZxingBarcodeBitmap(order.orderNumber, 520, 90)
        canvas.drawBitmap(receiptBarcodeBmp, (width - 520) / 2f, y, null)

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 20f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            "${order.orderNumber} • 03176858707 Tariq Mehmood | 03087321947 Tariq Mehmood",
            width / 2f,
            y + 118f,
            footerPaint
        )

        return bitmap
    }

    /**
     * Writes a Wholesale Order Receipt 🧾 as a PDF file to the given OutputStream.
     */
    fun writeOrderReceiptPdf(order: RetailerOrder, outputStream: OutputStream) {
        val bmp = createOrderReceiptBitmap(order)
        val pdfDocument = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            page.canvas.drawBitmap(bmp, 0f, 0f, null)
            pdfDocument.finishPage(page)
            pdfDocument.writeTo(outputStream)
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Writes a Bitmap as a high-quality JPG image to the given OutputStream.
     */
    fun writeBitmapAsJpg(bitmap: Bitmap, outputStream: OutputStream) {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
        outputStream.flush()
    }

    /**
     * Saves a Bitmap directly to the Android device's Pictures/TariqJaddahOptical gallery as a .JPG file
     * without requiring any storage permissions on Android 10+.
     */
    fun saveBitmapToGalleryAsJpg(
        context: Context,
        bitmap: Bitmap,
        fileNameWithoutExt: String
    ): String? {
        return try {
            val cleanName = "$fileNameWithoutExt.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, cleanName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/TariqJaddahOptical"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return null
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            cleanName
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Prints a Wholesale Order Receipt 🧾 via Android's native PrintManager.
     */
    fun printOrderReceipt(context: Context, order: RetailerOrder) {
        val bmp = createOrderReceiptBitmap(order)
        printBitmapViaSystemPrintManager(
            context = context,
            jobName = "TariqJaddahOptical_Receipt_${order.orderNumber}",
            bitmap = bmp
        )
    }

    /**
     * Renders a full A4-proportioned Sales Report page as a high-resolution Bitmap (used for both PDF generation and in-app PDF preview).
     */
    fun createSalesReportPageBitmap(uiState: WholesaleDashboardUiState): Bitmap {
        val orderRowsCount = uiState.reportFilteredOrders.size.coerceAtMost(18)
        val prodRowsCount = uiState.productSalesSummaries.size.coerceAtMost(12)
        val width = 1240
        val height = (1420 + orderRowsCount * 42 + prodRowsCount * 42).coerceAtLeast(1754)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        // Top Navy Header Banner
        val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 37, 68)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(40f, 40f, width - 40f, 210f), 20f, 20f, bannerPaint)

        val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            "TARIQ JADDAH OPTICAL — WHOLESALE SALES REPORT (PDF)",
            70f,
            102f,
            headerTitlePaint
        )

        val headerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(125, 211, 252)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            "طارق جدہ آپٹیکل ہول سیل سیلز رپورٹ • 03176858707 Tariq Mehmood | 03087321947 Tariq Mehmood",
            70f,
            144f,
            headerSubPaint
        )

        val filterLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            textSize = 20f
        }
        val selectedCatName = uiState.reportCategoryFilter?.displayName ?: "All Categories"
        val selectedSkuName = uiState.allProducts.find { it.id == uiState.reportProductIdFilter }?.sku ?: "All SKUs"
        val generatedAt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
        canvas.drawText(
            "Date Filter: ${uiState.reportDateRange.label}  |  Category: $selectedCatName  |  Product: $selectedSkuName  |  Generated: $generatedAt",
            70f,
            184f,
            filterLinePaint
        )

        // 4 KPI Summary Cards Row
        var y = 240f
        val kpiBoxW = (width - 80f - 36f) / 4f
        val kpiBoxH = 126f
        val kpis = listOf(
            Triple("TOTAL REVENUE (PKR)", formatCurrency(uiState.reportTotalRevenuePkr), "${uiState.reportFilteredOrders.size} Wholesale Orders"),
            Triple("TOTAL PIECES SOLD", "${uiState.reportTotalUnitsSold} Pieces", "Sold Strictly Per Piece"),
            Triple("GROSS PROFIT (PKR)", formatCurrency(uiState.reportTotalProfitPkr), "Cost: ${formatCurrency(uiState.reportTotalCostPkr)}"),
            Triple("PROFIT MARGIN (%)", String.format(Locale.US, "%.1f%%", uiState.reportProfitMarginPercent), "Net Wholesale Markup")
        )
        kpis.forEachIndexed { idx, (label, mainVal, sub) ->
            val left = 40f + idx * (kpiBoxW + 12f)
            val cardRect = RectF(left, y, left + kpiBoxW, y + kpiBoxH)
            val boxFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(241, 245, 249)
                style = Paint.Style.FILL
            }
            val boxStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(203, 213, 225)
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
            }
            canvas.drawRoundRect(cardRect, 14f, 14f, boxFill)
            canvas.drawRoundRect(cardRect, 14f, 14f, boxStroke)

            val kpiLblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 17f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val kpiValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 37, 68)
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val kpiSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(13, 148, 136)
                textSize = 17f
            }
            canvas.drawText(label, left + 18f, y + 34f, kpiLblPaint)
            canvas.drawText(mainVal, left + 18f, y + 76f, kpiValPaint)
            canvas.drawText(sub, left + 18f, y + 106f, kpiSubPaint)
        }

        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 37, 68)
            textSize = 25f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tableHeaderBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.FILL
        }
        val tableHeaderTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val rowTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 19f
        }
        val rowBoldPaint = Paint(rowTextPaint).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 2f
        }

        // Section 1: Category Breakdown
        y += 168f
        canvas.drawText("1. SALES SUMMARY BY OPTICAL CATEGORY (PIECES & PKR)", 40f, y, sectionTitlePaint)
        y += 16f
        canvas.drawRoundRect(RectF(40f, y, width - 40f, y + 42f), 8f, 8f, tableHeaderBgPaint)
        canvas.drawText("Category (Code)", 56f, y + 28f, tableHeaderTextPaint)
        canvas.drawText("Orders", 450f, y + 28f, tableHeaderTextPaint)
        canvas.drawText("Pieces Sold", 580f, y + 28f, tableHeaderTextPaint)
        canvas.drawText("Revenue (PKR)", 740f, y + 28f, tableHeaderTextPaint)
        canvas.drawText("Gross Profit", 940f, y + 28f, tableHeaderTextPaint)
        canvas.drawText("Margin %", 1100f, y + 28f, tableHeaderTextPaint)
        y += 46f

        uiState.categorySalesSummaries.forEach { cat ->
            y += 34f
            canvas.drawText(
                "${cat.category.displayName} (${cat.category.categoryNumberCode})",
                56f,
                y,
                rowBoldPaint
            )
            canvas.drawText("${cat.orderCount}", 450f, y, rowTextPaint)
            canvas.drawText("${cat.unitsSold} Pcs", 580f, y, rowBoldPaint)
            canvas.drawText(formatCurrency(cat.revenuePkr), 740f, y, rowBoldPaint)
            canvas.drawText(formatCurrency(cat.profitPkr), 940f, y, rowTextPaint)
            canvas.drawText(String.format(Locale.US, "%.1f%%", cat.marginPercent), 1100f, y, rowBoldPaint)
            y += 12f
            canvas.drawLine(40f, y, width - 40f, y, dividerPaint)
        }

        // Section 2: Individual Product Performance
        y += 48f
        canvas.drawText("2. PERFORMANCE BY INDIVIDUAL PRODUCT SKU (SOLD PER PIECE)", 40f, y, sectionTitlePaint)
        y += 16f
        canvas.drawRoundRect(RectF(40f, y, width - 40f, y + 42f), 8f, 8f, tableHeaderBgPaint)
        canvas.drawText("SKU & Product Name", 56f, y + 28f, tableHeaderTextPaint)
        canvas.drawText("Category", 510f, y + 28f, tableHeaderTextPaint)
        canvas.drawText("Pieces Sold", 670f, y + 28f, tableHeaderTextPaint)
        canvas.drawText("Revenue (PKR)", 810f, y + 28f, tableHeaderTextPaint)
        canvas.drawText("Profit (Margin)", 1000f, y + 28f, tableHeaderTextPaint)
        y += 46f

        uiState.productSalesSummaries.take(12).forEach { prod ->
            y += 32f
            val prodTitle = "${prod.sku} • ${prod.productName}".let {
                if (it.length > 36) it.take(34) + "…" else it
            }
            canvas.drawText(prodTitle, 56f, y, rowBoldPaint)
            canvas.drawText(prod.category.categoryNumberCode, 510f, y, rowTextPaint)
            canvas.drawText("${prod.unitsSold} Pcs", 670f, y, rowBoldPaint)
            canvas.drawText(formatCurrency(prod.revenuePkr), 810f, y, rowBoldPaint)
            canvas.drawText(
                "${formatCurrency(prod.profitPkr)} (${String.format(Locale.US, "%.0f%%", prod.marginPercent)})",
                1000f,
                y,
                rowTextPaint
            )
            y += 10f
            canvas.drawLine(40f, y, width - 40f, y, dividerPaint)
        }

        // Section 3: Detailed Order Transactions
        y += 48f
        canvas.drawText("3. RETAILER ORDER TRANSACTIONS & PAYMENT METHODS", 40f, y, sectionTitlePaint)
        y += 16f
        canvas.drawRoundRect(RectF(40f, y, width - 40f, y + 42f), 8f, 8f, tableHeaderBgPaint)
        canvas.drawText("Order No & Shop", 56f, y + 28f, tableHeaderTextPaint)
        canvas.drawText("SKU (Pieces)", 420f, y + 28f, tableHeaderTextPaint)
        canvas.drawText("Payment & Sender/Receiver", 620f, y + 28f, tableHeaderTextPaint)
        canvas.drawText("Total Bill (PKR)", 1020f, y + 28f, tableHeaderTextPaint)
        y += 46f

        uiState.reportFilteredOrders.take(18).forEach { order ->
            y += 32f
            val shopText = "${order.orderNumber} • ${order.retailerShopName}".let {
                if (it.length > 30) it.take(28) + "…" else it
            }
            canvas.drawText(shopText, 56f, y, rowBoldPaint)
            canvas.drawText("${order.productSku} (${order.quantity} Pcs)", 420f, y, rowTextPaint)
            val payInfo = "${order.paymentMethod.label}: ${order.senderNumber.take(12)} → ${order.receiverNumber.take(14)}"
            canvas.drawText(payInfo, 620f, y, rowTextPaint)
            canvas.drawText(formatCurrency(order.totalAmount), 1020f, y, rowBoldPaint)
            y += 10f
            canvas.drawLine(40f, y, width - 40f, y, dividerPaint)
        }

        return bitmap
    }

    /**
     * Writes the Sales Report as a PDF document to the given OutputStream.
     */
    fun writeSalesReportPdf(uiState: WholesaleDashboardUiState, outputStream: OutputStream) {
        val reportBmp = createSalesReportPageBitmap(uiState)
        val pdfDocument = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(reportBmp.width, reportBmp.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            page.canvas.drawBitmap(reportBmp, 0f, 0f, null)
            pdfDocument.finishPage(page)
            pdfDocument.writeTo(outputStream)
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Sends the Sales Report PDF directly to Android's native PrintManager.
     */
    fun printSalesReportPdf(context: Context, uiState: WholesaleDashboardUiState) {
        val reportBmp = createSalesReportPageBitmap(uiState)
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        printBitmapViaSystemPrintManager(
            context = context,
            jobName = "TariqJaddahOptical_SalesReport_$stamp",
            bitmap = reportBmp
        )
    }

    /**
     * Helper that uses Android's PrintManager + PrintDocumentAdapter to print or save any rendered page Bitmap as a PDF.
     */
    private fun printBitmapViaSystemPrintManager(
        context: Context,
        jobName: String,
        bitmap: Bitmap
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                val pdfDoc = PdfDocument()
                try {
                    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
                    val page = pdfDoc.startPage(pageInfo)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdfDoc.finishPage(page)

                    FileOutputStream(destination.fileDescriptor).use { out ->
                        pdfDoc.writeTo(out)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.localizedMessage)
                } finally {
                    pdfDoc.close()
                }
            }
        }
        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }
}
