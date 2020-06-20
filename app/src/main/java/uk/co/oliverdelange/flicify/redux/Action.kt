package uk.co.oliverdelange.flicify.redux

import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.PlayerState
import io.flic.flic2libandroid.Flic2Button

interface Action

sealed class Event : Action {
    object CheckPermissions : Action
    object StartScan : Action
    object StopScan : Action
    data class DisconnectFlic(val button: Flic2Button) : Action

    sealed class Tap : Event() {
        object MainButton : Tap()
    }

    data class Flic(
        val button: Flic2Button,
        val wasQueued: Boolean,
        val lastQueued: Boolean,
        val timestamp: Long,
        val isUp: Boolean,
        val isDown: Boolean
    ) : Event()
}

sealed class Result : Action {
    sealed class Track : Action {
        data class SavedToLibrary(val track: com.spotify.protocol.types.Track) : Track()
        data class RemovedFromLibrary(val track: com.spotify.protocol.types.Track) : Track()
        data class CanNotSaveToLibrary(val track: com.spotify.protocol.types.Track) : Track()
    }

    data class SpotifyPlayerUpdate(val playerState: PlayerState) : Action
    data class SpotifyConnected(val remote: SpotifyAppRemote) : Action
    data class SpotifyError(val t: Throwable) : Action

    sealed class Scan : Result() {
        object ScanStarted : Scan()
        object ScanStopped : Scan()
        data class ScanSuccess(val result: Int, val subCode: Int, val button: Flic2Button) : Scan()
        data class ScanFailure(val result: Int, val errorString: String) : Scan()
        object FlicDiscoveredButAlreadyPaired : Scan()
        object FlicDiscovered : Scan()
        object FlicConnected : Scan()
    }

    sealed class Flic : Result() {
        data class ConnectRequest(val button: Flic2Button) : Flic()
        data class Connect(val button: Flic2Button) : Flic()
        data class Disconnected(val button: Flic2Button) : Flic()
        data class Ready(val button: Flic2Button, val timestamp: Long) : Flic()
        data class Unpaired(val button: Flic2Button) : Flic()
    }

    sealed class LocationPermission : Result() {
        object Granted : LocationPermission()
        object Denied : LocationPermission()
    }
}