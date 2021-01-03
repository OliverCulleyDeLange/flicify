package uk.co.oliverdelange.flicify.redux

import com.freeletics.rxredux.SideEffect
import io.flic.flic2libandroid.Flic2Manager
import io.reactivex.rxkotlin.ofType
import timber.log.Timber
import uk.co.oliverdelange.flicify.flic.flic2ScanCallback

fun sideEffects() = listOf(
    logging,
    convertMainButtonTap,
    startScan,
    connectToAlreadyPaired,
    stopScan,
    disconnectFlic,
    unpairFlic
)

val logging: SideEffect<AppState, Action> = { actions, state ->
    actions.doOnNext {
        Timber.d(it.toString())
    }.ignoreElements().toObservable()
}

val convertMainButtonTap: SideEffect<AppState, Action> = { actions, state ->
    actions.ofType<Event.Tap.MainButton>()
        .map {
            when (val connectionState = state().flicConnectionState) {
                AppState.FlicConnectionState.Scanning -> Event.StopScan
                is AppState.FlicConnectionState.Connecting -> Event.UnpairFlic(connectionState.button)
                is AppState.FlicConnectionState.Connected -> Event.UnpairFlic(connectionState.button)
                is AppState.FlicConnectionState.Disconnected -> Event.StartScan
            }
        }
}

val startScan: SideEffect<AppState, Action> = { actions, state ->
    actions.ofType<Event.StartScan>()
        .map {
            Timber.d("Starting scan")
            Flic2Manager.getInstance().startScan(flic2ScanCallback())
            Result.Scan.ScanStarted
        }
}

val connectToAlreadyPaired: SideEffect<AppState, Action> = { actions, state ->
    actions.ofType<Result.Scan.FlicDiscoveredButAlreadyPaired>()
        .doOnNext {
            it.button.connect()
        }.ignoreElements().toObservable()
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
        .doOnNext { it.button.disconnectOrAbortPendingConnection() }
        .ignoreElements().toObservable()
}

val unpairFlic: SideEffect<AppState, Action> = { actions, state ->
    actions.ofType<Event.UnpairFlic>()
        .doOnNext {
            Flic2Manager.getInstance().forgetButton(it.button)
        }.ignoreElements().toObservable()
}