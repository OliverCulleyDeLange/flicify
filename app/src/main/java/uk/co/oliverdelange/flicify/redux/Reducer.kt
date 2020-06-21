package uk.co.oliverdelange.flicify.redux

import com.freeletics.rxredux.Reducer

val reducer: Reducer<AppState, Action> = { state, action ->
    when (action) {
        is Result.Scan.ScanStarted -> state.copy(
            flicConnectionState = AppState.FlicConnectionState.Scanning,
            flicInfo = "Press and hold your Flic until it connects"
        )
        is Result.Scan.ScanStopped -> state.copy(
            flicConnectionState = AppState.FlicConnectionState.Disconnected,
            flicInfo = "Scan stopped"
        )
        is Result.Scan.ScanSuccess -> state.copy(
            flicConnectionState = AppState.FlicConnectionState.Connected(action.button),
            flicInfo = "Scan Success (${action.result}-${action.subCode}) ${action.button}"
        )
        is Result.Scan.ScanFailure -> state.copy(
            flicConnectionState = AppState.FlicConnectionState.Disconnected,
            flicInfo = "Scan Error ${action.result}: ${action.errorString}"
        )
        is Result.Scan.FlicDiscovered -> state.copy(flicInfo = "Found a flic button... now connecting")
        is Result.Scan.FlicConnected -> state.copy(flicInfo = "Connected... now pairing")
        is Result.Scan.FlicDiscoveredButAlreadyPaired -> state.copy(flicInfo = "Found a flic, but it's already paired")

        is Result.Flic.ConnectRequest -> state.copy(flicInfo = "Flic ready", flicConnectionState = AppState.FlicConnectionState.Sleeping(action.button))
        is Result.Flic.Connect -> state.copy(flicConnectionState = AppState.FlicConnectionState.Connected(action.button))
        is Result.Flic.Disconnected -> state.copy(flicConnectionState = AppState.FlicConnectionState.Disconnected)
        is Result.Flic.Ready -> state.copy(flicInfo = "Flic ready", flicConnectionState = AppState.FlicConnectionState.Connected(action.button))
        is Result.Flic.Unpaired -> state.copy(flicInfo = "Flic disconnected", flicConnectionState = AppState.FlicConnectionState.Disconnected)

        is Event.Flic -> state.copy(
            flicDown = action.isDown,
            flicInfo = "Flic ${if (action.isDown) "pressed" else "connected"}!",
            flicConnectionState = AppState.FlicConnectionState.Connected(action.button)
        )

        is Result.SpotifyConnected -> state.copy(spotifyInfo = "Spotify connected", spotifyRemote = action.remote)
        is Result.SpotifyError -> state.copy(spotifyInfo = "Spotify error: ${action.t}")
        is Result.SpotifyPlayerUpdate -> state.copy(spotifyPlayerState = action.playerState)

        else -> state
    }
}