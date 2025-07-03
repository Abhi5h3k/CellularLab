package com.abhishek.cellularlab.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.abhishek.cellularlab.R
import com.abhishek.cellularlab.tests.iperf.IperfRunner
import com.abhishek.cellularlab.tests.iperf.IperfTestManage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fragment to allow users to input and execute raw iPerf commands.
 * Displays test output and logs in a scrollable view.
 */
class CommandLineFragment : Fragment() {

    // region UI Elements
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var inputCommand: EditText
    private lateinit var startBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var outputView: TextView
    private lateinit var commandTextTimer: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var outputLabel: TextView
    private lateinit var outputLayout: LinearLayout
    private lateinit var timerView: TextView

    // endregion

    // region State & Test Manager

    private var isAutoScrollEnabled = true
    private lateinit var gestureDetector: GestureDetector
    private var iperfManager: IperfTestManage? = null
    private var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    // endregion

    // region Constants
    companion object {
        // SharedPreferences keys
        private const val PREFS_NAME = "AppPreferences"
        private const val KEY_COMMAND = "inputCommand"
    }

    // region Fragment Lifecycle

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_command_line, container, false)

        // Keep screen on during test
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Initialize UI components
        initViews(view)

        // Load saved preferences
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        restoreSavedValues()


        // Setup gestures
        setupGestureScrollToggle()

        // Setup button listeners
        setupListeners()

        return view
    }

    /**
     * Initializes all required view references from the layout.
     */
    private fun initViews(view: View) {
        inputCommand = view.findViewById(R.id.command_input)
        startBtn = view.findViewById(R.id.run_command_button)
        stopBtn = view.findViewById(R.id.stop_command_button)
        outputView = view.findViewById(R.id.command_output)
        scrollView = view.findViewById(R.id.outputScrollView)
        commandTextTimer = view.findViewById(R.id.commandTextTimer)
        timerView = view.findViewById(R.id.commandTextTimer)
        outputLabel = view.findViewById(R.id.commandOutputLabel)
        outputLayout = view.findViewById(R.id.commandOutputLayout)
    }

    // region Preferences
    /**
     * Saves current input values to SharedPreferences.
     */
    private fun saveCurrentValues() {
        with(sharedPreferences.edit()) {
            putString(KEY_COMMAND, inputCommand.text.toString())
            apply()
        }
    }

    /**
     * Restores saved input values from SharedPreferences.
     */
    private fun restoreSavedValues() {
        inputCommand.setText(sharedPreferences.getString(KEY_COMMAND, ""))
    }
    // endregion

    /**
     * Sets up click listeners for Start and Stop buttons.
     */
    private fun setupListeners() {
        startBtn.setOnClickListener {
            val rawCommand = inputCommand.text.toString().trim()
            if (rawCommand.isNotEmpty()) {
                prepareForTest()
                runCommand(rawCommand)
                saveCurrentValues() // Save command after starting the test
            } else {
                Toast.makeText(requireContext(), "Enter a valid command", Toast.LENGTH_SHORT).show()
            }
        }

        stopBtn.setOnClickListener {
            stopBtn.isEnabled = false
            iperfManager?.stopTests {
                resetTestUI() // ✅ Only runs after runJob's onComplete
            }
        }
    }

    // endregion

    // region iPerf Execution

    /**
     * Launches the test by initializing [IperfTestManage] and running the provided command.
     */
    private fun runCommand(command: String) {
        val args = command.split("\\s+".toRegex()).toTypedArray()

        iperfManager = IperfTestManage(
            context = requireContext(),
            startBtn = startBtn,
            outputView = outputView,
            scrollView = scrollView,
            isAutoScrollEnabled = { isAutoScrollEnabled },
            timestamp = timestamp,
            startTimer = { IperfRunner.startTimer(timerView) },
            stopTimer = { IperfRunner.stopTimer(timerView) },
            onTestComplete = {
                // Re-enable inputs on completion
                startBtn.isEnabled = true
                inputCommand.isEnabled = true
                stopBtn.isEnabled = false
                stopBtn.visibility = View.GONE
            }
        )

        outputView.text = ""
        iperfManager?.startTest(args, testIterations = 1, waitTime = 1)
    }

    /**
     * Updates UI to indicate a test is running.
     */
    private fun prepareForTest() {
        startBtn.isEnabled = false
        inputCommand.isEnabled = false
        stopBtn.isEnabled = true
        stopBtn.visibility = View.VISIBLE

        listOf(outputLabel, outputLayout).forEach { it.visibility = View.VISIBLE }

        timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }

    /**
     * Resets the UI to allow starting a new test.
     */
    private fun resetTestUI() {
        startBtn.text = "Run Command"
        listOf(inputCommand, startBtn).forEach { it.visibility = View.VISIBLE }
        outputLabel.visibility = View.GONE
        outputLayout.visibility = View.GONE
    }

    // endregion

    // region Gesture Handling

    /**
     * Enables double-tap gesture on output scroll view to toggle auto-scroll.
     * Useful for pausing scrolling while reading long output.
     */
    private fun setupGestureScrollToggle() {
        gestureDetector =
            GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    isAutoScrollEnabled = !isAutoScrollEnabled
                    val msg =
                        if (isAutoScrollEnabled) "🔽 Auto-scroll enabled" else "🛑 Auto-scroll disabled"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    return true
                }
            })

        scrollView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    // endregion
}
