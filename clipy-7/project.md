# Project Notes

## Architecture Boundaries
- `com.example.clipystudio.ui`: Compose screens, navigation glue, immutable state rendering, and callback dispatch only.
- `com.example.clipystudio.editor`: ViewModel-facing editor orchestration models, screen aggregate state, selection state, panel state, and adapters from app storage models.
- `com.example.clipystudio.timeline`: timeline math, clip placement, snap, scroll, trim, touch slop, and gesture helper ownership. Existing `TimelineEngine` remains source-compatible and is exposed through this boundary for incremental migration.
- `com.example.clipystudio.media`: playback, preview seek dispatch, media metadata, and player-facing abstractions.
- `com.example.clipystudio.render`: export settings, render preparation, effect/transition render contracts, and export orchestration. Existing render engines remain source-compatible and are exposed through this boundary for incremental migration.
- `com.example.clipystudio.data`: repositories, persistence DTOs, autosave, serializers, undo/redo storage, and mapping to stable editor models.
- `com.example.clipystudio.designsystem`: colors, typography, spacing, touch targets, handles, guides, chips, panels, loading/empty/error primitives, and other stateless visual tokens.

New editor-facing code should prefer stable models under `editor.model` (`Project`, `Track`, `Clip`, `Overlay`, `AudioClip`, `TextClip`, `StickerClip`, `EffectClip`, `Transition`, `Keyframe`, and `ExportSettings`). Existing persistence models in `data` are intentionally preserved for saved-project compatibility and mapped into the editor model layer before screen rendering.

<!-- AUTO-GENERATED:CORE_START -->
## Core App Snapshot (Auto)
- Last updated: 2026-04-25 11:00 UTC
- App: Clipy Studio
- Slug: clipy
- Tagline: A premium offline-friendly Android video editor for fast, polished social videos.
- Target users: General users
- Design direction: Incremental release-hardening updates that preserve Clipy Studio's existing editor-first visual language while making production states feel safer, clearer, and more trustworthy. Focus on permission education, recoverable error states, cache/storage transparency, release identity polish, and non-disruptive feedback during memory-heavy media operations.
- Core constraints: android only, MVP first, offline-friendly where possible
Design style must align with: modern minimal premium
Typography should align with: clean geometric sans
Feature priorities: write the android app into a full-featured CapCut-level video editor
GLOBAL RULE:
Replicate CapCut behavior, UX, and interaction exactly. No simplified features. All tools must be functional and production-ready.
PROJECT SYSTEM:
- Create new project
- Draft auto-save
- Rename / duplicate / delete project
- Thumbnail generation
- Resume editing from last state
MEDIA IMPORT:
- Import images (JPG, PNG, WEBP)
- Import videos (MP4, MOV)
- Import audio (MP3, WAV, AAC)
- Multi-select media
- Preview media before adding
- Show duration and size
- Support large files
TIMELINE SYSTEM:
- Multi-track timeline (video, audio, text, sticker, effects)
- Drag & drop clips
- Trim start/end
- Split at playhead
- Merge clips
- Duplicate clip
- Reorder clips
- Snap edges
- Pinch zoom timeline
- Scroll timeline smoothly
- Show thumbnails
- Center playhead
- Real-time preview sync
VIDEO EDITING:
- Crop (fit, fill, custom)
- Rotate / flip
- Adjust aspect ratio (1:1, 9:16, 16:9)
- Speed control (0.5x–2x, curve speed optional)
- Reverse video
- Freeze frame
- Opacity control
- Replace clip
IMAGE EDITING:
- Set duration
- Crop / zoom
- Pan animation (Ken Burns effect)
- Rotate / flip
AUDIO FEATURES:
- Add background music
- Extract audio from video
- Trim audio
- Split audio
- Fade in / fade out
- Volume control
- Mute video audio
- Loop audio
- Multiple audio layers
- Sound effects (tap, whoosh, etc.)
TEXT SYSTEM:
- Add text overlay
- Edit content
- Font selection
- Font size / color / background
- Stroke / shadow
- Alignment
- Animation (fade, slide, typewriter, pop)
- Duration control
- Multiple text layers
STICKER SYSTEM:
- Add stickers (emoji, shapes, trending)
- Animated stickers (GIF-like)
- Drag / scale / rotate
- Duplicate / delete
- Set duration
- Layer order control
FILTERS & ADJUST:
- Filters (warm, cool, vintage, cinematic, B&W)
- Adjust:
  - Brightness
  - Contrast
  - Saturation
  - Exposure
  - Temperature
  - Sharpness
  - Vignette
