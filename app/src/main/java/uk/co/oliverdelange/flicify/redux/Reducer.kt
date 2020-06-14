package uk.co.oliverdelange.flicify.redux

import com.freeletics.rxredux.Reducer

val reducer: Reducer<AppState, Action> = { state, action ->
    when (action) {
        is Result.Scan.ScanStarted -> state.copy(connectionState = AppState.ConnectionState.Scanning)
        is Result.Scan.ScanStopped -> state.copy(connectionState = AppState.ConnectionState.Disconnected)
        is Result.Scan.ScanSuccess -> state.copy(
            connectionState = AppState.ConnectionState.Connected(action.button),
            flicInfo = "Success (${action.result}-${action.subCode}) ${action.button}"
        )
        is Result.Scan.ScanFailure -> state.copy(
            connectionState = AppState.ConnectionState.Disconnected,
            flicInfo = "Error ${action.result}: ${action.errorString}"
        )
        is Result.Scan.FlicDiscovered -> state.copy(flicInfo = "Found a flic button... now connecting")
        is Result.Scan.FlicConnected -> state.copy(flicInfo = "Connected... now pairing")
        is Result.Scan.FlicDiscoveredButAlreadyPaired -> state.copy(flicInfo = "Found a flic, but it's already paired")

        is Result.Flic.Connect -> state.copy(connectionState = AppState.ConnectionState.Connected(action.button))
        is Result.Flic.Disconnected -> state.copy(connectionState = AppState.ConnectionState.Disconnected)
        is Result.Flic.Ready -> state.copy(flicInfo = "Flic ready!")
        is Result.Flic.Unpaired -> state.copy(flicInfo = "Flic upaired!")

        is Event.Flic -> state.copy(flicDown = action.isDown, flicInfo = "Fic ${if (action.isDown) "pressed" else "connected"}!")

        is Result.SpotifyConnected -> state.copy(spotifyInfo = "Spotify connected")
        is Result.SpotifyError -> state.copy(spotifyInfo = "Spotify error: ${action.t}")
        is Result.SpotifyPlayerUpdate -> state.copy(spotifyInfo = "Currently playing: ${action.playerState.track.name} by ${action.playerState.track.artist.name}")
        else -> state
    }
}