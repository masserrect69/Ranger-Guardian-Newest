package au.com.rangerai.data

import android.util.Log

/**
 * Decodes 8-bit unsigned values.
 * Value = (raw byte as unsigned 0-255) * scale + offset
 */
class Unsigned8BitDecoder(
    private val byteIndex: Int = 0,
    private val scale: Double = 1.0,
    private val offset: Double = 0.0
) : PidDecoder {
    override fun decode(rawResponse: String, did: String): Double? {
        val dataBytes = extractDataBytes(rawResponse, did)
        if (byteIndex >= dataBytes.size) return null
        val rawValue = dataBytes[byteIndex].toInt() and 0xFF
        return (rawValue * scale) + offset
    }
}

/**
 * Decodes 16-bit unsigned values (Big Endian).
 *
 * CRITICAL for Ford Mode 22: The raw 16-bit value is ALWAYS treated as unsigned (0-65535).
 * The offset (e.g. -327.68 for injectors, -32.768 for cylinder balance) is applied AFTER scaling.
 *
 * The engineering scale/offset is supplied by the active PID definition.
 * Ford Mode 22 equations in this project are treated as experimental until
 * confirmed against a trusted PID definition and live reference values.
 *
 * Value = (A*256+B) * scale + offset
 */
class Unsigned16BitDecoder(
    private val byteIndex: Int = 0,
    private val scale: Double = 1.0,
    private val offset: Double = 0.0
) : PidDecoder {
    override fun decode(rawResponse: String, did: String): Double? {
        val dataBytes = extractDataBytes(rawResponse, did)
        val i = byteIndex

        if (dataBytes.size < i + 2) return null
        val msb = dataBytes[i].toInt() and 0xFF
        val lsb = dataBytes[i + 1].toInt() and 0xFF
        val rawValue = (msb shl 8) or lsb   // Always unsigned: 0-65535
        return (rawValue * scale) + offset
    }
}

/**
 * Decodes true 16-bit signed values (Two's Complement, Big Endian).
 * Used for parameters that can be negative, e.g. torque, fuel timing.
 * Value = signed16(A,B) * scale + offset
 */
class Signed16BitDecoder(
    private val byteIndex: Int = 0,
    private val scale: Double = 1.0,
    private val offset: Double = 0.0
) : PidDecoder {
    override fun decode(rawResponse: String, did: String): Double? {
        val dataBytes = extractDataBytes(rawResponse, did)
        val i = byteIndex
        if (dataBytes.size < i + 2) return null

        val msb = dataBytes[i].toInt() and 0xFF
        val lsb = dataBytes[i + 1].toInt() and 0xFF
        var rawValue = (msb shl 8) or lsb
        if (rawValue > 32767) rawValue -= 65536   // Two's complement

        return (rawValue * scale) + offset
    }
}

/**
 * Decodes a boolean (bit) value from a single byte.
 */
class BooleanDecoder(
    private val byteIndex: Int = 0,
    private val bitMask: Int = 0x01
) : PidDecoder {
    override fun decode(rawResponse: String, did: String): Double? {
        val dataBytes = extractDataBytes(rawResponse, did)
        if (byteIndex >= dataBytes.size) return null
        val raw = dataBytes[byteIndex].toInt() and 0xFF
        return if ((raw and bitMask) != 0) 1.0 else 0.0
    }
}

/**
 * Decodes a 32-bit unsigned value (Big Endian).
 */
class Unsigned32BitDecoder(
    private val byteIndex: Int = 0,
    private val scale: Double = 1.0,
    private val offset: Double = 0.0
) : PidDecoder {
    override fun decode(rawResponse: String, did: String): Double? {
        val dataBytes = extractDataBytes(rawResponse, did)
        val i = byteIndex
        if (dataBytes.size < i + 4) return null
        val b0 = dataBytes[i].toInt() and 0xFF
        val b1 = dataBytes[i + 1].toInt() and 0xFF
        val b2 = dataBytes[i + 2].toInt() and 0xFF
        val b3 = dataBytes[i + 3].toInt() and 0xFF
        val raw = (b0.toLong() shl 24) or (b1.toLong() shl 16) or (b2.toLong() shl 8) or b3.toLong()
        return raw * scale + offset
    }
}

/**
 * Mode 01 formula decoder — evaluates standard OBD-II formulas.
 */
