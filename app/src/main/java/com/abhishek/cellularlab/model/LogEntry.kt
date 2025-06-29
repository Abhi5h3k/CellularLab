package com.abhishek.cellularlab.model

import java.io.File

data class LogEntry(
    val file: File,
    val timestamp: String,
    val ip: String,
    val summaryText: String,
    val icon: String
)
