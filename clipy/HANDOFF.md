# HANDOFF - Clipy (for next Codex session)

## 1) Non-negotiable architecture (from user)
Use exactly **3 real Gradle modules** only:
- `:app`
- `:edit`
- `:export`

Do NOT create extra modules like `:core:*`, `:feature:*`, `:future:*`.

Dependency direction:
- `:app` -> `:edit`, `:export`
- `:export` -> `:edit` (only when needed)
- `:edit` must not depend on `:app` or `:export`

App id / namespace requirements:
- `applicationId = "com.nantcompany.clipy"`
- `namespace = "com.nantcompany.clipy"` (app)
- root package `com.nantcompany.clipy`

## 2) Current repository status (as of 2026-05-08)
Project root: `D:\Code\Coding\Clipy`

### Existing modules
- `:app` exists
- `:edit` exists
- `:export` exists

### Gradle files currently
- `settings.gradle.kts`: includes `:app`, `:edit`, `:export`
- `build.gradle.kts`: applies false for `android-application`, `android-library`, `kotlin-compose`
- `app/build.gradle.kts`: application + compose plugin, depends on `:edit` and `:export`
- `edit/build.gradle.kts`: android-library
- `export/build.gradle.kts`: android-library, depends on `:edit`, includes fileTree libs for `*.aar/*.jar`
- `gradle/libs.versions.toml`: contains lifecycle-viewmodel-compose alias and plugin aliases

### FFmpeg library copied
Copied from `D:\Code\Coding\Clipy2\media\libs` to:
- `export/libs/ffmpeg-kit-full-gpl-5.1.LTS-16K-full.aar`

## 3) Source code status

### :app
`app/src/main/java/com/nantcompany/clipy/...` has many scaffold screens/viewmodels already created:
- app: `ClipyApp`, `ClipyApplication`, `HiltSetup`
- navigation: `AppRoute`, `NavigationState`, `RootNavigatorViewModel`
- theme/design
- home/picker/tools(cut/compress/merge/extractaudio/slideshow)
- processing/result/history/settings/future

Main activity:
- `app/src/main/java/com/nantcompany/clipy/MainActivity.kt`
Manifest:
- `app/src/main/AndroidManifest.xml` uses `.app.ClipyApplication`

### :edit
Currently only one file exists:
- `edit/src/main/java/com/nantcompany/clipy/edit/common/ValidationResult.kt`

### :export
`export/src/main/java` folder not created yet.
No export Kotlin classes yet.

## 4) Known blocker from previous build attempts
Running Gradle wrapper failed with:
`FileNotFoundException ... gradle-9.1.0-bin.zip.lck (Access is denied)`

Likely another process (IDE/daemon) locking wrapper distribution file.

## 5) What next Codex must do (ordered)
1. Ensure Gradle lock issue is cleared (close conflicting Gradle process / retry command).
2. Build and fix compile errors in `:app` scaffold (very likely unresolved imports to :edit/:export classes not yet created).
3. Implement minimal but complete placeholders in `:edit` package tree:
   - media model/metadata/cache/thumbnail/validator
   - tool request + validators for cut/compress/merge/extractaudio/slideshow
   - preview/registry/future placeholder architecture
4. Implement minimal placeholders in `:export` package tree:
   - processor interfaces + ffmpeg processor
   - command/progress/session/job models
   - output/storage/share/service placeholders
   - classes referenced by app viewmodels (e.g. `ProcessingJobManager`, `ProcessEvent`, `OutputMedia`, `LocalOutputRepository`)
5. Re-run compile:
   - `./gradlew.bat :app:compileDebugKotlin`
   - then optionally `./gradlew.bat :app:assembleDebug`
6. If plugin conflict appears again (`Cannot add extension with name 'kotlin'`), keep plugin application minimal/consistent and avoid applying duplicate Kotlin Android plugin paths.

## 6) High-priority file checklist
- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `edit/build.gradle.kts`
- `export/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/nantcompany/clipy/**`
- `edit/src/main/java/com/nantcompany/clipy/edit/**`
- `export/src/main/java/com/nantcompany/clipy/export/**`

## 7) Definition of done
- Exactly 3 modules only (`:app`, `:edit`, `:export`)
- Correct dependency direction
- `com.nantcompany.clipy` app id/namespace consistent
- `:export` consumes FFmpeg AAR from `export/libs`
- Project compiles (`:app:compileDebugKotlin` passes)
- No UI screen classes inside `:edit` / `:export`
- No FFmpeg execution logic inside app Composables/ViewModels (only invoke export layer)

## 8) Quick verify commands
From `D:\Code\Coding\Clipy`:
- `./gradlew.bat :app:compileDebugKotlin`
- `./gradlew.bat :app:assembleDebug`
- `rg --files edit/src/main/java`
- `rg --files export/src/main/java`

