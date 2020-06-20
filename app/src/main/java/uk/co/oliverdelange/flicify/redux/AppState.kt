package uk.co.oliverdelange.flicify.redux

import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.PlayerState
import io.flic.flic2libandroid.Flic2Button

data class AppState(
    val flicConnectionState: FlicConnectionState = FlicConnectionState.Disconnected,
    val flicScanStatus: FlicScanStatus = FlicScanStatus.Complete,
    val flicInfo: String = "",
    val flicDown: Boolean = false,

    val spotifyPlayerState: PlayerState? = null,
    val spotifyRemote: SpotifyAppRemote? = null,
    val spotifyInfo: String = ""
) {
    sealed class FlicConnectionState {
        data class Sleeping(val button: Flic2Button) : FlicConnectionState()
        object Scanning : FlicConnectionState()
        data class Connected(val button: Flic2Button) : FlicConnectionState()
        object Disconnected : FlicConnectionState()
    }

    enum class FlicScanStatus {
        DiscoveredButAlreadyPaired, Discovered, Connected, Complete
    }
}