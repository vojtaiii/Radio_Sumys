package cz.sumys.rdiosum.applications

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.jakewharton.threetenabp.AndroidThreeTen

class SumysApplication: Application() {
    //private lateinit var log: Logger

    /**
     * Called upon the very start of the application.
     * Here used for creating the channels for notifications.
     */
    override fun onCreate() {
        super.onCreate()

        //createLogFolder()
        AndroidThreeTen.init(this)
        createNotificationChannels()
        //log.debug("Sumys application started")
    }

    // ---------------------------------------------------------------------------------------------

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // channels do not work on lesser versions
            val channelSong = NotificationChannel(
                    CHANNEL_1_ID, "sumys song", NotificationManager.IMPORTANCE_DEFAULT)
            channelSong.description = "Notification for radio Sumys playback"
            channelSong.lockscreenVisibility = Notification.VISIBILITY_PUBLIC


            val manager: NotificationManager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channelSong)
        }
    }

    /**
     * Creates log folder and initializes foo.log and logger
     * Do nothing if already created
     */
//    private fun createLogFolder() {
//        val path = applicationContext.getExternalFilesDir("")!!.absolutePath + "/" +
//                "SumysApplication" + "/" + "log"
//        if (!File(path).exists()) {
//            try {
//                val res = File(path).mkdirs()
//                log = LoggerFactory.getLogger(SumysApplication::class.java)
//                if (res) log.debug("Log directory created")
//                else log.error("Unable to create log folder")
//            } catch (e: SecurityException) {
//                log = LoggerFactory.getLogger(SumysApplication::class.java)
//                log.error("Unable to crate log folder", e)
//
//            }
//
//        }
//        else log = LoggerFactory.getLogger(SumysApplication::class.java)
//    }

    // ---------------------------------------------------------------------------------------------

    companion object {
        const val CHANNEL_1_ID = "sumys_song"
    }
}