package com.mateyou.duedate.data

data class CustomTemplate(
    val id: String,
    val name: String,
    val rawSms: String,
    val patternRegex: String,
    val groupMappings: Map<Int, String>, // Group Index -> Field (TAD, MAD, DAY, MONTH, YEAR, CARD_NAME, CARD_NUM, DATE_RAW)
    val billData: DueDate,
    val dateOrder: String? = null // e.g., "YEAR,MONTH,DAY" or "DAY,MONTH,YEAR"
)
