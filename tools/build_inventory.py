"""Source-derived registry, event, packet and source inventory for reconciliation."""
from pathlib import Path
import re, json, hashlib, zipfile
root = Path(__file__).resolve().parents[1]
result = {}
for module, reference in [('fauna', 'fauna-forge.jar'), ('bookofdragons', 'original.jar')]:
    source = root / module / 'src/main/java'
    registrations, events, packets = [], [], []
    for path in sorted(source.rglob('*.java')):
        text = path.read_text(encoding='utf-8')
        relative = path.relative_to(root).as_posix()
        for match in re.finditer(r'\b([A-Z][A-Z_]+)\.register\(\s*"([^"]+)"', text):
            registrations.append({'source': relative, 'registry': match[1], 'id': match[2]})
        for match in re.finditer(r'((?:CommonEvents|ClientEvents|FaunaTick)\.[A-Z_]+)\.register\(([^;]+)', text):
            events.append({'source': relative, 'event': match[1], 'listener': match[2].strip()})
        for match in re.finditer(r'INSTANCE\.(toServer|toClient)\(\s*"([^"]+)",\s*(\w+)\.class', text):
            packets.append({'source': relative, 'direction': match[1], 'id': match[2], 'type': match[3]})
    reference_path = root / '.reference' / reference
    result[module] = {'javaFiles': len(list(source.rglob('*.java'))), 'registrations': registrations,
        'events': events, 'packets': packets, 'referenceSha256': hashlib.sha256(reference_path.read_bytes()).hexdigest()}
(root / 'docs/source-inventory.json').write_text(json.dumps(result, indent=2)+'\n', encoding='utf-8')
for module, data in result.items():
    print(module, {k: len(v) if isinstance(v,list) else v for k,v in data.items() if k != 'referenceSha256'})
