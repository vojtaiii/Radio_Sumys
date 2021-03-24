package cz.sumys.rdiosum

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.exoplayer2.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class BackgroundSumysService: Service() {
    val log: Logger = LoggerFactory.getLogger(MainActivity::class.java)
    private lateinit var player: SimpleExoPlayer

    private lateinit var mHandlerThread: HandlerThread
    private lateinit var mHandler: Handler

    override fun onCreate() {
        log.debug("BackgroundSumysService created")
        startForeground(1, createSongNotification(applicationContext, "Rádio Sumýš", "Blbě čumíš"))

        //creates a thread
        mHandlerThread = HandlerThread("sumys.playbackThread")
        mHandlerThread.start()
        mHandler = Handler(mHandlerThread.looper)
        mHandler.post {
            // initialize and setup exo player
            player = SimpleExoPlayer.Builder(applicationContext).build()
            // setup error listener
            player.addListener(object : Player.EventListener {
                override fun onPlayerError(error: ExoPlaybackException) {
                    log.debug("ExoPlayer error, ${error.sourceException.message}")
                    this@BackgroundSumysService.stopSelf()
                }
            })
        }
    }

    /**
     * Start the audio stream ON SEPARATE thread using HandlerThread
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mHandler.post {
            try {
                log.debug("Stream initialized")
                // exo player plays the media item object
                val sumysItem = MediaItem.fromUri(TitleViewModel.STREAM_URL)
                // set the media item to be played
                player.setMediaItem(sumysItem);
                // Prepare the player.
                player.prepare()
                // Start the playback.
                player.play()
                fireSpinningIntent()
            } catch (e: Exception) {
                log.error("Failed to start stream, ${e.message}")
                this.stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        mHandler.post {
            try {
                player.pause()
                player.release()
                log.debug("Stream released")
            } catch (e: Exception) {
                log.error("Failed to release stream, ${e.message}")
            }
        }
    }

    private fun fireSpinningIntent() {
        log.debug("Sending spinning intent")
        val intent = Intent("spinning")
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
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