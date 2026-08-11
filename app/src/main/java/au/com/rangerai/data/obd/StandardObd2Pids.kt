package au.com.rangerai.data.obd

import au.com.rangerai.data.vehicles.*

/**
 * Universal OBD-II Mode 01 PIDs.
 * These work on ANY vehicle manufactured after 1996 (OBD-II mandate).
 * Formulas use A, B, C, D for response bytes.
 */
object StandardObd2Pids {

    val allPids: List<ManufacturerPid> = listOf(
        // === ENGINE CORE ===
        ManufacturerPid("0104", "Calculated Engine Load", "Percentage of available engine torque being used", "%", PidCategory.ENGINE, 0.0, 100.0, "A*100/255", 1, PollGroup.FAST),
        ManufacturerPid("0105", "Engine Coolant Temp", "Engine coolant temperature", "°C", PidCategory.COOLING, -40.0, 215.0, "A-40", 1, PollGroup.MEDIUM),
        ManufacturerPid("010B", "Intake MAP", "Intake manifold absolute pressure", "kPa", PidCategory.AIRFLOW, 0.0, 255.0, "A", 1, PollGroup.FAST),
        ManufacturerPid("010C", "Engine RPM", "Engine revolutions per minute", "RPM", PidCategory.ENGINE, 0.0, 16383.75, "(A*256+B)/4", 2, PollGroup.FAST),
        ManufacturerPid("010D", "Vehicle Speed", "Current vehicle speed", "km/h", PidCategory.ENGINE, 0.0, 255.0, "A", 1, PollGroup.FAST),
        ManufacturerPid("010E", "Timing Advance", "Ignition timing advance before TDC", "°", PidCategory.ENGINE, -64.0, 63.5, "A/2-64", 1, PollGroup.MEDIUM),
        ManufacturerPid("010F", "Intake Air Temp", "Intake air temperature", "°C", PidCategory.AIRFLOW, -40.0, 215.0, "A-40", 1, PollGroup.MEDIUM),
        ManufacturerPid("0110", "MAF Air Flow", "Mass air flow sensor reading", "g/s", PidCategory.AIRFLOW, 0.0, 655.35, "(A*256+B)/100", 2, PollGroup.FAST),
        ManufacturerPid("0111", "Throttle Position", "Absolute throttle position", "%", PidCategory.ENGINE, 0.0, 100.0, "A*100/255", 1, PollGroup.FAST),

        // === FUEL SYSTEM ===
        ManufacturerPid("0103", "Fuel System Status", "Open/closed loop fuel system status", "", PidCategory.FUEL, 0.0, 255.0, "A", 2, PollGroup.SLOW),
        ManufacturerPid("0106", "Short Term Fuel Trim B1", "Short term fuel trim - Bank 1", "%", PidCategory.FUEL, -100.0, 99.2, "(A-128)*100/128", 1, PollGroup.MEDIUM),
        ManufacturerPid("0107", "Long Term Fuel Trim B1", "Long term fuel trim - Bank 1", "%", PidCategory.FUEL, -100.0, 99.2, "(A-128)*100/128", 1, PollGroup.SLOW),
        ManufacturerPid("0108", "Short Term Fuel Trim B2", "Short term fuel trim - Bank 2", "%", PidCategory.FUEL, -100.0, 99.2, "(A-128)*100/128", 1, PollGroup.MEDIUM),
        ManufacturerPid("0109", "Long Term Fuel Trim B2", "Long term fuel trim - Bank 2", "%", PidCategory.FUEL, -100.0, 99.2, "(A-128)*100/128", 1, PollGroup.SLOW),
        ManufacturerPid("010A", "Fuel Pressure", "Fuel pressure (gauge)", "kPa", PidCategory.FUEL, 0.0, 765.0, "A*3", 1, PollGroup.MEDIUM),
        ManufacturerPid("012F", "Fuel Tank Level", "Fuel tank level input", "%", PidCategory.FUEL, 0.0, 100.0, "A*100/255", 1, PollGroup.SLOW),
        ManufacturerPid("0123", "Fuel Rail Gauge Pressure", "Fuel rail gauge pressure (diesel/GDI)", "kPa", PidCategory.FUEL, 0.0, 655350.0, "(A*256+B)*10", 2, PollGroup.FAST),
        ManufacturerPid("0122", "Fuel Rail Pressure Relative", "Fuel rail pressure relative to manifold vacuum", "kPa", PidCategory.FUEL, 0.0, 5177.265, "(A*256+B)*0.079", 2, PollGroup.FAST),

        // === OXYGEN SENSORS ===
        ManufacturerPid("0114", "O2 Sensor B1S1 Voltage", "Oxygen sensor voltage Bank 1 Sensor 1", "V", PidCategory.EMISSIONS, 0.0, 1.275, "A/200", 2, PollGroup.MEDIUM),
        ManufacturerPid("0115", "O2 Sensor B1S2 Voltage", "Oxygen sensor voltage Bank 1 Sensor 2", "V", PidCategory.EMISSIONS, 0.0, 1.275, "A/200", 2, PollGroup.MEDIUM),
        ManufacturerPid("0116", "O2 Sensor B1S3 Voltage", "Oxygen sensor voltage Bank 1 Sensor 3", "V", PidCategory.EMISSIONS, 0.0, 1.275, "A/200", 2, PollGroup.SLOW),
        ManufacturerPid("0117", "O2 Sensor B1S4 Voltage", "Oxygen sensor voltage Bank 1 Sensor 4", "V", PidCategory.EMISSIONS, 0.0, 1.275, "A/200", 2, PollGroup.SLOW),
        ManufacturerPid("0118", "O2 Sensor B2S1 Voltage", "Oxygen sensor voltage Bank 2 Sensor 1", "V", PidCategory.EMISSIONS, 0.0, 1.275, "A/200", 2, PollGroup.MEDIUM),
        ManufacturerPid("0119", "O2 Sensor B2S2 Voltage", "Oxygen sensor voltage Bank 2 Sensor 2", "V", PidCategory.EMISSIONS, 0.0, 1.275, "A/200", 2, PollGroup.MEDIUM),
        ManufacturerPid("0124", "O2 Sensor B1S1 Wide", "Wideband O2 equivalence ratio B1S1", "λ", PidCategory.EMISSIONS, 0.0, 2.0, "(A*256+B)*2/65536", 4, PollGroup.MEDIUM),
        ManufacturerPid("0134", "O2 Sensor B1S1 Current", "Wideband O2 current B1S1", "mA", PidCategory.EMISSIONS, -128.0, 128.0, "(C*256+D)/256-128", 4, PollGroup.MEDIUM),

        // === EMISSIONS / CATALYST ===
        ManufacturerPid("013C", "Catalyst Temp B1S1", "Catalyst temperature Bank 1 Sensor 1", "°C", PidCategory.EMISSIONS, -40.0, 6513.5, "(A*256+B)/10-40", 2, PollGroup.SLOW),
        ManufacturerPid("013D", "Catalyst Temp B2S1", "Catalyst temperature Bank 2 Sensor 1", "°C", PidCategory.EMISSIONS, -40.0, 6513.5, "(A*256+B)/10-40", 2, PollGroup.SLOW),
        ManufacturerPid("013E", "Catalyst Temp B1S2", "Catalyst temperature Bank 1 Sensor 2", "°C", PidCategory.EMISSIONS, -40.0, 6513.5, "(A*256+B)/10-40", 2, PollGroup.SLOW),
        ManufacturerPid("013F", "Catalyst Temp B2S2", "Catalyst temperature Bank 2 Sensor 2", "°C", PidCategory.EMISSIONS, -40.0, 6513.5, "(A*256+B)/10-40", 2, PollGroup.SLOW),

        // === ELECTRICAL ===
        ManufacturerPid("0142", "Control Module Voltage", "Battery/control module voltage", "V", PidCategory.ELECTRICAL, 0.0, 65.535, "(A*256+B)/1000", 2, PollGroup.MEDIUM),
        ManufacturerPid("0146", "Ambient Air Temp", "Ambient air temperature", "°C", PidCategory.CLIMATE, -40.0, 215.0, "A-40", 1, PollGroup.SLOW),

        // === TURBO / BOOST ===
        ManufacturerPid("0133", "Barometric Pressure", "Absolute barometric pressure", "kPa", PidCategory.AIRFLOW, 0.0, 255.0, "A", 1, PollGroup.SLOW),
        ManufacturerPid("0170", "Boost Pressure Control", "Boost pressure control status", "kPa", PidCategory.TURBO, 0.0, 6553.5, "(A*256+B)*0.1", 2, PollGroup.FAST),
        ManufacturerPid("0171", "VGT Control", "Variable geometry turbo control", "%", PidCategory.TURBO, 0.0, 100.0, "A*100/255", 1, PollGroup.FAST),

        // === EGR ===
        ManufacturerPid("012C", "Commanded EGR", "Commanded EGR valve position", "%", PidCategory.EGR, 0.0, 100.0, "A*100/255", 1, PollGroup.MEDIUM),
        ManufacturerPid("012D", "EGR Error", "EGR position error (commanded vs actual)", "%", PidCategory.EGR, -100.0, 99.2, "(A-128)*100/128", 1, PollGroup.MEDIUM),

        // === DPF / EXHAUST ===
        ManufacturerPid("017C", "DPF Temperature", "Diesel particulate filter temperature", "°C", PidCategory.DPF, -40.0, 6513.5, "(A*256+B)/10-40", 2, PollGroup.MEDIUM),
        ManufacturerPid("017E", "DPF Differential Pressure", "DPF differential pressure (backpressure)", "kPa", PidCategory.DPF, 0.0, 655.35, "(A*256+B)/100", 2, PollGroup.MEDIUM),
        ManufacturerPid("017F", "DPF Inlet Pressure", "DPF inlet pressure", "kPa", PidCategory.DPF, 0.0, 655.35, "(A*256+B)/100", 2, PollGroup.MEDIUM),

        // === TRANSMISSION ===
        ManufacturerPid("01A4", "Transmission Actual Gear", "Current gear ratio", "", PidCategory.TRANSMISSION, 0.0, 10.0, "A", 1, PollGroup.MEDIUM),
        ManufacturerPid("015E", "Engine Fuel Rate", "Engine fuel consumption rate", "L/h", PidCategory.FUEL, 0.0, 3276.75, "(A*256+B)*0.05", 2, PollGroup.FAST),
        ManufacturerPid("0161", "Engine Torque Demand", "Driver's demand engine torque", "%", PidCategory.ENGINE, -125.0, 130.0, "A-125", 1, PollGroup.FAST),
        ManufacturerPid("0162", "Actual Engine Torque", "Actual engine percent torque", "%", PidCategory.ENGINE, -125.0, 130.0, "A-125", 1, PollGroup.FAST),
        ManufacturerPid("0163", "Engine Reference Torque", "Engine reference torque", "Nm", PidCategory.ENGINE, 0.0, 65535.0, "A*256+B", 2, PollGroup.SLOW),

        // === TIMING / IGNITION ===
        ManufacturerPid("011C", "OBD Standard", "OBD standards this vehicle conforms to", "", PidCategory.MISC, 0.0, 255.0, "A", 1, PollGroup.ON_DEMAND),
        ManufacturerPid("011F", "Run Time Since Start", "Time since engine start", "sec", PidCategory.ENGINE, 0.0, 65535.0, "A*256+B", 2, PollGroup.SLOW),
        ManufacturerPid("0121", "Distance with MIL", "Distance traveled with MIL on", "km", PidCategory.EMISSIONS, 0.0, 65535.0, "A*256+B", 2, PollGroup.ON_DEMAND),
        ManufacturerPid("0131", "Distance Since Codes Cleared", "Distance since DTCs cleared", "km", PidCategory.EMISSIONS, 0.0, 65535.0, "A*256+B", 2, PollGroup.ON_DEMAND),
        ManufacturerPid("014D", "Time with MIL On", "Time run with MIL on", "min", PidCategory.EMISSIONS, 0.0, 65535.0, "A*256+B", 2, PollGroup.ON_DEMAND),
        ManufacturerPid("014E", "Time Since Codes Cleared", "Time since DTCs cleared", "min", PidCategory.EMISSIONS, 0.0, 65535.0, "A*256+B", 2, PollGroup.ON_DEMAND),

        // === EVAP / PURGE ===
        ManufacturerPid("012E", "Commanded Evap Purge", "Commanded evaporative purge", "%", PidCategory.EMISSIONS, 0.0, 100.0, "A*100/255", 1, PollGroup.SLOW),
        ManufacturerPid("0130", "Warm-ups Since Clear", "Number of warm-ups since codes cleared", "", PidCategory.EMISSIONS, 0.0, 255.0, "A", 1, PollGroup.ON_DEMAND),
        ManufacturerPid("0132", "Evap System Vapor Pressure", "Evaporative system vapor pressure", "Pa", PidCategory.EMISSIONS, -8192.0, 8191.75, "(A*256+B)/4-8192", 2, PollGroup.SLOW),

        // === SECONDARY AIR ===
        ManufacturerPid("0112", "Secondary Air Status", "Commanded secondary air status", "", PidCategory.EMISSIONS, 0.0, 255.0, "A", 1, PollGroup.SLOW),

        // === MISFIRE ===
        ManufacturerPid("0101", "Monitor Status", "Monitor status since DTCs cleared (MIL, DTC count)", "", PidCategory.ENGINE, 0.0, 255.0, "A", 4, PollGroup.ON_DEMAND),
        ManufacturerPid("0141", "Monitor Status Drive Cycle", "Monitor status this drive cycle", "", PidCategory.ENGINE, 0.0, 255.0, "A", 4, PollGroup.ON_DEMAND),

        // === HYBRID / EV (Mode 01 extended) ===
        ManufacturerPid("015B", "Hybrid Battery Pack Life", "Hybrid battery pack remaining life", "%", PidCategory.HYBRID, 0.0, 100.0, "A*100/255", 1, PollGroup.SLOW),
        ManufacturerPid("015C", "Engine Oil Temp", "Engine oil temperature", "°C", PidCategory.ENGINE, -40.0, 210.0, "A-40", 1, PollGroup.MEDIUM),
        ManufacturerPid("015D", "Fuel Injection Timing", "Fuel injection timing", "°", PidCategory.FUEL, -210.0, 301.992, "((A*256+B)-26880)/128", 2, PollGroup.MEDIUM),

        // === THROTTLE / PEDAL ===
        ManufacturerPid("0145", "Relative Throttle Position", "Relative throttle position", "%", PidCategory.ENGINE, 0.0, 100.0, "A*100/255", 1, PollGroup.FAST),
        ManufacturerPid("0147", "Absolute Throttle B", "Absolute throttle position B", "%", PidCategory.ENGINE, 0.0, 100.0, "A*100/255", 1, PollGroup.FAST),
        ManufacturerPid("0148", "Absolute Throttle C", "Absolute throttle position C", "%", PidCategory.ENGINE, 0.0, 100.0, "A*100/255", 1, PollGroup.FAST),
        ManufacturerPid("0149", "Accelerator Pedal D", "Accelerator pedal position D", "%", PidCategory.ENGINE, 0.0, 100.0, "A*100/255", 1, PollGroup.FAST),
        ManufacturerPid("014A", "Accelerator Pedal E", "Accelerator pedal position E", "%", PidCategory.ENGINE, 0.0, 100.0, "A*100/255", 1, PollGroup.FAST),
        ManufacturerPid("014B", "Accelerator Pedal F", "Accelerator pedal position F", "%", PidCategory.ENGINE, 0.0, 100.0, "A*100/255", 1, PollGroup.FAST),
        ManufacturerPid("014C", "Commanded Throttle Actuator", "Commanded throttle actuator", "%", PidCategory.ENGINE, 0.0, 100.0, "A*100/255", 1, PollGroup.FAST),

        // === FREEZE FRAME / VIN ===
        ManufacturerPid("0102", "Freeze DTC", "DTC that caused freeze frame", "", PidCategory.EMISSIONS, 0.0, 65535.0, "A*256+B", 2, PollGroup.ON_DEMAND),

        // === NOx / SCR (Mode 01 extended) ===
        ManufacturerPid("0183", "NOx Sensor Corrected B1", "NOx sensor corrected Bank 1", "ppm", PidCategory.DENOX, 0.0, 65535.0, "A*256+B", 2, PollGroup.MEDIUM),
        ManufacturerPid("0184", "NOx Sensor Corrected B2", "NOx sensor corrected Bank 2", "ppm", PidCategory.DENOX, 0.0, 65535.0, "A*256+B", 2, PollGroup.MEDIUM),

        // === EXHAUST GAS TEMP ===
        ManufacturerPid("0178", "EGT Bank 1 Sensor 1", "Exhaust gas temperature B1S1", "°C", PidCategory.EXHAUST, -40.0, 6513.5, "(A*256+B)/10-40", 2, PollGroup.MEDIUM),
        ManufacturerPid("0179", "EGT Bank 1 Sensor 2", "Exhaust gas temperature B1S2", "°C", PidCategory.EXHAUST, -40.0, 6513.5, "(A*256+B)/10-40", 2, PollGroup.MEDIUM),
        ManufacturerPid("017A", "EGT Bank 2 Sensor 1", "Exhaust gas temperature B2S1", "°C", PidCategory.EXHAUST, -40.0, 6513.5, "(A*256+B)/10-40", 2, PollGroup.MEDIUM),
        ManufacturerPid("017B", "EGT Bank 2 Sensor 2", "Exhaust gas temperature B2S2", "°C", PidCategory.EXHAUST, -40.0, 6513.5, "(A*256+B)/10-40", 2, PollGroup.MEDIUM),

        // === ABSOLUTE VALUES ===
        ManufacturerPid("0143", "Absolute Load Value", "Absolute load value", "%", PidCategory.ENGINE, 0.0, 25700.0, "(A*256+B)*100/255", 2, PollGroup.FAST),
        ManufacturerPid("0144", "Commanded Equivalence Ratio", "Commanded air-fuel equivalence ratio", "λ", PidCategory.FUEL, 0.0, 2.0, "(A*256+B)/32768", 2, PollGroup.MEDIUM)
    )

    /**
     * Mode 09 PIDs for vehicle identification
     */
    val vinRequestCommand = "0902" // Request VIN (Vehicle Identification Number)
    val ecuNameCommand = "090A"    // Request ECU name
    val calIdCommand = "0904"      // Request Calibration ID

    fun getMode01Command(pid: ManufacturerPid): String {
        // Mode 01 PIDs are stored as "01XX" - extract the PID part
        return pid.did.substring(2) // Remove "01" prefix, send as "01XX"
    }
}
