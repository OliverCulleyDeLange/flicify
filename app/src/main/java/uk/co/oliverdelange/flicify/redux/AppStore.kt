package uk.co.oliverdelange.flicify.redux

import android.util.Log
import com.freeletics.rxredux.Reducer
import com.freeletics.rxredux.SideEffect
import com.freeletics.rxredux.reduxStore
import io.flic.flic2libandroid.Flic2Button
import io.flic.flic2libandroid.Flic2Manager
import io.reactivex.rxkotlin.ofType
import io.reactivex.subjects.PublishSubject
import uk.co.oliverdelange.flicify.flic.flic2ScanCallback

interface Action

sealed class Event : Action {
    sealed class Scan : Event() {
        object Start : Scan()
        object Stop : Scan()
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
    val scanStatus: ScanStatus = ScanStatus.Complete
) {
    enum class ScanStatus {
        DiscoveredButAlreadyPaired, Discovered, Connected, Complete
    }
}

val reducer: Reducer<AppState, Action> = { state, action ->
    when (action) {
        is Event.Scan.Start -> state.copy(scanning = true)
        is Event.Scan.Stop -> state.copy(scanning = false)
        else -> state
    }
}

val logging: SideEffect<AppState, Action> = { actions, state ->
    actions.doOnNext {
        Log.v("ACTION", it.toString())
    }.ignoreElements().toObservable()
}

val startScan: SideEffect<AppState, Action> = { actions, state ->
    actions.ofType<Event.Scan.Start>().doOnNext {
        //            (findViewById<View>(R.id.scanNewButton) as Button).text = "Cancel scan"
//            (findViewById<View>(R.id.scanWizardStatus) as TextView).text =
//                "Press and hold down your Flic2 button until it connects"
        Log.d("SCAN", "Starting scan")
        Flic2Manager.getInstance().startScan(flic2ScanCallback())
    }.ignoreElements().toObservable()
}

val stopScan: SideEffect<AppState, Action> = { actions, state ->
    actions.ofType<Event.Scan.Stop>().doOnNext {
        //            scanWizardStatus.text = ""

    }.ignoreElements().toObservable()
}

object AppStore {
    private val actions = PublishSubject.create<Action>()

    val state = actions
        .reduxStore(AppState(), listOf(logging, startScan, stopScan), reducer)
        .distinctUntilChanged()

    fun dispatch(action: Action) {
        actions.onNext(action)
    }
}
