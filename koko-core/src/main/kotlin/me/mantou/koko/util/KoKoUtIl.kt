package me.mantou.koko.util

import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

fun ByteArray.zlibDecompress(bufferSize: Int = 1024): ByteArray{
    val outputStream = ByteArrayOutputStream()

    return outputStream.use {
        val inflater = Inflater()
        val buffer = ByteArray(bufferSize)
        inflater.setInput(this)

        while (!inflater.finished()) {
            outputStream.write(
                buffer,
                0,
                inflater.inflate(buffer)
            )
        }

        inflater.end()
        outputStream.toByteArray()
    }
}