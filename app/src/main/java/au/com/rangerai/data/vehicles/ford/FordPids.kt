package au.com.rangerai.data.vehicles.ford

import au.com.rangerai.data.vehicles.Manufacturer
import au.com.rangerai.data.vehicles.ManufacturerPid
import au.com.rangerai.data.vehicles.ObdProtocol
import au.com.rangerai.data.vehicles.PidCategory
import au.com.rangerai.data.vehicles.PollGroup
import au.com.rangerai.data.vehicles.QuirkSeverity
import au.com.rangerai.data.vehicles.VehicleProfile
import au.com.rangerai.data.vehicles.VehicleQuirk

/**
 * Small, vehicle-specific Ford PID profile for the user's 2018 Ranger 3.2L.
 *
 * The previous generic Ford catalogue contained duplicate DIDs assigned to
 * unrelated sensors. That made it impossible to know which formula was being
 * used. Only the Ranger-specific definitions supplied for this vehicle are
 * retained here. They are still marked experimental in the live registry and
 * must be checked against captured raw responses before being used for alerts.
 */
object FordPids {

    fun getDefaultProfile(): VehicleProfile = VehicleProfile(
        manufacturer = Manufacturer.FORD,
        model = "2018 Ranger PX MkII",
        engine = "3.2L five-cylinder Duratorq / P5AT",
        year = 2018,
        ecuAddress = "7E0",
        responseAddress = "7E8",
        protocol = ObdProtocol.CAN_11BIT_500,
        mode22Pids = allPids,
        knownQuirks = listOf(
            VehicleQuirk(
                description = "Ford Mode 22 equations are profile-specific and require live validation",
                pidAffected = null,
                severity = QuirkSeverity.SUPPRESS_ALERT,
                aiInstruction = "Experimental Ford values are excluded from automatic health conclusions until validated."
            )
        ),
        aiContext = "2018 Ford Ranger 3.2L five-cylinder diesel with 6R80 automatic. Protocol ISO 15765-4 CAN 11-bit 500 kbit/s. ECM replies on 7E8 and TCM replies on 7E9."
    )

    val allPids: List<ManufacturerPid> = listOf(
        ManufacturerPid(
            did = "1E1C",
            name = "Transmission Fluid Temperature",
            description = "6R80 automatic transmission fluid temperature",
            unit = "°C",
            category = PidCategory.TRANSMISSION,
            minValue = -40.0,
            maxValue = 180.0,
            formula = "(A*256+B)/16",
            byteCount = 2,
            pollGroup = PollGroup.MEDIUM
        ),
        ManufacturerPid(
            did = "2425",
            name = "EGT Pre-Turbo",
            description = "Exhaust gas temperature before the turbocharger",
            unit = "°C",
            category = PidCategory.EXHAUST,
            minValue = -40.0,
            maxValue = 1000.0,
            formula = "A*256+B",
            byteCount = 2,
            pollGroup = PollGroup.MEDIUM
        ),
        ManufacturerPid(
            did = "242C",
            name = "DPF Soot Load",
            description = "Ranger profile DPF soot load",
            unit = "%",
            category = PidCategory.DPF,
            minValue = 0.0,
            maxValue = 150.0,
            formula = "(A*256+B)/100",
            byteCount = 2,
            pollGroup = PollGroup.MEDIUM
        )
    )
}
