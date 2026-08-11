package au.com.rangerai.data

import au.com.rangerai.data.vehicles.Manufacturer
import au.com.rangerai.data.vehicles.VehicleRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObdParsingTest {

    @Test
    fun compactTorqueResponsesDecodeWithHeaders() {
        val coolant = "7e80341053c"
        val parsed = parseObdPayloads(coolant).single()
        assertEquals("7E8", parsed.responseHeader)
        assertEquals("41053C", parsed.payloadHex)
        assertEquals(20.0, Mode01Decoder("A-40").decode(coolant, "0105")!!, 0.001)

        val voltage = "7e804414237dc"
        assertEquals(14.300, Mode01Decoder("(A*256+B)*0.001", 2).decode(voltage, "0142")!!, 0.001)

        val catalystTemperature = "7e804413c0408"
        assertEquals(63.2, Mode01Decoder("(A*256+B)/10-40", 2).decode(catalystTemperature, "013C")!!, 0.001)
    }

    @Test
    fun supportedPidBitmapKeepsEcmAndTcmSeparate() {
        val raw = "7e9064100981a0013\n7e8064100983b8013"
        val result = decodeSupportedMode01Pids(raw, 0x00)
        assertTrue("05" in result.getValue("7E8"))
        assertTrue("0C" in result.getValue("7E9"))
    }

    @Test
    fun longCompactAndNumberedIsoTpResponsesAreReassembled() {
        val torqueLong = "7e800417f030162941e005da402000000000000000000"
        val parsedLong = parseObdPayloads(torqueLong).single()
        assertEquals("7E8", parsedLong.responseHeader)
        assertTrue(parsedLong.payloadHex.startsWith("417F"))

        val numbered = "7E8 10 0A 62 24 2C 00 64 12\r7E8 21 34 56 78 9A BC DE EF\r>"
        assertEquals("62242C0064123456789A", parseObdPayloads(numbered).single().payloadHex)
    }

    @Test
    fun rangerSpecificFormulasMatchSuppliedDefinitions() {
        assertEquals(
            90.0,
            Unsigned16BitDecoder(scale = 1.0 / 16.0).decode("7E9 05 62 1E 1C 05 A0", "1E1C")!!,
            0.001
        )
        assertEquals(
            50.0,
            Unsigned16BitDecoder(scale = 0.01).decode("7E8 05 62 24 2C 13 88", "242C")!!,
            0.001
        )
        assertEquals(
            500.0,
            Unsigned16BitDecoder(scale = 1.0).decode("7E8 05 62 24 25 01 F4", "2425")!!,
            0.001
        )
    }

    @Test
    fun dtcsAndRangerVinAreDecoded() {
        val dtcs = parseDiagnosticTroubleCodes("7E8 06 43 04 04 C1 84 00", DtcStatus.STORED)
        assertEquals(listOf("P0404", "U0184"), dtcs.map { it.code })
        assertEquals(Manufacturer.FORD, VehicleRegistry.detectManufacturer("MPB" + "0".repeat(14)))
    }

    @Test
    fun registryHasNoDuplicateRuntimeKeys() {
        val registry = PidRegistry().allPids
        assertEquals(registry.size, registry.map { it.key }.distinct().size)
        assertEquals(registry.size, registry.map { it.name }.distinct().size)
    }
}
