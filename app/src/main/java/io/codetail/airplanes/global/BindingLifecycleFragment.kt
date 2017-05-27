package io.codetail.airplanes.global

import android.arch.lifecycle.LifecycleFragment
import android.content.Context

import android.databinding.ViewDataBinding
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowId

/**
 * created at 5/20/17
 * @author Ozodrukh
 * @version 1.0
 */

abstract class BindingLifecycleFragment<out T : ViewDataBinding> : LifecycleFragment() {
    /** Fragment View part layout resource to inflate */
    protected abstract val layoutId: Int

    protected val dataBindingDelegate = LazyDataBindingComponent<T>();
    protected val viewComponent: T by dataBindingDelegate;
    private val lazyArguments = ArrayList<LazyArgumentValue<*>>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dataBindingDelegate.inflate(inflater, layoutId, container)
        return viewComponent.root;
    }

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)

        args?.let { arguments -> lazyArguments.forEach { it.init(arguments) } }
    }

    protected fun <T : Any> lazyArgument(name: String): LazyArgumentValue<T> {
        return LazyArgumentValue<T>(name).apply {
            lazyArguments.add(this)
        }
    }

    protected class LazyArgumentValue<T : Any>(val name: String) : Lazy<T> {
        lateinit var argument: T
        var argumentsSet = false

        override val value: T
            get() {
                if (!isInitialized()) {
                    throw RuntimeException("Arguments were not set yet")
                }

                return argument
            }

        fun init(args: Bundle = Bundle.EMPTY) {
            argumentsSet = true
            argument = args.get(name) as T? ?:
                    throw IllegalArgumentException("Not found $name")
        }

        override fun isInitialized(): Boolean {
            return argumentsSet
        }

    }
}
