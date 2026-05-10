# CLIPY_TODO_UX_UI_FULL.md

App name: Clipy  
Package name: `com.nantcompany.clipy`  
Primary goal: Build a complete, polished, dark-theme video utility app for Android.  
Core tools: Cut/Trim Video, Compress Video, Merge Videos, Extract Audio, Slideshow from Images.  
Future expansion placeholders: Filters, Stickers, Text Overlay, Crop, Rotate, Speed, Effects, GPU Preview, Timeline Editor.

This document is written for Claude Code / autonomous coding agents.  
Claude may generate the required data models, UI states, ViewModels, repositories, use cases, and implementation details based on the existing codebase.  
However, Claude must follow the UX, UI, product behavior, validation, testing, and completion rules in this document exactly.

---

# 0. AUTONOMOUS EXECUTION RULES

## 0.1 Main rule

Work autonomously until the app is complete.

Do not stop after one screen.
Do not stop after one feature.
Do not stop only because the project compiles once.
Do not stop because one export command returns success.
Do not stop because a tool/API/library returns HTTP 200 or success.

A feature is only done when:

- The user can complete the full flow from Home to Result.
- The UI is visually correct and polished.
- Loading, empty, error, invalid input, success, and back states are handled.
- Export result is verified, not assumed.
- The output file exists and is usable where applicable.
- The app does not crash on cancellation, invalid input, back navigation, or missing metadata.
- `./gradlew assembleDebug` passes after meaningful changes.

## 0.2 Completion signal

Only output:

```text
COMPLETE
```

when all checklist items in this file are complete and verified.

If not complete, output:

```text
NOT COMPLETE
```

and list what is done, what remains, and what is blocking progress.

## 0.3 Serious testing rule

Every core feature must be tested seriously before marking it done.

Testing must include:

- Valid input
- Invalid input
- Cancel picker
- Remove selected media
- Back navigation
- Export success
- Export failure or simulated failure if possible
- Output verification
- UI layout check
- Small screen safety check
- Build check

## 0.4 Safety rules

Never modify:

- Package name
- App ID
- Signing config
- Keystore files
- `.env`
- `google-services.json`
- Production credentials
- Release signing setup
- Secret API keys

Do not delete existing modules unless absolutely necessary.
Do not rewrite the entire project if focused implementation can complete the app.
Do not add fake working buttons.
If a feature is planned for later, show a clean disabled placeholder.

## 0.5 Required build commands

After meaningful code changes, run:

```bash
./gradlew assembleDebug
```

If available, also run:

```bash
./gradlew test
./gradlew lintDebug
```

If build/test/lint fails:

- Read the error.
- Fix the root cause.
- Rerun the command.
- Do not ignore failed commands.

---

# 1. PRODUCT DESCRIPTION

Clipy is a fast, modern, dark video toolkit app.

The user should open the app and immediately understand:

- I can cut videos.
- I can compress videos.
- I can merge multiple videos.
- I can extract audio from a video.
- I can create a slideshow video from images.
- I can find my exported files later.

Clipy should feel like a real production app, not a demo project.

The first version is not a full CapCut clone.  
It is a focused video utility toolkit with high-quality UX and clean architecture that can later evolve into a full editor.

Future editing tools must appear as planned placeholders, not broken functions.

---

# 2. DESIGN DIRECTION

## 2.1 Visual style

Use a modern dark creative video-editor style.

The app should look closer to CapCut-style utility UX than a default Material demo app.

Style keywords:

- Dark
- Premium
- Clean
- Compact
- Smooth
- Tool-focused
- Mobile-first
- Creator-friendly
- No clutter
- No oversized typography
- No boring white screens

## 2.2 Color system

Use a dark theme by default.

Suggested palette:

- App background: near black, charcoal, or very dark gray
- Main surface: dark gray
- Elevated card: slightly lighter dark gray
- Primary accent: cyan, blue, purple, or a subtle blue-purple gradient
- Success: green
- Error: red
- Warning: orange/yellow
- Primary text: white
- Secondary text: gray
- Disabled text: muted gray
- Dividers: transparent or subtle gray

Rules:

- Do not use a plain white background.
- Do not make the whole UI pure black with no hierarchy.
- Use contrast carefully so text is readable.
- Disabled buttons must look disabled clearly.
- Error states must be visible but not ugly.

## 2.3 Typography

Typography must be balanced.

Use hierarchy:

- Large title: app name, screen title, hero title
- Medium title: section headers, tool names
- Body: descriptions and normal content
- Small: metadata, duration, file size, status

Rules:

- Avoid huge text that clips.
- Avoid tiny unreadable text.
- Do not use long paragraphs in cards.
- Long file names must ellipsize safely.
- Buttons must not clip text.

## 2.4 Spacing

Use consistent spacing.

Recommended:

- Screen horizontal padding: 16dp
- Top section spacing: 16dp to 24dp
- Card padding: 14dp to 20dp
- Card spacing: 12dp to 16dp
- Bottom action spacing: 12dp
- Safe area bottom padding: required

Rules:

- No elements touching screen edges.
- No overlapping UI.
- No half-screen blank area.
- No bottom button hidden behind navigation bar.
- No status bar overlap.

## 2.5 Shapes

Use rounded UI.

Recommended:

- Main cards: 20dp to 28dp
- Tool cards: 20dp
- Buttons: 16dp to 24dp
- Thumbnail corners: 14dp to 18dp
- Dialogs/sheets: 24dp

## 2.6 Motion and feedback

Use subtle motion:

- Button ripple/press feedback
- Card press feedback
- Loading shimmer for media thumbnails
- Smooth progress animation during export
- Soft screen transitions if already available

Do not add heavy animation that causes lag.
Do not block the main thread.

---

