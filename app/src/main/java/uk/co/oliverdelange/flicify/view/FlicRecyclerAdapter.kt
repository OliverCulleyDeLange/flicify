package uk.co.oliverdelange.flicify.view

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.flic.flic2libandroid.Flic2Button
import io.flic.flic2libandroid.Flic2ButtonListener
import io.flic.flic2libandroid.Flic2Manager
import uk.co.oliverdelange.flicify.R
import uk.co.oliverdelange.flicify.flic.flic2ButtonListener
import java.util.ArrayList

data class ButtonData(val button: Flic2Button, val listener: Flic2ButtonListener? = null) {
    var holder: FlicViewHolder? = null
    var isDown = false
    val shapeColor: Int
        get() = when (button.connectionState) {
            Flic2Button.CONNECTION_STATE_CONNECTING -> Color.RED
            Flic2Button.CONNECTION_STATE_CONNECTED_STARTING -> Color.YELLOW
            Flic2Button.CONNECTION_STATE_CONNECTED_READY -> if (isDown) Color.BLUE else Color.GREEN
            else -> Color.BLACK
        }
}

class FlicViewHolder(var linearLayout: LinearLayout) : RecyclerView.ViewHolder(linearLayout) {
    var buttonData: ButtonData? = null
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

class FlicRecyclerViewAdapter : RecyclerView.Adapter<FlicViewHolder>() {
    var dataSet = ArrayList<ButtonData>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlicViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.view_flic, parent, false) as LinearLayout
        return FlicViewHolder(v)
    }

    override fun onBindViewHolder(holder: FlicViewHolder, position: Int) {
        val buttonData = dataSet[position]
        Log.d("View", "Binding $position: $buttonData")
        holder.buttonData = buttonData
        holder.buttonData!!.holder = holder
        holder.bdaddrTxt.text = buttonData.button.bdAddr
        holder.connectBtn.text = if (buttonData.button.connectionState == Flic2Button.CONNECTION_STATE_DISCONNECTED) "Connect" else "Disconnect"
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
        val buttonData = ButtonData(button, flic2ButtonListener)
        button.addListener(buttonData.listener)
        dataSet.add(buttonData)
        notifyItemInserted(dataSet.size - 1)
    }
}