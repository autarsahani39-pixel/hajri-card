package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PayType
import com.example.ui.components.AdMobBannerView
import com.example.ui.components.AdvanceColor
import com.example.ui.components.MonthPickerHeader
import com.example.ui.components.PresentColor
import com.example.ui.components.WorkerAvatar
import com.example.ui.viewmodel.HajriViewModel
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun SalaryReportScreen(
    viewModel: HajriViewModel,
    onOpenMusterCard: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedYearMonth by viewModel.selectedYearMonth.collectAsStateWithLifecycle()
    val summaries by viewModel.workerSummaries.collectAsStateWithLifecycle()

    val totalGrossSalary = summaries.sumOf { it.calculatedSalary }
    val totalAdvance = summaries.sumOf { it.totalAdvance }
    val totalNetPayable = summaries.sumOf { it.balancePayable }

    val monthName = selectedYearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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

        // Grand Salary Summary Box
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("salary_grand_summary_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Monthly Salary Summary ($monthName ${selectedYearMonth.year})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Gross Wage:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            Text(
                                "₹${String.format(Locale.ENGLISH, "%,.0f", totalGrossSalary)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Column {
                            Text("Total Advance:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            Text(
                                "− ₹${String.format(Locale.ENGLISH, "%,.0f", totalAdvance)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD54F)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Net Payable:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            Text(
                                "₹${String.format(Locale.ENGLISH, "%,.0f", totalNetPayable)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF69F0AE)
                            )
                        }
                    }
                }
            }
        }

        // Share Text Summary Button
        item {
            Button(
                onClick = {
                    val sb = StringBuilder()
                    sb.appendLine("📋 *Hajri Card - Monthly Salary Summary*")
                    sb.appendLine("Month: $monthName ${selectedYearMonth.year}")
                    sb.appendLine("===============================")
                    summaries.forEachIndexed { i, sum ->
                        sb.appendLine("${i + 1}. ${sum.worker.name} (${sum.worker.job})")
                        sb.appendLine("   Attendance: ${sum.presentDays} days | Wage: ₹${String.format(Locale.ENGLISH, "%.0f", sum.calculatedSalary)}")
                        sb.appendLine("   Advance: ₹${String.format(Locale.ENGLISH, "%.0f", sum.totalAdvance)} | Balance Due: ₹${String.format(Locale.ENGLISH, "%.0f", sum.balancePayable)}")
                        sb.appendLine("-------------------------------")
                    }
                    sb.appendLine("Total Gross Salary: ₹${String.format(Locale.ENGLISH, "%,.0f", totalGrossSalary)}")
                    sb.appendLine("Total Advance Deductions: ₹${String.format(Locale.ENGLISH, "%,.0f", totalAdvance)}")
                    sb.appendLine("👉 Total Net Payable: ₹${String.format(Locale.ENGLISH, "%,.0f", totalNetPayable)}")

                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, sb.toString())
                        type = "text/plain"
                    }
                    val chooser = Intent.createChooser(sendIntent, "Share Salary Sheet").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(chooser)
                    } catch (_: Exception) {}
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F766E),
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().testTag("share_salary_report_btn")
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Salary Summary", fontWeight = FontWeight.Bold)
            }
        }

        // All Workers Salary Cards List
        item {
            Text(
                text = "Workers Salary List (${summaries.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(summaries, key = { it.worker.id }) { sum ->
            val worker = sum.worker
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenMusterCard(worker.id) }
                    .testTag("salary_item_${worker.id}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            WorkerAvatar(name = worker.name, size = 36)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(worker.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "${worker.job} • ${if (worker.payType == PayType.DAILY) "₹${worker.dailyWage.toInt()}/day" else "₹${worker.monthlyPay.toInt()}/mo"}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Net Payable", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "₹${String.format(Locale.ENGLISH, "%,.0f", sum.balancePayable)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = if (sum.balancePayable >= 0) PresentColor else Color(0xFFDC2626)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Attendance: ${sum.presentDays} days (${sum.halfDays} HD, ${sum.absentDays} A)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Gross: ₹${String.format(Locale.ENGLISH, "%.0f", sum.calculatedSalary)} | Adv: ₹${String.format(Locale.ENGLISH, "%.0f", sum.totalAdvance)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E3A8A)
                        )
                    }
                }
            }
        }

        // Sponsored AdMob Banner
        item {
            AdMobBannerView(
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
        }
    }
}