- Apply per clip
EFFECTS:
- Blur
- Glow
- Shake
- Zoom
- Flash
- VHS
- Glitch
- Motion effects
- Layer effects
TRANSITIONS:
- Fade
- Slide (left/right/up/down)
- Zoom in/out
- Blur transition
- Duration control
- Apply between clips
OVERLAY SYSTEM:
- Add overlay video/image layer
- Resize / rotate / position
- Opacity control
- Blend modes (optional advanced)
CANVAS SYSTEM:
- Background color
- Background blur
- Adjust canvas ratio
- Border / padding
KEYFRAME SYSTEM (ADVANCED):
- Add keyframes for:
  - Position
  - Scale
  - Rotation
  - Opacity
- Interpolation between keyframes
GESTURE SYSTEM:
- Drag element
- Pinch zoom
- Rotate gesture
- Snap alignment guides
- Multi-touch support
UNDO / REDO:
- Track all editing actions
- Instant revert / reapply
EXPORT SYSTEM:
- Format: MP4, MOV
- Resolution: 720p, 1080p, 2K, 4K
- FPS: 24, 30, 60
- Bitrate/quality control
- Export progress %
- Cancel export
- Save to gallery
- Share output
PERFORMANCE:
- Smooth timeline (no lag)
- Lazy thumbnail loading
- Background rendering
- Memory optimization
- Handle large media files
UX REQUIREMENTS:
- Dark modern UI like CapCut
- No UI overlap
- Smooth animations
- Instant feedback on interaction
- Real-time preview updates
- Gesture must feel natural
STRICT:
- No fake UI
- No placeholder features
- All buttons must work
- Timeline must be accurate
- Preview must always sync with timeline
- Production-ready only
Use Android applicationId/package name: com.nantcompany.clipy
Additional follow-up requirement: [uxui] EDITOR SCREEN FULL SPEC:

Build the Editor Screen identical to CapCut in UI, UX, layout, gestures, timeline behavior, and editing flow.

1. LAYOUT STRUCTURE:
UI: vertical dark layout with Top Bar, Preview Canvas, Playback Controls, Timeline Area, and Bottom Tool Menu; Function: all zones must resize safely on different Android screens, support system bars, keyboard, and avoid overlap or clipped text.

2. TOP BAR:
UI: left Back button, center project name with ellipsis, right Undo, Redo, Export buttons; Function: Back shows Save & Exit / Discard / Cancel dialog, Undo reverts last edit, Redo reapplies edit, Export sends the full current project state to ExportScreen.

3. PREVIEW CANVAS:
UI: black preview area centered above timeline, supports 9:16, 1:1, 16:9, 4:5, original ratio; Function: render current video/image frame from timeline, show active text/sticker/overlay layers only at current playhead time, and update instantly when user seeks or edits.

4. PREVIEW OVERLAY CONTROLS:
UI: selected overlay shows bounding box, resize corners, rotate handle, delete button, center guide lines; Function: tap selects, drag moves, pinch scales, rotate gesture rotates, double tap text opens editor, delete removes overlay and timeline item.

5. PLAYBACK CONTROLS:
UI: compact row with Play/Pause, current time, total duration, optional mini progress indicator; Function: Play starts preview and moves timeline, Pause stops playback, seeking updates playhead, preview frame, active overlays, and selected clip state.

6. TIMELINE AREA:
UI: fixed center playhead, time ruler, video/image track, audio track, text track, sticker track, overlay track, effect track; Function: timeline scroll changes current time, playback scrolls timeline under fixed playhead, zoom scale changes clip width accurately.

7. VIDEO/IMAGE TRACK:
UI: main track clip blocks with thumbnails, duration label, selected border, trim handles; Function: tap selects clip, drag reorders/moves clip, left/right handles trim, double tap splits, long press enables drag mode, clips snap to neighbors and playhead.

8. AUDIO TRACK:
UI: waveform-style blocks below video track with name, duration, selected highlight, fade indicators; Function: audio clips can be moved, trimmed, split, duplicated, deleted, volume adjusted, faded in/out, looped, and synced with video timeline.

