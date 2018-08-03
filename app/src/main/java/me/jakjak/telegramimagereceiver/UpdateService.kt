package me.jakjak.telegramimagereceiver

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.os.*
import android.support.v4.app.NotificationCompat
import android.widget.Toast
import com.askjeffreyliu.floydsteinbergdithering.Utils
import me.jakjak.telegramimagereceiver.bluetooth.ByteConverterInterface
import me.jakjak.telegramimagereceiver.bluetooth.POSByteConverter
import me.jakjak.telegramimagereceiver.bluetooth.Printer
import java.io.IOException


class UpdateService : Service(), TelegramClient.Companion.EventHandler {

    val printer: Printer = Printer(this, BuildConfig.printerMacAddress)

    companion object {
        var isRunning = false
        var isConnected = false
        var onStopCallback: (() -> Unit)? = null
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val notification = createNotification(Constants.channelId)
        startForeground(Constants.foregroundServiceId, notification)
        isRunning = true

        try {
            printer.openConnection()
            isConnected = true

            TelegramClient.bindHandler(this)

            startReadLoop()
        } catch (e: IllegalAccessException) {
            onConnectionLost(e.message!!)
        } catch (e: IOException) {
            onConnectionLost("Could not connect to printer")
        }
        return START_STICKY
    }

    private fun onConnectionLost(errorMessage: String) {
        isConnected = false
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
    }

    private fun startReadLoop() {
        val handlerThread = HandlerThread("readThread")
        handlerThread.start()
        val handler = Handler(handlerThread.looper)

        handler.post {
            while (isRunning) {
                try {
                    if (isConnected) {
                        printer.read()
                    }
                    else {
                        printer.openConnection()
                    }
                    isConnected = true
                }
                catch (e: Exception) {
                    onConnectionLost(e.message!!)
                    Thread.sleep(5000)
                }
            }
        }
    }

    override fun onDestroy() {
        if (isRunning) {
            isRunning = false
            onStopCallback?.invoke()
            printer.closeConnection()
            TelegramClient.unbindHandler(this)
        }
        super.onDestroy()
    }

    override fun handleEvent(e: TelegramClient.Companion.Event, s: String?) {
        if (e == TelegramClient.Companion.Event.ImageReady) {
            handleImage(s.orEmpty())
        }
    }

    private fun handleImage(path: String) {
        val image = BitmapFactory.decodeFile(path)
        val bmp = Bitmap.createBitmap(image.getWidth(), image.getHeight(), image.getConfig())
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(image, 0f, 0f, null)

        val scaleFactor: Double

        // assumes square print
        if (bmp.width >= bmp.height) {
            scaleFactor= Constants.printWidth / bmp.width
        }
        else {
            scaleFactor = Constants.printWidth / bmp.height
        }
        val resizedBitmap = android.graphics.Bitmap.createScaledBitmap(bmp, (bmp.width * scaleFactor).toInt(), (bmp.height * scaleFactor).toInt(), false)

        val fsBitmap = Utils.floydSteinbergDithering(resizedBitmap)

        val converter: ByteConverterInterface = POSByteConverter()
        try {
            val bytes = converter.convert(fsBitmap)
            printer.print(bytes)
            doVibrate(100)
        }
        catch (e: Exception) {
            onConnectionLost(e.message!!)
        }
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

    private fun createAction(): NotificationCompat.Action {
        val intentAction = Intent(this, ActionReceiver::class.java)
        intentAction.putExtra(ActionReceiver.ACTION, ActionReceiver.ACTION_STOP)
        val pendingIntent: PendingIntent = PendingIntent.getBroadcast(this, Constants.notificationActionCode, intentAction, PendingIntent.FLAG_UPDATE_CURRENT)
        val action: NotificationCompat.Action = NotificationCompat.Action.Builder(android.R.drawable.arrow_down_float, "stop", pendingIntent).build()
        return action
    }
}
