package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val isAmoledBlack: Boolean = false,
    val useDynamicColors: Boolean = false,
    val accentColorIndex: Int = 0,
    val customAccentColor: Long = 0xFF2E7D32, // Primary accent (default Emerald/Forest)
    val quickTheme: String = "MINT", // "MINT", "SUNSET", "OCEAN", "SAKURA", "MIDNIGHT", "MOCHA", "CUSTOM"
    val cardStyle: String = "TINT", // "SOLID", "TINT", "OUTLINE"
    val bgStyle: String = "DEFAULT", // "AMOLED", "DARK_GRAY", "DARK_TINT", "PURE_WHITE", "LIGHT_GRAY", "LIGHT_TINT", "DEFAULT"
    val cornerRadius: String = "ROUNDED", // "ROUNDED" (28dp), "MEDIUM" (16dp), "STRAIGHT" (4dp)
    val density: String = "COMFORTABLE", // "COMFORTABLE", "COMPACT"
    val numberFont: String = "ROUNDED", // "DEFAULT", "ROUNDED", "MONOSPACE"
    val incomeColor: Long = 0xFF2E7D32,
    val expenseColor: Long = 0xFFC62828,
    val savingsColor: Long = 0xFF0288D1,
    val isColorblindPreset: Boolean = false,
    val currencyCode: String = "MXN",
    val currencySymbol: String = "$",
    val isFingerprintEnabled: Boolean = false,
    val isPinEnabled: Boolean = false,
    val pinCode: String = "",
    val remindersEnabled: Boolean = false,
    val reminderTime: String = "20:00",
    val reminderFrequency: String = "DAILY", // "DAILY", "WEEKDAYS", "WEEKENDS"
    val hapticLevel: String = "MEDIUM" // "OFF", "LOW", "MEDIUM", "HIGH"
)
