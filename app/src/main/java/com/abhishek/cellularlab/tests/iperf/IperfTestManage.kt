package com.abhishek.cellularlab.tests.iperf

import android.content.Context
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.abhishek.cellularlab.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class IperfTestManage(
    private val context: Context,
    private val startBtn: Button,
    private val outputView: TextView,
    private val scrollView: ScrollView,
    private val isAutoScrollEnabled: () -> Boolean,
    private val timestamp: String,
    private val runIperfLive: (Array<String>, IperfCallback) -> Unit,
    private val forceStopIperfTest: (IperfCallback) -> Unit,
    private val onTestComplete: () -> Unit,
    private val startTimer: () -> Unit,
    private val stopTimer: () -> Unit,
    private val isAutoReduceEnabled: () -> Boolean

) {
    // region Constants & Regex
    private val MAX_LOG_SIZE = 5 * 1024 * 1024 // 5 MB
    private val throughputRegex = Regex("""\s+(\d+(?:\.\d+)?)\s+(K|M|G)?bits/sec""")
    // endregion

    // region State
    private val createdLogFiles = mutableListOf<File>()
    private val packetLossHistory = mutableListOf<Float>()
    private var watchdogJob: Job? = null
    private var iperfJob: Job? = null
    private var lastIterationHadError = false
    private var wasAutoReducedOnPacketLoss = false
    private val mainScope = CoroutineScope(Dispatchers.Main)
    // endregion


    // region Test Entry Point
    fun startTest(
        args: Array<String>,
        testIterations: Int,
        waitTime: Int,
        isIncrementalRampUpTest: Boolean,
        isHybridTest: Boolean,
        isSmartIncrementalRampUpTest: Boolean
    ) {
        var currentArgs = args.copyOf()

        // region Timing & Config
        val testDurationMs = getTestDurationMillis(args) // includes buffer
        val timeoutMargin = 60_000L // 60 seconds grace
        val totalTimeout = testDurationMs + timeoutMargin
        val errorBackoffMs = (testDurationMs * 0.33).toLong().coerceAtLeast(20_000L)
        val waitTimeMillis = waitTime * 1000
        // endregion

        // region Bandwidth & Loss Config
        var originalBandwidth = extractBandwidthMbps(args) ?: 0
        var currentStepBandwidth = 50
        val lossThreshold = 75.0f
        val requiredHighLossCount = 2
        val historyWindowSize = 3
        // endregion

        var commandStr = args.joinToString(" ")

        val handler = CoroutineExceptionHandler { _, exception ->
            append("🚨 Uncaught Exception: ${exception.localizedMessage}")
            exception.printStackTrace()
        }

        iperfJob = CoroutineScope(Dispatchers.IO + handler).launch {

            startTimer() // ⏱ Start timer

            // region Hybrid: Run TCP bidirectional first to estimate bandwidth
            if (isHybridTest) {
                append("\n🔄 Running TCP bidirectional test to estimate max bandwidth...")
                val estimatedBandwidth = runTcpBidirAndGetBandwidth(currentArgs)
                if (estimatedBandwidth > 0) {
                    append("📶 Estimated bandwidth from TCP: ${estimatedBandwidth} Mbps")
                    currentArgs = updateBandwidth(currentArgs, estimatedBandwidth)
                    currentArgs = currentArgs.toMutableList().apply {
                        if (!contains("-u")) add("-u")
                    }.toTypedArray()
                    commandStr = currentArgs.joinToString(" ")
                } else {
                    append("⚠️ Failed to estimate bandwidth. Using original settings.")
                }
            }
            // endregion

            // region Main Test Loop
            repeat(testIterations) { iteration ->
                val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                append("\n\n🕒 [$currentTime] ──🚀 Starting iPerf3 Test ${iteration + 1}/$testIterations ──\n$commandStr")

                val testCompleted = CompletableDeferred<Unit>()
                var maxAchievedThisRun = 0

                // region Incremental Ramp-Up Logic
                if (isIncrementalRampUpTest) {
                    val targetBandwidth = extractBandwidthMbps(currentArgs)
                    if (targetBandwidth != null && originalBandwidth > 100) {
                        val stepSize = 50
                        val rampBandwidth =
                            ((iteration + 1) * stepSize).coerceAtMost(originalBandwidth)
                        currentArgs = updateBandwidth(currentArgs, rampBandwidth)
                        append("📈 Ramp-up bandwidth set to ${rampBandwidth}M")
                    } else {
                        append("ℹ️ Bandwidth too low for ramp-up. Skipping ramp logic.")
                    }
                }
                // endregion

                // region Smart Ramp-Up Logic
                if (isSmartIncrementalRampUpTest) {
                    currentArgs = updateBandwidth(currentArgs, currentStepBandwidth)
                    append("\n\n📶 Set bandwidth: ${currentStepBandwidth}M")
                    commandStr = currentArgs.joinToString(" ")
                    append("\n[New Command :] $commandStr")
                }
                // endregion

                // region Watchdog Launch
                watchdogJob = launch(Dispatchers.IO + handler) {
                    append("\n\n🐶 Watchdog launched – I'll jump in if things hang! 🚨")
                    delay(totalTimeout)
                    if (!testCompleted.isCompleted) {
                        append("⏰ Watchdog: Forcing test to abort after timeout.")
                        forceStopIperfTest(createIperfCallback(onLine = { line ->
                            append("📊 $line")
                            if (isSmartIncrementalRampUpTest) {
                                parseThroughputMbps(line)?.let {
                                    if (it > maxAchievedThisRun) maxAchievedThisRun = it
                                }
                            }
                        }, onError = {
                            append("\n❌ Error: $it")
                            lastIterationHadError = true
                            testCompleted.complete(Unit)
                        }, onComplete = {
                            append("\n🏁 [End] Iteration ${iteration + 1}\n")
                            testCompleted.complete(Unit)
                        }))
                        delay(3000) // allow JNI to shut down
                    }
                }
                // endregion

                // region Start Actual iPerf Test
                val runJob = launch(Dispatchers.IO) {
                    runIperfLive(currentArgs, createIperfCallback(onLine = { line ->
                        append("📊 $line")

                        parsePacketLoss(line)?.let { loss ->
                            if (packetLossHistory.size >= historyWindowSize) {
                                packetLossHistory.removeAt(0)
                            }
                            packetLossHistory.add(loss)
                        }

                        if (isSmartIncrementalRampUpTest) {
                            parseThroughputMbps(line)?.let {
                                if (it > maxAchievedThisRun) maxAchievedThisRun = it
                            }
                        }
                    }, onError = {
                        append("\n❌ Error: $it")
                        lastIterationHadError = true
                        testCompleted.complete(Unit)
                    }, onComplete = {
                        append("\n\n🏁 [End] Iteration ${iteration + 1}")
                        testCompleted.complete(Unit)
                    }))
                }
                // endregion

                // region Wait for Completion / Timeout
                try {
                    withTimeout(totalTimeout) {
                        testCompleted.await()
                    }
                } catch (e: TimeoutCancellationException) {
                    append("⏰ Timeout: iPerf did not respond for iteration ${iteration + 1}")
                }
                // endregion

                // region Auto Reduce Bandwidth if High Loss
                if (isAutoReduceEnabled() && packetLossHistory.size == historyWindowSize && packetLossHistory.count { it > lossThreshold } >= requiredHighLossCount) {
                    withContext(Dispatchers.Main) {
                        showReduceBandwidthDialog { reduce ->
                            if (reduce) {
                                currentStepBandwidth =
                                    (currentStepBandwidth * 0.8).toInt().coerceAtLeast(10)
                                append("📉 High packet loss detected. Reduced bandwidth to ${currentStepBandwidth}M.")
                                currentArgs = updateBandwidth(currentArgs, currentStepBandwidth)
                                wasAutoReducedOnPacketLoss = true
                            } else {
                                append("⚠️ High packet loss ignored. Keeping current bandwidth.")
                            }
                        }
                    }
                }
                // endregion

                // region Cleanup Run
                val cancelResult = withTimeoutOrNull(3000) {
                    runJob.cancelAndJoin()
                }
                if (cancelResult == null) {
                    append("⚠️ Timeout: JNI job did not cancel in time.")
                }
                // endregion

                // region Smart Ramp-Up Bandwidth Adjustment
                if ((isSmartIncrementalRampUpTest || wasAutoReducedOnPacketLoss) && !lastIterationHadError) {
                    currentStepBandwidth = evaluateBandwidthRampUp(
                        currentStepBandwidth, iteration, maxAchievedThisRun, originalBandwidth
                    )
                }
                // endregion

                // region Delay Between Iterations or End Summary
                if (iteration < testIterations - 1) {
                    if (lastIterationHadError) {
                        append("\n⏳ Error occurred. Waiting ${errorBackoffMs / 1000} seconds before next test...")
                        delay(errorBackoffMs)
                        lastIterationHadError = false
                    }
                    append("⏳ Waiting ${waitTime} seconds before next test...")
                    delay(waitTimeMillis.toLong())
                } else {
                    append("🏁 [End] All iterations completed.")
                    if (createdLogFiles.isNotEmpty()) {
                        append("\n📁 Logs saved in app-specific Downloads folder:")
                        createdLogFiles.forEachIndexed { index, file ->
                            append("   🔹 Part ${index + 1}: ${file.name}")
                        }
                    } else {
                        append("\n⚠️ No log files were created.")
                    }
                    stopTimer()
                    startBtn.text = "New Test"
                    withContext(Dispatchers.Main) {
                        onTestComplete()
                    }
                }
                // endregion
            }
            // end repeat
            // endregion
        }
    }
// endregion


    // region Test Lifecycle Control

    fun stopTests() {
        iperfJob?.cancel()
        watchdogJob?.cancel() // ✅ Cancel watchdog if active
        watchdogJob = null // Safe to reset
        stopTimer()
        onTestComplete()

        append("\n⛔ Stop requested. Test will terminate after the current iteration.\n")
    }

// endregion

// region Bandwidth Utilities

    private fun extractBandwidthMbps(args: Array<String>): Int? {
        val bIndex = args.indexOf("-b")
        return if (bIndex != -1 && bIndex + 1 < args.size) {
            args[bIndex + 1].removeSuffix("M").toIntOrNull()
        } else null
    }

    private fun updateBandwidth(args: Array<String>, newBandwidth: Int): Array<String> {
        val bIndex = args.indexOf("-b")
        return if (bIndex != -1 && bIndex + 1 < args.size) {
            args.toMutableList().apply {
                this[bIndex + 1] = "${newBandwidth}M"
            }.toTypedArray()
        } else {
            args
        }
    }

    private fun evaluateBandwidthRampUp(
        currentStepBandwidth: Int, iteration: Int, maxAchieved: Int, originalBw: Int
    ): Int {
        if (maxAchieved >= (currentStepBandwidth * 0.9) && currentStepBandwidth < originalBw) {
            append("\n\n📈 Smart Ramp-up: Achieved ${maxAchieved}M, increasing bandwidth.")
            return (currentStepBandwidth + ((iteration + 1) * 50)).coerceAtMost(originalBw)
        } else {
            append("⏸ Holding bandwidth at ${currentStepBandwidth}M.")
            return currentStepBandwidth
        }
    }

// endregion

// region TCP Bidirectional Estimate

    private suspend fun runTcpBidirAndGetBandwidth(args: Array<String>): Int {
        val tcpArgs = args.toMutableList().apply {
            removeAll(listOf("-u", "-b")) // Ensure TCP mode
            if (!contains("--bidir")) add("--bidir")
        }

        var maxBandwidthMbps = 0
        val completed = CompletableDeferred<Unit>()

        runIperfLive(tcpArgs.toTypedArray(), createIperfCallback(onLine = { line ->
            parseThroughputMbps(line)?.let {
                if (it > maxBandwidthMbps) maxBandwidthMbps = it
            }
        }, onError = {
            append("❌ TCP error: $it")
            completed.complete(Unit)
        }, onComplete = {
            append("✅ TCP bidir test complete.")
            completed.complete(Unit)
        }))

        val tcpTimeout = getTestDurationMillis(args, bufferSeconds = 10)
        withTimeoutOrNull(tcpTimeout) { completed.await() }

        return maxBandwidthMbps
    }

// endregion

// region iPerf Helpers

    fun getTestDurationMillis(args: Array<String>, bufferSeconds: Int = 5): Long {
        val tIndex = args.indexOf("-t")
        val testSeconds = if (tIndex != -1 && tIndex + 1 < args.size) {
            args[tIndex + 1].toIntOrNull() ?: 10
        } else {
            10
        }
        return (testSeconds + bufferSeconds) * 1000L
    }

    private fun createIperfCallback(
        onLine: (String) -> Unit = {}, onError: (String) -> Unit = {}, onComplete: () -> Unit = {}
    ): IperfCallback {
        return object : IperfCallback {
            override fun onOutput(line: String) = onLine(line)
            override fun onError(error: String) = onError(error)
            override fun onComplete() = onComplete()
        }
    }

// endregion

// region Output + Logging

    private fun append(text: String) {
        mainScope.launch {
            outputView.append("$text\n")
            if (isAutoScrollEnabled()) {
                scrollView.post {
                    scrollView.fullScroll(View.FOCUS_DOWN)
                }
            }

            try {
                val file = getWritableLogFile()
                file.appendText("$text\n")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getWritableLogFile(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        var part = 1
        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName

        lateinit var logFile: File
        do {
            val fileName = "iPerf3_${timestamp}_v${versionName}_$part.txt"
            logFile = File(dir, fileName)
            if (!logFile.exists() || logFile.length() < MAX_LOG_SIZE) break
            part++
        } while (true)

        if (!logFile.exists()) {
            logFile.createNewFile()
            createdLogFiles.add(logFile)
        } else if (!createdLogFiles.contains(logFile)) {
            createdLogFiles.add(logFile)
        }

        return logFile
    }

// endregion

// region Parsing Logic

    private fun parseThroughputMbps(line: String): Int? {
        val match = throughputRegex.find(line)
        val value = match?.groups?.get(1)?.value?.toFloatOrNull()
        val unit = match?.groups?.get(2)?.value

        val throughputMbps = when (unit) {
            "K" -> value?.div(1000)
            "M" -> value
            "G" -> value?.times(1000)
            else -> value?.div(1_000_000) // assume bits/sec if no unit
        }

        return throughputMbps?.toInt()
    }

    private fun parsePacketLoss(line: String): Float? {
        val lossRegex = Regex("""\((\d+(?:\.\d+)?)%\)""")
        return lossRegex.find(line)?.groups?.get(1)?.value?.toFloatOrNull()
    }

// endregion

// region UI Dialog

    private fun showReduceBandwidthDialog(onDecision: (Boolean) -> Unit) {
        val dialogView = View.inflate(context, R.layout.dialog_reduce_bandwidth, null)
        val countdownText = dialogView.findViewById<TextView>(R.id.reduceCountdownText)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.reduceProgressBar)

        val builder = AlertDialog.Builder(context).setView(dialogView)
            .setPositiveButton("Yes") { _, _ -> onDecision(true) }
            .setNegativeButton("No") { _, _ -> onDecision(false) }

        val dialog = builder.create()
        dialog.show()

        // Auto-confirm after countdown
        val countdownSeconds = 15
        var remaining = countdownSeconds
        val countdownJob = CoroutineScope(Dispatchers.Main).launch {
            while (remaining > 0 && dialog.isShowing) {
                delay(1000)
                remaining--
                countdownText.text = "Auto-confirming YES in $remaining seconds..."
                progressBar.progress = remaining
            }

            if (dialog.isShowing) {
                dialog.dismiss()
                onDecision(true)
            }
        }

        dialog.setOnDismissListener {
            countdownJob.cancel()
        }
    }

// endregion


}
