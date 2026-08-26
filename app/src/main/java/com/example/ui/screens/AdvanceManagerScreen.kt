package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Worker
import com.example.ui.components.AdvanceBgColor
import com.example.ui.components.AdvanceColor
import com.example.ui.components.AdvanceInputDialog
import com.example.ui.components.MonthPickerHeader
import com.example.ui.components.WorkerAvatar
import com.example.ui.viewmodel.HajriViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AdvanceManagerScreen(
    viewModel: HajriViewModel,
    onOpenMusterCard: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedYearMonth by viewModel.selectedYearMonth.collectAsStateWithLifecycle()
    val workers by viewModel.allWorkers.collectAsStateWithLifecycle()
    val attendances by viewModel.currentMonthAttendances.collectAsStateWithLifecycle()
    val summaries by viewModel.workerSummaries.collectAsStateWithLifecycle()

    val totalMonthAdvance = summaries.sumOf { it.totalAdvance }
    val workersWithAdvance = summaries.filter { it.totalAdvance > 0 }

    val advancesList = attendances.filter { it.advance > 0 }
        .sortedByDescending { it.date }

    var selectedWorkerForNewAdvance by remember { mutableStateOf<Worker?>(null) }
    var selectedDateForNewAdvance by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))) }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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

            // Total Advance Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("advance_summary_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AdvanceBgColor),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(AdvanceColor.copy(alpha = 0.5f))
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Total Advance This Month",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdvanceColor
                            )
                            Text(
                                text = "₹ ${String.format(Locale.ENGLISH, "%,.0f", totalMonthAdvance)}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AdvanceColor
                            )
                            Text(
                                text = "${workersWithAdvance.size} workers have taken advance",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = AdvanceColor,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
            }

            // Worker-wise Advance Breakdown
            item {
                Text(
                    text = "Worker Advance Ledger",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(workers, key = { it.id }) { worker ->
                val sum = summaries.find { it.worker.id == worker.id }
                val advAmt = sum?.totalAdvance ?: 0.0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("advance_worker_card_${worker.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).clickable { onOpenMusterCard(worker.id) }
                        ) {
                            WorkerAvatar(name = worker.name, size = 38)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(worker.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "${if (worker.workerId.isNotBlank()) worker.workerId else "ID:${worker.id}"} • ${worker.job}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "₹${String.format(Locale.ENGLISH, "%,.0f", advAmt)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = if (advAmt > 0) AdvanceColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                onClick = {
                                    selectedWorkerForNewAdvance = worker
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = AdvanceColor,
                                modifier = Modifier.padding(top = 4.dp).testTag("add_advance_worker_btn_${worker.id}")
                            ) {
                                Text(
                                    text = "+ Add Advance",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Recent Advance Log Entries
            if (advancesList.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Recent Advance Transactions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(advancesList, key = { it.id }) { att ->
                    val worker = workers.find { it.id == att.workerId }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = worker?.name ?: "Worker #${att.workerId}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Date: ${att.date} • ${if (att.remark.isNotBlank()) att.remark else "Advance"}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "₹${String.format(Locale.ENGLISH, "%.0f", att.advance)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = AdvanceColor
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedWorkerForNewAdvance != null) {
        AdvanceInputDialog(
            initialAmount = 0.0,
            initialRemark = "",
            dateString = selectedDateForNewAdvance,
            workerName = selectedWorkerForNewAdvance!!.name,
            onDismiss = { selectedWorkerForNewAdvance = null },
            onSave = { amt, remark ->
                selectedWorkerForNewAdvance?.let { w ->
                    viewModel.setAdvance(w.id, selectedDateForNewAdvance, amt, remark)
                }
                selectedWorkerForNewAdvance = null
                Toast.makeText(context, "Advance recorded successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
