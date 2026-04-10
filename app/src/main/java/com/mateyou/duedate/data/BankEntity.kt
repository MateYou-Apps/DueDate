package com.mateyou.duedate.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_banks")
data class BankEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isBrandedCard: Boolean,
    val senderIds: String, // Comma separated for banks (e.g. "AXISBK, AXISCC")
    val aliases: String,   // Comma separated for cards (e.g. "Super card, Supercard")
    val svgLogo: String? = null, // Store the raw SVG string
    val isDeleted: Boolean = false,
    val isBuiltIn: Boolean = false, // true if this is an override of a hardcoded app bank
    val builtInName: String? = null // The original name of the built-in bank to allow restoration if renamed
)
