package com.abhishek.cellularlab.tests.iperf

import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale


object IperfRunner {
    init {
        System.loadLibrary("cellularlab")
    }

    // Timer and test management
    private val timerJobs = mutableMapOf<TextView, Job>()
    private val startTimes = mutableMapOf<TextView, Long>()

    // endregion

    @JvmStatic
    external fun runIperfLive(arguments: Array<String>, callback: IperfCallback)

    @JvmStatic
    external fun forceStopIperfTest(callback: IperfCallback)

    // region Timer
    /**
     * Starts a coroutine timer to update elapsed time during the test.
     */

    fun startTimer(timerView: TextView) {
        stopTimer(timerView) // Stop any existing timer for this view
        startTimes[timerView] = System.currentTimeMillis()
        val job = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - (startTimes[timerView] ?: 0L)
                val formatted = String.format(
                    Locale.getDefault(),
                    "%02d:%02d:%02d",
                    (elapsed / 3600000).toInt(),
                    (elapsed / 60000 % 60).toInt(),
                    (elapsed / 1000 % 60).toInt()
                )
                timerView.text = "⏱ Elapsed: $formatted"
                delay(1000)
            }
        }
        timerJobs[timerView] = job
    }

    fun stopTimer(timerView: TextView) {
        timerJobs[timerView]?.cancel()
        timerJobs.remove(timerView)
        startTimes.remove(timerView)
    }
    // endregion


}