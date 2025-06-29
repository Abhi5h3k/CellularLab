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
 *
 * @param logs Mutable list of log entries to display.
 * @param onShare Callback invoked when a log file is to be shared.
 * @param onOpen Callback invoked when a log file is to be opened.
 */
class HistoryAdapter(
    private val logs: MutableList<LogEntry>,
    private val onShare: (File) -> Unit,
    private val onOpen: (File) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.LogViewHolder>() {

    //region ViewHolder
    /**
     * ViewHolder for a single log entry item.
     * Holds references to the views for each data item.
     */
    inner class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timestamp: TextView = view.findViewById(R.id.resultTimestamp)
        val ip: TextView = view.findViewById(R.id.resultIp)
        val resultSummary: TextView = view.findViewById(R.id.resultSummary)
        val resultIcon: TextView = view.findViewById(R.id.resultIcon)
        val btnMore: ImageView = view.findViewById(R.id.btnMore)
    }
    //endregion

    //region Data Update
    /**
     * Updates the adapter's data set with new log entries.
     * Clears the current list and adds all new entries, then refreshes the view.
     *
     * @param newEntries List of new LogEntry objects.
     */
    fun updateData(newEntries: List<LogEntry>) {
        logs.clear()
        logs.addAll(newEntries)
        notifyDataSetChanged()
    }
    //endregion

    //region Adapter Lifecycle Methods

    /**
     * Inflates the item layout and creates a ViewHolder.
     *
     * @param parent The parent ViewGroup.
     * @param viewType The type of the new view.
     * @return A new LogViewHolder instance.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log_entry, parent, false)
        return LogViewHolder(view)
    }

    /**
     * Binds data to the ViewHolder at the given position.
     * Sets up click listeners for item and menu actions.
     *
     * @param holder The ViewHolder to bind data to.
     * @param position The position of the item in the data set.
     */
    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val entry = logs[position]

        // Bind log entry data to views
        holder.timestamp.text = entry.timestamp
        holder.ip.text = entry.ip
        holder.resultSummary.text = entry.summaryText
        holder.resultIcon.text = entry.icon

        // Handle item click: open the log file
        holder.itemView.setOnClickListener { onOpen(entry.file) }

        // Handle "more" button click: show popup menu with actions
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
     *
     * @return The size of the logs list.
     */
    override fun getItemCount() = logs.size

    //endregion
}
//endregion

/*
Explanation:
- The adapter manages a list of log entries and binds them to a RecyclerView.
- Each item shows log details and a "more" button for actions (open, share, delete).
- Deletion is confirmed with a dialog and updates the list on success.
- Reflection is used to force icons in the popup menu due to Android limitations.
- Regions and comments are added for clarity and maintainability.
*/