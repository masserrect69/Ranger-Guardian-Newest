package au.com.rangerai.data

interface PidDecoder {
    fun decode(rawResponse: String, did: String): Double?
}
