package au.com.rangerai.data

enum class DtcStatus(val label: String) {
    STORED("Stored"),
    PENDING("Pending"),
    PERMANENT("Permanent")
}

data class DiagnosticTroubleCode(
    val code: String,
    val status: DtcStatus,
    val responseHeader: String?,
    val description: String? = null
)

/**
 * Parses positive Mode 03, 07 and 0A responses from every responding ECU.
 * The response header is retained so ECM (7E8) and TCM (7E9) codes remain
 * distinguishable even when they report the same five-character DTC.
 */
fun parseDiagnosticTroubleCodes(rawResponse: String, status: DtcStatus): List<DiagnosticTroubleCode> {
    val positiveService = when (status) {
        DtcStatus.STORED -> "43"
        DtcStatus.PENDING -> "47"
        DtcStatus.PERMANENT -> "4A"
    }

    return parseObdPayloads(rawResponse).flatMap { parsed ->
        val payload = parsed.payloadHex
        val start = payload.indexOf(positiveService)
        if (start < 0) return@flatMap emptyList()

        val data = payload.drop(start + positiveService.length)
        data.chunked(4)
            .filter { it.length == 4 && it != "0000" }
            .mapNotNull { rawCode ->
                rawCode.toIntOrNull(16)?.let { value ->
                    DiagnosticTroubleCode(
                        code = decodeDtc(value),
                        status = status,
                        responseHeader = parsed.responseHeader
                    )
                }
            }
    }.distinctBy { Triple(it.code, it.status, it.responseHeader) }
}

private fun decodeDtc(value: Int): String {
    val system = when ((value ushr 14) and 0x03) {
        0 -> 'P'
        1 -> 'C'
        2 -> 'B'
        else -> 'U'
    }
    val firstDigit = (value ushr 12) and 0x03
    val remaining = value and 0x0FFF
    return "$system$firstDigit${remaining.toString(16).uppercase().padStart(3, '0')}"
}
