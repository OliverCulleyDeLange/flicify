package uk.co.oliverdelange.flicify.flic

import io.flic.flic2libandroid.Flic2Button
import io.flic.flic2libandroid.Flic2Manager
import io.flic.flic2libandroid.Flic2ScanCallback
import uk.co.oliverdelange.flicify.redux.Results
import uk.co.oliverdelange.flicify.redux.store

fun flic2ScanCallback(): Flic2ScanCallback {
    return object : Flic2ScanCallback {
        override fun onDiscoveredAlreadyPairedButton(button: Flic2Button) {
            store.dispatch(Results.Scan.FlicDiscoveredButAlreadyPaired)
//        scanWizardStatus.text = "Found an already paired button. Try another button."
        }

        override fun onDiscovered(bdAddr: String) {
            store.dispatch(Results.Scan.FlicDiscovered)
//        scanWizardStatus.text = "Found Flic2, now connecting..."
        }

        override fun onConnected() {
            store.dispatch(Results.Scan.FlicConnected)
//        scanWizardStatus.text = "Connected. Now pairing..."
        }

        override fun onComplete(result: Int, subCode: Int, button: Flic2Button?) {

//        (findViewById<View>(R.id.scanNewButton) as Button).text = "Scan new button"
            if (result == Flic2ScanCallback.RESULT_SUCCESS) {
                store.dispatch(Results.Scan.ScanSuccess(result, subCode, button!!))

//            (findViewById<View>(R.id.scanWizardStatus) as TextView).text =
//                "Scan wizard success!"
//            (application as Flicify).listenToButtonWithToast(button!!)
//            flicRecyclerViewAdapter.addButton(button)
            } else {
                store.dispatch(Results.Scan.ScanFailure(result, Flic2Manager.errorCodeToString(result)))
//            (findViewById<View>(R.id.scanWizardStatus) as TextView).text =
//                "Scan wizard failed with code " + Flic2Manager.errorCodeToString(result)
            }
        }
    }
}