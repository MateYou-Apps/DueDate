@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.mateyou.duedate

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.mateyou.duedate.data.AppDatabase
import com.mateyou.duedate.data.CurrencyConfig
import com.mateyou.duedate.data.CustomTemplate
import com.mateyou.duedate.data.DueDate
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.regex.Pattern

enum class TemplateStep(val title: String, val description: String, val canSkip: Boolean) {
    TOTAL_DUE("Select Total Due", "Select Total Due Amount Tag", false),
    MIN_DUE("Select Minimum Due", "Select Minimum Due Amount Tag", true),
    CURRENCY("Select Currency", "Select all currency tags or type symbol/code manually.", false),
    DUE_DATE("Choose Due Date", "Select tag(s) that form the due date.", false),
    REORDER_DATE("Arrange Date", "Assign parts to Day, Month, and Year.", false),
    CARD_NAME("Select Card Name", "Select tag(s) that identify this card.", true),
    CARD_NUMBER("Select Card Number", "Select the tag with card digits.", true),
    CONFIRM("Verify Details", "Confirm if the selected data is accurate.", false),
    SAVE("Save Template", "Add a name for this configuration.", false)
}

data class SmsToken(
    val id: Int,
    val text: String,
    val isSelected: Boolean = false,
    val isDisabled: Boolean = false,
    val stepAssigned: TemplateStep? = null
)

