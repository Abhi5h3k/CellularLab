package com.abhishek.cellularlab.ui

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.abhishek.cellularlab.R
import com.abhishek.cellularlab.adapter.HistoryAdapter
import com.abhishek.cellularlab.model.LogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Fragment to display and manage the history of iPerf3 test result logs.
 * Handles loading, parsing, displaying, sharing, and opening log files.
 */
class ResultHistoryFragment : Fragment() {

    //region Variables

    /** Adapter for displaying log history in RecyclerView */
    private lateinit var adapter: HistoryAdapter

    /** Tracks last known log file count to detect changes */
    private var lastLogFileCount = 0

    /** Tracks last known modification time to detect changes */
    private var lastLogFileModified = 0L

    /** Swipe-to-refresh layout for manual refresh */
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    //endregion

    //region Fragment Lifecycle

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_result_history, container, false)

        // Initialize swipe-to-refresh and set refresh listener
        swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            refreshLogEntries()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        // Refresh logs if there are changes since last load
        if (shouldRefreshLogs()) {
            refreshLogEntries()
        }
    }

    //endregion

    //region Log Change Detection

    /**
     * Checks if the log files have changed (count or last modified).
     * Updates internal state if changes are detected.
     * @return true if logs should be refreshed, false otherwise
     */
    private fun shouldRefreshLogs(): Boolean {
        val dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val logFiles = dir?.listFiles() ?: emptyArray()
        val count = logFiles.size
        val lastModified = logFiles.maxOfOrNull { it.lastModified() } ?: 0L
        val changed = count != lastLogFileCount || lastModified != lastLogFileModified
        if (changed) {
            lastLogFileCount = count
            lastLogFileModified = lastModified
        }
        return changed
    }

    //endregion

    //region Log Loading and Parsing

    /**
     * Loads log files asynchronously, parses them, and updates the RecyclerView.
     * Uses coroutines to avoid blocking the UI thread.
     */
    private fun refreshLogEntries() {
        lifecycleScope.launch(Dispatchers.IO) {
            val logEntries = loadLogEntries()
            withContext(Dispatchers.Main) {
                if (!::adapter.isInitialized) {
                    // Initialize adapter and RecyclerView on first load
                    adapter = HistoryAdapter(
                        logEntries.toMutableList(),
                        ::shareLogFile,
                        ::openLogFile
                    )
                    view?.findViewById<RecyclerView>(R.id.historyRecyclerView)?.apply {
                        layoutManager = LinearLayoutManager(requireContext())
                        adapter = this@ResultHistoryFragment.adapter
                    }
                } else {
                    // Update adapter data on subsequent loads
                    adapter.updateData(logEntries)
                }
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    /**
     * Loads and parses all log files from the app's external downloads directory.
     * @return List of parsed LogEntry objects, sorted by last modified date (descending)
     */
    private fun loadLogEntries(): List<LogEntry> {
        val dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val logFiles = dir?.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()

        return logFiles.map {
            val (file, timestamp, ip, summaryText, icon) = parseLogDetails(it)
            LogEntry(file, timestamp, ip, summaryText, icon)
        }
    }

    /**
     * Parses a single log file to extract:
     * - Timestamp from filename
     * - IP address from file content
     * - Test success ratio and status icon
     * @param file Log file to parse
     * @return LogEntry with extracted details
     */
    private fun parseLogDetails(file: File): LogEntry {
        val name = file.name
        val timestampRegex = Regex("iPerf3_(\\d{8})_(\\d{4})")

        // Extract timestamp from filename (format: iPerf3_YYYYMMDD_HHMM)
        val timestamp = timestampRegex.find(name)?.let {
            val (date, time) = it.destructured
            val formattedDate =
                "${date.substring(0, 4)}-${date.substring(4, 6)}-${date.substring(6)}"
            val formattedTime = "${time.substring(0, 2)}:${time.substring(2)}"
            "$formattedDate $formattedTime"
        } ?: "Unknown"

        val contentLines = file.readLines()

        // Extract IP address from log content
        val ip = contentLines.find { it.contains("Connecting to host") }
            ?.substringAfter("host")
            ?.substringBefore(",")
            ?.trim() ?: "Unknown IP"

        // Extract total iterations from log content (default to 1 if not found)
        val totalIterations = contentLines.find { it.contains("Starting iPerf3 Test") }
            ?.substringAfter("Test")
            ?.substringAfter("/")
            ?.substringBefore("─")
            ?.trim()
            ?.toIntOrNull() ?: 1

        // Count successful test completions
        val successCount = contentLines.count { it.contains("📊 iperf Done.") }

        val ratioText = "$successCount / $totalIterations"

        // Choose emoji based on success percentage
        val icon = when {
            successCount == totalIterations -> "✅"
            successCount >= totalIterations / 2 -> "⚠️"
            else -> "❌"
        }

        return LogEntry(file, timestamp, ip, ratioText, icon)
    }

    //endregion

    //region File Actions

    /**
     * Shares the given log file using Android's share intent.
     * Uses FileProvider for secure file sharing.
     * @param file Log file to share
     */
    private fun shareLogFile(file: File) {
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share Log File"))
    }

    /**
     * Opens the given log file in a compatible viewer app.
     * Uses FileProvider for secure file access.
     * @param file Log file to open
     */
    private fun openLogFile(file: File) {
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/plain")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }

    //endregion
}