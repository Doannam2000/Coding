# Project Notes

<!-- AUTO-GENERATED:CORE_START -->
## Core App Snapshot (Auto)
- Last updated: 2026-04-23 00:56 UTC
- App: Clipy
- Slug: clipy
- Tagline: Fast social-ready video edits and GIF exports in seconds.
- Target users: Content creators, TikTok/Reels users, meme makers, casual users needing fast video editing
- Design direction: Premium dark creator-tool interface built on Material 3 with dense but clear editing controls, rounded surfaces, bold contrast, and motion that feels fast and purposeful rather than playful. The visual language should support a single-flow editing experience where preview media stays dominant, controls feel production-ready, and export actions are always visually prioritized.
- Core constraints: Android only, no backend, lightweight but powerful, limit GIF size/duration, smooth UX, handle large videos safely
Design style must align with: Modern dark creator-tool UI, clean layout, smooth animations, rounded cards, premium feel
Typography should align with: Inter
Feature priorities: Pick video, preview playback, trim start/end, crop ratios 1:1 4:5 9:16 16:9, change speed, mute audio, export GIF, export MP4, adjust GIF fps/resolution, adjust MP4 quality, reverse, boomerang, add text watermark, rename output, save, share, export progress, export history
Must-have requirements: Kotlin, Jetpack Compose, MVVM, Media3 ExoPlayer, FFmpeg processing, Room database, DataStore, proper content URI handling, optimized performance
Use Android applicationId/package name: com.nantcompany.clipy
Feature priorities update: Add draggable trim timeline like CapCut with thumbnail preview, start/end handles, playhead, and real-time synced video preview
Feature priorities update: Add a CapCut-style timeline editor with draggable start and end handles, thumbnail preview strip, movable playhead, and real-time synced video preview. Continue refining UX, gestures, and animations until the interaction feels as smooth and responsive as CapCut timeline editing.
Feature priorities update: Implement a CapCut-style timeline editor with pinch-to-zoom, smooth draggable scrolling, draggable start/end trim handles, frame-by-frame stepping, snap-to-keyframe cutting, looping preview for selected range, real-time synced video preview via ExoPlayer seek, and optimized thumbnail caching/lazy loading for ultra-smooth performance
Feature priorities update: Implement the basic MVP first: pick video from gallery, preview video playback, draggable start and end trim selection, real-time synced preview while trimming, export as short MP4, export as GIF, save output to device, share exported file, simple export progress UI, and basic export history
Feature priorities update: Add basic crop ratio presets for 1:1, 4:5, 9:16, and 16:9 so users can export clips for different social platforms
Additional follow-up requirement: Add mute toggle so users can choose whether exported MP4 keeps or removes original audio

### Core Features
- Splash screen followed by onboarding and main app flow
- Language selection with at least English and Vietnamese
- Video picker with proper content URI handling
- Preview playback using Media3 ExoPlayer
- Trim start and end points with visual timeline controls
- Crop presets for 1:1, 4:5, 9:16, and 16:9
- Speed adjustment controls for quick pacing changes
- Mute audio toggle for silent exports
- Reverse video effect
- Boomerang effect for loop-style clips
- Text watermark overlay with simple positioning presets
- Export to GIF with duration limits plus FPS and resolution controls
- Export to MP4 with quality controls
- Output rename before save
- Export progress screen with cancellable processing state if feasible
- Save to device and share sheet integration
- Export history stored locally with Room
- Settings area for language, default export preferences, and storage behavior
- Explicit exit action within the app shell

### Main Screens
- Splash Screen
- Intro and Language Selection
- Main Editor
- Export Progress
- Export Result and Share
- Export History
- Settings

### Architecture Core
- ui: Jetpack Compose
- pattern: MVVM
- storage: Room for export history, DataStore for preferences, MediaStore/content URI access for imported and exported files, and local cached timeline thumbnails for smooth editor performance
<!-- AUTO-GENERATED:CORE_END -->
