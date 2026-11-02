"""Export numbered, self-contained Discord messages from the player guide."""
from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[1]
source = (ROOT/'docs/PLAYER_GUIDE.md').read_text(encoding='utf-8')
sections = list(re.finditer(r'^## (\d{2})\. (.+)$', source, re.MULTILINE))
output = ROOT/'docs/discord-guide'
messages = []
for index, section in enumerate(sections):
    end = sections[index+1].start() if index+1 < len(sections) else len(source)
    message = source[section.start():end].strip()
    if index == 0: message = source[:section.start()].strip()+'\n\n'+message
    body = message+'\n'
    units = len(body.encode('utf-16-le'))//2
    if units > 1900: raise ValueError(f'Section {section.group(1)} has {units} characters; shorten it before posting')
    if sum(line.startswith('```') for line in message.splitlines()) % 2:
        raise ValueError(f'Unclosed code fence in section {section.group(1)}')
    if re.search(r'^\|', message, re.MULTILINE): raise ValueError('Discord messages must not use Markdown tables')
    slug = re.sub(r'[^a-z0-9]+', '-', section.group(2).lower()).strip('-')
    messages.append((section.group(1)+'-'+slug+'.md', body, units))
if not messages: raise ValueError('No numbered guide sections found')
output.mkdir(parents=True, exist_ok=True)
for filename, body, _ in messages: (output/filename).write_text(body, encoding='utf-8', newline='\n')
manifest = {'standardDiscordLimit':2000, 'exportBudget':1900,
            'messages':[{'file':name,'utf16CharactersIncludingFinalNewline':count} for name,_,count in messages]}
(output/'message-sizes.json').write_text(json.dumps(manifest, indent=2)+'\n', encoding='utf-8')
print(json.dumps({'messages':len(messages),'largestMessage':max(count for _,_,count in messages),'allWithinStandardLimit':all(count<=2000 for _,_,count in messages)}))
