"""Apply finite-value decoding only to actual registered client-to-server messages."""
from pathlib import Path
import re
root = Path(__file__).resolve().parents[1]
source = root / 'fauna/src/main/java/net/magister/liberanimalium/network'
network = (source / 'FaunaNetwork.java').read_text(encoding='utf-8')
messages = re.findall(r'INSTANCE\.toServer\(\s*"[^"]+",\s*(\w+)\.class', network)
changed = []
for name in messages:
    path = source / (name + '.java')
    text = path.read_text(encoding='utf-8')
    updated = re.sub(r'\b(buffer|buf)\.read(Float|Double)\(\)', lambda m: 'net.magister.liberanimalium.platform.net.PacketValues.finite'+m[2]+'('+m[1]+')', text)
    if text != updated:
        path.write_text(updated, encoding='utf-8')
        changed.append(name)
print('Finite-value decoders:', ', '.join(changed))
