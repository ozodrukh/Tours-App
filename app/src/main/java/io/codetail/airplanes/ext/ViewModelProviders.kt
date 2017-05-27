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


fun <T : ViewModel> ViewModelProvider.get(clazz: KClass<T>,
                                          key: String = clazz.qualifiedName ?: "no-name")
        : T = get(key, clazz.java)