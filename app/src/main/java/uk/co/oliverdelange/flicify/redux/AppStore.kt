package uk.co.oliverdelange.flicify.redux

import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.freeletics.rxredux.reduxStore
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import io.reactivex.Observable
import io.reactivex.rxkotlin.ofType
import io.reactivex.subjects.PublishSubject

object AppStore {
    private val actionsSubject = PublishSubject.create<Action>()
    val actions: Observable<Action>
        get() = actionsSubject

    val state: Observable<AppState> = actions
        .reduxStore(AppState(), sideEffects(), reducer)
        .doOnNext {
            Log.v("STATE", "$it")
        }
        .distinctUntilChanged()
        .share()

    /** Dispatch a single action to the store*/
    fun dispatch(action: Action) {
        Log.v("STORE", "Dispatching $action")
        actionsSubject.onNext(action)
    }

    /** Push a stream of actions to the store*/
    fun push(scope: LifecycleOwner, actions: Observable<Action>) {
        actions.doOnNext { dispatch(it) }.autoDispose(scope.scope()).subscribe()
    }

    /** Subscribe to state changes */
    fun state(scope: LifecycleOwner, stateHandler: (AppState) -> Unit) =
        state.autoDispose(scope.scope()).subscribe(stateHandler)

    /** Subscribe to actions */
    inline fun <reified T : Any> actions(scope: LifecycleOwner, noinline actionHandler: (T) -> Unit) {
        actions.ofType<T>().doOnNext(actionHandler).autoDispose(scope.scope()).subscribe()
    }
}
