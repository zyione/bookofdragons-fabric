"""Run only the isolated packaged Java 17 server; always stop the child started here."""
from pathlib import Path
import hashlib, json, queue, shutil, subprocess, threading, time
ROOT = Path(__file__).resolve().parents[1]
SERVER = ROOT / '.reference/packaged-server'
MODS = SERVER / 'mods'
MODS.mkdir(parents=True, exist_ok=True)
artifacts = [ROOT / 'fauna/build/libs/fauna-engine-fabric-1.0+1.20.1.fabric.dev.1.jar',
             ROOT / 'bookofdragons/build/libs/bookofdragons-fabric-1.4+1.20.1.fabric.dev.1.jar']
cache = Path.home() / '.gradle/caches/modules-2/files-2.1'
for group, name, version in [('net.fabricmc.fabric-api', 'fabric-api', '0.92.12+1.20.1'),
                              ('maven.modrinth', 'azurelib', 'VDXznzXQ'),
                              ('maven.modrinth', 'forge-config-api-port', 'Qs8LaZLo')]:
    matches = [p for p in (cache/group/name/version).rglob('*.jar') if not p.name.endswith('-sources.jar')]
    assert len(matches) == 1, (name, matches)
    artifacts.append(matches[0])
for path in artifacts: shutil.copy2(path, MODS/path.name)
(SERVER/'eula.txt').write_text('eula=true\n')
(SERVER/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25652\nonline-mode=false\nview-distance=3\nsimulation-distance=3\nmax-tick-time=60000\n')
lines = queue.Queue()
process = subprocess.Popen([r'C:\Program Files\Java\jdk-17\bin\java.exe', '-Xmx2G', '-jar', 'fabric-server-launch.jar', 'nogui'],
    cwd=SERVER, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding='utf-8', errors='replace')
def read():
    for line in process.stdout: lines.put(line)
threading.Thread(target=read, daemon=True).start()
ready, output, spawned = False, [], 0
ready_at = None
deadline = time.monotonic()+150
try:
    while time.monotonic() < deadline and process.poll() is None:
        if ready_at is not None and time.monotonic() - ready_at > 10:
            process.stdin.write('save-all\nstop\n'); process.stdin.flush()
            ready_at = None
        try: line = lines.get(timeout=1)
        except queue.Empty: continue
        output.append(line)
        if 'Done (' in line:
            ready = True
            ready_at = time.monotonic()
            for index, species in enumerate(['nightfury','deadlynadder','monstrousnightmare','gronckle','hiddeous_zippleback','skrill','whispering_death','terrible_terror','speed_stinger']):
                process.stdin.write(f'summon bookofdragons:{species} {index*10} 120 0 {{NoAI:1b}}\n')
            process.stdin.flush()
        if 'Summoned new' in line: spawned += 1
    process.wait(timeout=30)
finally:
    if process.poll() is None:
        process.terminate()
        process.wait(timeout=15)
    while not lines.empty(): output.append(lines.get())
    (ROOT/'.reference/packaged-server-launch.log').write_text(''.join(output), encoding='utf-8')
report = {'java': '17', 'ready': ready, 'spawnedSpecies': spawned, 'exitCode': process.returncode,
          'artifacts': {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in artifacts}}
(ROOT/'.reference/packaged-server-result.json').write_text(json.dumps(report, indent=2)+'\n')
print(json.dumps({'ready':ready, 'spawnedSpecies':spawned, 'exitCode': process.returncode}))
if not ready or spawned != 9 or process.returncode != 0: raise SystemExit(1)
