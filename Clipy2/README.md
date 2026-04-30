# Clipy

Clipy is an Android video editing app built with Kotlin and Jetpack Compose. The project is split into small Android library modules so UI, editor orchestration, media operations, and GPU filters can evolve independently.

## Project Map

| Module | Responsibility |
| --- | --- |
| `:app` | Application shell, Compose navigation, home/export screens, media picker entry points, and bridges from app state to editor/media modules. |
| `:video-editor` | UI-facing video editor contracts, editor screen/view model, timeline models, preview planning, and export planning. |
| `:media` | Media import, metadata, timeline editing, preview control, transcoding, rendering, audio processing, cache, diagnostics, and media session orchestration. |
| `:core` | Shared Android/Kotlin primitives that are not tied to app or editor UI. |
| `:gpuimage` | Local fork of Android GPUImage under `gpuimage-core`, including OpenGL filters and native build configuration. |
| `:editor` | Legacy or experimental editor/filter UI code. Avoid adding new production video-editor code here unless intentionally reviving this module. |

## Runtime Flow

1. `MainActivity` enables edge-to-edge rendering and hosts `ClipyApp`.
2. `ClipyApp` owns top-level Compose routing between home, editor, and export screens.
3. `ClipyAppState` stores the UI-facing project state: clips, selected tool, aspect preset, volumes, and navigation state.
4. `VideoEditorBridge` converts `ClipyAppState` into `VideoEditorSession` and asks `DefaultVideoEditorOrchestrator` for preview/export plans.
5. `DefaultMediaModuleFactory` wires media services used by import, metadata, preview, transcode, render, audio, cache, diagnostics, and sessions.

## Build And Test

Use the Gradle wrapper from the project root.

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :media:testDebugUnitTest
.\gradlew.bat testDebugUnitTest
```

`media/libs/ffmpeg-kit-full-gpl-5.1.LTS-16K-full.aar` is required by `:media` and consumed by `:app` through the local file tree dependency.

## Development Rules

- Keep new editor product work in `:app`, `:video-editor`, and `:media` unless there is a clear reason to touch `:editor`.
- Keep UI state transformations in `ClipyAppState` and bridge conversions in `VideoEditorBridge`; do not duplicate session mapping in screens.
- Keep media engine implementation details inside `:media`; expose behavior through contracts/factories rather than direct service construction from UI.
- Keep generated outputs out of git. Module-level `build/`, `.gradle/`, `.cxx/`, and `.externalNativeBuild/` directories are ignored.
- Prefer focused module tests for media/session/render behavior before broad app builds.

## Current Notes

- Root project name is `Clipy`; the workspace folder is `Clipy2`.
- The git working tree may contain generated build artifacts from previous local builds. Do not treat those as source changes.
