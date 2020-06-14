package uk.co.oliverdelange.flicify.redux

import android.util.Log
import com.freeletics.rxredux.SideEffect
import io.flic.flic2libandroid.Flic2Manager
import io.reactivex.rxkotlin.ofType
import uk.co.oliverdelange.flicify.flic.flic2ScanCallback

fun sideEffects() = listOf(
    logging,
    convertMainButtonTap,
    startScan,
    stopScan,
    disconnectFlic
)

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