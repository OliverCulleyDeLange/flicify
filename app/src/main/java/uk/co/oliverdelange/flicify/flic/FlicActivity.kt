package uk.co.oliverdelange.flicify.flic

import android.Manifest
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.flic.flic2libandroid.Flic2Button
import io.flic.flic2libandroid.Flic2ButtonListener
import io.flic.flic2libandroid.Flic2Manager
import io.flic.flic2libandroid.Flic2ScanCallback
import uk.co.oliverdelange.flicify.R
import java.util.*

class FlicActivity : AppCompatActivity() {
    private val flicRecyclerViewAdapter =
        FlicRecyclerViewAdapter()
    private var isScanning = false

    internal class FlicRecyclerViewAdapter :
        RecyclerView.Adapter<FlicRecyclerViewAdapter.FlicViewHolder>() {
        internal class ButtonData(var button: Flic2Button) {
            var holder: FlicViewHolder? =
                null
            var isDown = false
            var listener: Flic2ButtonListener? = null
            val shapeColor: Int
                get() = when (button.connectionState) {
                    Flic2Button.CONNECTION_STATE_CONNECTING -> Color.RED
                    Flic2Button.CONNECTION_STATE_CONNECTED_STARTING -> Color.YELLOW
                    Flic2Button.CONNECTION_STATE_CONNECTED_READY -> if (isDown) Color.BLUE else Color.GREEN
                    else -> Color.BLACK
                }

        }

        var dataSet =
            ArrayList<ButtonData>()

        internal class FlicViewHolder(var linearLayout: LinearLayout) :
            RecyclerView.ViewHolder(linearLayout) {
            var buttonData: ButtonData? =
                null
            var bdaddrTxt: TextView
            var connectBtn: Button
            var removeBtn: Button
            var circle: LinearLayout

            init {
                bdaddrTxt = linearLayout.findViewById(R.id.bdaddr)
                connectBtn = linearLayout.findViewById(R.id.button_connect)
                removeBtn = linearLayout.findViewById(R.id.button_remove)
                circle = linearLayout.findViewById(R.id.circle)
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): FlicViewHolder {
            val v =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_flic, parent, false) as LinearLayout
            return FlicViewHolder(v)
        }

        override fun onBindViewHolder(
            holder: FlicViewHolder,
            position: Int
        ) {
            val buttonData =
                dataSet[position]
            holder.buttonData = buttonData
            holder.buttonData!!.holder = holder
            holder.bdaddrTxt.text = buttonData.button.bdAddr
            holder.connectBtn.text =
                if (buttonData.button.connectionState == Flic2Button.CONNECTION_STATE_DISCONNECTED) "Connect" else "Disconnect"
            holder.circle.background.colorFilter = PorterDuffColorFilter(
                holder.buttonData!!.shapeColor,
                PorterDuff.Mode.SRC_ATOP
            )
            holder.connectBtn.setOnClickListener {
                if (holder.buttonData!!.button.connectionState == Flic2Button.CONNECTION_STATE_DISCONNECTED) {
                    holder.buttonData!!.button.connect()
                    holder.connectBtn.text = "Disconnect"
                } else {
                    holder.buttonData!!.button.disconnectOrAbortPendingConnection()
                    holder.connectBtn.text = "Connect"
                }
                holder.circle.background.colorFilter = PorterDuffColorFilter(
                    holder.buttonData!!.shapeColor,
                    PorterDuff.Mode.SRC_ATOP
                )
            }
            holder.removeBtn.setOnClickListener {
                Flic2Manager.getInstance().forgetButton(holder.buttonData!!.button)
            }
        }

        override fun getItemCount(): Int {
            return dataSet.size
        }

        fun onDestroy() {
            for (data in dataSet) {
                data.button.removeListener(data.listener)
            }
        }

        fun addButton(button: Flic2Button) {
            val buttonData =
                ButtonData(button)
            buttonData.listener = object : Flic2ButtonListener() {
                val holder: FlicViewHolder?
                    get() {
                        return if (buttonData.holder != null && buttonData.holder!!.buttonData === buttonData) {
                            buttonData.holder
                        } else null
                    }

                private fun updateColor() {
                    val holder =
                        holder
                    if (holder != null) {
                        holder.circle.background.colorFilter = PorterDuffColorFilter(
                            holder.buttonData!!.shapeColor,
                            PorterDuff.Mode.SRC_ATOP
                        )
                    }
                }

                override fun onButtonUpOrDown(
                    button: Flic2Button,
                    wasQueued: Boolean,
                    lastQueued: Boolean,
                    timestamp: Long,
                    isUp: Boolean,
                    isDown: Boolean
                ) {
                    buttonData.isDown = isDown
                    updateColor()
                }

                override fun onConnect(button: Flic2Button) {
                    updateColor()
                }

                override fun onReady(
                    button: Flic2Button,
                    timestamp: Long
                ) {
                    updateColor()
                }

                override fun onDisconnect(button: Flic2Button) {
                    updateColor()
                }

                override fun onUnpaired(button: Flic2Button) {
                    var index = -1
                    for (i in dataSet.indices) {
                        if (dataSet[i].button === button) {
                            index = i
                            break
                        }
                    }
                    if (index != -1) {
                        dataSet.removeAt(index)
                        notifyItemRemoved(index)
                    }
                }
            }
            button.addListener(buttonData.listener)
            dataSet.add(buttonData)
            notifyItemInserted(dataSet.size - 1)
        }
    }

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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanNewButton(findViewById(R.id.scanNewButton))
            } else {
                Toast.makeText(
                    applicationContext,
                    "Scanning needs Location permission, which you have rejected",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @TargetApi(23)
    fun scanNewButton(v: View?) {
        if (isScanning) {
            Flic2Manager.getInstance().stopScan()
            isScanning = false
            (findViewById<View>(R.id.scanNewButton) as Button).text = "Scan new button"
            (findViewById<View>(R.id.scanWizardStatus) as TextView).text = ""
        } else {
            val permissionCheck = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1
                )
                return
            }
            (findViewById<View>(R.id.scanNewButton) as Button).text = "Cancel scan"
            (findViewById<View>(R.id.scanWizardStatus) as TextView).text =
                "Press and hold down your Flic2 button until it connects"
            isScanning = true
            Flic2Manager.getInstance().startScan(object : Flic2ScanCallback {
                override fun onDiscoveredAlreadyPairedButton(button: Flic2Button) {
                    (findViewById<View>(R.id.scanWizardStatus) as TextView).text =
                        "Found an already paired button. Try another button."
                }

                override fun onDiscovered(bdAddr: String) {
                    (findViewById<View>(R.id.scanWizardStatus) as TextView).text =
                        "Found Flic2, now connecting..."
                }

                override fun onConnected() {
                    (findViewById<View>(R.id.scanWizardStatus) as TextView).text =
                        "Connected. Now pairing..."
                }

                override fun onComplete(
                    result: Int,
                    subCode: Int,
                    button: Flic2Button
                ) {
                    isScanning = false
                    (findViewById<View>(R.id.scanNewButton) as Button).text = "Scan new button"
                    if (result == Flic2ScanCallback.RESULT_SUCCESS) {
                        (findViewById<View>(R.id.scanWizardStatus) as TextView).text =
                            "Scan wizard success!"
                        (application as Flic2SampleApplication).listenToButtonWithToast(button)
                        flicRecyclerViewAdapter.addButton(button)
                    } else {
                        (findViewById<View>(R.id.scanWizardStatus) as TextView).text =
                            "Scan wizard failed with code " + Flic2Manager.errorCodeToString(result)
                    }
                }
            })
        }
    }
}