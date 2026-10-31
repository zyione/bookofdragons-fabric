"""Copy an instance's gameplay configuration into a disposable client test.

Never launches the original game directory or copies its saves/accounts/servers.
Requires tools/prepare_packaged_client.py's official Java 17 runtime.
"""
from pathlib import Path
import argparse, hashlib, json, shutil, subprocess, time, urllib.request

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--instance', type=Path, required=True, help='Launcher instance directory')
parser.add_argument('--name', default='bcg')
parser.add_argument('--timeout', type=int, default=900)
parser.add_argument('--reuse', action='store_true', help='Reuse the existing isolated copy')
options = parser.parse_args()
options.instance = options.instance.resolve()
if not options.name.replace('-', '').isalnum(): parser.error('name must be letters, numbers or hyphens')
source = (options.instance/'.minecraft').resolve()
target = ROOT/'.reference'/('instance-client-'+options.name)
target.mkdir(parents=True, exist_ok=True)
if (target.resolve().is_relative_to(source) or source.is_relative_to(target.resolve())
        or not (source/'mods').is_dir()): raise ValueError('Source and test directories must be separate')
folders = ['mods', 'config', 'defaultconfigs', 'kubejs', 'resourcepacks', 'shaderpacks',
           'fancymenu_data', 'datapacks', 'moonlight-global-datapacks', 'BODGeneticsPacks',
           'tacz', 'coremods', 'repository-resourcepacks']
if not options.reuse:
    if (target/'mods').exists(): raise ValueError('Test copy exists; use --reuse or choose a fresh name')
    for name in folders:
        if (source/name).is_dir(): shutil.copytree(source/name, target/name)
    shutil.copy2(source/'options.txt', target/'options.txt')
    inventory = {p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in (source/'mods').glob('*.jar')}
    (target/'source-mod-hashes.json').write_text(json.dumps(inventory, indent=2))
else:
    original = json.loads((target/'source-mod-hashes.json').read_text())
    current = {p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in (source/'mods').glob('*.jar')}
    if original != current: raise ValueError('Source mods changed; choose a fresh test name')
for module, name in [('fauna','fauna-engine-fabric-1.0+1.20.1.fabric.dev.1.jar'),
                     ('bookofdragons','bookofdragons-fabric-1.4+1.20.1.fabric.dev.1.jar'),
                     ('bookofdragons','bookofdragons-fabric-1.4+1.20.1.fabric.dev.1-verification.jar')]:
    shutil.copy2(ROOT/module/'build/libs'/name, target/'mods'/name)
prior = target/'verification'
if prior.exists(): prior.rename(target/('verification-previous-'+str(time.time_ns())))
runtime = ROOT/'.reference/packaged-client'
args = json.loads((runtime/'launch-args.json').read_text())
components = json.loads((options.instance/'mmc-pack.json').read_text())['components']
if next(c['version'] for c in components if c['uid']=='net.minecraft') != '1.20.1':
    raise ValueError('Only Minecraft 1.20.1 is supported')
loader = next(c['version'] for c in components if c['uid']=='net.fabricmc.fabric-loader')
with urllib.request.urlopen('https://meta.fabricmc.net/v2/versions/loader/1.20.1/'+loader+'/profile/json', timeout=30) as response:
    profile = json.load(response)
cp_index = args.index('-cp')+1
classpath = [p for p in args[cp_index].split(';') if not any(s in p for s in
             ['\\net\\fabricmc\\fabric-loader\\','\\net\\fabricmc\\sponge-mixin\\','\\org\\ow2\\asm\\'])]
for lib in profile['libraries']:
    group, name, version = lib['name'].split(':')
    relative = Path(group.replace('.','/'))/name/version/(name+'-'+version+'.jar')
    path = target/'libraries'/relative
    if not path.exists():
        path.parent.mkdir(parents=True, exist_ok=True)
        with urllib.request.urlopen(lib.get('url','https://maven.fabricmc.net/').rstrip('/')+'/'+relative.as_posix(), timeout=30) as response:
            path.write_bytes(response.read())
    if str(path) not in classpath: classpath.append(str(path))
args[cp_index] = ';'.join(classpath)
args[0] = '-Xmx6G'
args.insert(1, '-Dbookofdragons.instanceCompatibility=true')
args[args.index('--gameDir')+1] = str(target)
assets = options.instance.parent.parent/'assets'
if (assets/'indexes/5.json').exists(): args[args.index('--assetsDir')+1] = str(assets)
# Never use the user's fullscreen setting for an unattended test window.
settings = (target/'options.txt').read_text()
settings = '\n'.join('fullscreen:false' if line.startswith('fullscreen:') else line for line in settings.splitlines())+'\n'
(target/'options.txt').write_text(settings)
start = time.monotonic()
print('Starting isolated instance test: '+str(target), flush=True)
with (target/'launch.log').open('w', encoding='utf-8') as log:
    process = subprocess.Popen([r'C:\Program Files\Java\jdk-17\bin\java.exe',*args], cwd=target, stdout=log, stderr=subprocess.STDOUT)
    try: code = process.wait(timeout=options.timeout)
    except subprocess.TimeoutExpired:
        process.terminate();process.wait(timeout=15);raise
passed = target/'verification/passed.txt'
report = {'loader':loader,'java':17,'exitCode':code,'elapsedSeconds':round(time.monotonic()-start,1),
          'sourceTopLevelJars':len(json.loads((target/'source-mod-hashes.json').read_text())),
          'releaseJars':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in (target/'mods').glob('*.jar')
                         if p.name.startswith(('bookofdragons-', 'fauna-engine-')) and 'verification' not in p.name},
          'clientChecks':passed.read_text().splitlines() if passed.exists() else [],
          'scope':'Copied instance mods, configs, scripts, resource packs and shader settings; fresh isolated world; no user saves'}
(target/'result.json').write_text(json.dumps(report,indent=2))
print(json.dumps(report))
if code or not passed.exists(): raise SystemExit(1)
