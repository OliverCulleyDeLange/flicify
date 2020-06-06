package uk.co.oliverdelange.flicify.flic

import io.flic.flic2libandroid.Flic2Button
import io.flic.flic2libandroid.Flic2ButtonListener
import uk.co.oliverdelange.flicify.redux.Events
import uk.co.oliverdelange.flicify.redux.store

val flic2ButtonListener = object : Flic2ButtonListener() {
//    val holder: FlicViewHolder?
//        get() {
//            return if (buttonData.holder != null && buttonData.holder!!.buttonData === buttonData) {
//                buttonData.holder
//            } else null
//        }
//
//    private fun updateColor() {
//        val holder = holder
//        if (holder != null) {
//            holder.circle.background.colorFilter = PorterDuffColorFilter(
//                holder.buttonData!!.shapeColor,
//                PorterDuff.Mode.SRC_ATOP
//            )
//        }
//    }

    override fun onButtonUpOrDown(
        button: Flic2Button,
        wasQueued: Boolean,
        lastQueued: Boolean,
        timestamp: Long,
        isUp: Boolean,
        isDown: Boolean
    ) {
        store.dispatch(Events.Button.Down)
    }

    override fun onConnect(button: Flic2Button) {
        store.dispatch(Events.Button.Connect(button))
    }

    override fun onReady(button: Flic2Button, timestamp: Long) {
        store.dispatch(Events.Button.Ready(button, timestamp))
    }

    override fun onDisconnect(button: Flic2Button) {
        store.dispatch(Events.Button.Disconnect(button))
    }

    override fun onUnpaired(button: Flic2Button) {
        store.dispatch(Events.Button.Unpaired(button))
//
//        var index = -1
//        for (i in dataSet.indices) {
//            if (dataSet[i].button === button) {
//                index = i
//                break
//            }
//        }
//        if (index != -1) {
//            dataSet.removeAt(index)
//            notifyItemRemoved(index)
//        }
    }
}