9. TEXT TRACK:
UI: text clips shown as colored blocks with text preview label; Function: text appears only during its time range, can be moved, trimmed, duplicated, deleted, edited, animated, and selected from either preview or timeline.

10. STICKER/OVERLAY TRACK:
UI: sticker and overlay clips shown on separate tracks with small icon/thumbnail label; Function: stickers/overlays can be moved, resized on preview, trimmed on timeline, duplicated, deleted, reordered by layer, and shown only during active time.

11. CLIP EDIT PANEL:
UI: bottom contextual panel appears when a clip is selected with buttons Split, Delete, Duplicate, Speed, Volume, Replace, Mute, Crop, Rotate, Flip; Function: every button must perform real logic and update preview, timeline, project state, undo history, and autosave immediately.

12. TOOL MENU:
UI: bottom horizontal scroll menu with Edit, Audio, Text, Sticker, Overlay, Filter, Effect, Transition, Canvas, Speed, Export; Function: tapping a tool opens its panel, highlights selected tool, hides irrelevant controls, and keeps selected clip/overlay state.

13. AUDIO TOOL PANEL:
UI: tabs Device Music, Built-in Music, Extracted Audio, Sound Effects with list items containing title, duration, play, add; Function: preview audio, add to audio track at playhead, extract audio from video, and support trim/volume/fade after insertion.

14. TEXT TOOL PANEL:
UI: Add Text button, text input, font size, color, background, stroke, shadow, alignment, animation options; Function: creates editable text overlay at playhead, adds text clip to timeline, supports drag/scale/rotate on preview and duration editing.
Additional follow-up requirement: EDITOR SCREEN ADVANCED TOOLS:

Continue building Editor Screen tools starting from Sticker Panel to full advanced editing system, keeping CapCut-level behavior.

15. STICKER TOOL PANEL:
UI: sticker categories Emoji, Heart, Fire, Arrow, Shape, Reaction, Trending, Recent with grid layout; Function: tapping a sticker instantly adds it to preview and creates a sticker clip at playhead, then allows drag, scale, rotate, duplicate, delete, and adjust duration directly on timeline.

16. FILTER AND ADJUST PANEL:
UI: horizontal filter list with preview thumbnails and sliders below; Function: apply filters (warm, cool, vintage, cinematic, B&W) and adjust brightness, contrast, saturation, exposure, temperature, sharpness with real-time preview update.

17. EFFECT PANEL:
UI: grid of effects grouped by category (Basic, Motion, Blur, Glitch, Retro); Function: apply visual effects like blur, glow, shake, zoom, glitch as timeline-based effect clips that can be moved, trimmed, and removed.

18. TRANSITION PANEL:
UI: small button between video clips with panel showing transitions (fade, slide, zoom, blur) and duration slider; Function: apply transition between clips, update preview immediately, and allow removal or duration adjustment.

19. CANVAS PANEL:
UI: ratio selector (9:16, 1:1, 16:9, 4:5, original), background color and blur options; Function: update canvas size, maintain media transform, and apply background for non-fullscreen media.

20. SPEED TOOL:
UI: speed slider and presets (0.5x, 1x, 1.5x, 2x); Function: change playback speed of selected clip, update timeline duration accordingly, and sync preview instantly.

21. AUDIO PANEL:
UI: tabs (Device, Built-in, Extracted, Effects) with list items containing play and add buttons; Function: preview audio, add to audio track at playhead, then allow trim, move, volume, fade in/out, and loop.

22. TEXT PANEL:
UI: text input field with style controls (font size, color, background, stroke, shadow, animation); Function: add text overlay, update content, style, animation, and control timeline duration.

23. OVERLAY PANEL:
UI: media picker for overlay video/image; Function: add overlay layer above main video, support position, scale, rotation, opacity, and timeline duration.

24. KEYFRAME SYSTEM:
UI: keyframe toggle button and markers on timeline; Function: add keyframes for position, scale, rotation, opacity and interpolate between them for animation.

25. UNDO REDO SYSTEM:
UI: Undo/Redo buttons reflect state; Function: track all actions (clip edit, text, audio, effect) and allow instant revert/reapply.

