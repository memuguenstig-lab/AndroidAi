package com.example.viewmodel

import android.content.Context
import android.content.SharedPreferences

enum class ModelProvider {
    GEMINI,
    GROQ,
    LOCAL_ON_DEVICE
}

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var geminiApiKey: String
        get() = prefs.getString("gemini_api_key", "") ?: ""
        set(value) = prefs.edit().putString("gemini_api_key", value).apply()

    var groqApiKey: String
        get() = prefs.getString("groq_api_key", "") ?: ""
        set(value) = prefs.edit().putString("groq_api_key", value).apply()

    var localModelPath: String
        get() = prefs.getString("local_model_path", "/sdcard/Download/gemma-2b-it-cpu-int4.task") ?: "/sdcard/Download/gemma-2b-it-cpu-int4.task"
        set(value) = prefs.edit().putString("local_model_path", value).apply()

    var provider: ModelProvider
        get() = ModelProvider.valueOf(prefs.getString("provider", ModelProvider.GEMINI.name) ?: ModelProvider.GEMINI.name)
        set(value) = prefs.edit().putString("provider", value.name).apply()

    var erfinderMode: Boolean
        get() = prefs.getBoolean("erfinder_mode", false)
        set(value) = prefs.edit().putBoolean("erfinder_mode", value).apply()
}
