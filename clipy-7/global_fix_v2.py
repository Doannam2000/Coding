import os

base_path = 'app/src/main/java/com/example/clipystudio/ui/main'

# Specific imports for certain files
extra_imports_map = {
    'EditorPreviewSection.kt': """
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
""",
    'TimelineView.kt': """
import kotlinx.coroutines.Job
""",
}

def fix_file(path):
    rel_path = os.path.relpath(path, base_path).replace('\\', '/')
    filename = os.path.basename(path)

    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Fix private access for animateTimelineSettle
    content = content.replace('private suspend fun animateTimelineSettle', 'suspend fun animateTimelineSettle')

    # Remove accidental @Composable from non-composable functions
    # These functions use runCatching or are media loading which shouldn't be composable
    for func in ['loadThumbnailBitmap', 'readUriMetadata', 'persistReadPermission', 'readUriMetadataSafely', 'readMediaDurationMs']:
        content = content.replace(f'@Composable\nfun Context.{func}', f'fun Context.{func}')
        content = content.replace(f'@Composable\nfun {func}', f'fun {func}')

    # Fix lone @Composable at end of files or before non-functions
    lines = content.splitlines()
    new_lines = []
    for i, line in enumerate(lines):
        if line.strip() == '@Composable' and i + 1 < len(lines) and not lines[i+1].strip().startswith('fun'):
            continue
        new_lines.append(line)
        
        # Add extra imports after package line
        if line.startswith('package ') and filename in extra_imports_map:
            new_lines.append(extra_imports_map[filename])
            
    # Fix the inferred type issue: mutableStateOf<Job?>(null)
    new_content = "\n".join(new_lines)
    new_content = new_content.replace('mutableStateOf(null)', 'mutableStateOf<Job?>(null)')

    with open(path, 'w', encoding='utf-8') as f:
        f.write(new_content)

for root, dirs, files in os.walk(base_path):
    for f in files:
        if f.endswith('.kt'):
            fix_file(os.path.join(root, f))

print("Targeted fixes applied.")
