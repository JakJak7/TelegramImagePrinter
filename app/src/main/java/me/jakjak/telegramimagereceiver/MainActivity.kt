package me.jakjak.telegramimagereceiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.google.firebase.iid.FirebaseInstanceId
import org.drinkless.td.libcore.telegram.TdApi


class MainActivity : AppCompatActivity(), TelegramClient.Companion.EventHandler {
    val TAG: String = "MainActivity"

    override fun handleEvent(e: TelegramClient.Companion.Event, s: String?) {
        if (e == TelegramClient.Companion.Event.NeedAuth) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        TelegramClient.bindHandler(this)
        onParametersNeeded()
    }

    override fun onStop() {
        TelegramClient.unbindHandler(this)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { instanceIdResult ->
            val deviceToken = instanceIdResult.token
            TelegramClient.setToken(deviceToken)
            Log.d("Firebase", "token " + deviceToken)
        }
    }

    fun startService(view: View) {
        createChannel()
        startService(Intent(this, UpdateService::class.java))
    }

    private fun onParametersNeeded() {
        TelegramClient.client.send(TdApi.SetTdlibParameters(TdApi.TdlibParameters(
                false,
                getApplicationContext().getFilesDir().getAbsolutePath() + "/",
                getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + "/",
                true,
                true,
                true,
                false,
                Constants.appId,
                Constants.appHash,
                Constants.languageCode,
                Constants.device,
                Constants.androidVersion,
                Constants.version,
                true, // turn off storage optimizer if weird behavior
                false
        )), {
            Log.d(TelegramClient.TAG, "Set TDLib parameters")
        }, {
            Log.e(TelegramClient.TAG, "Set TDLib parameters failed")
        })

        TelegramClient.client.send(TdApi.CheckDatabaseEncryptionKey(Constants.secretEncryptionKey.toByteArray()), {
            Log.d(TelegramClient.TAG, "Set DatabaseEncryptionKey")
        }, {
            Log.e(TelegramClient.TAG, "Set DatabaseEncryptionKey failed")
        })

        TelegramClient.client.send(TdApi.SetAuthenticationPhoneNumber(Constants.phoneNumber, false, false), {
            Log.d(TelegramClient.TAG, "Set PhoneNumber")
        }, {
            Log.e(TelegramClient.TAG, "Set PhoneNumber failed")
        })
    }

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Sets an ID for the notification, so it can be updated.
            var notifyID = 1
            var CHANNEL_ID = "my_channel_01"// The id of the channel.
            var name = Constants.channelName// The user-visible name of the channel.
            var importance = NotificationManager.IMPORTANCE_HIGH
            var mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            // Create a notification and set the notification channel.

            val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            mNotificationManager.createNotificationChannel(mChannel)

            val b = NotificationCompat.Builder(this)
            b.setOngoing(true)
                    .setContentTitle("title")
                    .setContentText("content")
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setTicker("ticker?")
                    .setChannelId(CHANNEL_ID)

            val notification = b.build()
            notification.flags = notification.flags or Notification.FLAG_NO_CLEAR;

            mNotificationManager.notify(1337, notification)
        }
    }

    private fun showNotification(notification: Notification) {
        notification.flags = notification.flags or Notification.FLAG_NO_CLEAR;

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1337, notification)
    }
}
