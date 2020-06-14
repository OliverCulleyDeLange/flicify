package uk.co.oliverdelange.flicify.view

import android.view.View
import android.widget.Button
import io.reactivex.Observable

fun visibleIf(visible: Boolean) = if (visible) View.VISIBLE else View.GONE

fun Button.clicks() = Observable.create<Unit> { emitter ->
    setOnClickListener {
        emitter.onNext(Unit)
    }
}
