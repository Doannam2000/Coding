# Project Notes

<!-- AUTO-GENERATED:CORE_START -->
## Core App Snapshot (Auto)
- Last updated: 2026-04-23 22:24 UTC
- App: Clipy
- Slug: clipy
- Tagline: Fast social-ready video edits and GIF exports in seconds.
- Target users: Content creators, TikTok/Reels users, meme makers, casual users needing fast video editing
- Design direction: Extend the current dark, creator-focused Clipy language into a CapCut-identical media picker: matte black foundation, dense edge-to-edge content, restrained blue selection states, compact rounded surfaces, and fast utility-first chrome that keeps attention on the media grid and selected-order workflow.
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
Additional follow-up requirement: Design a modern mobile video editing app UI inspired by CapCut.

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
Additional follow-up requirement: Design a mobile video picker screen identical to CapCut.

Style:
- Dark mode (#0F0F0F background)
- Clean, minimal, modern
- Accent color: #2563EB (selected state)
- Rounded corners (8–12dp)
- Subtle shadows and overlays
- High contrast text (white/gray)

Layout:

1. Top Bar:
- Left: Close (X)
- Center: Title "Photos" or "Videos"
- Right: "Next" button (disabled until selection)
- Optional dropdown to switch Albums

2. Tab / Filter:
- Tabs: Videos | Photos | Live
- Active tab highlighted (underline or blue text)
- Smooth swipe between tabs

3. Media Grid:
- 3–4 columns responsive grid
- Square thumbnails (1:1)
- Each item:
  + Video thumbnail preview
  + Duration label (bottom-right, semi-transparent black bg)
  + Checkbox or number badge when selected
- Lazy loading for performance

4. Selection Behavior:
- Tap to select (multi-select supported)
- Show order number (1,2,3...) like CapCut
- Selected item:
  + Blue border (#2563EB)
  + Slight scale animation
- Max selection limit (optional)

5. Bottom Panel:
- Show selected items horizontally (preview strip)
- Drag to reorder selected videos
- "Add" or "Next" button (primary CTA)

6. Interaction:
- Smooth scrolling (no lag)
- Instant selection feedback
- Gesture friendly (tap, drag)
- Fast loading thumbnails

7. Extra UX:
- Show "Camera" item at first position
- Show album selector (Recent, Downloads, WhatsApp...)
- Support long press to preview video
- Auto-scroll to newest items

Constraints:
- Must feel identical to CapCut media picker
- Extremely smooth performance (60fps)
- No UI overlap or clutter
- Optimized for large media libraries

Output:
- Full mobile UI screen
- Clean spacing and hierarchy
- Production-ready layout

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
- Media Picker Screen
- Selected Media Tray

### Architecture Core
- ui: Jetpack Compose
- pattern: MVVM
- storage: Keep persisted behavior narrow: load device media through Android media APIs and hold picker tab, album, grid, preview, and selection ordering state in ViewModel memory; only pass finalized selected asset references into the existing editor import path unless current project persistence already stores imported media metadata.
<!-- AUTO-GENERATED:CORE_END -->
