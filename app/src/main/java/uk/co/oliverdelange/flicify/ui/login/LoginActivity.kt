package uk.co.oliverdelange.flicify.ui.login

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.flic.flic2libandroid.Flic2Button
import io.flic.flic2libandroid.Flic2Manager
import io.flic.flic2libandroid.Flic2ScanCallback
import io.flic.lib.FlicAppNotInstalledException
import io.flic.lib.FlicBroadcastReceiverFlags
import io.flic.lib.FlicManager
import kotlinx.android.synthetic.main.activity_login.*
import uk.co.oliverdelange.flicify.R
import uk.co.oliverdelange.flicify.flic.setFlicV1Credentials


class LoginActivity : AppCompatActivity() {
    val TAG = "LoginActivity"

    private lateinit var loginViewModel: LoginViewModel


    fun getV1Button() {
        try {
            FlicManager.getInstance(this) { manager ->
                manager.initiateGrabButton(this)
            }
        } catch (err: FlicAppNotInstalledException) {
            Toast.makeText(this, "Flic App is not installed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        FlicManager.getInstance(this) { manager ->
            val button = manager.completeGrabButton(requestCode, resultCode, data)
            if (button != null) {
                button.registerListenForBroadcast(FlicBroadcastReceiverFlags.UP_OR_DOWN or FlicBroadcastReceiverFlags.REMOVED)
                Toast.makeText(this, "Grabbed a button", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Did not grab any button", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
    private fun scanForFlic() {
        Log.d(TAG, "Scanning for flic")
        Flic2Manager.getInstance().startScan(object : Flic2ScanCallback {
            override fun onDiscoveredAlreadyPairedButton(button: Flic2Button?) {
                Log.d(TAG, "Found an already paired button. Try another button")
            }

            override fun onDiscovered(bdAddr: String?) {
                Log.d(TAG, "Found Flic2, now connecting")
            }

            override fun onConnected() {
                Log.d(TAG, "Connected. Now pairing")
            }

            override fun onComplete(result: Int, subCode: Int, button: Flic2Button?) {
                if (result == Flic2ScanCallback.RESULT_SUCCESS) {
                    Log.d(TAG, "Success. Use button now")
                } else {
                    Log.d(TAG, "Failed")
                }
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        if (requestCode == 1) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanForFlic()
            } else {
                Toast.makeText(
                    applicationContext,
                    "Scanning needs Location permission, which you have rejected",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        Flic2Manager.initAndGetInstance(applicationContext, Handler())
        setFlicV1Credentials()


        flicv1.setOnClickListener {
            getV1Button()
        }
        flicv2.setOnClickListener {
            Log.i(TAG, "Flic clicked")
            val permissionCheck =
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Asking for permissions")
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1
                )
            } else {
                scanForFlic()
            }
        }

        loginViewModel = ViewModelProviders.of(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                username.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
        })

        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)
            }
            if (loginResult.success != null) {
                updateUiWithUser(loginResult.success)
            }
            setResult(Activity.RESULT_OK)

            //Complete and destroy login activity once successful
            finish()
        })

        username.afterTextChanged {
            loginViewModel.loginDataChanged(
                username.text.toString(),
                password.text.toString()
            )
        }

        password.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    username.text.toString(),
                    password.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        loginViewModel.login(
                            username.text.toString(),
                            password.text.toString()
                        )
                }
                false
            }

            login.setOnClickListener {
                loading.visibility = View.VISIBLE
                loginViewModel.login(username.text.toString(), password.text.toString())
            }
        }
    }


    private fun updateUiWithUser(model: LoggedInUserView) {
        val welcome = getString(R.string.welcome)
        val displayName = model.displayName
        // TODO : initiate successful logged in experience
        Toast.makeText(
            applicationContext,
            "$welcome $displayName",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}