# 3. ARCHITECTURE REQUIREMENTS

## 3.1 Module rule

Do not create too many modules.

Preferred modules:

```text
app/
  Main Android application, navigation, theme, app-level DI, settings, home, shell UI

edit/
  Feature screens and editing flows:
  - media picker flow
  - preview flow
  - cut
  - compress
  - merge
  - extract audio
  - slideshow
  - future placeholders

export/
  Export engine abstraction:
  - export jobs
  - progress state
  - output file naming
  - output verification
  - export history
  - temp cleanup
```

If the existing project already has a different but reasonable structure, adapt without destructive rewrites.

## 3.2 Claude may generate models

Claude may generate any required models and state classes, such as:

- Media item model
- Media metadata model
- Export job model
- Export result model
- Export progress model
- Tool type enum
- Screen UI state
- Error model
- Export history entity
- Settings model

But the generated models must support all UX and feature requirements in this document.

## 3.3 Required state handling

Every screen must handle:

- Idle
- Loading
- Empty
- Content ready
- Invalid input
- Error
- Success
- Exporting where applicable

Do not create screens that assume perfect data.

## 3.4 Required reusable components

Create or reuse polished components:

- ClipyTopBar
- ClipyPrimaryButton
- ClipySecondaryButton
- ClipyBottomActionBar
- ClipyToolCard
- ClipyMediaCard
- ClipyThumbnail
- ClipyEmptyState
- ClipyErrorState
- ClipyLoadingState
- ClipyExportProgress
- ClipyPresetCard
- ClipySettingItem
- ClipySectionTitle
- ClipyConfirmationDialog
- ClipyDisabledFeatureCard
- ClipyInfoCard
- ClipyWarningCard

Component rules:

- Must support small screens.
- Must not clip text.
- Must have enabled/disabled states.
- Must be reusable.
- Must follow dark theme.

---

# 4. NAVIGATION STRUCTURE

## 4.1 Required screens

Implement these screens:

- SplashScreen
- OnboardingScreen
- HomeScreen
- MediaPickerScreen
- MediaPreviewScreen
- CutTrimScreen
- CompressScreen
- MergeScreen
- ExtractAudioScreen
- SlideshowScreen
- ExportProgressScreen
- ExportResultScreen
- RecentExportsScreen
- SettingsScreen
- FutureToolsPlaceholderScreen

## 4.2 Suggested route names

Claude may adapt route names, but the flow must exist.

```text
splash
onboarding
home
media_picker/{toolType}
media_preview/{toolType}
cut_trim
compress
merge
extract_audio
slideshow
export_progress/{jobId}
export_result/{resultId}
recent_exports
settings
future_tools
```

## 4.3 Navigation behavior

Rules:

- Splash should not remain in back stack after Home.
- Onboarding should not reappear after completed.
- Back from tool screens returns to previous screen safely.
- Back during export must be handled carefully.
- User must never get stuck.
- User must never see blank screen after navigation.

---

# 5. SPLASH SCREEN

## 5.1 Purpose

Show a clean app entry and route user to Onboarding or Home.

## 5.2 UI requirements

Splash screen must include:

- Dark background
- Clipy logo or generated app icon
- App name: Clipy
- Tagline: “Fast video tools” or similar
- Small loading indicator or subtle animated dot

## 5.3 UX behavior

- Show briefly.
- Do not delay longer than necessary.
- Navigate to onboarding if first launch.
- Navigate to home if onboarding completed.
- No back navigation to splash after leaving.

## 5.4 Test checklist

- [ ] Cold start opens splash
- [ ] Splash is dark and polished
- [ ] First launch goes to onboarding
- [ ] Later launch goes to home
- [ ] Back does not return to splash
- [ ] No crash on startup

---

# 6. ONBOARDING SCREEN

## 6.1 Purpose

Explain Clipy quickly and make the app feel polished.

## 6.2 UI options

Use either 3-page onboarding or a single elegant onboarding screen.

Recommended 3 pages:

1. Cut and compress videos quickly
2. Merge clips and extract audio
3. Create slideshow videos from photos

Each page must include:

- Icon or simple illustration
- Title
- Short description
- Page indicator
- Next button
- Skip button

## 6.3 UX behavior

- Skip goes to Home.
- Finish goes to Home.
- Save onboarding completed state.
- On next launch, go directly to Home.

## 6.4 Test checklist

- [ ] Onboarding appears on first launch
- [ ] Skip works
- [ ] Next works
- [ ] Finish works
- [ ] Completed state saved
- [ ] Relaunch skips onboarding
- [ ] No text clipping

---

# 7. HOME SCREEN

## 7.1 Purpose

Home is the main dashboard. It must immediately show all tools and recent exports.

## 7.2 Layout structure

Required layout:

```text
Top bar
  - App name: Clipy
  - Settings icon
  - Optional small badge or menu icon

Hero card
  - Strong headline
  - Short subtitle
  - Main action button
  - Secondary action button

Tool grid
  - Cut Video
  - Compress Video
  - Merge Videos
  - Extract Audio
  - Slideshow
  - More Tools / Coming Soon

Recent exports section
  - Last 3 exports if available
  - Empty state if none

Bottom safe area
```

## 7.3 Top bar details

Top bar must include:

- App title: Clipy
- Optional subtitle: Video Toolkit
- Settings icon on the right

Rules:

- Respect status bar.
- Do not use giant top padding.
- Do not overlap with status bar.

## 7.4 Hero card details

Hero card content:

Title examples:

- “Create faster with Clipy”
- “Fast video tools in one place”

Subtitle examples:

- “Cut, compress, merge, extract audio, and create slideshows in seconds.”

Actions:

- Primary: “Pick a video”
- Secondary: “Recent exports”