26. EXPORT SCREEN:
UI: format (MP4/MOV), resolution (720p–4K), FPS (24/30/60), quality selector, export button; Function: render final video combining all tracks and show progress.

27. EXPORT PROGRESS:
UI: progress bar, percentage, cancel button; Function: background rendering, allow cancel, handle failure and success.

28. EXPORT SUCCESS:
UI: preview final video with Share and Save buttons; Function: save to gallery and share via system intent.

29. PERFORMANCE:
UI must remain smooth; Function: lazy load thumbnails, optimize memory, run heavy tasks in background, avoid UI blocking.

30. STRICT RULES:
All tools must work with real logic; Function: no placeholder UI, no fake interactions, preview must match timeline exactly, production-ready only.
Additional follow-up requirement: TIMELINE ENGINE CORE PART 1:

1. TIMELINE DATA MODEL:
Function: define ProjectTimeline with tracks (video, audio, text, sticker, overlay, effect) and Clip model including id, type, mediaUri, startTimeMs, durationMs, trimStartMs, trimEndMs, speed, volume, transform, filter, effect.

2. TIME MAPPING:
Function: convert scrollOffsetPx to currentTimeMs using zoomScale and pixelsPerSecond and ensure stable mapping.

3. PLAYHEAD SYNC:
Function: keep preview synced with playhead; scrolling updates preview frame instantly and playback scrolls timeline under fixed playhead.

4. TRACK LAYOUT:
Function: calculate clip position and width based on time and zoom scale and render multi-track layout correctly.

5. CLIP HIT TEST:
Function: detect selected clip, trim handles, empty area, and interaction zones from touch input precisely.

6. DRAG CLIP:
Function: update clip startTimeMs during drag, snap to edges/playhead, and prevent invalid overlaps.

7. TRIM CLIP:
Function: update trimStartMs and trimEndMs with handle drag, enforce min duration, update preview in real time.

8. SPLIT CLIP:
Function: split selected clip at playhead and create two valid clips preserving all properties.

9. REORDER VIDEO CLIPS:
Function: reorder main track clips with drag and shift neighbors correctly.

10. MULTI-TRACK LOGIC:
Function: allow overlapping for audio/overlay/text while maintaining independent track behavior.
Additional follow-up requirement: TIMELINE ENGINE CORE PART 2:

11. ZOOM ENGINE:
Function: implement pinch zoom with focal point preservation and smooth scaling.

12. THUMBNAIL SYSTEM:
Function: lazy load thumbnails only for visible clips and cache results.

13. SNAP ENGINE:
Function: snap clip edges to neighbors, playhead, and markers with threshold.

14. ACTIVE CLIP RESOLUTION:
Function: resolve which clips and overlays are active at currentTimeMs.

15. TRANSITION TIME MODEL:
Function: manage transition duration between clips and calculate overlap.

16. KEYFRAME SUPPORT:
Function: interpolate position, scale, rotation, opacity values over time.

17. UNDO/REDO:
Function: store timeline actions and revert/reapply changes instantly.

18. AUTOSAVE:
Function: debounce save timeline state and restore exactly.

19. PERFORMANCE:
Function: optimize recomposition, scrolling, dragging, and memory usage.

20. STRICT:
Function: no fake timeline, no desync, no lag, production-ready behavior.
Additional follow-up requirement: RENDER PIPELINE PART 1:

1. RENDER INPUT:
Function: collect full project state including all tracks, clips, overlays, filters, effects, transitions, audio, and settings.

2. EXPORT SETTINGS:
Function: map user options (resolution, fps, format, quality) to encoder config.

3. RENDER GRAPH:
Function: build ordered render pipeline combining all layers.

4. FRAME SCHEDULER:
Function: iterate frames using fps and calculate currentTimeMs per frame.

5. MAIN VIDEO RENDER:
Function: render correct video/image frame with trim, speed, crop, transform.

6. IMAGE CLIP RENDER:
Function: convert image clips into timed frames with duration and animations.

7. VIDEO CLIP RENDER:
Function: decode and seek correct source frame using timeline mapping.

8. TRANSITION RENDER:
Function: blend frames between clips using transition progress.

9. OVERLAY RENDER:
Function: render overlays above main layer with transform and opacity.

