package uk.co.oliverdelange.flicify.redux

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.freeletics.rxredux.Reducer
import com.freeletics.rxredux.SideEffect
import com.freeletics.rxredux.reduxStore
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import io.flic.flic2libandroid.Flic2Button
import io.flic.flic2libandroid.Flic2Manager
import io.reactivex.Observable
import io.reactivex.rxkotlin.ofType
import io.reactivex.subjects.PublishSubject
import uk.co.oliverdelange.flicify.flic.flic2ScanCallback

interface Action

sealed class Event : Action {
    object CheckPermissions : Action

    sealed class Tap : Event() {
        object MainButton : Tap()
    }

    sealed class Button : Event() {
        object Down : Button()
        data class Connect(val button: Flic2Button) : Button()
        data class Ready(val button: Flic2Button, val timestamp: Long) : Button()
        data class Disconnect(val button: Flic2Button) : Button()
        data class Unpaired(val button: Flic2Button) : Button()
    }
}

sealed class Result : Action {
    sealed class Scan : Result() {
        object ScanStarted : Scan()
        object ScanStopped : Scan()
        data class ScanSuccess(val result: Int, val subCode: Int, val button: Flic2Button) : Scan()
        data class ScanFailure(val result: Int, val errorString: String) : Scan()
        object FlicDiscoveredButAlreadyPaired : Scan()
        object FlicDiscovered : Scan()
        object FlicConnected : Scan()
    }

    sealed class LocationPermission : Result() {
        object Granted : LocationPermission()
        object Denied : LocationPermission()
    }
}

data class AppState(
    val scanning: Boolean = false,
    val scanStatus: ScanStatus = ScanStatus.Complete,
    val info: String = ""
) {
    enum class ScanStatus {
        DiscoveredButAlreadyPaired, Discovered, Connected, Complete
    }
}

val reducer: Reducer<AppState, Action> = { state, action ->
    when (action) {
        is Event.Tap.MainButton -> state.copy(scanning = !state.scanning)
        is Result.Scan.ScanFailure -> state.copy(scanning = false, info = "Error ${action.result}: ${action.errorString}")
        is Result.Scan.FlicConnected,
        is Result.Scan.ScanSuccess -> state.copy(scanning = false)
        else -> state
    }
}

val logging: SideEffect<AppState, Action> = { actions, state ->
    actions.doOnNext {
        Log.v("ACTION", it.toString())
    }.ignoreElements().toObservable()
}

val startStopScan: SideEffect<AppState, Action> = { actions, state ->
    actions.ofType<Event.Tap.MainButton>()
        .map {
            // State is reduced before SideEffects happen
            if (state().scanning) {
                Log.d("SCAN", "Starting scan")
                Flic2Manager.getInstance().startScan(flic2ScanCallback())
                Result.Scan.ScanStarted
            } else {
                Flic2Manager.getInstance().stopScan()
                Result.Scan.ScanStopped
            }
        }
}

object AppStore {
    private val actionsSubject = PublishSubject.create<Action>()
    val actions: Observable<Action>
        get() = actionsSubject

    val state: Observable<AppState> = actions
        .reduxStore(AppState(), listOf(logging, startStopScan), reducer)
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
