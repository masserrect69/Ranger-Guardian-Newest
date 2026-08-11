package au.com.rangerai.data

/**
 * Decode SAE Mode 01 supported-PID bitmaps (PIDs 00/20/40/60/80/A0/C0).
 * Results are keyed by response CAN ID, e.g. 7E8 (ECM) and 7E9 (TCM).
 */
fun decodeSupportedMode01Pids(rawResponse: String, basePid: Int): Map<String, Set<String>> {
    require(basePid in 0..0xE0 && basePid % 0x20 == 0) { "Invalid support bitmap base PID: $basePid" }

    val echo = "41%02X".format(basePid)
    val result = linkedMapOf<String, MutableSet<String>>()

    parseObdPayloads(rawResponse).forEach { parsed ->
        val payload = parsed.payloadHex
        val echoIndex = payload.indexOf(echo)
        if (echoIndex < 0) return@forEach
        val bitmapStart = echoIndex + echo.length
        if (payload.length < bitmapStart + 8) return@forEach

        val bitmap = payload.substring(bitmapStart, bitmapStart + 8).toLongOrNull(16) ?: return@forEach
        val responseHeader = parsed.responseHeader ?: "UNKNOWN"
        val supported = result.getOrPut(responseHeader) { linkedSetOf() }

        for (offset in 1..32) {
            val mask = 1L shl (32 - offset)
            if ((bitmap and mask) != 0L) {
                supported += "%02X".format(basePid + offset)
            }
        }
    }

    return result.mapValues { it.value.toSet() }
}

fun mergeSupportedPidMaps(
    destination: MutableMap<String, MutableSet<String>>,
    source: Map<String, Set<String>>
) {
    source.forEach { (header, pids) ->
        destination.getOrPut(header) { linkedSetOf() }.addAll(pids)
    }
}
