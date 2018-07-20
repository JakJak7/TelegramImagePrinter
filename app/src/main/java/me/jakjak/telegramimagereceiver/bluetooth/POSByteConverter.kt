package me.jakjak.telegramimagereceiver.bluetooth

import android.graphics.Bitmap
import android.graphics.Color

class POSByteConverter : ByteConverterInterface {
    override fun convert(bitmap: Bitmap): ByteArray {
        val bytes = toBitArray(bitmap)
        val printableBytes = eachLinePixToCmd(bytes, bitmap.width, 0)

        val bytesList = ArrayList<Byte>()
        bytesList.addAll(ESC_Init.toList())
        bytesList.addAll(LF.toList())
        bytesList.addAll(printableBytes.toList())
        bytesList.addAll(POS_Set_PrtAndFeedPaper(0).toList())

        return bytesList.toByteArray()
    }

    private fun toBitArray(bitmap: Bitmap): ByteArray {
        val bytes = ArrayList<Byte>()
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                var intensity: Int

                val pixel = bitmap.getPixel(x, y)
                intensity = 255 - ((Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3)

                if (intensity > 128) {
                    bytes.add(1)
                }
                else {
                    bytes.add(0)
                }
            }
        }
        return bytes.toByteArray()
    }

    private val p0 = intArrayOf(0, 0x80)
    private val p1 = intArrayOf(0, 0x40)
    private val p2 = intArrayOf(0, 0x20)
    private val p3 = intArrayOf(0, 0x10)
    private val p4 = intArrayOf(0, 0x08)
    private val p5 = intArrayOf(0, 0x04)
    private val p6 = intArrayOf(0, 0x02)

    private fun eachLinePixToCmd(src: ByteArray, nWidth: Int, nMode: Int): ByteArray {
        val nHeight = src.size / nWidth
        val nBytesPerLine = nWidth / 8
        val data = ByteArray(nHeight * (8 + nBytesPerLine))
        var offset = 0
        var k = 0
        for (i in 0 until nHeight) {
            offset = i * (8 + nBytesPerLine)
            data[offset + 0] = 0x1d
            data[offset + 1] = 0x76
            data[offset + 2] = 0x30
            data[offset + 3] = (nMode and 0x01).toByte()
            data[offset + 4] = (nBytesPerLine % 0x100).toByte()
            data[offset + 5] = (nBytesPerLine / 0x100).toByte()
            data[offset + 6] = 0x01
            data[offset + 7] = 0x00
            for (j in 0 until nBytesPerLine) {
                data[offset + 8 + j] = (p0[src[k].toInt()] + p1[src[k + 1].toInt()]
                        + p2[src[k + 2].toInt()] + p3[src[k + 3].toInt()] + p4[src[k + 4].toInt()]
                        + p5[src[k + 5].toInt()] + p6[src[k + 6].toInt()] + src[k + 7].toInt()).toByte()
                k = k + 8
            }
        }

        return data
    }

    private val ESC: Byte = 0x1B
    private val NL: Byte = 0x0A

    private var ESC_Init = byteArrayOf(ESC, '@'.toByte())
    private var LF = byteArrayOf(NL)
    private var ESC_J = byteArrayOf(ESC, 'J'.toByte(), 0x00)


    private fun POS_Set_PrtAndFeedPaper(feed: Int): ByteArray {
        var inputFeed = feed
        if ((inputFeed > 255) or (inputFeed < 0)) {
            inputFeed = 25
        }

        ESC_J[2] = inputFeed.toByte()

        return ESC_J
    }

    override fun test(): ByteArray {
        return "Test!\n".toByteArray()
    }

    override fun newline(count: Int): ByteArray {
        val newline = " \r\n"
        val newlines = newline.repeat(count)
        return newlines.toByteArray()
    }
}