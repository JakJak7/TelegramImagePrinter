package me.jakjak.telegramimagereceiver

import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_main.*
import me.jakjak.telegramimagereceiver.TelegramClient.Companion.client

class MainActivity : AppCompatActivity(), TelegramClient.Companion.EventHandler {
    val TAG: String = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { instanceIdResult ->
            val deviceToken = instanceIdResult.token
            TelegramClient.setToken(deviceToken)
            Log.d("Firebase", "token " + deviceToken)
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

    override fun handleEvent(e: TelegramClient.Companion.Event, s: String?) {
        if (e == TelegramClient.Companion.Event.ImageReady) {
            handleImage(s.orEmpty())
        }
    }

    private fun handleImage(path: String) {
        var bmp = BitmapFactory.decodeFile(path)
        val factor = Constants.screenWidth / bmp.width
        bmp = android.graphics.Bitmap.createScaledBitmap(bmp, (bmp.width * factor).toInt(), (bmp.height * factor).toInt(), false)
        this.runOnUiThread({
            try {
                image.setImageBitmap(bmp)
            } catch (e: Exception) {
                Log.e(TAG, e.message)
            }
        })
    }
}
