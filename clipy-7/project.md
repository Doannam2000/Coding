# Project Notes

<!-- AUTO-GENERATED:CORE_START -->
## Core App Snapshot (Auto)
- Last updated: 2026-04-25 06:04 UTC
- App: Clipy Studio
- Slug: clipy
- Tagline: A premium offline-friendly Android video editor for fast, polished social videos.
- Target users: General users
- Design direction: Premium offline-first Android video editor with an original dark studio aesthetic: graphite-black surfaces, subtle depth, neon-cyan editing accents, rounded Material 3 components, dense but readable controls, and a timeline-first workspace that feels professional without cloning any specific competitor. The UI should prioritize accurate preview-timeline sync, clear functional affordances, and fast short-video creation for 9:16, 1:1, and 16:9 projects.
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
- Splash
- Intro
- Language Selection
- Project Dashboard
- Media Import
- Editor Workspace
- Export And Settings

### Architecture Core
- ui: Jetpack Compose
- pattern: MVVM
- storage: Room for projects, timeline state, settings, and undo/redo metadata; Android MediaStore and app-scoped files for imported media references, generated thumbnails, waveform caches, proxy files, autosave snapshots, and exported videos. Media playback and preview should use Media3/ExoPlayer with Compose integration. Export should use a staged Android media pipeline with MediaCodec/MediaMuxer or a proven FFmpeg-based library if licensing and app size are acceptable.
<!-- AUTO-GENERATED:CORE_END -->
