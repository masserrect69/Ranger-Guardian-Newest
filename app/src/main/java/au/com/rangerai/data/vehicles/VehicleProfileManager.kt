package au.com.rangerai.data.vehicles

import au.com.rangerai.data.obd.StandardObd2Pids
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton that manages the currently active vehicle profile.
 * All screens reference this to determine which PIDs are available.
 *
 * Only PIDs that exist in the active profile's manufacturer pack are shown.
 * This prevents phantom sensors (e.g., oil temp on a Ford Ranger that doesn't have one)
 * from appearing in the UI.
 */
object VehicleProfileManager {

    private val _activeProfile = MutableStateFlow<VehicleProfile?>(null)
    val activeProfile: StateFlow<VehicleProfile?> = _activeProfile.asStateFlow()

    private val _activeManufacturer = MutableStateFlow(Manufacturer.FORD)
    val activeManufacturer: StateFlow<Manufacturer> = _activeManufacturer.asStateFlow()

    /**
     * Set the active vehicle profile.
     * Called when user selects a vehicle from the VehicleSelectScreen.
     */
    fun setActiveVehicle(manufacturer: Manufacturer) {
        _activeManufacturer.value = manufacturer
        _activeProfile.value = VehicleRegistry.getProfileForManufacturer(manufacturer)
    }

    /**
     * Get the currently active profile, defaulting to Ford Ranger if none set.
     */
    fun getActiveProfile(): VehicleProfile {
        return _activeProfile.value ?: VehicleRegistry.getProfileForManufacturer(Manufacturer.FORD)
    }

    /**
     * Get ONLY the manufacturer-specific Mode 22 PIDs available for this vehicle.
     * These are the deep diagnostic parameters unique to this car.
     * If a PID isn't in this list, the car doesn't have that sensor/capability.
     */
    fun getAvailableMode22Pids(): List<ManufacturerPid> {
        return getActiveProfile().mode22Pids
    }

    /**
     * Get the universal Mode 01 OBD-II PIDs.
     * These are standard across all vehicles (RPM, speed, coolant, etc.)
     * but we still only show ones that are relevant to the vehicle type.
     */
    fun getAvailableMode01Pids(): List<ManufacturerPid> {
        val profile = getActiveProfile()
        return StandardObd2Pids.allPids.filter { pid ->
            // Filter out PIDs that don't apply to this vehicle type
            when {
                // This Ranger advertises catalyst-temperature PIDs 013C and 013E.
                // Only hide oxygen/ethanol entries that do not apply to this diesel profile.
                isDiesel(profile) && pid.did in listOf("013D", "013F", "0124", "0125", "0152") -> false
                // Non-turbo vehicles don't have boost PIDs
                !isTurbo(profile) && pid.did in listOf("0170", "0171", "0172", "0173", "0174") -> false
                // Non-hybrid vehicles don't have hybrid PIDs
                !isHybrid(profile) && pid.category == PidCategory.HYBRID -> false
                else -> true
            }
        }
    }

    /**
     * Get ALL available PIDs for this vehicle (Mode 01 + Mode 22 combined).
     * This is what the Parameters screen shows.
     */
    fun getAllAvailablePids(): List<DisplayPid> {
        val mode01 = getAvailableMode01Pids().map { pid ->
            DisplayPid(
                id = "01_${pid.did}",
                did = pid.did,
                name = pid.name,
                description = pid.description,
                unit = pid.unit,
                category = pid.category.displayName,
                mode = "Mode 01 (Universal)",
                minValue = pid.minValue,
                maxValue = pid.maxValue,
                pollGroup = pid.pollGroup.label
            )
        }

        val mode22 = getAvailableMode22Pids().map { pid ->
            DisplayPid(
                id = "22_${pid.did}",
                did = pid.did,
                name = pid.name,
                description = pid.description,
                unit = pid.unit,
                category = pid.category.displayName,
                mode = "Mode 22 (${getActiveProfile().manufacturer.displayName})",
                minValue = pid.minValue,
                maxValue = pid.maxValue,
                pollGroup = pid.pollGroup.label
            )
        }

        return mode01 + mode22
    }

    /**
     * Check if a specific PID name/category is available for this vehicle.
     * Used by the dashboard to decide which gauges to show.
     */
    fun isPidAvailable(pidName: String): Boolean {
        val allPids = getAllAvailablePids()
        return allPids.any { it.name.equals(pidName, ignoreCase = true) }
    }

    /**
     * Get available PID categories for this vehicle.
     * Only categories that have at least one PID are returned.
     */
    fun getAvailableCategories(): List<String> {
        return getAllAvailablePids()
            .map { it.category }
            .distinct()
            .sorted()
    }

    // === Helper functions to determine vehicle type ===

    private fun isDiesel(profile: VehicleProfile): Boolean {
        val engine = profile.engine.lowercase()
        return engine.contains("diesel") || engine.contains("tdi") ||
                engine.contains("crdi") || engine.contains("dci") ||
                engine.contains("d4d") || engine.contains("p4at") ||
                engine.contains("p5at") || engine.contains("duramax") ||
                engine.contains("skyactiv-d") || engine.contains("om6") ||
                engine.contains("4n15") || engine.contains("4jj") ||
                engine.contains("ys23") || engine.contains("b47") ||
                engine.contains("ea288") || engine.contains("ea897")
    }

    private fun isTurbo(profile: VehicleProfile): Boolean {
        val engine = profile.engine.lowercase()
        return engine.contains("turbo") || engine.contains("t ") ||
                engine.contains("tdi") || engine.contains("tsi") ||
                engine.contains("tfsi") || engine.contains("ecoboost") ||
                engine.contains("skyactiv-d") || engine.contains("crdi") ||
                engine.contains("dci") || engine.contains("p4at") ||
                engine.contains("p5at") || engine.contains("wrx") ||
                isDiesel(profile) // Most modern diesels are turbocharged
    }

    private fun isHybrid(profile: VehicleProfile): Boolean {
        val engine = profile.engine.lowercase()
        return engine.contains("hybrid") || engine.contains("phev") ||
                engine.contains("ev") || engine.contains("fxe")
    }

    /**
     * Initialize with default profile (Ford Ranger for backward compatibility).
     */
    init {
        setActiveVehicle(Manufacturer.FORD)
    }
}

/**
 * Unified PID display model used by the Parameters screen.
 * Combines Mode 01 and Mode 22 PIDs into a single displayable format.
 */
data class DisplayPid(
    val id: String,
    val did: String,
    val name: String,
    val description: String,
    val unit: String,
    val category: String,
    val mode: String,
    val minValue: Double,
    val maxValue: Double,
    val pollGroup: String
)
