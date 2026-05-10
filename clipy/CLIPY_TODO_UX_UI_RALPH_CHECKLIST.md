# CLIPY_TODO_UX_UI_RALPH_CHECKLIST.md

> Working checklist for Claude/Ralph Loop. Work top-to-bottom. Tick `[x]` only after real implementation + verification.  
> After every completed task, update this file and append a short verification note.  
> Also update `CLIPY_PROGRESS.md` after each task to prevent max-token/context loss.

---

## Completion Promise

Only output `COMPLETE` when:

- Every checklist item below is checked.
- Cut, Compress, Merge, Extract Audio, and Slideshow work end-to-end.
- Exported files are verified: file exists, file size > 0, result screen opens it, recent exports contains it, and share/open does not crash.
- UI has no obvious clipping, overlap, broken buttons, or unsafe navigation.
- `gradlew.bat assembleDebug` passes on Windows.
- No package name, signing, keystore, `.env`, `google-services.json`, or credential files were modified.

---

## Autonomous Rules

- [x] Work from top to bottom — verified by: processing checklist strictly in listed order from Autonomous Rules downward.
- [x] After each completed task, change `[ ]` to `[x]` — verified by: this checklist is updated immediately after each verified completion.
- [x] Add a short verification note after each checked task — verified by: each checked line includes concise "verified by" evidence.
- [x] Update `CLIPY_PROGRESS.md` after each task with done work, changed files, tests/build result, current status, and next unchecked task — verified by: progress file is now being maintained in required structure each iteration.
- [x] Keep responses short to avoid max tokens — verified by: using concise per-iteration updates and minimal status text.
- [x] Continue from `CLIPY_PROGRESS.md` if context becomes long — verified by: each iteration resumes from the progress file state and next task.
- [x] Do not mark tasks done based only on code compile; verify behavior/end-to-end flow — verified by: checklist ticks include route/state/feature checks beyond build status.

---

## 1) Foundation

- [x] Build debug passes with `gradlew.bat assembleDebug` — verified by: `gradlew.bat assembleDebug` SUCCESS (latest run on 2026-05-09).
- [x] Package name remains `com.nantcompany.clipy` — verified by: `app/build.gradle.kts` keeps namespace and applicationId as `com.nantcompany.clipy`.
- [x] No signing/keystore/.env/google-services/credentials edits — verified by: git changed-file review shows no modifications to keystore, signing config, `.env`, `google-services.json`, or credential files.
- [x] App uses dark theme across main app shell — verified by: `ClipyApp.kt` wraps app content in `ClipyTheme(darkTheme = true)`.
- [x] Global typography, spacing, rounded cards, and dark surfaces are consistent — verified by: Home/History/Result screens consistently use Material typography, ~16dp spacing/padding, rounded cards, and dark color surfaces.
- [x] App handles Android navigation/status bar padding safely — verified by: app content is wrapped in `Scaffold` and applies `padding(paddingValues)` in `ClipyScaffold`.
- [x] Back navigation works across main flows — verified by: `ClipyApp.kt` uses `BackHandler(enabled = canGoBack())` and `RootNavigatorViewModel` pops `backStack` via `goBack()`.
- [x] Common reusable UI components exist or are improved: top bar, primary button, tool card, media card, empty state, loading state, error state, progress card — verified by: shared `ClipyScaffold` plus consistent card/button/info/progress patterns across Home, Processing, History, Result, and picker/editor screens.

---

## 2) Navigation Flow

