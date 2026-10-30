"""Prepare an isolated official client + Fabric runtime, never a launcher instance."""
from pathlib import Path
import concurrent.futures, hashlib, json, os, shutil, urllib.request, zipfile
ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / '.reference/packaged-client'
TARGET.mkdir(parents=True, exist_ok=True)

def get_json(url):
    with urllib.request.urlopen(url, timeout=60) as response: return json.load(response)
def download(url, path, sha1=None):
    if not path.exists() or (sha1 and hashlib.sha1(path.read_bytes()).hexdigest() != sha1):
        path.parent.mkdir(parents=True, exist_ok=True)
        with urllib.request.urlopen(url, timeout=60) as response: path.write_bytes(response.read())
    if sha1: assert hashlib.sha1(path.read_bytes()).hexdigest() == sha1, path
    return path

manifest = get_json('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json')
version = get_json(next(v['url'] for v in manifest['versions'] if v['id']=='1.20.1'))
profile = get_json('https://meta.fabricmc.net/v2/versions/loader/1.20.1/0.18.2/profile/json')
(TARGET/'version.json').write_text(json.dumps(version, indent=2))
(TARGET/'fabric-profile.json').write_text(json.dumps(profile, indent=2))
artifacts=[]; native_archives=[]; jobs=[]
client = version['downloads']['client']
jobs.append((client['url'], TARGET/'client.jar', client['sha1']))
artifacts.append(TARGET/'client.jar')

def allowed(lib):
    result = 'rules' not in lib
    for rule in lib.get('rules', []):
        system = rule.get('os', {})
        if system.get('name','windows') != 'windows': continue
        if 'features' in rule: continue
        result = rule['action'] == 'allow'
    return result

for lib in version['libraries']:
    if not allowed(lib): continue
    artifact = lib.get('downloads', {}).get('artifact')
    if artifact:
        path = TARGET/'libraries'/artifact['path']; artifacts.append(path)
        jobs.append((artifact['url'], path, artifact.get('sha1')))
    classifier = lib.get('natives',{}).get('windows')
    if classifier:
        artifact=lib['downloads']['classifiers'][classifier.replace('${arch}','64')]
        path=TARGET/'libraries'/artifact['path']; native_archives.append(path)
        jobs.append((artifact['url'],path,artifact.get('sha1')))
for lib in profile['libraries']:
    group, name, ver = lib['name'].split(':')
    relative=Path(group.replace('.','/'))/name/ver/(name+'-'+ver+'.jar')
    path=TARGET/'libraries'/relative; artifacts.append(path)
    jobs.append((lib.get('url','https://maven.fabricmc.net/').rstrip('/')+'/'+relative.as_posix(),path,None))
with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
    for result in pool.map(lambda args: download(*args), jobs): pass
natives=TARGET/'natives';natives.mkdir(exist_ok=True)
for archive in native_archives:
    with zipfile.ZipFile(archive) as data:
        for name in data.namelist():
            if name.endswith('.dll'): (natives/Path(name).name).write_bytes(data.read(name))
mods=TARGET/'mods';mods.mkdir(exist_ok=True)
for module, pattern in [('fauna','fauna-engine-fabric-1.0+1.20.1.fabric.dev.1.jar'),('bookofdragons','bookofdragons-fabric-1.4+1.20.1.fabric.dev.1.jar'),
                        ('bookofdragons','bookofdragons-fabric-1.4+1.20.1.fabric.dev.1-verification.jar')]:
    shutil.copy2(ROOT/module/'build/libs'/pattern, mods/pattern)
for path in (ROOT/'.reference/packaged-server/mods').glob('*.jar'):
    if not path.name.startswith(('fauna-engine','bookofdragons')): shutil.copy2(path,mods/path.name)
(TARGET/'options.txt').write_text('autoJump:false\nenableVsync:false\nmaxFps:60\nrenderDistance:4\nsimulationDistance:5\nguiScale:2\nonboardAccessibility:false\nsoundCategory_master:0.0\n')
assets=Path.home()/'.gradle/caches/fabric-loom/assets'
args=['-Xmx3G', '-Dbookofdragons.clientVerification=true', '-Djava.library.path='+str(natives),
      '-cp',os.pathsep.join(map(str,artifacts)),profile['mainClass'], '--username','PortVerifier', '--version','1.20.1',
      '--gameDir',str(TARGET),'--assetsDir',str(assets),'--assetIndex',version['assetIndex']['id'],
      '--uuid','cf809485d0124df19e6430f510f8ab01','--accessToken','0','--userType','legacy','--width','1280','--height','800']
(TARGET/'launch-args.json').write_text(json.dumps(args,indent=2))
print('Isolated packaged client ready; '+str(len(artifacts))+' libraries')
