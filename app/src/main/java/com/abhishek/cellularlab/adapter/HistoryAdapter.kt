package com.abhishek.cellularlab.adapter

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.abhishek.cellularlab.R
import com.abhishek.cellularlab.model.LogEntry
import java.io.File

class HistoryAdapter(
    private val logs: List<LogEntry>,
    private val onShare: (File) -> Unit,
    private val onOpen: (File) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.LogViewHolder>() {

    inner class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timestamp = view.findViewById<TextView>(R.id.resultTimestamp)
        val ip = view.findViewById<TextView>(R.id.resultIp)
        val resultSummary = view.findViewById<TextView>(R.id.resultSummary)
        val resultIcon = view.findViewById<TextView>(R.id.resultIcon)
        val btnMore = view.findViewById<ImageView>(R.id.btnMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log_entry, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val entry = logs[position]
        holder.timestamp.text = entry.timestamp
        holder.ip.text = entry.ip
        holder.resultSummary.text = entry.summaryText
        holder.resultIcon.text = entry.icon

        holder.itemView.setOnClickListener { onOpen(entry.file) }

        holder.btnMore.setOnClickListener { anchor ->
            // ① Wrap the anchor’s context with your custom popup style
            val themedCtx = ContextThemeWrapper(anchor.context, R.style.CustomPopupMenu)

            // ② Create the PopupMenu with that themed context
            val popup = PopupMenu(themedCtx, anchor)

            // ③ Inflate your menu resource
            popup.menuInflater.inflate(R.menu.history_item_menu, popup.menu)

            // ④ (Optional) Force icons to show via reflection
            try {
                val field = popup.javaClass.getDeclaredField("mPopup").apply { isAccessible = true }
                val menuHelper = field.get(popup)
                val setIcons = menuHelper.javaClass
                    .getMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                setIcons.invoke(menuHelper, true)
            } catch (_: Exception) { /* ignore */
            }

            // ⑤ Handle menu item clicks
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_open -> onOpen(entry.file)
                    R.id.menu_share -> onShare(entry.file)
                }
                true
            }

            // ⑥ Finally, show the menu
            popup.show()
        }
    }

    override fun getItemCount() = logs.size
}
