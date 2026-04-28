# project.md - Core App Notes

## App Identity
- Name: Clipy2
- Path: D:\Code\Clipy2
- Type: android

## Core Features
- Document the main user-facing features here.
- Keep only important and core features (no minor details).
- Multi-clip video editing with trim, split, speed, volume, canvas, and filter controls.
- Export flow prepared for hardware-first rendering with MediaCodec and FFmpeg fallback.

## Core Architecture
- Document key modules/services and their responsibilities.
- Note important data flow and state management decisions.
- `:app` hosts Compose UI, media import, and editing state.
- `:video-editor` owns the editing pipeline contract: timeline session -> realtime preview plan -> OpenGL effect stage -> export plan.
- `ClipyAppState` remains the UI-facing source of truth and maps into `VideoEditorSession` through `VideoEditorBridge`.

## Important Decisions
- Record major behavior changes, breaking changes, and critical fixes.
- Video editing pipeline is introduced as a separate Android library module so preview/export engines can evolve without coupling them to Compose screens.

## Integrations
- APIs, SDKs, platform services, auth, and storage that are critical to the app.

## Reliability Notes
- Error handling, retry behavior, and recovery logic for critical paths.
