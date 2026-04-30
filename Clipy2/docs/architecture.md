# Architecture

## Module Boundaries

`app` is the composition root for the user experience. It owns `MainActivity`, `ClipyApp`, app navigation state, the Android photo picker launcher, home/export UI, and bridges from app state into editor sessions.

`video-editor` is the editor-facing feature module. It contains editor models, editor UI, the editor view model, realtime GPU preview view code, timeline components, and the orchestration layer that turns a `VideoEditorSession` into preview/export plans.

`media` is the media engine module. It contains import, metadata reading, timeline editing, preview control, transcode/render implementation, audio processing, cache management, diagnostics, and session management.

`gpuimage` is the local OpenGL/GPUImage implementation mapped to the `gpuimage-core` directory.

`core` is a lightweight shared module for primitives that should not depend on UI or media engine details.

`editor` currently contains older editor/filter code. Treat it as legacy unless the work explicitly targets it.

## State And Data Flow

1. The user enters through `MainActivity` and `ClipyApp`.
2. `ClipyApp` routes between home, editor, and export based on `ClipyAppState.currentScreen`.
3. Home launches Android Photo Picker or reopens the editor depending on the selected feature and existing clips.
4. `ClipyAppState` stores UI-facing project state.
5. `VideoEditorBridge` maps `ClipyAppState` into `VideoEditorSession`.
6. `DefaultVideoEditorOrchestrator` filters invalid clips, determines whether OpenGL is needed, and builds preview/export plans.
7. Media implementation details remain behind `:media` contracts and `DefaultMediaModuleFactory`.

## Ownership Rules

- Add user-facing Compose screens and app navigation in `:app`.
- Add editor timeline/session/preview/export contracts in `:video-editor`.
- Add media implementation and tests in `:media`.
- Add reusable non-UI primitives in `:core`.
- Avoid coupling screens directly to media implementations; route through state, bridges, contracts, or factories.
- Avoid adding new production code to `:editor` unless the module is intentionally brought back into the active architecture.
