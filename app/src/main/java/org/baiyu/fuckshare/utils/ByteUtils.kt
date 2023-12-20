package org.baiyu.fuckshare.utils

import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Objects

/**
 * Utility class for working with byte arrays and InputStreams.
 */
object ByteUtils {

    /**
     * Converts 4 or more bytes to an unsigned integer.
     *
     * @param order The byte order of the input bytes.
     * @return The converted unsigned integer value.
     */
    fun ByteArray.toUInt(order: ByteOrder): UInt {
        return ByteBuffer.wrap(this).apply {
            order(order)
        }.int.toUInt()
    }

    /**
     * Converts 2 or more bytes to an unsigned short.
     *
     * @param order The byte order of the input bytes.
     * @return The converted unsigned short value.
     */
    fun ByteArray.toUShort(order: ByteOrder): UShort {
        return ByteBuffer.wrap(this).apply {
            order(order)
        }.short.toUShort()
    }

    /**
     * Reads exactly [bytes.size] bytes from the input stream and stores them in the provided byte array.
     *
     * @param inputStream The input stream to read from.
     * @param bytes The byte array to store the read bytes.
     * @return The total number of bytes read.
     * @throws IOException If an I/O error occurs.
     */
    @Throws(IOException::class)
    fun readNBytes(inputStream: InputStream, bytes: ByteArray): Int {
        return readNBytes(inputStream, bytes, 0, bytes.size)
    }

    /**
     * Reads exactly [len] bytes from the input stream into the specified range of the byte array.
     *
     * @param inputStream The input stream to read from.
     * @param b The byte array to store the read bytes.
     * @param off The starting offset within the byte array.
     * @param len The number of bytes to read.
     * @return The total number of bytes read.
     * @throws IOException If an I/O error occurs.
     */
    @Throws(IOException::class)
    fun readNBytes(inputStream: InputStream, b: ByteArray, off: Int, len: Int): Int {
        Objects.checkFromIndexSize(off, len, b.size)
        var count: Int
        var n = 0
        while (n < len) {
            count = inputStream.read(b, off + n, len - n)
            if (count < 0) break
            n += count
        }
        return n
    }

    /**
     * Skips exactly [len] bytes in the input stream.
     *
     * @param inputStream The input stream to skip bytes from.
     * @param len The number of bytes to skip.
     * @return The total number of bytes actually skipped.
     * @throws IOException If an I/O error occurs.
     */
    fun skipNBytes(inputStream: InputStream, len: Long): Long {
        var n = len
        while (n > 0L) {
            val ns: Long = inputStream.skip(n)
            if (ns in 1L..n) {
                n -= ns
                continue
            }
            if (ns == 0L) {
                if (inputStream.read() == -1) {
                    break
                }
                --n
                continue
            }
            throw IOException("Unable to skip exactly")
        }
        return len - n
    }
}
