package cz.sumys.rdiosum

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class BackgroundSumysService: Service() {
    val log: Logger = LoggerFactory.getLogger(MainActivity::class.java)
    private lateinit var player: MediaPlayer

    private lateinit var mHandlerThread: HandlerThread
    private lateinit var mHandler: Handler

    override fun onCreate() {
        log.debug("BackgroundSumysService created")
        startForeground(1, createSongNotification(applicationContext, "Rádio Sumýš", "Blbě čumíš"))
        player = MediaPlayer()
        setMediaPlayer(applicationContext)

        //creates a thread
        mHandlerThread = HandlerThread("sumys.playbackThread")
        mHandlerThread.start()
        mHandler = Handler(mHandlerThread.looper)
    }

    /**
     * Start the audio stream ON SEPARATE thread using HandlerThread
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mHandler.post {
            try {
                log.debug("Stream initialized")
                player.setDataSource(TitleViewModel.STREAM_URL)
                player.prepare()
                player.start()
                fireSpinningIntent()
            } catch (e: Exception) {
                log.error("Failed to start stream, ${e.message}")
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        try {
            player.pause()
            player.reset()
            player.release()
            log.debug("Stream released")
        } catch (e: Exception) {
            log.error("Failed to release stream, ${e.message}")
        }
    }

    private fun fireSpinningIntent() {
        log.debug("Sending spinning intent")
        val intent = Intent("spinning")
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    private fun setMediaPlayer(context: Context) {
        // set wake lock so the phone cpu is still ready while streaming
        player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
        player.setOnErrorListener { _, what, extra ->
            log.debug("Media Player error = WHAT: $what EXTRA: $extra")
            false
        }
    }

    private fun createSongNotification(context: Context, title: String, message: String): Notification? {
        // content intent when user presses the notification body
        val activityIntent = Intent(context, MainActivity::class.java)
        activityIntent.action = Intent.ACTION_MAIN
        activityIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val contentIntent = PendingIntent.getActivity(context, 0, activityIntent, 0)

        // action intent for stop button
        val actionStopIntent = Intent(context, ActionReceiver::class.java)
        actionStopIntent.putExtra("action", "stop")
        val pActionStopIntent = PendingIntent.getBroadcast(context,
                1, actionStopIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        // action intent for notification dismiss
        val actionDeleteIntent = Intent(context, ActionReceiver::class.java)
        actionDeleteIntent.putExtra("action", "delete")
        val pActionDeleteIntent = PendingIntent.getBroadcast(context,
                1, actionDeleteIntent, 0)

        // picture and its bitmap
        val picture = BitmapFactory.decodeResource(resources, R.drawable.sumys_notification)

        val mediaSession = MediaSessionCompat(context, "sumys_tag")

        // define the notification looks
        return context.let {
            NotificationCompat.Builder(it, SumysApplication.CHANNEL_1_ID)
                .setSmallIcon(R.drawable.ic_sumys_icon)
                .setLargeIcon(picture)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.sessionToken)
                )
                .setContentIntent(contentIntent)
                .addAction(R.drawable.ic_notif_stop, "stop", pActionStopIntent)
                .setDeleteIntent(pActionDeleteIntent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}