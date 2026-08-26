package com.example.ui.screens

import android.app.Activity
import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AbsentColor
import com.example.ui.components.AdMobBannerView
import com.example.ui.components.AdvanceColor
import com.example.ui.components.DoublePresentColor
import com.example.ui.components.HalfDayColor
import com.example.ui.components.LeaveColor
import com.example.ui.components.MonthPickerHeader
import com.example.ui.components.PresentColor
import com.example.ui.viewmodel.HajriViewModel
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: HajriViewModel,
    onOpenMusterCard: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedYearMonth by viewModel.selectedYearMonth.collectAsStateWithLifecycle()
    val summaries by viewModel.workerSummaries.collectAsStateWithLifecycle()

    val totalWorkers = summaries.size
    val totalDaysInMonth = selectedYearMonth.lengthOfMonth()
    val maxPossibleAttendances = (totalWorkers * totalDaysInMonth).coerceAtLeast(1)

    val totalPresentDays = summaries.sumOf { it.presentDays }
    val totalHalfDays = summaries.sumOf { it.halfDays }
    val totalAbsentDays = summaries.sumOf { it.absentDays }
    val totalLeaveDays = summaries.sumOf { it.leaveDays }
    val totalDoubleDays = summaries.sumOf { it.doubleDays }

    val totalGrossSalary = summaries.sumOf { it.calculatedSalary }
    val totalAdvanceGiven = summaries.sumOf { it.totalAdvance }
    val totalBalancePayable = summaries.sumOf { it.balancePayable }

    val attendancePercentage = if (maxPossibleAttendances > 0) {
        (totalPresentDays / maxPossibleAttendances.toDouble()) * 100.0
    } else 0.0

    val monthName = selectedYearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Month Picker Header
        item {
            MonthPickerHeader(
                selectedYearMonth = selectedYearMonth,
                onPreviousMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() },
                onSelectYearMonth = { viewModel.setYearMonth(it) }
            )
        }

        // Section 1: Monthly Attendance Rate Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("report_attendance_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Attendance Rate Analysis",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PresentColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${String.format(Locale.ENGLISH, "%.1f", attendancePercentage)}%",
                                fontWeight = FontWeight.ExtraBold,
                                color = PresentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { (attendancePercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = PresentColor,
                        trackColor = Color(0xFFE2E8F0)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ReportStatItem("P (Full)", "$totalPresentDays", PresentColor)
                        ReportStatItem("½ (Half)", "$totalHalfDays", HalfDayColor)
                        ReportStatItem("P½ (1.5)", "$totalLeaveDays", LeaveColor)
                        ReportStatItem("PP (Double)", "$totalDoubleDays", DoublePresentColor)
                        ReportStatItem("A (Absent)", "$totalAbsentDays", AbsentColor)
                    }
                }
            }
        }

        // Section 2: Financial Overview Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("report_financial_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Financial Overview",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FinanceRow(
                        label = "Total Earned Wages:",
                        amount = totalGrossSalary,
                        color = Color(0xFF1E3A8A)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FinanceRow(
                        label = "Total Advance Deductions:",
                        amount = totalAdvanceGiven,
                        color = AdvanceColor
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                    FinanceRow(
                        label = "Net Balance Payable:",
                        amount = totalBalancePayable,
                        color = PresentColor,
                        isBold = true
                    )
                }
            }
        }

        // Section 3: Share Full Monthly Report
        item {
            Button(
                onClick = {
                    val sb = StringBuilder()
                    sb.appendLine("📊 *Hajri Card - Monthly Complete Report*")
                    sb.appendLine("Month: $monthName ${selectedYearMonth.year}")
                    sb.appendLine("Total Workers: $totalWorkers")
                    sb.appendLine("Average Attendance: ${String.format(Locale.ENGLISH, "%.1f", attendancePercentage)}%")
                    sb.appendLine("-------------------------------")
                    sb.appendLine("Total Gross Wages: ₹${String.format(Locale.ENGLISH, "%,.0f", totalGrossSalary)}")
                    sb.appendLine("Total Advances: ₹${String.format(Locale.ENGLISH, "%,.0f", totalAdvanceGiven)}")
                    sb.appendLine("Total Net Payable: ₹${String.format(Locale.ENGLISH, "%,.0f", totalBalancePayable)}")
                    sb.appendLine("===============================")
                    summaries.forEachIndexed { i, s ->
                        sb.appendLine("${i + 1}. ${s.worker.name} (${s.worker.job})")
                        sb.appendLine("   Attendance: ${s.presentDays} days | Wage: ₹${s.calculatedSalary.toInt()} | Adv: ₹${s.totalAdvance.toInt()} | Balance: ₹${s.balancePayable.toInt()}")
                    }

                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, sb.toString())
                        type = "text/plain"
                    }
                    val chooser = Intent.createChooser(sendIntent, "Share Monthly Report").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        com.example.data.ads.AdManager.showInterstitialAd(context, forceShow = true) {
                            try {
                                context.startActivity(chooser)
                            } catch (_: Exception) {}
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Report text copied", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA), contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("share_full_report_btn")
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Full Report", fontWeight = FontWeight.Bold)
            }
        }

        // Sponsored Banner Ad
        item {
            AdMobBannerView(
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun ReportStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FinanceRow(label: String, amount: Double, color: Color, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = if (isBold) 13.sp else 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "₹${String.format(Locale.ENGLISH, "%,.0f", amount)}",
            fontSize = if (isBold) 17.sp else 14.sp,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Bold,
            color = color
        )
    }
}
