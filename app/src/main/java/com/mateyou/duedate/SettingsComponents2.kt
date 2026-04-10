@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.mateyou.duedate

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.mateyou.duedate.data.AppDatabase
import com.mateyou.duedate.data.DueDate
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt


@Composable
fun BillParsingScreen(viewModel: DueDateViewModel, onBack: () -> Unit, onConfigureSms: (String) -> Unit) {
    var smsTextValue by remember { mutableStateOf(TextFieldValue("")) }
    var parsedResult by remember { mutableStateOf<DueDate?>(null) }
    var hasAttemptedParse by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val activeBanks by viewModel.activeBanks.collectAsState(initial = emptyList())
    val bankLogoMap = remember(activeBanks) {
        activeBanks.associateBy({ it.name.lowercase() }, { it.svgLogo })
    }

    var showAddBillOverlay by remember { mutableStateOf(false) }
    val allBankNames = remember { BankConfig.getAllBanks() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) }
                    Text("SMS Parser", style = MaterialTheme.typography.titleLarge)
                }

                IconButton(onClick = {
                    val parsingResultString = if (parsedResult != null) {
                        val result = parsedResult!!
                        """
                        Status: Successfully Parsed
                        Bank: ${result.bankName}
                        Card Name: ${result.cardName ?: "Not Available"}
                        Card Number: ${if (result.cardNumber != null) "●●${result.cardNumber}" else "Not Available"}
                        Currency: ${result.currencySymbol}
                        Total Due: ${String.format(Locale.getDefault(), "%.2f", result.amount)}
                        Minimum Due: ${if (result.minAmount != null) String.format(Locale.getDefault(), "%.2f", result.minAmount) else "Not Available"}
                        Due Date: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(result.dueDate))}
                        """.trimIndent()
                    } else if (hasAttemptedParse) {
                        "Status: Parsing Failed"
                    } else {
                        "Status: Parse not attempted yet"
                    }

                    val emailBody = """
                        Pasted SMS -
                        ${smsTextValue.text}

                        Parsing Result -
                        $parsingResultString

                        ----------
                        Type details here (optional)
                    """.trimIndent()

                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = "mailto:".toUri()
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("mateyouapps@gmail.com"))
                        putExtra(Intent.EXTRA_SUBJECT, "Bill Parser Report - DueDate App")
                        putExtra(Intent.EXTRA_TEXT, emailBody)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Outlined.Report, "Report issue", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // Info Box
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Info,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Paste an SMS from your bank below to test parsing. This helps identify issues with new formats. If you face an issue, use the Report button above to let us know. Your data is private and is parsed entirely on-device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Justify
                        )
                    }
                }

                // Input Field
                OutlinedTextField(
                    value = smsTextValue,
                    onValueChange = { smsTextValue = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                    placeholder = { Text("SMS Text", color = MaterialTheme.colorScheme.outline) },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Parse Button
                Button(
                    onClick = {
                        if (smsTextValue.text.isNotBlank()) {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            parsedResult = SmsParser.parse(context, smsTextValue.text, System.currentTimeMillis())
                            hasAttemptedParse = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Parse Text", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Results Section
                if (hasAttemptedParse) {
                    Text(
                        "Parse Result",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val result = parsedResult
                    if (result != null) {
                        val svgLogo = bankLogoMap[result.cardName?.lowercase() ?: ""] ?: bankLogoMap[result.bankName.lowercase()]

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Successfully Parsed", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                    }

                                    SvgLogo(
                                        svgString = svgLogo,
                                        bankName = result.bankName,
                                        cardName = result.cardName,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                ResultRow("Card Name:", result.cardName ?: "Not Available")
                                ResultRow("Card Number:", if (result.cardNumber != null) "●●${result.cardNumber}" else "Not Available")
                                ResultRow("Currency:", result.currencySymbol)
                                ResultRow("Total Due:", String.format(Locale.getDefault(), "%.2f", result.amount))
                                ResultRow("Minimum Due:", if (result.minAmount != null) String.format(Locale.getDefault(), "%.2f", result.minAmount) else "Not Available")
                                ResultRow("Due Date:", SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(result.dueDate)))
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                        ) {
                            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(12.dp))
                                Text("Could not parse this text. Please check the format.", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { onConfigureSms(smsTextValue.text) },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) {
                            Icon(Icons.Default.Settings, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Configure", style = MaterialTheme.typography.labelLarge)
                        }
                        
                        if (result != null) {
                            Button(
                                onClick = { showAddBillOverlay = true },
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Add Bill", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (showAddBillOverlay && parsedResult != null) {
        AddParsedBillOverlay(
            bill = parsedResult!!,
            viewModel = viewModel,
            allBanks = allBankNames,
            onDismiss = { showAddBillOverlay = false }
        )
    }

    BackHandler { onBack() }
}

@Composable
fun AddParsedBillOverlay(bill: DueDate, viewModel: DueDateViewModel, allBanks: List<String>, onDismiss: () -> Unit) {
    var currentStep by remember { mutableStateOf(0) } // 0: Verify, 1: Pick Bank
    var bankSearchText by remember { mutableStateOf(TextFieldValue("")) }
    var selectedBankName by remember { mutableStateOf<String?>(if (bill.bankName != "Bank") bill.bankName else null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = if (currentStep == 0) "Verify Details" else "Pick a Bank", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text(text = if (currentStep == 0) "Confirm if the extracted data is accurate." else "Pick a bank to associate this bill with.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (currentStep == 0) {
                BillCard(dueDate = bill, onTogglePaid = {}, isAnimating = false)
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val filteredBanks = allBanks.filter { it.contains(bankSearchText.text, ignoreCase = true) }.sorted()
                    if (filteredBanks.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            filteredBanks.forEach { bank ->
                                val isSelected = bank == selectedBankName
                                val logoRes = BankConfig.getBankLogo(bank)
                                Surface(onClick = { selectedBankName = bank }, shape = RoundedCornerShape(12.dp), color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null) {
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
                        onValueChange = { bankSearchText = it }, 
                        modifier = Modifier.fillMaxWidth(), 
                        label = { Text("Pick a Bank") }, 
                        placeholder = { Text("Search banks...") }, 
                        shape = RoundedCornerShape(16.dp), 
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (currentStep == 0) {
                        currentStep = 1
                    } else {
                        scope.launch {
                            val finalBill = bill.copy(bankName = selectedBankName ?: "Bank")
                            BillDetectionProcessor.processParsedBill(context, AppDatabase.getDatabase(context).dueDateDao(), finalBill, isManual = true)
                            Toast.makeText(context, "Bill Added", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    }
                },
                enabled = currentStep == 0 || selectedBankName != null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(if (currentStep == 0) "Next" else "Add", fontWeight = FontWeight.Bold)
            }
        }
    }
}


@Composable
fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

enum class ParseOption { BY_COUNT, BY_DATE }

@Composable
fun ParseExistingSmsDialog(onDismiss: () -> Unit, onParse: (Int?, Int?) -> Unit) {
    var selectedOption by remember { mutableStateOf(ParseOption.BY_COUNT) }
    var sliderValue by remember { mutableStateOf(25f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    "Parse Existing Messages",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Choose how far back to scan for missed bill SMS from your whitelisted bank sender IDs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().selectableGroup()) {
                Spacer(modifier = Modifier.height(16.dp))

                // Option 1: By Count
                Surface(
                    onClick = { selectedOption = ParseOption.BY_COUNT },
                    shape = RoundedCornerShape(16.dp),
                    color = if (selectedOption == ParseOption.BY_COUNT) MaterialTheme.colorScheme.primaryContainer.copy(
                        alpha = 0.3f
                    ) else Color.Transparent,
                    border = if (selectedOption == ParseOption.BY_COUNT) BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    ) else null
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedOption == ParseOption.BY_COUNT,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "By Message Count",
                            fontWeight = if (selectedOption == ParseOption.BY_COUNT) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Option 2: By Date
                Surface(
                    onClick = { selectedOption = ParseOption.BY_DATE },
                    shape = RoundedCornerShape(16.dp),
                    color = if (selectedOption == ParseOption.BY_DATE) MaterialTheme.colorScheme.primaryContainer.copy(
                        alpha = 0.3f
                    ) else Color.Transparent,
                    border = if (selectedOption == ParseOption.BY_DATE) BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    ) else null
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedOption == ParseOption.BY_DATE,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "By Date Range",
                            fontWeight = if (selectedOption == ParseOption.BY_DATE) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Slider Area
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 1f..50f,
                        steps = 48,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "1",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            "50",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val value = sliderValue.roundToInt()
                    val unit = if (selectedOption == ParseOption.BY_COUNT) "messages" else "days"

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Parsing bills over the last ",
                            color = MaterialTheme.colorScheme.outline
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                value.toString(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Text(" $unit", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedOption == ParseOption.BY_COUNT) onParse(
                        sliderValue.roundToInt(),
                        null
                    )
                    else onParse(null, sliderValue.roundToInt())
                },
                shape = RoundedCornerShape(24.dp)
            ) { Text("Start Parsing") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(28.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(horizontal = 24.dp)
    )
}
