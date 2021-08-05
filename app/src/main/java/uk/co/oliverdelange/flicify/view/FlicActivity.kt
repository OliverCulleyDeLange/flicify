package uk.co.oliverdelange.flicify.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flic.flic2libandroid.Flic2Manager
import kotlinx.android.synthetic.main.activity_flic.*
import timber.log.Timber
import uk.co.oliverdelange.flicify.Flicify
import uk.co.oliverdelange.flicify.R
import uk.co.oliverdelange.flicify.redux.AppState
import uk.co.oliverdelange.flicify.redux.AppStore
import uk.co.oliverdelange.flicify.redux.Event
import uk.co.oliverdelange.flicify.redux.Result
import uk.co.oliverdelange.flicify.speech.SpeechEvents

const val REQUEST_LOCATION_PERMISSIONS = 1

open class BaseActivity : AppCompatActivity() {
    lateinit var store: AppStore
    override fun onCreate(savedInstanceState: Bundle?) {
        store = (application as Flicify).store
        super.onCreate(savedInstanceState)
    }
}

class FlicActivity : BaseActivity() {

    private val flic2Manager = Flic2Manager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.v("onCreate FlicActivity")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flic)
        intent?.handle()
    }

    override fun onDestroy() {
        Timber.v("onDestroy FlicActivity")
        super.onDestroy()
        flic2Manager.stopScan()
    }

    override fun onStart() {
        Timber.v("onStart FlicActivity")
        super.onStart()
        uiUpdates()
        permissions()

        store.actions<Result.LocationPermission.Denied>(this) {
            Toast.makeText(
                applicationContext,
                "Scanning needs Location permission, which you have rejected",
                Toast.LENGTH_SHORT
            ).show()
        }

        store.push(this, mainFlicButton.clicks().map { Event.Tap.MainButton })
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Timber.i("New intent: $intent")
        intent?.handle()
    }

    private fun Intent.handle() {
        Timber.i("Handling Intent: $this with extras: ${extras?.keySet()?.map { it to extras?.get(it) }}")
        when (action) {
            Intent.ACTION_VIEW -> {
                when {
                    data != null -> data?.handleDeeplink()
                    hasExtra("name") -> {
                        val exercise = getStringExtra("name")
                        Timber.w("Start $exercise")
                        store.dispatch(SpeechEvents.Speak("Starting $exercise"))
                    }
                    hasExtra("itemListName") && hasExtra("itemListElementName") -> {
                        val listName = getStringExtra("itemListName")
                        val item = getStringExtra("itemListElementName")
                        Timber.i("itemListName: $listName")
                        Timber.i("itemListElementName: $item")
                        if (listName == "liked songs" && item == "this song") {
                            Timber.w("User wants to add this song to their liked songs")
                        } else Timber.w("Can't handle this request")
                    }
                }

            }
            else -> Timber.w("Unexpected intent action ${intent.action}")
        }
    }

    private fun Uri.handleDeeplink() {
        Timber.i("Got deeplink: $this")
        when (path) {
            "/start" -> {
                // Get the parameter defined as "exerciseType" and add it to the fragment arguments
                val exerciseType = getQueryParameter("exerciseType").orEmpty()
                Timber.w("Starting: $exerciseType")
            }
            "/stop" -> Timber.i("Stopping")
            else -> {
                Timber.w("Unexpected deeplink $path")
            }
        }
    }

    private fun permissions() {
        store.actions<Event.CheckPermissions>(this) {
            checkPermissions()
        }
        store.dispatch(Event.CheckPermissions)
    }

    private fun uiUpdates() {
        store.state(this) {
            Timber.v("State changed. Updating UI!")
            spinner.visibility = visibleIf(it.flicConnectionState == AppState.FlicConnectionState.Scanning)
            mainFlicButton.text = when (it.flicConnectionState) {
                is AppState.FlicConnectionState.Connecting -> getString(R.string.disconnect)
                is AppState.FlicConnectionState.Connected -> getString(R.string.disconnect)
                is AppState.FlicConnectionState.Disconnected -> getString(R.string.scan)
                AppState.FlicConnectionState.Scanning -> getString(R.string.cancel)
            }
            flicInfo.text = it.flicInfo
            flicInfo.setTextColor(if (it.flicDown) getColor(R.color.flicPurple) else getColor(R.color.spotifyBlack))
            spotifyInfo.text = it.spotifyInfo
            it.spotifyPlayerState?.let { player ->
                spotifyInfo.text = if (player.isPaused) "Spotify paused"
                else "Playing: ${player.track.name} by ${player.track.artist.name}"
            }

            flicBg.setBackgroundColor(getColor(if (it.flicDown) R.color.spotifyBlack else R.color.flicPurple))
        }
    }

    private fun checkPermissions() {
        val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_LOCATION_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                store.dispatch(Result.LocationPermission.Granted)
            } else {
                store.dispatch(Result.LocationPermission.Denied)
            }
        }
    }
}