10. TEXT RENDER:
Function: draw text with style, animation, and timing.
Additional follow-up requirement: RENDER PIPELINE PART 2:

11. STICKER RENDER:
Function: render sticker layers including animated support if available.

12. FILTER RENDER:
Function: apply filters and adjustments per frame.

13. EFFECT RENDER:
Function: apply timeline-based visual effects only during active duration.

14. KEYFRAME RENDER:
Function: interpolate animated properties per frame.

15. CANVAS RENDER:
Function: apply aspect ratio, background color, and blur.

16. AUDIO MIXING:
Function: mix all audio tracks with volume, fade, and sync.

17. AUDIO SYNC:
Function: align audio playback with timeline and video frames.

18. CODEC STRATEGY:
Function: use MediaCodec primarily, fallback to FFmpeg when needed.

19. TEMP FILE MANAGEMENT:
Function: handle temp files safely and clean up after export.

20. PROGRESS + CANCEL:
Function: emit progress states and support cancel with safe cleanup.

21. ERROR HANDLING:
Function: detect and report render errors with retry support.

22. OUTPUT SAVE:
Function: save final video via MediaStore and expose URI.

23. SHARE:
Function: share output via system intent.

24. PERFORMANCE:
Function: run in background, avoid UI blocking, prevent OOM.

25. STRICT:
Function: exported video must match preview exactly, no fake rendering.
Additional follow-up requirement: GESTURE PHYSICS + SCROLL INERTIA PART 1:

1. GLOBAL GESTURE RULE:
Function: all gestures must feel like CapCut: smooth, responsive, predictable, no jitter, no accidental movement, no delayed feedback.

2. TIMELINE SCROLL:
Function: horizontal timeline scroll must support natural drag, fling inertia, deceleration, edge resistance, and stop smoothly without sudden jump.

3. CENTER PLAYHEAD:
Function: playhead stays fixed at center while timeline content moves underneath; scroll position must always map accurately to currentTimeMs.

4. FLING PHYSICS:
Function: after user releases timeline drag, continue scrolling with velocity-based inertia and gradually slow down.

5. EDGE RESISTANCE:
Function: when timeline reaches start/end, apply soft resistance instead of hard stop, then settle back safely.

6. PINCH ZOOM:
Function: pinch zoom timeline smoothly around finger focal point, preserve playhead time, clamp min/max zoom, and avoid clip width jumping.

7. SNAP FEELING:
Function: when dragging or trimming clips, snap softly to playhead, clip edges, timeline start/end, transition points, and beat markers.

8. SNAP THRESHOLD:
Function: use pixel threshold for snapping, stronger when close and weaker when far, with subtle haptic/visual feedback if available.

9. DRAG CLIP PHYSICS:
Function: selected clip should lift slightly, follow finger exactly, snap to valid positions, and settle smoothly when released.

10. TRIM HANDLE PHYSICS:
Function: trim handles must be easy to grab, follow finger precisely, respect minimum duration, and snap to nearby edges/playhead.
Additional follow-up requirement: GESTURE PHYSICS + SCROLL INERTIA PART 2:

11. OVERLAY DRAG:
Function: text/sticker/overlay drag on preview must be 60fps, follow finger precisely, support center snap guides, and never jitter.

12. OVERLAY PINCH SCALE:
Function: pinch scaling overlay must be centered around gesture midpoint, clamp min/max scale, and update bounding box instantly.

13. OVERLAY ROTATE:
Function: two-finger rotate must rotate smoothly, support angle snapping at 0/45/90 degrees if close, and avoid sudden jumps.

14. MULTI-TOUCH PRIORITY:
Function: distinguish tap, drag, pinch, rotate, trim, and timeline scroll correctly to prevent gesture conflict.

15. TAP SELECTION:
Function: tap selects clip/overlay instantly; tap empty area clears selection; double tap text opens text editor.

16. LONG PRESS:
Function: long press on timeline clip enables drag/reorder mode with small scale-up/shadow feedback.

17. VISUAL FEEDBACK:
Function: show snap guide lines, selected borders, lifted clip shadow, trim handle highlight, and tool highlight during gestures.

18. HAPTIC FEEDBACK:
Function: add light haptic feedback for snap, split, delete, successful drop, and invalid action when available.

