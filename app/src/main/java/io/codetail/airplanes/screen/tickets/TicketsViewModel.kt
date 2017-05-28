package io.codetail.airplanes.screen.tickets

import io.codetail.airplanes.domain.Ticket
import java.util.*

/**
 * created at 5/19/17
 *
 * @author Ozodrukh
 * @version 1.0
 */

class TicketsViewModel() : android.arch.lifecycle.ViewModel() {

    private val database = io.codetail.airplanes.ToursApp.Companion.self.database
    private val livePublisher = io.reactivex.processors.BehaviorProcessor.create<List<io.codetail.airplanes.domain.Ticket>>()
    private val actionsExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

    private val tickets = java.util.ArrayList<Ticket>(io.codetail.airplanes.domain.FakeTickets)

    init {

    }

    fun add(ticket: io.codetail.airplanes.domain.Ticket) {
        tickets += ticket

        livePublisher.onNext(java.util.ArrayList(tickets))
    }

    /**
     * Updates ticket with new state. Performs asynchronous saving operation
     */
    fun setUserInterestType(ticket: io.codetail.airplanes.domain.Ticket, interestType: io.codetail.airplanes.domain.UserInterestType) {
        if (ticket.interestType == interestType) {
            ticket.interestType = io.codetail.airplanes.domain.UserInterestType.ORDINAL
        } else {
            ticket.interestType = interestType
        }

        actionsExecutor.submit {
            database.ticketsService().insert(ticket)
        }
    }

    /**
     * Live Tickets are loaded from Firebase, to get cached getTickets
     * use {@link #getTickets} instead
     */
    fun liveTickets(): io.reactivex.Flowable<List<Ticket>> {
        if (!livePublisher.hasSubscribers()) {
            com.google.firebase.database.FirebaseDatabase.getInstance().getReference("data")
                    .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                        override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                            livePublisher.onError(com.google.firebase.FirebaseException(error.message))
                        }

                        override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                            livePublisher.onNext(snapshot.children.map { io.codetail.airplanes.domain.Ticket(it) }.toList())
                        }
                    })
        }

        return livePublisher.share()
    }

    /**
     * Flattens dictionary to the list so that first item is list and then
     * sequence of related items
     *
     * Date -> Ticket, Ticket, Ticket
     * Date -> Ticket
     *
     * @return flat objects list
     */
    fun liveTicketsByDate(): io.reactivex.Flowable<List<Any>> {
        return liveTickets().map(groupByDate())
    }

    /**
     * Loads getTickets from database
     */
    fun tickets(interestType: io.codetail.airplanes.domain.UserInterestType = io.codetail.airplanes.domain.UserInterestType.ORDINAL): io.reactivex.Flowable<List<Ticket>> {
        return database.ticketsService().find(interestType)
    }

    /**
     * Flattens dictionary to the list so that first item is list and then
     * sequence of related items
     *
     * Date -> Ticket, Ticket, Ticket
     * Date -> Ticket
     *
     * @return flat objects list
     */
    fun ticketsByDate(interestType: io.codetail.airplanes.domain.UserInterestType): io.reactivex.Flowable<List<Any>> {
        return tickets(interestType).map(groupByDate())
    }

    /**
     * Maps list of getTickets to sequence of Date -> Tickets in the list
     */
    private fun groupByDate(): io.reactivex.functions.Function<List<Ticket>, List<Any>> {
        return io.reactivex.functions.Function<List<Ticket>, List<Any>> { tickets ->
            val group = tickets.groupBy { it.departure.date }
                    .map { entry -> listOf(entry.key, *entry.value.toTypedArray()) }

            if (group.isEmpty())
                emptyList()
            else group.reduce { acc, list ->
                (acc as? ArrayList ?: ArrayList(acc)).apply { addAll(list) }
            }
        }
    }
}