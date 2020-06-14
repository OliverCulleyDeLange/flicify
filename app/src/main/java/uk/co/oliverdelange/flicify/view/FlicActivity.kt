package uk.co.oliverdelange.flicify.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flic.flic2libandroid.Flic2Manager
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_flic.*
import uk.co.oliverdelange.flicify.R
import uk.co.oliverdelange.flicify.redux.AppStore
import uk.co.oliverdelange.flicify.redux.Event
import uk.co.oliverdelange.flicify.redux.Result

const val REQUEST_LOCATION_PERMISSIONS = 1

class FlicActivity : AppCompatActivity() {

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
            spinner.visibility = visibleIf(it.scanning)
            scanButton.text = if (it.scanning) getString(R.string.cancel) else getString(R.string.scan)
            infoText.text = it.info
        }
        AppStore.actions<Event.CheckPermissions>(this) {
            checkPermissions()
        }
        AppStore.push(this, scanButton.clicks().map { Event.Tap.MainButton })
        AppStore.dispatch(Event.CheckPermissions)
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
//                Toast.makeText(
//                    applicationContext,
//                    "Scanning needs Location permission, which you have rejected",
//                    Toast.LENGTH_SHORT
//                ).show()
            }
        }
    }
}

private fun Button.clicks() = Observable.create<Unit> { emitter ->
    setOnClickListener {
        emitter.onNext(Unit)
    }
}
