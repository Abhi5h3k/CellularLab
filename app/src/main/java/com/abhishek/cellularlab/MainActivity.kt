package com.abhishek.cellularlab

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
//import android.os.Environment
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.abhishek.cellularlab.tests.iperf.IperfCallback
import com.abhishek.cellularlab.tests.iperf.IperfTestManage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    // --- Existing and new Companion object ---
    companion object {
        init {
            // Load JNI shared library for native iPerf3 functionality
            System.loadLibrary("cellularlab")
        }

        // --- SharedPreferences Keys added here ---
        private const val PREFS_NAME = "AppPreferences" // Name of your SharedPreferences file

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

    // Native JNI function to run iperf test, used by IperfTestManager
    external fun runIperfLive(arguments: Array<String>, callback: IperfCallback)
    external fun forceStopIperfTest(callback: IperfCallback)

    // UI Elements
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
    private lateinit var titleiperftest: TextView
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

    private lateinit var versionTextView:TextView

    private var isAutoScrollEnabled = true
    private lateinit var gestureDetector: GestureDetector

    private var isIncrementalRampUpTest = false
    private var isSmartIncrementalRampUpTest = false
    private var isHybridTest = false


    //Timer related variables
    private var startTimeMillis: Long = 0L
    private var timerJob: Job? = null

    // Logging
    private var logFile: File? = null
    private var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    // Manager for iperf test logic
    private var iperfManager: IperfTestManage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Prevent screen from sleeping during test
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        versionTextView = findViewById<TextView>(R.id.appVersionText)
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        versionTextView.text = "v$versionName"

        // Bind views
        outputView = findViewById(R.id.textOutput)
        scrollView = findViewById(R.id.scrollView)
        timerView = findViewById(R.id.textTimer)
        startBtn = findViewById(R.id.buttonStart)
        stopBtn = findViewById(R.id.buttonStop)

        outputLabel = findViewById(R.id.outputLabel)
        outputLayout = findViewById(R.id.outputLayout)
        optionsLayout = findViewById(R.id.optionsLayout)
        wtLayout = findViewById(R.id.wtLayout)
        iterationLayout = findViewById(R.id.iterationLayout)
        titleAdvancedSettings = findViewById(R.id.titleAdvancedSettings)
        tdLayout = findViewById(R.id.tdLayout)
        intervalLayout = findViewById(R.id.intervalLayout)
        psLayout = findViewById(R.id.psLayout)
        bwLayout = findViewById(R.id.bwLayout)
        protocolLayout = findViewById(R.id.protocolLayout)
        durationLayout = findViewById(R.id.durationLayout)
        portLayout = findViewById(R.id.portLayout)
        hostLayout = findViewById(R.id.hostLayout)
        titleBasicSettings = findViewById(R.id.titleBasicSettings)
        titleiperftest = findViewById(R.id.titleiperftest)

        inputServerIp = findViewById(R.id.inputServerIp)
        inputPort = findViewById(R.id.inputPort)
        inputDuration = findViewById(R.id.inputDuration)
        spinnerProtocol = findViewById(R.id.spinnerProtocol)
        inputBandwidth = findViewById(R.id.inputBandwidth)
        parallelStreams = findViewById(R.id.parallelStreams)
        intervalSeconds = findViewById(R.id.intervalSeconds)
        testIterations = findViewById(R.id.testIterations)
        iterationWaitTime = findViewById(R.id.iterationWaitTime)
        checkboxDebug = findViewById(R.id.checkboxDebug)
        checkboxVerbose = findViewById(R.id.checkboxVerbose)
        bwLayout = findViewById(R.id.bwLayout)

        autoReduceCheckbox = findViewById<CheckBox>(R.id.autoReduceBandwidth)


        spinnerProtocol = findViewById(R.id.spinnerProtocol)
        testDirection = findViewById(R.id.testDirection)

        // Initialize SharedPreferences
        // 'this' refers to the Context of the Activity
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Restore values when the Activity is created
        restoreSavedValues()

        // Load spinner values from resources
        ArrayAdapter.createFromResource(
            this,
            R.array.test_direction_options,
            R.layout.spinner_item_white // Custom white-text layout
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_item_white)
            testDirection.adapter = adapter
        }

        // Optional: Set default selection to "Upload"
        testDirection.setSelection(0)


        // Load spinner values from resources
        ArrayAdapter.createFromResource(
            this,
            R.array.protocol_options,
            R.layout.spinner_item_white // Custom white-text layout
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_item_white)
            spinnerProtocol.adapter = adapter
        }

        // Optional: Set default selection to "Upload"
        spinnerProtocol.setSelection(0)

        spinnerProtocol.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                bwLayout.visibility =
                    if (position != 0 && position != 3) View.VISIBLE else View.GONE
                autoReduceCheckbox.visibility =
                    if (position != 0 && position != 3) View.VISIBLE else View.GONE
