package com.followupnadlan.setup

data class OemGuidanceBlock(
    val key: String,
    val title: String,
    val body: String
)

object OemGuidance {
    fun forManufacturer(rawManufacturerOrBrand: String?): OemGuidanceBlock {
        val normalized = rawManufacturerOrBrand.orEmpty().trim().lowercase()
        return when {
            normalized.contains("samsung") -> samsung
            normalized.contains("xiaomi") -> xiaomi
            normalized.contains("redmi") -> redmi
            normalized.contains("realme") -> realme
            normalized.contains("oneplus") || normalized.contains("one plus") -> onePlus
            else -> generic
        }
    }

    val samsung = OemGuidanceBlock(
        key = "samsung",
        title = "Samsung",
        body = "במכשירי Samsung ייתכן שחיסכון בסוללה יעצור את הזיהוי אחרי זמן.\n" +
            "פתח הגדרות סוללה והחרג את האפליקציה מחיסכון."
    )

    val xiaomi = OemGuidanceBlock(
        key = "xiaomi",
        title = "Xiaomi",
        body = "במכשירי Xiaomi/Redmi צריך לאשר Autostart ולבטל חיסכון סוללה לאפליקציה."
    )

    val redmi = OemGuidanceBlock(
        key = "redmi",
        title = "Redmi",
        body = xiaomi.body
    )

    val realme = OemGuidanceBlock(
        key = "realme",
        title = "Realme",
        body = "במכשירי Realme מומלץ לאפשר לאפליקציה לפעול ברקע ולבטל חיסכון סוללה לאפליקציה."
    )

    val onePlus = OemGuidanceBlock(
        key = "oneplus",
        title = "OnePlus",
        body = "במכשירי OnePlus מומלץ לאפשר לאפליקציה לפעול ברקע ולבטל חיסכון סוללה לאפליקציה."
    )

    val generic = OemGuidanceBlock(
        key = "generic",
        title = "הגדרות סוללה",
        body = "כדי שהאפליקציה תזהה סיום שיחה, מומלץ לאפשר לה לפעול ברקע."
    )
}
