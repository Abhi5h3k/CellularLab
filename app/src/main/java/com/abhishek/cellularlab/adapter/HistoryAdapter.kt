package com.abhishek.cellularlab.adapter

import android.app.AlertDialog
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.abhishek.cellularlab.R
import com.abhishek.cellularlab.model.LogEntry
import java.io.File

class HistoryAdapter(
    private val logs: MutableList<LogEntry>,
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
            val themedCtx = ContextThemeWrapper(anchor.context, R.style.CustomPopupMenu)
            val popup = PopupMenu(themedCtx, anchor)
            popup.menuInflater.inflate(R.menu.history_item_menu, popup.menu)

            // Force show icons
            try {
                val field = popup.javaClass.getDeclaredField("mPopup")
                field.isAccessible = true
                val menu = field.get(popup)
                val method = menu.javaClass.getDeclaredMethod(
                    "setForceShowIcon",
                    Boolean::class.javaPrimitiveType
                )
                method.invoke(menu, true)
            } catch (_: Exception) {
            }

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_open -> onOpen(entry.file)
                    R.id.menu_share -> onShare(entry.file)
                    R.id.menu_delete -> {
                        AlertDialog.Builder(anchor.context)
                            .setTitle("Delete Log")
                            .setMessage("Are you sure you want to delete this log file?")
                            .setPositiveButton("Delete") { _, _ ->
                                val pos = holder.bindingAdapterPosition
                                if (pos != RecyclerView.NO_POSITION) {
                                    val fileToDelete = logs[pos].file
                                    if (fileToDelete.delete()) {
                                        logs.removeAt(pos)
                                        notifyItemRemoved(pos)
                                    } else {
                                        Toast.makeText(
                                            anchor.context,
                                            "Failed to delete file",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
                true
            }

            popup.show()
        }
    }

    override fun getItemCount() = logs.size
}
