"""Import named reconstructions, retaining every shared feature and resource.

The excluded Forge trees remain reference material; their replacements are
tracked in docs/PORT_LEDGER.md. This is not an original-source build.
"""
from pathlib import Path
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[1]
for name, source, jar in [('bookofdragons', 'named', 'original.jar'),
                          ('fauna', 'fauna-named', 'fauna-forge.jar')]:
    module = ROOT / name
    for path in (ROOT / '.reference' / source).rglob('*.java'):
        relative = path.relative_to(ROOT / '.reference' / source)
        if 'forge' in relative.parts:
            continue
        target = module / 'src/main/java' / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        text = path.read_text(encoding='utf-8')
        text = text.replace('ResourceLocation.fromNamespaceAndPath(', 'new ResourceLocation(')
        target.write_text(text, encoding='utf-8')
    with zipfile.ZipFile(ROOT / '.reference' / jar) as archive:
        for info in archive.infolist():
            if info.is_dir() or info.filename.endswith('.class') or info.filename.startswith('META-INF/'):
                continue
            target = module / 'src/main/resources' / info.filename
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(archive.read(info))
