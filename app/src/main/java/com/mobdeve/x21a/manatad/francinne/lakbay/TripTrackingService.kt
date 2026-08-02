package com.mobdeve.x21a.manatad.francinne.lakbay

import android.app.Service
import android.content.Intent
import android.os.IBinder
import java.util.Timer
import java.util.TimerTask

class TripTrackingService : Service() {

    private var timer: Timer? = null

    companion object {
        const val ACTION_TRIP_UPDATE = "com.mobdeve.x21a.manatad.francinne.lakbay.TRIP_UPDATE"
        const val EXTRA_ELAPSED_SECONDS = "EXTRA_ELAPSED_SECONDS"
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start background tracking task using Timer
        var elapsedSeconds = 0
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                elapsedSeconds++
                // Broadcast tracking status update to Activity
                val broadcastIntent = Intent(ACTION_TRIP_UPDATE).apply {
                    putExtra(EXTRA_ELAPSED_SECONDS, elapsedSeconds)
                }
                sendBroadcast(broadcastIntent)
            }
        }, 1000, 1000)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Unbound service implementation
        return null
    }

    override fun onDestroy() {
        // Clean up background tasks when service is stopped
        timer?.cancel()
        timer = null
        super.onDestroy()
    }
}