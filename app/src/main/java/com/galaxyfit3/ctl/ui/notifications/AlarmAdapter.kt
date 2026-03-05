package com.galaxyfit3.ctl.ui.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.galaxyfit3.ctl.data.Alarm

class AlarmAdapter(
    private val onDelete: (Alarm) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.ViewHolder>() {

    private val alarms = mutableListOf<Alarm>()

    fun submitList(list: List<Alarm>) {
        alarms.clear()
        alarms.addAll(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text = TextView(itemView.context).apply {
            setPadding(0, 8, 0, 8)
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
        }
        init {
            (itemView as ViewGroup).addView(text)
            itemView.setOnLongClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) onDelete(alarms[pos])
                true
            }
        }

        fun bind(alarm: Alarm) {
            val time = String.format("%02d:%02d", alarm.hour, alarm.minute)
            val status = if (alarm.enabled) "ON" else "OFF"
            val days = decodeDays(alarm.repeatDays)
            text.text = "#${alarm.id}  $time  [$status]  $days"
        }

        private fun decodeDays(mask: Int): String {
            val names = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            return names.filterIndexed { i, _ -> (mask shr i) and 1 == 1 }.joinToString()
                .ifEmpty { "Once" }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LayoutInflater.from(parent.context).inflate(
            android.R.layout.simple_list_item_1, parent, false
        )
        return ViewHolder(layout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(alarms[position])
    override fun getItemCount() = alarms.size
}
