# Development Guide

## Common Commands

Run commands from the project root.

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :media:testDebugUnitTest
.\gradlew.bat testDebugUnitTest
.\gradlew.bat clean
```

## Before Changing Code

- Check which module owns the behavior before editing.
- Read nearby tests first when changing `:media` behavior.
- Keep changes small and module-local unless a boundary change is required.
- Do not modify generated build output under any `build/` directory.

## Before Committing

- Review `git status --short` and ignore generated artifacts.
- Run the narrowest relevant test first, then a broader build when practical.
- For media/session/render changes, prefer `:media:testDebugUnitTest`.
- For UI or wiring changes, run `:app:assembleDebug` when the Android SDK setup is available.

## Dependency Notes

- Versions are centralized in `gradle/libs.versions.toml` for Kotlin DSL modules.
- `:gpuimage` uses a Groovy build file because it wraps the local GPUImage fork.
- `:media` expects `media/libs/ffmpeg-kit-full-gpl-5.1.LTS-16K-full.aar` to exist locally.
