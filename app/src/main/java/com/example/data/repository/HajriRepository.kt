package com.example.data.repository

import com.example.data.local.HajriDao
import com.example.data.model.Attendance
import com.example.data.model.AttendanceStatus
import com.example.data.model.PayType
import com.example.data.model.SignatureRecord
import com.example.data.model.Worker
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HajriRepository(private val dao: HajriDao) {

    val allWorkers: Flow<List<Worker>> = dao.getAllActiveWorkers()
    val workerCount: Flow<Int> = dao.getActiveWorkerCount()

    fun getWorkerById(id: Long): Flow<Worker?> = dao.getWorkerFlowById(id)
    suspend fun getWorkerByIdOnce(id: Long): Worker? = dao.getWorkerById(id)

    suspend fun insertOrUpdateWorker(worker: Worker): Long {
        return if (worker.id == 0L) {
            dao.insertWorker(worker)
        } else {
            dao.updateWorker(worker)
            worker.id
        }
    }

    suspend fun deleteWorker(workerId: Long) {
        dao.deleteAttendanceForWorker(workerId)
        dao.deleteSignaturesForWorker(workerId)
        dao.deleteWorkerById(workerId)
    }

    fun getAttendanceForWorkerMonth(workerId: Long, yearMonth: String): Flow<List<Attendance>> {
        return dao.getAttendanceForWorkerMonth(workerId, yearMonth)
    }

    fun getAttendanceForDate(date: String): Flow<List<Attendance>> {
        return dao.getAttendanceForDate(date)
    }

    fun getAttendanceForMonth(yearMonth: String): Flow<List<Attendance>> {
        return dao.getAttendanceForMonth(yearMonth)
    }

    suspend fun recordAttendance(
        workerId: Long,
        date: String,
        status: AttendanceStatus,
        advance: Double = 0.0,
        remark: String = ""
    ) {
        val existing = dao.getAttendance(workerId, date)
        val toSave = if (existing != null) {
            existing.copy(
                status = status,
                advance = if (advance >= 0) advance else existing.advance,
                remark = if (remark.isNotBlank()) remark else existing.remark,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            Attendance(
                workerId = workerId,
                date = date,
                status = status,
                advance = if (advance > 0) advance else 0.0,
                remark = remark,
                updatedAt = System.currentTimeMillis()
            )
        }
        dao.insertOrUpdateAttendance(toSave)
    }

    suspend fun updateAdvance(workerId: Long, date: String, advanceAmount: Double, remark: String = "") {
        val existing = dao.getAttendance(workerId, date)
        if (existing != null) {
            dao.insertOrUpdateAttendance(
                existing.copy(
                    advance = advanceAmount,
                    remark = if (remark.isNotBlank()) remark else existing.remark,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            dao.insertOrUpdateAttendance(
                Attendance(
                    workerId = workerId,
                    date = date,
                    status = AttendanceStatus.PRESENT,
                    advance = advanceAmount,
                    remark = remark
                )
            )
        }
    }

    suspend fun markAllWorkersPresent(date: String, workers: List<Worker>) {
        val list = workers.map { worker ->
            val existing = dao.getAttendance(worker.id, date)
            existing?.copy(status = AttendanceStatus.PRESENT, updatedAt = System.currentTimeMillis())
                ?: Attendance(
                    workerId = worker.id,
                    date = date,
                    status = AttendanceStatus.PRESENT
                )
        }
        dao.insertAllAttendance(list)
    }

    fun getSignature(workerId: Long, yearMonth: String): Flow<SignatureRecord?> {
        return dao.getSignature(workerId, yearMonth)
    }

    suspend fun getSignatureOnce(workerId: Long, yearMonth: String): SignatureRecord? {
        return dao.getSignatureOnce(workerId, yearMonth)
    }

    suspend fun saveSignature(workerId: Long, yearMonth: String, signatureSvg: String) {
        val existing = dao.getSignatureOnce(workerId, yearMonth)
        val record = if (existing != null) {
            existing.copy(signatureSvg = signatureSvg, updatedAt = System.currentTimeMillis())
        } else {
            SignatureRecord(
                workerId = workerId,
                month = yearMonth,
                signatureSvg = signatureSvg
            )
        }
        dao.saveSignature(record)
    }

    suspend fun clearSignature(workerId: Long, yearMonth: String) {
        dao.deleteSignature(workerId, yearMonth)
    }

    /**
     * Cleans up any orphaned attendance & signature records.
     */
    suspend fun purgeSampleData() {
        dao.deleteOrphanedAttendance()
        dao.deleteOrphanedSignatures()
    }

    /**
     * Clears all local worker, attendance, and signature data from the database.
     */
    suspend fun clearAllData() {
        dao.deleteAllAttendance()
        dao.deleteAllSignatures()
        dao.deleteAllWorkers()
    }

    /**
     * Optional manual seeding only triggered when explicitly requested by user in settings.
     */
    suspend fun populateSampleDataIfEmpty() {
        val sampleWorkers = listOf(
            Worker(
                name = "Ram Kumar",
                fatherName = "Shyam Lal",
                mobile = "9876543210",
                address = "Plot 12, Industrial Area, Delhi",
                workerId = "WRK-001",
                job = "Master Craftsman",
                payType = PayType.DAILY,
                dailyWage = 650.0,
                monthlyPay = 18000.0,
                joiningDate = "2024-01-15",
                companyName = "Shree Ganesh Textiles & Fabrication"
            ),
            Worker(
                name = "Md. Arif",
                fatherName = "Abdul Rahim",
                mobile = "9812345678",
                address = "Street No. 4, Gandhi Nagar",
                workerId = "WRK-002",
                job = "Tailor / Artisan",
                payType = PayType.DAILY,
                dailyWage = 550.0,
                monthlyPay = 15000.0,
                joiningDate = "2024-03-01",
                companyName = "Shree Ganesh Textiles & Fabrication"
            ),
            Worker(
                name = "Suresh Yadav",
                fatherName = "Ram Avtar",
                mobile = "9765432109",
                address = "Ward 8, New Colony",
                workerId = "WRK-003",
                job = "Helper",
                payType = PayType.DAILY,
                dailyWage = 450.0,
                monthlyPay = 12000.0,
                joiningDate = "2024-05-10",
                companyName = "Shree Ganesh Textiles & Fabrication"
            ),
            Worker(
                name = "Dinesh Sharma",
                fatherName = "Om Prakash",
                mobile = "9988776655",
                address = "Sector 15, Noida",
                workerId = "WRK-004",
                job = "Supervisor",
                payType = PayType.MONTHLY,
                monthlyPay = 22000.0,
                dailyWage = 750.0,
                joiningDate = "2023-11-01",
                companyName = "Shree Ganesh Textiles & Fabrication"
            ),
            Worker(
                name = "Sonu Singh",
                fatherName = "Mahendra Singh",
                mobile = "9123456780",
                address = "Rasulpur, Dadri",
                workerId = "WRK-005",
                job = "Packer",
                payType = PayType.DAILY,
                dailyWage = 480.0,
                monthlyPay = 13000.0,
                joiningDate = "2024-06-20",
                companyName = "Shree Ganesh Textiles & Fabrication"
            )
        )

        val insertedIds = mutableListOf<Long>()
        for (w in sampleWorkers) {
            val id = dao.insertWorker(w)
            insertedIds.add(id)
        }

        val now = LocalDate.now()
        val yearMonthStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val currentDay = now.dayOfMonth

        insertedIds.forEachIndexed { index, workerId ->
            for (day in 1..currentDay) {
                val dateStr = String.format("%s-%02d", yearMonthStr, day)
                val dayOfWeek = LocalDate.of(now.year, now.month, day).dayOfWeek.value
                val status = when {
                    dayOfWeek == 7 -> AttendanceStatus.LEAVE
                    (day + index) % 11 == 0 -> AttendanceStatus.ABSENT
                    (day + index) % 7 == 0 -> AttendanceStatus.HALF_DAY
                    else -> AttendanceStatus.PRESENT
                }
                val advance = when {
                    day == 3 && index == 0 -> 1000.0
                    day == 10 && index == 0 -> 500.0
                    day == 5 && index == 1 -> 1500.0
                    day == 12 && index == 2 -> 800.0
                    day == 7 && index == 3 -> 2000.0
                    else -> 0.0
                }
                val remark = if (advance > 0) "Advance" else ""
                dao.insertOrUpdateAttendance(
                    Attendance(
                        workerId = workerId,
                        date = dateStr,
                        status = status,
                        advance = advance,
                        remark = remark
                    )
                )
            }
        }
    }
}
