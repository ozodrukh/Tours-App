package io.codetail.airplanes.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.os.Handler
import android.os.Looper
import timber.log.Timber
import java.io.Closeable
import java.io.InputStream
import java.util.concurrent.Executors

/**
 * created at 5/24/17

 * @author Ozodrukh
 * *
 * @version 1.0
 */

object BackgroundLoader {

    private inline fun <T : Closeable, R> closing(receiver: T, block: T.() -> R): R {
        try {
            return receiver.block();
        } finally {
            receiver.close()
        }
    }

    private const val IMG = "imgs/plane_primary_bg.png";
    private val uiExecutor = Handler(Looper.getMainLooper())
    private val ioExecutors = Executors.newSingleThreadExecutor()

    fun dispatch(context: Context, bounds: Rect, callback: (drawable: Drawable) -> Unit) {
        ioExecutors.submit {
            try {
                val targetBounds = closing(context.assets.open(IMG)) { getBounds(this) }
                val bitmap = closing(context.assets.open(IMG)) {
                    getDrawable(context.assets.open(IMG), targetBounds.width() / bounds.width())
                }

                Timber.d("Bitmap loaded {$bitmap}")

                uiExecutor.post {
                    val animate = TransitionDrawable(arrayOf(
                            ColorDrawable(Color.TRANSPARENT),
                            BitmapDrawable(context.resources, bitmap)
                    ))
                    animate.isCrossFadeEnabled = true

                    Timber.d("Transition created {$animate}")
                    callback(animate)

                    animate.startTransition(300)
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private fun getDrawable(stream: InputStream, sample: Int): Bitmap {
        val options = BitmapFactory.Options();
        options.inSampleSize = sample

        return BitmapFactory.decodeStream(stream, Rect(), options)
    }

    private fun getBounds(stream: InputStream): Rect {
        val options = BitmapFactory.Options();
        options.inJustDecodeBounds = true

        BitmapFactory.decodeStream(stream, Rect(), options)
        return Rect(0, 0, options.outWidth, options.outHeight)
    }

}
