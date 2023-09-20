package com.blusterhub.flutter_nude_checker

import android.content.Context
import androidx.annotation.NonNull
import com.blusterhub.flutter_nude_checker.NSFWScanner
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel


/** FlutterNudeCheckerPlugin */
class FlutterNudeCheckerPlugin: FlutterPlugin, MethodChannel.MethodCallHandler {
  private lateinit var channel : MethodChannel
  private lateinit var context: Context

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "com.blusterhub.flutter_nude_checker")
    channel.setMethodCallHandler(this)
    //context
    context = flutterPluginBinding.applicationContext
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
    if (call.method == "getNSFWScore") {
      val imagePath = call.argument<String>("imagePath")
      val modelPath = call.argument<String>("modelPath")
      if (imagePath != null) {
        try {
          NSFWScanner.initialize(
            context = context,
            modelPath = modelPath,
            enableGpuAcceleration = true,
            numThreads = 4,
          )
          val nsfwScore = NSFWScanner.scanFileForNSFWScore(imagePath)
          val response = mapOf(
            "nsfwScore" to nsfwScore.nsfwScore,
            "sfwScore" to nsfwScore.sfwScore,
            "timeConsumingToLoadData" to nsfwScore.timeConsumingToLoadData,
            "timeConsumingToScanData" to nsfwScore.timeConsumingToScanData
          )
          print(response)
          result.success(response)
        } catch (e: Exception) {
          result.error("NSFW_ERROR", e.message, null)
        }
      } else {
        result.error("INVALID_ARGUMENT", "Invalid arguments", null)
      }
    } else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }
}
