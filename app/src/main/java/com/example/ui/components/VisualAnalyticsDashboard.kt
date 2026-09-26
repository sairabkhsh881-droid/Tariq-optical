package com.example.ui.components

import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.StackedBarChart
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.OpticalCategory
import com.example.ui.CategorySalesSummary
import com.example.ui.DealerBusinessInsight
import com.example.ui.InsightPriority
import com.example.ui.LocalIsUrdu
import com.example.ui.MonthlyRevenuePoint
import com.example.ui.WholesaleDashboardUiState
import com.example.ui.theme.CriticalStockRed
import com.example.ui.theme.LowStockAmber
import com.example.ui.theme.PrecisionTeal
import com.example.ui.theme.StockHealthyGreen
import com.example.ui.tr
import java.util.Locale
import kotlin.math.max

enum class RechartsChartStyle(
    val labelEn: String,
    val labelUr: String
) {
    AREA_LINE_COMPOSED("Area & Line Trend", "ایریا اور لائن گراف"),
    REVENUE_PROFIT_BARS("Revenue vs Profit Bars", "آمدنی بمقابلہ منافع بار"),
    STACKED_CATEGORIES("Category Stacked PKR", "کیٹیگری اسٹیکڈ گراف")
}

private val RechartsBlue = Color(0xFF2563EB)
private val RechartsTeal = Color(0xFF0D9488)
private val RechartsAmber = Color(0xFFD97706)
private val RechartsEmerald = Color(0xFF059669)
private val RechartsIndigo = Color(0xFF4F46E5)

private fun categoryChartColor(category: OpticalCategory): Color = when (category) {
    OpticalCategory.EYEGLASS_FRAMES -> RechartsBlue
    OpticalCategory.OPHTHALMIC_LENSES -> RechartsTeal
    OpticalCategory.CONTACT_LENSES -> RechartsAmber
}

private fun formatCompactPkr(amount: Double): String {
    return when {
        amount >= 1_000_000 -> String.format(Locale.US, "PKR %.2fM", amount / 1_000_000.0)
        amount >= 1_000 -> String.format(Locale.US, "PKR %.0fk", amount / 1_000.0)
        else -> String.format(Locale.US, "PKR %.0f", amount)
    }
}

/**
 * Recharts-Style Visual Summary Dashboard displaying:
 * 1. Monthly Revenue & Gross Profit Trends in PKR (Interactive Composed Area/Line, Grouped Bar, & Stacked Category Chart)
 * 2. Top-Performing Product Categories in PKR (Donut Ring Chart + Horizontal Category Comparison Bars)
 * 3. Actionable Business Insights for Tariq Jaddah Optical Dealer
 */
