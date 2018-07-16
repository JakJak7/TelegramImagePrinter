package me.jakjak.telegramimagereceiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity

class ActionReceiver : BroadcastReceiver() {

    companion object {
        val ACTION = "action"
        val ACTION_STOP = "stop"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra(ACTION)
        if (action.equals(ACTION_STOP)) {
            val mNotificationManager = context.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
            mNotificationManager.cancel(1337)
            context.stopService(Intent(context, UpdateService::class.java))
        }
    }
}
