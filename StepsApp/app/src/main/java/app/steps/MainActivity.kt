package app.steps

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : Activity() {
    private val START = "2026-10-01"
    private val END = "2026-11-30"
    private val MIN = 7000

    private lateinit var count: TextView
    private lateinit var sub: TextView
    private lateinit var summary: TextView
    private lateinit var list: LinearLayout
    private val h = Handler(Looper.getMainLooper())
    private var sig = ""
    private var scrolled = false
    private val tick = object : Runnable {
        override fun run() { render(); h.postDelayed(this, 1000) }
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(40), dp(20), dp(20))
        }
        count = TextView(this).apply { textSize = 72f; gravity = Gravity.CENTER; setTypeface(null, Typeface.BOLD) }
        sub = TextView(this).apply { textSize = 16f; gravity = Gravity.CENTER }
        summary = TextView(this).apply { textSize = 16f; gravity = Gravity.CENTER; setPadding(0, dp(8), 0, 0) }
        val fix = Button(this).apply { text = "Виправити сьогодні"; setOnClickListener { edit(Store.today()) } }
        val head = TextView(this).apply { text = "Історія (натисни день, щоб виправити)"; setPadding(0, dp(24), 0, dp(8)) }
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(count); root.addView(sub); root.addView(summary); root.addView(fix); root.addView(head); root.addView(list)
        setContentView(ScrollView(this).apply { addView(root) })
        startCounting()
    }

    private fun startCounting() {
        val need = mutableListOf(Manifest.permission.ACTIVITY_RECOGNITION)
        if (Build.VERSION.SDK_INT >= 33) need.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = need.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) startSvc() else requestPermissions(missing.toTypedArray(), 1)
    }

    private fun startSvc() {
        if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED)
            try { startForegroundService(Intent(this, StepService::class.java)) } catch (e: Exception) {}
    }

    override fun onRequestPermissionsResult(r: Int, p: Array<String>, g: IntArray) {
        super.onRequestPermissionsResult(r, p, g)
        startSvc()
    }

    private fun rangeDays(): List<String> {
        val f = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val c = Calendar.getInstance()
        c.time = f.parse(START)!!
        val out = mutableListOf<String>()
        while (true) {
            val d = f.format(c.time)
            out.add(d)
            if (d >= END) break
            c.add(Calendar.DAY_OF_MONTH, 1)
        }
        return out
    }

    private fun edit(day: String) {
        val et = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(Store.get(this@MainActivity, day).toString())
        }
        AlertDialog.Builder(this).setTitle(day).setView(et)
            .setPositiveButton("Зберегти") { _, _ ->
                Store.set(this, day, et.text.toString().toIntOrNull() ?: 0)
                sig = ""; render()
            }
            .setNegativeButton("Скасувати", null).show()
    }

    private fun render() {
        val today = Store.today()
        val t = Store.get(this, today)
        count.text = "%,d".format(t)
        sub.text = "кроків сьогодні · %.2f км".format(t * 0.00075)

        val days = rangeDays()
        val steps = days.map { Store.get(this, it) }
        val met = days.indices.count { days[it] <= today && steps[it] >= MIN }
        val passed = days.count { it <= today }
        summary.text = "Норма %,d кроків: виконано %d з %d днів".format(MIN, met, passed)

        val s = today + steps.toString()
        if (s == sig) return
        sig = s
        list.removeAllViews()
        var todayRow: TextView? = null
        for (i in days.indices) {
            val d = days[i]
            val n = steps[i]
            val future = d > today
            val mark = when {
                future -> "·"
                n >= MIN -> "✓"
                d < today -> "✗"
                else -> "…"
            }
            val color = when {
                future -> 0xFF888888.toInt()
                n >= MIN -> 0xFF2E9E5B.toInt()
                d < today -> 0xFFD9534F.toInt()
                else -> null
            }
            val row = TextView(this).apply {
                text = "${d.substring(8)}.${d.substring(5, 7)}      %,d      $mark".format(n)
                textSize = 18f
                setPadding(0, dp(10), 0, dp(10))
                if (color != null) setTextColor(color)
                if (d == today) setTypeface(null, Typeface.BOLD)
                if (!future) setOnClickListener { edit(d) }
            }
            if (d == today) todayRow = row
            list.addView(row)
        }
        if (!scrolled) {
            scrolled = true
            todayRow?.let { r -> r.post { r.requestRectangleOnScreen(Rect(0, 0, r.width, r.height)) } }
        }
    }

    override fun onResume() { super.onResume(); h.post(tick) }
    override fun onPause() { h.removeCallbacks(tick); super.onPause() }
}
