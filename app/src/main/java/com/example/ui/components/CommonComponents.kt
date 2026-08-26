package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import java.util.Locale

// Specific brand color palette as mandated:
// Green = Present, Red = Absent, Blue = Half Day, Orange = Advance / Leave
val PresentColor = Color(0xFF16A34A)
val PresentBgColor = Color(0xFFDCFCE7)

val AbsentColor = Color(0xFFDC2626)
val AbsentBgColor = Color(0xFFFEE2E2)

val HalfDayColor = Color(0xFF2563EB)
val HalfDayBgColor = Color(0xFFDBEAFE)

val LeaveColor = Color(0xFFD97706)
val LeaveBgColor = Color(0xFFFEF3C7)

val DoublePresentColor = Color(0xFF7C3AED)
val DoublePresentBgColor = Color(0xFFF3E8FF)

val AdvanceColor = Color(0xFFEA580C)
val AdvanceBgColor = Color(0xFFFFEDD5)

@Composable
fun StatusBadge(
    status: AttendanceStatus?,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false
) {
    val (bgColor, textColor, code, text) = when (status) {
        AttendanceStatus.PRESENT -> Quad(PresentBgColor, PresentColor, "P", "Present (P)")
        AttendanceStatus.ABSENT -> Quad(AbsentBgColor, AbsentColor, "A", "Absent (A)")
        AttendanceStatus.HALF_DAY -> Quad(HalfDayBgColor, HalfDayColor, "½", "Half Day (½)")
        AttendanceStatus.LEAVE -> Quad(LeaveBgColor, LeaveColor, "P½", "1.5 Days (P½)")
        AttendanceStatus.DOUBLE_PRESENT -> Quad(DoublePresentBgColor, DoublePresentColor, "PP", "Double (PP)")
        null -> Quad(Color(0xFFF1F5F9), Color(0xFF64748B), "—", "None")
    }

    Surface(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        color = bgColor,
        contentColor = textColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (showLabel) 8.dp else 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (showLabel) text else code,
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (showLabel) 12.sp else 13.sp,
                color = textColor
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun WorkerAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Int = 44
) {
    val initials = name.trim().split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.toString() }
        .joinToString("")
        .ifBlank { "W" }

    val hash = name.hashCode()
    val colorList = listOf(
        Color(0xFF1E3A8A), Color(0xFF0F766E), Color(0xFF7C2D12),
        Color(0xFF4C1D95), Color(0xFF831843), Color(0xFF14532D)
    )
    val avatarBg = colorList[kotlin.math.abs(hash) % colorList.size]

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(avatarBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.4).sp
        )
    }
}

@Composable
fun SummaryMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(color.copy(alpha = 0.25f))
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun StatusSelectionDialog(
    currentStatus: AttendanceStatus?,
    onDismiss: () -> Unit,
    onSelectStatus: (AttendanceStatus) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Attendance Status", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AttendanceOptionRow(
                    status = AttendanceStatus.PRESENT,
                    title = "Present (P)",
                    desc = "Full day regular wage",
                    color = PresentColor,
                    bgColor = PresentBgColor,
                    isSelected = currentStatus == AttendanceStatus.PRESENT,
                    onClick = { onSelectStatus(AttendanceStatus.PRESENT) }
                )
                AttendanceOptionRow(
                    status = AttendanceStatus.ABSENT,
                    title = "Absent (A)",
                    desc = "No wage (Day Off)",
                    color = AbsentColor,
                    bgColor = AbsentBgColor,
                    isSelected = currentStatus == AttendanceStatus.ABSENT,
                    onClick = { onSelectStatus(AttendanceStatus.ABSENT) }
                )
                AttendanceOptionRow(
                    status = AttendanceStatus.HALF_DAY,
                    title = "Half Day (½)",
                    desc = "Half day wage (0.5 Day)",
                    color = HalfDayColor,
                    bgColor = HalfDayBgColor,
                    isSelected = currentStatus == AttendanceStatus.HALF_DAY,
                    onClick = { onSelectStatus(AttendanceStatus.HALF_DAY) }
                )
                AttendanceOptionRow(
                    status = AttendanceStatus.LEAVE,
                    title = "1.5 Days (P½)",
                    desc = "1 full day + half day (1.5x wage)",
                    color = LeaveColor,
                    bgColor = LeaveBgColor,
                    isSelected = currentStatus == AttendanceStatus.LEAVE,
                    onClick = { onSelectStatus(AttendanceStatus.LEAVE) }
                )
                AttendanceOptionRow(
                    status = AttendanceStatus.DOUBLE_PRESENT,
                    title = "Double (PP)",
                    desc = "2 full shifts (2.0x wage)",
                    color = DoublePresentColor,
                    bgColor = DoublePresentBgColor,
                    isSelected = currentStatus == AttendanceStatus.DOUBLE_PRESENT,
                    onClick = { onSelectStatus(AttendanceStatus.DOUBLE_PRESENT) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun AttendanceOptionRow(
    status: AttendanceStatus,
    title: String,
    desc: String,
    color: Color,
    bgColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = if (isSelected) bgColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(color)
        ) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = status.code,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AdvanceInputDialog(
    initialAmount: Double,
    initialRemark: String,
    dateString: String,
    workerName: String,
    onDismiss: () -> Unit,
    onSave: (amount: Double, remark: String) -> Unit
) {
    var amountText by remember {
        mutableStateOf(if (initialAmount > 0) String.format(Locale.ENGLISH, "%.0f", initialAmount) else "")
    }
    var remarkText by remember { mutableStateOf(initialRemark) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Record Advance",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "$workerName • Date: $dateString",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { char -> char.isDigit() || char == '.' }
                        errorMsg = null
                    },
                    label = { Text("Advance Amount (₹)*") },
                    placeholder = { Text("e.g. 500, 1000") },
                    leadingIcon = {
                        Icon(Icons.Default.AttachMoney, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = errorMsg != null,
                    supportingText = {
                        if (errorMsg != null) {
                            Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("advance_amount_input")
                )

                // Quick amount chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(200, 500, 1000, 2000).forEach { quickAmt ->
                        Surface(
                            onClick = { amountText = quickAmt.toString() },
                            shape = RoundedCornerShape(8.dp),
                            color = AdvanceBgColor,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "+₹$quickAmt",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdvanceColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = remarkText,
                    onValueChange = { remarkText = it },
                    label = { Text("Remark / Purpose") },
                    placeholder = { Text("e.g. Festival, household, travel expense") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt < 0) {
                        errorMsg = "Please enter a valid amount"
                    } else {
                        onSave(amt, remarkText)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AdvanceColor),
                modifier = Modifier.testTag("save_advance_btn")
            ) {
                Text("Save Advance", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
