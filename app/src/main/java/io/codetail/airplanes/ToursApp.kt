package io.codetail.airplanes

import android.app.Application
import io.codetail.airplanes.domain.AppDatabase
import io.codetail.airplanes.domain.Random
import timber.log.Timber
import timber.log.Timber.DebugTree

/**
 * created at 5/19/17

 * @author Ozodrukh
 * *
 * @version 1.0
 */

class ToursApp : Application() {

    companion object {
        val self: ToursApp
            get() = app

        private lateinit var app: ToursApp
    }

    val database by lazy {
        AppDatabase.createPersistentDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        app = this

        Timber.plant(DebugTree())
    }

}
