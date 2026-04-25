# Project Notes

<!-- AUTO-GENERATED:CORE_START -->
## Core App Snapshot (Auto)
- Last updated: 2026-04-25 07:10 UTC
- App: Clipy Studio
- Slug: clipy
- Tagline: A premium offline-friendly Android video editor for fast, polished social videos.
- Target users: General users
- Design direction: Incremental Editor Screen update only: preserve the existing dark premium Material 3 studio language, but make the editor feel closer to a professional mobile video timeline workspace with a fixed-center playhead, dense tool access, high-contrast clip selection, and contextual bottom panels. The screen should prioritize edit precision, thumb-friendly controls, immediate preview feedback, and non-overlapping safe-area behavior across phones and tablets.
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
- Editor Workspace
- Preview Canvas
- Timeline Area
- Clip Edit Panel
- Bottom Tool Menu
- Audio Tool Panel
- Text Tool Panel
- Export Handoff

### Architecture Core
- ui: Jetpack Compose
- pattern: MVVM
- storage: Continue using the existing SharedPreferences-backed repository and in-memory project state for the MVP. Extend current ViewModel state and timeline models only where needed for editor interactions, autosave snapshots, undo/redo command history, selected tool/panel state, overlay transforms, audio/text insertion, timeline zoom, and export handoff. Do not introduce Room, Media3, or native rendering in this follow-up unless already present in the codebase; represent preview frames, thumbnails, and waveforms with buildable MVP placeholders tied to real project/timeline state.
<!-- AUTO-GENERATED:CORE_END -->