19. PERFORMANCE:
Function: gestures must not trigger heavy recomposition; use lightweight state updates, derived state, pointerInput, Animatable/Decay animation, and background thumbnail loading.

20. STRICT:
Function: no laggy gesture, no accidental clip jump, no broken pinch zoom, no preview desync, no fake physics, production-ready only.
Additional follow-up requirement: GESTURE PHYSICS + SCROLL INERTIA PART 3:

21. TIMELINE DECAY ANIMATION:
Function: implement velocity-based decay after timeline fling using Compose decay animation, keep scrollOffsetPx clamped safely, update currentTimeMs continuously, and stop cleanly when user touches again.

22. TOUCH INTERRUPTION:
Function: if user touches timeline during fling/playback scroll, immediately stop inertia animation, cancel running scroll job, and give full control back to user without jump.

23. AUTO SCROLL WHILE DRAGGING:
Function: when dragging a clip near left/right edge of visible timeline, auto-scroll timeline slowly in that direction while keeping clip under finger.

24. DRAG BOUNDARY CLAMP:
Function: prevent clips from moving before 0ms, outside project duration, or into invalid positions; show resistance instead of hard snap when user drags beyond boundary.

25. TRIM PREVIEW SCRUB:
Function: while trimming left/right handle, update preview to trim boundary frame in real time so user sees exact cut point.

26. MAGNETIC SNAP PRIORITY:
Function: snap priority order must be playhead first, neighbor clip edges second, transition boundary third, beat markers fourth, timeline start/end last.

27. SNAP RELEASE SETTLE:
Function: when user releases a dragged/trimmed clip near snap target, animate it into exact snapped position within 80-140ms without bounce or overshoot.

28. INVALID DROP RECOVERY:
Function: if user drops clip into invalid position, animate clip back to last valid position, show short feedback, and keep timeline state unchanged.

29. SELECTION STABILITY:
Function: selected clip/overlay must remain selected during drag/trim/scale/rotate and only clear when user taps empty space or selects another item.

30. GESTURE DEBUG SAFETY:
Function: internally separate gesture state into Idle, Scrolling, Flinging, DraggingClip, TrimmingClip, ScalingOverlay, RotatingOverlay, MovingOverlay, Playing to avoid conflicting updates.
Additional follow-up requirement: GESTURE PHYSICS + SCROLL INERTIA PART 4:

31. PREVIEW SCRUB FEEL:
Function: timeline scrubbing must feel instant; when user scrolls timeline, preview seeks with throttled but responsive updates, avoids over-seeking, and always lands on exact final frame after scroll stops.

32. SEEK THROTTLING:
Function: during fast timeline scroll, throttle preview seek requests to avoid ExoPlayer overload, but update final seek immediately when user stops.

33. FRAME ACCURACY:
Function: when user releases scroll/trim, calculate exact currentTimeMs and seek preview to accurate frame based on timeline mapping.

34. PLAYBACK LOCK:
Function: when playback is running, disable clip drag/trim gestures or pause playback automatically before editing, matching CapCut-style behavior.

35. TOOL PANEL GESTURE ISOLATION:
Function: gestures inside bottom panels must not accidentally scroll timeline; timeline pointer input only active inside timeline bounds.

36. HANDLE TOUCH TARGET:
Function: trim/resize handles must have larger invisible touch target than visual size to make editing easy on mobile.

37. MINIMUM MOVEMENT THRESHOLD:
Function: small accidental finger movement after tap should not start drag; use touch slop threshold before entering drag state.

38. LAYER HIT PRIORITY:
Function: when multiple overlays overlap in preview, select the topmost visible layer by zIndex at touch point.

39. OVERLAY OUTSIDE CANVAS:
Function: allow overlay to move partially outside canvas but keep enough selectable area; show boundary guides and prevent losing item completely.

