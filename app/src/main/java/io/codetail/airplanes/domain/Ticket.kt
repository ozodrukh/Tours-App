package io.codetail.airplanes.domain

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.Exclude
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ThreadLocalRandom

/**
 * created at 5/19/17
 *
 * @author Ozodrukh
 * @version 1.0
 */

val TICKET_DATE_FORMAT = SimpleDateFormat("dd, MMMM yyyy", Locale.getDefault())

@Entity(tableName = "tours")
data class Ticket(
        @PrimaryKey(autoGenerate = true) val id: Int,
        val departure: TicketInfo,
        val back: TicketInfo,
        var interestType: UserInterestType = UserInterestType.ORDINAL) {

    constructor(dataSnapshot: DataSnapshot) : this(
            dataSnapshot.child("id").getInt(),
            TicketInfo(dataSnapshot.child("departure")),
            TicketInfo(dataSnapshot.child("back")),
            UserInterestType.valueOf(dataSnapshot.child("interestType").getString())
    )
}

data class TicketInfo(val date: String, val city: String, val country: String) {

    constructor(dataSnapshot: DataSnapshot) : this(
            dataSnapshot.child("date").getString(),
            dataSnapshot.child("city").getString(),
            dataSnapshot.child("country").getString()
    )

    @Exclude
    fun isEmpty(): Boolean {
        return date == "-" && city == "-" && country == "-"
    }
}

enum class UserInterestType {
    ORDINAL, INTERESTED, GOING
}

private fun DataSnapshot.getInt(): Int {
    return (getValue() as Long).toInt()
}

private fun DataSnapshot.getString(): String {
    return getValue() as String
}

val FakeTickets = listOf(*Random.generateFakeTickets());
val EMPTY_TICKET_INFO = TicketInfo("-", "-", "-")
val EMPTY_TICKET = Ticket(-1, EMPTY_TICKET_INFO, EMPTY_TICKET_INFO, UserInterestType.ORDINAL)

object Random {
    private val dateGroups = Array<String>(5) {
        randomDate(true)
    }

    private val CitiesMap = mapOf(
            "USA" to arrayOf("New York", "San Francisco", "Los Angeles"),
            "Uzbekistan" to arrayOf("Tashkent", "Samarkand", "Bukhara"),
            "Canada" to arrayOf("Toronto", "Montreal", "Kingston"),
            "Switzerland" to arrayOf("Zermatt", "Montreux", "Aarau")
    )

    private fun rand(min: Int, max: Int): Int {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private fun <K, V> Map<K, V>.randomKey(): K {
        return this.keys.elementAt(rand(0, this.keys.size - 1))
    }

    private fun <T> Array<T>.randomValue(): T {
        return this[rand(0, size - 1)];
    }

    private fun padding(value: String, token: Char, length: Int): String {
        val builder = StringBuilder(value)
        for (i in value.length until length) {
            builder.insert(0, token)
        }
        return builder.toString()
    }

    private fun randomDate(generate: Boolean = false): String {
        if (!generate) {
            return dateGroups.randomValue()
        } else {
            return Calendar.getInstance().run {
                set(Calendar.DAY_OF_MONTH, rand(0, 30))
                set(Calendar.MONTH, rand(0, 11))

                TICKET_DATE_FORMAT.format(time)
            }
        }
    }

    private fun randomTicketInfo(date: String = randomDate()): TicketInfo {
        val country = CitiesMap.randomKey()
        val city = CitiesMap.getValue(country).randomValue()

        return TicketInfo(randomDate(), city, country)
    }

    fun randomTicket(index: Int, departure: String = randomDate(), back: String = randomDate()): Ticket {
        return Ticket(index, randomTicketInfo(departure), randomTicketInfo(back), UserInterestType.ORDINAL)
    }

    fun generateFakeTickets(): Array<Ticket> {
        return Array<Ticket>(10) { index ->
            randomTicket(index)
        }
    }
}