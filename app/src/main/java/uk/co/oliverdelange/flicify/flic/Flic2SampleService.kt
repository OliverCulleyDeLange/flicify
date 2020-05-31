package uk.co.oliverdelange.flicify.flic

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import uk.co.oliverdelange.flicify.R
import uk.co.oliverdelange.flicify.ui.login.LoginActivity

class Flic2SampleService : Service() {
    private val NOTIFICATION_CHANNEL_ID = "Notification_Channel_Flic2SampleService"
    private val NOTIFICATION_CHANNEL_NAME: CharSequence = "Flic2Sample"
    override fun onCreate() {
        super.onCreate()
        val notificationIntent =
            Intent(this, LoginActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager =
                this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
        val notification = NotificationCompat.Builder(
            this.applicationContext,
            NOTIFICATION_CHANNEL_ID
        )
            .setContentTitle("Flic2Sample")
            .setContentText("Flic2Sample")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
        startForeground(SERVICE_NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not implemented")
    }

    class BootUpReceiver : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            // The Application class's onCreate has already been called at this point, which is what we want
        }
    }

    class UpdateReceiver : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            // The Application class's onCreate has already been called at this point, which is what we want
        }
    }

    companion object {
        private const val SERVICE_NOTIFICATION_ID = 123
    }
}