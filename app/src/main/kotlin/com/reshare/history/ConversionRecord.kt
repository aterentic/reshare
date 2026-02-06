package com.reshare.history

import com.reshare.converter.InputFormat
import com.reshare.converter.OutputFormat

data class ConversionRecord(
    val id: Long,
    val inputName: String,
    val inputFormat: InputFormat,
    val outputFormat: OutputFormat,
    val timestamp: Long,
    val outputPath: String,
    val sizeBytes: Long
)
