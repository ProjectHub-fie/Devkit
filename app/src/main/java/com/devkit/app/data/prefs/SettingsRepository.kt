package com.devkit.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "devkit_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AccentChoice { VIOLET, BLUE, TEAL, AMBER, ROSE, GREEN }

enum class HistoryRetention(val days: Int, val label: String) {
    DAYS_1(1, "1 day"),
    DAYS_7(7, "7 days"),
    DAYS_30(30, "30 days"),
    DAYS_90(90, "90 days"),
    FOREVER(0, "Keep forever"),
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentChoice = AccentChoice.VIOLET,
    val dynamicColor: Boolean = false,
    val jsonIndent: Int = 2,
    val defaultTimeoutSeconds: Int = 30,
    val historyRetention: HistoryRetention = HistoryRetention.DAYS_30,
    val followRedirects: Boolean = true,
    val warnOnCrossOriginSecrets: Boolean = true,
    val wrapResponseBody: Boolean = true,
    val fontSizeScale: Float = 1.0f,
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val ACCENT = stringPreferencesKey("accent")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val INDENT = intPreferencesKey("json_indent")
        val TIMEOUT = intPreferencesKey("default_timeout")
        val RETENTION = stringPreferencesKey("history_retention")
        val REDIRECTS = booleanPreferencesKey("follow_redirects")
        val WARN_ORIGIN = booleanPreferencesKey("warn_cross_origin")
        val WRAP = booleanPreferencesKey("wrap_response")
        val FONT_SCALE = stringPreferencesKey("font_scale")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            themeMode = prefs[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            accent = prefs[Keys.ACCENT]?.let { runCatching { AccentChoice.valueOf(it) }.getOrNull() }
                ?: AccentChoice.VIOLET,
            dynamicColor = prefs[Keys.DYNAMIC] ?: false,
            jsonIndent = (prefs[Keys.INDENT] ?: 2).coerceIn(1, 8),
            defaultTimeoutSeconds = (prefs[Keys.TIMEOUT] ?: 30).coerceIn(1, 120),
            historyRetention = prefs[Keys.RETENTION]?.let {
                runCatching { HistoryRetention.valueOf(it) }.getOrNull()
            } ?: HistoryRetention.DAYS_30,
            followRedirects = prefs[Keys.REDIRECTS] ?: true,
            warnOnCrossOriginSecrets = prefs[Keys.WARN_ORIGIN] ?: true,
            wrapResponseBody = prefs[Keys.WRAP] ?: true,
            fontSizeScale = prefs[Keys.FONT_SCALE]?.toFloatOrNull()?.coerceIn(0.8f, 1.6f) ?: 1.0f,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = mode.name }
    suspend fun setAccent(accent: AccentChoice) = context.dataStore.edit { it[Keys.ACCENT] = accent.name }
    suspend fun setDynamicColor(enabled: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC] = enabled }
    suspend fun setJsonIndent(indent: Int) = context.dataStore.edit { it[Keys.INDENT] = indent.coerceIn(1, 8) }
    suspend fun setDefaultTimeout(seconds: Int) =
        context.dataStore.edit { it[Keys.TIMEOUT] = seconds.coerceIn(1, 120) }
    suspend fun setHistoryRetention(retention: HistoryRetention) =
        context.dataStore.edit { it[Keys.RETENTION] = retention.name }
    suspend fun setFollowRedirects(enabled: Boolean) = context.dataStore.edit { it[Keys.REDIRECTS] = enabled }
    suspend fun setWarnOnCrossOriginSecrets(enabled: Boolean) =
        context.dataStore.edit { it[Keys.WARN_ORIGIN] = enabled }
    suspend fun setWrapResponseBody(enabled: Boolean) = context.dataStore.edit { it[Keys.WRAP] = enabled }
    suspend fun setFontScale(scale: Float) =
        context.dataStore.edit { it[Keys.FONT_SCALE] = scale.coerceIn(0.8f, 1.6f).toString() }
}
