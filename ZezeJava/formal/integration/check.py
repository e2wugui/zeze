"""Check the transaction/permission/persistence interface composition."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import subprocess
import time

ROOT = Path(__file__).resolve().parent
JAR = ROOT.parent / 'serializability/build/tools/tla2tools.jar'
SHA256 = '936a262061c914694dfd669a543be24573c45d5aa0ff20a8b96b23d01e050e88'
CASES = [
    ('increment', {}, None),
    ('pair', {'Scenario': '"pair"'}, None),
    ('skew', {'Scenario': '"skew"'}, None),
    ('no-flush', {'FlushBeforeGrant': 'FALSE'}, 'CacheFresh'),
    ('old-cache', {'ReloadAfterGrant': 'FALSE'}, 'CacheFresh'),
    ('no-validation', {'CheckVersions': 'FALSE'}, 'CommitOrderSerializable'),
    ('witness-both', {}, 'NeverBothCommit'),
    ('witness-handoff', {}, 'NeverHandoff'),
    ('witness-retry', {}, 'NeverRetry'),
    ('fatal-save', {}, None, 'FatalSave'),
    ('fatal-blocked', {'BlockIO':'TRUE'}, None, 'FatalSave'),
    ('fatal-no-gate', {'CloseGate':'FALSE'}, 'SavedOnCleanExit', 'FatalSave'),
    ('fatal-no-drain', {'DrainCommits':'FALSE'}, 'SavedOnCleanExit', 'FatalSave'),
    ('fatal-late-kill', {'KillDeadline':'11'}, 'NoOldWriteAfterGrant', 'FatalSave'),
    ('fatal-pause-boundary', {'BoundedWatchdog':'FALSE'}, 'NoOldWriteAfterGrant', 'FatalSave'),
    ('fatal-remote-io-boundary', {'RemotePending':'TRUE', 'BlockIO':'TRUE'}, 'NoOldWriteAfterGrant', 'FatalSave'),
    ('fatal-witness-save', {}, 'NoSavingWitness', 'FatalSave'),
    ('fatal-witness-retry', {}, 'NoRetryWitness', 'FatalSave'),
    ('fatal-witness-takeover', {}, 'NoTakeoverWitness', 'FatalSave'),
    ('fatal-witness-blocked-exit', {'BlockIO':'TRUE'}, 'NoBlockedExitWitness', 'FatalSave'),
]


def run(case, java, timeout):
    name, changes, expected, *modules = case
    module = modules[0] if modules else 'Handoff'
    out = ROOT / 'build' / name
    out.mkdir(parents=True, exist_ok=True)
    config = (ROOT / f'{module}.cfg').read_text(encoding='utf-8')
    for key, replacement in changes.items():
        config, count = re.subn(rf'(?m)^(\s*{key}\s*=\s*).+$',
                                lambda m: m[1] + replacement, config)
        if count != 1:
            raise ValueError(key)
    if expected:
        config = config.replace('INVARIANTS ', f'INVARIANTS {expected} ')
    path = out / 'model.cfg'
    path.write_text(config, encoding='utf-8')
    command = [java, '-Xmx768m', '-XX:+UseParallelGC', '-cp', str(JAR),
               'tlc2.TLC', '-workers', '1', '-seed', '1', '-fp', '0',
               '-config', str(path), '-metadir', str(out / 'states'), module]
    start = time.monotonic()
    try:
        result = subprocess.run(command, cwd=ROOT, encoding='utf-8', errors='replace',
                                stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                timeout=timeout)
        code, log = result.returncode, result.stdout
    except subprocess.TimeoutExpired as e:
        code = None
        log = e.stdout or b''
        if isinstance(log, bytes):
            log = log.decode('utf-8', errors='replace')
        log += f'\nTIMEOUT after {timeout}s; incomplete.\n'
    (out / 'tlc.log').write_text(log, encoding='utf-8')
    passed = (code == 12 and f'Invariant {expected} is violated.' in log) if expected else (
        code == 0 and 'Model checking completed. No error has been found.' in log)
    counts = re.findall(r'([\d,]+) states generated, ([\d,]+) distinct states found', log)
    record = dict(name=name, passed=passed, exit_code=code, expected_violation=expected,
                  seconds=round(time.monotonic()-start, 2),
                  generated=int(counts[-1][0].replace(',', '')) if counts else None,
                  distinct=int(counts[-1][1].replace(',', '')) if counts else None,
                  config_sha256=hashlib.sha256(config.encode()).hexdigest(),
                  log=f'build/{name}/tlc.log')
    print(json.dumps(record), flush=True)
    if not passed:
        print(log[-5000:], flush=True)
    return record


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--case', action='append')
    p.add_argument('--timeout', type=int, default=300)
    p.add_argument('--java', default='java')
    args = p.parse_args()
    if not JAR.exists() or hashlib.sha256(JAR.read_bytes()).hexdigest() != SHA256:
        p.error('Missing or incorrect pinned TLC jar')
    if args.timeout <= 0 or (args.case and set(args.case)-{c[0] for c in CASES}):
        p.error('Invalid case/timeout')
    cases = [c for c in CASES if not args.case or c[0] in args.case]
    metadata = dict(started_utc=datetime.now(timezone.utc).isoformat(),
                    revision=subprocess.check_output(['git','rev-parse','HEAD'], cwd=ROOT, text=True).strip(),
                    java=subprocess.check_output([args.java,'-version'], stderr=subprocess.STDOUT, text=True),
                    tlc_sha256=SHA256,
                    sources={f: hashlib.sha256((ROOT/f).read_bytes()).hexdigest()
                             for f in ('Handoff.tla','Handoff.cfg',
                                       'FatalSave.tla','FatalSave.cfg','check.py')})
    results = [run(c, args.java, args.timeout) for c in cases]
    metadata['results'] = results
    suffix = '-selected' if args.case else ''
    (ROOT/f'build/results{suffix}.json').write_text(json.dumps(metadata, indent=2), encoding='utf-8')
    raise SystemExit(0 if all(r['passed'] for r in results) else 1)


if __name__ == '__main__':
    main()
