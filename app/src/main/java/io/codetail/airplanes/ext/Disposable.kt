package io.codetail.airplanes.ext

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.internal.disposables.DisposableContainer

/**
 * created at 5/20/17

 * @author Ozodrukh
 * *
 * @version 1.0
 */

operator fun CompositeDisposable.plus(disposable: Disposable): Boolean {
    return add(disposable)
}

// Non Static
operator fun CompositeDisposable.minus(disposable: Disposable): Boolean  {
    return remove(disposable)
}