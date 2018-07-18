package me.jakjak.telegramimagereceiver.bluetooth

import android.graphics.Bitmap
import android.graphics.Color
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class ZebraByteConverter : ByteConverterInterface {
    override fun newline(): ByteArray {
        val byteList = ArrayList<Byte>()
        val text = "\r\n"
        addStringBytes(byteList, text)
        return byteList.toByteArray()
    }

    override fun test(): ByteArray {
        val byteList = ArrayList<Byte>()
        val text = "! 0 200 200 25 1\r\n" +
                "TEXT 7 0 0 0 Test string\r\n" +
                "PRINT\r\n"
        addStringBytes(byteList, text)
        return byteList.toByteArray()
    }

    override fun convert(bitmap: Bitmap): ByteArray {
        val byteList = ArrayList<Byte>()
        val header = "! 0 200 200 " + bitmap.height + " 1\r\n"
        addStringBytes(byteList, header)

        var loopWidth = 8 - (bitmap.width % 8)

        if (loopWidth == 8) {
            loopWidth = bitmap.width
        }
        else {
            loopWidth += bitmap.width
        }

        val data = String.format("EG %d %d %d %d ", loopWidth / 8, bitmap.height, 0, 0)
        addStringBytes(byteList, data)

        for (y in 0 until bitmap.height) {
            var bit = 128
            var currentValue = 0

            for (x in 0 until loopWidth) {
                var intensity: Int

                if (x < bitmap.width) {
                    val pixel = bitmap.getPixel(x,y)
                    intensity = 255 - ((Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3)
                }
                else {
                    intensity = 0
                }

                if (intensity >= 128) {
                    currentValue = currentValue or bit
                }

                bit = bit shr 1

                if (bit == 0) {
                    var hexString = java.lang.Integer.toHexString(currentValue)
                    while (hexString.length < 2) {
                        hexString = "0" + hexString
                    }
                    addStringBytes(byteList, hexString)
                    bit = 128
                    currentValue = 0
                }
            } // x
        } // y

        addStringBytes(byteList, "PRINT\r\n")
        return byteList.toByteArray()
    }

    private fun addStringBytes(byteList: ArrayList<Byte>, hexString: String) {
        byteList.addAll(hexString.toByteArray(Charsets.US_ASCII).asList())
    }
}