40. STRICT:
Function: gestures must be stable across small screens, large screens, landscape/portrait, high refresh rate devices, and low-end Android devices.
Additional follow-up requirement: PROJECT ARCHITECTURE FINALIZATION: Split app into clear modules: ui, editor, timeline, media, render, data, design-system. Create stable models Project, Track, Clip, Overlay, AudioClip, TextClip, StickerClip, EffectClip, Transition, Keyframe, ExportSettings. Ensure every screen uses ViewModel + StateFlow and no business logic is hidden inside Composable.
Additional follow-up requirement: QUALITY + BUGFIX PASS: Review the entire app and fix all broken flows: media import, editor open, timeline sync, preview playback, add audio/text/sticker, trim/split/delete, undo/redo, autosave, export, save/share. Remove all placeholder buttons, fake logic, unused code, broken navigation, UI overlap, clipped text, and laggy gestures.
Additional follow-up requirement: TESTING SCENARIOS: Add manual and automated test cases for: image-only project, video-only project, mixed image/video, add music, extract audio, add text, add sticker, apply filter/effect, transition, trim, split, reorder, undo/redo, reopen draft, export 720p/1080p, cancel export, share video, permission denied, missing file, low storage.
Additional follow-up requirement: RELEASE HARDENING: Prepare app for production release. Optimize memory, clear temp cache, handle Android permissions correctly, support Android 13+ media permissions, fix crashes, add friendly error messages, add app icon/name, privacy-safe storage behavior, ProGuard/R8 safe rules if needed, and ensure release build compiles without lint/manifest errors.

### Core Features
- Splash -> onboarding/intro -> main app flow
- Language selection with English and Vietnamese support
- Project dashboard with create, rename, duplicate, delete, autosave drafts, thumbnail generation, and resume last edit state
- Media import for images, videos, and audio using Android system pickers with multi-select, preview, duration, file size, and large-file handling safeguards
- Editor workspace with dark modern premium UI, preview canvas, centered playhead, and multi-track timeline for video, audio, text, stickers, and effects
- Timeline editing with drag reorder, trim start/end, split at playhead, duplicate clip, snap edges, pinch zoom, smooth scrolling, thumbnails, and real-time preview sync
- Video tools for crop fit/fill/custom, rotate, flip, aspect ratios 1:1/9:16/16:9, speed 0.5x-2x, freeze frame, opacity, replace clip, and mute source audio
- Image tools for duration, crop/zoom, rotate/flip, and Ken Burns-style pan animation
- Audio tools for background music, extracted audio, trim, split, fade in/out, volume, loop, mute video audio, multiple audio layers, and bundled sound effects
- Text overlays with content editing, font family, size, color, background, stroke, shadow, alignment, duration, multiple layers, and basic fade/slide/pop/typewriter animations
- Sticker overlays with emoji/shapes/local animated sticker assets, drag/scale/rotate gestures, duplicate/delete, duration, and layer order controls
- Filters and adjustments per clip including warm, cool, vintage, cinematic, black-and-white, brightness, contrast, saturation, exposure, temperature, sharpness, and vignette
- Effects and transitions including blur, glow, shake, zoom, flash, VHS, glitch, fade, directional slide, zoom, and blur transitions with duration control
- Overlay system for image/video layers with resize, rotate, position, opacity, and simple blend mode support where feasible
- Canvas controls for background color, background blur, aspect ratio, border, and padding
- Keyframe MVP for position, scale, rotation, and opacity with linear interpolation
- Gesture system for drag, pinch zoom, rotate, alignment guides, and multi-touch element manipulation
- Undo/redo stack covering timeline and property edits
- Export to MP4 first with configurable 720p/1080p/2K/4K, 24/30/60 FPS, bitrate/quality, progress, cancel, save to gallery, and share output; MOV may be listed as a later compatibility target if Android encoder support is constrained
- Settings area with language, export defaults, storage/cache controls, app info, and explicit exit action/path
- Performance foundations including lazy thumbnail loading, background rendering/export, media proxy strategies for large files, memory-aware preview, and responsive Compose UI

### Main Screens
- Main Editor Screen
- Media Permission And Picker Flow
- Export And Share UI
- Release App Identity
- Release Build And Diagnostics

### Architecture Core
- ui: Jetpack Compose
- pattern: MVVM
- storage: Keep existing repository and editor persistence boundaries. Harden storage by using app-owned cache/temp directories for intermediate render files, privacy-safe scoped storage or MediaStore/FileProvider content URIs for exported/shared videos, and persistable URI permissions only for user-selected media that must survive process restarts. Do not add broad external storage access or rewrite project persistence. Add cleanup, permission, error-mapping, and release-build safeguards around existing ViewModel, repository, media import, render/export, manifest, and resource boundaries.
<!-- AUTO-GENERATED:CORE_END -->
