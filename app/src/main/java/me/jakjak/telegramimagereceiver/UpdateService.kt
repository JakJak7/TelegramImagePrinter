package me.jakjak.telegramimagereceiver

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v4.app.NotificationCompat
import android.util.Log
import me.jakjak.telegramimagereceiver.bluetooth.*
import com.askjeffreyliu.floydsteinbergdithering.Utils.floydSteinbergDithering
import android.graphics.Bitmap
import com.askjeffreyliu.floydsteinbergdithering.Utils


class UpdateService : Service(), TelegramClient.Companion.EventHandler {

    val printer: Printer = Printer(this, BuildConfig.printerMacAddress)

    companion object {
        var isAlive = false
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        TelegramClient.bindHandler(this)
        isAlive = true

        val notification = createNotification(Constants.channelId)
        startForeground(Constants.foregroundServiceId, notification)

        printer.openConnection()

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        isAlive = false
        printer.closeConnection()
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
        val scaleFactor: Double

        // assumes square print
        if (bmp.width >= bmp.height) {
            scaleFactor= Constants.printWidth / bmp.width
        }
        else {
            scaleFactor = Constants.printWidth / bmp.height
        }
        bmp = android.graphics.Bitmap.createScaledBitmap(bmp, (bmp.width * scaleFactor).toInt(), (bmp.height * scaleFactor).toInt(), false)

        val fsBitmap = Utils.floydSteinbergDithering(bmp)

        val converter = POSByteConverter()
        try {
            val bytes = converter.convert(fsBitmap)
            val printCommand = converter.POS_Set_PrtAndFeedPaper(0)
            printer.print(converter.ESC_Init)
            printer.print(converter.LF)
            printer.print(bytes)
            printer.print(printCommand)
            //val test = converter.newline(6)
            //printer.print(test)
        }
        catch (e: Exception) {
            Log.e("muhService", e.localizedMessage)
        }


        doVibrate(100)
    }

    private fun doVibrate(vibtime: Long) {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(vibtime, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            //deprecated in API 26
            v.vibrate(vibtime)
        }
    }

    private fun createNotification(CHANNEL_ID: String): Notification {
        val action: NotificationCompat.Action = createAction()

        val b = NotificationCompat.Builder(this)
        b.setOngoing(true)
                .setContentTitle("Telegram image service")
                .setContentText("Running...")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setTicker("ticker?")
                .setChannelId(CHANNEL_ID)
                .addAction(action)

        val notification = b.build()
        notification.flags = notification.flags or Notification.FLAG_NO_CLEAR;
        return notification
    }

    val notificationActionCode = 1338

    private fun createAction(): NotificationCompat.Action {
        val intentAction = Intent(this, ActionReceiver::class.java)
        intentAction.putExtra(ActionReceiver.ACTION, ActionReceiver.ACTION_STOP)
        val pendingIntent: PendingIntent = PendingIntent.getBroadcast(this, notificationActionCode, intentAction, PendingIntent.FLAG_UPDATE_CURRENT)
        val action: NotificationCompat.Action = NotificationCompat.Action.Builder(android.R.drawable.arrow_down_float, "stop", pendingIntent).build()
        return action
    }
}
