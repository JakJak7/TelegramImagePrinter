package me.jakjak.telegramimagereceiver

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.content.Intent



class PushNotificationService : FirebaseMessagingService() {

    override fun onNewToken(p0: String?) {
        super.onNewToken(p0)
    }
    override fun onMessageReceived(p0: RemoteMessage?) {
        super.onMessageReceived(p0)

        val dialogIntent = Intent(this, LoginActivity::class.java)
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(dialogIntent)
    }
}