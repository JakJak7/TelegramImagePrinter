package me.jakjak.telegramimagereceiver

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()

        val client: Client = Client.create({
            Toast.makeText(this, "got update!", Toast.LENGTH_SHORT).show()
        }, {
            Toast.makeText(this, "update exception!", Toast.LENGTH_SHORT).show()
        }, {
            Toast.makeText(this, "default exception!", Toast.LENGTH_SHORT).show()
        })

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
            Toast.makeText(this, "got update!", Toast.LENGTH_SHORT).show()
        }, {
            Toast.makeText(this, "update exception!", Toast.LENGTH_SHORT).show()
        })
    }
}
