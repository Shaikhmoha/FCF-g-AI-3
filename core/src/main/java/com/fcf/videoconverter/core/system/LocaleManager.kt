package com.fcf.videoconverter.core.system

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

enum class AppLanguage(val tag: String, val displayName: String) {
    ENGLISH("en", "English"),
    BANGLA("bn", "বাংলা")
}

object LocaleManager {
    fun applyLanguage(language: AppLanguage) {
        val locales = LocaleListCompat.forLanguageTags(language.tag)
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
