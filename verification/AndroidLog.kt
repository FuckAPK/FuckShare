package android.util
object Log {
    @JvmStatic fun isLoggable(tag: String, level: Int) = false
    @JvmStatic fun d(tag: String, message: String) = 0
}