Behavior:

- Pick a video may open MediaPicker for Cut by default or show tool picker.
- Recent exports opens RecentExportsScreen.

## 7.5 Tool cards

Each tool card must contain:

- Icon
- Tool name
- Short description
- Supported media type label
- Action indicator or arrow

### Cut Video card

Icon: scissors  
Title: Cut Video  
Description: Trim start and end quickly  
Label: Video

Behavior:

- Opens single video picker.

### Compress Video card

Icon: compress/minimize  
Title: Compress  
Description: Reduce file size with quality presets  
Label: Video

Behavior:

- Opens single video picker.

### Merge Videos card

Icon: merge/layers  
Title: Merge  
Description: Join multiple clips into one video  
Label: Multi video

Behavior:

- Opens multiple video picker.

### Extract Audio card

Icon: music/audio wave  
Title: Extract Audio  
Description: Save audio from a video file  
Label: Video to audio

Behavior:

- Opens single video picker.

### Slideshow card

Icon: image stack  
Title: Slideshow  
Description: Turn photos into a video  
Label: Images

Behavior:

- Opens multiple image picker.

### More Tools card

Icon: sparkles/magic  
Title: More Tools  
Description: Filters, stickers, text and effects soon  
Label: Coming soon

Behavior:

- Opens FutureToolsPlaceholderScreen.

## 7.6 Recent exports preview

If exports exist:

- Show up to 3 recent exports.
- Each item shows thumbnail/icon, file name, type, date, and size.
- Tapping item opens result/open flow.
- Show “See all” button.

If empty:

- Show empty state card.
- Text: “No exports yet”
- Subtext: “Your finished videos and audio will appear here.”
- Optional button: “Create now”

## 7.7 Home test checklist

- [ ] Home screen implemented
- [ ] Top bar visible and safe
- [ ] Hero card implemented
- [ ] Tool grid implemented
- [ ] Every tool card has icon/title/description/label
- [ ] Cut card routes correctly
- [ ] Compress card routes correctly
- [ ] Merge card routes correctly
- [ ] Extract Audio card routes correctly
- [ ] Slideshow card routes correctly
- [ ] More Tools routes correctly
- [ ] Recent exports preview works
- [ ] Empty recent export state works
- [ ] Small screen checked
- [ ] No clipped text
- [ ] No overlapping UI
- [ ] Back behavior checked

---

# 8. MEDIA PICKER SCREEN

## 8.1 Purpose

Clipy must have its own media selection confirmation UI.

System picker may be used internally, but after selection the app must show selected files in Clipy UI.

## 8.2 Picker modes

Support these modes:

- Single video mode for Cut
- Single video mode for Compress
- Single video mode for Extract Audio
- Multiple video mode for Merge
- Multiple image mode for Slideshow

## 8.3 Layout

Required structure:

```text
Top bar
  - Back button
  - Dynamic title
  - Selected count if multiple mode

Instruction card
  - Tool-specific instruction
  - Supported media type

Selected media section
  - Empty state before selection
  - Media list/grid after selection

Pick button
  - Select video
  - Select videos
  - Select images

Bottom action bar
  - Continue button
  - Disabled until valid input
```

## 8.4 Dynamic titles

Cut:

- Title: “Select video to cut”

Compress:

- Title: “Select video to compress”

Merge:

- Title: “Select videos to merge”

Extract Audio:

- Title: “Select video for audio”

Slideshow:

- Title: “Select photos”

## 8.5 Instruction card text

Cut:

- “Choose one video. You will trim the start and end on the next screen.”

Compress:

- “Choose one video. Then select a quality preset and export a smaller file.”

Merge:

- “Choose at least two videos. The order you select will be the merge order.”

Extract Audio:

- “Choose one video. Clipy will save the audio as a separate file.”

Slideshow:

- “Choose at least two images. Clipy will turn them into a video slideshow.”

## 8.6 Selected media card

Each selected item must show:

- Thumbnail or icon
- File name
- Media type
- Duration for video
- File size if available
- Resolution if available
- Order number for merge/slideshow
- Remove button

If metadata is loading:

- Show thumbnail skeleton or spinner.
- Do not block UI forever.

If metadata fails:

- Show fallback icon.
- Show “Metadata unavailable”.
- Still allow user to remove the item.

## 8.7 Validation rules

Cut:

- Requires exactly 1 video.

Compress:

- Requires exactly 1 video.

Extract Audio:

- Requires exactly 1 video.

Merge:

- Requires at least 2 videos.

Slideshow:

- Requires at least 2 images.

Continue button must be disabled until valid.

Invalid state must explain what is wrong.

Examples:

- “Please select one video.”
- “Select at least 2 videos to merge.”
- “Select at least 2 images to create a slideshow.”

## 8.8 Picker cancellation

If user cancels picker:

- Do not crash.
- Stay on MediaPickerScreen.
- Keep previous selected items if any.
- Optionally show small message: “No media selected.”

## 8.9 Test checklist

- [ ] Single video picker works
- [ ] Multiple video picker works
- [ ] Multiple image picker works
- [ ] Cancel picker does not crash
- [ ] Selected media cards show metadata
- [ ] Remove item works
- [ ] Re-select works
- [ ] Continue disabled for invalid state
- [ ] Continue enabled for valid state
- [ ] Merge blocks one video
- [ ] Slideshow blocks one image
- [ ] Back works
- [ ] Metadata failure handled
- [ ] Small screen layout checked

---

# 9. MEDIA PREVIEW SCREEN

## 9.1 Purpose

Before editing, user should confirm selected media.

## 9.2 Video preview UI

For video tools, show:

- Video preview player
- Play/pause button
- Current time and total duration
- File name
- File size
- Resolution if available
- Continue button

