package uk.co.oliverdelange.flicify

import android.app.Application
import android.content.Intent
import android.os.Handler
import android.widget.Toast
import androidx.core.content.ContextCompat
import io.flic.flic2libandroid.Flic2Button
import io.flic.flic2libandroid.Flic2ButtonListener
import io.flic.flic2libandroid.Flic2Manager
import uk.co.oliverdelange.flicify.service.FlicifyService

class Flicify : Application() {
    override fun onCreate() {
        super.onCreate()

        // To prevent the application process from being killed while the app is running in the background, start a Foreground Service
        ContextCompat.startForegroundService(
            applicationContext, Intent(
                applicationContext,
                FlicifyService::class.java
            )
        )

        // Initialize the Flic2 manager to run on the same thread as the current thread (the main thread)
        val manager = Flic2Manager.initAndGetInstance(applicationContext, Handler())

        // Every time the app process starts, connect to all paired buttons and assign a click listener
        for (button in manager.buttons) {
            button.connect()
            listenToButtonWithToast(button)
        }
    }

    private fun listenToButtonWithToast(button: Flic2Button) {
        button.addListener(object : Flic2ButtonListener() {
            override fun onButtonUpOrDown(
                button: Flic2Button,
                wasQueued: Boolean,
                lastQueued: Boolean,
                timestamp: Long,
                isUp: Boolean,
                isDown: Boolean
            ) {
                if (wasQueued && button.readyTimestamp - timestamp > 15000) {
                    // Drop the event if it's more than 15 seconds old
                    return
                }
                if (isDown) {
                    Toast.makeText(
                        applicationContext,
                        "Button $button was pressed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }
}