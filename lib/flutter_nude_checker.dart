import 'dart:async';
import 'package:flutter/services.dart';

class FlutterNudeChecker {
  static const MethodChannel _channel =
      const MethodChannel('com.blusterhub.flutter_nude_checker');

  static Future<NSFWScore> getNSFWScore(
      String imagePath, String modelpath) async {
    try {
      final Map<Object?, Object?> result = await _channel.invokeMethod(
          'getNSFWScore', {'imagePath': imagePath, 'modelPath': modelpath});

      final Map<String, dynamic> resultMap = Map<String, dynamic>.from(result);

      final NSFWScore nsfwScore = NSFWScore.fromJson(resultMap);
      return nsfwScore;
    } catch (e) {
      throw Exception('Error while getting NSFW score: $e');
    }
  }
}

class NSFWScore {
  final double nsfwScore;
  final double sfwScore;
  final int timeConsumingToLoadData;
  final int timeConsumingToScanData;

  NSFWScore({
    required this.nsfwScore,
    required this.sfwScore,
    required this.timeConsumingToLoadData,
    required this.timeConsumingToScanData,
  });

  factory NSFWScore.fromJson(Map<String, dynamic> json) {
    return NSFWScore(
      nsfwScore: json['nsfwScore'],
      sfwScore: json['sfwScore'],
      timeConsumingToLoadData: json['timeConsumingToLoadData'],
      timeConsumingToScanData: json['timeConsumingToScanData'],
    );
  }
}
