package com.abhishek.cellularlab

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import java.io.File

class MarkdownViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_markdown_viewer)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        val markdownView = findViewById<TextView>(R.id.markdownView)
        markdownView.movementMethod = android.text.method.LinkMovementMethod.getInstance()

        val filePath = intent.getStringExtra("filePath") ?: return

        val file = File(filePath)
        if (!file.exists()) {
            markdownView.text = "File not found!"
            return
        }

        val markdownText = file.readText()
        val cleanedMarkdown = markdownText
            .removePrefix("```markdown\n")
            .removeSuffix("\n```")
            .trim()
        val markwon = Markwon.builder(this)
            .usePlugin(io.noties.markwon.core.CorePlugin.create()) // optional but helpful
            .usePlugin(HtmlPlugin.create())
            .usePlugin(TablePlugin.create(this))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TaskListPlugin.create(this))
            .usePlugin(LinkifyPlugin.create())
            .build()
        markwon.setMarkdown(markdownView, cleanedMarkdown)
    }
}
