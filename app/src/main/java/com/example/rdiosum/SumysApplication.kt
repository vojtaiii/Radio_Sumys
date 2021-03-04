package com.example.rdiosum

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class SumysApplication: Application() {

    /**
     * Called upon the very start of the application.
     * Here used for creating the channels for notifications.
     */
    override fun onCreate() {
        super.onCreate()

        createNotificationChannels()
    }

    // ---------------------------------------------------------------------------------------------

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // channels do not work on lesser versions
            val channelSong = NotificationChannel(
                    CHANNEL_1_ID, "sumys song", NotificationManager.IMPORTANCE_DEFAULT)
            channelSong.description = "Notification for Sumys playback behaviour"


            val manager: NotificationManager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channelSong)
        }
    }

    // ---------------------------------------------------------------------------------------------

    companion object {
        const val CHANNEL_1_ID = "sumys_song"
    }
}