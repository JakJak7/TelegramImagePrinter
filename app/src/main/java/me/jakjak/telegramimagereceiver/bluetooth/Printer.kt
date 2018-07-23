package me.jakjak.telegramimagereceiver.bluetooth

import android.bluetooth.BluetoothDevice.BOND_BONDED
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context

class Printer(val context: Context, val MAC: String) {

    val uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    lateinit var socket: BluetoothSocket

    public fun openConnection(): BluetoothSocket? {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (!manager.adapter.isEnabled) {
            manager.adapter.enable()
            throw IllegalAccessException("Bluetooth not enabled! Enabling...")
        }
        val device = manager.adapter.getRemoteDevice(MAC)

        if (device.bondState != BOND_BONDED) {
            throw IllegalAccessException("Not paired with printer!")
        }

        socket = device.createRfcommSocketToServiceRecord(uuid)
        socket.connect()

        return socket
    }

    public fun print(bytes: ByteArray) {
        val outputStream = socket.outputStream
        outputStream.write(bytes)
        outputStream.flush()
    }

    fun read() {
        val inputStream = socket.inputStream
        val buffer = ByteArray(0xFF)
        inputStream.read(buffer)
    }

    fun closeConnection() {
        socket.close()
    }
}