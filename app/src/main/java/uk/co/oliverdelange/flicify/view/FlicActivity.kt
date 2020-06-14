package uk.co.oliverdelange.flicify.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import io.flic.flic2libandroid.Flic2Manager
import kotlinx.android.synthetic.main.activity_flic.*
import uk.co.oliverdelange.flicify.R
import uk.co.oliverdelange.flicify.redux.AppState
import uk.co.oliverdelange.flicify.redux.AppStore
import uk.co.oliverdelange.flicify.redux.Event
import uk.co.oliverdelange.flicify.redux.Result


const val REQUEST_LOCATION_PERMISSIONS = 1

class FlicActivity : AppCompatActivity() {
    private val CLIENT_ID = "afa24a972e7040e097a6266c2dafff26"
    private val REDIRECT_URI = "https://flicify.oliverdelange.co.uk/auth"
    private var mSpotifyAppRemote: SpotifyAppRemote? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flic)
    }

    override fun onDestroy() {
        super.onDestroy()
        Flic2Manager.getInstance().stopScan()
    }

    override fun onStart() {
        super.onStart()
        AppStore.state(this) {
            Log.v("View", "State changed. Updating UI!")
            spinner.visibility = visibleIf(it.connectionState == AppState.ConnectionState.Scanning)
            mainButton.text = when (it.connectionState) {
                is AppState.ConnectionState.Connected -> getString(R.string.disconnect)
                AppState.ConnectionState.Disconnected -> getString(R.string.scan)
                AppState.ConnectionState.Scanning -> getString(R.string.cancel)
            }
            flicInfo.text = it.flicInfo
            spotifyInfo.text = it.spotifyInfo
        }
        AppStore.actions<Event.CheckPermissions>(this) {
            checkPermissions()
        }
        AppStore.actions<Result.SpotifyConnected>(this) {
            Log.i("Spotify", "Playing random playlist")
            mSpotifyAppRemote?.playerApi?.subscribeToPlayerState()?.setEventCallback { playerState ->
                AppStore.dispatch(Result.SpotifyPlayerUpdate(playerState))
            }
        }
        AppStore.actions<Result.LocationPermission.Denied>(this) {
            Toast.makeText(
                applicationContext,
                "Scanning needs Location permission, which you have rejected",
                Toast.LENGTH_SHORT
            ).show()
        }
        AppStore.push(this, mainButton.clicks().map { Event.Tap.MainButton })
        AppStore.dispatch(Event.CheckPermissions)

        val connectionParams = ConnectionParams.Builder(CLIENT_ID)
            .setRedirectUri(REDIRECT_URI)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(this, connectionParams,
            object : Connector.ConnectionListener {
                override fun onConnected(spotifyAppRemote: SpotifyAppRemote) {
                    mSpotifyAppRemote = spotifyAppRemote
                    Log.d("Spotify", "Spotify Remote Connected!")
                    AppStore.dispatch(Result.SpotifyConnected(spotifyAppRemote))
                }

                override fun onFailure(throwable: Throwable) {
                    Log.e("Spotify", throwable.message, throwable)
                    AppStore.dispatch(Result.SpotifyError(throwable))
                }
            })
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