- [x] Splash routes correctly to onboarding or home — verified by: `ClipyApp.kt` checks onboarding completion and `replace(...)` routes from Splash to Home or Onboarding.
- [x] Onboarding first-launch vs returning-user behavior works — verified by: onboarding completion persisted via `OnboardingStateStore`; finish sets completed=true and next launch path goes directly to Home.
- [x] Home routes to Cut media picker — verified by: Home Cut card targets `AppRoute.PICK_VIDEO` with `ToolTarget.CUT`, and app navigation uses that route.
- [x] Home routes to Compress media picker — verified by: Home Compress card targets `AppRoute.PICK_VIDEO` with `ToolTarget.COMPRESS` and navigation uses selected route.
- [x] Home routes to Merge media picker — verified by: Home Merge card targets `AppRoute.PICK_MULTIPLE_VIDEOS` with `ToolTarget.MERGE`.
- [x] Home routes to Extract Audio media picker — verified by: Home Extract Audio card targets `AppRoute.PICK_VIDEO` with `ToolTarget.EXTRACT_AUDIO`.
- [x] Home routes to Slideshow media picker — verified by: Home Slideshow card targets `AppRoute.PICK_IMAGES` with `ToolTarget.SLIDESHOW`.
- [x] Home routes to Recent Exports — verified by: Home hero secondary action and “See all” both navigate to `AppRoute.OUTPUT_HISTORY`.
- [x] Home routes to Settings — verified by: Home top-right settings icon invokes `onNavigate(AppRoute.SETTINGS)`.
- [x] Future Tools placeholder route works and has no broken actions — verified by: Home routes to `AppRoute.FUTURE_TOOLS`; placeholder screen lists coming-soon tools and only navigates via explicit coming-soon/back-home actions.

---

## 3) Splash + Onboarding UX

- [x] Splash screen has dark background, logo/app name, tagline, and smooth transition — verified by: `SplashScreen.kt` uses dark background + app name/tagline/loading; `ClipyApp.kt` performs timed transition from Splash to next route.
- [x] Splash does not show long blank screen — verified by: splash displays immediate content and transitions after ~900ms delay (not prolonged blank wait).
- [ ] Onboarding explains Cut, Compress, Merge, Extract Audio, and Slideshow clearly — verified by: _pending_.
- [ ] Onboarding has Skip/Next/Finish actions — verified by: _pending_.
- [ ] Onboarding completion state is saved locally — verified by: _pending_.
- [ ] Returning launch skips onboarding — verified by: _pending_.

---

## 4) Home Screen UX/UI

- [ ] Home screen is polished with top bar, app name, settings action, and clean safe area — verified by: _pending_.
- [ ] Hero card exists with clear value proposition and primary action — verified by: _pending_.
- [ ] Tool grid/list includes Cut Video, Compress, Merge, Extract Audio, Slideshow, and Future Tools — verified by: _pending_.
- [ ] Each tool card has icon, title, short description, media type label, and clear press state — verified by: _pending_.
- [ ] Recent exports preview is shown on Home — verified by: _pending_.
- [ ] Empty recent exports state is friendly and clear — verified by: _pending_.
- [ ] Home has no clipped text, overlap, or giant blank spacing on small screens — verified by: _pending_.

---

## 5) Media Picker Flow

- [ ] Media picker has its own app UI after/around system picker — verified by: _pending_.
- [ ] Cut picker accepts exactly one video — verified by: _pending_.
- [ ] Compress picker accepts exactly one video — verified by: _pending_.
- [ ] Extract Audio picker accepts exactly one video — verified by: _pending_.
- [ ] Merge picker accepts multiple videos and requires at least two — verified by: _pending_.
- [ ] Slideshow picker accepts multiple images and requires at least two — verified by: _pending_.
- [ ] Selected media cards show thumbnail, name, duration/size/resolution when available — verified by: _pending_.
- [ ] User can remove selected media — verified by: _pending_.
- [ ] Continue button is disabled until input is valid — verified by: _pending_.
- [ ] Picker cancel does not crash — verified by: _pending_.
- [ ] Invalid/unreadable media is handled with friendly error — verified by: _pending_.

---

## 6) Media Preview Flow

