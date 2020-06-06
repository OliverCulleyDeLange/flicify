package uk.co.oliverdelange.flicify.redux

import io.flic.flic2libandroid.Flic2Button
import org.reduxkotlin.Reducer
import org.reduxkotlin.applyMiddleware
import org.reduxkotlin.createStore

sealed class Events {
    sealed class Scan {
        object Start
        object Stop
    }
    sealed class Button {
        object Down
        data class Connect(val button: Flic2Button)
        data class Ready(val button: Flic2Button, val timestamp: Long)
        data class Disconnect(val button: Flic2Button)
        data class Unpaired(val button: Flic2Button)
    }
}

sealed class Results {
    sealed class Scan {
        data class ScanSuccess(val result: Int, val subCode: Int, val button: Flic2Button)
        data class ScanFailure(val result: Int, val errorString: String)
        object FlicDiscoveredButAlreadyPaired
        object FlicDiscovered
        object FlicConnected
    }
    sealed class LocationPermission{
        object Granted
        object Denied
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

val reducer: Reducer<AppState> = { state, action ->
    when (action) {
        is Events.Scan.Start -> state.copy(scanning = true)
        is Events.Scan.Stop -> state.copy(scanning = false)
        else -> state
    }
}

val store = createStore(reducer, AppState(), applyMiddleware(loggingMiddleware, startScan))
