package io.codetail.airplanes.domain

import android.arch.persistence.room.*
import io.reactivex.Flowable
import org.reactivestreams.Publisher

/**
 * created at 5/19/17
 *
 * @author Ozodrukh
 * @version 1.0
 */
@Dao
interface TicketsDAO {

    @Query("SELECT * FROM tours WHERE interestType = :p0")
    fun find(type: UserInterestType): Flowable<List<Ticket>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(ticket: Ticket)

    @Delete
    fun delete(ticket: Ticket)

}