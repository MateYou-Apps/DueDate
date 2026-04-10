package com.mateyou.duedate

import android.content.Context
import com.google.gson.Gson
import com.mateyou.duedate.data.CurrencyConfig
import com.mateyou.duedate.data.DueDate
import com.mateyou.duedate.data.CustomTemplate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

object SmsParser {
    
    fun parse(context: Context, rawBody: String, receivedDate: Long, sender: String? = null): DueDate? {
        // Clean up common SMS whitespace issues (NBSP, ZWSP, etc.)
        val body = rawBody.replace("\u00A0", " ") 
            .replace("\u202F", " ") 
            .replace("\u200B", "")  
            .replace("\uFEFF", "")  

        // 1. Try Custom Templates First
        // This allows user-defined logic to override built-in patterns
        val customResult = parseWithCustomTemplates(context, body, receivedDate, sender)
        if (customResult != null) {
            SmsDiagnosticLogger.logParse(
                context = context,
                body = rawBody,
                sender = sender,
                bankName = customResult.bankName,
                cardName = customResult.cardName,
                cardNumber = customResult.cardNumber,
                totalDue = customResult.amount,
                minDue = customResult.minAmount,
                dueDate = customResult.dueDate,
                currencySymbol = customResult.currencySymbol
            )
            return customResult
        }

        // 2. Fallback to Built-in Logic
        val amountResult = findTotalDueWithCurrency(body)
        val totalDue = amountResult?.first
        val currencySymbol = amountResult?.second ?: "₹"
        
        val minDue = findMinDue(body)
        val dueDate = findDueDate(context, body, receivedDate, currencySymbol)
        
        // Strictly parse bank names from the Sender ID mapping only
        val bankName = if (sender != null) BankConfig.getBankName(sender) else "Bank"
        val cardName = findCardName(body, bankName)
        val cardNumber = findCardNumber(body)

        // Log the parse attempt for diagnostics/debugging
        SmsDiagnosticLogger.logParse(
            context = context,
            body = rawBody,
            sender = sender,
            bankName = bankName,
            cardName = cardName,
            cardNumber = cardNumber,
            totalDue = totalDue,
            minDue = minDue,
            dueDate = dueDate,
            currencySymbol = currencySymbol
        )

        if (totalDue == null) return null

        return DueDate(
            bankName = bankName,
            cardName = cardName,
            cardNumber = cardNumber,
            amount = totalDue,
            minAmount = minDue,
            currencySymbol = currencySymbol,
            dueDate = dueDate,
            receivedDate = receivedDate
        )
    }

