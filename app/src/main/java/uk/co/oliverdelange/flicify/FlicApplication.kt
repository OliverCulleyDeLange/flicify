package uk.co.oliverdelange.flicify

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.util.Log
import androidx.core.content.ContextCompat
import io.flic.flic2libandroid.Flic2Button
import io.flic.flic2libandroid.Flic2Manager
import uk.co.oliverdelange.flicify.flic.connectFlics
import uk.co.oliverdelange.flicify.flic.flic2ButtonListener
import uk.co.oliverdelange.flicify.redux.AppStore
import uk.co.oliverdelange.flicify.service.FlicifyService

class Flicify : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.v("View", "onCreate Flicify Application")

        // To prevent the application process from being killed while the app is running in the background, start a Foreground Service
        ContextCompat.startForegroundService(
            applicationContext, Intent(
                applicationContext,
                FlicifyService::class.java
            )
        )

        // Initialize the Flic2 manager to run on the same thread as the current thread (the main thread)
        Flic2Manager.init(applicationContext, Handler())
    }
}