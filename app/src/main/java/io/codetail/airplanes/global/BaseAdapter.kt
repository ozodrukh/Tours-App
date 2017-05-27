package io.codetail.airplanes.global


import android.arch.lifecycle.Lifecycle
import android.content.Context
import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import io.reactivex.Flowable

/**
 * created at 5/20/17

 * @author Ozodrukh
 * *
 * @version 1.0
 */
class BaseAdapter<T, VC : ViewDataBinding>(lifecycle: Lifecycle, dataFlow: Flowable<List<T>>)
    : ObserverAdapter<T, BaseAdapter.DataBindingHolder<T, VC>>(lifecycle, dataFlow) {

    private lateinit var layoutInflater: LayoutInflater
    private val viewsMap = mutableMapOf<Int, ViewTypeFactory>()

    fun add(viewFactory: ViewTypeFactory) {
        viewsMap[viewFactory.viewType] = viewFactory
    }

    override fun onAttachedToRecyclerView(parent: RecyclerView) {
        super.onAttachedToRecyclerView(parent)

        layoutInflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    @Suppress("unchecked_cast")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataBindingHolder<T, VC> {
        val viewFactory = viewsMap[viewType]
        if (viewFactory is BindingViewFactory<*, *>) {
            return DataBindingHolder<T, VC>(
                    viewFactory.createViewComponent(layoutInflater, parent) as VC,
                    viewFactory as BindingViewFactory<T, VC>
            )
        } else {
            TODO("support other view types")
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        viewsMap.forEach { entry ->
            if (entry.value.supports(this, position, item as Any))
                return entry.key
        }

        throw IllegalStateException("No ViewTypeFactory found for $position")
    }

    override fun onBindViewHolder(holder: DataBindingHolder<T, VC>, position: Int) {
        holder.binder.bind(holder.viewComponent, getItem(position), position)
    }

    class DataBindingHolder<T, VC : ViewDataBinding>(val viewComponent: VC,
                                                     val binder: BindingViewFactory<T, VC>)
        : RecyclerView.ViewHolder(viewComponent.root) {
    }

    /**
     * Represent View Type that will be shown in adapter.
     * View type must have unique identifier
     */
    interface ViewTypeFactory {
        val viewType: Int

        /**
         * Supports
         */
        fun supports(adapter: BaseAdapter<*, *>, position: Int, item: Any): Boolean = true
    }

    interface BindingViewFactory<in Data, VC : ViewDataBinding> : ViewTypeFactory {

        /**
         * Bind Data object with view on the position
         *
         * @param component (ViewDataBinding) that represent this view
         * @param ticket Data on the position
         * @param position View position in adapter
         */
        fun bind(component: VC, data: Data, position: Int);

        /**
         * Creates ViewComponent for this ViewType
         */
        fun createViewComponent(inflater: LayoutInflater, parent: ViewGroup): VC {
            return DataBindingUtil.inflate<VC>(inflater, viewType, parent, false)
        }
    }
}