package uk.co.oliverdelange.flicify.flic

import android.util.Log
import io.flic.flic2libandroid.Flic2Button
import uk.co.oliverdelange.flicify.redux.AppStore
import uk.co.oliverdelange.flicify.redux.Result

fun connectFlics(buttons: List<Flic2Button>) {
    for (button in buttons) {
        val state = ConnectionState.values()[button.connectionState]
        Log.d("Flic", "Button name: ${button.name}, state: $state, macAddress: ${button.bdAddr} firmware: ${button.firmwareVersion}, battery: ${button.lastKnownBatteryLevel.voltage}, uuid: ${button.uuid}, sn: ${button.serialNumber}")
        if (button.connectionState == Flic2Button.CONNECTION_STATE_DISCONNECTED) {
            Log.d("Flic", "Connecting to disconnected known button: $button")
            button.addListener(flic2ButtonListener)
            button.connect()
            AppStore.dispatch(Result.Flic.ConnectRequest(button))
        }
    }
}

enum class ConnectionState {
    Disconnected, Connecting, ConnectedStarting, ConnectedReady
}