class Mode01Decoder(
    private val formula: String,
    private val expectedBytes: Int = 1
) : PidDecoder {
    override fun decode(rawResponse: String, did: String): Double? {
        // For Mode 01, DID is 4 chars like "010C"; strip the "01" prefix to get the PID
        val pid = if (did.length == 4 && did.startsWith("01", ignoreCase = true))
            did.substring(2).uppercase()
        else
            did.uppercase()

        val dataBytes = extractDataBytes(rawResponse, pid)
        if (dataBytes.size < expectedBytes) return null

        val A = if (dataBytes.isNotEmpty()) (dataBytes[0].toInt() and 0xFF).toDouble() else 0.0
        val B = if (dataBytes.size > 1) (dataBytes[1].toInt() and 0xFF).toDouble() else 0.0
        return when (formula.trim()) {
            "A"                     -> A
            "B"                     -> B
            "A-40"                  -> A - 40
            "A*100/255"             -> A * 100.0 / 255.0
            "A*3"                   -> A * 3
            "(A*256+B)/4"           -> (A * 256 + B) / 4.0
            "A*256+B"               -> A * 256 + B
            "(A*256+B)*10"          -> (A * 256 + B) * 10.0
            "(A*256+B)*0.05"        -> (A * 256 + B) * 0.05
            "(A*256+B)/10-40"       -> (A * 256 + B) / 10.0 - 40.0
            "(A*256+B)/10"          -> (A * 256 + B) / 10.0
            "(A*256+B)/16"          -> (A * 256 + B) / 16.0
            "(A*256+B)/100"         -> (A * 256 + B) / 100.0
            "(A-128)*100/128"       -> (A - 128.0) * 100.0 / 128.0
            "A-128"                 -> A - 128.0
            "((A*256+B)-26880)/128" -> ((A * 256 + B) - 26880.0) / 128.0
            "(A*256+B)*0.079"       -> (A * 256 + B) * 0.079
            "A/2-64"                -> A / 2.0 - 64.0
            "A-125"                 -> A - 125.0
            "(A*256+B)*0.0015259"   -> (A * 256 + B) * 0.0015259
            "(A*256+B)*0.1"         -> (A * 256 + B) * 0.1
            "(A*256+B)*0.01"        -> (A * 256 + B) * 0.01
            "(A*256+B)*0.001"       -> (A * 256 + B) * 0.001
            "(A*256+B)*0.25"        -> (A * 256 + B) * 0.25
            "(A*256+B)*4"           -> (A * 256 + B) * 4.0
            "(A*256+B)*0.04"        -> (A * 256 + B) * 0.04
            "(A*256+B)-32768"       -> (A * 256 + B) - 32768.0
            "A*0.1"                 -> A * 0.1
            "A*0.01"                -> A * 0.01
            "A*0.001"               -> A * 0.001
            "A*0.25"                -> A * 0.25
            "A*4"                   -> A * 4.0
            else                    -> A
        }
    }
}


/** Mode 01 PID 01: byte A bits 0-6 contain the stored DTC count. */
class Mode01DtcCountDecoder : PidDecoder {
    override fun decode(rawResponse: String, did: String): Double? {
        val pid = did.removePrefix("01")
        val dataBytes = extractDataBytes(rawResponse, pid)
        if (dataBytes.isEmpty()) return null
        return ((dataBytes[0].toInt() and 0xFF) and 0x7F).toDouble()
    }
}

/** Ford's common one-byte temperature encoding: A - 40 °C. */
class TemperatureDecoder(private val byteIndex: Int = 0) : PidDecoder {
    private val delegate = Unsigned8BitDecoder(byteIndex, scale = 1.0, offset = -40.0)
    override fun decode(rawResponse: String, did: String): Double? = delegate.decode(rawResponse, did)
}

/** Ford's common unsigned 16-bit percentage encoding: raw * 100 / 65535. */
class PercentageDecoder(private val byteIndex: Int = 0) : PidDecoder {
    private val delegate = Unsigned16BitDecoder(byteIndex, scale = 100.0 / 65535.0)
    override fun decode(rawResponse: String, did: String): Double? = delegate.decode(rawResponse, did)
}

/** Ford sensor-voltage encoding used by this profile: unsigned 16-bit millivolts. */
class VoltageDecoder(private val byteIndex: Int = 0) : PidDecoder {
    private val delegate = Unsigned16BitDecoder(byteIndex, scale = 0.001)
    override fun decode(rawResponse: String, did: String): Double? = delegate.decode(rawResponse, did)
}

typealias UnsignedByteDecoder = Unsigned8BitDecoder
