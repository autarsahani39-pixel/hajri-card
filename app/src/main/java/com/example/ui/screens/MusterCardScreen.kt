package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ads.AdManager
import com.example.data.model.AttendanceStatus
import com.example.data.model.PayType
import com.example.ui.components.AbsentColor
import com.example.ui.components.AdMobBannerView
import com.example.ui.components.AdvanceColor
import com.example.ui.components.AdvanceInputDialog
import com.example.ui.components.DoublePresentColor
import com.example.ui.components.HalfDayColor
import com.example.ui.components.LeaveColor
import com.example.ui.components.MonthPickerHeader
import com.example.ui.components.MusterTableView
import com.example.ui.components.PresentColor
import com.example.ui.components.SignaturePadSection
import com.example.ui.components.WorkerAvatar
import com.example.ui.pdf.MusterPdfGenerator
import com.example.ui.viewmodel.HajriViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MusterCardScreen(
    viewModel: HajriViewModel,
    onNavigateToWorkerEdit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedYearMonth by viewModel.selectedYearMonth.collectAsStateWithLifecycle()
    val workers by viewModel.allWorkers.collectAsStateWithLifecycle()
    val selectedWorkerId by viewModel.selectedWorkerId.collectAsStateWithLifecycle()
    val attendances by viewModel.selectedWorkerAttendances.collectAsStateWithLifecycle()
    val signatureRecord by viewModel.selectedWorkerSignature.collectAsStateWithLifecycle()

    // Auto-select first worker if none selected and preload ads
    LaunchedEffect(workers, selectedWorkerId) {
        if (selectedWorkerId == null && workers.isNotEmpty()) {
            viewModel.setSelectedWorkerId(workers.first().id)
        }
        AdManager.loadInterstitialAd(context)
    }

    val currentWorker = workers.find { it.id == selectedWorkerId } ?: workers.firstOrNull()

    val coroutineScope = rememberCoroutineScope()
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var showWorkerDropdown by remember { mutableStateOf(false) }

    // Dialog state for Advance input on a date
    var advanceDialogDate by remember { mutableStateOf<String?>(null) }
    var currentAdvanceAmount by remember { mutableStateOf(0.0) }
    var currentAdvanceRemark by remember { mutableStateOf("") }

    // Calculate real-time totals
    val pDays = attendances.count { it.status == AttendanceStatus.PRESENT }
    val hdDays = attendances.count { it.status == AttendanceStatus.HALF_DAY }
    val aDays = attendances.count { it.status == AttendanceStatus.ABSENT }
    val lDays = attendances.count { it.status == AttendanceStatus.LEAVE }
    val ppDays = attendances.count { it.status == AttendanceStatus.DOUBLE_PRESENT }
    val totalAdvance = attendances.sumOf { it.advance }

    val daysInMonth = selectedYearMonth.lengthOfMonth()
    val workedDays = (pDays.toDouble() * 1.0) + (hdDays.toDouble() * 0.5) + (lDays.toDouble() * 1.5) + (ppDays.toDouble() * 2.0)

    val calculatedSalary = if (currentWorker != null) {
        when (currentWorker.payType) {
            PayType.DAILY -> (pDays * currentWorker.dailyWage) + (hdDays * (currentWorker.dailyWage * 0.5)) + (lDays * (currentWorker.dailyWage * 1.5)) + (ppDays * (currentWorker.dailyWage * 2.0))
            PayType.MONTHLY -> {
                val dailyRate = if (daysInMonth > 0) currentWorker.monthlyPay / daysInMonth.toDouble() else 0.0
                workedDays * dailyRate
            }
        }
    } else 0.0

    val balancePayable = calculatedSalary - totalAdvance

    if (currentWorker == null) {
        Box(
            modifier = modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No Worker Found",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Please add a worker first to view their Muster Card.")
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Month Selector
        item {
            MonthPickerHeader(
                selectedYearMonth = selectedYearMonth,
                onPreviousMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() },
                onSelectYearMonth = { viewModel.setYearMonth(it) }
            )
        }

        // Worker Selector Dropdown Bar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("worker_selector_bar"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showWorkerDropdown = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WorkerAvatar(name = currentWorker.name, size = 36)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Selected Worker:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentWorker.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { onNavigateToWorkerEdit(currentWorker.id) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("edit_worker_shortcut_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 12.sp)
                    }

                    DropdownMenu(
                        expanded = showWorkerDropdown,
                        onDismissRequest = { showWorkerDropdown = false }
                    ) {
                        workers.forEach { w ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(w.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${if (w.workerId.isNotBlank()) w.workerId else "ID:${w.id}"} • ${w.job}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.setSelectedWorkerId(w.id)
                                    showWorkerDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Muster Card Header (Physical Card Reproduction)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("muster_card_header"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Blue Title Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E3A8A))
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "MUSTER CARD",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    Column(modifier = Modifier.padding(14.dp)) {
                        // Company & Address
                        val compName = if (currentWorker.companyName.isNotBlank()) currentWorker.companyName else "Acme Enterprises"
                        Text(
                            text = compName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (currentWorker.address.isNotBlank()) {
                            Text(
                                text = "Address: ${currentWorker.address}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        // 2-Column Info Grid
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                InfoRow("Worker Name", currentWorker.name)
                                Spacer(modifier = Modifier.height(6.dp))
                                InfoRow("Father/Husband", if (currentWorker.fatherName.isNotBlank()) currentWorker.fatherName else "—")
                                Spacer(modifier = Modifier.height(6.dp))
                                InfoRow("Designation / Role", if (currentWorker.job.isNotBlank()) currentWorker.job else "Worker")
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                val mName = selectedYearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                                InfoRow("Month & Year", "$mName ${selectedYearMonth.year}")
                                Spacer(modifier = Modifier.height(6.dp))
                                InfoRow("Worker ID", if (currentWorker.workerId.isNotBlank()) currentWorker.workerId else "WRK-${currentWorker.id}")
                                Spacer(modifier = Modifier.height(6.dp))
                                val payRateStr = if (currentWorker.payType == PayType.DAILY) {
                                    "₹${String.format(Locale.ENGLISH, "%.0f", currentWorker.dailyWage)} / Day"
                                } else {
                                    "₹${String.format(Locale.ENGLISH, "%.0f", currentWorker.monthlyPay)} / Month"
                                }
                                InfoRow("Pay Rate", payRateStr)
                            }
                        }
                    }
                }
            }
        }

        // Table Component (1 to 31 rows)
        item {
            MusterTableView(
                yearMonth = selectedYearMonth,
                attendances = attendances,
                onUpdateStatus = { dateStr, newStatus ->
                    viewModel.setAttendance(currentWorker.id, dateStr, newStatus)
                    if (newStatus == AttendanceStatus.PRESENT) {
                        AdManager.showInterstitialAd(context, forceShow = true) {}
                    }
                },
                onOpenAdvanceDialog = { dateStr, currentAdv, remark ->
                    advanceDialogDate = dateStr
                    currentAdvanceAmount = currentAdv
                    currentAdvanceRemark = remark
                }
            )
        }

        // Bottom Summary Card (Total Pay, Advance, Balance)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("muster_summary_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Salary & Balance Summary",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("P:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$pDays", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PresentColor)
                        }
                        Column {
                            Text("½:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$hdDays", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HalfDayColor)
                        }
                        Column {
                            Text("P½:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$lDays", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LeaveColor)
                        }
                        Column {
                            Text("PP:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$ppDays", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DoublePresentColor)
                        }
                        Column {
                            Text("A:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$aDays", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AbsentColor)
                        }
                        Column {
                            Text("Total:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val workedDaysFormatted = if (workedDays % 1.0 == 0.0) "${workedDays.toInt()}" else "$workedDays"
                            Text("$workedDaysFormatted days", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total Wage:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "₹${String.format(Locale.ENGLISH, "%,.0f", calculatedSalary)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = Color(0xFF1E3A8A)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total Advance:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "− ₹${String.format(Locale.ENGLISH, "%,.0f", totalAdvance)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = AdvanceColor
                            )
                        }

                        Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                            Text("Balance Payable:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "₹${String.format(Locale.ENGLISH, "%,.0f", balancePayable)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = if (balancePayable >= 0) PresentColor else Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }
        }

        // Signature / Thumb Impression Pad Section
        item {
            SignaturePadSection(
                signatureRecord = signatureRecord,
                onSaveSignature = { svgPointsJson ->
                    viewModel.saveSignature(svgPointsJson)
                    Toast.makeText(context, "Signature saved successfully!", Toast.LENGTH_SHORT).show()
                },
                onClearSignature = {
                    viewModel.clearSignature()
                    Toast.makeText(context, "Signature removed", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // PDF Muster Card Download & Share Action Buttons
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary: Download / View PDF
                Button(
                    onClick = {
                        if (isGeneratingPdf) return@Button
                        coroutineScope.launch {
                            isGeneratingPdf = true
                            try {
                                val pdfFile = withContext(Dispatchers.IO) {
                                    MusterPdfGenerator.generateAndShareMusterPdf(
                                        context = context,
                                        worker = currentWorker,
                                        yearMonth = selectedYearMonth,
                                        attendances = attendances,
                                        signatureRecord = signatureRecord,
                                        calculatedSalary = calculatedSalary,
                                        totalAdvance = totalAdvance,
                                        balancePayable = balancePayable
                                    )
                                }
                                isGeneratingPdf = false
                                if (pdfFile != null) {
                                    Toast.makeText(context, "Muster Card PDF ready!", Toast.LENGTH_SHORT).show()
                                    AdManager.showInterstitialAd(context, forceShow = true) {
                                        MusterPdfGenerator.openPdf(context, pdfFile)
                                    }
                                } else {
                                    Toast.makeText(context, "Error generating PDF. Please try again.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                isGeneratingPdf = false
                                Toast.makeText(context, "Error: ${e.localizedMessage ?: "Failed to generate PDF"}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E3A8A),
                        contentColor = Color.White
                    ),
                    enabled = !isGeneratingPdf,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("download_muster_pdf_btn")
                ) {
                    if (isGeneratingPdf) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Generating PDF...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Download / View Muster Card PDF",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Secondary: Share via WhatsApp / Other Apps
                OutlinedButton(
                    onClick = {
                        if (isGeneratingPdf) return@OutlinedButton
                        coroutineScope.launch {
                            isGeneratingPdf = true
                            try {
                                val pdfFile = withContext(Dispatchers.IO) {
                                    MusterPdfGenerator.generateAndShareMusterPdf(
                                        context = context,
                                        worker = currentWorker,
                                        yearMonth = selectedYearMonth,
                                        attendances = attendances,
                                        signatureRecord = signatureRecord,
                                        calculatedSalary = calculatedSalary,
                                        totalAdvance = totalAdvance,
                                        balancePayable = balancePayable
                                    )
                                }
                                isGeneratingPdf = false
                                if (pdfFile != null) {
                                    AdManager.showInterstitialAd(context, forceShow = true) {
                                        MusterPdfGenerator.sharePdf(context, pdfFile)
                                    }
                                } else {
                                    Toast.makeText(context, "Error generating PDF to share", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                isGeneratingPdf = false
                                Toast.makeText(context, "Error: ${e.localizedMessage ?: "Failed to share PDF"}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isGeneratingPdf,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("share_muster_pdf_btn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF16A34A))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share PDF on WhatsApp / Apps",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Sponsored Banner Ad
        item {
            AdMobBannerView(
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
        }
    }

    // Advance Input Dialog
    if (advanceDialogDate != null) {
        AdvanceInputDialog(
            initialAmount = currentAdvanceAmount,
            initialRemark = currentAdvanceRemark,
            dateString = advanceDialogDate!!,
            workerName = currentWorker.name,
            onDismiss = { advanceDialogDate = null },
            onSave = { amt, remark ->
                advanceDialogDate?.let { dStr ->
                    viewModel.setAdvance(currentWorker.id, dStr, amt, remark)
                }
                advanceDialogDate = null
                Toast.makeText(context, "Advance recorded successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
