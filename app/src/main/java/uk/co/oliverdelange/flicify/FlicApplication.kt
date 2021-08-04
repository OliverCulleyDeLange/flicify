package uk.co.oliverdelange.flicify

import android.app.Application
import android.content.Intent
import android.os.Handler
import androidx.core.content.ContextCompat
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import io.flic.flic2libandroid.Flic2Manager
import io.reactivex.Observable
import io.reactivex.rxkotlin.ofType
import timber.log.Timber
import uk.co.oliverdelange.flicify.flic.connectFlics
import uk.co.oliverdelange.flicify.redux.AppState
import uk.co.oliverdelange.flicify.redux.AppStore
import uk.co.oliverdelange.flicify.redux.Event
import uk.co.oliverdelange.flicify.redux.Result
import uk.co.oliverdelange.flicify.service.FlicifyService
import uk.co.oliverdelange.flicify.service.isServiceRunning
import uk.co.oliverdelange.flicify.speech.SpeechEvents
import uk.co.oliverdelange.flicify.speech.SpeechResults
import uk.co.oliverdelange.flicify.speech.initSpeech

class Flicify : Application() {
    private val flicServiceIntent by lazy { Intent(applicationContext, FlicifyService::class.java) }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(CustomDebugTree())
        }

        Timber.v("onCreate Flicify Application")

        // Initialize the Flic2 manager to run on the same thread as the current thread (the main thread)
        val flic2Manager = Flic2Manager.initAndGetInstance(applicationContext, Handler())

        AppStore.state.map { it.flicConnectionState }.distinctUntilChanged().subscribe {
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


        initSpeech(applicationContext).switchMap {
            when(it){
                is SpeechResults.Init.TTSInitSuccess -> {
                 Timber.w("TTS INIT'd")
                    val controller = it.speechController
                    AppStore.actions.ofType<SpeechEvents.Speak>().flatMap { speak ->
                        controller.speak(speak.speech)
                    }
                }
                is SpeechResults.Init.TTSInitError -> {
                 Timber.e(it.error, "Couldn't init TTS")
                    Observable.never()
                }
                SpeechResults.Init.SpeechEngineDeinitialised -> TODO()
            }
        }.subscribe()
    }
}

class CustomDebugTree : Timber.DebugTree() {
    override fun createStackElementTag(element: StackTraceElement): String? {
        return ":::FLICIFY:::" + super.createStackElementTag(element)
    }
}