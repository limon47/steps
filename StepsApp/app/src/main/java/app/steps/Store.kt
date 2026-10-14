package app.steps

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Store {
    private fun p(c: Context) = c.getSharedPreferences("steps", Context.MODE_PRIVATE)
    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    fun get(c: Context, day: String): Int = p(c).getInt("d_$day", 0)
    fun set(c: Context, day: String, v: Int) { p(c).edit().putInt("d_$day", v).apply() }

    fun days(c: Context): List<Pair<String, Int>> =
        p(c).all.filterKeys { it.startsWith("d_") }
            .map { it.key.removePrefix("d_") to (it.value as Int) }
            .sortedByDescending { it.first }

    // Датчик віддає суму кроків від останнього перезавантаження телефона.
    fun onSensor(c: Context, total: Long) {
        val pr = p(c)
        val last = pr.getLong("last", -1L)
        val delta = when {
            last < 0 -> 0L
            total >= last -> total - last
            else -> total // телефон перезавантажувався
        }
        pr.edit().putLong("last", total).apply()
        if (delta > 0) set(c, today(), get(c, today()) + delta.toInt())
    }
}
