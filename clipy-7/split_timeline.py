import os
import re

base_path = 'app/src/main/java/com/example/clipystudio/ui/main/editor/timeline'
timeline_view_file = os.path.join(base_path, 'TimelineView.kt')

if os.path.exists(timeline_view_file):
    with open(timeline_view_file, 'r', encoding='utf-8') as f:
        content = f.read()

    # Get header (assume first 60 lines are head)
    lines = content.splitlines()
    header_lines = []
    body_lines = []
    in_header = True
    for line in lines:
        if in_header:
            if line.startswith('@Composable') or line.startswith('val ') or line.startswith('fun ') or line.startswith('class '):
                in_header = False
                body_lines.append(line)
            else:
                header_lines.append(line)
        else:
            body_lines.append(line)
    
    header = "\n".join(header_lines) + "\n"
    
    # Split declarations
    decls = []
    current_decl = []
    current_name = ""
    for line in body_lines:
        if re.match(r'^(@Composable|internal|public|fun|val|class)\b', line):
            if current_decl:
                decls.append((current_name, "\n".join(current_decl)))
            current_decl = [line]
            match = re.search(r'(?:class|fun|val) ([A-Za-z0-9_]+)', line)
            current_name = match.group(1) if match else "UNKNOWN"
        else:
            current_decl.append(line)
    if current_decl:
        decls.append((current_name, "\n".join(current_decl)))

    mapping = {
        'TimelineView.kt': ['TimelineView'],
        'TimelineHeader.kt': ['TimelineHeader'],
        'EngineTrackLane.kt': ['EngineTrackLane'],
        'EngineClipBlock.kt': ['EngineClipBlock', 'TrimHandleGrip'],
        'TimelineSubComponents.kt': ['AutoScrollEdgeMask', 'EdgeResistanceMask', 'TimelineGuides']
    }

    for filename, names in mapping.items():
        chunks = [d[1] for d in decls if d[0] in names]
        if chunks:
            with open(os.path.join(base_path, filename), 'w', encoding='utf-8') as f:
                f.write(header)
                f.write("\n".join(chunks))

print("Timeline split completed.")
