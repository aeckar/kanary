package io.github.aeckar.kanary.io

import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * Provides type-specific operations for [OutputStream].
 * Operations write the appropriate primitive value, string, or [TypeFlag] to the underlying stream.
 */
internal class OutputDataStream(override val raw: OutputStream) : DataStream() {
    // ------------------------------ primitive write operations ------------------------------

    fun writeBoolean(cond: Boolean) = raw.write(if (cond) 1 else 0)
    fun writeByte(b: Byte) = raw.write(b.toInt())
    fun writeChar(n: Char) = writePrimitive(Char.SIZE_BYTES) { putChar(n) }
    fun writeShort(n: Short) = writePrimitive(Short.SIZE_BYTES) { putShort(n) }
    fun writeInt(n: Int) = writePrimitive(Int.SIZE_BYTES) { putInt(n) }
    fun writeLong(n: Long) = writePrimitive(Long.SIZE_BYTES) { putLong(n) }
    fun writeFloat(n: Float) = writePrimitive(Float.SIZE_BYTES) { putFloat(n) }
    fun writeDouble(n: Double) = writePrimitive(Double.SIZE_BYTES) { putDouble(n) }

    // ------------------------------ object write operations ------------------------------

    fun writeTypeFlag(flag: TypeFlag) = raw.write(flag.ordinal)

    fun writeString(s: String) {
        val byteArray = s.toByteArray(Charsets.UTF_8)
        writeInt(byteArray.size)
        raw.write(byteArray)
    }

    // ------------------------------------------------------------------------
    
    private inline fun writePrimitive(sizeBytes: Int, put: ByteBuffer.() -> Unit) {
        val byteBuffer = buffer.asByteBuffer()
        byteBuffer.apply(put)
        raw.write(byteBuffer.array(), 0, sizeBytes)
    }
}