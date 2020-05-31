package uk.co.oliverdelange.flicify.flic

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import io.flic.lib.FlicBroadcastReceiver
import io.flic.lib.FlicButton
import io.flic.lib.FlicManager
import uk.co.oliverdelange.flicify.R
import java.util.*

fun setFlicV1Credentials() {
    FlicManager.setAppCredentials(
        "10f992e4-a442-4f59-be95-ee629845892f",
        "1a358ca4-5b24-4b37-9107-307eaac91fb5",
        "Flicify"
    )
}

class ExampleBroadcastReceiver : FlicBroadcastReceiver() {
    override fun onRequestAppCredentials(context: Context) {
        setFlicV1Credentials()
    }

    override fun onButtonUpOrDown(
        context: Context,
        button: FlicButton,
        wasQueued: Boolean,
        timeDiff: Int,
        isUp: Boolean,
        isDown: Boolean
    ) {
        if (isDown) {
            val notification: Notification = Notification.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Button was pressed")
                .setContentText("Pressed last time at " + Date())
                .build()
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
                1,
                notification
            )
        }
    }

    override fun onButtonRemoved(context: Context, button: FlicButton) {
        Log.d("yo", "removed")
        Toast.makeText(context, "Button was removed", Toast.LENGTH_SHORT).show()
    }
}
