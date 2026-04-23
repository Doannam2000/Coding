# Project Notes

<!-- AUTO-GENERATED:CORE_START -->
## Core App Snapshot (Auto)
- Last updated: 2026-04-23 17:58 UTC
- App: Clipy
- Slug: clipy
- Tagline: Fast social-ready video edits and GIF exports in seconds.
- Target users: Content creators, TikTok/Reels users, meme makers, casual users needing fast video editing
- Design direction: Incrementally evolve the existing single-route editor into a more CapCut-like mobile workspace by deepening the premium dark timeline language, tightening spatial rhythm, and adding smoother creator-first interaction states without changing navigation or export flow. Keep the current dense docked editor structure, but extend it with a clearer three-zone hierarchy: rounded preview stage on top, ultra-readable multi-track timeline core in the middle, and a compact active-tool rail with contextual controls below.
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
Feature priorities update: Redesign the video editor UI and timeline to closely match CapCut style: large video preview on top, thumbnail timeline strip below, draggable start and end trim handles, central playhead, smooth horizontal scrolling, pinch-to-zoom timeline, dimmed unselected areas, highlighted selected range, real-time synced preview while dragging, looping preview for selected segment, and polished animations until the interaction feels as smooth and responsive as CapCut
Additional follow-up requirement: [fixbug] Fix the editor UI because it does not match CapCut style and the timeline feels laggy when swiping. Redesign the layout to look closer to CapCut with a cleaner preview area, thumbnail strip, selected range highlight, draggable trim handles, and central playhead. Optimize timeline rendering, gesture handling, thumbnail caching, lazy loading, and preview sync so horizontal swiping becomes smooth, responsive, and stable like CapCut.
Additional follow-up requirement: [fixbug] Fix the editor UI because it does not match CapCut style and the timeline feels laggy when swiping. Redesign the layout to look closer to CapCut with a cleaner preview area, thumbnail strip, selected range highlight, draggable trim handles, and central playhead. Optimize timeline rendering, gesture handling, thumbnail caching, lazy loading, and preview sync so horizontal swiping becomes smooth, responsive, and stable like CapCut.
Additional follow-up requirement: [fixbug] Fix the editor UI because it does not match CapCut style and the timeline feels laggy when swiping. Redesign the layout to look closer to CapCut with a cleaner preview area, thumbnail strip, selected range highlight, draggable trim handles, and central playhead. Optimize timeline rendering, gesture handling, thumbnail caching, lazy loading, and preview sync so horizontal swiping becomes smooth, responsive, and stable like CapCut.
Additional follow-up requirement: Redesign and polish the editor UI to be small, clean, modern, and visually close to CapCut with pixel-level accuracy. Implement a full timeline system including video track with thumbnail strip, draggable trim handles, central playhead, smooth scrolling, and zoom. Add a separate audio track below the video timeline with waveform visualization and support trimming, cutting, and adjusting audio independently. Improve spacing, typography, colors, and interactions so the UI feels minimal, premium, and identical to CapCut experience.
Additional follow-up requirement: Redesign and polish the editor UI to be small, clean, modern, and visually close to CapCut with pixel-level accuracy. Implement a full timeline system including video track with thumbnail strip, draggable trim handles, central playhead, smooth scrolling, and zoom. Add a separate audio track below the video timeline with waveform visualization and support trimming, cutting, and adjusting audio independently. Improve spacing, typography, colors, and interactions so the UI feels minimal, premium, and identical to CapCut experience.
Additional follow-up requirement: [fixbug] Design a modern mobile video editing app UI inspired by CapCut.

Style:
- Clean, minimal, dark mode (primary: #0F0F0F, secondary: #1C1C1E)
- Accent color: #2563EB (blue highlight for active tools)
- Smooth, professional, creator-focused UI
- Rounded corners (12–16dp), soft shadows, glassmorphism overlays
- High contrast text (white/gray)

Layout:
1. Top Preview Area:
   - Video preview with rounded corners
   - Play/Pause button centered
   - Timeline scrubber overlay
   - Gesture support (pinch to zoom, drag to seek)

2. Timeline Editor (Core focus like CapCut):
   - Horizontal scrollable timeline
   - Multiple tracks:
     + Video track (thumbnails)
     + Audio track (waveform)
     + Text/Sticker track
   - Draggable clips (trim, split, move)
   - Current time indicator (vertical line)
   - Smooth scrolling, no lag when dragging

3. Bottom Tool Bar:
   - Icons with labels (Edit, Audio, Text, Effects, Filters)
   - Active state highlighted (blue)
   - Expandable tool panel when selected

4. Editing Panel:
   - Context-based tools (Trim, Split, Speed, Volume, Fade)
   - Slider controls (smooth, responsive)
   - Real-time preview update

5. Interaction:
   - Smooth animation (like CapCut)
   - No lag when scrubbing timeline
   - Gesture-first UX (drag, pinch, swipe)
   - Snap-to-grid timeline alignment

6. Extra Features UI:
   - Add audio button (+)
   - Add text overlay
   - Layer management
   - Undo/Redo floating buttons

Constraints:
- UI must feel identical to CapCut experience
- Timeline interaction must be ultra smooth (no jitter)
- Prioritize usability for beginners and creators
- Mobile-first design (Android & iOS)

Output:
- Full UI layout with all components
- Clean spacing, no overlapping elements
- Production-ready design system

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
- Editor Screen
- Timeline Editor
- Tool Workspace

### Architecture Core
- ui: Jetpack Compose
- pattern: MVVM
- storage: Keep existing Room/DataStore and current editor persistence behavior unchanged; store the richer tool-panel, multi-track presentation, gesture, and clip-interaction state in editor-local/viewmodel memory, persisting only through the existing project save path if that path already supports timeline edit metadata.
<!-- AUTO-GENERATED:CORE_END -->
