package com.galaxyfit3.ctl.ui.health

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.galaxyfit3.ctl.bluetooth.ConnectionState
import com.galaxyfit3.ctl.bluetooth.HealthService
import com.galaxyfit3.ctl.databinding.FragmentHealthBinding
import com.galaxyfit3.ctl.ui.MainActivity
import kotlinx.coroutines.launch

class HealthFragment : Fragment() {

    private var _binding: FragmentHealthBinding? = null
    private val binding get() = _binding!!

    private var healthService: HealthService? = null
    private var hrMonitoring = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHealthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val conn = (activity as? MainActivity)?.bleService?.connectionManager
        if (conn != null) {
            healthService = HealthService(conn)
        }

        binding.btnHeartRate.setOnClickListener { toggleHeartRate() }
        binding.btnRefreshHealth.setOnClickListener { refreshAll() }

        // Observe heart rate updates
        healthService?.let { hs ->
            viewLifecycleOwner.lifecycleScope.launch {
                hs.heartRate.collect { reading ->
                    if (reading != null) {
                        binding.tvHeartRate.text = "${reading.bpm} bpm"
                    }
                }
            }
        }
    }

    private fun toggleHeartRate() {
        val hs = healthService ?: return showNotConnected()
        if (hrMonitoring) {
            hs.stopHeartRateMonitor()
            binding.btnHeartRate.text = "Start Monitor"
            hrMonitoring = false
        } else {
            hs.startHeartRateMonitor()
            binding.btnHeartRate.text = "Stop Monitor"
            hrMonitoring = true
        }
    }

    private fun refreshAll() {
        val hs = healthService ?: return showNotConnected()
        val conn = (activity as? MainActivity)?.bleService?.connectionManager
        if (conn?.state?.value != ConnectionState.READY) return showNotConnected()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val info = hs.readDeviceInfo()
                binding.tvDeviceInfo.text = buildString {
                    appendLine("${info.manufacturer} ${info.model}")
                    appendLine("FW: ${info.firmwareRevision}  HW: ${info.hardwareRevision}")
                    appendLine("SW: ${info.softwareRevision}  Battery: ${info.batteryLevel}%")
                }

                val steps = hs.readSteps()
                binding.tvSteps.text = "${steps.steps}"
                binding.tvStepsDetail.text = "${steps.distanceMeters}m | ${steps.caloriesBurned} cal"

                val sleep = hs.readSleep()
                binding.tvSleep.text = buildString {
                    appendLine("Total: ${sleep.totalMinutes / 60}h ${sleep.totalMinutes % 60}m")
                    appendLine("Deep: ${sleep.deepMinutes}m  Light: ${sleep.lightMinutes}m")
                    appendLine("REM: ${sleep.remMinutes}m  Awake: ${sleep.awakeMinutes}m")
                }

                val spo2 = hs.readSpO2()
                binding.tvSpo2.text = "${spo2.percentage}%"

                val stress = hs.readStress()
                binding.tvStress.text = "${stress.level}/100"

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showNotConnected() {
        Toast.makeText(requireContext(), "Not connected to Galaxy Fit3", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        healthService?.stopHeartRateMonitor()
        super.onDestroyView()
        _binding = null
    }
}
