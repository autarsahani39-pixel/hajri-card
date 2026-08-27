package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthCredentials
import com.example.data.auth.AuthManager
import com.example.data.auth.AuthResult
import com.example.data.auth.UserProfile
import com.google.firebase.auth.PhoneAuthProvider
import com.example.data.language.AppLanguage
import com.example.data.language.LanguageManager
import com.example.data.local.HajriDatabase
import com.example.data.model.Attendance
import com.example.data.model.AttendanceStatus
import com.example.data.model.PayType
import com.example.data.model.SignatureRecord
import com.example.data.model.Worker
import com.example.data.model.WorkerMonthSummary
import com.example.data.repository.HajriRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class HajriViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HajriRepository
    private val authManager: AuthManager = AuthManager.getInstance(application)

    val userProfile: StateFlow<UserProfile> = authManager.userProfile

    init {
        LanguageManager.initialize(application)
        val database = HajriDatabase.getDatabase(application)
        repository = HajriRepository(database.hajriDao())
    }

    val currentLanguage: StateFlow<AppLanguage> = LanguageManager.currentLanguage

    // Selected Month & Year for Muster Card and calculations
    private val _selectedYearMonth = MutableStateFlow(YearMonth.now())
    val selectedYearMonth: StateFlow<YearMonth> = _selectedYearMonth.asStateFlow()

    // Selected Date for Daily Attendance marking
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Selected Worker ID for Muster Card View
    private val _selectedWorkerId = MutableStateFlow<Long?>(null)
    val selectedWorkerId: StateFlow<Long?> = _selectedWorkerId.asStateFlow()

    // Search query for workers
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Active workers
    val allWorkers: StateFlow<List<Worker>> = repository.allWorkers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered workers based on search query
    val filteredWorkers: StateFlow<List<Worker>> = combine(allWorkers, _searchQuery) { workers, query ->
        if (query.isBlank()) {
            workers
        } else {
            val q = query.trim().lowercase()
            workers.filter {
                it.name.lowercase().contains(q) ||
                it.workerId.lowercase().contains(q) ||
                it.mobile.contains(q) ||
                it.job.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current month attendances for all workers
    val currentMonthAttendances: StateFlow<List<Attendance>> = _selectedYearMonth.flatMapLatest { ym ->
        val ymStr = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        repository.getAttendanceForMonth(ymStr)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected date attendances for daily marking
    val selectedDateAttendances: StateFlow<List<Attendance>> = _selectedDate.flatMapLatest { date ->
        val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        repository.getAttendanceForDate(dateStr)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected worker's attendance list for selected month
    val selectedWorkerAttendances: StateFlow<List<Attendance>> = combine(_selectedWorkerId, _selectedYearMonth) { workerId, ym ->
        Pair(workerId, ym)
    }.flatMapLatest { (workerId, ym) ->
        val ymStr = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        if (workerId != null) {
            repository.getAttendanceForWorkerMonth(workerId, ymStr)
        } else {
            MutableStateFlow(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected worker signature record
    val selectedWorkerSignature: StateFlow<SignatureRecord?> = combine(_selectedWorkerId, _selectedYearMonth) { workerId, ym ->
        Pair(workerId, ym)
    }.flatMapLatest { (workerId, ym) ->
        val ymStr = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        if (workerId != null) {
            repository.getSignature(workerId, ymStr)
        } else {
            MutableStateFlow(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Worker summaries for current month
    val workerSummaries: StateFlow<List<WorkerMonthSummary>> = combine(allWorkers, currentMonthAttendances, _selectedYearMonth) { workers, attendances, ym ->
        val attByWorker = attendances.groupBy { it.workerId }
        val daysInMonth = ym.lengthOfMonth()

        workers.map { worker ->
            val workerAtts = attByWorker[worker.id] ?: emptyList()
            val presentCount = workerAtts.count { it.status == AttendanceStatus.PRESENT }
            val halfDayCount = workerAtts.count { it.status == AttendanceStatus.HALF_DAY }
            val pHalfCount = workerAtts.count { it.status == AttendanceStatus.LEAVE }
            val doublePresentCount = workerAtts.count { it.status == AttendanceStatus.DOUBLE_PRESENT }
            val absentCount = workerAtts.count { it.status == AttendanceStatus.ABSENT }
            val totalAdvance = workerAtts.sumOf { it.advance }

            val workedDaysEquivalent = (presentCount.toDouble() * 1.0) + (halfDayCount.toDouble() * 0.5) + (pHalfCount.toDouble() * 1.5) + (doublePresentCount.toDouble() * 2.0)

            val calculatedSalary = when (worker.payType) {
                PayType.DAILY -> {
                    (presentCount * worker.dailyWage) + (halfDayCount * (worker.dailyWage * 0.5)) + (pHalfCount * (worker.dailyWage * 1.5)) + (doublePresentCount * (worker.dailyWage * 2.0))
                }
                PayType.MONTHLY -> {
                    // For monthly employees: if full month worked or pro-rated
                    val dailyRate = if (daysInMonth > 0) worker.monthlyPay / daysInMonth.toDouble() else 0.0
                    workedDaysEquivalent * dailyRate
                }
            }

            val balance = calculatedSalary - totalAdvance

            WorkerMonthSummary(
                worker = worker,
                presentDays = workedDaysEquivalent,
                halfDays = halfDayCount,
                absentDays = absentCount,
                leaveDays = pHalfCount,
                doubleDays = doublePresentCount,
                totalAdvance = totalAdvance,
                calculatedSalary = calculatedSalary,
                balancePayable = balance
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month navigation actions
    fun nextMonth() {
        _selectedYearMonth.value = _selectedYearMonth.value.plusMonths(1)
    }

    fun previousMonth() {
        _selectedYearMonth.value = _selectedYearMonth.value.minusMonths(1)
    }

    fun setYearMonth(yearMonth: YearMonth) {
        _selectedYearMonth.value = yearMonth
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setSelectedWorkerId(id: Long?) {
        _selectedWorkerId.value = id
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Worker operations
    fun saveWorker(worker: Worker, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.insertOrUpdateWorker(worker)
            onComplete(id)
        }
    }

    fun deleteWorker(workerId: Long) {
        viewModelScope.launch {
            if (_selectedWorkerId.value == workerId) {
                _selectedWorkerId.value = null
            }
            repository.deleteWorker(workerId)
        }
    }

    // Attendance operations
    fun setAttendance(
        workerId: Long,
        date: String,
        status: AttendanceStatus,
        advance: Double = -1.0,
        remark: String = ""
    ) {
        viewModelScope.launch {
            repository.recordAttendance(workerId, date, status, advance, remark)
        }
    }

    fun setAdvance(
        workerId: Long,
        date: String,
        advanceAmount: Double,
        remark: String = ""
    ) {
        viewModelScope.launch {
            repository.updateAdvance(workerId, date, advanceAmount, remark)
        }
    }

    fun markAllPresentToday() {
        viewModelScope.launch {
            val todayStr = _selectedDate.value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            repository.markAllWorkersPresent(todayStr, allWorkers.value)
        }
    }

    fun saveSignature(signatureSvg: String) {
        val workerId = _selectedWorkerId.value ?: return
        val ymStr = _selectedYearMonth.value.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        viewModelScope.launch {
            repository.saveSignature(workerId, ymStr, signatureSvg)
        }
    }

    fun clearSignature() {
        val workerId = _selectedWorkerId.value ?: return
        val ymStr = _selectedYearMonth.value.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        viewModelScope.launch {
            repository.clearSignature(workerId, ymStr)
        }
    }

    fun clearAllData(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.clearAllData()
            _selectedWorkerId.value = null
            onComplete?.invoke()
        }
    }

    fun resetAndSeedSampleData() {
        viewModelScope.launch {
            repository.clearAllData()
            repository.populateSampleDataIfEmpty()
        }
    }

    fun signInWithEmail(
        email: String,
        pass: String,
        ownerName: String = "",
        businessName: String = "",
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        authManager.signInWithEmail(
            email = email,
            pass = pass,
            ownerName = ownerName,
            businessName = businessName,
            onSuccess = {
                if (businessName.isNotBlank()) {
                    updateAllCompanyDetails(businessName, "")
                }
                onSuccess()
            },
            onFailure = onFailure
        )
    }

    fun registerWithEmail(
        email: String,
        pass: String,
        ownerName: String,
        businessName: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        authManager.registerWithEmail(
            email = email,
            pass = pass,
            ownerName = ownerName,
            businessName = businessName,
            onSuccess = {
                if (businessName.isNotBlank()) {
                    updateAllCompanyDetails(businessName, "")
                }
                onSuccess()
            },
            onFailure = onFailure
        )
    }

    fun sendPasswordReset(
        email: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        authManager.sendPasswordResetEmail(email, onSuccess, onFailure)
    }

    fun updateOwnerProfile(ownerName: String, businessName: String) {
        authManager.updateProfile(ownerName, businessName)
        if (businessName.isNotBlank()) {
            updateAllCompanyDetails(businessName, "")
        }
    }

    fun updateAllCompanyDetails(companyName: String, address: String) {
        viewModelScope.launch {
            allWorkers.value.forEach { w ->
                val updated = w.copy(
                    companyName = companyName,
                    address = if (w.address.isBlank()) address else w.address
                )
                repository.insertOrUpdateWorker(updated)
            }
        }
    }

    fun isWhatsAppConfigured(): Boolean {
        return authManager.isWhatsAppConfigured()
    }

    fun getWhatsAppBackendUrl(): String {
        return authManager.getWhatsAppBackendUrl()
    }

    fun setWhatsAppBackendUrl(url: String) {
        authManager.setWhatsAppBackendUrl(url)
    }

    fun authenticateWithCredentials(
        credentials: AuthCredentials,
        onResult: (AuthResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = authManager.authenticate(credentials)
            if (result is AuthResult.Success) {
                if (result.profile.businessName.isNotBlank()) {
                    updateAllCompanyDetails(result.profile.businessName, "")
                }
            }
            onResult(result)
        }
    }

    fun requestWhatsAppLogin(
        phoneNumber: String,
        onResult: (AuthResult) -> Unit
    ) {
        authenticateWithCredentials(
            credentials = AuthCredentials.WhatsApp(phoneNumber = phoneNumber),
            onResult = onResult
        )
    }

    fun verifyWhatsAppCode(
        phoneNumber: String,
        sessionToken: String,
        code: String,
        ownerName: String = "",
        businessName: String = "",
        onResult: (AuthResult) -> Unit
    ) {
        authenticateWithCredentials(
            credentials = AuthCredentials.WhatsApp(
                phoneNumber = phoneNumber,
                serverSessionToken = sessionToken,
                verificationCode = code,
                ownerName = ownerName,
                businessName = businessName
            ),
            onResult = onResult
        )
    }

    fun sendPhoneOtp(
        activity: Activity,
        phoneNumber: String,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks,
        timeoutSeconds: Long = 60L
    ) {
        authManager.sendPhoneOtp(activity, phoneNumber, callbacks, timeoutSeconds)
    }

    fun verifyPhoneOtp(
        verificationId: String,
        code: String,
        ownerName: String = "",
        businessName: String = "",
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        authManager.verifyPhoneOtp(
            verificationId = verificationId,
            code = code,
            ownerName = ownerName,
            businessName = businessName,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun logoutUser() {
        authManager.logout()
    }

    fun setLanguage(languageCode: String): AppLanguage {
        return LanguageManager.setLanguage(getApplication(), languageCode)
    }

    fun getSupportedLanguages(): List<AppLanguage> {
        return LanguageManager.SUPPORTED_LANGUAGES
    }
}
