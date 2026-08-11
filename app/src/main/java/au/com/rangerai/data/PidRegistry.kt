package au.com.rangerai.data

/**
 * PID registry for the configured Ford Ranger diesel profile.
 * Uses one authoritative list for polling, display and AI context.  Standard
 * Mode 01 entries are validated against the supplied Torque Pro scan; Ford
 * Mode 22 equations remain profile-specific and are guarded by range checks.
 *
 * Verified protocol: ISO 15765-4 CAN 11/500
 * Request headers: ECM 7E0, TCM 7E1 (responses 7E8 and 7E9)
 */
class PidRegistry {
    val allPids: List<PidDefinition> by lazy {
        buildList().also { pids ->
            val duplicateKeys = pids.groupBy { it.key }.filterValues { it.size > 1 }.keys
            require(duplicateKeys.isEmpty()) { "Duplicate PID keys: ${duplicateKeys.joinToString()}" }
            val duplicateNames = pids.groupBy { it.name }.filterValues { it.size > 1 }.keys
            require(duplicateNames.isEmpty()) { "Duplicate PID names: ${duplicateNames.joinToString()}" }
        }
    }

    fun getHighPriorityPids(): List<PidDefinition> = allPids.filter { it.priority == PidPriority.HIGH }
    fun getMediumPriorityPids(): List<PidDefinition> = allPids.filter { it.priority == PidPriority.MEDIUM }