## 9.3 Image preview UI

For slideshow, show:

- Grid or horizontal image preview
- Selected count
- Order number
- Remove option if user returns to picker or if supported
- Continue button

## 9.4 Player lifecycle

If using ExoPlayer/Media3:

- Initialize safely.
- Pause on lifecycle pause.
- Release on dispose.
- Do not leak player.
- If preview fails, show fallback thumbnail and error message.

## 9.5 Test checklist

- [ ] Video preview loads
- [ ] Play/pause works
- [ ] File info shown
- [ ] Image preview shown
- [ ] Continue routes to correct editor
- [ ] Back releases player
- [ ] Preview failure handled
- [ ] No crash on lifecycle pause/resume

---

# 10. CUT / TRIM VIDEO SCREEN

## 10.1 Purpose

Allow user to cut a video by selecting start and end time.

## 10.2 Layout

Required structure:

```text
Top bar
  - Back
  - Title: Cut Video

Preview area
  - Video player
  - Play/pause
  - Current time / total duration

Trim area
  - Timeline or range slider
  - Start handle
  - End handle
  - Selected duration
  - Start time text
  - End time text

Fine tune controls
  - Start -0.1s
  - Start +0.1s
  - End -0.1s
  - End +0.1s

Actions
  - Reset
  - Play selection

Export settings
  - Output name
  - Keep original quality toggle if supported

Bottom action
  - Export Cut Video
```

## 10.3 UX details

The user must clearly understand:

- Where the cut starts.
- Where the cut ends.
- How long the final clip will be.

Rules:

- End time cannot be before start time.
- Start time cannot be negative.
- End time cannot exceed video duration.
- Minimum selected duration must be enforced.
- Reset returns to full duration.
- Play selection plays only selected range if possible.
- Export button disabled for invalid range.

If thumbnail timeline is difficult, implement a polished range slider first. Do not leave a broken timeline.

## 10.4 Validation messages

Use clear messages:

- “Invalid trim range.”
- “Selected duration is too short.”
- “Video duration is unavailable.”
- “Could not load this video.”

## 10.5 Export behavior

When user taps Export:

- Create cut export job.
- Navigate to ExportProgressScreen or show full-screen export progress.
- Prevent duplicate taps.
- On success, verify output file exists and size > 0.
- Save to recent exports.
- Navigate to ExportResultScreen.
- On failure, show retry option.

## 10.6 Test checklist

- [ ] Cut screen implemented
- [ ] Video preview works
- [ ] Current time updates
- [ ] Range selection works
- [ ] Start/end validation works
- [ ] Fine tune controls work
- [ ] Reset works
- [ ] Play selection works or gracefully disabled
- [ ] Export button disabled when invalid
- [ ] Export cut works
- [ ] Output file exists
- [ ] Output file size > 0
- [ ] Export result opens
- [ ] Failure state tested
- [ ] Back behavior tested

---

# 11. COMPRESS VIDEO SCREEN

## 11.1 Purpose

Allow user to reduce video file size with simple presets.

## 11.2 Layout

Required structure:

```text
Top bar
  - Back
  - Title: Compress Video

Original file card
  - Thumbnail or preview
  - File name
  - Original size
  - Duration
  - Resolution

Preset section
  - Small file
  - Balanced
  - High quality

Advanced settings
  - Resolution selector
  - Bitrate option or slider if supported
  - FPS option if supported
  - Keep audio toggle
  - Estimated output size if possible

Bottom action
  - Compress Video
```

## 11.3 Preset details

Small file:

- Smaller output
- Lower quality
- Good for sharing

Balanced:

- Default selected
- Good quality and smaller size

High quality:

- Better quality
- Larger file

## 11.4 UX rules

- Default preset must be Balanced.
- User should not need technical knowledge.
- Advanced settings can be collapsed if too much content.
- If estimated size is unavailable, do not fake it.
- If original size is unknown, show “Unknown size”.

## 11.5 Export behavior

On export:

- Create compress export job.
- Show progress.
- Prevent duplicate export.
- Verify output file exists and size > 0.
- Prefer output smaller than input for Small/Balanced where possible.
- Save recent export.
- Show result screen.

## 11.6 Test checklist

- [ ] Compress screen implemented
- [ ] Original file info displayed
- [ ] Preset cards implemented
- [ ] Balanced selected by default
- [ ] Preset switching works
- [ ] Resolution selector works or is safely hidden
- [ ] Keep audio toggle works if supported
- [ ] Compress export works
- [ ] Output file exists
- [ ] Output file size > 0
- [ ] Recent export saved
- [ ] Failure state tested
- [ ] Back behavior tested

---

# 12. MERGE VIDEOS SCREEN

## 12.1 Purpose

Allow user to join multiple videos into one video.

## 12.2 Layout

Required structure:

```text
Top bar
  - Back
  - Title: Merge Videos
  - Add button

Clip order section
  - List or horizontal timeline
  - Thumbnail
  - Order number
  - File name
  - Duration
  - Remove button
  - Drag handle if reorder supported

Preview section
  - Preview selected clip or first clip
  - Play/pause

Settings
  - Output name
  - Fit mode for different sizes
  - Transition placeholder

Bottom action
  - Merge Videos
```

## 12.3 UX details

User must clearly understand the final order.

Rules:

- Require at least 2 videos.
- Show order numbers.
- Allow removing clips.
- Allow adding more clips.
- Reorder should be implemented if feasible.
- If reorder is not implemented, show a clear limitation and keep selected order stable.
- Warn when videos have different orientation or resolution.
- Do not crash with mixed portrait/landscape videos.

## 12.4 Fit mode

If possible, support:

- Fit with background
- Crop to fill
- Keep original / auto

If not possible, show only supported option and explain it.

