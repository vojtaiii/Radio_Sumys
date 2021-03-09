package cz.sumys.rdiosum

import android.R
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Builder
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContentProviderCompat.requireContext
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class MyFirebaseMessagingService: FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.i("FirebaseMessagingService", "Remote notification received")

        val notificationManager = NotificationManagerCompat.from(applicationContext)

        // media session for notification style
        val mediaSession = MediaSessionCompat(applicationContext, "sumys_tag")

        // picture and its bitmap
        val picture = BitmapFactory.decodeResource(resources, cz.sumys.rdiosum.R.drawable.sumys_notification)

        // content intent when user presses the notification body
        val activityIntent = Intent(applicationContext, MainActivity::class.java)
        activityIntent.action = Intent.ACTION_MAIN
        activityIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val contentIntent = PendingIntent.getActivity(applicationContext, 0, activityIntent, 0)

        val notification = Builder(this, "sumys_info")
            .setSmallIcon(cz.sumys.rdiosum.R.drawable.ic_sumys_icon)
            .setLargeIcon(picture)
            .setContentTitle(message.notification?.title)
            .setContentText(message.notification?.body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
            )
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2, notification)
    }
}