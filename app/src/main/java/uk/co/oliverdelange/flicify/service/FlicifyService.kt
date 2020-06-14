package uk.co.oliverdelange.flicify.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import io.reactivex.rxkotlin.ofType
import uk.co.oliverdelange.flicify.R
import uk.co.oliverdelange.flicify.redux.AppStore
import uk.co.oliverdelange.flicify.redux.Event
import uk.co.oliverdelange.flicify.redux.Result
import uk.co.oliverdelange.flicify.view.FlicActivity

class FlicifyService : Service() {
    private val NOTIFICATION_CHANNEL_ID = "Notification_Channel_Flicify"
    private val NOTIFICATION_CHANNEL_NAME: CharSequence = "Flicify"

    private val CLIENT_ID = "afa24a972e7040e097a6266c2dafff26"
    private val REDIRECT_URI = "https://flicify.oliverdelange.co.uk/auth"
    private var spotifyAppRemote: SpotifyAppRemote? = null

    override fun onCreate() {
        super.onCreate()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(mChannel)
        }
        startForeground(SERVICE_NOTIFICATION_ID, getNotification(false))

        AppStore.actions.ofType<Event.Flic>().doOnNext {
            Log.w("BG", "Flic event: $it")
            notificationManager.notify(SERVICE_NOTIFICATION_ID, getNotification(it.isDown))
        }.subscribe()

        AppStore.actions.ofType<Result.SpotifyConnected>().doOnNext {
            Log.i("Spotify", "Spotify connected. Subscribing to playerState updates")
            spotifyAppRemote?.playerApi?.subscribeToPlayerState()?.setEventCallback { playerState ->
                AppStore.dispatch(Result.SpotifyPlayerUpdate(playerState))
            }
        }.subscribe()

        connectSpotifyRemote()
    }

    private fun connectSpotifyRemote() {
        Log.i("Spotify", "Connecting to Spotify remote")
        val connectionParams = ConnectionParams.Builder(CLIENT_ID)
            .setRedirectUri(REDIRECT_URI)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(this, connectionParams,
            object : Connector.ConnectionListener {
                override fun onConnected(spotifyAppRemote: SpotifyAppRemote) {
                    this@FlicifyService.spotifyAppRemote = spotifyAppRemote
                    Log.d("Spotify", "Spotify Remote Connected!")
                    AppStore.dispatch(Result.SpotifyConnected(spotifyAppRemote))
                }

                override fun onFailure(throwable: Throwable) {
                    Log.e("Spotify", throwable.message, throwable)
                    AppStore.dispatch(Result.SpotifyError(throwable))
                }
            })
    }

    private fun getNotification(down: Boolean): Notification? =
        NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Flicify")
            .setContentText("Flic -> Spotify")
            .setSmallIcon(if (down) R.drawable.ic_notification_pressed else R.drawable.ic_notification)
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
        super.onDestroy()
        Log.d("Service", "Destroying Flicify service")
        SpotifyAppRemote.disconnect(spotifyAppRemote)
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not implemented")
    }

    class BootUpReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.w("APP", "Device restarted")
            // The Application class's onCreate has already been called at this point, which is what we want
        }
    }

    class UpdateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.w("APP", "App re-installed / updated")
            // The Application class's onCreate has already been called at this point, which is what we want
        }
    }

    companion object {
        private const val SERVICE_NOTIFICATION_ID = 123
    }
}