from pathlib import Path
import json, subprocess, time
ROOT=Path(__file__).resolve().parents[1]
TARGET=ROOT/'.reference/packaged-client'
args=json.loads((TARGET/'launch-args.json').read_text())
prior = TARGET/'verification'
if prior.exists(): prior.rename(TARGET/('verification-previous-'+str(time.time_ns())))
with (ROOT/'.reference/packaged-client-launch.log').open('w',encoding='utf-8') as log:
    process=subprocess.Popen([r'C:\Program Files\Java\jdk-17\bin\java.exe',*args],cwd=TARGET,stdout=log,stderr=subprocess.STDOUT)
    try: code=process.wait(timeout=240)
    except subprocess.TimeoutExpired:
        process.terminate();process.wait(timeout=15);raise
passed=TARGET/'verification/passed.txt'
print(json.dumps({'exitCode':code,'passed':passed.exists()}))
if code or not passed.exists(): raise SystemExit(1)
