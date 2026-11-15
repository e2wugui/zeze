"""Pinned single-worker TLC checks; errors/timeouts never count as passing."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import subprocess
import time

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[2]
JAR = ROOT.parent / 'serializability/build/tools/tla2tools.jar'
SHA = '936a262061c914694dfd669a543be24573c45d5aa0ff20a8b96b23d01e050e88'

CASES = [
    ('generic-safety', 'Models', None),
    ('generic-liveness', 'Models', None),
    ('evolution-safety', 'Models', None),
    ('evolution-liveness', 'Models', None),
    ('three-way-safety', 'Models', None),
    ('three-way-liveness', 'Models', None),
    ('attempt-boundary-256', 'Models', None),
    ('attempt-boundary-256-liveness', 'Models', None),
    ('negative-order', 'Models', 'NoWaitCycle'),
    ('negative-upgrade', 'Models', 'NoWaitCycle'),
    ('negative-release', 'Models', 'ReturnedReleased'),
    ('witness-retained', 'Models', 'NeverRetained'),
    ('witness-released', 'Models', 'NeverReleased'),
    ('witness-upgrade', 'Models', 'NeverUpgraded'),
    ('witness-insert', 'Models', 'NeverInserted'),
    ('witness-exhausted', 'Models', 'NeverExhausted'),
    ('witness-exception', 'Models', 'NeverException'),
    ('witness-obsolete', 'Models', 'NeverObsoleteReleased'),
    ('witness-trailing', 'Models', 'NeverTrailingRetained'),
    ('record-safety', 'RecordNesting', None),
    ('record-liveness', 'RecordNesting', None),
    ('negative-record-reverse', 'RecordNesting', 'NoCrossLockCycle'),
    ('cross-layer-safety', 'CrossLayers', None),
    ('cross-layer-liveness', 'CrossLayers', None),
    ('negative-reduce-guard', 'CrossLayers', 'NoCrossLayerCycle'),
    ('finite-competition', 'Barging', None),
    ('continuous-progress', 'Barging', None),
    ('continuous-starvation', 'Barging', 'temporal'),
    ('continuous-scheduled-starvation', 'Barging', 'temporal'),
    ('negative-leak-progress', 'Barging', 'temporal'),
]

def digest(p):
    return hashlib.sha256(p.read_bytes()).hexdigest()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java', default='java')
    parser.add_argument('--case', action='append')
    parser.add_argument('--timeout', type=int, default=300)
    args = parser.parse_args()
    if not JAR.exists() or digest(JAR) != SHA:
        raise SystemExit('Incorrect shared TLC 1.7.4 jar')
    if args.case and set(args.case) - {c[0] for c in CASES}:
        parser.error('Unknown case')
    out = ROOT / 'build'
    out.mkdir(exist_ok=True)
    metadata = {
        'started_utc': datetime.now(timezone.utc).isoformat(),
        'revision': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
        'java': subprocess.check_output([args.java, '-version'], stderr=subprocess.STDOUT, text=True).strip(),
        'tlc_sha256': SHA,
        'sources': {str(p.relative_to(REPO)).replace('\\','/'): digest(p) for p in
                    [*ROOT.glob('*.tla'), *ROOT.glob('*.cfg'), Path(__file__),
                     ROOT / 'run-java.ps1', ROOT / 'java-evidence.gradle',
                     *[REPO / ('ZezeJava/ZezeJava/src/main/java/' + name) for name in
                       ['Zeze/Transaction/Transaction.java', 'Zeze/Transaction/Lockey.java',
                        'Zeze/Transaction/Locks.java', 'Zeze/Util/Locks.java',
                        'Zeze/Transaction/TableKey.java', 'Zeze/Transaction/Record.java',
                        'Zeze/Transaction/Record1.java', 'Zeze/Transaction/TableX.java',
                        'Zeze/Transaction/TableCache.java', 'Zeze/Transaction/RelativeRecordSet.java']],
                     REPO / 'ZezeJava/ZezeJavaTest/src/Zeze/Transaction/TestFormalLockProtocol.java']}}
    (out / ('selected-metadata.json' if args.case else 'metadata.json')).write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2), encoding='utf-8')
    results = []
    for name, module, expected in CASES:
        if args.case and name not in args.case:
            continue
        target = out / name
        target.mkdir(exist_ok=True)
        cmd = [args.java, '-Xmx768m', '-XX:+UseParallelGC', '-cp', str(JAR), 'tlc2.TLC',
               '-workers', '1', '-seed', '1', '-fp', '0', '-config', str(ROOT / (name + '.cfg')),
               '-metadir', str(target / 'states'), module]
        start = time.monotonic()
        try:
            run = subprocess.run(cmd, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                 encoding='utf-8', errors='replace', timeout=args.timeout)
            output, code = run.stdout, run.returncode
        except subprocess.TimeoutExpired as err:
            output, code = err.stdout or b'', None
            if isinstance(output, bytes):
                output = output.decode('utf-8', errors='replace')
            output += '\nTIMEOUT: incomplete check\n'
        (target / 'tlc.log').write_text(output, encoding='utf-8')
        if expected == 'temporal':
            passed = code == 13 and 'Temporal properties were violated.' in output
        elif expected:
            passed = code == 12 and f'Invariant {expected} is violated.' in output
        else:
            passed = code == 0 and 'Model checking completed. No error has been found.' in output
        counts = re.findall(r'([\d,]+) states generated, ([\d,]+) distinct states found', output)
        result = dict(name=name, passed=passed, exit_code=code, expected_violation=expected,
                      seconds=round(time.monotonic()-start,2),
                      generated=int(counts[-1][0].replace(',','')) if counts else None,
                      distinct=int(counts[-1][1].replace(',','')) if counts else None,
                      log=f'build/{name}/tlc.log')
        results.append(result)
        print(json.dumps(result, ensure_ascii=False), flush=True)
        if not passed:
            print(output[-6000:], flush=True)
    (out / ('selected-results.json' if args.case else 'results.json')).write_text(
        json.dumps(results, ensure_ascii=False, indent=2), encoding='utf-8')
    raise SystemExit(0 if all(r['passed'] for r in results) else 1)

if __name__ == '__main__':
    main()
