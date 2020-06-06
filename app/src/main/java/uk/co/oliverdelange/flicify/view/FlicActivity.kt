package uk.co.oliverdelange.flicify.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.flic.flic2libandroid.Flic2Manager
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_flic.*
import uk.co.oliverdelange.flicify.R
import uk.co.oliverdelange.flicify.redux.AppStore
import uk.co.oliverdelange.flicify.redux.Event
import uk.co.oliverdelange.flicify.redux.Result

const val REQUEST_LOCATION_PERMISSIONS = 1

class FlicActivity : AppCompatActivity() {

    private val flicRecyclerViewAdapter = FlicRecyclerViewAdapter()

    private val disposables = mutableListOf<Disposable>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flic)
        val recyclerView = findViewById<RecyclerView>(R.id.flicsView)
        recyclerView.setHasFixedSize(true)
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = flicRecyclerViewAdapter
        for (button in Flic2Manager.getInstance().buttons) {
            flicRecyclerViewAdapter.addButton(button)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // This will make sure button listeners are correctly removed
        flicRecyclerViewAdapter.onDestroy()
        // Stop a scan, if it's running
        Flic2Manager.getInstance().stopScan()
    }

    override fun onStart() {
        super.onStart()
        disposables.add(AppStore.state.subscribe {
            Log.v("View", "Something happened!")
        })
        scanNewButton.setOnClickListener {
            AppStore.dispatch(Event.Scan.Start)
        }
    }

    override fun onStop() {
        super.onStop()
        disposables.forEach { it.dispose() }
    }

    fun checkPermissions() {
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