package uk.co.oliverdelange.flicify

import android.app.Application
import android.content.Intent
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import io.flic.flic2libandroid.Flic2Button
import io.flic.flic2libandroid.Flic2ButtonListener
import io.flic.flic2libandroid.Flic2Manager
import uk.co.oliverdelange.flicify.flic.flic2ButtonListener
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
        Flic2Manager.initAndGetInstance(applicationContext, Handler()).apply {
            for (button: Flic2Button in this.buttons){
                Log.d("Flic", "Adding listener to known button: $button")
                button.connect()
                button.addListener(flic2ButtonListener)
            }
        }
    }
}