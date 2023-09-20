# Flutter Nude Photo Checker - A Flutter Package for Detecting Nude Images

![Flutter](https://img.shields.io/badge/Flutter-%5E3.13.3-blue.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

The `flutter_nude_checker` package provides a simple way to detect nude images in Flutter apps. It uses the [yahoo open_nsfw](https://github.com/yahoo/open_nsfw) model to detect nude images. The model is able to detect nudity with better accuracy.


## Features

- Detect nude images in Flutter apps
- Detect nudity with GPU acceleration
- Detect nudity in images from the internet or from the device's gallery



## Installation

1. Add this package to your `pubspec.yaml` file:

   ```yaml
   dependencies:
     flutter_nude_checker: ^1.0.0
   ```

2. Run `flutter pub get` to install the package.

## Usage
Download the tflite model from [here](https://github.com/xeron56/flutter_nude_checker/releases/download/1.0.0/nsfw.tflite) and add it to the assets folder of your project. Then, add the following code to your project to detect nude images:

```dart
  Future<void> _getNSFWScore() async {
    
    final modelPath = await getModelPath("nsfw.tflite");

    final picker = ImagePicker();
    try {
      final XFile? image = await picker.pickImage(source: ImageSource.gallery); // You can also use ImageSource.camera for the camera
      if (image != null) {
        final nsfwScore =
            await FlutterNudeChecker.getNSFWScore(image.path, modelPath);
        print("NSFW Score: ${nsfwScore.nsfwScore}");
       
      } else {
        print("No image selected.");
      }
    } catch (e) {
      print("Error: $e");
    }
  }
```

For more detailed usage and customization options, please refer to the [documentation](https://pub.dev/packages/flutter_nude_checker).

## Contributing

We welcome contributions! If you have any ideas, bug fixes, or improvements, please open an issue or submit a pull request on our [GitHub repository](https://github.com/xeron56/flutter_nude_checker).

## TODO

- [x] Add support for Android
- [ ] Add support for iOS

## License

This package is available under the MIT License. See the [LICENSE](LICENSE) file for more details.

## About

This package is developed and maintained by [MD. SHAHIDUL ISLAM](https://github.com/xeron56).

If you have any questions or need assistance, feel free to contact us at [shahidul1425@cseku.ac.bd](mailto:shahidul1425@cseku.ac.bd).
