package me.jakjak.telegramimagereceiver

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.firebase.iid.FirebaseInstanceId

class MainActivity : AppCompatActivity() {
    val TAG: String = "MainActivity"

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
}
