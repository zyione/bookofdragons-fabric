"""Compare staged Git blobs, rather than working files alone, with reference assets."""
from pathlib import Path
import hashlib,json,subprocess,zipfile
ROOT=Path(__file__).resolve().parents[1]
checks=[]
for module,reference in [('bookofdragons','original.jar'),('fauna','fauna-forge.jar')]:
    with zipfile.ZipFile(ROOT/'.reference'/reference) as archive:
        for name in archive.namelist():
            if name.endswith('/') or name.endswith('.class') or name.startswith('META-INF/') or name.endswith(('.mixins.json','.refmap.json')): continue
            target=f'{module}/src/main/resources/{name}'
            checks.append((target,hashlib.sha256(archive.read(name)).hexdigest()))
request=''.join(':'+path+'\n' for path,_ in checks)
data=subprocess.run(['git','cat-file','--batch'],input=request.encode(),cwd=ROOT,capture_output=True,check=True).stdout
offset=0; differences=[]
for path,expected in checks:
    end=data.index(b'\n',offset); header=data[offset:end].split();offset=end+1
    if header[-1]==b'missing': differences.append(path+': missing');continue
    size=int(header[-1]);blob=data[offset:offset+size];offset+=size+1
    if hashlib.sha256(blob).hexdigest()!=expected: differences.append(path+': changed')
result={'compared':len(checks),'differences':differences}
(ROOT/'.reference/staged-resource-result.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result))
if differences:raise SystemExit(1)
