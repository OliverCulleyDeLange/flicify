package uk.co.oliverdelange.flicify

import android.app.Application
import android.content.Intent
import android.os.Handler
import androidx.core.content.ContextCompat
import io.flic.flic2libandroid.Flic2Manager
import timber.log.Timber
import uk.co.oliverdelange.flicify.flic.connectFlics
import uk.co.oliverdelange.flicify.redux.AppState
import uk.co.oliverdelange.flicify.redux.AppStore
import uk.co.oliverdelange.flicify.service.FlicifyService
import uk.co.oliverdelange.flicify.service.isServiceRunning

class Flicify : Application() {
    private val flicServiceIntent by lazy { Intent(applicationContext, FlicifyService::class.java) }

    lateinit var store: AppStore

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(CustomDebugTree())
        }

        Timber.v("onCreate Flicify Application")
        store = AppStore(applicationContext)

        // Initialize the Flic2 manager to run on the same thread as the current thread (the main thread)
        val flic2Manager = Flic2Manager.initAndGetInstance(applicationContext, Handler())

        store.state.map { it.flicConnectionState }.distinctUntilChanged().subscribe {
            when (it) {
                is AppState.FlicConnectionState.Connected -> if (!isServiceRunning(FlicifyService::class.java)) {
                    // To prevent the application process from being killed while the app is running in the background, start a Foreground Service
                    Timber.w("Starting foreground flic service from Activity")
                    ContextCompat.startForegroundService(applicationContext, flicServiceIntent)
                }
                is AppState.FlicConnectionState.Disconnected -> if (isServiceRunning(FlicifyService::class.java)) {
                    Timber.w("Stopping flic service from Activity")
                    stopService(flicServiceIntent)
                }
            }
        }

        connectFlics(flic2Manager.buttons)
    }
}

class CustomDebugTree : Timber.DebugTree() {
    override fun createStackElementTag(element: StackTraceElement): String? {
        return ":::FLICIFY:::" + super.createStackElementTag(element)
    }
}