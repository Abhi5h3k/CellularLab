package com.abhishek.cellularlab.tests.iperf

import android.content.Context
import android.widget.ScrollView
import android.widget.TextView
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.view.View
import android.widget.Button
import android.os.Environment
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import com.abhishek.cellularlab.R


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
    private val MAX_LOG_SIZE = 5 * 1024 * 1024 // 5 MB
    //private val MAX_LOG_SIZE = 1024 * 10 // 10 KB for test
    private val createdLogFiles = mutableListOf<File>()
    private val packetLossHistory = mutableListOf<Float>()
    private var watchdogJob: Job? = null


    private var iperfJob: Job? = null
    private var lastIterationHadError = false
    private val mainScope = CoroutineScope(Dispatchers.Main)

    private var wasAutoReducedOnPacketLoss = false


    val throughputRegex = Regex("""\s+(\d+(?:\.\d+)?)\s+(K|M|G)?bits/sec""")


    fun startTest(
        args: Array<String>,
        testIterations: Int,
        waitTime: Int,
        isIncrementalRampUpTest: Boolean,
        isHybridTest: Boolean,
        isSmartIncrementalRampUpTest: Boolean
    ) {
        var currentArgs = args.copyOf()

        val testDurationMs = getTestDurationMillis(args) // includes buffer
        val timeoutMargin = 60_000L // 60 seconds extra grace
        val totalTimeout = testDurationMs + timeoutMargin
        val errorBackoffMs = (testDurationMs * 0.33).toLong().coerceAtLeast(20_000L)


        var originalBandwidth = extractBandwidthMbps(args) ?: 0
        var currentStepBandwidth = 50
        val lossThreshold = 75.0f
        //val lossThreshold = 5.0f
        val requiredHighLossCount = 2
        val historyWindowSize = 3


        val waitTimeMillis = waitTime * 1000

        var commandStr = args.joinToString(" ")

        val handler = CoroutineExceptionHandler { _, exception ->
            append("🚨 Uncaught Exception: ${exception.localizedMessage}")
            exception.printStackTrace()
        }

        iperfJob = CoroutineScope(Dispatchers.IO + handler).launch {

            startTimer() // ⏱ Start


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

            repeat(testIterations) { iteration ->
                val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                append("\n\n🕒 [$currentTime] ──🚀 Starting iPerf3 Test ${iteration + 1}/$testIterations ──\n$commandStr")

                val testCompleted = CompletableDeferred<Unit>()

                var maxAchievedThisRun = 0

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

                if (isSmartIncrementalRampUpTest) {
                    // Update bandwidth if using smart ramp-up
                    // Use currentBandwidth for this run
                    currentArgs = updateBandwidth(currentArgs, currentStepBandwidth)
                    append("\n\n📶 Set bandwidth: ${currentStepBandwidth}M")
                    commandStr = currentArgs.joinToString(" ")
                    append("\n[New Command :] $commandStr")

                }


                // Start watchdog after launching iPerf test
                watchdogJob = launch(Dispatchers.IO + handler) {
                    append("\n\n🐶 Watchdog launched – Don't mind me, just chilling in the corner. I'll jump in if you get stuck! 🚨\n")
                    delay(totalTimeout)
                    if (!testCompleted.isCompleted) {
                        append("⏰ Watchdog: Forcing test to abort after timeout.")
                        forceStopIperfTest(
                            createIperfCallback(
                                onLine = { line ->
                                    append("📊 $line")
                                    if (isSmartIncrementalRampUpTest) {
                                        parseThroughputMbps(line)?.let {
                                            if (it > maxAchievedThisRun) maxAchievedThisRun = it
                                        }
                                    }
                                },
                                onError = { error ->
                                    append("\n❌ Error: $error")
                                    lastIterationHadError = true
                                    testCompleted.complete(Unit)
                                },
                                onComplete = {
                                    append("\n🏁 [End] Iteration ${iteration + 1}\n")
                                    testCompleted.complete(Unit)
                                }
                            ))
                        delay(3000) // give JNI time to mark done and exit
                    }
                }


                // Run JNI in a background coroutine
                val runJob = launch(Dispatchers.IO) {
                    runIperfLive(
                        currentArgs, createIperfCallback(
                            onLine = { line ->
                                append("📊 $line")

                                parsePacketLoss(line)?.let { loss ->
                                    if (packetLossHistory.size >= historyWindowSize) packetLossHistory.removeAt(0)
                                    packetLossHistory.add(loss)
                                }

                                if (isSmartIncrementalRampUpTest) {
                                    parseThroughputMbps(line)?.let {
                                        if (it > maxAchievedThisRun) maxAchievedThisRun = it
                                    }
                                }
                            },
                            onError = { error ->
                                append("\n❌ Error: $error")
                                lastIterationHadError = true
                                testCompleted.complete(Unit)
                            },
                            onComplete = {
                                append("\n\n🏁 [End] Iteration ${iteration + 1}")
                                testCompleted.complete(Unit)
                            }
                        ))
                }




                try {
                    withTimeout(totalTimeout) {
                        testCompleted.await()
                    }
                } catch (e: TimeoutCancellationException) {
                    append("⏰ Timeout: iPerf did not respond for iteration ${iteration + 1}")
                }

                if (isAutoReduceEnabled() &&
                    packetLossHistory.size == historyWindowSize &&
                    packetLossHistory.count { it > lossThreshold }  >= requiredHighLossCount
                ) {

                    withContext(Dispatchers.Main) {
                        showReduceBandwidthDialog { reduce ->
                            if (reduce) {
                                currentStepBandwidth = (currentStepBandwidth * 0.8).toInt().coerceAtLeast(10)
                                append("📉 High packet loss detected. Reduced bandwidth to ${currentStepBandwidth}M.")
                                currentArgs = updateBandwidth(currentArgs, currentStepBandwidth)
                                wasAutoReducedOnPacketLoss = true
                            } else {
                                append("⚠️ High packet loss ignored. Keeping current bandwidth.")
                            }
                        }
                    }
                }

                // Optionally cancel JNI if it’s still running
                val cancelResult = withTimeoutOrNull(3000) {
                    runJob.cancelAndJoin()
                }
                if (cancelResult == null) {
                    append("⚠️ Timeout: JNI job did not cancel in time.")
                }

                if ((isSmartIncrementalRampUpTest || wasAutoReducedOnPacketLoss) && !lastIterationHadError) {
                    currentStepBandwidth = evaluateBandwidthRampUp(
                        currentStepBandwidth, iteration, maxAchievedThisRun, originalBandwidth
                    )
                }

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
                    stopTimer() // stop timer
                    startBtn.text = "New Test"
                    withContext(Dispatchers.Main) {
                        onTestComplete()
                    }

                }
            }
        }
    }

    fun stopTests() {
        iperfJob?.cancel()
        watchdogJob?.cancel()  // ✅ Cancel watchdog if active
        watchdogJob = null // optional but safe to reset
        stopTimer() // stop timer
        onTestComplete()

        append("\n⛔ Stop requested. Test will terminate after the current iteration.\n")
    }

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

    private suspend fun runTcpBidirAndGetBandwidth(args: Array<String>): Int {
        val tcpArgs = args.toMutableList().apply {
            removeAll(listOf("-u", "-b")) // Ensure it's TCP
            if (!contains("--bidir")) add("--bidir")
        }

        var maxBandwidthMbps = 0
        val completed = CompletableDeferred<Unit>()

        runIperfLive(
            tcpArgs.toTypedArray(), createIperfCallback(
                onLine = { line ->
                    parseThroughputMbps(line)?.let {
                        if (it > maxBandwidthMbps) maxBandwidthMbps = it
                    }
                },
                onError = {
                    append("❌ TCP error: $it")
                    completed.complete(Unit)
                },
                onComplete = {
                    append("✅ TCP bidir test complete.")
                    completed.complete(Unit)
                }
            ))

        val tcpTimeout = getTestDurationMillis(args, bufferSeconds = 10)
        withTimeoutOrNull(tcpTimeout) { completed.await() }

        return maxBandwidthMbps
    }

    fun getTestDurationMillis(args: Array<String>, bufferSeconds: Int = 5): Long {
        val tIndex = args.indexOf("-t")
        val testSeconds = if (tIndex != -1 && tIndex + 1 < args.size) {
            args[tIndex + 1].toIntOrNull() ?: 10
        } else {
            10
        }

        val totalSeconds = testSeconds + bufferSeconds
        return totalSeconds * 1000L
    }

    private fun createIperfCallback(
        onLine: (String) -> Unit = {},
        onError: (String) -> Unit = {},
        onComplete: () -> Unit = {}
    ): IperfCallback {
        return object : IperfCallback {
            override fun onOutput(line: String) = onLine(line)
            override fun onError(error: String) = onError(error)
            override fun onComplete() = onComplete()
        }
    }

    private fun parseThroughputMbps(line: String): Int? {
        val match = throughputRegex.find(line)
        val value = match?.groups?.get(1)?.value?.toFloatOrNull()
        val unit = match?.groups?.get(2)?.value

        val throughputMbps = when (unit) {
            "K" -> value?.div(1000)
            "M" -> value
            "G" -> value?.times(1000)
            else -> value?.div(1_000_000) // assume bits/sec
        }

        return throughputMbps?.toInt()
    }

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
        var logFile: File

        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName

        do {
            val fileName = "iPerf3_${timestamp}_v${versionName}_$part.txt"
            logFile = File(dir, fileName)
            if (!logFile.exists() || logFile.length() < MAX_LOG_SIZE) break
            part++
        } while (true)

        if (!logFile.exists()) {
            logFile.createNewFile()
            createdLogFiles.add(logFile) // ✅ Track newly created file
        } else if (!createdLogFiles.contains(logFile)) {
            createdLogFiles.add(logFile) // ✅ Track reused but not-yet-tracked file
        }

        return logFile
    }

    private fun parsePacketLoss(line: String): Float? {
        val lossRegex = Regex("""\((\d+(?:\.\d+)?)%\)""")
        return lossRegex.find(line)?.groups?.get(1)?.value?.toFloatOrNull()
    }