    private fun parseWithCustomTemplates(context: Context, body: String, receivedDate: Long, sender: String?): DueDate? {
        val prefs = context.getSharedPreferences("custom_templates", Context.MODE_PRIVATE)
        val json = prefs.getString("templates_list", "[]") ?: "[]"
        
        // Using Array parsing instead of TypeToken to avoid R8 shrinking issues
        val templates = try {
            Gson().fromJson(json, Array<CustomTemplate>::class.java)?.toList()
        } catch (e: Exception) {
            null
        } ?: return null

        for (template in templates) {
            try {
                val pattern = Pattern.compile(template.patternRegex, Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
                val matcher = pattern.matcher(body)
                if (matcher.find()) {
                    var tad: Double? = null
                    var mad: Double? = null
                    var day: Int? = null
                    var monthStr: String? = null
                    var year: Int? = null
                    var dateRaw: String? = null
                    var cardName: String? = null
                    var cardNum: String? = null
                    var capturedCurrency: String? = null

                    // Extract values using capture groups defined in the template
                    // Sorted by index to handle multi-token names (e.g. "Platinum" + "Card")
                    template.groupMappings.keys.sorted().forEach { groupIndex ->
                        val field = template.groupMappings[groupIndex]
                        val value = matcher.group(groupIndex) ?: return@forEach
                        when (field) {
                            "TAD" -> tad = value.replace(Regex("[^0-9.-]"), "").toDoubleOrNull()
                            "MAD" -> mad = value.replace(Regex("[^0-9.-]"), "").toDoubleOrNull()
                            "DAY" -> day = value.filter { it.isDigit() }.toIntOrNull()
                            "MONTH" -> monthStr = value
                            "YEAR" -> year = value.filter { it.isDigit() }.toIntOrNull()
                            "DATE_RAW" -> dateRaw = value
                            "CURRENCY" -> capturedCurrency = value
                            "CARD_NAME" -> {
                                val current = cardName
                                cardName = if (current == null) value.trim() else "${current.trim()} ${value.trim()}"
                            }
                            "CARD_NUM" -> cardNum = value.filter { it.isDigit() }.takeLast(4)
                        }
                    }

                    if (tad == null) continue

                    // Process the Date extracted from the template
                    val cal = Calendar.getInstance().apply { timeInMillis = receivedDate }
                    if (dateRaw != null) {
                        // 1. Strict Mode: If the template has a saved order, use it strictly
                        if (!template.dateOrder.isNullOrBlank()) {
                            // Clean ordinal suffixes (st, nd, rd, th) before splitting
                            val cleanedDateRaw = dateRaw!!.replace(Regex("(\\d+)(st|nd|rd|th)", RegexOption.IGNORE_CASE), "$1")
                            val parts = cleanedDateRaw.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }
                            val order = template.dateOrder.split(",")
                            
                            if (parts.size >= order.size) {
                                order.forEachIndexed { index, partType ->
                                    val value = parts[index]
                                    when (partType) {
                                        "DAY" -> day = value.filter { it.isDigit() }.toIntOrNull()
                                        "MONTH" -> monthStr = value
                                        "YEAR" -> year = value.filter { it.isDigit() }.toIntOrNull()
                                    }
                                }
                            }
                        }

                        // 2. Regex Mode: If strict parsing didn't find parts, try standard regex match
                        if (day == null || monthStr == null || year == null) {
                            val parsedDate = findDueDate(context, dateRaw!!, receivedDate, "₹")
                            if (parsedDate != receivedDate) {
                                cal.timeInMillis = parsedDate
                            }
                        }
                        
                        // Apply final identified parts
                        day?.let { cal.set(Calendar.DAY_OF_MONTH, it) }
                        monthStr?.let { m ->
                            val months = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
                            val mIndex = months.indexOf(m.lowercase().take(3))
                            if (mIndex != -1) cal.set(Calendar.MONTH, mIndex)
                            else m.filter { it.isDigit() }.toIntOrNull()?.let { cal.set(Calendar.MONTH, it - 1) }
                        }
                        year?.let { 
                            val y = if (it < 100) 2000 + it else it
                            cal.set(Calendar.YEAR, y)
                        }
                    } else {
                        // Use separate DAY/MONTH/YEAR groups if they were tagged individually
                        day?.let { cal.set(Calendar.DAY_OF_MONTH, it) }
                        monthStr?.let { m ->
                            val months = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
                            val mIndex = months.indexOf(m.lowercase().take(3))
                            if (mIndex != -1) cal.set(Calendar.MONTH, mIndex)
                            else m.filter { it.isDigit() }.toIntOrNull()?.let { cal.set(Calendar.MONTH, it - 1) }
                        }
                        year?.let {
                            val y = if (it < 100) 2000 + it else it
                            cal.set(Calendar.YEAR, y)
                        }
                    }

                    // Currency logic: Prioritize template capture, then fallback to core detection
                    val finalCurrency = if (capturedCurrency != null) {
                        CurrencyConfig.getSymbol(capturedCurrency!!)
                    } else {
                        findTotalDueWithCurrency(body)?.second ?: "₹"
                    }

                    // Bank name: Prioritize Sender ID mapping so templates stay bank-agnostic
                    val bankNameFromSender = if (sender != null) BankConfig.getBankName(sender) else null
                    val finalBankName = if (bankNameFromSender == null || bankNameFromSender == "Bank") {
                        template.billData.bankName
                    } else {
                        bankNameFromSender
                    }

                    return DueDate(
                        bankName = finalBankName,
                        cardName = cardName ?: template.billData.cardName,
                        cardNumber = cardNum ?: template.billData.cardNumber,
                        amount = tad!!,
                        minAmount = mad,
                        currencySymbol = finalCurrency,
                        dueDate = cal.timeInMillis,
                        receivedDate = receivedDate
                    )
                }
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    private fun findTotalDueWithCurrency(body: String): Pair<Double, String>? {
        val currencyRegex = CurrencyConfig.getCurrencyRegex()
        val patterns = listOf(
            "(?i)Total\\s*(?:amount|amt)?\\s*Due(?:[:\\s]+(?:is|of|amounting to)?)?[:\\s]*($currencyRegex)?[:\\s]*(-?[\\d,]+\\.?\\d*)",
            "(?i)Amount\\s+Due(?:[:\\s]+(?:is|of|amounting to)?)?[:\\s]*($currencyRegex)?[:\\s]*(-?[\\d,]+\\.?\\d*)",
            "(?i)bill of\\s*($currencyRegex)?\\s*(-?[\\d,]+\\.?\\d*)",
            "(?i)Pay\\s*($currencyRegex)?\\s*(-?[\\d,]+\\.?\\d*)",
            "(?i)TAD[:\\s]*($currencyRegex)?[:\\s]*(-?[\\d,]+\\.?\\d*)",
            "(?i)Total\\s+Due\\s+($currencyRegex)\\s*(-?[\\d,]+\\.?\\d*)",
            "(?i)Total\\s+Due(?:[:\\s]+(?:is|of|amounting to)?)?[:\\s]*(-?[\\d,]+\\.?\\d*)"
        )
        
        for (p in patterns) {
            val matcher = Pattern.compile(p, Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(body)
            if (matcher.find()) {
                val symbolOrCodeMatch = matcher.group(1)
                val amountMatch = matcher.group(2) ?: matcher.group(1)
                val finalAmount = if (amountMatch != null && amountMatch.any { it.isDigit() }) {
                    amountMatch.replace(",", "").toDoubleOrNull()
                } else null
                
                if (finalAmount != null) {
                    val rawMatch = symbolOrCodeMatch?.trim() ?: "₹"
                    val symbol = CurrencyConfig.getSymbol(rawMatch)
                    return finalAmount to symbol
                }
            }
        }
        return null
    }

    private fun findMinDue(body: String): Double? {
        val currencyRegex = CurrencyConfig.getCurrencyRegex()
        val patterns = listOf(
            "(?i)Min(?:imum)?\\s*(?:amount|amt)?\\s*due(?:[:\\s]+(?:is|of|amounting to)?)?[:\\s]*($currencyRegex)?[:\\s]*(-?[\\d,]+\\.?\\d*)",
            "(?i)Min\\.?(?:imum)?\\s*(?:amount|amt)?\\s*due(?:[:\\s]+(?:is|of|amounting to)?)?[:\\s]*($currencyRegex)?[:\\s]*(-?[\\d,]+\\.?\\d*)",
            "(?i)MAD[:\\s]*($currencyRegex)?[:\\s]*(-?[\\d,]+\\.?\\d*)",
            "(?i)Min\\s+Due\\s+Rs\\s*(-?[\\d,]+\\.?\\d*)"
        )
        for (p in patterns) {
            val matcher = Pattern.compile(p, Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(body)
            if (matcher.find()) {
                val match = matcher.group(2) ?: matcher.group(1) ?: continue
                return match.replace(",", "").toDoubleOrNull()
            }
        }
        return null
    }

    private fun findDueDate(context: Context, body: String, receivedDate: Long, currencySymbol: String): Long {
        // Handle ordinal suffixes (1st, 2nd...)
        val cleanedBody = body.replace(Regex("(\\d+)(st|nd|rd|th)", RegexOption.IGNORE_CASE), "$1")

        // Standard patterns using word boundaries (\b) to prevent partial year matches (the "26 May" bug)
        // Also added yyyy/MM/dd and yyyy-MM-dd as these are common standard formats
        val patterns = listOf(
            "\\b(\\d{4}/\\d{1,2}/\\d{1,2})\\b",
            "\\b(\\d{4}-\\d{1,2}-\\d{1,2})\\b",
            "\\b(\\d{1,2}-\\d{1,2}-\\d{4})\\b",
            "\\b(\\d{1,2}/\\d{1,2}-\\d{2})\\b",
            "\\b(\\d{1,2}-\\w{3,9}-\\d{4})\\b",
            "\\b(\\d{1,2}-\\w{3,9}-\\d{2})\\b",
            "\\b(\\d{1,2}/\\d{1,2}/\\d{4})\\b",
            "\\b(\\d{1,2}/\\d{1,2}/\\d{2})\\b",
            "\\b(\\d{1,2}/\\w{3,9}/\\d{4})\\b",
            "\\b(\\d{1,2}/\\w{3,9}/\\d{2})\\b",
            "\\b(\\d{1,2}\\s+\\w{3,9},?\\s+\\d{4})\\b",
            "\\b(\\d{1,2}\\s+\\w{3,9},?\\s+\\d{2})\\b",
            "\\b(\\w{3,9}\\s+\\d{1,2},?\\s+\\d{4})\\b",
            "\\b(\\w{3,9}\\s+\\d{1,2},?\\s+\\d{2})\\b",
            "\\b(\\d{1,2}\\s+\\w{3,9})\\b",
            "(?<![/\\-])\\b(\\d{1,2}/\\d{1,2})\\b(?![/\\-])", // Lookaround to prevent matching parts of a full date
            "(?i)Due\\s+by\\s+(\\d{1,2}-\\d{1,2}-\\d{4})",
            "(?i)Due\\s+by\\s+(\\d{1,2}-\\d{1,2}-\\d{2})",
            "(?i)Due\\s+by\\s+(\\d{1,2}/\\d{1,2}/\\d{4})",
            "(?i)Due\\s+by\\s+(\\d{1,2}/\\d{1,2}/\\d{2})",
            "(?i)due\\s+on\\s+(\\d{1,2}/\\d{1,2})"
        )
        
        val sharedPrefs = context.getSharedPreferences("due_date_prefs", Context.MODE_PRIVATE)
        val formatPref = sharedPrefs.getString("date_format_preference", DateFormatPreference.DAY_MONTH.name)
        val isMonthDay = formatPref == DateFormatPreference.MONTH_DAY.name
        
        val dateFormats = if (isMonthDay) {
            listOf(
                "MM-dd-yyyy", "MM/dd/yyyy", "M/d/yyyy", "MM-dd-yy", "MM/dd/yy", "M/d/yy", "MM/dd", "M/d",
                "dd-MM-yyyy", "dd-MMM-yyyy", "dd-MMMM-yyyy", "dd-MM-yy", "dd-MMM-yy", "dd-MMMM-yy", "dd/MM/yyyy", "dd/MMM/yyyy", "dd/MMMM/yyyy", "dd/MM/yy", "d/M/yy", "dd MMM yyyy", "dd MMMM yyyy", "dd MMM yy", "dd MMMM yy", "MMM dd yyyy", "MMMM dd yyyy", "dd MMM", "dd MMMM", "MMM dd", "MMMM dd", "dd MMM, yyyy", "dd MMMM, yyyy", "dd MMM, yy", "dd MMMM, yy", "dd/MM", "d/M",
                "yyyy/MM/dd", "yyyy-MM-dd", "yyyy/M/d", "yyyy-M-d"
            )
        } else {
            listOf(
                "dd-MM-yyyy", "dd/MM/yyyy", "d/M/yyyy", "dd-MM-yy", "dd/MM/yy", "d/M/yy", "dd/MM", "d/M",
                "dd-MMM-yyyy", "dd-MMMM-yyyy", "dd-MMM-yy", "dd-MM-yy", "dd-MMMM-yy", "dd/MMM/yyyy", "dd/MMMM/yyyy", "dd MMM yyyy", "dd MMMM yyyy", "dd MMM yy", "dd MMMM yy", "MMM dd yyyy", "MMMM dd yyyy", "dd MMM", "dd MMMM", "MMM dd", "MMMM dd", "dd MMM, yyyy", "dd MMMM, yyyy", "dd MMM, yy", "dd MMMM, yy",
                "yyyy/MM/dd", "yyyy-MM-dd", "yyyy/M/d", "yyyy-M-d"
            )
        }

        for (p in patterns) {
            val matcher = Pattern.compile(p, Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(cleanedBody)
            while (matcher.find()) {
                val rawDateStr = matcher.group(1) ?: continue
                val dateStr = rawDateStr.replace(",", "").replace(".", "")
                for (format in dateFormats) {
                    try {
                        val sdf = SimpleDateFormat(format, Locale.US)
                        sdf.isLenient = false
                        val date = sdf.parse(dateStr)
                        if (date != null) {
                            val cal = Calendar.getInstance()
                            val currentYear = cal.get(Calendar.YEAR)
                            cal.time = date
                            if (cal.get(Calendar.YEAR) < 2000) { 
                                cal.set(Calendar.YEAR, currentYear)
                            }
                            return cal.timeInMillis
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }
            }
        }
        // Fallback: Default to receiving timestamp
        return receivedDate
    }

    private fun findCardName(body: String, bankName: String): String? {
        val keywords = mapOf(
            "FIRST WOW!" to "FIRST.*?WOW",
            "OneCard" to "OneCard|BOB.*?One|BOBCARD.*?One",
            "Stable Money" to "Stable.*?Money|StableMoney",
            "SuperCard" to "SuperCard",
            "IDFC FIRST" to "IDFC.*?FIRST"
        )
        for ((name, pattern) in keywords) {
            if (Regex(pattern, setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).containsMatchIn(body)) {
                return name
            }
        }

        val genericPattern = "(?i)(?:Your|Dear|Hi|\\b$bankName\\b|statement\\s+for)?\\s*\\b([A-Z0-9! ]{2,20})\\s+Credit\\s+Card"
        val match = Regex(genericPattern).find(body)
        var extracted = match?.groups?.get(1)?.value?.trim()

        if (extracted != null) {
            val noiseWords = listOf("Your", "Dear", "Generated", "eStatement", "Statement", "statement", "Bank", bankName, "is", "for", "the", "an", "on", "of", "account").map { it.lowercase() }
            val words = extracted.split(" ").filter { it.lowercase() !in noiseWords && it.isNotBlank() }
            if (words.isNotEmpty()) {
                extracted = words.joinToString(" ")
                if (extracted.startsWith(bankName, ignoreCase = true)) {
                    extracted = extracted.substring(bankName.length).trim()
                }
                if (extracted.isEmpty() || extracted.equals(bankName, ignoreCase = true) || extracted.all { it.isDigit() }) return null
                return extracted
            }
        }
        return null
    }

    private fun findCardNumber(body: String): String? {
        val priorityPatterns = listOf(
            "XX\\s*(\\d{4})",
            "X\\s*(\\d{4})",
            "\\*\\*\\s*(\\d{4})",
            "xx(\\d{4})"
        )
        for (p in priorityPatterns) {
            val match = Regex(p, RegexOption.IGNORE_CASE).find(body)
            val num = match?.groups?.get(1)?.value
            if (num != null) return num
        }

        val secondaryPatterns = listOf(
            "Card\\s*[X\\*\\s]*(\\d{4})",
            "ending\\s+(?:in|with)\\s+(\\d{4})",
            "account\\s*[X\\*\\s]*(\\d{4})",
            "a/c\\s*no\\.?\\s*[X\\*\\s]*(\\d{4})"
        )
        for (p in secondaryPatterns) {
            val regex = Regex(p, RegexOption.IGNORE_CASE)
            val matches = regex.findAll(body)
            for (match in matches) {
                val num = match.groups[1]?.value ?: continue
                val numInt = num.toIntOrNull()
                if (numInt != null && numInt !in 2024..2035) {
                    return num
                }
            }
        }
        return null
    }
}
