package me.jakjak.telegramimagereceiver

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    val TAG: String = "MainActivity"

    lateinit var client: Client

    val secretEncryptionKey: String = "***REMOVED***"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        client = Client.create({
            Log.d(TAG, "Got update!")
        }, {
            Log.e(TAG, "Update exception!")
        }, {
            Log.e(TAG, "Default exception!")
        })
    }

    override fun onStart() {
        super.onStart()

        client.send(TdApi.SetTdlibParameters(TdApi.TdlibParameters(
                false,
                getApplicationContext().getFilesDir().getAbsolutePath() + "/",
                getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + "/",
                true,
                true,
                true,
                false,
                ***REMOVED***,
                "***REMOVED***",
                "EN-en",
                "Huawei P8",
                "6.0",
                "1.0",
                true, // turn off storage optimizer if weird behavior
                false
        )), {
            Log.d(TAG, "Set TDLib parameters")
        }, {
            Log.e(TAG, "Set TDLib parameters failed")
        })

        client.send(TdApi.CheckDatabaseEncryptionKey(secretEncryptionKey.toByteArray()), {
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
