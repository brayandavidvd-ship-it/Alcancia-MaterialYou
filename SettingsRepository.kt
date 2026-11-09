package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.db.SettingsDao
import com.example.data.model.AppSettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val settingsDao: SettingsDao,
    private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "alcancia_settings_cache"
        private const val KEY_CACHED = "has_cached_settings"

        fun getCachedSettings(context: Context): AppSettingsEntity {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_CACHED, false)) {
                return AppSettingsEntity()
            }
            return AppSettingsEntity(
                themeMode = prefs.getString("themeMode", "SYSTEM") ?: "SYSTEM",
                isAmoledBlack = prefs.getBoolean("isAmoledBlack", false),
                useDynamicColors = prefs.getBoolean("useDynamicColors", false),
                accentColorIndex = prefs.getInt("accentColorIndex", 0),
                customAccentColor = prefs.getLong("customAccentColor", 0xFF2E7D32),
                quickTheme = prefs.getString("quickTheme", "MINT") ?: "MINT",
                cardStyle = prefs.getString("cardStyle", "TINT") ?: "TINT",
                bgStyle = prefs.getString("bgStyle", "DEFAULT") ?: "DEFAULT",
                cornerRadius = prefs.getString("cornerRadius", "ROUNDED") ?: "ROUNDED",
                density = prefs.getString("density", "COMFORTABLE") ?: "COMFORTABLE",
                numberFont = prefs.getString("numberFont", "ROUNDED") ?: "ROUNDED",
                incomeColor = prefs.getLong("incomeColor", 0xFF2E7D32),
                expenseColor = prefs.getLong("expenseColor", 0xFFC62828),
                savingsColor = prefs.getLong("savingsColor", 0xFF0288D1),
                isColorblindPreset = prefs.getBoolean("isColorblindPreset", false),
                currencyCode = prefs.getString("currencyCode", "MXN") ?: "MXN",
                currencySymbol = prefs.getString("currencySymbol", "$") ?: "$",
                isFingerprintEnabled = prefs.getBoolean("isFingerprintEnabled", false),
                isPinEnabled = prefs.getBoolean("isPinEnabled", false),
                pinCode = prefs.getString("pinCode", "") ?: "",
                remindersEnabled = prefs.getBoolean("remindersEnabled", false),
                reminderTime = prefs.getString("reminderTime", "20:00") ?: "20:00",
                reminderFrequency = prefs.getString("reminderFrequency", "DAILY") ?: "DAILY",
                hapticLevel = prefs.getString("hapticLevel", "MEDIUM") ?: "MEDIUM"
            )
        }

        fun saveToCache(context: Context, settings: AppSettingsEntity) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString("themeMode", settings.themeMode)
                .putBoolean("isAmoledBlack", settings.isAmoledBlack)
                .putBoolean("useDynamicColors", settings.useDynamicColors)
                .putInt("accentColorIndex", settings.accentColorIndex)
                .putLong("customAccentColor", settings.customAccentColor)
                .putString("quickTheme", settings.quickTheme)
                .putString("cardStyle", settings.cardStyle)
                .putString("bgStyle", settings.bgStyle)
                .putString("cornerRadius", settings.cornerRadius)
                .putString("density", settings.density)
                .putString("numberFont", settings.numberFont)
                .putLong("incomeColor", settings.incomeColor)
                .putLong("expenseColor", settings.expenseColor)
                .putLong("savingsColor", settings.savingsColor)
                .putBoolean("isColorblindPreset", settings.isColorblindPreset)
                .putString("currencyCode", settings.currencyCode)
                .putString("currencySymbol", settings.currencySymbol)
                .putBoolean("isFingerprintEnabled", settings.isFingerprintEnabled)
                .putBoolean("isPinEnabled", settings.isPinEnabled)
                .putString("pinCode", settings.pinCode)
                .putBoolean("remindersEnabled", settings.remindersEnabled)
                .putString("reminderTime", settings.reminderTime)
                .putString("reminderFrequency", settings.reminderFrequency)
                .putString("hapticLevel", settings.hapticLevel)
                .putBoolean(KEY_CACHED, true)
                .commit()
        }
    }

    fun getCachedSettings(): AppSettingsEntity {
        return getCachedSettings(context)
    }

    val settings: Flow<AppSettingsEntity> = settingsDao.getSettings().map { entity ->
        val current = entity ?: getCachedSettings()
        saveToCache(context, current)
        current
    }

    suspend fun getSettingsSync(): AppSettingsEntity {
        return settingsDao.getSettingsSync() ?: getCachedSettings()
    }

    suspend fun updateSettings(settings: AppSettingsEntity) {
        saveToCache(context, settings)
        settingsDao.setSettings(settings)
    }

    suspend fun setThemeMode(mode: String) {
        val current = getSettingsSync()
        updateSettings(current.copy(themeMode = mode))
    }

    suspend fun setAmoledBlack(enabled: Boolean) {
        val current = getSettingsSync()
        updateSettings(current.copy(isAmoledBlack = enabled))
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        val current = getSettingsSync()
        updateSettings(current.copy(useDynamicColors = enabled))
    }

    suspend fun setAccentColorIndex(index: Int, customColor: Long = 0L) {
        val current = getSettingsSync()
        val updated = if (index >= 0) {
            current.copy(
                accentColorIndex = index,
                quickTheme = "CUSTOM",
                useDynamicColors = false
            )
        } else {
            current.copy(
                accentColorIndex = -1,
                customAccentColor = customColor,
                quickTheme = "CUSTOM",
                useDynamicColors = false
            )
        }
        updateSettings(updated)
    }

    suspend fun setQuickTheme(theme: com.example.ui.theme.QuickThemeOption) {
        val current = getSettingsSync()
        updateSettings(
            current.copy(
                quickTheme = theme.id,
                accentColorIndex = -1,
                customAccentColor = theme.accentColor.value.toLong(),
                cardStyle = theme.cardStyle,
                bgStyle = theme.bgStyle,
                cornerRadius = theme.cornerRadius,
                useDynamicColors = false
            )
        )
    }

    suspend fun setCardStyle(style: String) {
        val current = getSettingsSync()
        updateSettings(current.copy(cardStyle = style, quickTheme = "CUSTOM"))
    }

    suspend fun setBgStyle(style: String) {
        val current = getSettingsSync()
        updateSettings(
            current.copy(
                bgStyle = style,
                isAmoledBlack = (style == "AMOLED"),
                quickTheme = "CUSTOM",
                useDynamicColors = false
            )
        )
    }

    suspend fun setCornerRadius(radius: String) {
        val current = getSettingsSync()
        updateSettings(current.copy(cornerRadius = radius, quickTheme = "CUSTOM"))
    }

    suspend fun setDensity(density: String) {
        val current = getSettingsSync()
        updateSettings(current.copy(density = density))
    }

    suspend fun setNumberFont(font: String) {
        val current = getSettingsSync()
        updateSettings(current.copy(numberFont = font))
    }

    suspend fun setFinancialColors(income: Long, expense: Long, savings: Long, isColorblind: Boolean = false) {
        val current = getSettingsSync()
        updateSettings(
            current.copy(
                incomeColor = income,
                expenseColor = expense,
                savingsColor = savings,
                isColorblindPreset = isColorblind
            )
        )
    }

    suspend fun setCurrency(code: String, symbol: String) {
        val current = getSettingsSync()
        updateSettings(current.copy(currencyCode = code, currencySymbol = symbol))
    }

    // Security
    suspend fun setFingerprintEnabled(enabled: Boolean) {
        val current = getSettingsSync()
        updateSettings(current.copy(isFingerprintEnabled = enabled))
    }

    suspend fun setPinSecurity(enabled: Boolean, pinCode: String = "") {
        val current = getSettingsSync()
        updateSettings(current.copy(isPinEnabled = enabled, pinCode = pinCode))
    }

    // Reminders
    suspend fun setRemindersConfig(enabled: Boolean, time: String = "20:00", frequency: String = "DAILY") {
        val current = getSettingsSync()
        updateSettings(current.copy(remindersEnabled = enabled, reminderTime = time, reminderFrequency = frequency))
    }

    // Haptics
    suspend fun setHapticLevel(level: String) {
        val current = getSettingsSync()
        updateSettings(current.copy(hapticLevel = level))
    }
}
