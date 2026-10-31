"""Publish compact evidence; keep downloaded tools, runtime logs and worlds local."""
from pathlib import Path
import hashlib,json,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
def read_json(relative): return json.loads((ROOT/relative).read_text(encoding='utf-8'))
junit=ET.parse(ROOT/'fauna/build/test-results/test/TEST-net.magister.liberanimalium.platform.net.FragmentAssemblerTest.xml').getroot()
games=ET.parse(ROOT/'.reference/gametest/reports/gametest.xml').getroot()
result={
    'date':'2026-10-31', 'minecraft':'1.20.1','loader':'0.18.2','fabricApi':'0.92.12+1.20.1','buildJvm':21,'packagedRuntimeJvm':17,
    'junit': {k:junit.get(k) for k in ['tests','failures','errors','skipped']},
    'gameTests':[case.get('name') for case in games.iter('testcase')],
    'packagedClient': (ROOT/'.reference/packaged-client/verification/passed.txt').read_text().splitlines(),
    'packagedServer':read_json('.reference/packaged-server-result.json'),
    'reproducibleReleaseJars':read_json('.reference/repro-result.json'),
    'resources':{},'releaseArtifacts':{},'manualCoverage':'See VERIFICATION.md; exhaustive gameplay and real-player latency tests remain unverified.'
}
compat = ROOT/'.reference/pack-compat-result.json'
if compat.exists(): result['modpackCompatibility'] = json.loads(compat.read_text())
bcg = ROOT/'docs/bcg-compatibility-results.json'
if bcg.exists():
    evidence = json.loads(bcg.read_text())
    result['bigChadGuysCompatibility'] = {
        'report':'bcg-compatibility-results.json', 'clientChecks':len(evidence['clientChecks']),
        'sourceTopLevelJars':evidence['sourceTopLevelJars'], 'exitCode':evidence['exitCode'],
        'shaderPack':evidence['shaderPack'], 'shadersEnabled':evidence['shadersEnabled']}
for module,prefix in [('fauna','fauna'),('bookofdragons','book')]:
    audit=read_json('.reference/'+prefix+'-packaged-audit.json')
    result['resources'][module]=audit['resourceComparison']
    for path in (ROOT/module/'build/libs').glob('*.jar'):
        if any(word in path.name for word in ['sources','verification']): continue
        result['releaseArtifacts'][path.name]={'bytes':path.stat().st_size,'sha256':hashlib.sha256(path.read_bytes()).hexdigest()}
(ROOT/'docs/verification-results.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'junit':result['junit'],'gameTests':len(result['gameTests']),'clientChecks':len(result['packagedClient']),
    'packagedServerReady':result['packagedServer']['ready'],'reproducible':all(result['reproducibleReleaseJars'].values())}))
