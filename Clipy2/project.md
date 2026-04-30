# project.md - Clipy Project Notes

## App Identity
- Name: Clipy
- Workspace folder: Clipy2
- Type: Android video editor
- Stack: Kotlin, Jetpack Compose, Gradle, AndroidX Media3, GPUImage/OpenGL, FFmpeg Kit fallback

## Core Features
- Multi-clip video editing with trim, split, speed, volume, canvas, and filter controls.
- Export flow prepared for hardware-first rendering with MediaCodec and FFmpeg fallback.
- Home flow for starting feature-specific edit sessions and continuing an existing project.
- Export screen backed by the editor export plan.

## Core Architecture
- `:app` hosts Compose UI, media import entry points, top-level routing, and editing state.
- `:video-editor` owns editor-facing contracts, editor screens/state, and the editing pipeline contract: timeline session -> realtime preview plan -> OpenGL effect stage -> export plan.
- `:media` owns import, metadata, timeline operations, preview control, transcode, render, audio processing, cache, diagnostics, and media session orchestration.
- `:gpuimage` is a local GPUImage/OpenGL module backed by `gpuimage-core`.
- `:core` is reserved for shared primitives that are not tied to app or editor UI.
- `:editor` contains legacy or experimental editor/filter code; new production video-editor work should normally go to `:video-editor`.
- `ClipyAppState` is the UI-facing source of truth and maps into `VideoEditorSession` through `VideoEditorBridge`.
- `DefaultVideoEditorOrchestrator` normalizes editor sessions and creates preview/export plans.
- `DefaultMediaModuleFactory` wires the media services used at runtime.

## Important Decisions
- Video editing pipeline is introduced as a separate Android library module so preview/export engines can evolve without coupling them to Compose screens.
- Module build outputs are ignored at every module level to avoid committing generated Android/Gradle artifacts.
- `media/libs/ffmpeg-kit-full-gpl-5.1.LTS-16K-full.aar` is a required local dependency.

## Integrations
- Android Photo Picker via `ActivityResultContracts.PickMultipleVisualMedia`.
- AndroidX Media3/ExoPlayer for preview control in `:media`.
- Android MediaCodec as the preferred export path in editor plans.
- FFmpeg Kit as software fallback for transcode/render paths.
- GPUImage/OpenGL for visual effects.

## Reliability Notes
- Keep generated `build/`, `.gradle/`, `.cxx/`, and `.externalNativeBuild/` artifacts out of reviews and commits.
- Prefer `:media:testDebugUnitTest` for media/session/render regression checks.
- Run `:app:assembleDebug` before validating an end-to-end Android build.
