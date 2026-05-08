import tempfile
from dataclasses import dataclass
from pathlib import Path


@dataclass
class Workspace:
    root: Path


def create_workspace(prefix: str = "android-agent-") -> Workspace:
    p = Path(tempfile.mkdtemp(prefix=prefix))
    return Workspace(root=p)
