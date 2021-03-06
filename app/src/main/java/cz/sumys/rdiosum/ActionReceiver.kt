package cz.sumys.rdiosum

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat.startActivity

/**
 * Class for handling actions from notification
 */
class ActionReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        when (intent?.getStringExtra("action")) {
            "stop" -> deleteAction(context)
            "delete" -> deleteAction(context)
        }
        Log.i("ActionReceiver", "onReceive() reached, action = ${intent?.getStringExtra("action")}")
    }


    /**
     * The action was "delete" or "stop,
     */
    private fun deleteAction(context: Context?) {
        val activityIntent = Intent(context, MainActivity::class.java)
        activityIntent.action = Intent.ACTION_MAIN
        activityIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context?.startActivity(activityIntent)

        context?.sendBroadcast(Intent("NOTIFICATION_DISMISSED"))

    }
}