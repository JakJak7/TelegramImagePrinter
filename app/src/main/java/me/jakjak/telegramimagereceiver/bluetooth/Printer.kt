package me.jakjak.telegramimagereceiver.bluetooth

import android.bluetooth.BluetoothDevice.BOND_BONDED
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context

class Printer(val context: Context, val MAC: String) {

    val uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    lateinit var socket: BluetoothSocket

    public fun openConnection(): Boolean {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (!manager.adapter.isEnabled) {
            manager.adapter.enable()
        }
        val device = manager.adapter.getRemoteDevice(MAC)

        /*if (device.bondState != BOND_BONDED) {
            return false
        }*/

        socket = device.createRfcommSocketToServiceRecord(uuid)
        socket.connect()

        return true
    }

    public fun print(bytes: ByteArray) {
        val outputStream = socket.outputStream
        outputStream.write(bytes)
        outputStream.flush()
    }

    fun closeConnection() {
        socket.close()
    }
}