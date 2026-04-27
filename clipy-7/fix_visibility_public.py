import os
import re

base_path = 'app/src/main/java/com/example/clipystudio/ui/main'

def walk_files(path):
    for root, dirs, files in os.walk(path):
        for f in files:
            if f.endswith('.kt'):
                yield os.path.join(root, f)

patterns = [
    (r'^internal enum class', 'enum class'),
    (r'^internal data class', 'data class'),
    (r'^internal val', 'val'),
    (r'^internal fun', 'fun'),
    (r'^internal suspend fun', 'suspend fun'),
    (r'^internal class', 'class'),
    (r'^private enum class', 'enum class'),
    (r'^private data class', 'data class'),
    (r'^private val', 'val'),
    (r'^private fun', 'fun'),
    (r'^private suspend fun', 'suspend fun'),
    (r'^private class', 'class')
]

for filepath in walk_files(base_path):
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
        
    new_lines = []
    for line in lines:
        new_line = line
        for pattern, replacement in patterns:
            if re.match(pattern, line):
                new_line = re.sub(pattern, replacement, line)
                break
        new_lines.append(new_line)
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.writelines(new_lines)

print("Visibility updated to public for all files in ui/main.")
