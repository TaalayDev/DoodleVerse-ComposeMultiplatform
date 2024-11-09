# DoodleVerse

DoodleVerse is a versatile, cross-platform drawing application built with Kotlin Multiplatform and Jetpack Compose. It offers a rich set of drawing tools and features, making it suitable for both casual sketching and more complex digital art projects.

## Features

- **Cross-platform support**: Runs on Android, iOS, Web, and Desktop.
- **Multiple brush types**: Including pencil, marker, watercolor, and various creative brushes.
- **Shape tools**: Rectangle, circle, line, arrow, ellipse, and polygon.
- **Layer support**: Create, manage, and organize your artwork in layers.
- **Color picker**: Advanced color selection with opacity control.
- **Undo/Redo functionality**: Easily correct mistakes or revisit previous states.
- **Project management**: Create, save, and edit multiple projects.
- **Customizable canvas size**: Choose from preset sizes or create custom dimensions.

## Getting Started

### Prerequisites

- JDK 11 or later
- Android Studio Arctic Fox or later (for Android development)
- Xcode 12 or later (for iOS development)
- Gradle 7.0 or later

### Setup

1. Clone the repository:
   ```
   git clone https://github.com/TaalayDev/DoodleVerse-ComposeMultiplatform.git
   ```

2. Open the project in Android Studio or your preferred IDE.

3. Sync the Gradle files.

4. Run the desired target:
  - For Android: Run the `androidApp` configuration.
  - For Desktop: Run the `desktopApp` configuration.
  - For iOS: Open the Xcode project in the `iosApp` folder and run it.
  - For Web: Run the `wasmJsBrowserDevelopmentRun` Gradle task.

## Project Structure

- `/composeApp`: Contains the shared Kotlin code for all platforms.
  - `commonMain`: Code shared across all platforms.
  - `androidMain`, `iosMain`, `desktopMain`, `webMain`: Platform-specific code.
- `/iosApp`: iOS-specific code and project files.

## Key Components

- `DrawingScreen.kt`: Main screen for the drawing interface.
- `DrawViewModel.kt`: ViewModel managing the drawing state and operations.
- `BrushData.kt`: Defines various brush types and their behaviors.
- `DrawCanvas.kt`: Core drawing canvas implementation.
- `LayersPanel.kt`: UI for managing layers.
- `ColorPicker.kt`: Color selection component.

## Contributing

Contributions to DoodleVerse are welcome! Please follow these steps:

1. Fork the repository.
2. Create a new branch for your feature or bug fix.
3. Make your changes and commit them with descriptive commit messages.
4. Push your changes to your fork.
5. Submit a pull request to the main repository.

Please ensure your code adheres to the existing style and includes appropriate tests.

## License

This project is licensed under the MIT License - You can search for the license on the web.

## Acknowledgments

- Jetpack Compose for the UI framework.
- Kotlin Multiplatform for enabling cross-platform development.

## Contact

For any queries or suggestions, please open an issue in the GitHub repository or contact TaalayDev at a.u.taalay@gmail.com.

## Screenshots

![Example](screenshots/Screenshot_1.png)