@Composable
fun VisualSummaryDashboardSection(
    uiState: WholesaleDashboardUiState,
    onSelectCategoryFilter: (OpticalCategory?) -> Unit,
    onJumpToLowStock: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isUrdu = LocalIsUrdu.current
    val monthlyPoints = uiState.monthlyRevenueTrends
    val categorySummaries = remember(uiState.categorySalesSummaries) {
        uiState.categorySalesSummaries.sortedByDescending { it.revenuePkr }
    }
    var selectedChartStyle by remember { mutableStateOf(RechartsChartStyle.AREA_LINE_COMPOSED) }
    var selectedMonthIndex by remember(monthlyPoints.size) {
        mutableIntStateOf((monthlyPoints.size - 1).coerceAtLeast(0))
    }
    var showInsightsExpanded by remember { mutableStateOf(true) }

    val activeMonthPoint = monthlyPoints.getOrNull(
        selectedMonthIndex.coerceIn(0, (monthlyPoints.size - 1).coerceAtLeast(0))
    )
    val sixMonthTotalRevenuePkr = remember(monthlyPoints) {
        monthlyPoints.sumOf { it.revenuePkr }
    }
    val sixMonthTotalProfitPkr = remember(monthlyPoints) {
        monthlyPoints.sumOf { it.profitPkr }
    }
    val topCategory = categorySummaries.firstOrNull()

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("visual_summary_dashboard_section")
    ) {
        // =========================================================================
        // CARD 1: RECHARTS MONTHLY REVENUE & PROFIT TREND CHART (PKR)
        // =========================================================================
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp)
                )
                .testTag("recharts_monthly_revenue_card")
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                // Header Row
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
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(RechartsBlue.copy(alpha = 0.14f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = "Monthly Revenue Trends",
                                tint = RechartsBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = tr(
                                    "Monthly Revenue & Profit Trends (PKR)",
                                    "ماہانہ آمدنی اور منافع کا رجحان (PKR)"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "6-Month Recharts Visual Summary • Tap any month bar/node to inspect",
                                    "6 ماہ کا بصری خلاصہ • تفصیل دیکھنے کے لیے کسی بھی مہینے پر کلک کریں"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = RechartsEmerald.copy(alpha = 0.14f),
                        contentColor = RechartsEmerald,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = formatCompactPkr(sixMonthTotalRevenuePkr),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Chart Mode Switcher Chips (Composed Area+Line / Grouped Bar / Stacked Category)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(RechartsChartStyle.entries) { style ->
                        val selected = selectedChartStyle == style
                        FilterChip(
                            selected = selected,
                            onClick = { selectedChartStyle = style },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (style) {
                                        RechartsChartStyle.AREA_LINE_COMPOSED -> Icons.Default.ShowChart
                                        RechartsChartStyle.REVENUE_PROFIT_BARS -> Icons.Default.BarChart
                                        RechartsChartStyle.STACKED_CATEGORIES -> Icons.Default.StackedBarChart
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = if (isUrdu) style.labelUr else style.labelEn,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("chart_style_chip_${style.name.lowercase()}")
                        )
                    }
                }

                // Recharts Interactive Canvas Area
                if (monthlyPoints.isNotEmpty()) {
                    RechartsMonthlyTrendCanvas(
                        points = monthlyPoints,
                        chartStyle = selectedChartStyle,
                        selectedIndex = selectedMonthIndex.coerceIn(0, monthlyPoints.lastIndex),
                        isUrdu = isUrdu,
                        onSelectMonth = { idx -> selectedMonthIndex = idx },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(235.dp)
                            .testTag("recharts_monthly_canvas")
                    )

                    // Month Selector Pills
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(monthlyPoints) { idx, pt ->
                            val isSelected = idx == selectedMonthIndex
                            Surface(
                                color = if (isSelected) RechartsBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .clickable { selectedMonthIndex = idx }
                                    .testTag("month_selector_pill_${pt.monthLabel.lowercase()}")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isUrdu) pt.urduMonthLabel else pt.monthLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = formatCompactPkr(pt.revenuePkr),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }

                    // Recharts Legend Row
                    RechartsLegendRow(chartStyle = selectedChartStyle)

                    // Interactive Recharts Tooltip Card for Selected Month
                    if (activeMonthPoint != null) {
                        RechartsActiveMonthTooltipCard(
                            point = activeMonthPoint,
                            sixMonthRevenueTotal = sixMonthTotalRevenuePkr,
                            sixMonthProfitTotal = sixMonthTotalProfitPkr
                        )
                    }
                }
            }
        }

        // =========================================================================
        // CARD 2: TOP-PERFORMING PRODUCT CATEGORIES IN PKR (DONUT + BARS)
        // =========================================================================
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp)
                )
                .testTag("recharts_top_categories_card")
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp)
            ) {
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
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(RechartsTeal.copy(alpha = 0.14f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = "Top Product Categories",
                                tint = RechartsTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = tr(
                                    "Top-Performing Product Categories (PKR)",
                                    "سب سے زیادہ فروخت ہونے والی پراڈکٹ کیٹیگریز (PKR)"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "Revenue share, per-piece volume & wholesale profit margins",
                                    "کیٹیگری کے لحاظ سے آمدنی کا تناسب، فروخت شدہ پیس اور منافع"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (topCategory != null && topCategory.revenuePkr > 0) {
                        Surface(
                            color = categoryChartColor(topCategory.category).copy(alpha = 0.14f),
                            contentColor = categoryChartColor(topCategory.category),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "#1 ${topCategory.category.categoryNumberCode}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Donut Chart + Category Summary Legend Side-by-Side
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RechartsCategoryDonutChart(
                        categories = categorySummaries,
                        totalRevenuePkr = uiState.reportTotalRevenuePkr,
                        modifier = Modifier
                            .size(148.dp)
                            .testTag("recharts_category_donut_chart")
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        categorySummaries.forEachIndexed { rankIdx, summary ->
                            val sharePct = if (uiState.reportTotalRevenuePkr > 0) {
                                (summary.revenuePkr / uiState.reportTotalRevenuePkr) * 100.0
                            } else 0.0
                            val catColor = categoryChartColor(summary.category)
                            val isFiltered = uiState.reportCategoryFilter == summary.category

                            Surface(
                                color = if (isFiltered) {
                                    catColor.copy(alpha = 0.15f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectCategoryFilter(
                                            if (isFiltered) null else summary.category
                                        )
                                    }
                                    .testTag("top_category_row_${summary.category.name.lowercase()}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(catColor)
                                        )
                                        Column {
                                            Text(
                                                text = "#${rankIdx + 1} ${if (isUrdu) summary.category.urduName else summary.category.displayName}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${summary.unitsSold} ${tr("Pieces", "پیس")} • ${String.format(Locale.US, "%.1f", sharePct)}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = formatCompactPkr(summary.revenuePkr),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = catColor
                                        )
                                        Text(
                                            text = "${String.format(Locale.US, "%.0f", summary.marginPercent)}% ${tr("margin", "منافع")}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StockHealthyGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Horizontal Recharts Bar Comparison for Categories in PKR
                Text(
                    text = tr(
                        "Category Revenue vs Gross Profit Comparison (PKR):",
                        "کیٹیگری آمدنی بمقابلہ خالص منافع کا موازنہ (PKR):"
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val maxCatRev = remember(categorySummaries) {
                    categorySummaries.maxOfOrNull { it.revenuePkr }?.coerceAtLeast(1.0) ?: 1.0
                }

                categorySummaries.forEach { summary ->
                    val revRatio = (summary.revenuePkr / maxCatRev).toFloat().coerceIn(0f, 1f)
                    val profRatio = (summary.profitPkr / maxCatRev).toFloat().coerceIn(0f, 1f)
                    val catColor = categoryChartColor(summary.category)

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${if (isUrdu) summary.category.urduName else summary.category.displayName} (${summary.category.categoryNumberCode})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${formatCurrency(summary.revenuePkr)}  •  ${tr("Profit", "منافع")}: ${formatCurrency(summary.profitPkr)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = catColor
                            )
                        }

                        // Dual stacked bar: Revenue bar with Profit overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(revRatio)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(catColor.copy(alpha = 0.35f))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(profRatio)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(catColor)
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // CARD 3: ACTIONABLE DEALER BUSINESS INSIGHTS (PKR)
        // =========================================================================
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp)
                )
                .testTag("actionable_business_insights_card")
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(16.dp)
            ) {
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
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(RechartsAmber.copy(alpha = 0.16f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "Actionable Business Insights",
                                tint = RechartsAmber,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = tr(
                                    "Actionable Dealer Business Insights (PKR)",
                                    "ڈیلر کے لیے قابلِ عمل کاروباری تجاویز (PKR)"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "Real-time recommendations from monthly revenue, stock & Khata ledgers",
                                    "ماہانہ فروخت، اسٹاک اور کھاتہ کی بنیاد پر کاروباری تجاویز"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = { showInsightsExpanded = !showInsightsExpanded },
                        modifier = Modifier.testTag("toggle_business_insights_btn")
                    ) {
                        Icon(
                            imageVector = if (showInsightsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Business Insights"
                        )
                    }
                }

                AnimatedVisibility(visible = showInsightsExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        uiState.dealerBusinessInsights.forEach { insight ->
                            DealerInsightItemCard(
                                insight = insight,
                                isUrdu = isUrdu,
                                onFilterCategory = { cat -> onSelectCategoryFilter(cat) },
                                onJumpToLowStock = onJumpToLowStock
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RechartsMonthlyTrendCanvas(
    points: List<MonthlyRevenuePoint>,
    chartStyle: RechartsChartStyle,
    selectedIndex: Int,
    isUrdu: Boolean,
    onSelectMonth: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxRevenue = remember(points) {
        (points.maxOfOrNull { it.revenuePkr } ?: 100_000.0).coerceAtLeast(50_000.0) * 1.15
    }
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
            .padding(top = 12.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
            .pointerInput(points.size) {
                detectTapGestures { tapOffset ->
                    val leftAxisPad = 68.dp.toPx()
                    val rightPad = 14.dp.toPx()
                    val chartW = (size.width - leftAxisPad - rightPad).coerceAtLeast(1f)
                    if (points.isNotEmpty() && tapOffset.x >= leftAxisPad - 16f) {
                        val relX = (tapOffset.x - leftAxisPad).coerceIn(0f, chartW)
                        val slotW = chartW / points.size.toFloat()
                        val tappedIdx = (relX / slotW).toInt().coerceIn(0, points.lastIndex)
                        onSelectMonth(tappedIdx)
                    }
                }
            }
    ) {
        val leftPad = 66.dp.toPx()
        val rightPad = 14.dp.toPx()
        val topPad = 16.dp.toPx()
        val bottomPad = 32.dp.toPx()

        val plotW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
        val plotH = (size.height - topPad - bottomPad).coerceAtLeast(1f)
        val baseY = topPad + plotH

        val axisPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(
                (labelTextColor.red * 255).toInt(),
                (labelTextColor.green * 255).toInt(),
                (labelTextColor.blue * 255).toInt()
            )
            textSize = 10.5.dp.toPx()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Draw 4 Horizontal Cartesian Dashed Gridlines + Y-Axis PKR Labels
        val gridSteps = 4
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        for (i in 0..gridSteps) {
            val ratio = i.toFloat() / gridSteps.toFloat()
            val y = baseY - ratio * plotH
            val valPkr = maxRevenue * ratio

            drawLine(
                color = gridColor.copy(alpha = 0.65f),
                start = Offset(leftPad, y),
                end = Offset(leftPad + plotW, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = if (i == 0) null else dashEffect
            )

            val yLabel = when {
                valPkr >= 1_000_000 -> String.format(Locale.US, "%.1fM", valPkr / 1_000_000.0)
                valPkr >= 1_000 -> String.format(Locale.US, "%.0fk", valPkr / 1_000.0)
                else -> "0"
            }
            drawContext.canvas.nativeCanvas.drawText(
                yLabel,
                6.dp.toPx(),
                y + 4.dp.toPx(),
                axisPaint
            )
        }

        if (points.isEmpty()) return@Canvas

        val slotW = plotW / points.size.toFloat()

        // Highlight active column cursor band (Recharts Tooltip Cursor)
        val activeCenterX = leftPad + slotW * (selectedIndex + 0.5f)
        drawRoundRect(
            color = RechartsBlue.copy(alpha = 0.08f),
            topLeft = Offset(leftPad + slotW * selectedIndex + 4f, topPad),
            size = Size((slotW - 8f).coerceAtLeast(8f), plotH),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )
        drawLine(
            color = RechartsBlue.copy(alpha = 0.45f),
            start = Offset(activeCenterX, topPad),
            end = Offset(activeCenterX, baseY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = dashEffect
        )

        when (chartStyle) {
            RechartsChartStyle.AREA_LINE_COMPOSED -> {
                val revOffsets = points.mapIndexed { idx, pt ->
                    val cx = leftPad + slotW * (idx + 0.5f)
                    val cy = baseY - ((pt.revenuePkr / maxRevenue).toFloat().coerceIn(0f, 1f) * plotH)
                    Offset(cx, cy)
                }
                val profOffsets = points.mapIndexed { idx, pt ->
                    val cx = leftPad + slotW * (idx + 0.5f)
                    val cy = baseY - ((pt.profitPkr / maxRevenue).toFloat().coerceIn(0f, 1f) * plotH)
                    Offset(cx, cy)
                }

                // Smooth Area & Line for Revenue (PKR)
                val linePath = Path().apply {
                    revOffsets.forEachIndexed { idx, pt ->
                        if (idx == 0) {
                            moveTo(pt.x, pt.y)
                        } else {
                            val prev = revOffsets[idx - 1]
                            val ctrlX = (prev.x + pt.x) / 2f
                            cubicTo(ctrlX, prev.y, ctrlX, pt.y, pt.x, pt.y)
                        }
                    }
                }
                val areaPath = Path().apply {
                    addPath(linePath)
                    lineTo(revOffsets.last().x, baseY)
                    lineTo(revOffsets.first().x, baseY)
                    close()
                }

                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            RechartsBlue.copy(alpha = 0.36f),
                            RechartsBlue.copy(alpha = 0.03f)
                        ),
                        startY = topPad,
                        endY = baseY
                    )
                )

                // Gross Profit smooth line
                val profitPath = Path().apply {
                    profOffsets.forEachIndexed { idx, pt ->
                        if (idx == 0) {
                            moveTo(pt.x, pt.y)
                        } else {
                            val prev = profOffsets[idx - 1]
                            val ctrlX = (prev.x + pt.x) / 2f
                            cubicTo(ctrlX, prev.y, ctrlX, pt.y, pt.x, pt.y)
                        }
                    }
                }
                val profitAreaPath = Path().apply {
                    addPath(profitPath)
                    lineTo(profOffsets.last().x, baseY)
                    lineTo(profOffsets.first().x, baseY)
                    close()
                }
                drawPath(
                    path = profitAreaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            RechartsEmerald.copy(alpha = 0.25f),
                            RechartsEmerald.copy(alpha = 0.02f)
                        ),
                        startY = topPad,
                        endY = baseY
                    )
                )

                drawPath(
                    path = linePath,
                    color = RechartsBlue,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = profitPath,
                    color = RechartsEmerald,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Draw Recharts Data Nodes
                revOffsets.forEachIndexed { idx, pt ->
                    val isSelected = idx == selectedIndex
                    drawCircle(
                        color = Color.White,
                        radius = if (isSelected) 7.dp.toPx() else 5.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = RechartsBlue,
                        radius = if (isSelected) 5.dp.toPx() else 3.5.dp.toPx(),
                        center = pt
                    )
                }
                profOffsets.forEachIndexed { idx, pt ->
                    val isSelected = idx == selectedIndex
                    drawCircle(
                        color = Color.White,
                        radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = RechartsEmerald,
                        radius = if (isSelected) 4.dp.toPx() else 2.8.dp.toPx(),
                        center = pt
                    )
                }
            }

            RechartsChartStyle.REVENUE_PROFIT_BARS -> {
                val barW = (slotW * 0.32f).coerceAtLeast(6f)
                points.forEachIndexed { idx, pt ->
                    val centerX = leftPad + slotW * (idx + 0.5f)
                    val revH = ((pt.revenuePkr / maxRevenue).toFloat().coerceIn(0f, 1f) * plotH).coerceAtLeast(4f)
                    val profH = ((pt.profitPkr / maxRevenue).toFloat().coerceIn(0f, 1f) * plotH).coerceAtLeast(4f)

                    // Revenue Bar
                    drawRoundRect(
                        color = RechartsBlue,
                        topLeft = Offset(centerX - barW - 2f, baseY - revH),
                        size = Size(barW, revH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    // Profit Bar
                    drawRoundRect(
                        color = RechartsEmerald,
                        topLeft = Offset(centerX + 2f, baseY - profH),
                        size = Size(barW, profH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }

            RechartsChartStyle.STACKED_CATEGORIES -> {
                val barW = (slotW * 0.52f).coerceAtLeast(10f)
                points.forEachIndexed { idx, pt ->
                    val centerX = leftPad + slotW * (idx + 0.5f)
                    val leftX = centerX - barW / 2f

                    val hFrames = (pt.framesRevenuePkr / maxRevenue).toFloat().coerceIn(0f, 1f) * plotH
                    val hOph = (pt.ophthalmicRevenuePkr / maxRevenue).toFloat().coerceIn(0f, 1f) * plotH
                    val hCl = (pt.contactLensesRevenuePkr / maxRevenue).toFloat().coerceIn(0f, 1f) * plotH

                    var currentBottom = baseY
                    if (hFrames > 0f) {
                        drawRoundRect(
                            color = RechartsBlue,
                            topLeft = Offset(leftX, currentBottom - hFrames),
                            size = Size(barW, hFrames),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                        currentBottom -= hFrames
                    }
                    if (hOph > 0f) {
                        drawRoundRect(
                            color = RechartsTeal,
                            topLeft = Offset(leftX, currentBottom - hOph),
                            size = Size(barW, hOph),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                        currentBottom -= hOph
                    }
                    if (hCl > 0f) {
                        drawRoundRect(
                            color = RechartsAmber,
                            topLeft = Offset(leftX, currentBottom - hCl),
                            size = Size(barW, hCl),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }
                }
            }
        }

        // X-Axis Month Labels
        val xLabelPaint = AndroidPaint(axisPaint).apply {
            textAlign = AndroidPaint.Align.CENTER
        }
        points.forEachIndexed { idx, pt ->
            val cx = leftPad + slotW * (idx + 0.5f)
            xLabelPaint.color = if (idx == selectedIndex) {
                android.graphics.Color.rgb(37, 99, 235)
            } else {
                axisPaint.color
            }
            val labelText = if (isUrdu) pt.urduMonthLabel else pt.monthLabel
            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                cx,
                baseY + 20.dp.toPx(),
                xLabelPaint
            )
        }
    }
}

@Composable
private fun RechartsLegendRow(chartStyle: RechartsChartStyle) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        when (chartStyle) {
            RechartsChartStyle.STACKED_CATEGORIES -> {
                LegendDotItem(
                    color = RechartsBlue,
                    label = tr("Frames (CAT-10)", "فریم (CAT-10)")
                )
                LegendDotItem(
                    color = RechartsTeal,
                    label = tr("Ophthalmic (CAT-20)", "نظر کے لینز (CAT-20)")
                )
                LegendDotItem(
                    color = RechartsAmber,
                    label = tr("Contact Lenses (CAT-30)", "آئی لینز (CAT-30)")
                )
            }
            else -> {
                LegendDotItem(
                    color = RechartsBlue,
                    label = tr("Wholesale Revenue (PKR)", "ہول سیل آمدنی (PKR)")
                )
                LegendDotItem(
                    color = RechartsEmerald,
                    label = tr("Gross Profit (PKR)", "خالص منافع (PKR)")
                )
            }
        }
    }
}

@Composable
private fun LegendDotItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RechartsActiveMonthTooltipCard(
    point: MonthlyRevenuePoint,
    sixMonthRevenueTotal: Double,
    sixMonthProfitTotal: Double
) {
    val isUrdu = LocalIsUrdu.current
    val isPositiveGrowth = point.momGrowthPercent >= 0.0
    val growthSign = if (isPositiveGrowth) "+" else ""

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = RechartsBlue.copy(alpha = 0.35f),
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("recharts_active_tooltip_card")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${if (isUrdu) point.urduMonthLabel else point.monthLabel} (${point.monthKey}) — ${point.ordersCount} ${tr("Orders", "آرڈرز")} • ${point.piecesSold} ${tr("Pieces", "پیس")}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    color = if (isPositiveGrowth) StockHealthyGreen.copy(alpha = 0.16f) else CriticalStockRed.copy(alpha = 0.16f),
                    contentColor = if (isPositiveGrowth) StockHealthyGreen else CriticalStockRed,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$growthSign${String.format(Locale.US, "%.1f", point.momGrowthPercent)}% MoM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = tr("MONTH REVENUE (PKR)", "ماہانہ فروخت (PKR)"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(point.revenuePkr),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = RechartsBlue
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = tr("GROSS PROFIT (PKR)", "خالص منافع (PKR)"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(point.profitPkr),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = RechartsEmerald
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = tr("6-MO TOTAL (PKR)", "6 ماہ کل آمدنی"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCompactPkr(sixMonthRevenueTotal),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Category Breakdown inside Tooltip
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${tr("Frames", "فریم")}: ${formatCompactPkr(point.framesRevenuePkr)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = RechartsBlue,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${tr("Ophthalmic", "نظر کے لینز")}: ${formatCompactPkr(point.ophthalmicRevenuePkr)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = RechartsTeal,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${tr("Contact Lenses", "آئی لینز")}: ${formatCompactPkr(point.contactLensesRevenuePkr)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = RechartsAmber,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun RechartsCategoryDonutChart(
    categories: List<CategorySalesSummary>,
    totalRevenuePkr: Double,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            val strokeW = 20.dp.toPx()
            val diameter = size.minDimension - strokeW
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            if (totalRevenuePkr <= 0.0 || categories.isEmpty()) {
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.4f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                )
            } else {
                var currentStartAngle = -90f
                categories.forEach { cat ->
                    val sweep = ((cat.revenuePkr / totalRevenuePkr) * 360.0).toFloat()
                    if (sweep > 0.5f) {
                        drawArc(
                            color = categoryChartColor(cat.category),
                            startAngle = currentStartAngle,
                            sweepAngle = (sweep - 2.5f).coerceAtLeast(1f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                        currentStartAngle += sweep
                    }
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = tr("Total PKR", "کل فروخت"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatCompactPkr(totalRevenuePkr),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${categories.sumOf { it.unitsSold }} Pcs",
                style = MaterialTheme.typography.labelSmall,
                color = PrecisionTeal,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DealerInsightItemCard(
    insight: DealerBusinessInsight,
    isUrdu: Boolean,
    onFilterCategory: (OpticalCategory?) -> Unit,
    onJumpToLowStock: () -> Unit
) {
    val accentColor = when (insight.priority) {
        InsightPriority.TOP_CATEGORY -> RechartsBlue
        InsightPriority.REVENUE_MOMENTUM -> RechartsEmerald
        InsightPriority.RESTOCK_ACTION -> LowStockAmber
        InsightPriority.KHATA_CASHFLOW -> RechartsIndigo
    }
    val icon = when (insight.priority) {
        InsightPriority.TOP_CATEGORY -> Icons.Default.EmojiEvents
        InsightPriority.REVENUE_MOMENTUM -> Icons.Default.Analytics
        InsightPriority.RESTOCK_ACTION -> Icons.Default.WarningAmber
        InsightPriority.KHATA_CASHFLOW -> Icons.Default.AccountBalanceWallet
    }

    Surface(
        color = accentColor.copy(alpha = 0.08f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accentColor.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .testTag("dealer_insight_${insight.id}")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isUrdu) insight.titleUr else insight.titleEn,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = accentColor.copy(alpha = 0.16f),
                    contentColor = accentColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isUrdu) insight.metricBadgeUr else insight.metricBadgeEn,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = if (isUrdu) insight.detailUr else insight.detailEn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (insight.priority == InsightPriority.RESTOCK_ACTION) {
                OutlinedButton(
                    onClick = onJumpToLowStock,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = tr("Open Low-Stock SKUs & POs", "کم اسٹاک پراڈکٹس دیکھیں"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (insight.targetCategory != null) {
                OutlinedButton(
                    onClick = { onFilterCategory(insight.targetCategory) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = tr(
                            "Filter Report by ${insight.targetCategory.displayName}",
                            "${insight.targetCategory.urduName} کی رپورٹ فلٹر کریں"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