@Composable
fun ConfigureSmsTemplatesScreen(viewModel: DueDateViewModel, initialSms: String? = null, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var smsTextValue by remember { mutableStateOf(TextFieldValue(initialSms ?: "")) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    
    var showConfigOverlay by remember { mutableStateOf(false) }
    var selectedTemplateForView by remember { mutableStateOf<CustomTemplate?>(null) }
    var editingTemplateId by remember { mutableStateOf<String?>(null) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentStep by remember { mutableStateOf(TemplateStep.TOTAL_DUE) }
    val tokens = remember { mutableStateListOf<SmsToken>() }
    
    // Config state
    val dateParts = remember { mutableStateListOf<SmsToken>() }
    var dayToken by remember { mutableStateOf<SmsToken?>(null) }
    var monthToken by remember { mutableStateOf<SmsToken?>(null) }
    var yearToken by remember { mutableStateOf<SmsToken?>(null) }
    var configName by remember { mutableStateOf(TextFieldValue("")) }
    var addBillToMainList by remember { mutableStateOf(false) }
    var bankSearchText by remember { mutableStateOf(TextFieldValue("")) }
    var selectedBankName by remember { mutableStateOf<String?>(null) }
    var manualCurrency by remember { mutableStateOf(TextFieldValue("")) }

    val activeBanks by viewModel.activeBanks.collectAsState(initial = emptyList())
    val allBankNames = remember { BankConfig.getAllBanks() }

    // Saved templates
    val savedTemplates = remember { mutableStateListOf<CustomTemplate>() }
    
    // Load templates on init
    remember {
        val prefs = context.getSharedPreferences("custom_templates", Context.MODE_PRIVATE)
        val json = prefs.getString("templates_list", "[]") ?: "[]"
        // Array parsing for R8
        val list = try {
            Gson().fromJson(json, Array<CustomTemplate>::class.java)?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        savedTemplates.clear()
        savedTemplates.addAll(list)
        true
    }

    fun saveTemplates() {
        val prefs = context.getSharedPreferences("custom_templates", Context.MODE_PRIVATE)
        val json = Gson().toJson(savedTemplates.toList())
        prefs.edit().putString("templates_list", json).apply()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) }
                Text("Configure SMS Templates", style = MaterialTheme.typography.titleLarge)
            }

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Paste a representative SMS below to begin creating a custom parsing template. This allows you to add support for banks or formats not currently handled by the app. This feature is still experimental and may not always work correctly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Justify
                        )
                    }
                }

                OutlinedTextField(
                    value = smsTextValue,
                    onValueChange = { smsTextValue = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                    placeholder = { Text("Paste SMS Text here", color = MaterialTheme.colorScheme.outline) },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (smsTextValue.text.isNotBlank()) {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            tokens.clear()
                            smsTextValue.text.split(Regex("\\s+")).filter { it.isNotBlank() }.forEachIndexed { index, word ->
                                tokens.add(SmsToken(index, word))
                            }
                            currentStep = TemplateStep.TOTAL_DUE
                            showConfigOverlay = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Start", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(32.dp))
                Text("Added Configurations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                
                if (savedTemplates.isEmpty()) {
                    Text("No custom templates added yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline, modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), textAlign = TextAlign.Center)
                } else {
                    savedTemplates.forEach { template ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { selectedTemplateForView = template },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            ListItem(
                                headlineContent = { Text(template.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showConfigOverlay) {
        ModalBottomSheet(
            onDismissRequest = { showConfigOverlay = false; editingTemplateId = null },
            sheetState = sheetState,
            dragHandle = null,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.wrapContentHeight() // Dynamic height
        ) {
            val sampleBill = remember(currentStep, dayToken, monthToken, yearToken, selectedBankName, tokens, manualCurrency) {
                val totalDueToken = tokens.find { it.stepAssigned == TemplateStep.TOTAL_DUE }
                val minDueToken = tokens.find { it.stepAssigned == TemplateStep.MIN_DUE }
                val cardNameTokens = tokens.filter { it.stepAssigned == TemplateStep.CARD_NAME }
                val cardNumberToken = tokens.find { it.stepAssigned == TemplateStep.CARD_NUMBER }
                val currencyToken = tokens.find { it.stepAssigned == TemplateStep.CURRENCY }
                
                val cal = Calendar.getInstance()
                dayToken?.text?.filter { it.isDigit() }?.toIntOrNull()?.let { cal.set(Calendar.DAY_OF_MONTH, it) }
                monthToken?.text?.let { m ->
                    val months = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
                    val mIndex = months.indexOf(m.lowercase().take(3))
                    if (mIndex != -1) cal.set(Calendar.MONTH, mIndex)
                    else m.filter { it.isDigit() }.toIntOrNull()?.let { cal.set(Calendar.MONTH, it - 1) }
                }
                yearToken?.text?.filter { it.isDigit() }?.toIntOrNull()?.let {
                    val y = if (it < 100) 2000 + it else it
                    cal.set(Calendar.YEAR, y)
                }

                val rawCurr = currencyToken?.text ?: manualCurrency.text
                val symbol = if (rawCurr.isNotBlank()) CurrencyConfig.getSymbol(rawCurr) else "₹"

                DueDate(
                    bankName = selectedBankName ?: "Bank",
                    cardName = if(cardNameTokens.isNotEmpty()) cardNameTokens.joinToString(" ") { it.text } else null,
                    cardNumber = cardNumberToken?.text?.filter { it.isDigit() }?.takeLast(4),
                    amount = totalDueToken?.text?.replace(Regex("[^0-9.-]"), "")?.toDoubleOrNull() ?: 0.0,
                    minAmount = minDueToken?.text?.replace(Regex("[^0-9.-]"), "")?.toDoubleOrNull(),
                    currencySymbol = symbol,
                    dueDate = cal.timeInMillis,
                    receivedDate = System.currentTimeMillis()
                )
            }

            ConfigWizardOverlay(
                step = currentStep,
                tokens = tokens,
                dateParts = dateParts,
                dayToken = dayToken,
                monthToken = monthToken,
                yearToken = yearToken,
                sampleBill = sampleBill,
                configName = configName,
                manualCurrency = manualCurrency,
                onManualCurrencyChange = { manualCurrency = it },
                onConfigNameChange = { configName = it },
                addBillToMainList = addBillToMainList,
                onAddBillToMainListChange = { addBillToMainList = it },
                bankSearchText = bankSearchText,
                onBankSearchChange = { bankSearchText = it },
                allBanks = allBankNames,
                selectedBank = selectedBankName,
                onBankSelect = { selectedBankName = it },
                onTokenClick = { clickedToken ->
                    val index = tokens.indexOfFirst { it.id == clickedToken.id }
                    if (index != -1) {
                        when (currentStep) {
                            TemplateStep.TOTAL_DUE, TemplateStep.MIN_DUE, TemplateStep.CARD_NUMBER -> {
                                tokens.forEachIndexed { i, token ->
                                    if (token.stepAssigned == null && token.isSelected && i != index) {
                                        tokens[i] = token.copy(isSelected = false)
                                    }
                                }
                                tokens[index] = clickedToken.copy(isSelected = !clickedToken.isSelected)
                            }
                            TemplateStep.CURRENCY, TemplateStep.DUE_DATE, TemplateStep.CARD_NAME -> {
                                tokens[index] = clickedToken.copy(isSelected = !clickedToken.isSelected)
                                if (currentStep == TemplateStep.CURRENCY && tokens[index].isSelected) {
                                    manualCurrency = TextFieldValue("")
                                }
                            }
                            else -> {}
                        }
                    }
                },
                onAssignDatePart = { token, part ->
                    when(part) {
                        "day" -> dayToken = token
                        "month" -> monthToken = token
                        "year" -> yearToken = token
                    }
                },
                onResetDateParts = {
                    dayToken = null
                    monthToken = null
                    yearToken = null
                },
                onNext = {
                    if (currentStep == TemplateStep.SAVE) {
                        // Pattern Generation Logic
                        val groupMappings = mutableMapOf<Int, String>()
                        var currentGroup = 1
                        val regexParts = mutableListOf<String>()
                        
                        tokens.forEach { token ->
                            if (token.stepAssigned != null) {
                                when (token.stepAssigned) {
                                    TemplateStep.TOTAL_DUE -> { regexParts.add("(\\S+)"); groupMappings[currentGroup++] = "TAD" }
                                    TemplateStep.MIN_DUE -> { regexParts.add("(\\S+)"); groupMappings[currentGroup++] = "MAD" }
                                    TemplateStep.CURRENCY -> { 
                                        regexParts.add("(\\S+)")
                                        groupMappings[currentGroup++] = "CURRENCY"
                                    }
                                    TemplateStep.DUE_DATE -> {
                                        val isSingleToken = tokens.count { it.stepAssigned == TemplateStep.DUE_DATE } == 1
                                        if (isSingleToken) { regexParts.add("(\\S+)"); groupMappings[currentGroup++] = "DATE_RAW" }
                                        else { regexParts.add("(\\S+)"); val partName = when(token.id) { dayToken?.id -> "DAY"; monthToken?.id -> "MONTH"; yearToken?.id -> "YEAR"; else -> "UNKNOWN" }; groupMappings[currentGroup++] = partName }
                                    }
                                    TemplateStep.CARD_NAME -> { regexParts.add("(\\S+)"); groupMappings[currentGroup++] = "CARD_NAME" }
                                    TemplateStep.CARD_NUMBER -> { regexParts.add("(\\S+)"); groupMappings[currentGroup++] = "CARD_NUM" }
                                    else -> regexParts.add(Pattern.quote(token.text))
                                }
                            } else { regexParts.add(Pattern.quote(token.text)) }
                        }

                        // Save the date part order if it was a single token split into DAY, MONTH, YEAR
                        val dateOrder = if (tokens.count { it.stepAssigned == TemplateStep.DUE_DATE } == 1) {
                            val order = mutableMapOf<Int, String>()
                            if (dayToken != null) order[dayToken!!.id] = "DAY"
                            if (monthToken != null) order[monthToken!!.id] = "MONTH"
                            if (yearToken != null) order[yearToken!!.id] = "YEAR"
                            order.entries.sortedBy { it.key }.map { it.value }.joinToString(",")
                        } else null

                        val pattern = regexParts.joinToString("\\s+")
                        val newTemplate = CustomTemplate(
                            id = editingTemplateId ?: System.currentTimeMillis().toString(), 
                            name = configName.text, 
                            rawSms = smsTextValue.text, 
                            patternRegex = pattern, 
                            groupMappings = groupMappings, 
                            billData = sampleBill.copy(bankName = "Bank"), // Make stored preview agnostic
                            dateOrder = dateOrder
                        )

                        if (editingTemplateId != null) {
                            val idx = savedTemplates.indexOfFirst { it.id == editingTemplateId }
                            if (idx != -1) savedTemplates[idx] = newTemplate
                        } else { savedTemplates.add(newTemplate) }
                        
                        if (addBillToMainList) {
                            scope.launch {
                                BillDetectionProcessor.processParsedBill(
                                    context,
                                    AppDatabase.getDatabase(context).dueDateDao(),
                                    sampleBill,
                                    isManual = true
                                )
                            }
                            Toast.makeText(context, "Bill Added", Toast.LENGTH_SHORT).show()
                        }
                        saveTemplates()
                        showConfigOverlay = false
                        editingTemplateId = null
                        smsTextValue = TextFieldValue("")
                        configName = TextFieldValue("")
                        bankSearchText = TextFieldValue("")
                        selectedBankName = null
                        manualCurrency = TextFieldValue("")
                    } else {
                        val nextStep = when (currentStep) {
                            TemplateStep.TOTAL_DUE -> { tokens.forEachIndexed { i, token -> if (token.isSelected) tokens[i] = token.copy(isDisabled = true, isSelected = false, stepAssigned = TemplateStep.TOTAL_DUE) }; TemplateStep.MIN_DUE }
                            TemplateStep.MIN_DUE -> { tokens.forEachIndexed { i, token -> if (token.isSelected) tokens[i] = token.copy(isDisabled = true, isSelected = false, stepAssigned = TemplateStep.MIN_DUE) }; TemplateStep.CURRENCY }
                            TemplateStep.CURRENCY -> { 
                                tokens.forEachIndexed { i, token -> if (token.isSelected) tokens[i] = token.copy(isDisabled = true, isSelected = false, stepAssigned = TemplateStep.CURRENCY) }
                                TemplateStep.DUE_DATE 
                            }
                            TemplateStep.DUE_DATE -> {
                                val selected = tokens.filter { it.isSelected }
                                if (selected.size == 1) {
                                    val parts = selected[0].text.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }
                                    dateParts.clear(); parts.forEachIndexed { i, p -> dateParts.add(SmsToken(i, p)) }
                                } else { dateParts.clear(); selected.forEach { dateParts.add(it.copy(isSelected = false)) } }
                                TemplateStep.REORDER_DATE
                            }
                            TemplateStep.REORDER_DATE -> { tokens.forEachIndexed { i, token -> if (token.isSelected) tokens[i] = token.copy(isDisabled = true, isSelected = false, stepAssigned = TemplateStep.DUE_DATE) }; TemplateStep.CARD_NAME }
                            TemplateStep.CARD_NAME -> { tokens.forEachIndexed { i, token -> if (token.isSelected) tokens[i] = token.copy(isDisabled = true, isSelected = false, stepAssigned = TemplateStep.CARD_NAME) }; TemplateStep.CARD_NUMBER }
                            TemplateStep.CARD_NUMBER -> { tokens.forEachIndexed { i, token -> if (token.isSelected) tokens[i] = token.copy(isDisabled = true, isSelected = false, stepAssigned = TemplateStep.CARD_NUMBER) }; TemplateStep.CONFIRM }
                            TemplateStep.CONFIRM -> TemplateStep.SAVE
                            else -> currentStep
                        }
                        currentStep = nextStep
                    }
                },
                onSkip = {
                    when (currentStep) {
                        TemplateStep.MIN_DUE -> currentStep = TemplateStep.CURRENCY
                        TemplateStep.CARD_NAME -> currentStep = TemplateStep.CARD_NUMBER
                        TemplateStep.CARD_NUMBER -> currentStep = TemplateStep.CONFIRM
                        else -> {
                            val nextStepOrdinal = currentStep.ordinal + 1
                            if (nextStepOrdinal < TemplateStep.entries.size) currentStep = TemplateStep.entries[nextStepOrdinal]
                        }
                    }
                },
                onDismiss = { showConfigOverlay = false; editingTemplateId = null }
            )
        }
    }

    selectedTemplateForView?.let { template ->
        var showDeleteConfirm by remember { mutableStateOf(false) }
        ModalBottomSheet(
            onDismissRequest = { selectedTemplateForView = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(24.dp)) {
                Text("Saved Configuration", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Text("Original SMS:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                    Text(template.rawSms, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(16.dp))
                Text("Sample Result:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                BillCard(dueDate = template.billData, onTogglePaid = {}, isAnimating = false)
                Spacer(Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { editingTemplateId = template.id; smsTextValue = TextFieldValue(template.rawSms); selectedTemplateForView = null; tokens.clear(); template.rawSms.split(Regex("\\s+")).filter { it.isNotBlank() }.forEachIndexed { index, word -> tokens.add(SmsToken(index, word)) }; currentStep = TemplateStep.TOTAL_DUE; showConfigOverlay = true }, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(28.dp)) { Icon(Icons.Default.Edit, null); Spacer(Modifier.width(8.dp)); Text("Edit") }
                    Button(onClick = { showDeleteConfirm = true }, modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error), shape = RoundedCornerShape(28.dp)) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp)); Text("Delete") }
                }
            }
        }
        if (showDeleteConfirm) { AlertDialog(onDismissRequest = { showDeleteConfirm = false }, title = { Text("Delete Configuration?") }, text = { Text("The app will forget this custom parsing template. This action cannot be undone.") }, confirmButton = { TextButton(onClick = { savedTemplates.remove(template); saveTemplates(); showDeleteConfirm = false; selectedTemplateForView = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }) }
    }
    BackHandler { onBack() }
}

@Composable
fun ConfigWizardOverlay(
    step: TemplateStep,
    tokens: List<SmsToken>,
    dateParts: List<SmsToken>,
    dayToken: SmsToken?,
    monthToken: SmsToken?,
    yearToken: SmsToken?,
    sampleBill: DueDate,
    configName: TextFieldValue,
    manualCurrency: TextFieldValue,
    onManualCurrencyChange: (TextFieldValue) -> Unit,
    onConfigNameChange: (TextFieldValue) -> Unit,
    addBillToMainList: Boolean,
    onAddBillToMainListChange: (Boolean) -> Unit,
    bankSearchText: TextFieldValue,
    onBankSearchChange: (TextFieldValue) -> Unit,
    allBanks: List<String>,
    selectedBank: String?,
    onBankSelect: (String) -> Unit,
    onTokenClick: (SmsToken) -> Unit,
    onAssignDatePart: (SmsToken, String) -> Unit,
    onResetDateParts: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = step.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = step.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Box(modifier = Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(scrollState)) {
            when (step) {
                TemplateStep.REORDER_DATE -> { DateReorderView(dateParts, dayToken, monthToken, yearToken, onAssignDatePart, onResetDateParts) }
                TemplateStep.CONFIRM -> { Column(modifier = Modifier.fillMaxWidth()) { BillCard(dueDate = sampleBill, onTogglePaid = {}, isAnimating = false) } }
                TemplateStep.SAVE -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = configName, onValueChange = onConfigNameChange, modifier = Modifier.fillMaxWidth(), label = { Text("Configuration Name") }, placeholder = { Text("e.g. My Custom Bank Configuration") }, shape = RoundedCornerShape(16.dp), singleLine = true)
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth().clickable { onAddBillToMainListChange(!addBillToMainList) }, verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = addBillToMainList, onCheckedChange = onAddBillToMainListChange); Spacer(Modifier.width(8.dp)); Text("Add this bill to my active bills list") }
                        
                        if (addBillToMainList) {
                            Spacer(modifier = Modifier.height(24.dp))
                            val filteredBanks = allBanks.filter { it.contains(bankSearchText.text, ignoreCase = true) }.sorted()
                            if (filteredBanks.isNotEmpty()) {
                                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    filteredBanks.forEach { bank ->
                                        val isSelected = bank == selectedBank
                                        val logoRes = BankConfig.getBankLogo(bank)
                                        Surface(onClick = { onBankSelect(bank) }, shape = RoundedCornerShape(12.dp), color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null) {
                                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) { 
                                                    if (logoRes != null) {
                                                        Icon(painterResource(logoRes), null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                                                    } else {
                                                        Text(bank.take(1), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                }
                                                Spacer(Modifier.height(4.dp))
                                                Text(bank, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            OutlinedTextField(
                                value = bankSearchText, 
                                onValueChange = { onBankSearchChange(it) },
                                modifier = Modifier.fillMaxWidth(), 
                                label = { Text("Pick a Bank") }, 
                                placeholder = { Text("Search banks...") }, 
                                shape = RoundedCornerShape(16.dp), 
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
                TemplateStep.CURRENCY -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { 
                            tokens.forEach { token -> TokenTag(token = token, onClick = { if (!token.isDisabled) onTokenClick(token) }) } 
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        val isTagSelected = tokens.any { it.stepAssigned == null && it.isSelected }
                        OutlinedTextField(
                            value = manualCurrency,
                            onValueChange = onManualCurrencyChange,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isTagSelected,
                            label = { Text("Manual Currency (Symbol or 3-letter code)") },
                            placeholder = { Text("e.g. GBP or $") },
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                disabledTextColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
                else -> { FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { tokens.forEach { token -> TokenTag(token = token, onClick = { if (!token.isDisabled) onTokenClick(token) }) } } }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (step.canSkip) { TextButton(onClick = onSkip, modifier = Modifier.weight(1f).height(56.dp)) { Text("Skip") } }
            val isNextEnabled = when(step) {
                TemplateStep.CURRENCY -> tokens.any { it.isSelected } || manualCurrency.text.isNotBlank()
                TemplateStep.REORDER_DATE -> dayToken != null && monthToken != null
                TemplateStep.SAVE -> configName.text.isNotBlank() && (!addBillToMainList || selectedBank != null)
                TemplateStep.CONFIRM -> true
                else -> tokens.any { it.isSelected }
            }
            Button(onClick = onNext, enabled = isNextEnabled, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(28.dp)) { Text(if (step == TemplateStep.SAVE) "Save" else "Next", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun DateReorderView(parts: List<SmsToken>, day: SmsToken?, month: SmsToken?, year: SmsToken?, onAssign: (SmsToken, String) -> Unit, onReset: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Available Parts:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { parts.forEach { part -> val isUsed = part.id == day?.id || part.id == month?.id || part.id == year?.id; TokenTag(token = part.copy(isDisabled = isUsed), onClick = { if (!isUsed) { when { day == null -> onAssign(part, "day"); month == null -> onAssign(part, "month"); year == null -> onAssign(part, "year") } } }) } }
        Spacer(Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { DateSlot("Day", day, Modifier.weight(1f)); DateSlot("Month", month, Modifier.weight(1f)); DateSlot("Year", year, Modifier.weight(1f)) }
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onReset, modifier = Modifier.align(Alignment.CenterHorizontally)) { Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Reset Arrangement") }
    }
}

@Composable
fun DateSlot(label: String, token: SmsToken?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(12.dp)).background(if (token != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { if (token != null) { Text(token.text, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer) } }
    }
}

@Composable
fun TokenTag(token: SmsToken, onClick: () -> Unit) {
    val bgColor = when { token.isDisabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f); token.isSelected -> MaterialTheme.colorScheme.primary; else -> MaterialTheme.colorScheme.surfaceVariant }
    val textColor = when { token.isDisabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f); token.isSelected -> MaterialTheme.colorScheme.onPrimary; else -> MaterialTheme.colorScheme.onSurfaceVariant }
    val borderModifier = if (token.isSelected) Modifier else Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
    Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(bgColor).then(borderModifier).clickable(enabled = !token.isDisabled) { onClick() }.padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) { Row(verticalAlignment = Alignment.CenterVertically) { Text(text = token.text, color = textColor, style = MaterialTheme.typography.bodyMedium); if (token.isSelected) { Spacer(modifier = Modifier.width(4.dp)); Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = textColor) } } }
}
