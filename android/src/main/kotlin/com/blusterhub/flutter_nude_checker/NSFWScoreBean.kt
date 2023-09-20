package com.blusterhub.flutter_nude_checker

import android.graphics.Bitmap
import java.lang.Exception
import java.nio.ByteBuffer

/**
 * [nsfwScore] NSFW score
 * [sfwScore] SFW score
 * [timeConsumingToLoadData] Time taken to load data
 * [timeConsumingToScanData] Time taken to scan data
 */
data class NSFWScoreBean(
    val nsfwScore: Float,
    val sfwScore: Float,
    val timeConsumingToLoadData: Long,
    val timeConsumingToScanData: Long
) {
    override fun toString(): String {
        return "nsfwScore: $nsfwScore, sfwScore: $sfwScore, TimeConsumingToLoadData: $timeConsumingToLoadData ms, TimeConsumingToScanData: $timeConsumingToScanData ms)"
    }
}

fun Boolean.ifTrue(onTrue: () -> Unit, onFalse: () -> Unit) {
    if (this) onTrue() else onFalse()
}

class NSFWException(message: String) : Exception(message)

data class ConvertBitmapResultBean(val imgData: ByteBuffer, val executionTime: Long)

fun Bitmap.getNSFWScore() = NSFWScanner.scanBitmapForNSFWScore(this)
