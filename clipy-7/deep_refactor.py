import os
import re

base_path = 'app/src/main/java/com/example/clipystudio/ui/main'
screens_path = os.path.join(base_path, 'screens')
editor_path = os.path.join(base_path, 'editor')
panels_path = os.path.join(editor_path, 'panels')
timeline_path = os.path.join(editor_path, 'timeline')
components_path = os.path.join(editor_path, 'components')
models_path = os.path.join(base_path, 'models')

for p in [screens_path, editor_path, panels_path, timeline_path, components_path, models_path]:
    if not os.path.exists(p):
        os.makedirs(p)

def get_header(package_suffix=""):
    package = "com.example.clipystudio.ui.main"
    if package_suffix:
        package += "." + package_suffix
    
    # Standard imports for most files
    imports = """
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clipystudio.data.*
import com.example.clipystudio.filter.*
import com.example.clipystudio.theme.*
import com.example.clipystudio.ui.main.*
import com.example.clipystudio.ui.main.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.*
"""
    return f"package {package}\n{imports}\n"

# I'll read everything from the 5 files I created earlier
source_files = [
    'MainModels.kt',
    'AppScreens.kt',
    'EditorPanels.kt',
    'EditorTimeline.kt',
    'MainScreen.kt'
]

all_decls = []

for filename in source_files:
    path = os.path.join(base_path, filename)
    if not os.path.exists(path): continue
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
        
    # Split by @Composable or top-level declarations
    # This is tricky because of nested blocks. 
    # I'll use a simpler approach: chunking by leading non-indented lines.
    lines = content.splitlines()
    current_decl = []
    current_name = ""
    in_header = True
    for line in lines:
        if line.startswith('package ') or line.startswith('import '):
            continue
        
        pattern = r'^(@Composable|internal enum|internal data|internal val|internal fun|internal class|fun|val|enum|class|data class)\b'
        if re.match(pattern, line):
            if current_decl:
                all_decls.append((current_name, "\n".join(current_decl)))
            current_decl = [line]
            match = re.search(r'(?:class|fun|val) ([A-Za-z0-9_]+)', line)
            current_name = match.group(1) if match else "DECORATOR"
        else:
            if current_decl:
                current_decl.append(line)
    if current_decl:
        all_decls.append((current_name, "\n".join(current_decl)))

