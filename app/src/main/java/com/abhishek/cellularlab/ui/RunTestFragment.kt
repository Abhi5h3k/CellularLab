package com.abhishek.cellularlab.ui

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.fragment.app.Fragment
import com.abhishek.cellularlab.R
import com.abhishek.cellularlab.tests.iperf.IperfRunner
import com.abhishek.cellularlab.tests.iperf.IperfTestManage
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RunTestFragment : Fragment() {

    // region Constants
    companion object {

        // SharedPreferences keys
        private const val PREFS_NAME = "AppPreferences"
        private const val KEY_SERVER_IP = "serverIp"
        private const val KEY_PORT = "port"
        private const val KEY_DURATION = "duration"
        private const val KEY_PARALLEL_STREAMS = "parallelStreams"
        private const val KEY_INTERVAL_SECONDS = "intervalSeconds"
        private const val KEY_TEST_ITERATIONS = "testIterations"
        private const val KEY_ITERATION_WAIT_TIME = "iterationWaitTime"
        private const val KEY_DEBUG_CHECKBOX = "debugCheckbox"
        private const val KEY_VERBOSE_CHECKBOX = "verboseCheckbox"
    }

    // endregion

    // region View Declarations
    // UI elements
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var outputView: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var timerView: TextView
    private lateinit var startBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var outputLabel: TextView
    private lateinit var outputLayout: LinearLayout
    private lateinit var optionsLayout: LinearLayout
    private lateinit var wtLayout: LinearLayout
    private lateinit var iterationLayout: LinearLayout
    private lateinit var titleAdvancedSettings: TextView
    private lateinit var tdLayout: LinearLayout
    private lateinit var intervalLayout: LinearLayout
    private lateinit var psLayout: LinearLayout
    private lateinit var bwLayout: LinearLayout
    private lateinit var protocolLayout: LinearLayout
    private lateinit var durationLayout: LinearLayout
    private lateinit var portLayout: LinearLayout
    private lateinit var hostLayout: LinearLayout
    private lateinit var titleBasicSettings: TextView

    private lateinit var spinnerProtocol: Spinner
    private lateinit var testDirection: Spinner

    private lateinit var inputServerIp: EditText
    private lateinit var inputPort: EditText
    private lateinit var inputDuration: EditText
    private lateinit var inputBandwidth: EditText
    private lateinit var parallelStreams: EditText
    private lateinit var intervalSeconds: EditText
    private lateinit var testIterations: EditText
    private lateinit var iterationWaitTime: EditText
    private lateinit var checkboxDebug: CheckBox
    private lateinit var checkboxVerbose: CheckBox
    private lateinit var autoReduceCheckbox: CheckBox

    private lateinit var versionTextView: TextView
    // endregion

    // region State & Managers
    private var isAutoScrollEnabled = true
    private lateinit var gestureDetector: GestureDetector

    // Test mode flags
    private var isIncrementalRampUpTest = false
    private var isSmartIncrementalRampUpTest = false
    private var isHybridTest = false

    private var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    private var iperfManager: IperfTestManage? = null

    // region Fragment Lifecycle
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_run_test, container, false)

        // Bind all UI elements
        bindViews(view)

        // Keep screen on during test
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Set info icon click listeners for help dialogs
        view.findViewById<View>(R.id.iconInfoIp).setOnClickListener { onInfoIconClick(it) }
        view.findViewById<View>(R.id.iconInfoPort).setOnClickListener { onInfoIconClick(it) }
        view.findViewById<View>(R.id.iconInfoProtocol).setOnClickListener { onInfoIconClick(it) }
        view.findViewById<View>(R.id.iconInfoDuration).setOnClickListener { onInfoIconClick(it) }
        view.findViewById<View>(R.id.iconInfoBandwidth).setOnClickListener { onInfoIconClick(it) }
        view.findViewById<View>(R.id.iconInfoParallelStreams)
            .setOnClickListener { onInfoIconClick(it) }
        view.findViewById<View>(R.id.iconInfoInterval).setOnClickListener { onInfoIconClick(it) }
        view.findViewById<View>(R.id.iconInfoTestDirection)
            .setOnClickListener { onInfoIconClick(it) }
        view.findViewById<View>(R.id.iconInfoIterations).setOnClickListener { onInfoIconClick(it) }
        view.findViewById<View>(R.id.iconInfoWaitTime).setOnClickListener { onInfoIconClick(it) }
        view.findViewById<View>(R.id.iconInfoOptions).setOnClickListener { onInfoIconClick(it) }

        // Setup protocol and direction spinners
        setupSpinners()
        // Setup gesture for toggling auto-scroll
        setupGestureScrollToggle()

        // Load saved preferences
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        restoreSavedValues()

        // Button click listeners
        startBtn.setOnClickListener { onStartButtonClick() }
        stopBtn.setOnClickListener {
            stopBtn.isEnabled = false
            iperfManager?.stopTests {
                resetTestUI() // ✅ Only runs after runJob's onComplete
            }
        }

        // Show intro guide on first launch
        if (isFirstLaunch()) {
            requireActivity().window.decorView.post {
                showIntroGuide()
                markFirstLaunchComplete()
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        // Ensure layout is refreshed
        view?.requestLayout()
    }
//    override fun onDestroyView() {
//        super.onDestroyView()
//        iperfManager?.clear()
//        iperfManager = null
//    }
    // endregion

    // region UI Setup
    /**
     * Binds all view elements from the layout to properties.
     */
    private fun bindViews(view: View) {
        outputView = view.findViewById(R.id.textOutput)
        scrollView = view.findViewById(R.id.scrollView)
        timerView = view.findViewById(R.id.textTimer)
        startBtn = view.findViewById(R.id.buttonStart)
        stopBtn = view.findViewById(R.id.buttonStop)

        outputLabel = view.findViewById(R.id.outputLabel)
        outputLayout = view.findViewById(R.id.outputLayout)
        optionsLayout = view.findViewById(R.id.optionsLayout)
        wtLayout = view.findViewById(R.id.wtLayout)
        iterationLayout = view.findViewById(R.id.iterationLayout)
        titleAdvancedSettings = view.findViewById(R.id.titleAdvancedSettings)
        tdLayout = view.findViewById(R.id.tdLayout)
        intervalLayout = view.findViewById(R.id.intervalLayout)
        psLayout = view.findViewById(R.id.psLayout)
        bwLayout = view.findViewById(R.id.bwLayout)
        protocolLayout = view.findViewById(R.id.protocolLayout)
        durationLayout = view.findViewById(R.id.durationLayout)
        portLayout = view.findViewById(R.id.portLayout)
        hostLayout = view.findViewById(R.id.hostLayout)
        titleBasicSettings = view.findViewById(R.id.titleBasicSettings)

        spinnerProtocol = view.findViewById(R.id.spinnerProtocol)
        testDirection = view.findViewById(R.id.testDirection)

        inputServerIp = view.findViewById(R.id.inputServerIp)
        inputPort = view.findViewById(R.id.inputPort)
        inputDuration = view.findViewById(R.id.inputDuration)
        inputBandwidth = view.findViewById(R.id.inputBandwidth)
        parallelStreams = view.findViewById(R.id.parallelStreams)
        intervalSeconds = view.findViewById(R.id.intervalSeconds)
        testIterations = view.findViewById(R.id.testIterations)
        iterationWaitTime = view.findViewById(R.id.iterationWaitTime)
        checkboxDebug = view.findViewById(R.id.checkboxDebug)
        checkboxVerbose = view.findViewById(R.id.checkboxVerbose)
        autoReduceCheckbox = view.findViewById(R.id.autoReduceBandwidth)
    }

    /**
     * Sets up protocol and test direction spinners with listeners.
     */
    private fun setupSpinners() {
        // Protocol spinner
        ArrayAdapter.createFromResource(
            requireContext(), R.array.protocol_options, R.layout.spinner_item_white
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_item_white)
            spinnerProtocol.adapter = adapter
        }
        spinnerProtocol.setSelection(0)

        spinnerProtocol.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                // Show/hide bandwidth options based on protocol
                val show = position != 0 && position != 3
                bwLayout.visibility = if (show) View.VISIBLE else View.GONE
                autoReduceCheckbox.visibility = if (show) View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Test direction spinner
        ArrayAdapter.createFromResource(
            requireContext(), R.array.test_direction_options, R.layout.spinner_item_white
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_item_white)
            testDirection.adapter = adapter
        }
        testDirection.setSelection(0)
    }

    // TODO: Refactor this to a common utility class [IperfRunner.kt]
    /**
     * Enables double-tap gesture on the output scroll view to toggle auto-scroll.
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

    // region Test Flow
    /**
     * Handles the start button click: validates input, builds arguments, and starts the test.
     */
    private fun onStartButtonClick() {
        // If test is finished, reset UI for new test
        if (startBtn.text.toString() == "New Test") {
            startBtn.text = "START TEST"
            resetTestUI()
            return
        }

        saveCurrentValues()
        val iperfArgs = getIperfArguments() ?: return

        timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        outputView.text = ""

        startBtn.isEnabled = false
        stopBtn.isEnabled = true
        stopBtn.visibility = View.VISIBLE

        // Show output, hide options
        listOf(outputLabel, outputLayout).forEach { it.visibility = View.VISIBLE }
        listOf(
            optionsLayout, wtLayout, iterationLayout, titleAdvancedSettings, tdLayout,
            intervalLayout, psLayout, bwLayout, protocolLayout, durationLayout,
            portLayout, hostLayout, titleBasicSettings
        ).forEach { it.visibility = View.GONE }

        // Initialize and start iPerf test manager
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
                startBtn.isEnabled = true
                stopBtn.isEnabled = false
                stopBtn.visibility = View.GONE
            },
            isAutoReduceEnabled = { autoReduceCheckbox.isChecked }
        )

        val iterations = testIterations.text.toString().toIntOrNull() ?: 1
        val waitTime = iterationWaitTime.text.toString().toIntOrNull() ?: 15

        iperfManager?.startTest(
            iperfArgs,
            iterations,
            waitTime,
            isIncrementalRampUpTest,
            isHybridTest,
            isSmartIncrementalRampUpTest
        )
    }

    /**
     * Resets the UI to its initial state for a new test.
     */
    private fun resetTestUI() {
        startBtn.text = "START TEST"
        outputLabel.visibility = View.GONE
        outputLayout.visibility = View.GONE

        listOf(
            optionsLayout, wtLayout, iterationLayout, titleAdvancedSettings, tdLayout,
            intervalLayout, psLayout, bwLayout, protocolLayout, durationLayout,
            portLayout, hostLayout, titleBasicSettings
        ).forEach { it.visibility = View.VISIBLE }
    }
    // endregion


    // region Argument Builder
    /**
     * Builds the iPerf command-line arguments from user input.
     * Returns null and shows a Toast if validation fails.
     */
    private fun getIperfArguments(): Array<String>? {
        val serverIp = inputServerIp.text.toString().trim()
        val port = inputPort.text.toString().toIntOrNull()
        val duration = inputDuration.text.toString().toIntOrNull()
        val bandwidth = inputBandwidth.text.toString().toIntOrNull()
        val parallel = parallelStreams.text.toString().toIntOrNull()
        val interval = intervalSeconds.text.toString().toIntOrNull()
        val protocol = spinnerProtocol.selectedItem.toString()
        val testDirection = testDirection.selectedItem.toString()
        val isDebug = checkboxDebug.isChecked
        val isVerbose = checkboxVerbose.isChecked

        // Validate required fields
        if (serverIp.isEmpty() || port == null || duration == null || interval == null) {
            Toast.makeText(
                requireContext(),
                "Please fill all required fields correctly.",
                Toast.LENGTH_SHORT
            ).show()
            return null
        }

        val args = mutableListOf(
            "iperf3", "-c", serverIp, "-p", port.toString(),
            "-t", duration.toString(), "-i", interval.toString()
        )

        if (parallel != null) {
            args.addAll(listOf("-P", parallel.toString()))
        }

        // Add direction flags
        when (testDirection) {
            "Download (-R)" -> args.add("-R")
            "Bidirectional (--bidir)" -> args.add("--bidir")
        }

        // Add protocol-specific flags
        when (protocol) {
            "UDP", "UDP Incremental Ramp-Up Test", "Smart Ramp-Up Strategy" -> {
                if (bandwidth == null) {
                    Toast.makeText(
                        requireContext(),
                        "Please enter valid bandwidth.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return null
                }
                args.addAll(listOf("-u", "-b", "${bandwidth}M"))
                isIncrementalRampUpTest = protocol.contains("Incremental")
                isSmartIncrementalRampUpTest = protocol.contains("Smart")
            }

            "TCP + UDP Hybrid Strategy" -> isHybridTest = true
        }

        if (isDebug) args.add("-d")
        if (isVerbose) args.add("-V")

        return args.toTypedArray()
    }
    // endregion

    // region Preferences
    /**
     * Saves current input values to SharedPreferences.
     */
    private fun saveCurrentValues() {
        with(sharedPreferences.edit()) {
            putString(KEY_SERVER_IP, inputServerIp.text.toString())
            putString(KEY_PORT, inputPort.text.toString())
            putString(KEY_DURATION, inputDuration.text.toString())
            putString(KEY_PARALLEL_STREAMS, parallelStreams.text.toString())
            putString(KEY_INTERVAL_SECONDS, intervalSeconds.text.toString())
            putString(KEY_TEST_ITERATIONS, testIterations.text.toString())
            putString(KEY_ITERATION_WAIT_TIME, iterationWaitTime.text.toString())
            putBoolean(KEY_DEBUG_CHECKBOX, checkboxDebug.isChecked)
            putBoolean(KEY_VERBOSE_CHECKBOX, checkboxVerbose.isChecked)
            apply()
        }
    }

    /**
     * Restores saved input values from SharedPreferences.
     */
    private fun restoreSavedValues() {
        inputServerIp.setText(sharedPreferences.getString(KEY_SERVER_IP, ""))
        inputPort.setText(sharedPreferences.getString(KEY_PORT, "5202"))
        inputDuration.setText(sharedPreferences.getString(KEY_DURATION, "30"))
        parallelStreams.setText(sharedPreferences.getString(KEY_PARALLEL_STREAMS, "1"))
        intervalSeconds.setText(sharedPreferences.getString(KEY_INTERVAL_SECONDS, "1"))
        testIterations.setText(sharedPreferences.getString(KEY_TEST_ITERATIONS, "1"))
        iterationWaitTime.setText(sharedPreferences.getString(KEY_ITERATION_WAIT_TIME, "15"))
        checkboxDebug.isChecked = sharedPreferences.getBoolean(KEY_DEBUG_CHECKBOX, false)
        checkboxVerbose.isChecked = sharedPreferences.getBoolean(KEY_VERBOSE_CHECKBOX, false)
    }
    // endregion

    // region Info Dialogs
    /**
     * Shows an info dialog with help text for the given field.
     */
    fun onInfoIconClick(view: View) {
        val message = when (view.id) {
            R.id.iconInfoIp -> getString(R.string.info_ip)
            R.id.iconInfoPort -> getString(R.string.info_port)
            R.id.iconInfoProtocol -> getString(R.string.info_protocol)
            R.id.iconInfoDuration -> getString(R.string.info_duration)
            R.id.iconInfoBandwidth -> getString(R.string.info_bandwidth)
            R.id.iconInfoParallelStreams -> getString(R.string.info_parallel_streams)
            R.id.iconInfoInterval -> getString(R.string.info_interval)
            R.id.iconInfoTestDirection -> getString(R.string.info_test_direction)
            R.id.iconInfoIterations -> getString(R.string.info_iterations)
            R.id.iconInfoWaitTime -> getString(R.string.info_wait_time)
            R.id.iconInfoOptions -> getString(R.string.info_options)
            else -> getString(R.string.info_default)
        }

        AlertDialog.Builder(requireContext()).setTitle("Info").setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    // endregion

    // region First Launch Logic
    /**
     * Checks if this is the first launch of the app.
     */
    private fun isFirstLaunch(): Boolean {
        val prefs = requireActivity().getSharedPreferences("cellularlab_prefs", MODE_PRIVATE)
        return prefs.getBoolean("first_launch", true)
    }

    /**
     * Marks that the first launch guide has been shown.
     */
    private fun markFirstLaunchComplete() {
        val prefs = requireActivity().getSharedPreferences("cellularlab_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("first_launch", false).apply()
    }

    /**
     * Shows an interactive intro guide using TapTargetView.
     */
    fun showIntroGuide() {
        versionTextView = requireActivity().findViewById(R.id.appVersionText)
        requireActivity().window.decorView.post {
            val sequence = TapTargetSequence(requireActivity())
                .targets(
                    themedTarget(
                        versionTextView,
                        "About the Developer",
                        "Tap to learn about the developer or share this app.",
                        1
                    ),
                    themedTarget(
                        inputServerIp,
                        "Server IP",
                        "Enter the IP address of the iPerf3 server you want to test against.\n\nTap the info icon beside each field for more guidance.",
                        2
                    ),
                )
                .listener(object : TapTargetSequence.Listener {
                    override fun onSequenceFinish() {
                        Toast.makeText(
                            requireContext(),
                            "You're ready to start testing 🚀",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onSequenceStep(lastTarget: TapTarget, targetClicked: Boolean) {}
                    override fun onSequenceCanceled(lastTarget: TapTarget?) {}
                })
            sequence.start()
        }
    }

    /**
     * Helper to create a themed TapTarget for the intro guide.
     */
    fun themedTarget(
        view: View,
        title: String,
        description: String,
        id: Int,
        radius: Int = 60
    ): TapTarget {
        return TapTarget.forView(view, title, description)
            .outerCircleColor(R.color.colorAccent)
            .titleTextColor(android.R.color.white)
            .descriptionTextColor(android.R.color.white)
            .textTypeface(Typeface.SANS_SERIF)
            .dimColor(R.color.colorPrimaryDark)
            .tintTarget(true)
            .transparentTarget(true)
            .drawShadow(true)
            .id(id)
            .targetRadius(radius)
    }
    // endregion
}