import os

base_path = 'app/src/main/java/com/example/clipystudio/ui/main'

# Add missing imports and fix common issues
add_imports = """
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.BoxScope
"""

def fix_file(path):
    with open(path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    new_lines = []
    has_layout_import = False
    pkg_line = -1
    for i, line in enumerate(lines):
        if line.startswith('package '): pkg_line = i
        if 'import androidx.compose.foundation.layout.*' in line:
            has_layout_import = True
        
        # Robustly ensure @Composable is followed by a function
        # (Sometimes my script might have split them)
        if line.strip() == '@Composable' and i + 1 < len(lines) and not lines[i+1].strip().startswith('fun'):
            # This is a lone @Composable, skip it if the next line is empty
            if not lines[i+1].strip(): continue
        
        # Correctly apply @Composable to functions starting with uppercase if missing
        if line.strip().startswith('fun ') and line.strip()[4].isupper() and '@Composable' not in lines[i-1] and '@Composable' not in line:
            new_lines.append('@Composable\n')
        
        new_lines.append(line)

    # Insert missing imports after package
    if pkg_line != -1:
        new_lines.insert(pkg_line + 1, add_imports)

    with open(path, 'w', encoding='utf-8') as f:
        f.writelines(new_lines)

for root, dirs, files in os.walk(base_path):
    for f in files:
        if f.endswith('.kt'):
            fix_file(os.path.join(root, f))

print("Global fixes applied.")
