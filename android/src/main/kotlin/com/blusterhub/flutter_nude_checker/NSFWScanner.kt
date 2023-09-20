package com.blusterhub.flutter_nude_checker

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.*
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.text.DecimalFormat

object NSFWScanner {

    private var applicationContext: Context? = null
    private lateinit var modelInterpreter: Interpreter

    private const val INPUT_WIDTH = 224
    private const val INPUT_HEIGHT = 224
    private var isDebugLogEnabled = false

    fun initialize(
        context: Context,
        modelPath: String? = null,
        enableGpuAcceleration: Boolean = true,
        numThreads: Int = 4
    ) {
        applicationContext?.let {
            logDebug("NSFWScanner has already been initialized. Skipping this initialization.")
            return
        }

        applicationContext = context

        val interpreterOptions = getInterpreterOptions(enableGpuAcceleration, numThreads)

        if (modelPath.isNullOrEmpty()) {
            logDebug("No model path provided. Attempting to load the 'nsfw.tflite' model from assets.")
            try {
                val assetFileDescriptor = applicationContext!!.assets.openFd("nsfw.tflite")
                val fileDescriptor = assetFileDescriptor.fileDescriptor
                val fileChannel = FileInputStream(fileDescriptor).channel
                val modelByteBuffer = fileChannel.map(
                    FileChannel.MapMode.READ_ONLY,
                    assetFileDescriptor.startOffset,
                    assetFileDescriptor.declaredLength
                )

                modelInterpreter = Interpreter(modelByteBuffer, interpreterOptions)
                logDebug("Loaded the model file successfully from assets.")
            } catch (fileNotFoundException: FileNotFoundException) {
                applicationContext = null
                logError("Failed to load the 'nsfw.tflite' model from assets.")
                throw NSFWException("Failed to load the 'nsfw.tflite' model from assets.")
            }
        } else {
            logDebug("Attempting to load the model from the provided path.")
            try {
                val modelFile = File(modelPath)
                if (modelFile.exists()) {
                    modelInterpreter = Interpreter(modelFile, interpreterOptions)
                    logDebug("Model loaded successfully from the provided path.")
                } else {
                    applicationContext = null
                    logError("Model file not found.")
                    throw FileNotFoundException("Model file not found at path: '$modelPath'")
                }
            } catch (e: Exception) {
                applicationContext = null
                logError("Error loading the model file.")
                throw NSFWException("Failed to load the model file at path: '$modelPath'")
            }
        }

        logDebug("NSFWScanner initialized successfully! GPU acceleration is ${if (enableGpuAcceleration) "enabled" else "disabled"}.")
    }

    fun enableDebugLog() {
        isDebugLogEnabled = true
    }

    private fun logDebug(content: String) {
        if (isDebugLogEnabled) Log.d(NSFWScanner::class.java.simpleName, content)
    }

    private fun logError(content: String) {
        if (isDebugLogEnabled) Log.e(NSFWScanner::class.java.simpleName, content)
    }

    private fun getInterpreterOptions(enableGpuAcceleration: Boolean, numThreads: Int): Interpreter.Options {
        return Interpreter.Options().apply {
            setNumThreads(numThreads)
            if (enableGpuAcceleration) {
                addDelegate(GpuDelegate())
                setAllowBufferHandleOutput(true)
                setAllowFp16PrecisionForFp32(true)
            }
        }
    }

    fun scanFileForNSFWScore(file: File): NSFWScoreBean {
        return scanBitmapForNSFWScore(BitmapFactory.decodeFile(file.path))
    }

    fun scanFileForNSFWScore(file: File, onResult: (NSFWScoreBean) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            val result = scanBitmapForNSFWScore(BitmapFactory.decodeFile(file.path))
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    fun scanFileForNSFWScore(filePath: String): NSFWScoreBean {
        return scanBitmapForNSFWScore(BitmapFactory.decodeFile(filePath))
    }

    fun scanFileForNSFWScore(filePath: String, onResult: (NSFWScoreBean) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            val result = scanBitmapForNSFWScore(BitmapFactory.decodeFile(filePath))
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    fun scanBitmapForNSFWScore(bitmap: Bitmap): NSFWScoreBean {
        applicationContext?.let {
            val startTime = SystemClock.uptimeMillis()
            ByteArrayOutputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                stream.close()
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
                val result = convertBitmapToByteBuffer(resizedBitmap)
                val output = Array(1) { FloatArray(2) }
                synchronized(this@NSFWScanner) {
                    modelInterpreter.run(result.imgData, output)
                    val decimalFormat = DecimalFormat("0.000")
                    return NSFWScoreBean(
                        decimalFormat.format(output[0][1]).toFloat(),
                        decimalFormat.format(output[0][0]).toFloat(),
                        result.executionTime,
                        SystemClock.uptimeMillis() - startTime
                    ).also {
                        logDebug("Scanning completed ($result) -> $it")
                    }
                }
            }
        }
        throw NSFWException("Please call NSFWScanner.initialize(...) before scanning.")
    }

    fun scanBitmapForNSFWScore(bitmap: Bitmap, onResult: (NSFWScoreBean) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            val result = scanBitmapForNSFWScore(bitmap)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    private data class ConvertBitmapResult(
        val imgData: ByteBuffer,
        val executionTime: Long
    )

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ConvertBitmapResult {
        val imgData = ByteBuffer.allocateDirect(1 * INPUT_WIDTH * INPUT_HEIGHT * 3 * 4)
        imgData.order(ByteOrder.LITTLE_ENDIAN)

        val startTime = SystemClock.uptimeMillis()
        imgData.rewind()
        val intValues = IntArray(INPUT_WIDTH * INPUT_HEIGHT)
        bitmap.getPixels(
            intValues,
            0,
            INPUT_WIDTH,
            Math.max((bitmap.height - INPUT_HEIGHT) / 2, 0),
            Math.max((bitmap.width - INPUT_WIDTH) / 2, 0),
            INPUT_WIDTH,
            INPUT_HEIGHT
        )
        for (color in intValues) {
            imgData.putFloat((Color.blue(color) - 104).toFloat())
            imgData.putFloat((Color.green(color) - 117).toFloat())
            imgData.putFloat((Color.red(color) - 123).toFloat())
        }
        return ConvertBitmapResult(imgData, SystemClock.uptimeMillis() - startTime)
    }
}
