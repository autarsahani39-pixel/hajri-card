package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Attendance
import com.example.data.model.AttendanceStatus
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MusterTableView(
    yearMonth: YearMonth,
    attendances: List<Attendance>,
    onUpdateStatus: (dateStr: String, status: AttendanceStatus) -> Unit,
    onOpenAdvanceDialog: (dateStr: String, currentAdvance: Double, remark: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val daysInMonth = yearMonth.lengthOfMonth()
    val today = LocalDate.now()
    val isCurrentMonthAndYear = today.year == yearMonth.year && today.monthValue == yearMonth.monthValue
    val attendanceMap = remember(attendances) { attendances.associateBy { it.date } }

    var selectedDayForStatusDialog by remember { mutableStateOf<String?>(null) }
    var currentStatusForDialog by remember { mutableStateOf<AttendanceStatus?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("muster_table_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Table Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Date",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.width(72.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Status",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1.3f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Advance",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1.1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Sign / Remark",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }

            // Table Body: 1 to 31 rows (or days in month)
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                for (day in 1..daysInMonth) {
                    val dateObj = LocalDate.of(yearMonth.year, yearMonth.month, day)
                    val dateStr = String.format(Locale.ENGLISH, "%04d-%02d-%02d", yearMonth.year, yearMonth.monthValue, day)
                    val att = attendanceMap[dateStr]
                    val isToday = isCurrentMonthAndYear && today.dayOfMonth == day
                    val dayOfWeek = dateObj.dayOfWeek.value
                    val isSunday = dayOfWeek == 7

                    MusterTableRow(
                        day = day,
                        dateObj = dateObj,
                        dateStr = dateStr,
                        attendance = att,
                        isToday = isToday,
                        isSunday = isSunday,
                        onStatusClick = {
                            selectedDayForStatusDialog = dateStr
                            currentStatusForDialog = att?.status ?: AttendanceStatus.PRESENT
                        },
                        onAdvanceClick = {
                            onOpenAdvanceDialog(
                                dateStr,
                                att?.advance ?: 0.0,
                                att?.remark ?: ""
                            )
                        }
                    )

                    if (day < daysInMonth) {
                        HorizontalDivider(
                            color = if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            thickness = if (isToday) 1.5.dp else 0.8.dp
                        )
                    }
                }
            }
        }
    }

    if (selectedDayForStatusDialog != null) {
        StatusSelectionDialog(
            currentStatus = currentStatusForDialog,
            onDismiss = { selectedDayForStatusDialog = null },
            onSelectStatus = { newStatus ->
                selectedDayForStatusDialog?.let { dStr ->
                    onUpdateStatus(dStr, newStatus)
                }
                selectedDayForStatusDialog = null
            }
        )
    }
}

@Composable
private fun MusterTableRow(
    day: Int,
    dateObj: LocalDate,
    dateStr: String,
    attendance: Attendance?,
    isToday: Boolean,
    isSunday: Boolean,
    onStatusClick: () -> Unit,
    onAdvanceClick: () -> Unit
) {
    val dayNameShort = dateObj.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

    val rowBgColor = when {
        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        isSunday -> Color(0xFFFFF1F2) // Light red/pink for Sunday
        day % 2 == 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        else -> MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBgColor)
            .then(
                if (isToday) Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(0.dp)
                ) else Modifier
            )
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date Column
        Column(
            modifier = Modifier.width(72.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format(Locale.ENGLISH, "%02d", day),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isSunday) AbsentColor else if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = dayNameShort,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSunday) AbsentColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isToday) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "Today",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }

        // Status Button Column
        Box(
            modifier = Modifier
                .weight(1.3f)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val status = attendance?.status
            Surface(
                onClick = onStatusClick,
                shape = RoundedCornerShape(8.dp),
                color = when (status) {
                    AttendanceStatus.PRESENT -> PresentBgColor
                    AttendanceStatus.ABSENT -> AbsentBgColor
                    AttendanceStatus.HALF_DAY -> HalfDayBgColor
                    AttendanceStatus.LEAVE -> LeaveBgColor
                    AttendanceStatus.DOUBLE_PRESENT -> DoublePresentBgColor
                    null -> Color(0xFFF1F5F9)
                },
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        when (status) {
                            AttendanceStatus.PRESENT -> PresentColor.copy(alpha = 0.5f)
                            AttendanceStatus.ABSENT -> AbsentColor.copy(alpha = 0.5f)
                            AttendanceStatus.HALF_DAY -> HalfDayColor.copy(alpha = 0.5f)
                            AttendanceStatus.LEAVE -> LeaveColor.copy(alpha = 0.5f)
                            AttendanceStatus.DOUBLE_PRESENT -> DoublePresentColor.copy(alpha = 0.5f)
                            null -> Color(0xFFCBD5E1)
                        }
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("status_row_$day")
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = status?.code ?: "—",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = when (status) {
                            AttendanceStatus.PRESENT -> PresentColor
                            AttendanceStatus.ABSENT -> AbsentColor
                            AttendanceStatus.HALF_DAY -> HalfDayColor
                            AttendanceStatus.LEAVE -> LeaveColor
                            AttendanceStatus.DOUBLE_PRESENT -> DoublePresentColor
                            null -> Color(0xFF64748B)
                        }
                    )
                }
            }
        }

        // Advance Amount Column
        Box(
            modifier = Modifier
                .weight(1.1f)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val advanceAmt = attendance?.advance ?: 0.0
            Surface(
                onClick = onAdvanceClick,
                shape = RoundedCornerShape(8.dp),
                color = if (advanceAmt > 0) AdvanceBgColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = if (advanceAmt > 0) CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(AdvanceColor.copy(alpha = 0.6f))
                ) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("advance_row_$day")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (advanceAmt > 0) "₹${String.format(Locale.ENGLISH, "%.0f", advanceAmt)}" else "+ ₹",
                        fontWeight = if (advanceAmt > 0) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp,
                        color = if (advanceAmt > 0) AdvanceColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Sign / Remark Column
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val remark = attendance?.remark ?: ""
            val isPresent = attendance?.status == AttendanceStatus.PRESENT
            Surface(
                onClick = onAdvanceClick,
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (remark.isNotBlank()) {
                        Text(
                            text = remark,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else if (isPresent) {
                        Text(
                            text = "✓ Sign",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PresentColor
                        )
                    } else {
                        Text(
                            text = "—",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
