package me.jakjak.telegramimagereceiver.bluetooth

import android.graphics.Bitmap
import android.graphics.Color
import java.nio.charset.StandardCharsets

class ZebraByteConverter : ByteConverterInterface {
    override fun convert(bitmap: Bitmap): ByteArray {
        var byteList = ArrayList<Byte>()
        val header = "! 0 200 200 " + bitmap.height + " 1\r\n"
        byteList.addAll(header.toByteArray(StandardCharsets.US_ASCII).asList())

        var loopWidth = 8 - (bitmap.width % 8)

        if (loopWidth == 8) {
            loopWidth = bitmap.width
        }
        else {
            loopWidth += bitmap.width
        }

        var data = String.format("EG %d %d %d %d ", loopWidth / 8, bitmap.height, 0, 0, 1)
        byteList.addAll(data.toByteArray(StandardCharsets.US_ASCII).asList())

        for (y in 0..bitmap.height - 1) {
            var bit = 128
            var currentValue = 0

            for (x in 0..loopWidth - 1) {
                var intensity: Int

                if (x < bitmap.width) {
                    var pixel = bitmap.getPixel(x,y)
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
                    val hexString = java.lang.Integer.toHexString(currentValue)
                    byteList.addAll(hexString.toByteArray(StandardCharsets.US_ASCII).asList())
                    bit = 128
                    currentValue = 0
                }
            } // x
        } // y

        byteList.addAll("PRINT\r\n".toByteArray(StandardCharsets.US_ASCII).asList())
        return byteList.toByteArray()
    }
}