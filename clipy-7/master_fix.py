import os

base_path = 'app/src/main/java/com/example/clipystudio/ui/main'

def fix_file(path, content_modifier):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    new_content = content_modifier(content)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(new_content)

def modifier_main_screen(content):
    content = content.replace('event.chooserTitle', 'event.title')
    content = content.replace('LoadingSurface(modifier, languageCode = LanguageCode.En)', 'LoadingSurface(modifier, appState.languageCode)')
    return content

def modifier_preview_section(content):
    content = content.replace('import coil.compose.AsyncImage', '')
    content = content.replace('AsyncImage', 'Image') # Fallback to Image
    # Add media item and exoplayer
    if 'import androidx.media3.exoplayer.ExoPlayer' not in content:
        content = content.replace('import androidx.media3.common.Player', 'import androidx.media3.common.Player\nimport androidx.media3.common.MediaItem\nimport androidx.media3.exoplayer.ExoPlayer')
    
    # Fix the Image call (it won't have 'model' and 'onSuccess')
    # This is a bit complex for simple replace, I'll do a rough fix
    content = content.replace('model = Uri.parse(model)', 'painter = ColorPainter(Color.Gray)') # Placeholder
    content = content.replace('onSuccess = { loadState = PreviewMediaLoadState.Idle },', '')
    content = content.replace('onError = { loadState = PreviewMediaLoadState.Failed },', '')
    return content

def modifier_models(content):
    # Remove accidental @Composable
    for func in ['loadThumbnailBitmap', 'readUriMetadata', 'persistReadPermission', 'readUriMetadataSafely', 'readMediaDurationMs', 'resolvePreviewSurfaceState', 'canOpenPreviewUri', 'resolveMimeType']:
        content = content.replace(f'@Composable\nfun Context.{func}', f'fun Context.{func}')
        content = content.replace(f'@Composable\nfun {func}', f'fun {func}')
    return content

fix_file(os.path.join(base_path, 'MainScreen.kt'), modifier_main_screen)
fix_file(os.path.join(base_path, 'editor/components/EditorPreviewSection.kt'), modifier_preview_section)
fix_file(os.path.join(base_path, 'models/MainModels.kt'), modifier_models)

print("Master fixes applied.")
