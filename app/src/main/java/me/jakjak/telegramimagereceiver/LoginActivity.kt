package me.jakjak.telegramimagereceiver

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_login.*
import org.drinkless.td.libcore.telegram.TdApi


class LoginActivity : AppCompatActivity(), TelegramClient.Companion.EventHandler {
    val TAG: String = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        toggleInput(true)
    }

    override fun handleEvent(e: TelegramClient.Companion.Event, s: String?) {
        if (e == TelegramClient.Companion.Event.NeedAuth) {
            toggleInput(false)
        }
        else if (e == TelegramClient.Companion.Event.LoggedIn) {
            Preferences.setPreference(this, Preferences.PHONE_NUMBER, phoneNumberField?.text.toString())

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        TelegramClient.bindHandler(this)
    }

    override fun onStop() {
        TelegramClient.unbindHandler(this)
        super.onStop()
    }

    fun toggleInput(needPhone: Boolean) {
        runOnUiThread({
            phoneNumberField.isEnabled = needPhone
            submitPhoneNumberButton.isEnabled = needPhone

            codeField.isEnabled = !needPhone
            submitCodeButton.isEnabled = !needPhone
        })
    }

    fun submitPhoneNumber(view: View) {
        val phoneNumber: String = phoneNumberField?.text.toString()
        TelegramClient.client.send(TdApi.SetAuthenticationPhoneNumber(phoneNumber, false, false), {
            Log.d(TelegramClient.TAG, "Set PhoneNumber")
        }, {
            Log.e(TelegramClient.TAG, "Set PhoneNumber failed")
        })
    }

    fun submitCode(view: View) {
        val code: String = codeField?.text.toString()
        TelegramClient.client.send(TdApi.CheckAuthenticationCode(code,"",""), {
            Log.d(TAG, "Set auth code")
        }, {
            Log.e(TAG, "Set auth code failed")
        })
    }
}
