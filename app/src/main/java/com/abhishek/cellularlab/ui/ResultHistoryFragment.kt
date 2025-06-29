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
import com.abhishek.cellularlab.R
import com.abhishek.cellularlab.adapter.HistoryAdapter
import com.abhishek.cellularlab.model.LogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ResultHistoryFragment : Fragment() {
    //region Variables

    // Adapter for displaying log history in RecyclerView
    private lateinit var adapter: HistoryAdapter

    //endregion

    //region Fragment Lifecycle

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_result_history, container, false)

        // Load log entries asynchronously and set up RecyclerView
        lifecycleScope.launch(Dispatchers.IO) {
            val logEntries = loadLogEntries()
            withContext(Dispatchers.Main) {
                // Initialize adapter with log entries and action handlers
                adapter = HistoryAdapter(logEntries.toMutableList(), ::shareLogFile, ::openLogFile)
                val recyclerView = view.findViewById<RecyclerView>(R.id.historyRecyclerView)
                recyclerView.layoutManager = LinearLayoutManager(requireContext())
                recyclerView.adapter = adapter
            }
        }

        return view
    }

    //endregion

    //region Log Loading and Parsing

    /**
     * Loads log files from the app's external downloads directory,
     * sorts them by last modified date (descending), and parses details.
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
     * Parses a log file to extract:
     * - Timestamp from filename
     * - IP address from file content
     * - Test success ratio and status icon
     */
    private fun parseLogDetails(file: File): LogEntry {
        val name = file.name
        val timestampRegex = Regex("iPerf3_(\\d{8})_(\\d{4})")

        // Extract timestamp from filename
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

        // Extract total iterations from log content
        val totalIterations = contentLines.find { it.contains("Starting iPerf3 Test") }
            ?.substringAfter("Test")
            ?.substringAfter("/")
            ?.substringBefore("─")
            ?.trim()
            ?.toIntOrNull() ?: 1

        // Count successful test completions
        val successCount = contentLines.count { it.contains("iperf Done.") }

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