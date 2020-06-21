package uk.co.oliverdelange.flicify.view

import android.Manifest
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

const val REQUEST_LOCATION_PERMISSIONS = 1

class FlicActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.v("onCreate FlicActivity")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flic)
    }

    override fun onDestroy() {
        Timber.v("onDestroy FlicActivity")
        super.onDestroy()
        Flic2Manager.getInstance().stopScan()
    }

    override fun onStart() {
        Timber.v("onStart FlicActivity")
        super.onStart()
        AppStore.state(this) {
            Timber.v("State changed. Updating UI!")
            spinner.visibility = visibleIf(it.flicConnectionState == AppState.FlicConnectionState.Scanning)
            mainFlicButton.text = when (it.flicConnectionState) {
                is AppState.FlicConnectionState.Sleeping -> getString(R.string.disconnect)
                is AppState.FlicConnectionState.Connected -> getString(R.string.disconnect)
                AppState.FlicConnectionState.Disconnected -> getString(R.string.scan)
                AppState.FlicConnectionState.Scanning -> getString(R.string.cancel)
            }
            flicInfo.text = it.flicInfo
            spotifyInfo.text = it.spotifyInfo
            it.spotifyPlayerState?.let { player ->
                spotifyInfo.text = if (player.isPaused) "Spotify paused"
                else "${player.track.name} by ${player.track.artist.name}"
            }

            flicBg.setBackgroundColor(getColor(if (it.flicDown) R.color.spotifyBlack else R.color.flicPurple))
        }
        AppStore.actions<Event.CheckPermissions>(this) {
            checkPermissions()
        }

        AppStore.actions<Result.LocationPermission.Denied>(this) {
            Toast.makeText(
                applicationContext,
                "Scanning needs Location permission, which you have rejected",
                Toast.LENGTH_SHORT
            ).show()
        }
        AppStore.push(this, mainFlicButton.clicks().map { Event.Tap.MainButton })
        AppStore.dispatch(Event.CheckPermissions)

        connectFlics(Flic2Manager.getInstance().buttons)
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