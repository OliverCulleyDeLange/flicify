package uk.co.oliverdelange.flicify.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import io.reactivex.rxkotlin.ofType
import uk.co.oliverdelange.flicify.R
import uk.co.oliverdelange.flicify.redux.AppStore
import uk.co.oliverdelange.flicify.redux.Event
import uk.co.oliverdelange.flicify.view.FlicActivity

class FlicifyService : Service() {
    private val NOTIFICATION_CHANNEL_ID = "Notification_Channel_Flicify"
    private val NOTIFICATION_CHANNEL_NAME: CharSequence = "Flicify"

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