package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PayType
import com.example.data.model.Worker
import com.example.ui.viewmodel.HajriViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AddEditWorkerScreen(
    viewModel: HajriViewModel,
    workerId: Long?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val workers by viewModel.allWorkers.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val existingWorker = remember(workerId, workers) {
        if (workerId != null && workerId > 0) workers.find { it.id == workerId } else null
    }

    var name by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var customWorkerId by remember { mutableStateOf("") }
    var job by remember { mutableStateOf("") }
    var payType by remember { mutableStateOf(PayType.DAILY) }
    var dailyWage by remember { mutableStateOf("") }
    var monthlyPay by remember { mutableStateOf("") }
    var joiningDate by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))) }
    var companyName by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var wageError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(existingWorker, userProfile) {
        if (existingWorker != null) {
            name = existingWorker.name
            fatherName = existingWorker.fatherName
            mobile = existingWorker.mobile
            address = existingWorker.address
            customWorkerId = existingWorker.workerId
            job = existingWorker.job
            payType = existingWorker.payType
            dailyWage = if (existingWorker.dailyWage > 0) String.format(Locale.ENGLISH, "%.0f", existingWorker.dailyWage) else ""
            monthlyPay = if (existingWorker.monthlyPay > 0) String.format(Locale.ENGLISH, "%.0f", existingWorker.monthlyPay) else ""
            joiningDate = if (existingWorker.joiningDate.isNotBlank()) existingWorker.joiningDate else LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            companyName = if (existingWorker.companyName.isNotBlank()) existingWorker.companyName else userProfile.businessName
        } else {
            if (customWorkerId.isBlank()) {
                val nextNum = (((workers.maxOfOrNull { it.id } ?: 0L) + 1L)).toString().padStart(3, '0')
                customWorkerId = "WRK-$nextNum"
            }
            if (companyName.isBlank() && userProfile.businessName.isNotBlank()) {
                companyName = userProfile.businessName
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Banner Title
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (existingWorker != null) "Edit Worker Details" else "Add New Worker",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = "Fill in employee details for muster attendance and wage ledger",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
            }
        }

        // Section 1: Basic Identity
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Basic Identity", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    label = { Text("Worker Name*") },
                    placeholder = { Text("e.g. John Doe") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = { if (nameError != null) Text(nameError!!, color = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.fillMaxWidth().testTag("input_worker_name")
                )

                OutlinedTextField(
                    value = fatherName,
                    onValueChange = { fatherName = it },
                    label = { Text("Father / Husband Name") },
                    placeholder = { Text("e.g. Robert Doe") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_father_name")
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customWorkerId,
                        onValueChange = { customWorkerId = it },
                        label = { Text("Worker ID") },
                        placeholder = { Text("WRK-001") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_worker_id")
                    )

                    OutlinedTextField(
                        value = job,
                        onValueChange = { job = it },
                        label = { Text("Job / Role") },
                        placeholder = { Text("e.g. Technician, Helper") },
                        leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f).testTag("input_worker_job")
                    )
                }

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it.filter { char -> char.isDigit() } },
                    label = { Text("Mobile Number") },
                    placeholder = { Text("9876543210") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_worker_mobile")
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address / Location") },
                    placeholder = { Text("Area, City, State") },
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_worker_address")
                )
            }
        }

        // Section 2: Wage & Pay Rate Details
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Wage & Pay Rate", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                // Pay Type Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = payType == PayType.DAILY,
                        onClick = { payType = PayType.DAILY },
                        label = { Text("Daily Wage", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f).testTag("chip_pay_daily")
                    )
                    FilterChip(
                        selected = payType == PayType.MONTHLY,
                        onClick = { payType = PayType.MONTHLY },
                        label = { Text("Monthly Salary", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f).testTag("chip_pay_monthly")
                    )
                }

                if (payType == PayType.DAILY) {
                    OutlinedTextField(
                        value = dailyWage,
                        onValueChange = {
                            dailyWage = it.filter { char -> char.isDigit() || char == '.' }
                            wageError = null
                        },
                        label = { Text("Daily Wage Rate (₹)*") },
                        placeholder = { Text("e.g. 500, 650") },
                        leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = wageError != null,
                        supportingText = { if (wageError != null) Text(wageError!!, color = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.fillMaxWidth().testTag("input_daily_wage")
                    )
                } else {
                    OutlinedTextField(
                        value = monthlyPay,
                        onValueChange = {
                            monthlyPay = it.filter { char -> char.isDigit() || char == '.' }
                            wageError = null
                        },
                        label = { Text("Monthly Pay (₹)*") },
                        placeholder = { Text("e.g. 15000, 20000") },
                        leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = wageError != null,
                        supportingText = { if (wageError != null) Text(wageError!!, color = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.fillMaxWidth().testTag("input_monthly_pay")
                    )
                }
            }
        }

        // Section 3: Company & Joining
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Company & Joining Details", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Factory / Company Name") },
                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_company_name")
                )

                OutlinedTextField(
                    value = joiningDate,
                    onValueChange = { joiningDate = it },
                    label = { Text("Joining Date (YYYY-MM-DD)") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_joining_date")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Submit Button
        Button(
            onClick = {
                if (name.isBlank()) {
                    nameError = "Please enter worker name"
                    return@Button
                }
                val dWageVal = dailyWage.toDoubleOrNull() ?: 0.0
                val mPayVal = monthlyPay.toDoubleOrNull() ?: 0.0

                if (payType == PayType.DAILY && dWageVal <= 0) {
                    wageError = "Please enter a valid daily wage rate"
                    return@Button
                }
                if (payType == PayType.MONTHLY && mPayVal <= 0) {
                    wageError = "Please enter a valid monthly salary"
                    return@Button
                }

                val workerToSave = Worker(
                    id = existingWorker?.id ?: 0L,
                    name = name.trim(),
                    fatherName = fatherName.trim(),
                    mobile = mobile.trim(),
                    address = address.trim(),
                    workerId = customWorkerId.trim(),
                    job = job.trim(),
                    payType = payType,
                    dailyWage = dWageVal,
                    monthlyPay = mPayVal,
                    joiningDate = joiningDate.trim(),
                    companyName = companyName.trim()
                )

                viewModel.saveWorker(workerToSave) { savedId ->
                    viewModel.setSelectedWorkerId(savedId)
                    Toast.makeText(
                        context,
                        if (existingWorker != null) "Worker details updated successfully!" else "Worker added successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    onNavigateBack()
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("submit_worker_btn")
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (existingWorker != null) "Update Worker" else "Save Worker",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}
