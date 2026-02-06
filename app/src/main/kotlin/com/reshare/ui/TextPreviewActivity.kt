package com.reshare.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.reshare.R
import com.reshare.share.ShareHandler
import com.reshare.share.StorageSaver
import java.io.File

/**
 * Shows converted text in a selectable preview, allowing the user to share
 * a selection, the full text, or save the file via SAF.
 */
class TextPreviewActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var safLauncher: ActivityResultLauncher<Intent>

    private var fullText: String = ""
    private var filePath: String? = null
    private var mimeType: String = "text/plain"
    private var extension: String = "txt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_preview)

        textView = findViewById(R.id.text_content)

        safLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleSafResult(result.resultCode, result.data)
        }

        mimeType = intent.getStringExtra(EXTRA_MIME_TYPE) ?: "text/plain"
        extension = intent.getStringExtra(EXTRA_EXTENSION) ?: "txt"
        filePath = intent.getStringExtra(EXTRA_FILE_PATH)

        fullText = intent.getStringExtra(EXTRA_TEXT)
            ?: filePath?.let { File(it).readText(Charsets.UTF_8) }
            ?: run {
                finish()
                return
            }

        textView.text = fullText

        findViewById<Button>(R.id.btn_share_selection).setOnClickListener {
            shareSelection()
        }
        findViewById<Button>(R.id.btn_share_all).setOnClickListener {
            shareAll()
        }
        findViewById<Button>(R.id.btn_save_to).setOnClickListener {
            saveTo()
        }
    }

    private fun selectedText(): String {
        val start = textView.selectionStart
        val end = textView.selectionEnd
        return if (start >= 0 && end > start) {
            fullText.substring(start, end)
        } else {
            fullText
        }
    }

    private fun shareSelection() {
        ShareHandler(this).shareText(selectedText())
        finishAll()
    }

    private fun shareAll() {
        ShareHandler(this).shareText(fullText)
        finishAll()
    }

    private fun saveTo() {
        val suggestedName = "converted.$extension"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, suggestedName)
        }
        safLauncher.launch(intent)
    }

    private fun handleSafResult(resultCode: Int, data: Intent?) {
        val uri = data?.data
        if (resultCode != RESULT_OK || uri == null) {
            return
        }

        val saver = StorageSaver(contentResolver)
        val saved = saver.saveTextToUri(fullText, uri)

        if (saved) {
            Toast.makeText(this, R.string.save_success, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show()
        }
        finishAll()
    }

    private fun finishAll() {
        cleanupFile()
        setResult(RESULT_OK)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupFile()
    }

    private fun cleanupFile() {
        filePath?.let { File(it).delete() }
        filePath = null
    }

    companion object {
        const val EXTRA_TEXT = "com.reshare.extra.TEXT"
        const val EXTRA_FILE_PATH = "com.reshare.extra.FILE_PATH"
        const val EXTRA_MIME_TYPE = "com.reshare.extra.MIME_TYPE"
        const val EXTRA_EXTENSION = "com.reshare.extra.EXTENSION"

        /** Max size in bytes for passing text via intent extra. */
        private const val MAX_INTENT_TEXT_SIZE = 500_000

        fun newIntent(
            context: Context,
            file: File,
            mimeType: String,
            extension: String
        ): Intent {
            val text = file.readText(Charsets.UTF_8)
            return Intent(context, TextPreviewActivity::class.java).apply {
                putExtra(EXTRA_MIME_TYPE, mimeType)
                putExtra(EXTRA_EXTENSION, extension)
                if (text.toByteArray(Charsets.UTF_8).size <= MAX_INTENT_TEXT_SIZE) {
                    putExtra(EXTRA_TEXT, text)
                    file.delete()
                } else {
                    putExtra(EXTRA_FILE_PATH, file.absolutePath)
                }
            }
        }
    }
}
