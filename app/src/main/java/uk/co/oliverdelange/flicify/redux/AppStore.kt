package uk.co.oliverdelange.flicify.redux

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.freeletics.rxredux.reduxStore
import com.jakewharton.rx.replayingShare
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import io.reactivex.Observable
import io.reactivex.rxkotlin.ofType
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

private val actionsSubject = PublishSubject.create<Action>()

class AppStore(val context: Context) {
    val actions: Observable<Action>
        get() = actionsSubject

    val state: Observable<AppState> = actions
        .reduxStore(
            AppState(spotifyInfo = "Spotify not linked yet"),
            sideEffects(context),
            reducer
        )
        .distinctUntilChanged()
        .doOnNext {
            Timber.v("$it")
        }
        .replayingShare()

    fun currentState(): AppState = state.doOnNext { Timber.v( "currentState: $it") }.blockingFirst()

    /** Dispatch a single action to the store*/
    fun dispatch(action: Action) {
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

    companion object {
        fun dispatch(action: Action) = actionsSubject.onNext(action)
    }
}
