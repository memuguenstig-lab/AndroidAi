# AI File Agent

AI File Agent is a modern Android application that allows you to load local workspaces (directories or full SD card storage) and use AI models (local and cloud-based) to analyze and propose changes to the files. It uses the Gemini API, Groq, or on-device LLMs via MediaPipe.

## Features

- **Local Storage Access:** Load a directory or the entire external storage to view text and source code files.
- **AI File Analysis:** Select a local workspace and prompt the AI to analyze, modify, or rewrite files directly.
- **Local Model Support:** Download and use local models through MediaPipe GenAI Tasks. Supports Gemma, Phi-2, Falcon, Llama, and Mistral directly on the device!
- **Cloud Models:** Connect to Gemini or Groq using your API keys.

## Getting Started

1. Download the latest APK from the [Releases](https://github.com/) page or clone the repository.
2. Build the project using Android Studio or Gradle:
   ```bash
   ./gradlew assembleDebug
   ```
3. Open the app on your Android device.

## Setup

For **Cloud Models (Gemini/Groq):**
- Open the Settings menu in the app.
- Provide your API keys.

For **Local Models:**
- Open the Settings menu in the app.
- Choose a model from the list of available local models.
- Tap "Download" to fetch the `.task` file directly to your device.
- Wait for the download to finish, then select it.

## Permissions

The app requires the "All Files Access" (Manage External Storage) permission to efficiently access source code and text files across your entire device. You will be prompted to grant this permission on the first launch on Android 11+.

## Architecture

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Local LLMs:** MediaPipe Tasks GenAI
- **Networking:** Retrofit & OkHttp

## License

This project is open-source.
