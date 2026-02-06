package com.reshare.ui

import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.reshare.R
import com.reshare.history.ConversionHistoryDb
import com.reshare.history.ConversionRecord
import com.reshare.share.ShareHandler
import java.io.File

class HistoryActivity : AppCompatActivity() {

    private lateinit var db: ConversionHistoryDb
    private lateinit var listView: ListView
    private lateinit var emptyText: TextView
    private lateinit var clearButton: Button
    private var records: List<ConversionRecord> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        title = getString(R.string.history_title)

        db = ConversionHistoryDb(this)
        listView = findViewById(R.id.list_history)
        emptyText = findViewById(R.id.text_empty)
        clearButton = findViewById(R.id.btn_clear_history)

        listView.setOnItemClickListener { _, _, position, _ ->
            reshare(records[position])
        }

        clearButton.setOnClickListener {
            confirmClear()
        }

        loadRecords()
    }

    private fun loadRecords() {
        records = db.queryAll()
        listView.adapter = HistoryAdapter()
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (records.isEmpty()) {
            listView.visibility = View.GONE
            emptyText.visibility = View.VISIBLE
            clearButton.visibility = View.GONE
        } else {
            listView.visibility = View.VISIBLE
            emptyText.visibility = View.GONE
            clearButton.visibility = View.VISIBLE
        }
    }

    private fun reshare(record: ConversionRecord) {
        val file = File(record.outputPath)
        if (file.exists()) {
            ShareHandler(this).shareFile(file, record.outputFormat)
        } else {
            Toast.makeText(this, R.string.history_file_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmClear() {
        AlertDialog.Builder(this)
            .setMessage(R.string.history_clear_confirm)
            .setPositiveButton(R.string.history_clear) { _, _ ->
                db.deleteAll()
                loadRecords()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private inner class HistoryAdapter : BaseAdapter() {

        override fun getCount(): Int = records.size

        override fun getItem(position: Int): ConversionRecord = records[position]

        override fun getItemId(position: Int): Long = records[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_history, parent, false)
            val record = records[position]

            view.findViewById<TextView>(R.id.text_input_name).text = record.inputName
            view.findViewById<TextView>(R.id.text_formats).text =
                "${record.inputFormat.name} -> ${record.outputFormat.name}"
            view.findViewById<TextView>(R.id.text_size).text = formatSize(record.sizeBytes)
            view.findViewById<TextView>(R.id.text_timestamp).text =
                DateUtils.getRelativeTimeSpanString(
                    record.timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )

            return view
        }
    }

    companion object {
        fun formatSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
                else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            }
        }
    }
}
