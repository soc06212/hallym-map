package xyz.ridsoft.hal.model

import android.content.Context
import com.google.gson.Gson
import xyz.ridsoft.hal.R
import xyz.ridsoft.hal.data.DataManager

abstract class MapPoint(
    open var id: Int,
    open var name: String,
    open var latitude: Double,
    open var longitude: Double
) {
    var searchTag: String? = null
    var kr: String? = null
    var en: String? = null
    var buildingNo: Int = Place.DEFAULT_BUILDING_NUMBER

    fun getLocalizedString(context: Context): String {
        val locales = context.resources.configuration.locales
        var locale = "en_US"
        for (j in 0 until locales.size()) {
            if (arrayOf("ko_KR", "en_US").contains(locales[j].toString())) {
                locale = locales[j].toString()
                break
            }
        }

        return when (locale) {
            "ko_KR" -> kr ?: name
            "en_US" -> en ?: name
            else -> name
        }
    }
}

class Place(
    id: Int,
    name: String,
    latitude: Double,
    longitude: Double
) : MapPoint(id, name, latitude, longitude) {

    companion object {
        const val DEFAULT_BUILDING_NUMBER = 0
    }

    var legacyName: String? = null
    var facilityId: Array<Int>? = null

}

class Facility(
    id: Int,
    name: String,
    latitude: Double,
    longitude: Double,
    var type: FacilityType,
    var floor: Int
) : MapPoint(id, name, latitude, longitude) {

    companion object {
        enum class FacilityType {
            CAFE, STUDY_ROOM, STORE_CONVENIENCE, STORE_BOOK, STORE_STATIONERY,
            ATM, CAFETERIA, PRINTER, POST, VENDING_MACHINE, ETC;

            companion object {

                fun getRawArray(): Array<FacilityType> {
                    return arrayOf(
                        CAFE, STUDY_ROOM, STORE_CONVENIENCE, STORE_BOOK, STORE_STATIONERY,
                        ATM, CAFETERIA, PRINTER, POST, VENDING_MACHINE, ETC
                    )
                }

                fun getArray(): Array<FacilityType> {
                    val types = ArrayList<FacilityType>()
                    val raw = getRawArray()
                    for (t in raw) {
                        for (f in DataManager.facilities!!) {
                            if (f.type == t) {
                                types.add(t)
                                break
                            }
                        }
                    }
                    return types.toTypedArray()
                }

                fun getString(context: Context, facilityType: FacilityType): String {
                    return when (facilityType) {
                        CAFE -> context.resources.getString(R.string.facility_cafe)
                        STUDY_ROOM -> context.resources.getString(R.string.facility_study_room)
                        STORE_CONVENIENCE -> context.resources.getString(R.string.facility_convenience_store)
                        STORE_BOOK -> context.resources.getString(R.string.facility_book_store)
                        STORE_STATIONERY -> context.resources.getString(R.string.facility_stationery_store)
                        ATM -> context.resources.getString(R.string.facility_atm)
                        CAFETERIA -> context.resources.getString(R.string.facility_cafeteria)
                        PRINTER -> context.resources.getString(R.string.facility_printer)
                        POST -> context.resources.getString(R.string.facility_post)
                        VENDING_MACHINE -> context.resources.getString(R.string.facility_vending_machine)
                        ETC -> context.resources.getString(R.string.facility_etc)
                    }
                }

                fun getIconId(facilityType: FacilityType): Int {
                    return when (facilityType) {
                        CAFE -> R.drawable.baseline_local_cafe_24
                        STUDY_ROOM -> R.drawable.baseline_groups_24
                        STORE_CONVENIENCE -> R.drawable.baseline_store_24
                        STORE_BOOK -> R.drawable.baseline_auto_stories_24
                        STORE_STATIONERY -> R.drawable.baseline_architecture_24
                        ATM -> R.drawable.baseline_atm_24
                        CAFETERIA -> R.drawable.baseline_dining_24
                        PRINTER -> R.drawable.baseline_print_24
                        POST -> R.drawable.baseline_local_post_office_24
                        VENDING_MACHINE -> R.drawable.baseline_local_drink_24
                        ETC -> R.drawable.baseline_more_24
                    }
                }
            }
        }
    }

}