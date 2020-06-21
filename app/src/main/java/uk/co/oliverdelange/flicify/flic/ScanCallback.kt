package uk.co.oliverdelange.flicify.flic

import io.flic.flic2libandroid.Flic2Button
import io.flic.flic2libandroid.Flic2Manager
import io.flic.flic2libandroid.Flic2ScanCallback
import timber.log.Timber
import uk.co.oliverdelange.flicify.redux.AppStore
import uk.co.oliverdelange.flicify.redux.Result

fun flic2ScanCallback(): Flic2ScanCallback {
    return object : Flic2ScanCallback {
        override fun onDiscoveredAlreadyPairedButton(button: Flic2Button) {
            AppStore.dispatch(Result.Scan.FlicDiscoveredButAlreadyPaired)
        }

        override fun onDiscovered(bdAddr: String) {
            AppStore.dispatch(Result.Scan.FlicDiscovered)
        }

        override fun onConnected() {
            AppStore.dispatch(Result.Scan.FlicConnected)
        }

        override fun onComplete(result: Int, subCode: Int, button: Flic2Button?) {
            if (result == Flic2ScanCallback.RESULT_SUCCESS) {
               if (button != null){
                    button.addListener(flic2ButtonListener)
                    AppStore.dispatch(Result.Scan.ScanSuccess(result, subCode, button))
                }else {
                   Timber.e("Successful scan completion, but no button!")
               }
            } else {
                AppStore.dispatch(Result.Scan.ScanFailure(result, Flic2Manager.errorCodeToString(result)))
            }
        }
    }
}