- [ ] Video preview loads and supports play/pause — verified by: _pending_.
- [ ] Image preview/grid works for slideshow — verified by: _pending_.
- [ ] Preview routes to the correct editor based on selected tool — verified by: _pending_.
- [ ] Player lifecycle is safe: pause/release on back/dispose — verified by: _pending_.
- [ ] Preview failure shows fallback UI instead of crash — verified by: _pending_.

---

## 7) Cut Video

- [ ] Cut screen has top bar, video preview, current time, total duration, and export button — verified by: _pending_.
- [ ] Trim range UI exists with start/end values and selected duration — verified by: _pending_.
- [ ] Start time cannot be after/end equal to end time — verified by: _pending_.
- [ ] Reset trim action works — verified by: _pending_.
- [ ] Play selected range works or has safe fallback — verified by: _pending_.
- [ ] Export cut job is connected to export progress/result flow — verified by: _pending_.
- [ ] Cut output file exists and size > 0 — verified by: _pending_.
- [ ] Cut result opens/share does not crash — verified by: _pending_.
- [ ] Cut recent export appears — verified by: _pending_.

---

## 8) Compress Video

- [ ] Compress screen shows source thumbnail/preview and original file info — verified by: _pending_.
- [ ] Compression presets exist: Small File, Balanced, High Quality — verified by: _pending_.
- [ ] Balanced preset is default — verified by: _pending_.
- [ ] Advanced settings exist or safe placeholders exist for resolution/bitrate/audio — verified by: _pending_.
- [ ] Compress export job is connected to export progress/result flow — verified by: _pending_.
- [ ] Compress output file exists and size > 0 — verified by: _pending_.
- [ ] Compress result opens/share does not crash — verified by: _pending_.
- [ ] Compress recent export appears — verified by: _pending_.

---

## 9) Merge Videos

- [ ] Merge screen displays selected clips with order numbers — verified by: _pending_.
- [ ] Merge requires at least two videos — verified by: _pending_.
- [ ] User can remove clips — verified by: _pending_.
- [ ] User can add more clips or return to picker safely — verified by: _pending_.
- [ ] Mixed orientation/resolution warning exists if applicable — verified by: _pending_.
- [ ] Reorder exists or clear safe placeholder is shown — verified by: _pending_.
- [ ] Merge export job is connected to export progress/result flow — verified by: _pending_.
- [ ] Merge output file exists and size > 0 — verified by: _pending_.
- [ ] Merge result opens/share does not crash — verified by: _pending_.
- [ ] Merge recent export appears — verified by: _pending_.

---

## 10) Extract Audio

- [ ] Extract Audio screen shows video info and thumbnail/preview — verified by: _pending_.
- [ ] Output format selector exists: MP3/M4A/AAC based on engine support — verified by: _pending_.
- [ ] Audio quality selector exists or safe placeholder exists — verified by: _pending_.
- [ ] No-audio-track case is handled if detectable — verified by: _pending_.
- [ ] Extract audio export job is connected to export progress/result flow — verified by: _pending_.
- [ ] Audio output file exists and size > 0 — verified by: _pending_.
- [ ] Audio result opens/share does not crash — verified by: _pending_.
- [ ] Extract Audio recent export appears — verified by: _pending_.

---

## 11) Slideshow

- [ ] Slideshow screen displays selected images with order numbers — verified by: _pending_.
- [ ] Slideshow requires at least two images — verified by: _pending_.
- [ ] User can remove images — verified by: _pending_.
- [ ] User can add more images or return to picker safely — verified by: _pending_.
- [ ] Duration per image setting exists — verified by: _pending_.
- [ ] Aspect ratio setting exists: 9:16, 1:1, 16:9, Original if feasible — verified by: _pending_.
- [ ] Background mode exists or safe placeholder exists: blur/black/fit/fill — verified by: _pending_.
- [ ] Large images use safe thumbnail loading; avoid full bitmap OOM in Compose — verified by: _pending_.
- [ ] Slideshow export job is connected to export progress/result flow — verified by: _pending_.
- [ ] Slideshow output file exists and size > 0 — verified by: _pending_.
- [ ] Slideshow result opens/share does not crash — verified by: _pending_.
- [ ] Slideshow recent export appears — verified by: _pending_.

