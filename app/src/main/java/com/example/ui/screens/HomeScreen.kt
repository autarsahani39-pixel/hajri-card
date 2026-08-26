package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.AttendanceStatus
import com.example.data.model.PayType
import com.example.data.model.Worker
import com.example.ui.components.AdMobBannerView
import com.example.ui.components.MonthPickerHeader
import com.example.ui.components.PresentBgColor
import com.example.ui.components.PresentColor
import com.example.ui.components.SummaryMetricCard
import com.example.ui.components.WorkerAvatar
import com.example.ui.viewmodel.HajriViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class QuickMenuItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun HomeScreen(
    viewModel: HajriViewModel,
    onNavigateTo: (String) -> Unit,
    onSelectWorkerForMuster: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedYearMonth by viewModel.selectedYearMonth.collectAsStateWithLifecycle()
    val workers by viewModel.allWorkers.collectAsStateWithLifecycle()
    val summaries by viewModel.workerSummaries.collectAsStateWithLifecycle()
    val todayAttendances by viewModel.selectedDateAttendances.collectAsStateWithLifecycle()

    val totalWorkers = workers.size
    val todayPresent = todayAttendances.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.HALF_DAY }
    val todayAbsent = todayAttendances.count { it.status == AttendanceStatus.ABSENT }
    val totalAdvanceMonth = summaries.sumOf { it.totalAdvance }
    val totalPayableMonth = summaries.sumOf { it.balancePayable }

    val menuItems = listOf(
        QuickMenuItem("add_worker", stringResource(R.string.menu_add_worker_title), stringResource(R.string.menu_add_worker_desc), Icons.Default.GroupAdd, Color(0xFF1E3A8A)),
        QuickMenuItem("all_workers", stringResource(R.string.menu_all_workers_title), stringResource(R.string.menu_all_workers_desc), Icons.Default.Group, Color(0xFF0F766E)),
        QuickMenuItem("daily_attendance", stringResource(R.string.menu_daily_att_title), stringResource(R.string.menu_daily_att_desc), Icons.Default.CalendarMonth, Color(0xFF16A34A)),
        QuickMenuItem("muster_card", stringResource(R.string.menu_muster_card_title), stringResource(R.string.menu_muster_card_desc), Icons.Default.ReceiptLong, Color(0xFF2563EB)),
        QuickMenuItem("advance", stringResource(R.string.menu_advance_title), stringResource(R.string.menu_advance_desc), Icons.Default.CreditCard, Color(0xFFEA580C)),
        QuickMenuItem("salary", stringResource(R.string.menu_salary_title), stringResource(R.string.menu_salary_desc), Icons.Default.Payments, Color(0xFF0D9488)),
        QuickMenuItem("reports", stringResource(R.string.menu_reports_title), stringResource(R.string.menu_reports_desc), Icons.Default.Assessment, Color(0xFF9333EA)),
        QuickMenuItem("settings", stringResource(R.string.menu_settings_title), stringResource(R.string.menu_settings_desc), Icons.Default.Settings, Color(0xFF475569))
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_hero_banner"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = stringResource(R.string.app_tagline),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Action: Mark all present today
                    Button(
                        onClick = { viewModel.markAllPresentToday() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_mark_all_present_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.home_mark_all_present),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Month Picker Selector
        item {
            MonthPickerHeader(
                selectedYearMonth = selectedYearMonth,
                onPreviousMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() },
                onSelectYearMonth = { viewModel.setYearMonth(it) }
            )
        }

        // Live Dashboard Summary Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.home_monthly_overview),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryMetricCard(
                        title = stringResource(R.string.home_stat_workers),
                        value = "$totalWorkers",
                        subtitle = stringResource(R.string.home_stat_total_active),
                        icon = Icons.Default.Group,
                        color = Color(0xFF1E3A8A),
                        modifier = Modifier.weight(1f).testTag("summary_total_workers")
                    )
                    SummaryMetricCard(
                        title = stringResource(R.string.home_stat_present),
                        value = "$todayPresent",
                        subtitle = stringResource(R.string.home_stat_today),
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF16A34A),
                        modifier = Modifier.weight(1f).testTag("summary_today_present")
                    )
                    SummaryMetricCard(
                        title = stringResource(R.string.home_stat_absent),
                        value = "$todayAbsent",
                        subtitle = stringResource(R.string.home_stat_today),
                        icon = Icons.Default.Person,
                        color = Color(0xFFDC2626),
                        modifier = Modifier.weight(1f).testTag("summary_today_absent")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryMetricCard(
                        title = stringResource(R.string.home_stat_advance),
                        value = "₹${String.format(Locale.ENGLISH, "%,.0f", totalAdvanceMonth)}",
                        subtitle = stringResource(R.string.home_stat_this_month),
                        icon = Icons.Default.CreditCard,
                        color = Color(0xFFEA580C),
                        modifier = Modifier.weight(1f).testTag("summary_total_advance")
                    )
                    SummaryMetricCard(
                        title = stringResource(R.string.home_stat_net_payable),
                        value = "₹${String.format(Locale.ENGLISH, "%,.0f", totalPayableMonth)}",
                        subtitle = stringResource(R.string.home_stat_pending_balance),
                        icon = Icons.Default.AccountBalanceWallet,
                        color = Color(0xFF0F766E),
                        modifier = Modifier.weight(1f).testTag("summary_total_payable")
                    )
                }
            }
        }

        // Quick Navigation Menu Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.home_quick_menu),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 2x4 grid for navigation items
                for (row in menuItems.chunked(2)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (item in row) {
                            Surface(
                                onClick = { onNavigateTo(item.id) },
                                shape = RoundedCornerShape(16.dp),
                                color = item.color.copy(alpha = 0.08f),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(item.color.copy(alpha = 0.35f))
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("menu_btn_${item.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(item.color.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = null,
                                            tint = item.color,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = item.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.subtitle,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Worker Muster Cards Carousel
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.home_worker_muster_cards),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${stringResource(R.string.home_view_all)} (${workers.size})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onNavigateTo("all_workers") }
                    )
                }

                if (workers.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(stringResource(R.string.home_no_workers_registered), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { onNavigateTo("add_worker") }) {
                                Text(stringResource(R.string.menu_add_worker_title))
                            }
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(workers) { worker ->
                            val sum = summaries.find { it.worker.id == worker.id }
                            Surface(
                                onClick = {
                                    onSelectWorkerForMuster(worker.id)
                                    onNavigateTo("muster_card")
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = CardDefaults.outlinedCardBorder(),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .width(220.dp)
                                    .testTag("quick_worker_card_${worker.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        WorkerAvatar(name = worker.name, size = 36)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = worker.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (worker.workerId.isNotBlank()) worker.workerId else worker.job,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${stringResource(R.string.muster_present_days)}: ${sum?.presentDays ?: 0.0}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PresentColor
                                        )
                                        Text(
                                            text = "Adv: ₹${String.format(Locale.ENGLISH, "%.0f", sum?.totalAdvance ?: 0.0)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFEA580C)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${stringResource(R.string.home_stat_net_payable)}:",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "₹${String.format(Locale.ENGLISH, "%,.0f", sum?.balancePayable ?: 0.0)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
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

        // Sponsored AdMob Banner
        item {
            AdMobBannerView(
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )
        }
    }
}
