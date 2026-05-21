# TODO.md - Clipy Feature & UX/UI Checklist

## 🚀 Core Features (MVP)

### Onboarding & Navigation
- [x] Splash screen với branding
- [x] Onboarding flow cho first-time users
- [x] Home screen với 4 tabs: Studio, Library, Tools, Settings
- [x] Bottom glass navigation bar
- [x] Tab switching animation mượt (using HorizontalPager)

### Studio Page (Main Workspace)
- [x] Hero card với gradient background
- [x] Quick access tools grid
- [x] Recent exports preview
- [x] Tool cards với icons + labels

### Library Page
- [x] Danh sách exported files
- [x] Thumbnail preview cho videos
- [x] File metadata (size, duration, date)
- [x] Share/delete actions
- [x] Empty state khi chưa có exports
- [x] Pull-to-refresh
- [x] "See All" shortcut to Output History

### Tools Page
- [x] Grid layout các tools (10+ tools correctly wired)
- [x] Tool categories (Basic, Advanced, Coming Soon)
- [x] Tool card design consistent
- [x] Disabled state cho future tools

### Settings Page
- [x] App version info
- [x] Storage management (Correctly calculating & clearing cache)
- [x] Export quality preferences
- [ ] Theme settings (nếu có)
- [x] About section
- [x] Clear cache option (Deletes imports/outputs)

---

## 🎬 Video Editing Tools

### Cut/Trim Video
- [x] Pick video từ gallery
- [x] Video player với timeline
- [x] Drag handles để chọn start/end
- [x] Preview trimmed segment
- [x] Duration display
- [x] Validate min/max duration
- [x] Export trimmed video
- [x] Loading state during processing
- [x] Success result screen

### Compress Video
- [x] Pick video từ gallery
- [x] Show original file size
- [x] Quality presets (Low, Medium, High)
- [x] Custom bitrate slider
- [x] Estimated output size
- [ ] Preview quality comparison
- [x] Export compressed video
- [x] Progress bar với percentage
- [x] Result Screen với size comparison

### Merge Videos
- [x] Pick multiple videos
- [x] Reorder videos trong list (Up/Down buttons)
- [x] Remove video từ merge list
- [x] Preview thumbnails
- [x] Total duration display
- [x] "Add More" clips during merge setup
- [x] Transition selection (Crossfade, Wipe, Slide, etc.)
- [x] Export merged video
- [x] Handle different resolutions
- [x] Handle different formats

### Extract Audio
- [x] Pick video file
- [x] Show video metadata
- [x] Audio format selection (MP3, AAC, WAV)
- [x] Bitrate options
- [ ] Preview audio waveform (optional)
- [x] Export audio file
- [x] Result Screen với audio player

### Slideshow from Images
- [x] Pick multiple images
- [x] Reorder images (Up/Down buttons)
- [x] Remove images
- [x] "Add More" images during slideshow setup
- [x] Duration per image slider
- [x] Transition effects dropdown
- [x] Background music picker
- [x] Preview slideshow
- [x] Export as video
- [x] Handle different image sizes/ratios

---

## 🎨 Advanced Tools (Placeholders/Future)

### Filters
- [x] Filter presets grid (Sepia, Grayscale, Cyberpunk)
- [x] Real-time preview (GPU accelerated via ClipyGpuFilterManager)
- [x] Intensity slider (Brightness, Contrast, Saturation)
- [x] Export với filter applied (FFmpeg mapping)

### Crop Video
- [x] Aspect ratio presets (1:1, 16:9, 9:16, 4:3)
- [x] Custom crop area selector
- [x] Drag/resize crop box
- [x] Preview cropped result
- [x] Export cropped video

### Rotate Video
- [x] Rotate 90° CW/CCW buttons
- [x] Flip horizontal/vertical
- [x] Preview orientation
- [x] Export rotated video

### Speed Control
- [x] Speed slider (0.25x - 4x)
- [x] Preset buttons (0.5x, 1x, 2x)
- [x] Duration recalculation
- [x] Preview với new speed
- [x] Export speed change (video + audio)

### Text Overlay
- [x] Text input field
- [x] Font color picker (White, Red, Green, Blue, etc.)
- [x] Size slider
- [x] Position drag on preview (Percentage-based)
- [ ] Animation options (fade in/out, scroll) - **ROADMAP**
- [ ] Timeline để set start/end time - **ROADMAP**
- [x] Export with drawtext filter

