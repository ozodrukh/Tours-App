package io.codetail.airplanes.global


import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.os.Looper
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import io.codetail.airplanes.utils.Reflections
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import timber.log.Timber
import java.lang.reflect.Proxy

/**
 * created at 5/20/17

 * @author Ozodrukh
 * *
 * @version 1.0
 */
abstract class ObserverAdapter<T, VH : RecyclerView.ViewHolder>
(// Constructor arguments
        val lifecycle: Lifecycle,
        dataFlow: Flowable<List<T>>
)
    : RecyclerView.Adapter<VH>(), LifecycleObserver {

    private lateinit var subscription: Subscription

    private var items = DataHolder<T>()
    private var attachedToParent = true
    private var parent: RecyclerView? = null

    private val flowCallback = object : Subscriber<DiffUtil.DiffResult> {
        override fun onSubscribe(s: Subscription) {
            s.request(Long.MAX_VALUE)

            subscription = s
        }

        override fun onNext(result: DiffUtil.DiffResult) {
            val threadName = Thread.currentThread().name
            Timber.d("Diff calculated. Items in adapter $itemCount updating on $threadName thread")

            if (attachedToParent && !(parent?.isComputingLayout ?: false)) {
                result.dispatchUpdatesTo(this@ObserverAdapter)
            }
        }

        override fun onError(t: Throwable) =
                Timber.e(t, "Observer adapter haven't implemented Error handling")

        override fun onComplete() = destroy()
    }

    init {
        dataFlow.doOnNext { Timber.d("Observed new $it") }
                .map { data -> DataHolder(items.new, data) }
                .doOnNext { items = it }
                .map { DiffUtil.calculateDiff(it) }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(flowCallback)

        lifecycle.addObserver(this)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        parent = recyclerView
        attachedToParent = true
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        parent = null
        attachedToParent = false
    }

    @OnLifecycleEvent(value = Lifecycle.Event.ON_DESTROY)
    fun destroy() {
        subscription.cancel()
        lifecycle.removeObserver(this)
        Timber.d("Adapter is destroyed")
    }

    fun getItem(position: Int): T = items.new[position]

    override fun getItemCount(): Int {
        return items.new.size
    }

    private class DataHolder<T>(var current: List<T> = emptyList(),
                                var new: List<T> = emptyList())
        : DiffUtil.Callback() {

        override fun getOldListSize(): Int = current.size
        override fun getNewListSize(): Int = new.size

        override fun areItemsTheSame(left: Int, right: Int): Boolean {
            return current[left] === new[right]
        }

        override fun areContentsTheSame(left: Int, right: Int): Boolean {
            return current[left] == new[right]
        }

        fun mutate(fresh: List<T>): DataHolder<T> {
            return DataHolder(current = new, new = fresh)
        }

        override fun toString(): String {
            return "Holder{old=${current.size}, new=${new.size}}"
        }
    }
}
