package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PayType {
    DAILY,
    MONTHLY
}

enum class AttendanceStatus(val code: String, val labelHi: String, val labelEn: String) {
    PRESENT("P", "Present (P)", "Present (P)"),
    ABSENT("A", "Absent (A)", "Absent (A)"),
    HALF_DAY("½", "Half Day (½)", "Half Day (½)"),
    LEAVE("P½", "1.5 Days (P½)", "1.5 Days (P½)"),
    DOUBLE_PRESENT("PP", "Double (PP)", "Double (PP)")
}

@Entity(tableName = "workers")
data class Worker(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val fatherName: String = "",
    val mobile: String = "",
    val address: String = "",
    val workerId: String = "", // e.g. "WRK-101"
    val job: String = "", // e.g. "Mistri", "Helper", "Karigar", "Tailor"
    val payType: PayType = PayType.DAILY,
    val monthlyPay: Double = 0.0,
    val dailyWage: Double = 0.0,
    val joiningDate: String = "", // "YYYY-MM-DD"
    val companyName: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attendance",
    indices = [
        androidx.room.Index(value = ["workerId", "date"], unique = true)
    ]
)
data class Attendance(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workerId: Long,
    val date: String, // "YYYY-MM-DD"
    val status: AttendanceStatus = AttendanceStatus.PRESENT,
    val advance: Double = 0.0,
    val remark: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "signatures",
    indices = [
        androidx.room.Index(value = ["workerId", "month"], unique = true)
    ]
)
data class SignatureRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workerId: Long,
    val month: String, // "YYYY-MM"
    val signatureSvg: String = "", // Serialized path points JSON or SVG
    val updatedAt: Long = System.currentTimeMillis()
)

data class WorkerMonthSummary(
    val worker: Worker,
    val presentDays: Double,
    val halfDays: Int,
    val absentDays: Int,
    val leaveDays: Int,
    val doubleDays: Int = 0,
    val totalAdvance: Double,
    val calculatedSalary: Double,
    val balancePayable: Double
)
