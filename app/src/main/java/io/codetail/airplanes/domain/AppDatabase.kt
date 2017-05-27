package io.codetail.airplanes.domain

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import android.content.Context

/**
 * created at 5/19/17

 * @author Ozodrukh
 * *
 * @version 1.0
 */

@Database(
        version = 1,
        entities = arrayOf(Ticket::class)
)
@TypeConverters(TickersConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ticketsService(): TicketsDAO

    companion object {
        const val DB_NAME = "tours-app-main.db"

        fun createInMemoryDatabase(context: Context): AppDatabase
                = Room.inMemoryDatabaseBuilder(context.applicationContext, AppDatabase::class.java).build()

        fun createPersistentDatabase(context: Context): AppDatabase
                = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME).build()
    }
}