//    private fun showReduceBandwidthDialog(onDecision: (Boolean) -> Unit) {
//        val builder = AlertDialog.Builder(context)
//        builder.setTitle("High Packet Loss Detected")
//            .setMessage("Shall we reduce the bandwidth to improve reliability?")
//            .setPositiveButton("Yes") { _, _ -> onDecision(true) }
//            .setNegativeButton("No") { _, _ -> onDecision(false) }
//
//        val dialog = builder.create()
//        dialog.show()
//
//        // Auto-select yes after 5 seconds
//        CoroutineScope(Dispatchers.Main).launch {
//            delay(5000)
//            if (dialog.isShowing) {
//                dialog.dismiss()
//                onDecision(true)
//            }
//        }
//    }

    private fun showReduceBandwidthDialog(onDecision: (Boolean) -> Unit) {
        val dialogView = View.inflate(context, R.layout.dialog_reduce_bandwidth, null)
        val countdownText = dialogView.findViewById<TextView>(R.id.reduceCountdownText)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.reduceProgressBar)

        val builder = AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("Yes") { _, _ -> onDecision(true) }
            .setNegativeButton("No") { _, _ -> onDecision(false) }

        val dialog = builder.create()
        dialog.show()

        // Auto-confirm logic
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
                onDecision(true) // Default is yes
            }
        }

        dialog.setOnDismissListener {
            countdownJob.cancel() // Clean up coroutine
        }
    }

    private fun evaluateBandwidthRampUp(currentStepBandwidth:Int, iteration: Int, maxAchieved: Int, originalBw: Int): Int {
        if (maxAchieved >= (currentStepBandwidth * 0.9) && currentStepBandwidth < originalBw) {

            append("\n\n📈 Smart Ramp-up: Achieved ${maxAchieved}M, increasing bandwidth to ${currentStepBandwidth}M for next run.")
            append("\n\n✅ Increasing bandwidth to ${currentStepBandwidth}M for next run.")
            return (currentStepBandwidth + ((iteration + 1) * 50)).coerceAtMost(originalBw)
        } else {
            append("⏸ Holding bandwidth at ${currentStepBandwidth}M.")
            return  currentStepBandwidth
        }
    }

}
