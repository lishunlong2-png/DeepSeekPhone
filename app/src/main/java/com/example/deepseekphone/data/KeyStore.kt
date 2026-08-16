package com.example.deepseekphone.data

import android.content.Context

/**
 * API Key 的本地存储。
 *
 * 使用 SharedPreferences（应用私有目录，其他应用读不到）。
 * Key 只存在本机，App 内没有任何地方会把 Key 上传到第三方。
 *
 * 提示：如果希望更高安全性，可升级为 EncryptedSharedPreferences
 * （androidx.security:security-crypto），原理是使用 Android Keystore
 * 的硬件密钥加密后再落盘。
 */
class KeyStore(context: Context) {

    private val prefs = context.getSharedPreferences("deepseek_phone", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_API_KEY, value.trim()).apply()
        }

    private companion object {
        const val KEY_API_KEY = "api_key"
    }
}
