package au.com.rangerai.data.vehicles

import au.com.rangerai.data.vehicles.ford.FordPids

/**
 * Registry for looking up vehicle profiles by manufacturer.
 */
object VehicleRegistry {

    /**
     * Detect manufacturer from VIN prefix.
     */
    fun detectManufacturer(vin: String): Manufacturer {
        val upperVin = vin.uppercase()
        for (manufacturer in Manufacturer.values()) {
            if (manufacturer == Manufacturer.UNIVERSAL) continue
            for (prefix in manufacturer.vinPrefixes) {
                if (upperVin.startsWith(prefix)) {
                    return manufacturer
                }
            }
        }
        return Manufacturer.UNIVERSAL
    }

    /**
     * Get all available manufacturers (excluding UNIVERSAL).
     */
    fun getAvailableProfiles(): List<Manufacturer> {
        return Manufacturer.values().filter { it != Manufacturer.UNIVERSAL }
    }

    /**
     * Get the vehicle profile for a given manufacturer.
     */
    fun getProfileForManufacturer(manufacturer: Manufacturer): VehicleProfile {
        return when (manufacturer) {
            Manufacturer.FORD -> FordPids.getDefaultProfile()
            else -> getUniversalProfile()
        }
    }

    /**
     * Get a universal OBD-II profile (Mode 01 only).
     */
    fun getUniversalProfile(): VehicleProfile {
        return VehicleProfile(
            manufacturer = Manufacturer.UNIVERSAL,
            model = "Generic OBD-II Vehicle",
            engine = "Unknown",
            year = 2000,
            ecuAddress = "7E0",
            responseAddress = "7E8",
            protocol = ObdProtocol.AUTO,
            mode22Pids = emptyList(),
            knownQuirks = emptyList(),
            aiContext = "Generic OBD-II vehicle. Only standard Mode 01 PIDs are available."
        )
    }
}
