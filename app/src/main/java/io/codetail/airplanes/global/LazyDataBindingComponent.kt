package io.codetail.airplanes.global;

import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.view.LayoutInflater
import android.view.ViewGroup

class LazyDataBindingComponent<out T : ViewDataBinding> : Lazy<T> {
    private var inflated = false
    private lateinit var component: T;

    override val value: T
        get() = component

    /**
     * Inflates DataBindingComponent using DataBindingUtil
     */
    fun inflate(inflater: LayoutInflater, layoutId: Int, container: ViewGroup? = null,
                attachToParent: Boolean = false) {
        if (isInitialized()) {
            throw UnsupportedOperationException("ViewDataBinding component has already been inflated")
        }

        component = DataBindingUtil.inflate(inflater, layoutId, container, attachToParent)
        inflated = true
    }

    override fun isInitialized(): Boolean {
        return inflated;
    }
}