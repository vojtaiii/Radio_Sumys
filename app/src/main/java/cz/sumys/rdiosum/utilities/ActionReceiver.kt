package cz.sumys.rdiosum.utilities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import cz.sumys.rdiosum.activities.MainActivity
import cz.sumys.rdiosum.fragments.TitleFragment
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Class for handling actions from notification
 */
class ActionReceiver: BroadcastReceiver() {
    val log: Logger = LoggerFactory.getLogger(MainActivity::class.java)

    override fun onReceive(context: Context?, intent: Intent?) {

        when (intent?.getStringExtra("action")) {
            "stop" -> deleteAction(context)
            "delete" -> deleteAction(context)
        }
       log.debug("onReceive() reached, action = ${intent?.getStringExtra("action")}")
    }


    /**
     * The action was "delete" or "stop
     * Send broadcast to app that stop on notification was pressed
     * SumysBackgroundService should be listening
     */
    private fun deleteAction(context: Context?) {
        // this stop the streaming service
        val sumysIntent = Intent(context, BackgroundSumysService::class.java)
        context?.stopService(sumysIntent)

        // this notify the title view model
        if (context != null) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(Intent("NOTIFICATION_DISMISSED"))
        }

    }
}