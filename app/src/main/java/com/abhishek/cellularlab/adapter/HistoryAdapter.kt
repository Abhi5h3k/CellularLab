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

//region Adapter Definition
/**
 * RecyclerView Adapter for displaying a list of LogEntry items.
 * Handles item clicks, sharing, and deletion.
 */
class HistoryAdapter(
    private val logs: MutableList<LogEntry>,
    private val onShare: (File) -> Unit, // Callback for sharing a log file
    private val onOpen: (File) -> Unit   // Callback for opening a log file
) : RecyclerView.Adapter<HistoryAdapter.LogViewHolder>() {

    //region ViewHolder
    /**
     * ViewHolder for a single log entry item.
     * Holds references to the views for each data item.
     */
    inner class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timestamp = view.findViewById<TextView>(R.id.resultTimestamp)
        val ip = view.findViewById<TextView>(R.id.resultIp)
        val resultSummary = view.findViewById<TextView>(R.id.resultSummary)
        val resultIcon = view.findViewById<TextView>(R.id.resultIcon)
        val btnMore = view.findViewById<ImageView>(R.id.btnMore)
    }
    //endregion

    //region Adapter Lifecycle Methods

    /**
     * Inflates the item layout and creates a ViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log_entry, parent, false)
        return LogViewHolder(view)
    }

    /**
     * Binds data to the ViewHolder at the given position.
     * Sets up click listeners for item and menu actions.
     */
    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val entry = logs[position]

        // Bind log entry data to views
        holder.timestamp.text = entry.timestamp
        holder.ip.text = entry.ip
        holder.resultSummary.text = entry.summaryText
        holder.resultIcon.text = entry.icon

        // Handle item click (open log)
        holder.itemView.setOnClickListener { onOpen(entry.file) }

        // Handle "more" button click (show popup menu)
        holder.btnMore.setOnClickListener { anchor ->
            // Use a themed context for the popup menu
            val themedCtx = ContextThemeWrapper(anchor.context, R.style.CustomPopupMenu)
            val popup = PopupMenu(themedCtx, anchor)
            popup.menuInflater.inflate(R.menu.history_item_menu, popup.menu)

            // Force show icons in the popup menu using reflection (Android quirk)
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
                // Ignore reflection errors
            }

            // Handle popup menu item clicks
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_open -> onOpen(entry.file) // Open log
                    R.id.menu_share -> onShare(entry.file) // Share log
                    R.id.menu_delete -> {
                        // Show confirmation dialog before deleting
                        AlertDialog.Builder(anchor.context)
                            .setTitle("Delete Log")
                            .setMessage("Are you sure you want to delete this log file?")
                            .setPositiveButton("Delete") { _, _ ->
                                val pos = holder.bindingAdapterPosition
                                if (pos != RecyclerView.NO_POSITION) {
                                    val fileToDelete = logs[pos].file
                                    // Attempt to delete the file
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

    /**
     * Returns the total number of log entries.
     */
    override fun getItemCount() = logs.size

    //endregion
}
//endregion