package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ads.AdManager
import com.example.data.model.AttendanceStatus
import com.example.data.model.Worker
import com.example.ui.components.AbsentBgColor
import com.example.ui.components.AbsentColor
import com.example.ui.components.AdvanceBgColor
import com.example.ui.components.AdvanceColor
import com.example.ui.components.AdvanceInputDialog
import com.example.ui.components.DoublePresentBgColor
import com.example.ui.components.DoublePresentColor
import com.example.ui.components.HalfDayBgColor
import com.example.ui.components.HalfDayColor
import com.example.ui.components.LeaveBgColor
import com.example.ui.components.LeaveColor
import com.example.ui.components.PresentBgColor
import com.example.ui.components.PresentColor
import com.example.ui.components.WorkerAvatar
import com.example.ui.viewmodel.HajriViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DailyAttendanceScreen(
    viewModel: HajriViewModel,
    onOpenMusterCard: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val workers by viewModel.allWorkers.collectAsStateWithLifecycle()
    val todayAttendances by viewModel.selectedDateAttendances.collectAsStateWithLifecycle()

    val dateStr = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val isToday = selectedDate == LocalDate.now()
    val dayName = selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    val dayOfWeekHindi = when (selectedDate.dayOfWeek.value) {
        1 -> "सोमवार (Mon)"
        2 -> "मंगलवार (Tue)"
        3 -> "बुधवार (Wed)"
        4 -> "गुरुवार (Thu)"
        5 -> "शुक्रवार (Fri)"
        6 -> "शनिवार (Sat)"
        else -> "रविवार (Sun)"
    }

    val attMap = remember(todayAttendances) { todayAttendances.associateBy { it.workerId } }

    var advanceDialogWorker by remember { mutableStateOf<Worker?>(null) }
    var currentAdvanceAmount by remember { mutableStateOf(0.0) }
    var currentAdvanceRemark by remember { mutableStateOf("") }

    val pCount = todayAttendances.count { it.status == AttendanceStatus.PRESENT }
    val hdCount = todayAttendances.count { it.status == AttendanceStatus.HALF_DAY }
    val aCount = todayAttendances.count { it.status == AttendanceStatus.ABSENT }
    val lCount = todayAttendances.count { it.status == AttendanceStatus.LEAVE }
    val ppCount = todayAttendances.count { it.status == AttendanceStatus.DOUBLE_PRESENT }
    val unmarkedCount = (workers.size - todayAttendances.size).coerceAtLeast(0)

    androidx.compose.runtime.LaunchedEffect(Unit) {
        AdManager.loadInterstitialAd(context)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Date Switcher Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("date_switcher_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { viewModel.setSelectedDate(selectedDate.minusDays(1)) },
                        modifier = Modifier.testTag("prev_date_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Day",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (isToday) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PresentColor
                                ) {
                                    Text(
                                        text = "आज (Today)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = dayOfWeekHindi,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(
                        onClick = { viewModel.setSelectedDate(selectedDate.plusDays(1)) },
                        modifier = Modifier.testTag("next_date_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Day",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Quick "Mark All Present" Action & Status Stats Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Button(
                        onClick = {
                            viewModel.markAllPresentToday()
                            Toast.makeText(context, "सभी कर्मचारियों की हाजिरी 'P' दर्ज की गई!", Toast.LENGTH_SHORT).show()
                            AdManager.showInterstitialAd(context, forceShow = true) {}
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PresentColor,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("mark_all_present_btn")
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "आज सभी की हाजिरी लगाएं (Mark All Present)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Daily summary counters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DailyCountBadge(label = "P", count = pCount, color = PresentColor, bgColor = PresentBgColor)
                        DailyCountBadge(label = "½", count = hdCount, color = HalfDayColor, bgColor = HalfDayBgColor)
                        DailyCountBadge(label = "P½", count = lCount, color = LeaveColor, bgColor = LeaveBgColor)
                        DailyCountBadge(label = "PP", count = ppCount, color = DoublePresentColor, bgColor = DoublePresentBgColor)
                        DailyCountBadge(label = "A", count = aCount, color = AbsentColor, bgColor = AbsentBgColor)
                    }
                }
            }
        }

        // List of workers with individual marking chips
        item {
            Text(
                text = "कर्मचारी हाजिरी सूची (${workers.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(workers, key = { it.id }) { worker ->
            val currentAtt = attMap[worker.id]
            val currentStatus = currentAtt?.status
            val advanceAmt = currentAtt?.advance ?: 0.0

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_worker_card_${worker.id}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenMusterCard(worker.id) }
                        ) {
                            WorkerAvatar(name = worker.name, size = 36)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = worker.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${if (worker.workerId.isNotBlank()) worker.workerId else "ID:${worker.id}"} • ${worker.job}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Advance button / indicator
                        Surface(
                            onClick = {
                                advanceDialogWorker = worker
                                currentAdvanceAmount = advanceAmt
                                currentAdvanceRemark = currentAtt?.remark ?: ""
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (advanceAmt > 0) AdvanceBgColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.testTag("advance_btn_${worker.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (advanceAmt > 0) "Adv: ₹${String.format(Locale.ENGLISH, "%.0f", advanceAmt)}" else "+ Adv",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (advanceAmt > 0) AdvanceColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 5 Quick Attendance Status Toggle Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AttendanceStatusButton(
                            label = "P",
                            sub = "उपस्थित",
                            isSelected = currentStatus == AttendanceStatus.PRESENT,
                            activeColor = PresentColor,
                            activeBgColor = PresentBgColor,
                            modifier = Modifier.weight(1f).testTag("btn_p_${worker.id}"),
                            onClick = {
                                viewModel.setAttendance(worker.id, dateStr, AttendanceStatus.PRESENT)
                                AdManager.showInterstitialAd(context, forceShow = false) {}
                            }
                        )

                        AttendanceStatusButton(
                            label = "A",
                            sub = "अनुपस्थित",
                            isSelected = currentStatus == AttendanceStatus.ABSENT,
                            activeColor = AbsentColor,
                            activeBgColor = AbsentBgColor,
                            modifier = Modifier.weight(1f).testTag("btn_a_${worker.id}"),
                            onClick = {
                                viewModel.setAttendance(worker.id, dateStr, AttendanceStatus.ABSENT)
                            }
                        )

                        AttendanceStatusButton(
                            label = "½",
                            sub = "आधा दिन",
                            isSelected = currentStatus == AttendanceStatus.HALF_DAY,
                            activeColor = HalfDayColor,
                            activeBgColor = HalfDayBgColor,
                            modifier = Modifier.weight(1f).testTag("btn_hd_${worker.id}"),
                            onClick = {
                                viewModel.setAttendance(worker.id, dateStr, AttendanceStatus.HALF_DAY)
                            }
                        )

                        AttendanceStatusButton(
                            label = "P½",
                            sub = "डेढ़ दिन",
                            isSelected = currentStatus == AttendanceStatus.LEAVE,
                            activeColor = LeaveColor,
                            activeBgColor = LeaveBgColor,
                            modifier = Modifier.weight(1f).testTag("btn_l_${worker.id}"),
                            onClick = {
                                viewModel.setAttendance(worker.id, dateStr, AttendanceStatus.LEAVE)
                            }
                        )

                        AttendanceStatusButton(
                            label = "PP",
                            sub = "डबल",
                            isSelected = currentStatus == AttendanceStatus.DOUBLE_PRESENT,
                            activeColor = DoublePresentColor,
                            activeBgColor = DoublePresentBgColor,
                            modifier = Modifier.weight(1f).testTag("btn_pp_${worker.id}"),
                            onClick = {
                                viewModel.setAttendance(worker.id, dateStr, AttendanceStatus.DOUBLE_PRESENT)
                            }
                        )
                    }
                }
            }
        }
    }

    if (advanceDialogWorker != null) {
        AdvanceInputDialog(
            initialAmount = currentAdvanceAmount,
            initialRemark = currentAdvanceRemark,
            dateString = dateStr,
            workerName = advanceDialogWorker!!.name,
            onDismiss = { advanceDialogWorker = null },
            onSave = { amt, remark ->
                advanceDialogWorker?.let { w ->
                    viewModel.setAdvance(w.id, dateStr, amt, remark)
                }
                advanceDialogWorker = null
                Toast.makeText(context, "एडवांस दर्ज किया गया!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun AttendanceStatusButton(
    label: String,
    sub: String,
    isSelected: Boolean,
    activeColor: Color,
    activeBgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) activeBgColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(activeColor)
        ) else null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = sub,
                fontSize = 9.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun DailyCountBadge(label: String, count: Int, color: Color, bgColor: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}
