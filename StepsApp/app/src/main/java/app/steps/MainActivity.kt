package app.steps

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private lateinit var count: TextView
    private lateinit var sub: TextView
    private lateinit var list: LinearLayout
    private val h = Handler(Looper.getMainLooper())
    private var sig = ""
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
        val fix = Button(this).apply { text = "Виправити сьогодні"; setOnClickListener { edit(Store.today()) } }
        val head = TextView(this).apply { text = "Історія (натисни день, щоб виправити)"; setPadding(0, dp(24), 0, dp(8)) }
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(count); root.addView(sub); root.addView(fix); root.addView(head); root.addView(list)
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
        val t = Store.get(this, Store.today())
        count.text = "%,d".format(t)
        sub.text = "кроків сьогодні · %.2f км".format(t * 0.00075)
        val days = Store.days(this)
        val s = days.toString()
        if (s == sig) return
        sig = s
        list.removeAllViews()
        for ((d, n) in days.take(90)) {
            list.addView(TextView(this).apply {
                text = "$d      %,d".format(n)
                textSize = 18f
                setPadding(0, dp(10), 0, dp(10))
                setOnClickListener { edit(d) }
            })
        }
    }

    override fun onResume() { super.onResume(); h.post(tick) }
    override fun onPause() { h.removeCallbacks(tick); super.onPause() }
}
