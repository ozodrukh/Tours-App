package io.codetail.airplanes.ext

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.support.v4.content.res.ResourcesCompat
import android.util.TypedValue

/**
 * created at 5/24/17
 *
 * @author Ozodrukh
 * @version 1.0
 */
val TMP_VALUE = TypedValue()

inline fun <R> attrs(target: TypedArray, block: TypedArray.() -> R): R {
    try {
        return target.block()
    } finally {
        target.recycle()
    }
}

fun Int.loadResColor(context: Context): Int = loadResColor(context.resources, context.theme)
fun Int.loadResColor(resource: Resources, theme: Resources.Theme): Int
        = ResourcesCompat.getColor(resource, this, theme)

/**
 * @return Resource Id
 */
@SuppressLint("Recycle")
fun Int.loadAttr(context: Context): Int {
    attrs(context.obtainStyledAttributes(intArrayOf(this))) {
        if (getValue(0, TMP_VALUE)) {
            return TMP_VALUE.resourceId
        } else {
            throw Resources.NotFoundException("Attribute or Resource not found")
        }
    }
}

val Resources.density: Float
    get() = displayMetrics.density

fun Int.dp(context: Context): Int {
    return times(context.resources.density).toInt()
}