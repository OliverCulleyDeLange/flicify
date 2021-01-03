package uk.co.oliverdelange.flicify.flic

import io.flic.flic2libandroid.Flic2Button
import io.flic.flic2libandroid.Flic2ButtonListener
import uk.co.oliverdelange.flicify.redux.AppStore
import uk.co.oliverdelange.flicify.redux.Event
import uk.co.oliverdelange.flicify.redux.Result

val flic2ButtonListener = object : Flic2ButtonListener() {
    override fun onButtonUpOrDown(
        button: Flic2Button,
        wasQueued: Boolean,
        lastQueued: Boolean,
        timestamp: Long,
        isUp: Boolean,
        isDown: Boolean
    ) {
        AppStore.dispatch(Event.FlicUpOrDown(button, wasQueued, lastQueued, timestamp, isUp, isDown))
    }

    override fun onConnect(button: Flic2Button) {
        AppStore.dispatch(Result.Flic.Connected(button))
    }

    override fun onReady(button: Flic2Button, timestamp: Long) {
        AppStore.dispatch(Result.Flic.Ready(button, timestamp))
    }

    override fun onDisconnect(button: Flic2Button) {
        AppStore.dispatch(Result.Flic.Disconnected(button))
    }

    override fun onUnpaired(button: Flic2Button) {
        AppStore.dispatch(Result.Flic.Unpaired(button))
    }
}