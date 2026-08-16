# 本项目 release 未开启混淆（isMinifyEnabled = false）。
# 若将来开启，需要保留 kotlinx.serialization 与 OkHttp 相关规则：
# -keepattributes *Annotation*, InnerClasses
# -dontnote kotlinx.serialization.**
# -keepclassmembers class **$$serializer { *; }
# -keepclasseswithmembers class * { @kotlinx.serialization.Serializable <fields>; }