## 12.5 Transition placeholder

Show:

- None
- Fade: Coming soon, disabled
- Cross dissolve: Coming soon, disabled

Do not make disabled transition buttons clickable.

## 12.6 Export behavior

On export:

- Create merge export job.
- Show progress.
- Prevent duplicate taps.
- Verify output file exists and size > 0.
- Save recent export.
- Show result screen.

## 12.7 Test checklist

- [ ] Merge screen implemented
- [ ] At least 2 videos required
- [ ] Clip order visible
- [ ] Remove clip works
- [ ] Add more clips works
- [ ] Reorder works or limitation is clear
- [ ] Mixed orientation warning implemented
- [ ] Export merge works
- [ ] Output file exists
- [ ] Output file size > 0
- [ ] Recent export saved
- [ ] Failure state tested
- [ ] Back behavior tested

---

# 13. EXTRACT AUDIO SCREEN

## 13.1 Purpose

Allow user to save audio from a selected video.

## 13.2 Layout

Required structure:

```text
Top bar
  - Back
  - Title: Extract Audio

Video info card
  - Thumbnail
  - File name
  - Duration
  - Size
  - Resolution

Audio settings
  - Output format
  - Quality
  - Optional trim range

Preview section
  - Play video or audio preview
  - Current time

Bottom action
  - Extract Audio
```

## 13.3 Output format

Support what the engine can support.

Preferred options:

- MP3
- M4A
- AAC

If only one format is supported, show it clearly and do not display fake options.

## 13.4 Quality options

Use simple choices:

- Low
- Standard
- High

Default: Standard.

## 13.5 UX rules

- If video has no audio, show error before export if detectable.
- On success, result screen should show audio file.
- Audio result should have open/share actions.
- Do not show video-only preview as audio success.

## 13.6 Export behavior

On export:

- Create extract audio job.
- Show progress.
- Prevent duplicate taps.
- Verify output file exists and size > 0.
- Save recent export as audio type.
- Show result screen.

## 13.7 Test checklist

- [ ] Extract Audio screen implemented
- [ ] Video info displayed
- [ ] Format selector implemented or safely simplified
- [ ] Quality selector implemented
- [ ] No-audio case handled if possible
- [ ] Extract audio export works
- [ ] Output audio exists
- [ ] Output file size > 0
- [ ] Audio result opens or shares
- [ ] Recent export saved
- [ ] Failure state tested
- [ ] Back behavior tested

---

# 14. SLIDESHOW SCREEN

## 14.1 Purpose

Allow user to create a video slideshow from selected images.

## 14.2 Layout

Required structure:

```text
Top bar
  - Back
  - Title: Slideshow
  - Add images button

Image sequence section
  - Horizontal list or grid
  - Thumbnail
  - Order number
  - Remove button
  - Drag handle if reorder supported

Preview section
  - Current slide preview
  - Aspect ratio frame
  - Play slideshow preview if possible

Settings
  - Duration per image
  - Aspect ratio
  - Background mode
  - Transition placeholder
  - Music placeholder

Bottom action
  - Create Slideshow
```

## 14.3 Duration per image

Options:

- 1 second
- 2 seconds
- 3 seconds
- 5 seconds
- Custom if easy

Default: 2 or 3 seconds.

## 14.4 Aspect ratio options

Support if possible:

- 9:16
- 1:1
- 16:9
- Original / Auto

Preview frame should visually reflect selected ratio if possible.

## 14.5 Background options

Support if possible:

- Blur
- Black
- Fit
- Fill

If not implemented, use a safe default and show future placeholder for advanced background.

## 14.6 Transition placeholder

Show disabled coming-soon options:

- Fade
- Slide
- Zoom

Do not make them clickable if not implemented.

## 14.7 Music placeholder

Show a disabled card:

- “Add music”
- “Coming soon”

Do not create a broken music picker.

## 14.8 Memory safety

Important for images:

- Do not load full-size bitmaps directly into Compose.
- Use Coil or safe thumbnail loading.
- Avoid OOM with large images.
- Export should decode efficiently.
- Large image failure must show user-friendly error.

## 14.9 Export behavior

On export:

- Create slideshow export job.
- Show progress.
- Prevent duplicate taps.
- Verify output file exists and size > 0.
- Save recent export.
- Show result screen.

## 14.10 Test checklist

- [ ] Slideshow screen implemented
- [ ] At least 2 images required
- [ ] Image sequence visible
- [ ] Remove image works
- [ ] Add more images works
- [ ] Reorder works or limitation is clear
- [ ] Duration setting works
- [ ] Aspect ratio setting works
- [ ] Preview frame implemented
- [ ] Background mode implemented or safely simplified
- [ ] Disabled transition placeholders implemented
- [ ] Disabled music placeholder implemented
- [ ] Slideshow export works
- [ ] Output video exists
- [ ] Output file size > 0
- [ ] Large image safety considered
- [ ] Recent export saved
- [ ] Failure state tested
- [ ] Back behavior tested

---

# 15. EXPORT PROGRESS SCREEN

## 15.1 Purpose

Show export progress for all tools in a consistent way.

## 15.2 Layout

Required structure:

```text
Top section
  - Exporting title
  - Tool-specific subtitle

Progress section
  - Circular or linear progress
  - Percent text if known
  - Step text

Info card
  - Output name
  - Tool type
  - Keep app open warning if needed

Actions
  - Cancel if supported
  - Retry only after failure
```

## 15.3 Step labels

Use clear labels:

- Preparing
- Reading media
- Processing
- Writing file
- Finalizing
- Verifying output
- Complete

## 15.4 Unknown progress

If exact progress is unavailable:

- Show indeterminate progress.
- Show current step.
- Do not fake exact percent.