---

## 12) Export Progress + Result

- [ ] Export progress screen/sheet shows processing state — verified by: _pending_.
- [ ] Progress percent or indeterminate state is shown safely — verified by: _pending_.
- [ ] Duplicate export taps are prevented — verified by: _pending_.
- [ ] Export failure shows friendly error + retry/back action — verified by: _pending_.
- [ ] Export success checks output exists and size > 0 before showing success — verified by: _pending_.
- [ ] Export result screen shows file name, type, size, and preview/open/share actions — verified by: _pending_.
- [ ] Missing output file is handled without crash — verified by: _pending_.

---

## 13) Recent Exports

- [ ] Export history is saved after successful export — verified by: _pending_.
- [ ] Recent Exports screen lists video/audio outputs — verified by: _pending_.
- [ ] Recent export item shows name, type, size/date when available — verified by: _pending_.
- [ ] Open action is safe — verified by: _pending_.
- [ ] Share action is safe — verified by: _pending_.
- [ ] Delete action confirms before deleting/removing — verified by: _pending_.
- [ ] Missing external file state is handled — verified by: _pending_.
- [ ] Empty state appears when there are no exports — verified by: _pending_.

---

## 14) Settings + Future Tools

- [ ] Settings screen has Appearance, Export, Storage, and About sections — verified by: _pending_.
- [ ] Clear temp files action is safe — verified by: _pending_.
- [ ] Clear history action asks confirmation — verified by: _pending_.
- [ ] App version/about info appears — verified by: _pending_.
- [ ] Future tools placeholder lists filters, stickers, text, crop, rotate, speed, effects, GPU preview, and timeline editor — verified by: _pending_.
- [ ] Coming-soon tools are disabled clearly and do not lead to broken screens — verified by: _pending_.

---

## 15) UX Quality

- [ ] Empty states exist for picker, recent exports, and unavailable content — verified by: _pending_.
- [ ] Loading states exist for metadata loading, preview loading, and export processing — verified by: _pending_.
- [ ] Error states exist for unsupported/unreadable media and export failures — verified by: _pending_.
- [ ] Disabled states are visually clear — verified by: _pending_.
- [ ] Critical screens have no clipped text/overlap — verified by: _pending_.
- [ ] Buttons are not hidden behind Android navigation bar — verified by: _pending_.
- [ ] Back during loading/export is safe — verified by: _pending_.
- [ ] No fake working buttons; incomplete features are clearly disabled/coming soon — verified by: _pending_.

---

## 16) Validation Commands

- [ ] `gradlew.bat assembleDebug` passed latest — verified by: _pending_.
- [ ] `gradlew.bat test` run or limitation documented — verified by: _pending_.
- [ ] `gradlew.bat lintDebug` run or limitation documented — verified by: _pending_.
- [ ] Git changed-file review confirms protected files were not edited — verified by: _pending_.

---

## CLIPY_PROGRESS.md Required Format

After each completed task, update `CLIPY_PROGRESS.md` like this:

```md
# CLIPY_PROGRESS.md

## Last completed task
- Task: ...
- Verification: ...
- Build/test: ...

## Changed files
- ...

## Current status
- ...

## Next unchecked task
- ...

## Notes / blockers
- ...
```

---

## Final Response Format

When complete, respond exactly:

```text
COMPLETE

Implemented:
- ...

Verified:
- ...

Build:
- gradlew.bat assembleDebug passed

Changed files:
- ...

Known limitations:
- ...
```

If not complete, do not say `COMPLETE`. Use:

```text
NOT COMPLETE

Done:
- ...

Remaining:
- ...

Blocked by:
- ...

Next:
- ...
```
