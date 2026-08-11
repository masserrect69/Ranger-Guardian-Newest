package au.com.rangerai.data

enum class PidCategory(val displayName: String) {
    ENGINE_CORE("Engine Core"),
    BOOST_TURBO("Boost/Turbo"),
    FUEL_SYSTEM("Fuel System"),
    INJECTORS("Injectors"),
    AIR_FLOW("Air Flow"),
    EGR_SYSTEM("EGR System"),
    DPF_SYSTEM("DPF System"),
    EXHAUST_TEMPS("Exhaust Temps"),
    DENOX_DESOX("DeNOx/AdBlue"),
    TRANSMISSION("Transmission"),
    ELECTRICAL("Electrical"),
    MISCELLANEOUS("Miscellaneous")
}