## 15.5 Duplicate export prevention

User must not be able to start the same export multiple times by tapping repeatedly.

## 15.6 Cancel behavior

If cancel is supported:

- Ask confirmation.
- Stop export safely.
- Clean temp file if possible.

If cancel is not supported:

- Hide cancel or show disabled state with explanation.

## 15.7 Test checklist

- [ ] ExportProgressScreen implemented
- [ ] All tools use same progress UI
- [ ] Known progress shown
- [ ] Unknown progress handled
- [ ] Step text shown
- [ ] Duplicate export prevented
- [ ] Success routes to result
- [ ] Failure state works
- [ ] Retry works if supported
- [ ] Cancel works or is safely disabled
- [ ] Back behavior handled

---

# 16. EXPORT RESULT SCREEN

## 16.1 Purpose

Show final output and give actions.

## 16.2 Layout

Required structure:

```text
Success header
  - Check icon
  - “Export complete”
  - Tool-specific success subtitle

Output preview card
  - Video preview for video output
  - Audio card/player/open action for audio output
  - File name
  - File size
  - Duration if available
  - Saved location label

Actions
  - Open / Play
  - Share
  - Save to gallery if needed
  - Create another
  - Back to Home
```

## 16.3 Verification before success

Do not show success until:

- Output file exists.
- Output file size > 0.
- App can create a valid URI for the file.
- File is added to export history.

## 16.4 Share behavior

Use Android share sheet.

Share must not crash.

If file missing:

- Show error.
- Do not open share sheet.

## 16.5 Test checklist

- [ ] ExportResultScreen implemented
- [ ] Video result preview works
- [ ] Audio result handling works
- [ ] File name shown
- [ ] File size shown
- [ ] Open/play works
- [ ] Share works
- [ ] Create another works
- [ ] Back Home works
- [ ] Missing file handled
- [ ] Result is not shown before verification

---

# 17. RECENT EXPORTS SCREEN

## 17.1 Purpose

Allow user to view and manage exported files.

## 17.2 Layout

Required structure:

```text
Top bar
  - Back
  - Title: Recent Exports

Filter tabs
  - All
  - Videos
  - Audio

Export list
  - Thumbnail/icon
  - File name
  - Tool type
  - File size
  - Created date
  - More menu

Empty state
  - Icon
  - Title
  - Description
  - Create button
```

## 17.3 Export item actions

Each item should support:

- Open/play
- Share
- Delete from history
- Delete actual file only with explicit confirmation if implemented

## 17.4 Missing file behavior

If a file was deleted externally:

- Show “File missing” state.
- Disable open/share.
- Allow removing from history.

## 17.5 Test checklist

- [ ] RecentExportsScreen implemented
- [ ] Empty state works
- [ ] Export appears after success
- [ ] All filter works
- [ ] Video filter works
- [ ] Audio filter works
- [ ] Open works
- [ ] Share works
- [ ] Delete history works
- [ ] Missing file handled
- [ ] Back behavior tested

---

# 18. SETTINGS SCREEN

## 18.1 Purpose

Provide basic app settings and maintenance actions.

## 18.2 Layout

Required sections:

```text
Appearance
  - Dark theme info or theme option

Export
  - Default output folder info
  - Default quality if implemented

Storage
  - Clear temporary files
  - Clear export history

About
  - App version
  - Privacy policy placeholder
  - Terms placeholder
```

## 18.3 Clear temp files

Behavior:

- Clear only temporary files.
- Do not delete user exports.
- Show success message.
- Show error if cleanup fails.

## 18.4 Clear export history

Behavior:

- Ask confirmation.
- Clear history list.
- Do not delete actual files unless clearly stated and separately confirmed.

## 18.5 Test checklist

- [ ] Settings screen implemented
- [ ] Appearance section implemented
- [ ] Export section implemented
- [ ] Storage section implemented
- [ ] About section implemented
- [ ] Clear temp files works
- [ ] Clear history confirmation works
- [ ] Clear history works
- [ ] Back behavior tested
- [ ] No dangerous file deletion

---

# 19. FUTURE TOOLS PLACEHOLDER SCREEN

## 19.1 Purpose

Show planned future features without pretending they work.

## 19.2 Required future tools

Show disabled cards for:

- Filters
- Stickers
- Text Overlay
- Crop
- Rotate
- Speed
- Effects
- GPU Preview
- Timeline Editor

## 19.3 UI

Each disabled future tool card should include:

- Icon
- Tool name
- Short description
- “Coming soon” label

Screen must include:

- Title: “More tools coming soon”
- Description explaining Clipy will expand into advanced editing
- Button: Back to Home

## 19.4 Rules

- Disabled cards must not navigate to broken screens.
- No fake implementation.
- No crash on tapping disabled cards.

## 19.5 Test checklist

- [ ] FutureToolsPlaceholderScreen implemented
- [ ] All future tools listed
- [ ] Disabled state clear
- [ ] Tapping disabled cards does not crash
- [ ] Back to Home works

---

# 20. EXPORT ENGINE UX REQUIREMENTS

Claude may implement export using the current project engine, FFmpeg wrapper, MediaCodec, or existing codebase tools.

The implementation detail is flexible, but UX behavior is not flexible.

## 20.1 Required export job types

Support export jobs for:

- Cut video
- Compress video
- Merge videos
- Extract audio
- Slideshow

## 20.2 Required export states

Support states equivalent to:

- Idle
- Preparing
- Processing
- Finalizing
- Verifying
- Success
- Failed
- Cancelled, if supported

## 20.3 Output naming

Use clean output file names:

