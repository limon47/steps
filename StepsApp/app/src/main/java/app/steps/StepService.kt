package app.steps

import android.app.*
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder

class StepService : Service(), SensorEventListener {
    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("steps", "Кроки", NotificationManager.IMPORTANCE_LOW))
        startForeground(1, notif())
        val sm = getSystemService(SensorManager::class.java)
        sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let {
            sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL, 30_000_000)
        }
    }

    private fun notif(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, "steps")
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentTitle("Кроки сьогодні: ${Store.get(this, Store.today())}")
            .setOngoing(true)
            .setContentIntent(open)
            .build()
    }

    override fun onSensorChanged(e: SensorEvent) {
        Store.onSensor(this, e.values[0].toLong())
        getSystemService(NotificationManager::class.java).notify(1, notif())
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
    override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_STICKY
    override fun onDestroy() {
        getSystemService(SensorManager::class.java).unregisterListener(this)
        super.onDestroy()
    }
}
