package uk.co.oliverdelange.flicify.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import timber.log.Timber
import uk.co.oliverdelange.flicify.Flicify
import uk.co.oliverdelange.flicify.R
import uk.co.oliverdelange.flicify.redux.AppState
import uk.co.oliverdelange.flicify.redux.Event
import uk.co.oliverdelange.flicify.redux.Result
import uk.co.oliverdelange.flicify.view.FlicActivity


class FlicifyService : Service() {
    val store = (application as Flicify).store
    private val vibrateMilliseconds = 200L
    private val NOTIFICATION_CHANNEL_ID = "Notification_Channel_Flicify"
    private val NOTIFICATION_CHANNEL_NAME: CharSequence = "Flicify"

    private val CLIENT_ID = "afa24a972e7040e097a6266c2dafff26"
    private val REDIRECT_URI = "https://flicify.oliverdelange.co.uk/auth"

    private val disposables = CompositeDisposable()

    private val upNotification by lazy { getNotification(false) }
    private val downNotification by lazy { getNotification(true) }

    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun onCreate() {
        Timber.v("onCreate FlicifyService")
        super.onCreate()

        store.actions.ofType<Event.FlicUpOrDown>().doOnNext {
            Timber.w("Flic event: $it")
            notificationManager.notify(SERVICE_NOTIFICATION_ID, if (it.isDown) downNotification else upNotification)
            if (it.isDown) {
                addOrRemoveTrackFromLibrary()
            }
        }.subscribe().addTo(disposables)

        store.actions.ofType<Result.SpotifyConnected>().doOnNext {
            Timber.i("Spotify connected. Subscribing to playerState updates")
            it.remote.playerApi?.subscribeToPlayerState()?.setEventCallback { playerState ->
                store.dispatch(Result.SpotifyPlayerUpdate(playerState))
            }
        }.subscribe().addTo(disposables)

        store.actions.ofType<Result.Track>().doOnNext {
            when (it) {
                is Result.Track.SavedToLibrary -> playSound(R.raw.added)
                is Result.Track.RemovedFromLibrary -> playSound(R.raw.removed)
                is Result.Track.CanNotSaveToLibrary -> playSound(R.raw.error)
            }
        }.subscribe().addTo(disposables)


        store.state.subscribe {
            when (it.flicConnectionState) {
                is AppState.FlicConnectionState.Connected -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val mChannel = NotificationChannel(
                            NOTIFICATION_CHANNEL_ID,
                            NOTIFICATION_CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_LOW
                        )
                        notificationManager.createNotificationChannel(mChannel)
                    }
                    startForeground(SERVICE_NOTIFICATION_ID, upNotification)
                }
                else -> {
                    stopForeground(true)
                }
            }

            Timber.v("State in service : $it")
        }.addTo(disposables)

        connectSpotifyRemote()
    }

    private fun playSound(res: Int) {
        MediaPlayer.create(applicationContext, res).apply {
            setOnCompletionListener {
                Timber.i("Releasing Media Player")
                it.release()
            }
            Timber.i("Playing sound")
            start()
        }

        getSystemService(Context.VIBRATOR_SERVICE).apply {
            Timber.i("Vibrating")
            val v = this as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(vibrateMilliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
            } else v.vibrate(vibrateMilliseconds);
        }
    }

    private fun addOrRemoveTrackFromLibrary() {
        Timber.i("Adding / removing song from library")
        //TODO RXify
        val spotifyRemote = store.currentState().spotifyRemote
        spotifyRemote?.playerApi?.playerState?.setResultCallback { player ->
            val track = player.track
            Timber.d("Got current song URI: ${track.uri}")
            spotifyRemote.userApi?.getLibraryState(track.uri)?.setResultCallback { trackState ->
                when {
                    trackState.isAdded -> {
                        Timber.d("Removing currently playing song")
                        spotifyRemote.userApi?.removeFromLibrary(track.uri)?.setResultCallback {
                            Timber.d("Removed song from library!")
                            store.dispatch(Result.Track.RemovedFromLibrary(track))
                        }
                    }
                    trackState.canAdd -> {
                        Timber.d("Saving currently playing song")
                        spotifyRemote.userApi?.addToLibrary(track.uri)?.setResultCallback {
                            Timber.d("Saved song to library!")
                            store.dispatch(Result.Track.SavedToLibrary(track))
                        }
                    }
                    else -> store.dispatch(Result.Track.CanNotSaveToLibrary(track))
                }
            }
        }
    }

    private fun connectSpotifyRemote() {
        Timber.i("Connecting to Spotify remote")
        val connectionParams = ConnectionParams.Builder(CLIENT_ID)
            .setRedirectUri(REDIRECT_URI)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(this, connectionParams,
            object : Connector.ConnectionListener {
                override fun onConnected(spotifyAppRemote: SpotifyAppRemote) {
                    Timber.d("Spotify Remote Connected!")
                    store.dispatch(Result.SpotifyConnected(spotifyAppRemote))
                }

                override fun onFailure(throwable: Throwable) {
                    Timber.e(throwable.message, throwable)
                    store.dispatch(Result.SpotifyError(throwable))
                }
            })
    }

    private fun getNotification(down: Boolean): Notification? =
        NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Flicify")
            .setContentText("Flic -> Spotify")
            .setSmallIcon(if (down) R.drawable.ic_notification_pressed else R.drawable.ic_notification)
//            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, FlicActivity::class.java),
                    PendingIntent.FLAG_CANCEL_CURRENT
                )
            )
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        Timber.v("onDestroy FlicifyService")
        super.onDestroy()
        Timber.d("Destroying Flicify service")
        SpotifyAppRemote.disconnect(store.currentState().spotifyRemote)
        disposables.dispose()
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not implemented")
    }

    class BootUpReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.w("Device restarted")
            // The Application class's onCreate has already been called at this point, which is what we want
        }
    }

    class UpdateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.w("App re-installed / updated")
            // The Application class's onCreate has already been called at this point, which is what we want
        }
    }

    companion object {
        private const val SERVICE_NOTIFICATION_ID = 123
    }
}

//https://stackoverflow.com/questions/600207/how-to-check-if-a-service-is-running-on-android
fun Context.isServiceRunning(serviceClass: Class<*>): Boolean {
    val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
    for (service in manager!!.getRunningServices(Int.MAX_VALUE)) {
        if (serviceClass.name == service.service.className) {
            return true
        }
    }
    return false
}