```text
Clipy_Cut_yyyyMMdd_HHmmss.mp4
Clipy_Compress_yyyyMMdd_HHmmss.mp4
Clipy_Merge_yyyyMMdd_HHmmss.mp4
Clipy_Audio_yyyyMMdd_HHmmss.mp3
Clipy_Slideshow_yyyyMMdd_HHmmss.mp4
```

Adjust extension based on actual output format.

## 20.4 Output verification

After any export returns success, verify:

- Output file exists.
- Output file size > 0.
- Output URI can be opened/shared.
- Export history saved.

Do not mark export successful before verification.

## 20.5 User-friendly error messages

Use messages like:

- “Could not read the selected file.”
- “This video format is not supported.”
- “The selected file is missing.”
- “Export failed. Please try another file.”
- “Not enough storage space.”
- “No audio track was found.”
- “The output file could not be created.”
- “Something went wrong while processing this media.”

Do not show raw stack traces to the user.

## 20.6 Test checklist

- [ ] Cut export job implemented
- [ ] Compress export job implemented
- [ ] Merge export job implemented
- [ ] Extract audio export job implemented
- [ ] Slideshow export job implemented
- [ ] Progress states implemented
- [ ] Success verification implemented
- [ ] Failure states implemented
- [ ] Output naming implemented
- [ ] Recent export saving implemented
- [ ] Temp cleanup implemented

---

# 21. MEDIA METADATA UX REQUIREMENTS

## 21.1 Metadata to show

For video:

- File name
- Duration
- Size
- Resolution
- Thumbnail
- MIME type if useful

For image:

- File name
- Size
- Resolution
- Thumbnail

For audio output:

- File name
- Size
- Duration if available
- Format

## 21.2 Fallbacks

If metadata is unavailable:

- Show “Unknown duration”
- Show “Unknown size”
- Show “Unknown resolution”
- Show fallback thumbnail/icon

Do not crash.

## 21.3 Large media handling

- Do not decode full video/image into memory for UI thumbnails.
- Use safe thumbnail loading.
- Avoid OOM.
- Show loading state while metadata loads.

## 21.4 Test checklist

- [ ] Video metadata shown
- [ ] Image metadata shown
- [ ] Thumbnail shown
- [ ] Fallback thumbnail works
- [ ] Unknown duration fallback works
- [ ] Unknown size fallback works
- [ ] Large file does not freeze UI
- [ ] Metadata failure does not crash

---

# 22. PERMISSIONS AND FILE ACCESS UX

## 22.1 Permission strategy

Prefer Android Photo Picker / system picker where possible.

Do not request broad storage permission unless absolutely required.

## 22.2 Denied permission UI

If permission is needed and denied, show:

- Icon
- Title: “Permission needed”
- Description explaining why
- Button: “Allow access”
- Button: “Open settings” if permanently denied

## 22.3 Cancellation behavior

If user cancels picker:

- Do not crash.
- Stay on current screen.
- Keep previous selections.
- Show optional toast/snackbar.

## 22.4 Test checklist

- [ ] Picker works without unnecessary permission
- [ ] Permission denied handled
- [ ] Permanently denied handled
- [ ] Picker cancellation handled
- [ ] Android 13+ behavior checked
- [ ] Older Android behavior considered if supported

---

# 23. GLOBAL EMPTY / ERROR / LOADING STATES

## 23.1 Empty states

Every list or selection area must have a polished empty state.

Examples:

Media picker empty:

- “No media selected”
- “Choose a video to continue.”

Recent exports empty:

- “No exports yet”
- “Your finished files will appear here.”

## 23.2 Loading states

Use loading states for:

- Splash loading
- Media metadata loading
- Thumbnail loading
- Export preparing
- Export processing
- Export verification

## 23.3 Error states

Error UI must include:

- Clear title
- Friendly message
- Retry action if possible
- Back/home action if needed

## 23.4 Test checklist

- [ ] Empty states implemented
- [ ] Loading states implemented
- [ ] Error states implemented
- [ ] Retry actions implemented where useful
- [ ] User-friendly error messages used
- [ ] No raw stack traces shown

---

# 24. STRICT UI QUALITY CHECKLIST

The app must not have:

- [ ] Overlapping text
- [ ] Clipped buttons
- [ ] Clipped long file names
- [ ] Huge random blank spaces
- [ ] Bottom buttons hidden by navigation bar
- [ ] Status bar overlap
- [ ] Hard-to-read contrast
- [ ] Broken disabled buttons
- [ ] Fake buttons that do nothing
- [ ] Export success without real output verification
- [ ] Crash on picker cancel
- [ ] Crash on invalid media
- [ ] Crash on missing metadata
- [ ] Crash on back during loading
- [ ] Crash on back during export

Before final completion, verify all these are false.

---

# 25. DETAILED MANUAL TEST PLAN

Claude must reason through and execute as many tests as possible.  
If automated UI tests are not available, perform build checks and code-level verification, then clearly report what was manually verified by reasoning and what requires device testing.

## 25.1 Home tests

- [ ] Open app
- [ ] Splash appears
- [ ] Onboarding or Home appears correctly
- [ ] Home tool cards visible
- [ ] Settings opens
- [ ] Recent exports opens
- [ ] Every tool card routes to correct picker mode
- [ ] No clipping on small screen

## 25.2 Picker tests

- [ ] Open Cut picker
- [ ] Cancel picker
- [ ] Select one video
- [ ] Remove selected video
- [ ] Continue disabled when no video
- [ ] Continue enabled when one video selected
- [ ] Open Merge picker
- [ ] Select one video only, continue disabled
- [ ] Select two videos, continue enabled
- [ ] Open Slideshow picker
- [ ] Select one image only, continue disabled
- [ ] Select two images, continue enabled

## 25.3 Cut tests

