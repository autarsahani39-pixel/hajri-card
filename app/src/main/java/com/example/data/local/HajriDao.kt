package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Attendance
import com.example.data.model.SignatureRecord
import com.example.data.model.Worker
import kotlinx.coroutines.flow.Flow

@Dao
interface HajriDao {

    // --- WORKERS ---
    @Query("SELECT * FROM workers WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveWorkers(): Flow<List<Worker>>

    @Query("SELECT * FROM workers ORDER BY id DESC")
    fun getAllWorkersList(): Flow<List<Worker>>

    @Query("SELECT * FROM workers WHERE id = :id LIMIT 1")
    suspend fun getWorkerById(id: Long): Worker?

    @Query("SELECT * FROM workers WHERE id = :id LIMIT 1")
    fun getWorkerFlowById(id: Long): Flow<Worker?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: Worker): Long

    @Update
    suspend fun updateWorker(worker: Worker)

    @Delete
    suspend fun deleteWorker(worker: Worker)

    @Query("DELETE FROM workers WHERE id = :workerId")
    suspend fun deleteWorkerById(workerId: Long)

    @Query("SELECT COUNT(*) FROM workers WHERE isActive = 1")
    fun getActiveWorkerCount(): Flow<Int>

    // --- ATTENDANCE ---
    @Query("SELECT * FROM attendance WHERE workerId = :workerId AND date LIKE :yearMonth || '%' ORDER BY date ASC")
    fun getAttendanceForWorkerMonth(workerId: Long, yearMonth: String): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE date = :date")
    fun getAttendanceForDate(date: String): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE date LIKE :yearMonth || '%'")
    fun getAttendanceForMonth(yearMonth: String): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE workerId = :workerId AND date = :date LIMIT 1")
    suspend fun getAttendance(workerId: Long, date: String): Attendance?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAttendance(attendance: Attendance): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAttendance(attendances: List<Attendance>)

    @Query("DELETE FROM attendance WHERE workerId = :workerId")
    suspend fun deleteAttendanceForWorker(workerId: Long)

    @Query("DELETE FROM attendance WHERE workerId = :workerId AND date = :date")
    suspend fun deleteAttendance(workerId: Long, date: String)

    // --- SIGNATURES ---
    @Query("SELECT * FROM signatures WHERE workerId = :workerId AND month = :yearMonth LIMIT 1")
    fun getSignature(workerId: Long, yearMonth: String): Flow<SignatureRecord?>

    @Query("SELECT * FROM signatures WHERE workerId = :workerId AND month = :yearMonth LIMIT 1")
    suspend fun getSignatureOnce(workerId: Long, yearMonth: String): SignatureRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSignature(signature: SignatureRecord): Long

    @Query("DELETE FROM signatures WHERE workerId = :workerId AND month = :yearMonth")
    suspend fun deleteSignature(workerId: Long, yearMonth: String)

    @Query("DELETE FROM signatures WHERE workerId = :workerId")
    suspend fun deleteSignaturesForWorker(workerId: Long)

    // --- PURGE / RESET ---
    @Query("DELETE FROM attendance WHERE workerId NOT IN (SELECT id FROM workers)")
    suspend fun deleteOrphanedAttendance()

    @Query("DELETE FROM signatures WHERE workerId NOT IN (SELECT id FROM workers)")
    suspend fun deleteOrphanedSignatures()

    @Query("DELETE FROM workers")
    suspend fun deleteAllWorkers()

    @Query("DELETE FROM attendance")
    suspend fun deleteAllAttendance()

    @Query("DELETE FROM signatures")
    suspend fun deleteAllSignatures()
}
