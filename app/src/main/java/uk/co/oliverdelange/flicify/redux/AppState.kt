package uk.co.oliverdelange.flicify.redux

import io.flic.flic2libandroid.Flic2Button

data class AppState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val scanStatus: ScanStatus = ScanStatus.Complete,
    val flicInfo: String = "",
    val flicDown: Boolean = false,
    val spotifyInfo: String = ""
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