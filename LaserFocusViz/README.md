# Laser Focus Viz

An Android app that visualizes what a phone's autofocus/laser-assisted depth
sensor sees, in real time.

Debug APKs are built automatically by CI on every push — see the
"Build LaserFocusViz APK" GitHub Actions workflow artifacts.

## How it works

Android does not expose the raw laser-autofocus ranging value through a
public API. What *is* public, via Camera2, is:

1. **Depth-output cameras** — some phones expose a ToF (time-of-flight) or
   structured-light sensor (often the same hardware module marketed as
   "laser autofocus") through `CameraCharacteristics.REQUEST_AVAILABLE_
   CAPABILITIES_DEPTH_OUTPUT`. When present, the app opens a `DEPTH16`
   stream alongside the normal preview and decodes each 16-bit sample into
   a millimeter distance (low 13 bits) and a confidence value (top 3 bits),
   then renders it as a color-coded heat map (red = close, violet = far)
   overlaid on the live camera preview.
2. **Fallback: focus distance telemetry** — on devices without a depth
   camera (most phones with only a laser/PDAF autofocus assist), the app
   instead reads `CaptureResult.LENS_FOCUS_DISTANCE` every frame while
   continuous autofocus runs, and shows a live gauge of the current focus
   distance in meters. This isn't a full depth map — it's a single-point
   estimate of whatever the AF system is currently focused on — but it's
   the closest thing to "what the laser is measuring" that's exposed on
   that hardware.

The app auto-detects which mode to use at startup and shows a status banner
explaining which one is active.

## Project layout

- `CameraDepthController.kt` — Camera2 session/lifecycle management, DEPTH16
  decoding, and the depth-vs-focus-distance fallback logic.
- `DepthVisualizerView.kt` — custom `View` that renders either the color-coded
  depth bitmap or the focus-distance gauge.
- `MainActivity.kt` — permission handling and wiring between the camera
  controller and the preview/overlay views.

## Building

Open the `LaserFocusViz/` directory in Android Studio (Hedgehog or newer) and
let it sync — it will generate the Gradle wrapper automatically. To build
from the command line instead, generate the wrapper once with a local Gradle
install:

```
gradle wrapper --gradle-version 8.4
./gradlew assembleDebug
```

## Requirements

- Android 6.0 (API 23) or newer.
- A physical device with a camera (the depth heat map requires a device with
  a ToF/depth-output camera; other devices fall back to the focus-distance
  gauge). This won't produce meaningful output on an emulator.
