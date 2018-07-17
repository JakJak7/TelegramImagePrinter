package me.jakjak.telegramimagereceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ActionReceiver : BroadcastReceiver() {

    companion object {
        val ACTION = "action"
        val ACTION_STOP = "stop"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra(ACTION)
        if (action.equals(ACTION_STOP)) {
            context.stopService(Intent(context, UpdateService::class.java))
        }
    }
}