- [ ] Preview loads
- [ ] Play/pause works
- [ ] Start handle changes
- [ ] End handle changes
- [ ] Invalid range blocked
- [ ] Reset works
- [ ] Export works
- [ ] Output file exists
- [ ] Result screen opens

## 25.4 Compress tests

- [ ] Original file info shown
- [ ] Balanced preset selected
- [ ] Small file preset selectable
- [ ] High quality preset selectable
- [ ] Export works
- [ ] Output file exists
- [ ] Output file size > 0
- [ ] Result screen opens

## 25.5 Merge tests

- [ ] Two videos selected
- [ ] Order visible
- [ ] Remove clip works
- [ ] Add more works
- [ ] Less than two clips blocked
- [ ] Export works
- [ ] Output file exists
- [ ] Result screen opens

## 25.6 Extract audio tests

- [ ] Video selected
- [ ] Output format visible
- [ ] Quality visible
- [ ] Export works
- [ ] Output audio exists
- [ ] Open/share works
- [ ] No-audio video handled if possible

## 25.7 Slideshow tests

- [ ] Two images selected
- [ ] Image order visible
- [ ] Remove image works
- [ ] Duration setting works
- [ ] Aspect ratio setting works
- [ ] Export works
- [ ] Output video exists
- [ ] Large image safety considered

## 25.8 Export tests

- [ ] Progress screen shown
- [ ] Duplicate export prevented
- [ ] Success result shown only after output verification
- [ ] Failure state works
- [ ] Retry works if possible
- [ ] Recent export saved

## 25.9 Recent exports tests

- [ ] Export appears in list
- [ ] Open works
- [ ] Share works
- [ ] Delete history works
- [ ] Missing file handled
- [ ] Empty state works

## 25.10 Settings tests

- [ ] Settings opens
- [ ] Clear temp works safely
- [ ] Clear history asks confirmation
- [ ] Clear history works
- [ ] Back works

---

# 26. FINAL ACCEPTANCE CHECKLIST

Do not output COMPLETE until all are checked.

## 26.1 Foundation

- [ ] App builds with `./gradlew assembleDebug`
- [ ] Package name remains `com.nantcompany.clipy`
- [ ] Dark theme applied
- [ ] Navigation implemented
- [ ] Back navigation works
- [ ] Safe area handled
- [ ] No signing/secret files modified

## 26.2 Screens

- [ ] SplashScreen complete
- [ ] OnboardingScreen complete
- [ ] HomeScreen complete
- [ ] MediaPickerScreen complete
- [ ] MediaPreviewScreen complete
- [ ] CutTrimScreen complete
- [ ] CompressScreen complete
- [ ] MergeScreen complete
- [ ] ExtractAudioScreen complete
- [ ] SlideshowScreen complete
- [ ] ExportProgressScreen complete
- [ ] ExportResultScreen complete
- [ ] RecentExportsScreen complete
- [ ] SettingsScreen complete
- [ ] FutureToolsPlaceholderScreen complete

## 26.3 Core features

- [ ] Cut video works end-to-end
- [ ] Compress video works end-to-end
- [ ] Merge videos works end-to-end
- [ ] Extract audio works end-to-end
- [ ] Slideshow works end-to-end

## 26.4 Export

- [ ] Export progress shown
- [ ] Export success handled
- [ ] Export failure handled
- [ ] Output file existence verified
- [ ] Output file size verified
- [ ] Recent export saved
- [ ] Share/open actions work

## 26.5 UX

- [ ] No text clipping
- [ ] No overlapping UI
- [ ] No broken buttons
- [ ] Loading states implemented
- [ ] Error states implemented
- [ ] Empty states implemented
- [ ] Disabled states clear
- [ ] Small screen layout checked
- [ ] Android navigation bar padding checked

## 26.6 Testing

- [ ] Picker cancel tested
- [ ] Invalid media tested
- [ ] Missing metadata tested
- [ ] Back during loading tested
- [ ] Back during export tested
- [ ] Export failure tested
- [ ] Large media handling considered/tested
- [ ] `./gradlew assembleDebug` passes
- [ ] `./gradlew test` run if available
- [ ] `./gradlew lintDebug` run if available

---

# 27. RALPH LOOP COMMAND

Use this command in Claude Code after placing this file in the project root as `TODO.md`:

```text
/ralph-loop "Read CLAUDE.md and TODO.md. Build Clipy exactly according to TODO.md. You may generate models, state classes, ViewModels, repositories, use cases, and UI components as needed based on the current codebase. Work through every unchecked item. Do not stop after one screen or one feature. Test each feature seriously before marking it done. Run ./gradlew assembleDebug after meaningful changes. If build fails, fix it and rerun. If any export/API/tool/library returns success, verify the real output/result before marking done. Do not modify package name, signing config, keystore files, .env, google-services.json, or production credentials. Only output COMPLETE when all TODO.md checklist items are done, all core features are verified, UI has no clipping/overlap, output files are verified, and assembleDebug passes." --max-iterations 100 --completion-promise "All TODO.md checklist items are checked, Cut/Compress/Merge/Extract Audio/Slideshow work end-to-end, export output files are verified, UI has no clipping or overlap, ./gradlew assembleDebug passes, and Claude outputs COMPLETE"
```

---

# 28. FINAL RESPONSE FORMAT FOR CLAUDE

When everything is finished, Claude must respond exactly with this structure:

```text
COMPLETE

Implemented:
- ...

Tested:
- ...

Build:
- ./gradlew assembleDebug passed

Changed files:
- ...

Known limitations:
- ...
```

If anything is not finished, Claude must respond:

```text
NOT COMPLETE

Done:
- ...

Remaining:
- ...

Blocked by:
- ...

Next steps:
- ...
```

Do not output COMPLETE if any core feature, UI requirement, export verification, or build check is incomplete.
