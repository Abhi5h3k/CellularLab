package com.abhishek.cellularlab.adapter

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.abhishek.cellularlab.BuildConfig
import com.abhishek.cellularlab.MarkdownViewerActivity
import com.abhishek.cellularlab.R
import com.abhishek.cellularlab.model.LogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException

//region Adapter Definition
/**
 * RecyclerView Adapter for displaying a list of LogEntry items.
 * Handles item clicks, sharing, and deletion.
 *
 * @param logs Mutable list of log entries to display.
 * @param onShare Callback invoked when a log file is to be shared.
 * @param onOpen Callback invoked when a log file is to be opened.
 */
//private val GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY
private const val MODEL = "gemma-3n-e2b-it"
private const val MAX_LOG_CHARS = 20000  // Roughly ~5,000 tokens

private const val PREFS_NAME = "AiConfig"
private const val PREF_KEY_GEMINI = "geminiApiKey"
private const val PREF_KEY_MODEL = "geminiModel"

class HistoryAdapter(
    private val logs: MutableList<LogEntry>,
    private val onShare: (File) -> Unit,
    private val onOpen: (File) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.LogViewHolder>() {


    //region ViewHolder
    /**
     * ViewHolder for a single log entry item.
     * Holds references to the views for each data item.
     */
    inner class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timestamp: TextView = view.findViewById(R.id.resultTimestamp)
        val ip: TextView = view.findViewById(R.id.resultIp)
        val resultSummary: TextView = view.findViewById(R.id.resultSummary)
        val resultIcon: TextView = view.findViewById(R.id.resultIcon)
        val btnMore: ImageView = view.findViewById(R.id.btnMore)
    }
    //endregion

    //region Data Update
    /**
     * Updates the adapter's data set with new log entries.
     * Clears the current list and adds all new entries, then refreshes the view.
     *
     * @param newEntries List of new LogEntry objects.
     */
    fun updateData(newEntries: List<LogEntry>) {
        logs.clear()
        logs.addAll(newEntries)
        notifyDataSetChanged()
    }
    //endregion

    fun getEffectiveGeminiKey(context: android.content.Context): String {
        return if (BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            BuildConfig.GEMINI_API_KEY
        } else {
            val prefs =
                context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            prefs.getString(PREF_KEY_GEMINI, "") ?: ""
        }
    }


    //region Adapter Lifecycle Methods

    /**
     * Inflates the item layout and creates a ViewHolder.
     *
     * @param parent The parent ViewGroup.
     * @param viewType The type of the new view.
     * @return A new LogViewHolder instance.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log_entry, parent, false)
        return LogViewHolder(view)
    }

    /**
     * Binds data to the ViewHolder at the given position.
     * Sets up click listeners for item and menu actions.
     *
     * @param holder The ViewHolder to bind data to.
     * @param position The position of the item in the data set.
     */
    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val entry = logs[position]

        // Bind log entry data to views
        holder.timestamp.text = entry.timestamp
        holder.ip.text = entry.ip
        holder.resultSummary.text = entry.summaryText
        holder.resultIcon.text = entry.icon

        // Handle item click: open the log file
        holder.itemView.setOnClickListener { onOpen(entry.file) }

        // Handle "more" button click: show popup menu with actions
        holder.btnMore.setOnClickListener { anchor ->
            // Debug
            //Toast.makeText(anchor.context, "Key: ${BuildConfig.GEMINI_API_KEY} , Is blank : ${GEMINI_API_KEY.isBlank()}", Toast.LENGTH_LONG).show()

            // Use a themed context for the popup menu
            val themedCtx = ContextThemeWrapper(anchor.context, R.style.CustomPopupMenu)
            val popup = PopupMenu(themedCtx, anchor)
            popup.menuInflater.inflate(R.menu.history_item_menu, popup.menu)

            // Force show icons in the popup menu using reflection (Android quirk)
            try {
                val field = popup.javaClass.getDeclaredField("mPopup")
                field.isAccessible = true
                val menu = field.get(popup)
                val method = menu.javaClass.getDeclaredMethod(
                    "setForceShowIcon",
                    Boolean::class.javaPrimitiveType
                )
                method.invoke(menu, true)
            } catch (_: Exception) {
                // Ignore reflection errors
            }

            // Handle popup menu item clicks
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_open -> onOpen(entry.file) // Open log
                    R.id.menu_share -> onShare(entry.file) // Share log
                    R.id.menu_delete -> {
                        // Show confirmation dialog before deleting
                        AlertDialog.Builder(anchor.context)
                            .setTitle("Delete Log")
                            .setMessage("Are you sure you want to delete this log file?")
                            .setPositiveButton("Delete") { _, _ ->
                                val pos = holder.bindingAdapterPosition
                                if (pos != RecyclerView.NO_POSITION) {
                                    val fileToDelete = logs[pos].file
                                    // Attempt to delete the file
                                    if (fileToDelete.delete()) {
                                        logs.removeAt(pos)
                                        notifyItemRemoved(pos)
                                    } else {
                                        Toast.makeText(
                                            anchor.context,
                                            "Failed to delete file",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }

                    R.id.menu_ai_analyze -> {
                        val geminiKey = getEffectiveGeminiKey(anchor.context)
                        if (geminiKey.isBlank()) {
                            Toast.makeText(anchor.context, "AI key not set", Toast.LENGTH_SHORT)
                                .show()
                            showUpdateApiKeyDialog(anchor)
                            return@setOnMenuItemClickListener true
                        }

                        val logFile = entry.file
                        val aiFile = File(logFile.parent, "AI_${logFile.name}")

                        if (aiFile.exists()) {
                            //onOpen(aiFile)
                            val intent = Intent(anchor.context, MarkdownViewerActivity::class.java)
                            intent.putExtra("filePath", aiFile.absolutePath)
                            anchor.context.startActivity(intent)
                            return@setOnMenuItemClickListener true
                        }

                        val logText = logFile.readText()
                        if (logText.length > MAX_LOG_CHARS) {
                            Toast.makeText(
                                anchor.context,
                                "Log too long for AI analysis",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnMenuItemClickListener true
                        }

                        val promptText = """
You are a professional network engineer and a technical writer.

Analyze the following iperf3 log and generate a concise, structured report in **Markdown format** with these sections:

---

## 📊 iPerf3 Test Analysis

### 1. **Test Summary**
- Identify and mention:
  - Protocol: TCP or UDP
  - Direction: Upload / Download / Bidirectional (if clear from the command)
  - Parallel streams (e.g., -P value)
  - Requested bandwidth (e.g., -b value if UDP)
  - Duration (e.g., -t value)
  - Client IP and Server IP
  - Port used

### 2. **Performance Observations**
- Summarize what the log shows:
  - Bandwidth values
  - Retransmissions (Retr column)
  - Packet loss (for UDP)
  - Jitter (for UDP)
  - General stability or variance

### 3. **Detected Issues**
- Point out:
  - If retransmissions are present
  - If there's significant packet loss
  - If bandwidth is highly inconsistent

If no major issues, say so clearly.

### 4. **Recommendations**
- Suggest network tuning, retrying with different parameters, or infrastructure checks **only if issues are seen**.

### 5. **Quality Rating**
- One of: Excellent / Good / Fair / Poor
- Keep it short and justify the rating.

---

**Format your response as raw Markdown** — no code blocks, no triple backticks. Use **bullet points** and **bold** labels for clarity.
Respond in Markdown without wrapping it in a markdown code block (i.e., no ```markdown). Just raw markdown. Use Emojis when appropriate.

---

**Log:**  
$logText
""".trimIndent()


                        // Async HTTP call
                        Toast.makeText(
                            anchor.context,
                            "Sending log to AI for analysis...",
                            Toast.LENGTH_SHORT
                        ).show()
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val response = sendGeminiRequest(anchor.context, promptText)
                                aiFile.writeText(response)

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        anchor.context,
                                        "AI analysis complete",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    //onOpen(aiFile)
                                    val intent =
                                        Intent(anchor.context, MarkdownViewerActivity::class.java)
                                    intent.putExtra("filePath", aiFile.absolutePath)
                                    anchor.context.startActivity(intent)
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    AlertDialog.Builder(anchor.context)
                                        .setTitle("AI Analysis Failed")
                                        .setMessage(e.message ?: "Something went wrong.")
                                        .setPositiveButton("OK", null)
                                        .setNegativeButton("Update API Key") { _, _ ->
                                            showUpdateApiKeyDialog(anchor)
                                        }
                                        .show()
                                }
                                Log.e("AI_ANALYSIS", "Failed to analyze log", e)

                            }
                        }
                    }

                }
                true
            }

            popup.show()
        }
    }

    private fun showUpdateApiKeyDialog(view: View) {
        val context = view.context
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentKey = prefs.getString(PREF_KEY_GEMINI, "") ?: ""
        val currentModel = prefs.getString(PREF_KEY_MODEL, MODEL) // fallback to default

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_api_key_input, null)
        val inputEditText = dialogView.findViewById<EditText>(R.id.apiKeyEditText)
        val modelEditText = dialogView.findViewById<EditText>(R.id.modelEditText)

        inputEditText.setText(currentKey)
        modelEditText.setText(currentModel)

        val dialog = AlertDialog.Builder(context, R.style.CustomAlertDialog)
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newKey = inputEditText.text.toString().trim()
                val newModel = modelEditText.text.toString().trim()
                prefs.edit()
                    .putString(PREF_KEY_GEMINI, newKey)
                    .putString(PREF_KEY_MODEL, newModel)
                    .apply()
                Toast.makeText(context, "API key & model updated", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        // Force button text color if needed
        val color = ContextCompat.getColor(context, R.color.colorTextPrimary)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(color)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(color)
    }


    /**
     * Returns the total number of log entries.
     *
     * @return The size of the logs list.
     */
    override fun getItemCount() = logs.size

    //endregion

    suspend fun sendGeminiRequest(context: Context, prompt: String): String {
        val GEMINI_API_KEY = getEffectiveGeminiKey(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val model = prefs.getString(PREF_KEY_MODEL, MODEL) ?: MODEL
        val json = """
        {
          "contents": [
            {
              "parts": [
                { "text": ${JSONObject.quote(prompt)} }
              ]
            }
          ]
        }
    """.trimIndent()

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

        val client = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS) // increase if needed
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("X-goog-api-key", GEMINI_API_KEY)
            .post(json.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Unexpected code $response")

        val body = response.body?.string() ?: throw IOException("Empty response")

        val root = JSONObject(body)
        val candidates = root.getJSONArray("candidates")
        if (candidates.length() == 0) throw IOException("No candidates")

        val content = candidates.getJSONObject(0).getJSONObject("content")
        val parts = content.getJSONArray("parts")
        return parts.getJSONObject(0).getString("text").trim()
    }


}
//endregion

/*
Explanation:
- The adapter manages a list of log entries and binds them to a RecyclerView.
- Each item shows log details and a "more" button for actions (open, share, delete).
- Deletion is confirmed with a dialog and updates the list on success.
- Reflection is used to force icons in the popup menu due to Android limitations.
- Regions and comments are added for clarity and maintainability.
*/