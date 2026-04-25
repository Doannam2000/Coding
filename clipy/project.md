# Project Notes

<!-- AUTO-GENERATED:CORE_START -->
## Core App Snapshot (Auto)
- Last updated: 2026-04-25 05:55 UTC
- App: Clipy
- Slug: clipy
- Tagline: Fast social-ready video edits and GIF exports in seconds.
- Target users: Content creators, TikTok/Reels users, meme makers, casual users needing fast video editing
- Design direction: Incremental CapCut-style upgrade on top of the current Compose + MVVM foundation: keep existing navigation shell and component architecture stable, evolve editor-centric screens into a professional dark, high-contrast workflow, and align branding/motion for app ID migration to com.nantcompany.capcuteditor without disruptive UI rewrites.
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
Additional follow-up requirement: Design a modern mobile video editing app with a full flow identical to CapCut:
Home Screen → Media Picker → Video Editor.

GLOBAL STYLE:
- Dark mode (#0F0F0F background)
- Clean, minimal, creator-focused UI
- Accent color: #2563EB (active / highlight)
- Rounded corners (12–16dp)
- Smooth animations, 60fps interactions
- High contrast text (white / gray)

-----------------------------------
1. HOME SCREEN (Landing)
-----------------------------------

Layout:
- Top:
  + App logo / title (left)
  + Profile / Settings icon (right)

- Center:
  + Large primary button:
    "New Project"
    (rounded, blue #2563EB, strong CTA)
  + Subtitle: "Create your video"

- Below:
  + Recent Projects (horizontal list)
    * Thumbnail preview
    * Video duration
    * Last edited time

- Bottom:
  + Simple navigation (Home / Templates / Profile)

UX:
- Tap "New Project" → go to Media Picker
- Smooth fade/scale transition
- Clean, uncluttered

-----------------------------------
2. MEDIA PICKER (Select Photos/Videos)
-----------------------------------

Layout:
- Top bar:
  + Close (X)
  + Title: "Videos"
  + "Next" button (disabled until selection)

- Tabs:
  + Videos | Photos
  + Active tab highlighted

- Grid:
  + 3–4 columns
  + Square thumbnails
  + Each item:
    * Video preview
    * Duration (bottom-right overlay)
    * Selection order badge (1,2,3...)

- First item:
  + Camera shortcut

- Bottom:
  + Selected items preview strip (horizontal)
  + Drag to reorder

UX:
- Multi-select
- Blue border when selected (#2563EB)
- Smooth scrolling (lazy load)
- Tap "Next" → go to Editor

-----------------------------------
3. VIDEO EDITOR (Core CapCut UI)
-----------------------------------

Layout:

A. Top Preview:
- Video player (rounded corners)
- Play/Pause center button
- Timeline scrub overlay
- Gesture:
  + Tap to play/pause
  + Pinch to zoom

B. Timeline (MAIN FOCUS):
- Horizontal scroll
- Multi-layer tracks:
  + Video track (thumbnails)
  + Audio track (waveform)
  + Text / sticker layers
- Features:
  + Drag clips
  + Trim (resize edges)
  + Split at playhead
- Current playhead (center vertical line)

C. Bottom Toolbar:
- Tabs:
  Edit | Audio | Text | Effects | Filters
- Active tab highlighted (blue)

D. Editing Panel:
- Context tools:
  + Trim
  + Split
  + Speed
  + Volume
  + Fade
- Sliders with real-time preview

E. Floating Controls:
- Undo / Redo
- Add (+) button

UX:
- Timeline must be ultra smooth (no lag)
- Dragging clips must feel like CapCut
- Snap alignment when editing
- Real-time preview updates

-----------------------------------
TRANSITIONS
-----------------------------------
- Home → Picker:
  Fade + scale up

- Picker → Editor:
  Slide in from right

- Back navigation:
  Smooth reverse animation

-----------------------------------
CONSTRAINTS
-----------------------------------
- Must feel identical to CapCut UX
- No lag in timeline interaction
- Clean spacing, no overlapping UI
- Mobile-first (Android & iOS)

-----------------------------------
OUTPUT
-----------------------------------
- Full UI design for 3 screens
- Ready for implementation
- Clear component hierarchy
Additional follow-up requirement: Design a modern mobile video editing app with a full flow identical to CapCut:
Home Screen → Media Picker → Video Editor.

GLOBAL STYLE:
- Dark mode (#0F0F0F background)
- Clean, minimal, creator-focused UI
- Accent color: #2563EB (active / highlight)
- Rounded corners (12–16dp)
- Smooth animations, 60fps interactions
- High contrast text (white / gray)

-----------------------------------
1. HOME SCREEN (Landing)
-----------------------------------

Layout:
- Top:
  + App logo / title (left)
  + Profile / Settings icon (right)

- Center:
  + Large primary button:
    "New Project"
    (rounded, blue #2563EB, strong CTA)
  + Subtitle: "Create your video"

- Below:
  + Recent Projects (horizontal list)
    * Thumbnail preview
    * Video duration
    * Last edited time

- Bottom:
  + Simple navigation (Home / Templates / Profile)

UX:
- Tap "New Project" → go to Media Picker
- Smooth fade/scale transition
- Clean, uncluttered

-----------------------------------
2. MEDIA PICKER (Select Photos/Videos)
-----------------------------------

Layout:
- Top bar:
  + Close (X)
  + Title: "Videos"
  + "Next" button (disabled until selection)

- Tabs:
  + Videos | Photos
  + Active tab highlighted

- Grid:
  + 3–4 columns
  + Square thumbnails
  + Each item:
    * Video preview
    * Duration (bottom-right overlay)
    * Selection order badge (1,2,3...)

- First item:
  + Camera shortcut

- Bottom:
  + Selected items preview strip (horizontal)
  + Drag to reorder

UX:
- Multi-select
- Blue border when selected (#2563EB)
- Smooth scrolling (lazy load)
- Tap "Next" → go to Editor

-----------------------------------
3. VIDEO EDITOR (Core CapCut UI)
-----------------------------------

Layout:

A. Top Preview:
- Video player (rounded corners)
- Play/Pause center button
- Timeline scrub overlay
- Gesture:
  + Tap to play/pause
  + Pinch to zoom

B. Timeline (MAIN FOCUS):
- Horizontal scroll
- Multi-layer tracks:
  + Video track (thumbnails)
  + Audio track (waveform)
  + Text / sticker layers
- Features:
  + Drag clips
  + Trim (resize edges)
  + Split at playhead
- Current playhead (center vertical line)

C. Bottom Toolbar:
- Tabs:
  Edit | Audio | Text | Effects | Filters
- Active tab highlighted (blue)

D. Editing Panel:
- Context tools:
  + Trim
  + Split
  + Speed
  + Volume
  + Fade
- Sliders with real-time preview

E. Floating Controls:
- Undo / Redo
- Add (+) button

UX:
- Timeline must be ultra smooth (no lag)
- Dragging clips must feel like CapCut
- Snap alignment when editing
- Real-time preview updates

-----------------------------------
TRANSITIONS
-----------------------------------
- Home → Picker:
  Fade + scale up

- Picker → Editor:
  Slide in from right

- Back navigation:
  Smooth reverse animation

-----------------------------------
CONSTRAINTS
-----------------------------------
- Must feel identical to CapCut UX
- No lag in timeline interaction
- Clean spacing, no overlapping UI
- Mobile-first (Android & iOS)

-----------------------------------
OUTPUT
-----------------------------------
- Full UI design for 3 screens
- Ready for implementation
- Clear component hierarchy
Additional follow-up requirement: [fixbug] Fix critical bugs and improve UI/UX across the app (Home, Media Picker, Templates, Profile, Language screen).

-----------------------------------
1. BUG: Media Picker not loading videos
-----------------------------------

Issues:
- Video list is empty or not loading
- Thumbnails not displayed
- Slow or broken loading

Fix:
- Ensure proper permission handling:
  + Android 13+: READ_MEDIA_VIDEO
  + Below Android 13: READ_EXTERNAL_STORAGE
- Query device media using MediaStore correctly
- Load videos sorted by date (latest first)
- Generate thumbnails efficiently
- Handle empty state (show "No videos found")
- Add loading state (shimmer grid)

Performance:
- Use lazy loading / pagination
- Cache thumbnails
- Avoid blocking UI thread

-----------------------------------
2. BUG: Templates & Profile tabs are empty
-----------------------------------

Fix Templates tab:
- Add placeholder UI:
  + List of template cards
  + Thumbnail + title
  + "Coming soon" badge
- Optional:
  + Fake/mock data for now
  + Category filter (Trending, Vlog, TikTok)

Fix Profile tab:
- Add basic profile UI:
  + Avatar (circle)
  + Username
  + Email (optional)
- Add menu list:
  + My Projects
  + Settings
  + Language
  + About
- Add logout button (UI only if no backend)

-----------------------------------
3. UI ISSUE: Language screen looks bad
-----------------------------------

Redesign Language Screen:

Style:
- Modern, minimal, clean
- Dark mode
- Rounded cards

Layout:
- Top:
  + Title: "Language"
  + Back button

- List:
  + Language items (English, Vietnamese, etc.)
  + Each item:
    * Language name
    * Optional flag icon
    * Radio/check indicator

UX:
- Highlight selected language (blue accent #2563EB)
- Smooth selection animation
- Large touch area
- Divider or card spacing

Extra:
- Auto-detect system language
- Show "Recommended" label

-----------------------------------
4. GENERAL UI IMPROVEMENTS
-----------------------------------

- Fix spacing and alignment issues
- Ensure no overlapping UI
- Improve typography hierarchy
- Add loading states (skeleton/shimmer)
- Add empty states (friendly message + icon)

-----------------------------------
5. PERFORMANCE & UX
-----------------------------------

- Ensure smooth scrolling (60fps)
- Avoid unnecessary recomposition / re-render
- Optimize image loading
- Improve gesture responsiveness

-----------------------------------
OUTPUT
-----------------------------------

- Fixed Media Picker (video loading works)
- Functional Templates & Profile UI (even with mock data)
- Redesigned Language screen (clean, modern)
- Stable and smooth UX
Additional follow-up requirement: [fixbug] Fix Media Picker bug: the app has full storage/media permissions but the picker still loads 0 media items.

Investigate and fix these possible causes:

1. MediaStore query may be wrong
- Use correct collection:
  MediaStore.Video.Media.EXTERNAL_CONTENT_URI for videos
  MediaStore.Images.Media.EXTERNAL_CONTENT_URI for photos
- Do not query only app-specific folders
- Sort by DATE_ADDED DESC
- Check MIME_TYPE filter is not too strict

2. Android version permission handling
- Android 13+:
  READ_MEDIA_VIDEO for videos
  READ_MEDIA_IMAGES for photos
- Android 14+:
  handle READ_MEDIA_VISUAL_USER_SELECTED if partial access is granted
- Android 12 and below:
  READ_EXTERNAL_STORAGE
- After permission granted, reload media immediately

3. URI creation
- Build content URI correctly:
  ContentUris.withAppendedId(collectionUri, mediaId)
- Do not use raw file paths directly
- Do not depend on DATA column because it is deprecated

4. Cursor handling
- Check cursor is not null
- Check moveToFirst / moveToNext correctly
- Log cursor.count
- Log each media id, uri, displayName, duration
- Close cursor safely

5. UI state bug
- Make sure loaded media list updates state
- Do not reset mediaList to empty after load
- Do not filter videos/photos incorrectly in UI
- Verify selected tab actually queries correct media type

6. Debug logs required
Add logs:
- Android SDK version
- Granted permissions
- Current selected tab
- Query URI
- Cursor count
- Final media list size

Expected result:
- Media Picker must load all videos/photos from device gallery
- Show empty state only when cursor.count == 0
- Work on Android 10, 11, 12, 13, 14, 15+
Additional follow-up requirement: [fixbug] crash when select video/image and continue
Additional follow-up requirement: [fixbug] crash when select video/image and continue
Additional follow-up requirement: [fixbug] Crash when select video/image
Additional follow-up requirement: [fixbug] Crash when select video/image
Additional follow-up requirement: [fixbug] Crash when select video/image
Additional follow-up requirement: [fixbug] Crash when select video/image
Additional follow-up requirement: [fixbug] BotVibeProject/clipy/app/src/main/java/com/nantcompany/clipy/ClipyApp.kt:3831:14 Unresolved reference 'isVideoSource'.
Additional follow-up requirement: [fixbug] ClipyApp.kt:3831:14 Unresolved reference 'isVideoSource'.
Additional follow-up requirement: [uxui] You are a Senior Mobile Architect & Video Editor Engine Builder.

Your task: Transform an existing mobile app into a professional video editing app similar to CapCut.

## CORE REQUIREMENTS

Build a FULL-FEATURED VIDEO EDITOR with:

### 🎬 1. Timeline Editing (CRITICAL)
- Multi-track timeline (video, audio, text, sticker)
- Drag & drop clips
- Trim, split, merge clips
- Smooth horizontal scroll timeline
- Zoom in/out timeline (pinch gesture)
- Snap to edges (like CapCut)
- Preview sync with timeline

### 🎵 2. Audio System
- Add background music
- Extract audio from video
- Trim audio
- Fade in / fade out
- Volume control per track
- Multi audio layers

### 🖼 3. Media Input
Support import:
- Images (jpg, png)
- Videos (mp4, mov, etc.)
- Audio files

From:
- Local storage
- Gallery picker

### 🎨 4. Effects & Visual Tools
- Filters (brightness, contrast, LUT presets)
- Transitions between clips
- Blur / sharpen
- Color grading basic tools
- Speed control (0.5x → 2x)

### 😎 5. Sticker & Text
- Add text overlays
- Custom fonts
- Text animation (fade, slide, scale)
- Stickers (static + animated)
- Drag/resize/rotate elements on preview

### 📺 6. Preview Player
- Real-time preview
- Play / pause
- Seek by timeline
- Sync UI with timeline position

### ⚡️ 7. Performance (IMPORTANT)
- Smooth timeline (no lag)
- Use background processing for rendering
- Optimize memory usage
- Lazy loading media

### 🎞 8. Export System
Export video:
- Formats: MP4, MOV
- Resolution: 720p, 1080p, 2K, 4K
- FPS options (24, 30, 60)
- Progress indicator while exporting

### 🧠 9. Architecture
- MVVM
- Clean architecture
- Separate:
  - UI layer
  - Editing engine
  - Media processing

### 🧱 10. UI/UX (LIKE CAPCUT)
- Dark theme
- Bottom toolbar (Edit, Audio, Text, Sticker)
- Timeline at bottom
- Preview on top
- Smooth animations
- Modern, minimal UI

### 🛠 11. State Management
- Handle:
  - Undo / redo
  - Current timeline position
  - Selected clip
  - Editing mode

---

## OUTPUT REQUIREMENTS

Generate:

1. Full UI screens:
   - Home (pick media)
   - Editor screen (timeline + preview)
   - Export screen

2. Core components:
   - TimelineView
   - VideoPlayer
   - AudioTrack
   - OverlayLayer (text/sticker)

3. Editing engine:
   - Clip model
   - Track model
   - Timeline state

4. Export pipeline:
   - Render video with effects
   - Combine audio + video

5. Clean, production-ready code
- No placeholder logic
- No fake UI
- Fully functional structure

---

## EXTRA (OPTIONAL BUT RECOMMENDED)

- Gesture controls (drag, pinch, scale)
- Auto-save project
- Draft system
- Templates (basic)

---

## IMPORTANT

This is NOT a demo app.
This is a REAL video editor like CapCut.

Ensure:
- Smooth UX
- No UI overlap
- Fully usable flow
Additional follow-up requirement: Rewrite the entire app into a professional CapCut-style mobile video editor.

App ID must use:
com.nantcompany.capcuteditor

Main goal:
Turn the current app into a full video editing app like CapCut. Users can create projects, import photos/videos/music, add stickers/text/effects/filters, edit everything on a timeline, preview in real time, and export final videos.

==================================================
1. APP FLOW
==================================================

Required screens:
- Splash screen
- Intro / onboarding screen
- Home screen
- Project list / Draft screen
- Media picker screen
- Editor screen
- Music library screen
- Sticker library screen
- Text editor panel
- Filter / effect panel
- Export settings screen
- Export progress screen
- Settings screen

Main user flow:
1. User opens app
2. User taps “New Project”
3. User selects photos/videos
4. App opens editor
5. User edits timeline
6. User adds music, text, stickers, filters, transitions
7. User previews video
8. User exports MP4/MOV

==================================================
2. MEDIA IMPORT
==================================================

Support importing:
- Images: JPG, JPEG, PNG, WEBP
- Videos: MP4, MOV, MKV, AVI if supported
- Music/audio: MP3, WAV, AAC, M4A

Media picker requirements:
- Show tabs: Photos, Videos, Music
- Display grid thumbnails
- Support multi-select
- Show selected count
- Show video duration
- Show image preview
- Handle permissions properly
- Handle empty media state
- Handle permission denied state
- Handle loading state

When importing images:
- Convert each image into a timeline clip
- Default duration: 3 seconds
- Allow user to change duration
- Support crop, fit, fill modes
- Support Ken Burns style zoom animation later

When importing videos:
- Preserve original duration
- Generate thumbnail
- Allow trim before/after import
- Support mute original audio

When importing music:
- Add music into audio track
- Show waveform placeholder/visual track
- Allow trim, volume, fade in/out

==================================================
3. EDITOR SCREEN UI LIKE CAPCUT
==================================================

Editor layout:
- Top app bar:
  - Back
  - Undo
  - Redo
  - Export button
- Preview area:
  - Video/image preview
  - Text overlay layer
  - Sticker overlay layer
  - Drag/scale/rotate overlay controls
- Playback controls:
  - Play / pause
  - Current time / total duration
  - Seek sync with timeline
- Timeline area:
  - Time ruler
  - Video/image track
  - Audio track
  - Text track
  - Sticker track
  - Playhead center line
- Bottom toolbar:
  - Edit
  - Audio
  - Text
  - Sticker
  - Filter
  - Effect
  - Transition
  - Canvas
  - Speed
  - Export

UI style:
- Dark theme like CapCut
- Modern rounded buttons
- Clean spacing
- Smooth animations
- No overlapping UI
- No clipped text
- Timeline must scroll smoothly
- Selected clip must have clear border/handle

==================================================
4. TIMELINE FEATURES
==================================================

Timeline must support:
- Multi-track editing
- Video/image track
- Audio/music track
- Text overlay track
- Sticker overlay track
- Effect/filter track
- Transition track

Clip actions:
- Select clip
- Drag clip
- Reorder clip
- Trim start
- Trim end
- Split at playhead
- Delete clip
- Duplicate clip
- Change duration
- Mute video audio
- Adjust volume
- Change speed
- Reverse video if possible

Timeline behavior:
- Horizontal scroll
- Pinch to zoom timeline
- Snap clip edges
- Center playhead like CapCut
- Preview must sync with scroll position
- Smooth drag without lag
- Lazy load thumbnails
- Show duration labels

==================================================
5. AUDIO / MUSIC FEATURES
==================================================

Audio features:
- Add music from device
- Add built-in sample music list
- Extract audio from video
- Trim audio
- Split audio
- Delete audio
- Adjust volume
- Fade in
- Fade out
- Loop audio
- Mute original video audio
- Support multiple audio layers

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
- Splash Screen (existing, modified)
- Intro / Onboarding Screen (existing, modified)
- Home Screen (existing, expanded)
- Project List / Draft Screen (new)
- Media Picker Screen (new or major expansion)
- Editor Screen (existing, major expansion)
- Music Library Screen (new)
- Sticker Library Screen (new)
- Text Editor Panel (new panel flow)
- Filter / Effect Panel (new panel flow)
- Export Settings Screen (existing, expanded)
- Export Progress Screen (existing, expanded)
- Settings Screen (existing, modified)

### Architecture Core
- ui: Jetpack Compose
- pattern: MVVM
- storage: Keep DataStore + existing repositories; add Room-backed project/draft index and serialized timeline snapshots, while retaining MediaStore URI references and persisted URI permissions for imported assets.
<!-- AUTO-GENERATED:CORE_END -->
