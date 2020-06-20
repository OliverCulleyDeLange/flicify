package uk.co.oliverdelange.flicify.redux

import com.spotify.protocol.types.PlayerState
import io.flic.flic2libandroid.Flic2Button

data class AppState(
    val connectionState: FlicConnectionState = FlicConnectionState.Disconnected,
    val scanStatus: FlicScanStatus = FlicScanStatus.Complete,
    val flicInfo: String = "",
    val flicDown: Boolean = false,
    val playerState: PlayerState? = null,
    val spotifyInfo: String = ""
) {
    sealed class FlicConnectionState {
        object Scanning : FlicConnectionState()
        data class Connected(val button: Flic2Button) : FlicConnectionState()
        object Disconnected : FlicConnectionState()
    }

    enum class FlicScanStatus {
        DiscoveredButAlreadyPaired, Discovered, Connected, Complete
    }
}