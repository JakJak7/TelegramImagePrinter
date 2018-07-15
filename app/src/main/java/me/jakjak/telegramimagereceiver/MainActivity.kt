package me.jakjak.telegramimagereceiver

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
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
}
