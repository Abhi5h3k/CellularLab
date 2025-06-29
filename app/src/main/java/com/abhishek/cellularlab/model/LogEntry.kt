package com.abhishek.cellularlab.model

import java.io.File

// region Data Model

/**
 * Represents a single log entry in the application.
 *
 * @property file The file associated with this log entry.
 * @property timestamp The timestamp when the log entry was created (format: String).
 * @property ip The IP address related to the log entry.
 * @property summaryText A brief summary or description of the log entry.
 * @property icon A string representing the icon (could be a resource name or URL).
 */
data class LogEntry(
    val file: File,           // The file where the log is stored or referenced
    val timestamp: String,    // Timestamp of the log entry (e.g., "2024-06-01T12:34:56Z")
    val ip: String,           // IP address associated with the log entry
    val summaryText: String,  // Short summary or description of the log
    val icon: String          // Icon identifier (resource name, path, or URL)
)

// endregion