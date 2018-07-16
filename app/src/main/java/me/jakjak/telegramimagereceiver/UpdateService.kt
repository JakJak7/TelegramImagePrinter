package me.jakjak.telegramimagereceiver

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator

class UpdateService : Service(), TelegramClient.Companion.EventHandler {

    companion object {
        var isAlive = false
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        TelegramClient.bindHandler(this)
        isAlive = true
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        isAlive = false
        TelegramClient.unbindHandler(this)
        super.onDestroy()
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

        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val vibtime: Long = 100
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(vibtime, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            //deprecated in API 26
            v.vibrate(vibtime)
        }
    }
}
