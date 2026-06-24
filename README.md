# PixelCalc

PixelCalc is a small Android calculator experiment built to validate a fast
desktop-to-device Android development workflow.

## Features

- Pixel-style calculator UI built with Jetpack Compose
- Standard mode with basic arithmetic, constants, powers, square root, and
  radian-based trigonometric functions
- Matrix mode with multiplication, inverse, determinant, and result reuse via
  `C->A` / `C->B`
- Lightweight Kotlin calculation engines with unit tests

## Tech Stack

- Kotlin
- Jetpack Compose + Material 3
- Android Gradle Plugin 9.2.1
- Gradle 9.4.1
- Compile SDK: Android API 36.1

## Build

From the project root:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

The debug APK is copied to:

```text
app/build/outputs/apk/debug/PixelCalc-0.2.1-debug.apk
```

## Install With ADB

```powershell
adb install -r app/build/outputs/apk/debug/PixelCalc-0.2.1-debug.apk
```

## Status

This is an MVP prototype for learning and validating the Android pipeline, not a
production calculator.
