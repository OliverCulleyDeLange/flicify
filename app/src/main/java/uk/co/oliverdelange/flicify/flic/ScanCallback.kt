package uk.co.oliverdelange.flicify.flic

import io.flic.flic2libandroid.Flic2Button
import io.flic.flic2libandroid.Flic2Manager
import io.flic.flic2libandroid.Flic2ScanCallback
import uk.co.oliverdelange.flicify.redux.Result
import uk.co.oliverdelange.flicify.redux.AppStore

fun flic2ScanCallback(): Flic2ScanCallback {
    return object : Flic2ScanCallback {
        override fun onDiscoveredAlreadyPairedButton(button: Flic2Button) {
            AppStore.dispatch(Result.Scan.FlicDiscoveredButAlreadyPaired)
//        scanWizardStatus.text = "Found an already paired button. Try another button."
        }

        override fun onDiscovered(bdAddr: String) {
            AppStore.dispatch(Result.Scan.FlicDiscovered)
//        scanWizardStatus.text = "Found Flic2, now connecting..."
        }

        override fun onConnected() {
            AppStore.dispatch(Result.Scan.FlicConnected)
//        scanWizardStatus.text = "Connected. Now pairing..."
        }

        override fun onComplete(result: Int, subCode: Int, button: Flic2Button?) {

//        (findViewById<View>(R.id.scanNewButton) as Button).text = "Scan new button"
            if (result == Flic2ScanCallback.RESULT_SUCCESS) {
                AppStore.dispatch(Result.Scan.ScanSuccess(result, subCode, button!!))

//            (findViewById<View>(R.id.scanWizardStatus) as TextView).text =
//                "Scan wizard success!"
//            (application as Flicify).listenToButtonWithToast(button!!)
//            flicRecyclerViewAdapter.addButton(button)
            } else {
                AppStore.dispatch(Result.Scan.ScanFailure(result, Flic2Manager.errorCodeToString(result)))
//            (findViewById<View>(R.id.scanWizardStatus) as TextView).text =
//                "Scan wizard failed with code " + Flic2Manager.errorCodeToString(result)
            }
        }
    }
}