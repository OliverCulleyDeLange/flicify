package uk.co.oliverdelange.flicify.redux

import com.freeletics.rxredux.Reducer

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