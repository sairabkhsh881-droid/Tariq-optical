package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.OpticalProduct

object OpticalNotificationHelper {
    const val CHANNEL_ID = "optivault_low_stock_alerts"
    private const val CHANNEL_NAME = "Low-Stock Threshold Push Alerts"
    private const val CHANNEL_DESC =
        "Real-time push alerts when wholesale frames, ophthalmic lenses, or contact lenses fall below configured stock thresholds"

    fun ensureChannelCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun dispatchLowStockPushNotification(
        context: Context,
        product: OpticalProduct,
        triggerReason: String
    ) {
        ensureChannelCreated(context)
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            product.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (product.currentStock == 0) {
            "Tariq Jaddah Optical • OUT OF STOCK: ${product.sku}"
        } else {
            "Tariq Jaddah Optical • Low Stock: ${product.sku} (${product.currentStock} Pcs)"
        }

        val body = "${product.name} (${product.category.displayName}) is at ${product.currentStock} Pieces " +
            "(Threshold ≤ ${product.lowStockThreshold} Pieces). $triggerReason"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(1000 + product.id, notification)
        } catch (_: SecurityException) {
            // Handled gracefully if permission is revoked
        }
    }

    fun dispatchCategoryThresholdPushNotification(
        context: Context,
        categoryName: String,
        newThreshold: Int,
        affectedCount: Int
    ) {
        ensureChannelCreated(context)
        if (!hasNotificationPermission(context)) return

        val title = "Category Threshold Alert: $categoryName"
        val body = "Threshold set to ≤ $newThreshold units. $affectedCount SKUs in $categoryName now require manual restock."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(2000 + categoryName.hashCode(), notification)
        } catch (_: SecurityException) {
            // Ignore if permission not granted
        }
    }
}