//                validateInputs()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }


        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                isAutoScrollEnabled = !isAutoScrollEnabled
                val msg =
                    if (isAutoScrollEnabled) "🔽 Auto-scroll enabled" else "🛑 Auto-scroll disabled"
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                return true
            }
        })

        @Suppress("ClickableViewAccessibility")
        scrollView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false // allow normal scroll behavior
        }

        // Start button action
        startBtn.setOnClickListener {
            if (startBtn.text.toString() == "New Test") {
                startBtn.text = "START TEST"
                resetTestUI()
                return@setOnClickListener
            }

            // Call the function to save values here
            saveCurrentValues()

            val iperfArgs = getIperfArguments() ?: return@setOnClickListener

            timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
//            logFile = createLogFile("iPerf3_$timestamp.txt")

            outputView.text = "" // ✅ Clear output view

            startBtn.isEnabled = false
            stopBtn.isEnabled = true
            stopBtn.visibility = View.VISIBLE

            outputLabel.visibility = View.VISIBLE
            outputLayout.visibility = View.VISIBLE


            optionsLayout.visibility = View.GONE
            wtLayout.visibility = View.GONE
            iterationLayout.visibility = View.GONE
            titleAdvancedSettings.visibility = View.GONE
            tdLayout.visibility = View.GONE
            intervalLayout.visibility = View.GONE
            psLayout.visibility = View.GONE
            bwLayout.visibility = View.GONE
            protocolLayout.visibility = View.GONE
            durationLayout.visibility = View.GONE
            portLayout.visibility = View.GONE
            hostLayout.visibility = View.GONE
            titleBasicSettings.visibility = View.GONE
            titleiperftest.visibility = View.GONE

            iperfManager = IperfTestManage(
                context = this,
                startBtn = startBtn,
                outputView = outputView,
                scrollView = scrollView,
                isAutoScrollEnabled = { isAutoScrollEnabled },
                timestamp = timestamp,
                runIperfLive = ::runIperfLive,
                forceStopIperfTest = ::forceStopIperfTest,
                startTimer = ::startTimer,
                stopTimer = ::stopTimer,
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

        // Stop button action
        stopBtn.setOnClickListener {
            iperfManager?.stopTests()
            resetTestUI()

        }
    }

    private fun resetTestUI() {
        outputLabel.visibility = View.GONE
        outputLayout.visibility = View.GONE

        optionsLayout.visibility = View.VISIBLE
        wtLayout.visibility = View.VISIBLE
        iterationLayout.visibility = View.VISIBLE
        titleAdvancedSettings.visibility = View.VISIBLE
        tdLayout.visibility = View.VISIBLE
        intervalLayout.visibility = View.VISIBLE
        psLayout.visibility = View.VISIBLE
        bwLayout.visibility = View.VISIBLE
        protocolLayout.visibility = View.VISIBLE
        durationLayout.visibility = View.VISIBLE
        portLayout.visibility = View.VISIBLE
        hostLayout.visibility = View.VISIBLE
        titleBasicSettings.visibility = View.VISIBLE
        titleiperftest.visibility = View.VISIBLE
    }

    // <-- Timer related functions
    private fun startTimer() {
        startTimeMillis = System.currentTimeMillis()
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTimeMillis
                val hours = (elapsed / 3600000).toInt()
                val minutes = (elapsed / 60000 % 60).toInt()
                val seconds = (elapsed / 1000 % 60).toInt()
                val formatted =
                    String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
                timerView.text = "⏱ Elapsed: $formatted"
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
    }

    // Timer related functions -->

    // Create or reuse a log file in Downloads directory
//    private fun createLogFile(fileName: String): File {
//        val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
//        val file = File(dir, fileName)
//        if (!file.exists()) file.createNewFile()
//        return file
//    }

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

        if (serverIp.isEmpty() || port == null || duration == null || interval == null
        ) {
            Toast.makeText(this, "Please fill all required fields correctly.", Toast.LENGTH_SHORT)
                .show()
            return null
        }

        val args = mutableListOf(
            "iperf3",
            "-c", serverIp,
            "-p", port.toString(),
            "-t", duration.toString(),
            "-i", interval.toString()
        )
        if (parallel != null) {
            args.add("-P")
            args.add(parallel.toString())
        }

        when (testDirection) {
            "Download (-R)" -> args.add("-R")
            "Bidirectional (--bidir)" -> args.add("--bidir")
        }

        if (protocol == "UDP" || protocol == "UDP Incremental Ramp-Up Test" || protocol == "Smart Ramp-Up Strategy") {
            if (bandwidth == null) {
                Toast.makeText(this, "Please enter valid bandwidth.", Toast.LENGTH_SHORT).show()
                return null
            }

            args.add("-u")
            args.add("-b")
            args.add("${bandwidth}M") // Add M suffix



            if (protocol == "UDP Incremental Ramp-Up Test") {
                isIncrementalRampUpTest = true
            } else if (protocol == "Smart Ramp-Up Strategy") {
                isSmartIncrementalRampUpTest = true
            }
        } else if (protocol == "TCP + UDP Hybrid Strategy") {
            isHybridTest = true
        }


        if (isDebug) args.add("-d")
        if (isVerbose) args.add("-V")

        return args.toTypedArray()
    }

    // --- Save Function ---
    private fun saveCurrentValues() {
        val editor = sharedPreferences.edit()

        // Save EditText values as Strings
        editor.putString(KEY_SERVER_IP, inputServerIp.text.toString())
        editor.putString(KEY_PORT, inputPort.text.toString())
        editor.putString(KEY_DURATION, inputDuration.text.toString())
        editor.putString(KEY_PARALLEL_STREAMS, parallelStreams.text.toString())
        editor.putString(KEY_INTERVAL_SECONDS, intervalSeconds.text.toString())
        editor.putString(KEY_TEST_ITERATIONS, testIterations.text.toString())
        editor.putString(KEY_ITERATION_WAIT_TIME, iterationWaitTime.text.toString())

        // Save CheckBox states as Booleans
        editor.putBoolean(KEY_DEBUG_CHECKBOX, checkboxDebug.isChecked)
        editor.putBoolean(KEY_VERBOSE_CHECKBOX, checkboxVerbose.isChecked)


        // This is the key change: using commit()
//        val saveSuccessful = editor.commit() // Blocks until write is complete
//        val saveStatus = if (saveSuccessful) "SUCCESSFUL" else "FAILED"
//        Toast.makeText(this, "saveStatus : ${saveStatus}", Toast.LENGTH_LONG).show()

    }

    // --- Restore Function ---
    private fun restoreSavedValues() {
        // Restore EditText values
        // getString(key, defaultValue) - defaultValue is returned if the key doesn't exist
        inputServerIp.setText(sharedPreferences.getString(KEY_SERVER_IP, ""))
        inputPort.setText(sharedPreferences.getString(KEY_PORT, "5201"))
        inputDuration.setText(sharedPreferences.getString(KEY_DURATION, "60"))
        parallelStreams.setText(sharedPreferences.getString(KEY_PARALLEL_STREAMS, "1"))
        intervalSeconds.setText(sharedPreferences.getString(KEY_INTERVAL_SECONDS, "1"))
        testIterations.setText(sharedPreferences.getString(KEY_TEST_ITERATIONS, "1"))
        iterationWaitTime.setText(sharedPreferences.getString(KEY_ITERATION_WAIT_TIME, "15"))

        // Restore CheckBox states
        // getBoolean(key, defaultValue) - defaultValue (false) for checkboxes is typical
        checkboxDebug.isChecked = sharedPreferences.getBoolean(KEY_DEBUG_CHECKBOX, false)
        checkboxVerbose.isChecked = sharedPreferences.getBoolean(KEY_VERBOSE_CHECKBOX, false)

//        displayAllSharedPreferences()
    }

    fun onInfoIconClick(view: View) {
        val context = view.context
        val message = when (view.id) {
            R.id.iconInfoIp -> context.getString(R.string.info_ip)
            R.id.iconInfoPort -> context.getString(R.string.info_port)
            R.id.iconInfoProtocol -> context.getString(R.string.info_protocol)
            R.id.iconInfoDuration -> context.getString(R.string.info_duration)
            R.id.iconInfoBandwidth -> context.getString(R.string.info_bandwidth)
            R.id.iconInfoParallelStreams -> context.getString(R.string.info_parallel_streams)
            R.id.iconInfoInterval -> context.getString(R.string.info_interval)
            R.id.iconInfoTestDirection -> context.getString(R.string.info_test_direction)
            R.id.iconInfoIterations -> context.getString(R.string.info_iterations)
            R.id.iconInfoWaitTime -> context.getString(R.string.info_wait_time)
            R.id.iconInfoOptions -> context.getString(R.string.info_options)
            else -> context.getString(R.string.info_default)
        }

        AlertDialog.Builder(context)
            .setTitle("Info")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }


}
