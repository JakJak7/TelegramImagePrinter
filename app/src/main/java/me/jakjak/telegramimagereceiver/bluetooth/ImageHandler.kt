package me.jakjak.telegramimagereceiver.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.graphics.Bitmap
import java.util.*

class ImageHandler(context: Context, val mac: String) {

    val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    val device = manager.adapter.getRemoteDevice(mac)

    val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    val socket = device.createRfcommSocketToServiceRecord(uuid)

    fun getServices() {
        var services = device.uuids
        var first = services[0]
    }

    fun pair(context: Context, mac: String): Boolean {
        manager.adapter.enable()
        return true
    }

    fun sendImage(bitmap: Bitmap): Boolean {
        if (!manager.adapter.isEnabled) {
            manager.adapter.enable()
        }

        if (device.bondState != BluetoothDevice.BOND_BONDED) {
            return false
        }

        if (!socket.isConnected) {
            socket.connect()
            return false
        }

        val imageBytes = getBytes(bitmap)

        val outputStream = socket.outputStream
        outputStream.write(imageBytes)
        outputStream.flush()

        return true
    }

    private fun getBytes(bitmap: Bitmap): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}