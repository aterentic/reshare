package com.reshare.history

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.reshare.converter.InputFormat
import com.reshare.converter.OutputFormat
import java.io.File

class ConversionHistoryDb(private val context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_INPUT_NAME TEXT NOT NULL,
                $COL_INPUT_FORMAT TEXT NOT NULL,
                $COL_OUTPUT_FORMAT TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_OUTPUT_PATH TEXT NOT NULL,
                $COL_SIZE_BYTES INTEGER NOT NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insert(record: ConversionRecord) {
        val values = ContentValues().apply {
            put(COL_INPUT_NAME, record.inputName)
            put(COL_INPUT_FORMAT, record.inputFormat.name)
            put(COL_OUTPUT_FORMAT, record.outputFormat.name)
            put(COL_TIMESTAMP, record.timestamp)
            put(COL_OUTPUT_PATH, record.outputPath)
            put(COL_SIZE_BYTES, record.sizeBytes)
        }
        writableDatabase.insert(TABLE_NAME, null, values)
    }

    fun queryAll(): List<ConversionRecord> {
        val records = mutableListOf<ConversionRecord>()
        val cursor = readableDatabase.query(
            TABLE_NAME, null, null, null, null, null,
            "$COL_TIMESTAMP DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val record = ConversionRecord(
                    id = it.getLong(it.getColumnIndexOrThrow(COL_ID)),
                    inputName = it.getString(it.getColumnIndexOrThrow(COL_INPUT_NAME)),
                    inputFormat = InputFormat.valueOf(it.getString(it.getColumnIndexOrThrow(COL_INPUT_FORMAT))),
                    outputFormat = OutputFormat.valueOf(it.getString(it.getColumnIndexOrThrow(COL_OUTPUT_FORMAT))),
                    timestamp = it.getLong(it.getColumnIndexOrThrow(COL_TIMESTAMP)),
                    outputPath = it.getString(it.getColumnIndexOrThrow(COL_OUTPUT_PATH)),
                    sizeBytes = it.getLong(it.getColumnIndexOrThrow(COL_SIZE_BYTES))
                )
                records.add(record)
            }
        }
        return records
    }

    fun deleteAll() {
        val historyDir = File(context.filesDir, HISTORY_DIR)
        if (historyDir.exists()) {
            historyDir.listFiles()?.forEach { it.delete() }
        }
        writableDatabase.delete(TABLE_NAME, null, null)
    }

    companion object {
        const val DATABASE_NAME = "conversion_history.db"
        const val DATABASE_VERSION = 1
        const val TABLE_NAME = "history"
        const val HISTORY_DIR = "history"

        const val COL_ID = "_id"
        const val COL_INPUT_NAME = "input_name"
        const val COL_INPUT_FORMAT = "input_format"
        const val COL_OUTPUT_FORMAT = "output_format"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_OUTPUT_PATH = "output_path"
        const val COL_SIZE_BYTES = "size_bytes"

        /**
         * Copies a converted file to filesDir/history/ with a unique name and returns the path.
         */
        fun copyToHistory(context: Context, sourceFile: File, outputFormat: OutputFormat): File {
            val historyDir = File(context.filesDir, HISTORY_DIR).also { it.mkdirs() }
            val destFile = File(historyDir, "${System.currentTimeMillis()}_${sourceFile.name}")
            sourceFile.copyTo(destFile, overwrite = true)
            return destFile
        }
    }
}
