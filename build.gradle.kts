// 根构建脚本：声明所有插件版本（apply false = 只声明，不应用）
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0" apply false
}
