package com.galaxyfit3.ctl.ui.scan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.galaxyfit3.ctl.bluetooth.ScannedDevice
import com.galaxyfit3.ctl.databinding.ItemDeviceBinding

class DeviceAdapter(
    private val onConnect: (ScannedDevice) -> Unit
) : ListAdapter<ScannedDevice, DeviceAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ScannedDevice>() {
            override fun areItemsTheSame(a: ScannedDevice, b: ScannedDevice) = a.address == b.address
            override fun areContentsTheSame(a: ScannedDevice, b: ScannedDevice) = a == b
        }
    }

    inner class ViewHolder(private val binding: ItemDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(device: ScannedDevice) {
            binding.deviceName.text = device.name ?: "Unknown"
            binding.deviceAddress.text = device.address
            binding.deviceRssi.text = "RSSI: ${device.rssi} dBm"
            binding.btnConnect.setOnClickListener { onConnect(device) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeviceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
