# Project Notes

<!-- AUTO-GENERATED:CORE_START -->
## Core App Snapshot (Auto)
- Last updated: 2026-04-25 02:20 UTC
- App: Clipy
- Slug: clipy
- Tagline: Fast social-ready video edits and GIF exports in seconds.
- Target users: Content creators, TikTok/Reels users, meme makers, casual users needing fast video editing
- Design direction: Keep the current Media Picker visual language unchanged and introduce only stability-focused feedback states around image/video selection so the flow feels reliable, predictable, and non-disruptive when a media item cannot be opened immediately.
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
- Selection Destination Screen

### Architecture Core
- ui: Jetpack Compose
- pattern: MVVM
- storage: Retain the current ViewModel-managed UI state and MediaStore/content-URI access. Add only minimal validation and safe payload transfer around the selection-to-continue integration point.
<!-- AUTO-GENERATED:CORE_END -->
