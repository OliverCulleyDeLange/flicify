package uk.co.oliverdelange.flicify.redux

import android.util.Log
import io.flic.flic2libandroid.Flic2Manager
import org.reduxkotlin.middleware
import uk.co.oliverdelange.flicify.flic.flic2ScanCallback

val loggingMiddleware = middleware<AppState> { store, next, action ->
    Log.v("ACTION", action.toString())
    next(action)
}

val startScan = middleware<AppState> { store, next, action ->
    when (action) {
        is Events.Scan.Start -> {
//            (findViewById<View>(R.id.scanNewButton) as Button).text = "Cancel scan"
//            (findViewById<View>(R.id.scanWizardStatus) as TextView).text =
//                "Press and hold down your Flic2 button until it connects"
            Log.d("SCAN","Starting scan")
            Flic2Manager.getInstance().startScan(flic2ScanCallback())
        }
        is Events.Scan.Stop -> {
            Log.d("SCAN","Stopping scan")
            Flic2Manager.getInstance().stopScan()
//            scanNewButton.text = "Scan new button"
//            scanWizardStatus.text = ""
        }
    }
    next(action)
}