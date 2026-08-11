package au.com.rangerai.data.vehicles

/**
 * Supported vehicle manufacturers.
 */
enum class Manufacturer(val displayName: String, val vinPrefixes: List<String>) {
    FORD("Ford", listOf("1FA", "1FB", "1FC", "1FD", "1FE", "1FF", "2FA", "2FB", "3FA", "3FB", "WF0", "MPB")),
    TOYOTA("Toyota", listOf("JT", "4T", "5T", "2T")),
    MAZDA("Mazda", listOf("JM1", "JM3", "JM6")),
    VOLKSWAGEN("Volkswagen", listOf("WVW", "WV1", "WV2", "WV3")),
    AUDI("Audi", listOf("WAU", "TRU")),
    BMW("BMW", listOf("WBA", "WBS", "WBX", "WBY")),
    MERCEDES("Mercedes-Benz", listOf("WDB", "WDD", "WDC")),
    HOLDEN("Holden", listOf("6G1", "6G2", "6G3")),
    HYUNDAI("Hyundai", listOf("KMH", "KMJ")),
    KIA("Kia", listOf("KNA", "KND", "KNE")),
    NISSAN("Nissan", listOf("JN1", "JN3", "JN6", "JN8", "5N1")),
    SUBARU("Subaru", listOf("JF1", "JF2", "4S3", "4S4")),
    MITSUBISHI("Mitsubishi", listOf("JA3", "JA4", "JA32")),
    HONDA("Honda", listOf("JHM", "1HG", "2HG", "3HG")),
    ISUZU("Isuzu", listOf("JAA", "JAB")),
    VOLVO("Volvo", listOf("YV1", "YV4")),
    JEEP("Jeep", listOf("1C4", "1J4", "1J8")),
    DODGE_RAM("Dodge/Ram", listOf("1C3", "1D3", "1D7", "3C6")),
    LAND_ROVER("Land Rover", listOf("SAL")),
    PORSCHE("Porsche", listOf("WP0", "WP1")),
    LEXUS("Lexus", listOf("JTH", "JTHB", "JTHF")),
    UNIVERSAL("Universal OBD-II", emptyList())
}

/**
 * OBD-II communication protocols.
 */
enum class ObdProtocol(val atCommand: String, val description: String) {
    AUTO("ATSP0", "Auto-detect"),
    CAN_11BIT_500("ATSP6", "CAN 11-bit 500 kbps"),
    CAN_29BIT_500("ATSP7", "CAN 29-bit 500 kbps"),
    CAN_11BIT_250("ATSP8", "CAN 11-bit 250 kbps"),
    CAN_29BIT_250("ATSP9", "CAN 29-bit 250 kbps"),
    ISO9141("ATSP3", "ISO 9141-2"),
    KWP2000_FAST("ATSP5", "KWP2000 Fast"),
    KWP2000_SLOW("ATSP4", "KWP2000 Slow"),
    J1850_PWM("ATSP1", "J1850 PWM"),
    J1850_VPW("ATSP2", "J1850 VPW")
}

/**
 * PID categories for grouping in the Parameters screen.
 */
enum class PidCategory(val displayName: String, val colorHex: String) {
    ENGINE("Engine", "#00D4FF"),
    TURBO("Turbo/Boost", "#FF8C00"),
    FUEL("Fuel System", "#FFD600"),
    INJECTORS("Injectors", "#FF8C00"),
    AIRFLOW("Airflow/MAF", "#00E676"),
    EGR("EGR System", "#FF8C00"),
    DPF("DPF/Exhaust", "#FF1744"),
    EXHAUST("Exhaust", "#FF1744"),
    TRANSMISSION("Transmission", "#66E5FF"),
    BRAKES("Brakes/ABS", "#FF1744"),
    ELECTRICAL("Electrical", "#FFD600"),
    CLIMATE("Climate/AC", "#00D4FF"),
    BODY("Body/BCM", "#66E5FF"),
    EMISSIONS("Emissions", "#00E676"),
    COOLING("Cooling", "#00D4FF"),
    STEERING("Steering", "#66E5FF"),
    SUSPENSION("Suspension", "#66E5FF"),
    DENOX("DeNOx/AdBlue", "#00E676"),
    HYBRID("Hybrid/EV", "#00E676"),
    MISC("Miscellaneous", "#FFFFFF")
}

/**
 * Polling frequency groups.
 */
enum class PollGroup(val intervalMs: Long, val label: String) {
    FAST(100L, "10 Hz"),
    MEDIUM(500L, "2 Hz"),
    SLOW(2000L, "0.5 Hz"),
    ON_DEMAND(0L, "On Request")
}

/**
 * Severity of a known vehicle quirk.
 */
enum class QuirkSeverity {
    INFO,
    SUPPRESS_ALERT,
    MODIFIED
}

/**
 * A known quirk or modification for a specific vehicle.
 */
data class VehicleQuirk(
    val description: String,
    val pidAffected: String? = null,
    val severity: QuirkSeverity = QuirkSeverity.INFO,
    val aiInstruction: String = ""
)

/**
 * A manufacturer-specific PID definition.
 */
data class ManufacturerPid(
    val did: String,
    val name: String,
    val description: String,
    val unit: String,
    val category: PidCategory,
    val minValue: Double = 0.0,
    val maxValue: Double = 100.0,
    val formula: String,
    val byteCount: Int = 2,
    val pollGroup: PollGroup = PollGroup.MEDIUM,
    val warningThreshold: Double? = null,
    val criticalThreshold: Double? = null
)

/**
 * A complete vehicle profile with ECU addresses and PID list.
 */
data class VehicleProfile(
    val manufacturer: Manufacturer,
    val model: String,
    val engine: String,
    val year: Int,
    val ecuAddress: String = "7E0",
    val responseAddress: String = "7E8",
    val protocol: ObdProtocol = ObdProtocol.CAN_11BIT_500,
    val mode22Pids: List<ManufacturerPid> = emptyList(),
    val knownQuirks: List<VehicleQuirk> = emptyList(),
    val aiContext: String = ""
)
