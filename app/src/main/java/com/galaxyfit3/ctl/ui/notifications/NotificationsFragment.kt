package com.galaxyfit3.ctl.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.galaxyfit3.ctl.bluetooth.ConnectionState
import com.galaxyfit3.ctl.bluetooth.NotificationService
import com.galaxyfit3.ctl.data.Alarm
import com.galaxyfit3.ctl.data.Notification
import com.galaxyfit3.ctl.databinding.FragmentNotificationsBinding
import com.galaxyfit3.ctl.ui.MainActivity
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private var notifService: NotificationService? = null
    private lateinit var alarmAdapter: AlarmAdapter
    private var nextAlarmId = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val conn = (activity as? MainActivity)?.bleService?.connectionManager
        if (conn != null) {
            notifService = NotificationService(conn)
        }

        alarmAdapter = AlarmAdapter { alarm ->
            deleteAlarm(alarm)
        }
        binding.alarmList.layoutManager = LinearLayoutManager(requireContext())
        binding.alarmList.adapter = alarmAdapter

        binding.btnSendNotif.setOnClickListener { sendNotification() }
        binding.btnAddAlarm.setOnClickListener { addAlarm() }

        loadAlarms()
    }

    private fun sendNotification() {
        val ns = notifService ?: return showNotConnected()
        val title = binding.etNotifTitle.text?.toString()?.trim() ?: ""
        val body = binding.etNotifBody.text?.toString()?.trim() ?: ""
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Title required", Toast.LENGTH_SHORT).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                ns.sendNotification(
                    Notification(
                        appPackage = requireContext().packageName,
                        title = title,
                        body = body
                    )
                )
                Toast.makeText(requireContext(), "Notification sent", Toast.LENGTH_SHORT).show()
                binding.etNotifTitle.text?.clear()
                binding.etNotifBody.text?.clear()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addAlarm() {
        val ns = notifService ?: return showNotConnected()
        val hour = binding.etAlarmHour.text?.toString()?.toIntOrNull() ?: return
        val minute = binding.etAlarmMinute.text?.toString()?.toIntOrNull() ?: return

        if (hour !in 0..23 || minute !in 0..59) {
            Toast.makeText(requireContext(), "Invalid time", Toast.LENGTH_SHORT).show()
            return
        }

        val alarm = Alarm(id = nextAlarmId++, hour = hour, minute = minute, enabled = true, repeatDays = 0)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                ns.setAlarm(alarm)
                loadAlarms()
                binding.etAlarmHour.text?.clear()
                binding.etAlarmMinute.text?.clear()
                Toast.makeText(requireContext(), "Alarm set", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteAlarm(alarm: Alarm) {
        val ns = notifService ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                ns.deleteAlarm(alarm.id)
                loadAlarms()
                Toast.makeText(requireContext(), "Alarm deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadAlarms() {
        val ns = notifService ?: return
        val conn = (activity as? MainActivity)?.bleService?.connectionManager
        if (conn?.state?.value != ConnectionState.READY) return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val alarms = ns.getAlarms()
                alarmAdapter.submitList(alarms)
                if (alarms.isNotEmpty()) {
                    nextAlarmId = alarms.maxOf { it.id } + 1
                }
            } catch (_: Exception) { }
        }
    }

    private fun showNotConnected() {
        Toast.makeText(requireContext(), "Not connected to Galaxy Fit3", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