    private fun buildList(): List<PidDefinition> = mutableListOf<PidDefinition>().apply {
        // ===== VERIFIED STANDARD MODE 01 PIDS =====
        // The vLinker/Torque scan supplied for this Ranger reports these PIDs as
        // supported by the 7E8 ECM. The poller also performs live bitmap discovery
        // and skips any PID not advertised by the connected ECU.
        add(PidDefinition("DTC_COUNT_01", "0101", "Stored DTC Count / MIL Status", "", PidCategory.MISCELLANEOUS, PidPriority.LOW, 0.0, 127.0, Mode01DtcCountDecoder(), "7E0", 1))
        add(PidDefinition("ENG_LOAD", "0104", "Calculated Engine Load", "%", PidCategory.ENGINE_CORE, PidPriority.MEDIUM, 0.0, 100.0, Mode01Decoder("A*100/255", 1), "7E0", 1))
        add(PidDefinition("ECT_01", "0105", "Engine Coolant Temperature", "°C", PidCategory.ENGINE_CORE, PidPriority.HIGH, -40.0, 150.0, Mode01Decoder("A-40", 1), "7E0", 1))
        add(PidDefinition("MAP_01", "010B", "Intake Manifold Absolute Pressure", "kPa", PidCategory.BOOST_TURBO, PidPriority.HIGH, 0.0, 255.0, Mode01Decoder("A", 1), "7E0", 1))
        add(PidDefinition("RPM_01", "010C", "Engine RPM", "rpm", PidCategory.ENGINE_CORE, PidPriority.HIGH, 0.0, 5000.0, Mode01Decoder("(A*256+B)/4", 2), "7E0", 1))
        add(PidDefinition("VSS", "010D", "Vehicle Speed", "km/h", PidCategory.ENGINE_CORE, PidPriority.HIGH, 0.0, 255.0, Mode01Decoder("A", 1), "7E0", 1))
        add(PidDefinition("IAT_01", "010F", "Intake Air Temperature", "°C", PidCategory.AIR_FLOW, PidPriority.MEDIUM, -40.0, 150.0, Mode01Decoder("A-40", 1), "7E0", 1))
        add(PidDefinition("MAF_01", "0110", "Mass Air Flow", "g/s", PidCategory.AIR_FLOW, PidPriority.MEDIUM, 0.0, 655.35, Mode01Decoder("(A*256+B)*0.01", 2), "7E0", 1))
        add(PidDefinition("THROTTLE_01", "0111", "Throttle Position", "%", PidCategory.ENGINE_CORE, PidPriority.MEDIUM, 0.0, 100.0, Mode01Decoder("A*100/255", 1), "7E0", 1))
        add(PidDefinition("OBD_STANDARD_01", "011C", "OBD Standard Identifier", "", PidCategory.MISCELLANEOUS, PidPriority.LOW, 0.0, 255.0, Mode01Decoder("A", 1), "7E0", 1))
        add(PidDefinition("RUN_TIME_01", "011F", "Run Time Since Engine Start", "s", PidCategory.ENGINE_CORE, PidPriority.LOW, 0.0, 65535.0, Mode01Decoder("A*256+B", 2), "7E0", 1))
        add(PidDefinition("DIST_MIL_01", "0121", "Distance Travelled With MIL On", "km", PidCategory.MISCELLANEOUS, PidPriority.LOW, 0.0, 65535.0, Mode01Decoder("A*256+B", 2), "7E0", 1))
        add(PidDefinition("EGR_CMD_01", "012C", "Commanded EGR", "%", PidCategory.EGR_SYSTEM, PidPriority.MEDIUM, 0.0, 100.0, Mode01Decoder("A*100/255", 1), "7E0", 1))
        add(PidDefinition("EGR_ERROR_01", "012D", "EGR Error", "%", PidCategory.EGR_SYSTEM, PidPriority.MEDIUM, -100.0, 100.0, Mode01Decoder("(A-128)*100/128", 1), "7E0", 1))
        add(PidDefinition("FUEL_LEVEL_01", "012F", "Fuel Level Input", "%", PidCategory.FUEL_SYSTEM, PidPriority.LOW, 0.0, 100.0, Mode01Decoder("A*100/255", 1), "7E0", 1))
        add(PidDefinition("WARMUPS_01", "0130", "Warm-ups Since Codes Cleared", "", PidCategory.MISCELLANEOUS, PidPriority.LOW, 0.0, 255.0, Mode01Decoder("A", 1), "7E0", 1))
        add(PidDefinition("DIST_CLEAR_01", "0131", "Distance Since Codes Cleared", "km", PidCategory.MISCELLANEOUS, PidPriority.LOW, 0.0, 65535.0, Mode01Decoder("A*256+B", 2), "7E0", 1))
        add(PidDefinition("BARO_01", "0133", "Barometric Pressure", "kPa", PidCategory.BOOST_TURBO, PidPriority.LOW, 0.0, 255.0, Mode01Decoder("A", 1), "7E0", 1))
        add(PidDefinition("CAT_TEMP_B1S1_01", "013C", "Catalyst Temperature Bank 1 Sensor 1", "°C", PidCategory.EXHAUST_TEMPS, PidPriority.LOW, -40.0, 1000.0, Mode01Decoder("(A*256+B)/10-40", 2), "7E0", 1))
        add(PidDefinition("CAT_TEMP_B1S2_01", "013E", "Catalyst Temperature Bank 1 Sensor 2", "°C", PidCategory.EXHAUST_TEMPS, PidPriority.LOW, -40.0, 1000.0, Mode01Decoder("(A*256+B)/10-40", 2), "7E0", 1))
        add(PidDefinition("BATT_V_01", "0142", "Control Module Voltage", "V", PidCategory.ELECTRICAL, PidPriority.MEDIUM, 0.0, 18.0, Mode01Decoder("(A*256+B)*0.001", 2), "7E0", 1))
        add(PidDefinition("REL_THROTTLE_01", "0145", "Relative Throttle Position", "%", PidCategory.ENGINE_CORE, PidPriority.MEDIUM, 0.0, 100.0, Mode01Decoder("A*100/255", 1), "7E0", 1))
        add(PidDefinition("AAT_01", "0146", "Ambient Air Temperature", "°C", PidCategory.ENGINE_CORE, PidPriority.LOW, -40.0, 100.0, Mode01Decoder("A-40", 1), "7E0", 1))
        add(PidDefinition("APP_D_01", "0149", "Accelerator Pedal Position D", "%", PidCategory.ENGINE_CORE, PidPriority.MEDIUM, 0.0, 100.0, Mode01Decoder("A*100/255", 1), "7E0", 1))
        add(PidDefinition("APP_E_01", "014A", "Accelerator Pedal Position E", "%", PidCategory.ENGINE_CORE, PidPriority.MEDIUM, 0.0, 100.0, Mode01Decoder("A*100/255", 1), "7E0", 1))
        add(PidDefinition("THROTTLE_CMD_01", "014C", "Commanded Throttle Actuator", "%", PidCategory.ENGINE_CORE, PidPriority.MEDIUM, 0.0, 100.0, Mode01Decoder("A*100/255", 1), "7E0", 1))
        add(PidDefinition("TIME_MIL_01", "014D", "Run Time With MIL On", "min", PidCategory.MISCELLANEOUS, PidPriority.LOW, 0.0, 65535.0, Mode01Decoder("A*256+B", 2), "7E0", 1))
        add(PidDefinition("TIME_CLEAR_01", "014E", "Time Since Trouble Codes Cleared", "min", PidCategory.MISCELLANEOUS, PidPriority.LOW, 0.0, 65535.0, Mode01Decoder("A*256+B", 2), "7E0", 1))
        add(PidDefinition("FUEL_TYPE_01", "0151", "Fuel Type Identifier", "", PidCategory.FUEL_SYSTEM, PidPriority.LOW, 0.0, 255.0, Mode01Decoder("A", 1), "7E0", 1))
        add(PidDefinition("FRP_ABS_01", "0159", "Fuel Rail Absolute Pressure", "kPa", PidCategory.FUEL_SYSTEM, PidPriority.MEDIUM, 0.0, 655350.0, Mode01Decoder("(A*256+B)*10", 2), "7E0", 1))
        add(PidDefinition("REL_ACCEL_01", "015A", "Relative Accelerator Pedal Position", "%", PidCategory.ENGINE_CORE, PidPriority.MEDIUM, 0.0, 100.0, Mode01Decoder("A*100/255", 1), "7E0", 1))
        add(PidDefinition("INJ_TIMING_01", "015D", "Fuel Injection Timing", "°", PidCategory.FUEL_SYSTEM, PidPriority.MEDIUM, -210.0, 302.0, Mode01Decoder("((A*256+B)-26880)/128", 2), "7E0", 1))
        add(PidDefinition("FUEL_RATE_01", "015E", "Engine Fuel Rate", "L/h", PidCategory.FUEL_SYSTEM, PidPriority.MEDIUM, 0.0, 3276.75, Mode01Decoder("(A*256+B)*0.05", 2), "7E0", 1))
        add(PidDefinition("DRIVER_TORQUE_01", "0161", "Driver Demand Engine Torque", "%", PidCategory.ENGINE_CORE, PidPriority.MEDIUM, -125.0, 130.0, Mode01Decoder("A-125", 1), "7E0", 1))
        add(PidDefinition("ACTUAL_TORQUE_01", "0162", "Actual Engine Torque", "%", PidCategory.ENGINE_CORE, PidPriority.MEDIUM, -125.0, 130.0, Mode01Decoder("A-125", 1), "7E0", 1))
        add(PidDefinition("REFERENCE_TORQUE_01", "0163", "Engine Reference Torque", "Nm", PidCategory.ENGINE_CORE, PidPriority.LOW, 0.0, 65535.0, Mode01Decoder("A*256+B", 2), "7E0", 1))
        add(PidDefinition("FRICTION_TORQUE_01", "018E", "Engine Friction Percent Torque", "%", PidCategory.ENGINE_CORE, PidPriority.LOW, -125.0, 130.0, Mode01Decoder("A-125", 1), "7E0", 1))

        // ===== USER-SUPPLIED RANGER-SPECIFIC MODE 22 PIDS =====
        // These definitions came from the user's 2018 Ranger 3.2L / 6R80
        // Torque references. They remain opt-in until live raw responses are
        // captured alongside a trusted reference value.
        add(PidDefinition("EGT_PRE_RANGER", "2425", "EGT Pre-Turbo (Ranger profile)", "°C", PidCategory.EXHAUST_TEMPS, PidPriority.MEDIUM, -40.0, 1000.0, Unsigned16BitDecoder(0, 1.0), "7E0", 0))
        add(PidDefinition("DPF_SOOT_RANGER", "242C", "DPF Soot Load (Ranger profile)", "%", PidCategory.DPF_SYSTEM, PidPriority.MEDIUM, 0.0, 150.0, Unsigned16BitDecoder(0, 0.01), "7E0", 0))

        // ===== EXPERIMENTAL FORD MODE 22 DIDS =====
        // These remain available for this private Ranger profile, but are opt-in
        // because the supplied Torque exports do not include the engineering
        // equations needed to independently validate each Ford DID.
        add(PidDefinition("RPM", "F40C", "Engine Speed", "rpm", PidCategory.ENGINE_CORE, PidPriority.HIGH, 0.0, 5000.0, Unsigned16BitDecoder(0, 0.25), "7E0", 0))
        add(PidDefinition("ECT", "F405", "Engine Coolant Temperature", "°C", PidCategory.ENGINE_CORE, PidPriority.HIGH, -40.0, 150.0, TemperatureDecoder(0), "7E0", 0))
        add(PidDefinition("EOT", "F446", "Engine Oil Temperature", "°C", PidCategory.ENGINE_CORE, PidPriority.MEDIUM, -40.0, 200.0, TemperatureDecoder(0), "7E0", 0))
        add(PidDefinition("EOTV", "F447", "Engine Oil Temperature Voltage", "V", PidCategory.ENGINE_CORE, PidPriority.LOW, 0.0, 5.0, VoltageDecoder(0), "7E0", 0))
        add(PidDefinition("APP", "F449", "Accelerator Pedal Position", "%", PidCategory.ENGINE_CORE, PidPriority.HIGH, 0.0, 100.0, PercentageDecoder(0), "7E0", 0))
        add(PidDefinition("APP1_V", "F44A", "Accelerator Pedal Sensor 1 Voltage", "V", PidCategory.ENGINE_CORE, PidPriority.LOW, 0.0, 5.0, VoltageDecoder(0), "7E0", 0))
        add(PidDefinition("APP2_V", "F44B", "Accelerator Pedal Sensor 2 Voltage", "V", PidCategory.ENGINE_CORE, PidPriority.LOW, 0.0, 5.0, VoltageDecoder(0), "7E0", 0))
        add(PidDefinition("CET", "F462", "Calculated Engine Torque", "Nm", PidCategory.ENGINE_CORE, PidPriority.MEDIUM, 0.0, 600.0, Unsigned16BitDecoder(0, 1.0, -32768.0), "7E0", 0))
        add(PidDefinition("BATT_V", "F442", "Battery Voltage", "V", PidCategory.ENGINE_CORE, PidPriority.MEDIUM, 0.0, 18.0, Unsigned16BitDecoder(0, 0.001), "7E0", 0))
        add(PidDefinition("ARPMDES", "F450", "Desired Idle RPM", "rpm", PidCategory.ENGINE_CORE, PidPriority.LOW, 0.0, 2000.0, Unsigned16BitDecoder(0, 0.25), "7E0", 0))
        add(PidDefinition("ENG_CRANK", "DD01", "Engine Cranking", "", PidCategory.ENGINE_CORE, PidPriority.LOW, 0.0, 1.0, BooleanDecoder(0), "7E0", 0))
        add(PidDefinition("ENG_ST_PCM", "DD02", "Engine Start PCM", "", PidCategory.ENGINE_CORE, PidPriority.LOW, 0.0, 1.0, BooleanDecoder(0), "7E0", 0))
        add(PidDefinition("BOOST_A", "F42F", "Actual Boost", "%", PidCategory.BOOST_TURBO, PidPriority.HIGH, 0.0, 300.0, Unsigned16BitDecoder(0, 0.01), "7E0", 0))
        add(PidDefinition("BOOST_A_CMD", "F430", "Commanded Boost", "%", PidCategory.BOOST_TURBO, PidPriority.HIGH, 0.0, 300.0, Unsigned16BitDecoder(0, 0.01), "7E0", 0))
        add(PidDefinition("BOOST_A_STATUS", "F431", "Boost Actuator Status", "", PidCategory.BOOST_TURBO, PidPriority.LOW, 0.0, 255.0, UnsignedByteDecoder(0), "7E0", 0))
        add(PidDefinition("MAP", "F40B", "Manifold Absolute Pressure", "kPa", PidCategory.BOOST_TURBO, PidPriority.HIGH, 0.0, 400.0, Unsigned16BitDecoder(0, 0.1), "7E0", 0))
        add(PidDefinition("MAP_DMD", "F40D", "MAP Demand", "kPa", PidCategory.BOOST_TURBO, PidPriority.MEDIUM, 0.0, 400.0, Unsigned16BitDecoder(0, 0.1), "7E0", 0))
        add(PidDefinition("MAP_V", "F40E", "MAP Sensor Voltage", "V", PidCategory.BOOST_TURBO, PidPriority.LOW, 0.0, 5.0, VoltageDecoder(0), "7E0", 0))
        add(PidDefinition("BARO_V", "F433", "Barometric Pressure Voltage", "V", PidCategory.BOOST_TURBO, PidPriority.LOW, 0.0, 5.0, VoltageDecoder(0), "7E0", 0))
        add(PidDefinition("TCBP", "F470", "Turbo Compressor Bypass Pressure", "kPa", PidCategory.BOOST_TURBO, PidPriority.HIGH, 0.0, 400.0, Unsigned16BitDecoder(0, 0.1), "7E0", 0))
        add(PidDefinition("TCBP_DSD", "F471", "Turbo Compressor BP Desired", "kPa", PidCategory.BOOST_TURBO, PidPriority.MEDIUM, 0.0, 400.0, Unsigned16BitDecoder(0, 0.1), "7E0", 0))
        add(PidDefinition("TURBO_SOV_DSD", "F472", "Turbo SOV Desired", "%", PidCategory.BOOST_TURBO, PidPriority.MEDIUM, 0.0, 100.0, PercentageDecoder(0), "7E0", 0))
        add(PidDefinition("TURBO_SOV_MES", "F473", "Turbo SOV Measured", "%", PidCategory.BOOST_TURBO, PidPriority.MEDIUM, 0.0, 100.0, PercentageDecoder(0), "7E0", 0))
        add(PidDefinition("VGTDC", "F474", "VGT Duty Cycle", "%", PidCategory.BOOST_TURBO, PidPriority.HIGH, 0.0, 100.0, PercentageDecoder(0), "7E0", 0))
        add(PidDefinition("VGT_A_ACT", "F475", "VGT Actual Position", "%", PidCategory.BOOST_TURBO, PidPriority.HIGH, 0.0, 100.0, Unsigned16BitDecoder(0, 0.01), "7E0", 0))
        add(PidDefinition("VGT_A_STAT", "F476", "VGT Actuator Status", "", PidCategory.BOOST_TURBO, PidPriority.LOW, 0.0, 255.0, UnsignedByteDecoder(0), "7E0", 0))
        add(PidDefinition("VGT_CMD", "F477", "VGT Commanded Position", "%", PidCategory.BOOST_TURBO, PidPriority.MEDIUM, 0.0, 100.0, Unsigned16BitDecoder(0, 0.01), "7E0", 0))
        add(PidDefinition("EX_MAN_PRS", "F478", "Exhaust Manifold Pressure", "kPa", PidCategory.BOOST_TURBO, PidPriority.MEDIUM, 0.0, 400.0, Unsigned16BitDecoder(0, 0.1), "7E0", 0))
        add(PidDefinition("BOO2", "F479", "Boost Secondary", "", PidCategory.BOOST_TURBO, PidPriority.LOW, 0.0, 1.0, BooleanDecoder(0), "7E0", 0))
        add(PidDefinition("FRP_A", "F423", "Fuel Rail Pressure Actual", "kPa", PidCategory.FUEL_SYSTEM, PidPriority.HIGH, 0.0, 200000.0, Unsigned16BitDecoder(0, 10.0), "7E0", 0))
        add(PidDefinition("FRP_ABS", "F424", "Fuel Rail Pressure Absolute", "kPa", PidCategory.FUEL_SYSTEM, PidPriority.MEDIUM, 0.0, 200000.0, Unsigned16BitDecoder(0, 10.0), "7E0", 0))
        add(PidDefinition("FRP_A_CMD", "F425", "Fuel Rail Pressure Commanded", "kPa", PidCategory.FUEL_SYSTEM, PidPriority.HIGH, 0.0, 200000.0, Unsigned16BitDecoder(0, 10.0), "7E0", 0))
        add(PidDefinition("FRP_B", "F426", "Fuel Rail Pressure B", "kPa", PidCategory.FUEL_SYSTEM, PidPriority.MEDIUM, 0.0, 200000.0, Unsigned16BitDecoder(0, 10.0), "7E0", 0))
        add(PidDefinition("FRP_B_CMD", "F427", "Fuel Rail Pressure B Commanded", "kPa", PidCategory.FUEL_SYSTEM, PidPriority.LOW, 0.0, 200000.0, Unsigned16BitDecoder(0, 10.0), "7E0", 0))
        add(PidDefinition("FRP_DSD", "F428", "Fuel Rail Pressure Desired", "kPa", PidCategory.FUEL_SYSTEM, PidPriority.MEDIUM, 0.0, 200000.0, Unsigned16BitDecoder(0, 10.0), "7E0", 0))
        add(PidDefinition("FRP_V", "F429", "Fuel Rail Pressure Voltage", "V", PidCategory.FUEL_SYSTEM, PidPriority.LOW, 0.0, 5.0, VoltageDecoder(0), "7E0", 0))
        add(PidDefinition("FRPS_V", "F42A", "Fuel Rail Pressure Sensor Voltage", "V", PidCategory.FUEL_SYSTEM, PidPriority.LOW, 0.0, 5.0, VoltageDecoder(0), "7E0", 0))
        add(PidDefinition("FPL_CMD", "F42B", "Fuel Pump Command", "", PidCategory.FUEL_SYSTEM, PidPriority.LOW, 0.0, 1.0, BooleanDecoder(0), "7E0", 0))
        add(PidDefinition("FP_RELAY", "F42C", "Fuel Pump Relay", "", PidCategory.FUEL_SYSTEM, PidPriority.LOW, 0.0, 1.0, BooleanDecoder(0), "7E0", 0))
        add(PidDefinition("FRT", "F44E", "Fuel Rail Temperature", "°C", PidCategory.FUEL_SYSTEM, PidPriority.MEDIUM, -40.0, 120.0, TemperatureDecoder(0), "7E0", 0))
        add(PidDefinition("FUEL_TIMING", "F44F", "Fuel Injection Timing", "°", PidCategory.FUEL_SYSTEM, PidPriority.MEDIUM, -30.0, 30.0, Unsigned16BitDecoder(0, 0.01, -327.68), "7E0", 0))
        add(PidDefinition("FRPEN_ENGSYNC", "DD10", "FRP Engine Sync", "", PidCategory.FUEL_SYSTEM, PidPriority.LOW, 0.0, 1.0, BooleanDecoder(0), "7E0", 0))
        add(PidDefinition("INJ_Q_1_TOT", "F480", "Injector 1 Total Quantity", "mg", PidCategory.INJECTORS, PidPriority.HIGH, 0.0, 100.0, Unsigned16BitDecoder(0, 0.01, -327.68), "7E0", 0))
        add(PidDefinition("INJ_Q_2_TOT", "F481", "Injector 2 Total Quantity", "mg", PidCategory.INJECTORS, PidPriority.HIGH, 0.0, 100.0, Unsigned16BitDecoder(0, 0.01, -327.68), "7E0", 0))
        add(PidDefinition("INJ_Q_3_TOT", "F482", "Injector 3 Total Quantity", "mg", PidCategory.INJECTORS, PidPriority.HIGH, 0.0, 100.0, Unsigned16BitDecoder(0, 0.01, -327.68), "7E0", 0))
        add(PidDefinition("INJ_Q_4_TOT", "F483", "Injector 4 Total Quantity", "mg", PidCategory.INJECTORS, PidPriority.HIGH, 0.0, 100.0, Unsigned16BitDecoder(0, 0.01, -327.68), "7E0", 0))
        add(PidDefinition("INJ_Q_5_TOT", "F484", "Injector 5 Total Quantity", "mg", PidCategory.INJECTORS, PidPriority.HIGH, 0.0, 100.0, Unsigned16BitDecoder(0, 0.01, -327.68), "7E0", 0))
        add(PidDefinition("CYL_BAL_1", "F48A", "Cylinder 1 Balance", "mg", PidCategory.INJECTORS, PidPriority.HIGH, -5.0, 5.0, Unsigned16BitDecoder(0, 0.001, -32.768), "7E0", 0))
        add(PidDefinition("CYL_BAL_2", "F48B", "Cylinder 2 Balance", "mg", PidCategory.INJECTORS, PidPriority.HIGH, -5.0, 5.0, Unsigned16BitDecoder(0, 0.001, -32.768), "7E0", 0))
        add(PidDefinition("CYL_BAL_3", "F48C", "Cylinder 3 Balance", "mg", PidCategory.INJECTORS, PidPriority.HIGH, -5.0, 5.0, Unsigned16BitDecoder(0, 0.001, -32.768), "7E0", 0))
        add(PidDefinition("CYL_BAL_4", "F48D", "Cylinder 4 Balance", "mg", PidCategory.INJECTORS, PidPriority.HIGH, -5.0, 5.0, Unsigned16BitDecoder(0, 0.001, -32.768), "7E0", 0))
        add(PidDefinition("CYL_BAL_5", "F48E", "Cylinder 5 Balance", "mg", PidCategory.INJECTORS, PidPriority.HIGH, -5.0, 5.0, Unsigned16BitDecoder(0, 0.001, -32.768), "7E0", 0))
        add(PidDefinition("CYL_FUELMOD_1", "F48F", "Cylinder 1 Fuel Modifier", "%", PidCategory.INJECTORS, PidPriority.MEDIUM, -25.0, 25.0, Signed16BitDecoder(0, 0.01), "7E0", 0))
        add(PidDefinition("CYL_FUELMOD_2", "F490", "Cylinder 2 Fuel Modifier", "%", PidCategory.INJECTORS, PidPriority.MEDIUM, -25.0, 25.0, Signed16BitDecoder(0, 0.01), "7E0", 0))
        add(PidDefinition("CYL_FUELMOD_3", "F491", "Cylinder 3 Fuel Modifier", "%", PidCategory.INJECTORS, PidPriority.MEDIUM, -25.0, 25.0, Signed16BitDecoder(0, 0.01), "7E0", 0))
        add(PidDefinition("CYL_FUELMOD_4", "F492", "Cylinder 4 Fuel Modifier", "%", PidCategory.INJECTORS, PidPriority.MEDIUM, -25.0, 25.0, Signed16BitDecoder(0, 0.01), "7E0", 0))
        add(PidDefinition("CYL_FUELMOD_5", "F493", "Cylinder 5 Fuel Modifier", "%", PidCategory.INJECTORS, PidPriority.MEDIUM, -25.0, 25.0, Signed16BitDecoder(0, 0.01), "7E0", 0))
        add(PidDefinition("INJ1_F", "DD20", "Injector 1 Fault", "", PidCategory.INJECTORS, PidPriority.LOW, 0.0, 1.0, BooleanDecoder(0), "7E0", 0))
        add(PidDefinition("INJ2_F", "DD21", "Injector 2 Fault", "", PidCategory.INJECTORS, PidPriority.LOW, 0.0, 1.0, BooleanDecoder(0), "7E0", 0))
        add(PidDefinition("INJ3_F", "DD22", "Injector 3 Fault", "", PidCategory.INJECTORS, PidPriority.LOW, 0.0, 1.0, BooleanDecoder(0), "7E0", 0))
        add(PidDefinition("INJ4_F", "DD23", "Injector 4 Fault", "", PidCategory.INJECTORS, PidPriority.LOW, 0.0, 1.0, BooleanDecoder(0), "7E0", 0))
        add(PidDefinition("INJ5_F", "DD24", "Injector 5 Fault", "", PidCategory.INJECTORS, PidPriority.LOW, 0.0, 1.0, BooleanDecoder(0), "7E0", 0))
        add(PidDefinition("MAF", "F410", "Mass Air Flow", "g/s", PidCategory.AIR_FLOW, PidPriority.HIGH, 0.0, 500.0, Unsigned16BitDecoder(0, 0.01), "7E0", 0))
        add(PidDefinition("MAF_A", "F411", "Mass Air Flow Actual", "g/s", PidCategory.AIR_FLOW, PidPriority.HIGH, 0.0, 500.0, Unsigned16BitDecoder(0, 0.01), "7E0", 0))
        add(PidDefinition("MAF_DSD", "F412", "Mass Air Flow Desired", "g/s", PidCategory.AIR_FLOW, PidPriority.MEDIUM, 0.0, 500.0, Unsigned16BitDecoder(0, 0.01), "7E0", 0))
        add(PidDefinition("MAFCOMPARE", "F413", "MAF Comparison", "", PidCategory.AIR_FLOW, PidPriority.LOW, 0.0, 2.0, Unsigned16BitDecoder(0, 0.01), "7E0", 0))
        add(PidDefinition("IAT_V", "F406", "Intake Air Temp Voltage", "V", PidCategory.AIR_FLOW, PidPriority.LOW, 0.0, 5.0, VoltageDecoder(0), "7E0", 0))
        add(PidDefinition("IAT11", "F407", "Intake Air Temperature 1-1", "°C", PidCategory.AIR_FLOW, PidPriority.MEDIUM, -40.0, 100.0, TemperatureDecoder(0), "7E0", 0))
        add(PidDefinition("IAT12", "F408", "Intake Air Temperature 1-2", "°C", PidCategory.AIR_FLOW, PidPriority.LOW, -40.0, 100.0, TemperatureDecoder(0), "7E0", 0))
        add(PidDefinition("IAT13", "F409", "Intake Air Temperature 1-3", "°C", PidCategory.AIR_FLOW, PidPriority.LOW, -40.0, 100.0, TemperatureDecoder(0), "7E0", 0))
        add(PidDefinition("CACT11", "F460", "Charge Air Cooler Temp 1-1", "°C", PidCategory.AIR_FLOW, PidPriority.MEDIUM, -40.0, 100.0, TemperatureDecoder(0), "7E0", 0))
        add(PidDefinition("CACT12", "F461", "Charge Air Cooler Temp 1-2", "°C", PidCategory.AIR_FLOW, PidPriority.LOW, -40.0, 100.0, TemperatureDecoder(0), "7E0", 0))
        add(PidDefinition("CACT_V", "F463", "Charge Air Cooler Temp Voltage", "V", PidCategory.AIR_FLOW, PidPriority.LOW, 0.0, 5.0, VoltageDecoder(0), "7E0", 0))
        add(PidDefinition("DPF_PRESS_DIF", "F4C0", "DPF Differential Pressure", "kPa", PidCategory.DPF_SYSTEM, PidPriority.HIGH, 0.0, 100.0, Unsigned16BitDecoder(0, 0.01), "7E0", 0))
        add(PidDefinition("DPF_SOOT_PCT_OL", "F4C3", "DPF Soot Load (Open Loop)", "%", PidCategory.DPF_SYSTEM, PidPriority.MEDIUM, 0.0, 100.0, Unsigned16BitDecoder(0, 0.01), "7E0", 0))
        add(PidDefinition("DPF_SOOT_OL", "F4A8", "DPF Soot Open Loop", "", PidCategory.DPF_SYSTEM, PidPriority.LOW, 0.0, 255.0, UnsignedByteDecoder(0), "7E0", 0))
        add(PidDefinition("GPF_SOOT_PCT_CL", "F4A9", "GPF Soot % Closed Loop", "%", PidCategory.DPF_SYSTEM, PidPriority.LOW, 0.0, 100.0, Unsigned16BitDecoder(0, 0.01), "7E0", 0))
        add(PidDefinition("SOOT_OIL", "F4AA", "Soot in Oil", "mg", PidCategory.DPF_SYSTEM, PidPriority.LOW, 0.0, 1000.0, Unsigned16BitDecoder(0, 0.1), "7E0", 0))
        add(PidDefinition("DPF_REGEN", "F4B0", "DPF Regeneration Active", "", PidCategory.DPF_SYSTEM, PidPriority.MEDIUM, 0.0, 1.0, BooleanDecoder(0), "7E0", 0))
        add(PidDefinition("DPF_REGEN_PCT", "F4B1", "DPF Regeneration %", "%", PidCategory.DPF_SYSTEM, PidPriority.MEDIUM, 0.0, 100.0, PercentageDecoder(0), "7E0", 0))
        add(PidDefinition("DPF_REGN_STAT", "F4B2", "DPF Regeneration Status", "", PidCategory.DPF_SYSTEM, PidPriority.LOW, 0.0, 255.0, UnsignedByteDecoder(0), "7E0", 0))
        add(PidDefinition("DPF_REGN_TYP", "F4B3", "DPF Regeneration Type", "", PidCategory.DPF_SYSTEM, PidPriority.LOW, 0.0, 255.0, UnsignedByteDecoder(0), "7E0", 0))
        add(PidDefinition("DPF_REGN_AVGD", "F4B4", "DPF Regen Average Distance", "km", PidCategory.DPF_SYSTEM, PidPriority.LOW, 0.0, 1000.0, Unsigned16BitDecoder(0, 1.0), "7E0", 0))
        add(PidDefinition("DPF_REGN_AVGT", "F4B5", "DPF Regen Average Time", "min", PidCategory.DPF_SYSTEM, PidPriority.LOW, 0.0, 60.0, Unsigned16BitDecoder(0, 0.1), "7E0", 0))
        add(PidDefinition("AVG_TIME_DPF", "F4B6", "Avg Time Between DPF Regens", "min", PidCategory.DPF_SYSTEM, PidPriority.LOW, 0.0, 10000.0, Unsigned16BitDecoder(0, 1.0), "7E0", 0))
        add(PidDefinition("DIST_LAST_DPF", "F4B7", "Distance Since Last DPF Regen", "km", PidCategory.DPF_SYSTEM, PidPriority.LOW, 0.0, 2000.0, Unsigned16BitDecoder(0, 1.0), "7E0", 0))
        add(PidDefinition("DIST_DPF_2ND_3RD", "F4B8", "Dist DPF 2nd-3rd Regen", "km", PidCategory.DPF_SYSTEM, PidPriority.LOW, 0.0, 2000.0, Unsigned16BitDecoder(0, 1.0), "7E0", 0))
        add(PidDefinition("DIST_DPF_3RD_4TH", "F4B9", "Dist DPF 3rd-4th Regen", "km", PidCategory.DPF_SYSTEM, PidPriority.LOW, 0.0, 2000.0, Unsigned16BitDecoder(0, 1.0), "7E0", 0))
        add(PidDefinition("DPF_FAIL_NUM", "F4BA", "DPF Failure Number", "", PidCategory.DPF_SYSTEM, PidPriority.LOW, 0.0, 255.0, UnsignedByteDecoder(0), "7E0", 0))
        add(PidDefinition("DPF_D2C_DIS", "F4BB", "DPF Distance to Clean Disable", "km", PidCategory.DPF_SYSTEM, PidPriority.LOW, 0.0, 100000.0, Unsigned16BitDecoder(0, 1.0), "7E0", 0))
        add(PidDefinition("LTADPF", "F4BC", "Long Term Adaptation DPF", "%", PidCategory.DPF_SYSTEM, PidPriority.LOW, -50.0, 50.0, Signed16BitDecoder(0, 0.01), "7E0", 0))
        add(PidDefinition("EGT11", "F4D0", "Exhaust Gas Temp B1S1", "°C", PidCategory.EXHAUST_TEMPS, PidPriority.HIGH, 0.0, 900.0, Unsigned16BitDecoder(0, 0.1), "7E0", 0))
        add(PidDefinition("EGT12", "F4D1", "Exhaust Gas Temp B1S2", "°C", PidCategory.EXHAUST_TEMPS, PidPriority.HIGH, 0.0, 900.0, Unsigned16BitDecoder(0, 0.1), "7E0", 0))
        add(PidDefinition("EGT13", "F4D2", "Exhaust Gas Temp B1S3", "°C", PidCategory.EXHAUST_TEMPS, PidPriority.HIGH, 0.0, 900.0, Unsigned16BitDecoder(0, 0.1), "7E0", 0))
        add(PidDefinition("EGT14", "F4D3", "Exhaust Gas Temp B1S4", "°C", PidCategory.EXHAUST_TEMPS, PidPriority.HIGH, 0.0, 900.0, Unsigned16BitDecoder(0, 0.1), "7E0", 0))
        add(PidDefinition("SCR_F_FST_B04", "F4D4", "SCR Fast Filter B04", "", PidCategory.DENOX_DESOX, PidPriority.LOW, 0.0, 255.0, UnsignedByteDecoder(0), "7E0", 0))
        add(PidDefinition("SCR_F_SLW_B04", "F4D5", "SCR Slow Filter B04", "", PidCategory.DENOX_DESOX, PidPriority.LOW, 0.0, 255.0, UnsignedByteDecoder(0), "7E0", 0))
        add(PidDefinition("SCR_F_FST_B06", "F4D6", "SCR Fast Filter B06", "", PidCategory.DENOX_DESOX, PidPriority.LOW, 0.0, 255.0, UnsignedByteDecoder(0), "7E0", 0))
        add(PidDefinition("SCR_F_SLW_B06", "F4D7", "SCR Slow Filter B06", "", PidCategory.DENOX_DESOX, PidPriority.LOW, 0.0, 255.0, UnsignedByteDecoder(0), "7E0", 0))
        add(PidDefinition("CCM_EVAL", "F4E0", "Comprehensive Component Monitor", "", PidCategory.MISCELLANEOUS, PidPriority.LOW, 0.0, 255.0, UnsignedByteDecoder(0), "7E0", 0))
        add(PidDefinition("ECPC_CMD", "F4E1", "Electronic Clutch PC Command", "", PidCategory.MISCELLANEOUS, PidPriority.LOW, 0.0, 1.0, BooleanDecoder(0), "7E0", 0))
        add(PidDefinition("ECPC_F", "F4E2", "Electronic Clutch PC Fault", "", PidCategory.MISCELLANEOUS, PidPriority.LOW, 0.0, 1.0, BooleanDecoder(0), "7E0", 0))
        add(PidDefinition("GLOWPLUG_RLY", "F4E3", "Glow Plug Relay", "", PidCategory.MISCELLANEOUS, PidPriority.LOW, 0.0, 1.0, BooleanDecoder(0), "7E0", 0))
        add(PidDefinition("GP_LMP", "F4E4", "Glow Plug Lamp", "", PidCategory.MISCELLANEOUS, PidPriority.LOW, 0.0, 1.0, BooleanDecoder(0), "7E0", 0))
        add(PidDefinition("LOW_OIL", "F4E5", "Low Oil Level Warning", "", PidCategory.MISCELLANEOUS, PidPriority.LOW, 0.0, 1.0, BooleanDecoder(0), "7E0", 0))
        add(PidDefinition("ECTC", "F4E6", "Engine Coolant Temp Calculated", "°C", PidCategory.MISCELLANEOUS, PidPriority.LOW, -40.0, 150.0, TemperatureDecoder(0), "7E0", 0))

        // ===== EXPERIMENTAL TRANSMISSION DIDS (request 7E1 / response 7E9) =====
        add(PidDefinition("TFT", "1E1C", "Transmission Fluid Temperature (6R80 profile)", "°C", PidCategory.TRANSMISSION, PidPriority.MEDIUM, -40.0, 180.0, Unsigned16BitDecoder(0, 1.0 / 16.0), "7E1", 0))
        add(PidDefinition("GEAR_CMD", "1E12", "Commanded Gear", "", PidCategory.TRANSMISSION, PidPriority.MEDIUM, 0.0, 7.0, UnsignedByteDecoder(0), "7E1", 0))
        add(PidDefinition("TCC_SLIP", "1E0A", "Torque Converter Slip", "rpm", PidCategory.TRANSMISSION, PidPriority.MEDIUM, -500.0, 3000.0, Signed16BitDecoder(0, 0.25), "7E1", 0))
        add(PidDefinition("TURBINE_RPM", "1E01", "Turbine Shaft Speed", "rpm", PidCategory.TRANSMISSION, PidPriority.MEDIUM, 0.0, 6000.0, Unsigned16BitDecoder(0, 0.25), "7E1", 0))
    }
}
