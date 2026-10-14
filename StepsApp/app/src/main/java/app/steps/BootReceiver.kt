package app.steps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        try { c.startForegroundService(Intent(c, StepService::class.java)) } catch (e: Exception) {}
    }
}
