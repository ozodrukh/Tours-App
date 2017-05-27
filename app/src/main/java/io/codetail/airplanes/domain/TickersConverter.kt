package io.codetail.airplanes.domain

import android.arch.persistence.room.TypeConverter
import org.json.JSONObject

/**
 * created at 5/19/17

 * @author Ozodrukh
 * *
 * @version 1.0
 */

class TickersConverter {

    @TypeConverter
    fun FromTicketInfo(info: TicketInfo): String {
        return JSONObject().apply {
            put("city", info.city)
            put("country", info.country)
            put("date", info.date)
        }.toString()
    }

    @TypeConverter
    fun ToTicketInfo(value: String): TicketInfo? {
        JSONObject(value).run {
            return TicketInfo(
                    date = getString("date"),
                    city = getString("city"),
                    country = getString("country")
            )
        }
    }

    @TypeConverter
    fun FromUserInterestType(value: UserInterestType): String {
        return value.name
    }

    @TypeConverter
    fun ToUserInterestType(name: String): UserInterestType {
        return UserInterestType.valueOf(name)
    }
}