# Define distribution
mapping = {
    'models/Screen.kt': ['Screen'],
    'models/Copy.kt': ['Copy', 'copyFor', 'onboardingPages'],
    'models/MainModels.kt': ['TimelineClipPreviewState', 'TimelineGestureOverlayState', 'PreviewGestureFeedback', 'PreviewSurfaceState', 'ClipVisualState', 'VideoPreviewLoadState', 'EditorChromeBackground', 'EditorChromeSurface', 'EditorChromeSurfaceAlt', 'EditorChromeSurfaceLow', 'EditorChromeBorder', 'EditorChromePrimary', 'EditorChromeAudio', 'EditorChromeAudioAccent', 'EditorChromeMuted', 'EditorTimelineGrid', 'EditorChromeDanger', 'BottomNavItem', 'topBarChevronGlyph', 'toolbarGlyph', 'navGlyph', 'clipTypeBadge', 'ImportPermissionNotice', 'IntroPage', 'ThumbnailFrame', 'UriMetadata', 'stageColor'],
    
    'screens/IntroScreen.kt': ['IntroScreen', 'OnboardingIllustration', 'Dot'],
    'screens/DashboardScreen.kt': ['DashboardScreen', 'ProjectCard', 'EmptyState'],
    'screens/LanguageScreen.kt': ['LanguageScreen', 'LanguageCard'],
    'screens/ImportScreen.kt': ['ImportScreen', 'MediaAssetCard'],
    'screens/ExportScreen.kt': ['ExportScreen', 'RenderPipelineSummary', 'StatusPill', 'ExportProgressPanel', 'ExportSuccessPanel', 'ExportOptionCard'],
    'screens/SettingsScreen.kt': ['SettingsScreen', 'SettingsRow'],
    
    'editor/EditorScreen.kt': ['EditorScreen'],
    'editor/components/EditorTopBar.kt': ['EditorTopBar'],
    'editor/components/EditorPreviewSection.kt': ['EditorPreviewSection', 'PreviewCanvas', 'PreviewLayerChip', 'PreviewMediaSurface', 'ImagePreviewSurface', 'FilteredVideoFramePreview', 'VideoPreviewPlayer', 'PreviewStatusCard', 'PlaybackControls'],
    'editor/components/EditorTimelineSection.kt': ['EditorTimelineSection', 'EditorTimelineToolbar', 'CompactToolbarIconButton', 'CompactToolbarAction'],
    'editor/components/EditorBottomBar.kt': ['EditorBottomBar', 'AddMediaFab'],
    'editor/components/EditorPanelHost.kt': ['EditorPanelHost'],
    
    'editor/panels/AudioToolPanel.kt': ['AudioToolPanel'],
    'editor/panels/TextToolPanel.kt': ['TextToolPanel'],
    'editor/panels/StickerToolPanel.kt': ['StickerToolPanel', 'StickerTile'],
    'editor/panels/FilterToolPanel.kt': ['FilterAdjustPanel', 'FilterPreviewChip'],
    'editor/panels/EffectToolPanel.kt': ['EffectToolPanel', 'EffectTile'],
    'editor/panels/TransitionToolPanel.kt': ['TransitionToolPanel', 'ClipTransitionPanel'],
    'editor/panels/CanvasToolPanel.kt': ['CanvasToolPanel'],
    'editor/panels/SpeedToolPanel.kt': ['SpeedToolPanel'],
    'editor/panels/OverlayToolPanel.kt': ['OverlayToolPanel', 'MediaMiniCard'],
    'editor/panels/CommonPanels.kt': ['AdjustmentControl', 'LayerActions', 'ToolPanel', 'ToolRail', 'ClipEditPanel'],
    
    'editor/timeline/TimelineView.kt': ['TimelineView', 'TimelineHeader', 'AutoScrollEdgeMask', 'EngineTrackLane', 'EngineClipBlock', 'TrimHandleGrip', 'EdgeResistanceMask', 'TimelineGuides'],
}

file_contents = {k: [] for k in mapping.keys()}

for name, content in all_decls:
    assigned = False
    for target_file, names in mapping.items():
        if name in names:
            file_contents[target_file].append(content)
            assigned = True
            break
    if not assigned:
        # Fallback for extension functions or things not explicitly mapped
        if 'Timeline.' in content: file_contents['models/MainModels.kt'].append(content)
        elif 'Context.' in content: file_contents['models/MainModels.kt'].append(content)
        elif 'TimelineClip.' in content: file_contents['models/MainModels.kt'].append(content)
        elif 'ClipyStudioApp' in content: pass # Header will keep it? No, I'll put it in MainScreen.kt
        elif 'MainScreen' in content: pass

# Write files
for rel_path, chunks in file_contents.items():
    if not chunks: continue
    full_path = os.path.join(base_path, rel_path.replace('/', os.sep))
    package_suffix = rel_path.rsplit('/', 1)[0].replace('/', '.') if '/' in rel_path else ""
    with open(full_path, 'w', encoding='utf-8') as f:
        f.write(get_header(package_suffix))
        f.write("\n".join(chunks))

# Finally rewrite MainScreen.kt and ClipyStudioApp
main_screen_content = ""
for name, content in all_decls:
    if name in ['MainScreen', 'ClipyStudioApp', 'toShareUri']:
        main_screen_content += content + "\n\n"

with open(os.path.join(base_path, 'MainScreen.kt'), 'w', encoding='utf-8') as f:
    f.write(get_header())
    # Add manual imports for navigation if needed, but the header has it.
    f.write(main_screen_content)

# Delete the temporary split files from previous step
for filename in source_files:
    if filename != 'MainScreen.kt':
        p = os.path.join(base_path, filename)
        if os.path.exists(p): os.remove(p)

print("Deep refactoring completed.")