### Stickers
- [x] Sticker library grid (Local assets)
- [x] Drag sticker onto video preview (Percentage-based)
- [ ] Resize/rotate sticker - **ROADMAP**
- [ ] Timeline để set duration - **ROADMAP**
- [x] Export with overlay filter

---

## 📂 Media Pickers

### Pick Video Screen
- [x] Gallery grid với thumbnails
- [x] Video duration overlay
- [x] File size display
- [x] Search/filter
- [x] Permission handling
- [x] Empty state
- [x] Loading state

### Pick Multiple Videos Screen
- [x] Multi-select mode
- [x] Selected count badge
- [x] Clear selection button
- [x] Confirm selection button

### Pick Images Screen
- [x] Gallery grid
- [x] Multi-select
- [x] Permission handling
- [x] Confirm selection button

### Pick Audio Screen
- [x] Audio files list
- [x] Duration display
- [x] Play preview button
- [x] Search/filter

---

## ⚙️ Processing & Results

### Processing Screen
- [x] Progress bar animated
- [x] Percentage display
- [x] Current operation label
- [x] Cancel button
- [x] Handle cancellation gracefully
- [x] Error handling với retry
- [x] **Background processing via WorkManager**

### Result Screen
- [x] Success animation/icon
- [x] Output file preview (thumbnail/player)
- [x] File metadata (size, duration, path)
- [x] Share button
- [x] **Save to Gallery (MediaStore compliance)**
- [x] Open in player button
- [x] Back to home button

### Video Player Screen
- [x] Full-screen video player
- [x] Play/pause controls
- [x] Seek bar
- [x] Current time / total time
- [x] Back button

---

## ✨ UI/UX Polish

### Design System
- [x] Dark theme consistent across app
- [x] Color palette defined (primary, accent, error, success)
- [x] Typography scale (H1-H6, body, caption)
- [x] Spacing system (4dp grid)
- [x] Border radius consistent
- [x] Glass morphism effects (bottom nav, cards)

### Components
- [x] ClipyButton (primary, secondary)
- [x] ClipyCard với gradient borders
- [x] ClipyLoadingIndicator
- [x] ClipyTextField
- [x] ClipySlider
- [x] ClipyTopBar
- [x] ClipyErrorState

### Animations
- [x] Screen transitions smooth (fadeIn/fadeOut)
- [x] Button press feedback
- [x] List item animations
- [x] Progress animations

---

## 🛠️ Technical Requirements

### Architecture
- [x] Clean Architecture (3 Gradle modules: :app, :edit, :export)
- [x] MVVM với ViewModels
- [x] UiState + UiEvent pattern
- [x] Repository pattern
- [x] Hilt dependency injection (Infrastructure ready)

### Media Processing
- [x] FFmpeg integration (FFmpeg Kit)
- [x] Media3 Transformer for hardware acceleration (Cut, Compress, Rotate, Speed, Crop)
- [x] Video metadata extraction
- [x] Thumbnail generation
- [x] Audio extraction
- [x] Video compression
- [x] Video merging
- [x] Error handling cho FFmpeg failures

### Storage & Permissions
- [x] Runtime permissions (READ_MEDIA)
- [x] **Scoped storage compliance (Internal file importing + MediaStore exporting)**
- [x] File provider setup
- [x] Cache management
- [x] Output directory structure

### Performance
- [x] Lazy loading cho galleries
- [x] Image/video thumbnail caching (Coil)
- [x] Smooth 60fps UI

### Build & Release
- [x] ProGuard rules enabled
- [x] Version management
- [x] Compile SDK 36 (Android 16 Preview compatible)

---

## 🐛 Bug Fixes & Edge Cases

### General
- [x] Handle back navigation từ mỗi screen
- [x] Handle orientation changes
- [x] Percentage-based overlay coordinates (Fixed pixel scaling bug)
- [x] Integer division fix in FFmpeg command generation
- [x] Process death handling via WorkManager

---

## 🤖 Platform Compliance

### Android
- [x] Target SDK 34+
- [x] Min SDK 24 (Android 7.0)
- [x] Material Design 3 guidelines
- [x] Adaptive icons
- [x] Edge-to-edge layout

---

**Last Updated:** 2026-05-21  
**Status:** MVP COMPLETE & PRODUCTION READY  
**Target Release:** Stable v1.0
