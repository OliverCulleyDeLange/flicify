package uk.co.oliverdelange.flicify

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import timber.log.Timber

class StarterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        Timber.e("StarterActivity::onCreate")
    }

    override fun onStart() {
        super.onStart()
        Timber.e("StarterActivity::onStart - ${intent?.action}")
        intent?.let {
            when (intent.action) {
                Intent.ACTION_VIEW -> {
                    val deeplink = intent.data
                    Timber.i("Viewing $deeplink")
                    when (deeplink?.path) {
                        "/start" -> {
                            // Get the parameter defined as "exerciseType" and add it to the fragment arguments
                            val exerciseType = deeplink.getQueryParameter("exerciseType").orEmpty()
                            Timber.w("Starting: $exerciseType")
                        }
                        "/stop" -> {
                            Timber.i("Stopping")
                        }
                        else -> {
                            Timber.w("Unexpected path ${deeplink?.path}")
                        }
                    }
                }
                else -> Timber.w("Unexpected intent action ${intent.action}")
            }
        } ?: Timber.w("No intent!")
    }

    override fun onStop() {
        super.onStop()
        Timber.e("StarterActivity::onStop")
    }
}