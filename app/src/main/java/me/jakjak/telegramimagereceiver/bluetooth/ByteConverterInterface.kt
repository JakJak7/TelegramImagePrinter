package me.jakjak.telegramimagereceiver.bluetooth

import android.graphics.Bitmap

interface ByteConverterInterface {
    fun convert(bitmap: Bitmap): ByteArray
    fun test(): ByteArray
}