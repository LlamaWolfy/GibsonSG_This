package com.galaxyfit3.ctl.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.galaxyfit3.ctl.bluetooth.ConnectionState
import com.galaxyfit3.ctl.bluetooth.SettingsService
import com.galaxyfit3.ctl.databinding.FragmentSettingsBinding
import com.galaxyfit3.ctl.ui.MainActivity
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private var settingsService: SettingsService? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val conn = (activity as? MainActivity)?.bleService?.connectionManager
        if (conn != null) {
            settingsService = SettingsService(conn)
        }

        val vibLabels = arrayOf("Off", "Low", "Medium", "High")

        binding.switchDnd.setOnCheckedChangeListener { _, isChecked ->
            setDnd(isChecked)
        }

        binding.sliderVibration.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val level = value.toInt()
                binding.tvVibrationLabel.text = vibLabels[level]
                setVibration(level)
            }
        }

        binding.sliderBrightness.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val level = value.toInt()
                binding.tvBrightnessLabel.text = "$level"
                setBrightness(level)
            }
        }

        binding.btnSetFace.setOnClickListener {
            val idx = binding.etWatchFace.text?.toString()?.toIntOrNull() ?: return@setOnClickListener
            setWatchFace(idx)
        }

        binding.btnRefreshSettings.setOnClickListener { refreshSettings() }

        refreshSettings()
    }

    private fun refreshSettings() {
        val ss = settingsService ?: return
        val conn = (activity as? MainActivity)?.bleService?.connectionManager
        if (conn?.state?.value != ConnectionState.READY) return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val settings = ss.readSettings()
                binding.switchDnd.isChecked = settings.dndEnabled
                binding.sliderVibration.value = settings.vibrationIntensity.toFloat()
                binding.sliderBrightness.value = settings.screenBrightness.toFloat()
                binding.etWatchFace.setText(settings.watchFaceIndex.toString())
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setDnd(enabled: Boolean) {
        val ss = settingsService ?: return showNotConnected()
        viewLifecycleOwner.lifecycleScope.launch {
            try { ss.setDoNotDisturb(enabled) }
            catch (e: Exception) { showError(e) }
        }
    }

    private fun setVibration(level: Int) {
        val ss = settingsService ?: return showNotConnected()
        viewLifecycleOwner.lifecycleScope.launch {
            try { ss.setVibrationIntensity(level) }
            catch (e: Exception) { showError(e) }
        }
    }

    private fun setBrightness(level: Int) {
        val ss = settingsService ?: return showNotConnected()
        viewLifecycleOwner.lifecycleScope.launch {
            try { ss.setScreenBrightness(level) }
            catch (e: Exception) { showError(e) }
        }
    }

    private fun setWatchFace(index: Int) {
        val ss = settingsService ?: return showNotConnected()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                ss.setWatchFace(index)
                Toast.makeText(requireContext(), "Watch face set to #$index", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { showError(e) }
        }
    }

    private fun showNotConnected() {
        Toast.makeText(requireContext(), "Not connected to Galaxy Fit3", Toast.LENGTH_SHORT).show()
    }

    private fun showError(e: Exception) {
        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
