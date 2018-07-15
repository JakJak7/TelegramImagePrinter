package me.jakjak.telegramimagereceiver

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_login.*
import me.jakjak.telegramimagereceiver.TelegramClient.Companion.client
import org.drinkless.td.libcore.telegram.TdApi


class LoginActivity : AppCompatActivity(), TelegramClient.Companion.EventHandler {

    val TAG: String = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }

    override fun handleEvent(e: TelegramClient.Companion.Event, s: String?) {
        if (e == TelegramClient.Companion.Event.LoggedIn) {
            val intent = Intent(this, MainActivity::class.java)
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

    private fun onParametersNeeded() {
        client.send(TdApi.SetTdlibParameters(TdApi.TdlibParameters(
                false,
                getApplicationContext().getFilesDir().getAbsolutePath() + "/",
                getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + "/",
                true,
                true,
                true,
                false,
                Constants.appId,
                Constants.appHash,
                "EN-en",
                Constants.device,
                Constants.androidVersion,
                "1.0",
                true, // turn off storage optimizer if weird behavior
                false
        )), {
            Log.d(TAG, "Set TDLib parameters")
        }, {
            Log.e(TAG, "Set TDLib parameters failed")
        })

        client.send(TdApi.CheckDatabaseEncryptionKey(Constants.secretEncryptionKey.toByteArray()), {
            Log.d(TAG, "Set DatabaseEncryptionKey")
        }, {
            Log.e(TAG, "Set DatabaseEncryptionKey failed")
        })

        client.send(TdApi.SetAuthenticationPhoneNumber("***REMOVED***", false, false), {
            Log.d(TAG, "Set PhoneNumber")
        }, {
            Log.e(TAG, "Set PhoneNumber failed")
        })
    }

    fun submitCode(view: View) {
        val code: String = codeField?.text.toString()
        client.send(TdApi.CheckAuthenticationCode(code,"",""), {
            Log.d(TAG, "Set auth code")
        }, {
            Log.e(TAG, "Set auth code failed")
        })
    }
}
