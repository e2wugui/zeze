"""Finite TLC checks. No network/download; one worker and independent state dirs."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import time

ROOT = Path(__file__).resolve().parent
JAVA_ROOT = ROOT.parents[1]
JAR = ROOT.parent / 'serializability/build/tools/tla2tools.jar'
JAR_HASH = '936a262061c914694dfd669a543be24573c45d5aa0ff20a8b96b23d01e050e88'
java = Path(os.environ.get('JAVA_HOME', r'C:\Program Files\Eclipse Adoptium\jdk-21.0.13+9')) / 'bin/java.exe'
CASES = []

def case(name, module, constants, invariants, violates=None):
    CASES.append((name, module, constants, invariants, violates))

pbase = {'Clients': '{1,2}', 'MaxRequests': '1', 'InitialShared': 'FALSE', 'Fault': '"none"'}
pinv = ['TypeOK','BookCoherent','LocalCoherent','Covered','Visible']
case('exclusive', 'Permissions', pbase, pinv)
case('shared-three', 'Permissions', dict(pbase, Clients='{1,2,3}', InitialShared='TRUE'), pinv)
case('retry-two', 'Permissions', dict(pbase, MaxRequests='2'), pinv)
case('negative-timeout', 'Permissions', dict(pbase, Fault='"timeout-success"'), ['LocalCoherent'], 'LocalCoherent')
case('negative-flush', 'Permissions', dict(pbase, Fault='"no-flush"'), ['Visible'], 'Visible')
for name, invariant in [('handoff','NoHandoff'),('late-response','NoLateResponse'),('reduce-timeout','NoFailedReduction')]:
    case('witness-'+name,'Permissions',pbase,[invariant],invariant)
case('session-generation','Sessions',{'Fault':'"none"'},['OldSessionIsolated','Covered'])
case('negative-generation','Sessions',{'Fault':'"ignore-generation"'},['OldSessionIsolated'],'OldSessionIsolated')
case('witness-relogin','Sessions',{'Fault':'"none"'},['NoPreservingReLogin'],'NoPreservingReLogin')
case('witness-login','Sessions',{'Fault':'"none"'},['NoFreshLogin'],'NoFreshLogin')
case('witness-kick','Sessions',{'Fault':'"none"'},['NoKick'],'NoKick')
case('witness-close','Sessions',{'Fault':'"none"'},['NoClose'],'NoClose')
lbase={'Pause':'FALSE','FenceOnResume':'FALSE','Fault':'"none"'}
case('bounded-watchdog','Lease',lbase,['NoTwoEffectiveWriters'])
case('pause-resume-counterexample','Lease',dict(lbase,Pause='TRUE'),['NoTwoEffectiveWriters'],'NoTwoEffectiveWriters')
case('resume-fencing-contract','Lease',dict(lbase,Pause='TRUE',FenceOnResume='TRUE'),['NoTwoEffectiveWriters'])
case('negative-disconnect','Lease',dict(lbase,Fault='"disconnect-reclaims"'),['NoTwoEffectiveWriters'],'NoTwoEffectiveWriters')
case('halt-grace-budget-counterexample','HaltBudget',{'GlobalDeadline':'5','ImmediateHalt':'FALSE'},['NoTwoEffectiveWriters'],'NoTwoEffectiveWriters')
case('halt-grace-included-contract','HaltBudget',{'GlobalDeadline':'9','ImmediateHalt':'FALSE'},['NoTwoEffectiveWriters'])
case('halt-immediate-fixed','HaltBudget',{'GlobalDeadline':'5','ImmediateHalt':'TRUE'},['NoTwoEffectiveWriters'])
for name, invariant in [('release','NoRelease'),('exit','NoKill')]:
    case('witness-'+name,'Lease',lbase,[invariant],invariant)

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    assert sha(JAR)==JAR_HASH, 'TLC identity mismatch'
    out=ROOT/'build'
    out.mkdir(exist_ok=True)
    source_paths = [
        'ZezeJava/src/main/java/Zeze/Services/GlobalCacheManagerAsyncServer.java',
        'ZezeJava/src/main/java/Zeze/Services/GlobalCacheManagerServer.java',
        'ZezeJava/src/main/java/Zeze/Services/GlobalCacheManagerWithRaft.java',
        'ZezeJava/src/main/java/Zeze/Services/AchillesHeelConfig.java',
        'ZezeJava/src/main/java/Zeze/Application.java',
        'ZezeJava/src/main/java/Zeze/Services/GlobalCacheManagerConst.java',
        'ZezeJava/src/main/java/Zeze/Transaction/GlobalAgent.java',
        'ZezeJava/src/main/java/Zeze/Transaction/GlobalClient.java',
        'ZezeJava/src/main/java/Zeze/Transaction/GlobalAgentBase.java',
        'ZezeJava/src/main/java/Zeze/Transaction/AchillesHeelDaemon.java',
        'ZezeJava/src/main/java/Zeze/Transaction/TableX.java',
        'ZezeJava/src/main/java/Zeze/Transaction/Record1.java',
        'ZezeJava/src/main/java/Zeze/Net/Rpc.java',
        'ZezeJavaTest/src/Zeze/Services/TestFormalCacheCoherence.java',
    ]
    metadata={'head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=JAVA_ROOT,text=True).strip(),
              'tlc_sha256':sha(JAR),'java':subprocess.run([str(java),'-version'],capture_output=True,text=True).stderr,
              'sources':{p:sha(JAVA_ROOT/p) for p in source_paths if (JAVA_ROOT/p).exists()},
              'models':{p.name:sha(p) for p in ROOT.glob('*.tla')}}
    (out/'metadata.json').write_text(json.dumps(metadata,ensure_ascii=False,indent=2),encoding='utf-8')
    results=[]
    for name,module,constants,invariants,violates in CASES:
        run=out/name
        run.mkdir(exist_ok=True)
        cfg='SPECIFICATION Spec\nCHECK_DEADLOCK FALSE\nCONSTANTS\n'
        cfg+=''.join(f'  {k} = {v}\n' for k,v in constants.items())
        cfg+='INVARIANTS\n'+''.join(f'  {v}\n' for v in invariants)
        (ROOT/(name+'.cfg')).write_text(cfg,encoding='utf-8')
        command=[str(java),'-Xmx1500m','-cp',str(JAR),'tlc2.TLC','-workers','1',
                 '-metadir',str(run/'states'),'-config',name+'.cfg',module+'.tla']
        start=time.monotonic()
        with (run/'tlc.log').open('w',encoding='utf-8') as log:
            result=subprocess.run(command,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,timeout=480)
        log=(run/'tlc.log').read_text(encoding='utf-8')
        if violates and not name.startswith('witness-'):
            traces = ROOT / 'traces'
            traces.mkdir(exist_ok=True)
            (traces / (name+'.log')).write_text(log,encoding='utf-8')
        ok=(result.returncode==0 and 'Model checking completed. No error' in log) if not violates else (
            result.returncode==12 and f'Invariant {violates} is violated' in log)
        counts=re.search(r'([\d,]+) states generated, ([\d,]+) distinct states found',log)
        row={'case':name,'module':module,'exit':result.returncode,'expected_violation':violates,
             'accepted':ok,'seconds':round(time.monotonic()-start,2),
             'generated':int(counts[1].replace(',','')) if counts else None,
             'distinct':int(counts[2].replace(',','')) if counts else None,
             'log':str((run/'tlc.log').relative_to(ROOT))}
        results.append(row)
        (out/'results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
        print(json.dumps(row,ensure_ascii=False),flush=True)
        if not ok:
            print(log[-5000:])
            raise SystemExit(1)
    (ROOT/'verification.json').write_text(json.dumps({'metadata':metadata,'results':results},ensure_ascii=False,indent=2),encoding='utf-8')

if __name__=='__main__':
    main()
