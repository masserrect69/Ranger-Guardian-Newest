package au.com.rangerai.data

/**
 * Latest decoded vehicle values plus an update time for every PID.
 *
 * A timestamp per parameter prevents stale values from being treated as live
 * after a disconnect or a sensor stops responding.
 */
data class VehicleState(
    val parameters: Map<String, Double> = emptyMap(),
    val parameterTimestamps: Map<String, Long> = emptyMap(),
    val lastUpdateMs: Long = 0L
) {
    fun updateParameter(name: String, value: Double, nowMs: Long = System.currentTimeMillis()): VehicleState =
        copy(
            parameters = parameters + (name to value),
            parameterTimestamps = parameterTimestamps + (name to nowMs),
            lastUpdateMs = nowMs
        )

    fun clearLiveData(): VehicleState = VehicleState()

    operator fun get(name: String): Double? = parameters[name]

    fun ageMs(name: String, nowMs: Long = System.currentTimeMillis()): Long? =
        parameterTimestamps[name]?.let { (nowMs - it).coerceAtLeast(0L) }

    fun isFresh(name: String, maxAgeMs: Long = DEFAULT_FRESHNESS_MS): Boolean =
        parameters.containsKey(name) && (ageMs(name) ?: Long.MAX_VALUE) <= maxAgeMs

    fun freshValue(name: String, maxAgeMs: Long = DEFAULT_FRESHNESS_MS): Double? =
        parameters[name]?.takeIf { isFresh(name, maxAgeMs) }

    fun freshParameters(maxAgeMs: Long = DEFAULT_FRESHNESS_MS): Map<String, Double> =
        parameters.filterKeys { isFresh(it, maxAgeMs) }

    private fun newestValue(vararg names: String): Double? = names
        .mapNotNull { name ->
            val value = parameters[name] ?: return@mapNotNull null
            val timestamp = parameterTimestamps[name] ?: 0L
            Triple(name, value, timestamp)
        }
        .maxByOrNull { it.third }
        ?.second

    // ===== ENGINE CORE =====
    val rpm: Double get() = newestValue("RPM_01", "RPM") ?: 0.0
    val ect: Double get() = newestValue("ECT_01", "ECT", "ECTC") ?: 0.0
    val oilTemp: Double get() = parameters["EOT"] ?: 0.0
    val vss: Double get() = parameters["VSS"] ?: 0.0
    val throttlePos: Double get() = newestValue("THROTTLE_01", "APP") ?: 0.0
    val torque: Double get() = parameters["CET"] ?: 0.0
    val iat: Double get() = newestValue("IAT_01", "IAT11") ?: 0.0
    val ambientTemp: Double get() = newestValue("AAT_01", "AAT") ?: 0.0
    val fuelTemp: Double get() = parameters["FRT"] ?: 0.0

    // ===== BOOST / TURBO =====
    val map: Double get() = newestValue("MAP_01", "MAP", "CIMAP") ?: 0.0
    val mapCmd: Double get() = parameters["MAP_DMD"] ?: 0.0
    val boostActual: Double get() = parameters["BOOST_A"] ?: 0.0
    val boostCommanded: Double get() = parameters["BOOST_A_CMD"] ?: 0.0
    val vgtCmd: Double get() = parameters["VGT_CMD"] ?: 0.0
    val vgtAct: Double get() = parameters["VGT_A_ACT"] ?: 0.0
    val exhaustBackpressure: Double get() = parameters["EX_MAN_PRS"] ?: 0.0

    // ===== FUEL SYSTEM =====
    val frpActual: Double get() = parameters["FRP_A"] ?: 0.0
    val frpCommanded: Double get() = parameters["FRP_A_CMD"] ?: 0.0
    val fuelRailPressure: Double get() = frpActual
    val maf: Double get() = newestValue("MAF_01", "MAF", "MAF_A") ?: 0.0

    // ===== INJECTORS =====
    val injQ1: Double get() = parameters["INJ_Q_1_TOT"] ?: 0.0
    val injQ2: Double get() = parameters["INJ_Q_2_TOT"] ?: 0.0
    val injQ3: Double get() = parameters["INJ_Q_3_TOT"] ?: 0.0
    val injQ4: Double get() = parameters["INJ_Q_4_TOT"] ?: 0.0
    val injQ5: Double get() = parameters["INJ_Q_5_TOT"] ?: 0.0
    val cylBal1: Double get() = parameters["CYL_BAL_1"] ?: 0.0
    val cylBal2: Double get() = parameters["CYL_BAL_2"] ?: 0.0
    val cylBal3: Double get() = parameters["CYL_BAL_3"] ?: 0.0
    val cylBal4: Double get() = parameters["CYL_BAL_4"] ?: 0.0
    val cylBal5: Double get() = parameters["CYL_BAL_5"] ?: 0.0

    // ===== EGR =====
    val egrPos: Double get() = parameters["EGR_A_ACT"] ?: 0.0
    val egrCmd: Double get() = parameters["EGR_PCT"] ?: 0.0

    // ===== DPF =====
    val dpfSootPct: Double get() = newestValue("DPF_SOOT_RANGER", "DPF_SOOT_PCT_OL") ?: 0.0
    val dpfDiffPressure: Double get() = parameters["DPF_PRESS_DIF"] ?: 0.0
    val dpfTempIn: Double get() = parameters["EGT12"] ?: 0.0
    val dpfTempOut: Double get() = parameters["EGT13"] ?: 0.0
    val dpfRegenStatus: Double get() = parameters["DPF_REGEN"] ?: 0.0
    val dpfAshLoad: Double get() = parameters["DPF_ASH_LOAD"] ?: 0.0
    val dpfDistSinceRegen: Double get() = parameters["DIST_LAST_DPF"] ?: 0.0

    // ===== EXHAUST TEMPS =====
    val egt11: Double get() = newestValue("EGT_PRE_RANGER", "EGT11") ?: 0.0
    val egt12: Double get() = parameters["EGT12"] ?: 0.0
    val egt13: Double get() = parameters["EGT13"] ?: 0.0
    val egt14: Double get() = parameters["EGT14"] ?: 0.0

    // ===== TRANSMISSION =====
    val tft: Double get() = parameters["TFT"] ?: 0.0
    val gearCmd: Double get() = parameters["GEAR_CMD"] ?: 0.0
    val gearAct: Double get() = parameters["GEAR_ACT"] ?: 0.0
    val tcSlip: Double get() = parameters["TC_SLIP"] ?: 0.0
    val tss: Double get() = parameters["TSS"] ?: 0.0
    val tccSlip: Double get() = parameters["TCC_SLIP"] ?: 0.0

    // ===== ELECTRICAL =====
    val batteryVoltage: Double get() = newestValue("BATT_V_01", "BATT_V") ?: 0.0

    companion object {
        const val DEFAULT_FRESHNESS_MS = 5_000L
    }
}
