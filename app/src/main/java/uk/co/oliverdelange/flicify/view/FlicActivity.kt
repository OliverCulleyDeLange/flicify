package uk.co.oliverdelange.flicify.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flic.flic2libandroid.Flic2Manager
import kotlinx.android.synthetic.main.activity_flic.*
import timber.log.Timber
import uk.co.oliverdelange.flicify.R
import uk.co.oliverdelange.flicify.flic.connectFlics
import uk.co.oliverdelange.flicify.redux.AppState
import uk.co.oliverdelange.flicify.redux.AppStore
import uk.co.oliverdelange.flicify.redux.Event
import uk.co.oliverdelange.flicify.redux.Result
import uk.co.oliverdelange.flicify.service.FlicifyService

const val REQUEST_LOCATION_PERMISSIONS = 1


class FlicActivity : AppCompatActivity() {

    private val flic2Manager = Flic2Manager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.v("onCreate FlicActivity")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flic)
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

        AppStore.actions<Result.LocationPermission.Denied>(this) {
            Toast.makeText(
                applicationContext,
                "Scanning needs Location permission, which you have rejected",
                Toast.LENGTH_SHORT
            ).show()
        }

        AppStore.push(this, mainFlicButton.clicks().map { Event.Tap.MainButton })
    }

    private fun permissions() {
        AppStore.actions<Event.CheckPermissions>(this) {
            checkPermissions()
        }
        AppStore.dispatch(Event.CheckPermissions)
    }

    private fun uiUpdates() {
        AppStore.state(this) {
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
                AppStore.dispatch(Result.LocationPermission.Granted)
            } else {
                AppStore.dispatch(Result.LocationPermission.Denied)
            }
        }
    }
}