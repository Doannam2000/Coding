import os
import re

source_file = 'app/src/main/java/com/example/clipystudio/ui/main/MainScreen.kt'
with open(source_file, 'r', encoding='utf-8') as f:
    full_content = f.read()

# Separate header (package + imports)
lines = full_content.splitlines()
header_lines = []
content_lines = []
in_header = True
for line in lines:
    if in_header:
        if line.strip().startswith('@') or line.strip().startswith('fun ') or line.strip().startswith('class ') or line.strip().startswith('val ') or line.strip().startswith('enum ') or line.strip().startswith('private ') or line.strip().startswith('internal '):
            in_header = False
            content_lines.append(line)
        else:
            header_lines.append(line)
    else:
        content_lines.append(line)

header = "\n".join(header_lines) + "\n"

# Add navigation imports to header if missing
if 'androidx.navigation3' not in header:
    header += "import androidx.navigation3.runtime.NavKey\n"

# Robust splitting: Chunks start with a non-indented line (except annotations)
chunks = []
current_chunk = []
current_name = "UNKNOWN"

def get_name(line):
    match = re.search(r'(?:class|fun|val) ([A-Za-z0-9_]+)', line)
    return match.group(1) if match else None

for line in content_lines:
    # A new chunk starts if we see a non-indented declaration or annotation
    if line.strip() and not line.startswith(' ') and not line.startswith('\t'):
        # If it's an annotation, it probably belonging to the next declaration
        # and we should check if the current chunk is empty or not.
        if line.startswith('@'):
            if current_chunk and not current_chunk[-1].startswith('@'):
                chunks.append((current_name, "\n".join(current_chunk)))
                current_chunk = []
                current_name = "DECORATOR"
        elif any(line.startswith(x) for x in ['fun ', 'class ', 'val ', 'enum ', 'private ', 'internal ', 'data ']):
             if current_chunk and not current_chunk[-1].startswith('@'):
                chunks.append((current_name, "\n".join(current_chunk)))
                current_chunk = []
             
             name = get_name(line)
             if name: current_name = name
    
    current_chunk.append(line)

if current_chunk:
    chunks.append((current_name, "\n".join(current_chunk)))

# Distribution Mapping
mapping = {
    'models/Screen.kt': ['Screen'],
    'models/Copy.kt': ['Copy', 'copyFor', 'onboardingPages'],
    'models/MainModels.kt': ['TimelineClipPreviewState', 'TimelineGestureOverlayState', 'PreviewGestureFeedback', 'PreviewSurfaceState', 'ClipVisualState', 'VideoPreviewLoadState', 'EditorChromeBackground', 'EditorChromeSurface', 'EditorChromeSurfaceAlt', 'EditorChromeSurfaceLow', 'EditorChromeBorder', 'EditorChromePrimary', 'EditorChromeAudio', 'EditorChromeAudioAccent', 'EditorChromeMuted', 'EditorTimelineGrid', 'EditorChromeDanger', 'BottomNavItem', 'topBarChevronGlyph', 'toolbarGlyph', 'navGlyph', 'clipTypeBadge', 'ImportPermissionNotice', 'IntroPage', 'ThumbnailFrame', 'UriMetadata', 'stageColor', 'Timeline', 'TimelineClip'],
    
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
    'editor/panels/StickerToolPanel.kt': ['StickerToolPanel', 'StickerTile', 'StickerLibrary'],
    'editor/panels/FilterToolPanel.kt': ['FilterAdjustPanel', 'FilterPreviewChip'],
    'editor/panels/EffectToolPanel.kt': ['EffectToolPanel', 'EffectTile'],
    'editor/panels/TransitionToolPanel.kt': ['TransitionToolPanel', 'ClipTransitionPanel'],
    'editor/panels/CanvasToolPanel.kt': ['CanvasToolPanel'],
    'editor/panels/SpeedToolPanel.kt': ['SpeedToolPanel'],
    'editor/panels/OverlayToolPanel.kt': ['OverlayToolPanel', 'MediaMiniCard'],
    'editor/panels/CommonPanels.kt': ['AdjustmentControl', 'LayerActions', 'ToolPanel', 'ToolRail', 'ClipEditPanel'],
    
    'editor/timeline/TimelineView.kt': ['TimelineView'],
    'editor/timeline/TimelineHeader.kt': ['TimelineHeader'],
    'editor/timeline/EngineTrackLane.kt': ['EngineTrackLane'],
    'editor/timeline/EngineClipBlock.kt': ['EngineClipBlock', 'TrimHandleGrip'],
    'editor/timeline/TimelineSubComponents.kt': ['AutoScrollEdgeMask', 'EdgeResistanceMask', 'TimelineGuides'],
}

# Functions to always keep in MainScreen.kt
keep_in_main = ['MainScreen', 'ClipyStudioApp', 'toShareUri']

# Elements needed by multiple screens (put in a shared file or models)
shared_elements = ['StudioScreen', 'TopStrip', 'LoadingSurface', 'ErrorSurface', 'Screen']

def get_target_file(name, content):
    if name in keep_in_main: return 'MainScreen.kt'
    for target, names in mapping.items():
        if name in names: return target
    
    # Fallbacks
    if name in shared_elements: return 'models/SharedComponents.kt'
    if 'fun Context.' in content: return 'models/MainModels.kt'
    if 'fun Timeline.' in content: return 'models/MainModels.kt'
    if 'fun TimelineClip.' in content: return 'models/MainModels.kt'
    
    return 'models/Miscellaneous.kt'

final_files = {}

for name, content in chunks:
    # Clean up visibility
    clean_content = content.replace('private val ', 'val ').replace('private fun ', 'fun ')
    clean_content = clean_content.replace('private class ', 'class ').replace('private enum ', 'enum ')
    clean_content = clean_content.replace('private data ', 'data ')
    clean_content = clean_content.replace('internal val ', 'val ').replace('internal fun ', 'fun ')
    clean_content = clean_content.replace('internal class ', 'class ').replace('internal enum ', 'enum ')
    clean_content = clean_content.replace('internal data ', 'data ')

    target = get_target_file(name, clean_content)
    if target not in final_files: final_files[target] = []
    final_files[target].append(clean_content)

base_path = 'app/src/main/java/com/example/clipystudio/ui/main'
for rel_path, contents in final_files.items():
    full_path = os.path.join(base_path, rel_path.replace('/', os.sep))
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    
    pkg = "com.example.clipystudio.ui.main"
    if '/' in rel_path: pkg += "." + rel_path.rsplit('/', 1)[0].replace('/', '.')
    
    with open(full_path, 'w', encoding='utf-8') as f:
        f.write(f"package {pkg}\n")
        # Add a bunch of common imports (simpler than selective import)
        f.write("""
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
import com.example.clipystudio.*
import com.example.clipystudio.ui.main.*
import com.example.clipystudio.ui.main.models.*
import com.example.clipystudio.ui.main.screens.*
import com.example.clipystudio.ui.main.editor.*
import com.example.clipystudio.ui.main.editor.components.*
import com.example.clipystudio.ui.main.editor.panels.*
import com.example.clipystudio.ui.main.editor.timeline.*
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.*
""")
        f.write("\n")
        f.write("\n\n".join(contents))

print("Super deep refactor completed.")
