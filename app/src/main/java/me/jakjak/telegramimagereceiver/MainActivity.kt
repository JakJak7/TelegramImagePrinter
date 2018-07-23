package me.jakjak.telegramimagereceiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_main.*
import org.drinkless.td.libcore.telegram.TdApi
import java.util.*


class MainActivity : AppCompatActivity(), TelegramClient.Companion.EventHandler {
    val TAG: String = "MainActivity"

    override fun handleEvent(e: TelegramClient.Companion.Event, s: String?) {
        if (e == TelegramClient.Companion.Event.NeedPhone || e == TelegramClient.Companion.Event.NeedAuth) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { instanceIdResult ->
            val deviceToken = instanceIdResult.token
            TelegramClient.setToken(deviceToken)
            Log.d("Firebase", "token " + deviceToken)
        }

        createNotificationChannel()
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

    override fun onResume() {
        super.onResume()
        updateButtons(UpdateService.isAlive)
        UpdateService.onStopCallback = {
            updateButtons(UpdateService.isAlive)
        }
    }

    private fun updateButtons(alive: Boolean) {
        startButton.isEnabled = !alive
        stopButton.isEnabled = alive
    }

    override fun onPause() {
        super.onPause()
        UpdateService.onStopCallback = null
    }

    fun startService(view: View) {
        val intent = Intent(this, UpdateService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        startButton.isEnabled = false
        stopButton.isEnabled = true
    }

    fun stopService(view: View) {
        stopService(Intent(this, UpdateService::class.java))
        startButton.isEnabled = true
        stopButton.isEnabled = false
    }

    fun openUserList(view: View) {
        val intent = Intent(this, UserListActivity::class.java)
        startActivity(intent)
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
                BuildConfig.appId,
                BuildConfig.appHash,
                Locale.getDefault().toString(),
                Build.MODEL,
                Build.VERSION.RELEASE,
                BuildConfig.VERSION_NAME,
                true, // turn off storage optimizer if weird behavior
                false
        )), {
            Log.d(TelegramClient.TAG, "Set TDLib parameters")
        }, {
            Log.e(TelegramClient.TAG, "Set TDLib parameters failed")
        })

        TelegramClient.client.send(TdApi.CheckDatabaseEncryptionKey(BuildConfig.secretEncryptionKey.toByteArray()), {
            Log.d(TelegramClient.TAG, "Set DatabaseEncryptionKey")
        }, {
            Log.e(TelegramClient.TAG, "Set DatabaseEncryptionKey failed")
        })

        val phoneNumber = Preferences.getPreferences(this, Preferences.PHONE_NUMBER)
        if (phoneNumber != null) {
            TelegramClient.client.send(TdApi.SetAuthenticationPhoneNumber(phoneNumber, false, false), {
                Log.d(TelegramClient.TAG, "Set PhoneNumber")
            }, {
                Log.e(TelegramClient.TAG, "Set PhoneNumber failed")
            })
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Sets an ID for the notification, so it can be updated.
            val CHANNEL_ID = Constants.channelId// The id of the channel.
            val name = Constants.channelName// The user-visible name of the channel.
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            // Create a notification and set the notification channel.

            val mNotificationManager = getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
            mNotificationManager.createNotificationChannel(mChannel)
        }
    }
}
