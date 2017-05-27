package io.codetail.airplanes.screen

import android.arch.lifecycle.ViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.codetail.airplanes.ToursApp
import io.codetail.airplanes.domain.FakeTickets
import io.codetail.airplanes.domain.Ticket
import io.codetail.airplanes.domain.UserInterestType
import io.reactivex.Flowable
import io.reactivex.functions.Function
import io.reactivex.processors.BehaviorProcessor
import java.util.*
import java.util.concurrent.Executors

/**
 * created at 5/19/17
 *
 * @author Ozodrukh
 * @version 1.0
 */

class TicketsViewModel() : ViewModel() {

    private val database = ToursApp.self.database
    private val livePublisher = BehaviorProcessor.create<List<Ticket>>()
    private val actionsExecutor = Executors.newSingleThreadExecutor()

    private val tickets = ArrayList<Ticket>(FakeTickets)

    init {

    }

    fun add(ticket: Ticket) {
        tickets += ticket

        livePublisher.onNext(ArrayList(tickets))
    }

    /**
     * Updates ticket with new state. Performs asynchronous saving operation
     */
    fun setUserInterestType(ticket: Ticket, userInterestType: UserInterestType) {
        if (ticket.interestType == userInterestType) {
            ticket.interestType = UserInterestType.ORDINAL
        } else {
            ticket.interestType = userInterestType
        }

        actionsExecutor.submit {
            database.ticketsService().insert(ticket)
        }
    }

    /**
     * Live Tickets are loaded from Firebase, to get cached tickets
     * use {@link #tickets} instead
     */
    fun liveTickets(): Flowable<List<Ticket>> {
        if (!livePublisher.hasSubscribers()) {
            FirebaseDatabase.getInstance().getReference("data")
                    .addValueEventListener(object : ValueEventListener {
                        override fun onCancelled(error: DatabaseError) {
                            livePublisher.onError(FirebaseException(error.message))
                        }

                        override fun onDataChange(snapshot: DataSnapshot) {
                            livePublisher.onNext(snapshot.children.map { Ticket(it) }.toList())
                        }
                    })
        }

        return livePublisher.share()
    }

    /**
     * Loads tickets from database
     */
    fun tickets(interestType: UserInterestType = UserInterestType.ORDINAL): Flowable<List<Ticket>> {
        return database.ticketsService().find(interestType)
    }

    /**
     * Maps just list of tickets to sequence of Date -> Tickets in the list
     */
    fun separatedByDate(): Function<List<Ticket>, List<Any>> {
        return Function<List<Ticket>, List<Any>> { tickets ->
            tickets.groupBy { it.departure.date }
                    .map { entry -> listOf(entry.key, *entry.value.toTypedArray()) }
                    .run {
                        when (size) {
                            0 -> emptyList()
                            else -> reduce { acc, list ->
                                (acc as? ArrayList ?: ArrayList(acc)).apply { addAll(list) }
                            }
                        }
                    }
        }
    }
}