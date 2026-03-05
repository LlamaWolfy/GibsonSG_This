package com.galaxyfit3.ctl.ui.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.galaxyfit3.ctl.bluetooth.BleScanner
import com.galaxyfit3.ctl.bluetooth.ConnectionState
import com.galaxyfit3.ctl.databinding.FragmentScanBinding
import com.galaxyfit3.ctl.ui.MainActivity
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var scanner: BleScanner
    private lateinit var adapter: DeviceAdapter
    private val devices = mutableMapOf<String, com.galaxyfit3.ctl.bluetooth.ScannedDevice>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scanner = BleScanner(requireContext())

        adapter = DeviceAdapter { device ->
            val conn = (activity as? MainActivity)?.bleService?.connectionManager ?: return@DeviceAdapter
            conn.connect(device.address)
            Toast.makeText(requireContext(), "Connecting to ${device.name}…", Toast.LENGTH_SHORT).show()
        }
        binding.deviceList.layoutManager = LinearLayoutManager(requireContext())
        binding.deviceList.adapter = adapter

        binding.btnScan.setOnClickListener { startScan() }

        // Observe connection state
        val conn = (activity as? MainActivity)?.bleService?.connectionManager
        if (conn != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                conn.state.collect { state ->
                    binding.connectionStatus.text = when (state) {
                        ConnectionState.DISCONNECTED -> "Disconnected"
                        ConnectionState.CONNECTING -> "Connecting…"
                        ConnectionState.CONNECTED -> "Connected"
                        ConnectionState.DISCOVERING_SERVICES -> "Discovering services…"
                        ConnectionState.READY -> "Connected & Ready"
                    }
                }
            }
        }
    }

    private fun startScan() {
        if (!scanner.isBluetoothEnabled) {
            Toast.makeText(requireContext(), "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        devices.clear()
        adapter.submitList(emptyList())
        binding.scanProgress.visibility = View.VISIBLE
        binding.btnScan.isEnabled = false

        scanner.scan()
            .onEach { device ->
                devices[device.address] = device
                adapter.submitList(devices.values.toList())
            }
            .catch { e ->
                Toast.makeText(requireContext(), "Scan error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            .onCompletion {
                binding.scanProgress.visibility = View.GONE
                binding.btnScan.isEnabled = true
                if (devices.isEmpty()) {
                    Toast.makeText(requireContext(), "No Galaxy Fit3 found", Toast.LENGTH_SHORT).show()
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
