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
    object StartScan : Action
    object StopScan : Action
    data class DisconnectFlic(val button: Flic2Button) : Action

    sealed class Tap : Event() {
        object MainButton : Tap()
    }

    data class Flic(
        val button: Flic2Button,
        val wasQueued: Boolean,
        val lastQueued: Boolean,
        val timestamp: Long,
        val isUp: Boolean,
        val isDown: Boolean
    ) : Event()
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

    sealed class Flic : Result() {
        data class Connect(val button: Flic2Button) : Flic()
        data class Disconnected(val button: Flic2Button) : Flic()
        data class Ready(val button: Flic2Button, val timestamp: Long) : Flic()
        data class Unpaired(val button: Flic2Button) : Flic()
    }

    sealed class LocationPermission : Result() {
        object Granted : LocationPermission()
        object Denied : LocationPermission()
    }
}

data class AppState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val scanStatus: ScanStatus = ScanStatus.Complete,
    val info: String = "",
    val flicDown: Boolean = false
) {
    sealed class ConnectionState {
        object Scanning : ConnectionState()
        data class Connected(val button: Flic2Button) : ConnectionState()
        object Disconnected : ConnectionState()
    }

    enum class ScanStatus {
        DiscoveredButAlreadyPaired, Discovered, Connected, Complete
    }
}

val reducer: Reducer<AppState, Action> = { state, action ->
    when (action) {
        is Result.Scan.ScanStarted -> state.copy(connectionState = AppState.ConnectionState.Scanning)
        is Result.Scan.ScanStopped -> state.copy(connectionState = AppState.ConnectionState.Disconnected)
        is Result.Scan.ScanSuccess -> state.copy(
            connectionState = AppState.ConnectionState.Connected(action.button),
            info = "Success (${action.result}-${action.subCode}) ${action.button}"
        )
        is Result.Scan.ScanFailure -> state.copy(
            connectionState = AppState.ConnectionState.Disconnected,
            info = "Error ${action.result}: ${action.errorString}"
        )
        is Result.Scan.FlicDiscovered -> state.copy(info = "Found a flic button... now connecting")
        is Result.Scan.FlicConnected -> state.copy(info = "Connected... now pairing")
        is Result.Scan.FlicDiscoveredButAlreadyPaired -> state.copy(info = "Found a flic, but it's already paired")

        is Result.Flic.Connect -> state.copy(connectionState = AppState.ConnectionState.Connected(action.button))
        is Result.Flic.Disconnected -> state.copy(connectionState = AppState.ConnectionState.Disconnected)
        is Result.Flic.Ready -> state.copy(info = "Flic ready!")
        is Result.Flic.Unpaired -> state.copy(info = "Flic upaired!")

        is Event.Flic -> state.copy(flicDown = action.isDown, info = "Fic ${if (action.isDown) "pressed" else "connected"}!")
        else -> state
    }
}

val logging: SideEffect<AppState, Action> = { actions, state ->
    actions.doOnNext {
        Log.d("ACTION", it.toString())
    }.ignoreElements().toObservable()
}

val convertMainButtonTap: SideEffect<AppState, Action> = { actions, state ->
    actions.ofType<Event.Tap.MainButton>()
        .map {
            when (val connectionState = state().connectionState) {
                AppState.ConnectionState.Scanning -> Event.StopScan
                is AppState.ConnectionState.Connected -> Event.DisconnectFlic(connectionState.button)
                AppState.ConnectionState.Disconnected -> Event.StartScan
            }
        }
}

val startScan: SideEffect<AppState, Action> = { actions, state ->
    actions.ofType<Event.StartScan>()
        .map {
            Log.d("SCAN", "Starting scan")
            Flic2Manager.getInstance().startScan(flic2ScanCallback())
            Result.Scan.ScanStarted
        }
}

val stopScan: SideEffect<AppState, Action> = { actions, state ->
    actions.ofType<Event.StopScan>()
        .map {
            Flic2Manager.getInstance().stopScan()
            Result.Scan.ScanStopped
        }
}

val disconnectFlic: SideEffect<AppState, Action> = { actions, state ->
    actions.ofType<Event.DisconnectFlic>()
        .map {
            Flic2Manager.getInstance().forgetButton(it.button)
            Result.Flic.Disconnected(it.button)
        }
}

val sideEffects: List<SideEffect<AppState, Action>> = listOf(
    logging, convertMainButtonTap, startScan, stopScan, disconnectFlic
)

object AppStore {
    private val actionsSubject = PublishSubject.create<Action>()
    val actions: Observable<Action>
        get() = actionsSubject

    val state: Observable<AppState> = actions
        .reduxStore(AppState(), sideEffects, reducer)
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
