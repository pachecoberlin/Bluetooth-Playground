package de.pacheco.bleplayground

import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.pacheco.bleplayground.databinding.ScannedDeviceInfoBinding

class ScanResultAdapter(private val scanResults: List<ScanResult>, private val onClickListener: ((device: ScanResult) -> Unit)) :
    RecyclerView.Adapter<ScanResultAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ScannedDeviceInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view, onClickListener)
    }

    override fun getItemCount() = scanResults.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = scanResults[position]
        holder.bind(item)
    }

    class ViewHolder(private val scanResultBinding: ScannedDeviceInfoBinding, private val onClickListener: ((device: ScanResult) -> Unit)) :
        RecyclerView.ViewHolder(scanResultBinding.root) {

        fun bind(result: ScanResult) {
            scanResultBinding.deviceName.text = result.device.name ?: "No Device Name"
            scanResultBinding.macAddress.text = result.device.address
            scanResultBinding.signalStrength.text = itemView.context.getString(R.string.signal_strength, result.rssi)
            scanResultBinding.root.setOnClickListener { onClickListener.invoke(result) }
        }
    }
}