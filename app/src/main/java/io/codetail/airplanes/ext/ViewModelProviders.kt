package io.codetail.airplanes.ext

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import kotlin.reflect.KClass

/**
 * created at 5/20/17
 *
 * @author Ozodrukh
 * @version 1.0
 */


inline fun <reified T : ViewModel> ViewModelProvider.get(key: String = T::class.java.name): T =
        get(key, T::class.java)