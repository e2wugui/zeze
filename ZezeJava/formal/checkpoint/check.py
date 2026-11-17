"""Single-worker, completed TLC runs; expected counterexamples are explicit."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time

HERE = Path(__file__).resolve().parent
FORMAL = HERE.parent
ROOT = FORMAL.parents[1]
JAR = FORMAL / 'serializability/build/tools/tla2tools.jar'
JAVA = Path(os.environ.get('JAVA_HOME', r'C:\Program Files\Eclipse Adoptium\jdk-21.0.13+9')) / 'bin/java.exe'
SHA = '936a262061c914694dfd669a543be24573c45d5aa0ff20a8b96b23d01e050e88'

def digest(p):
    return hashlib.sha256(p.read_bytes()).hexdigest()

def cases(section):
    if section == 'checkpoint':
        yield 'table', 'async', 'none', False, None, False, 2, 2
        yield 'table-three-writes', 'async', 'none', False, None, False, 3, 2
        yield 'immediately', 'immediate', 'none', False, None, False, 2, 2
        yield 'when-commit', 'whenCommit', 'none', False, None, False, 2, 2
        yield 'eventual-flush', 'async', 'none', False, None, True, 2, 1
        yield 'negative-early-clean', 'async', 'earlyClean', False, 'CleanSound', False, 2, 1
        yield 'negative-unlocked-commit', 'async', 'unlocked', False, 'CleanSound', False, 2, 1
        yield 'negative-overlapping-batches', 'async', 'overlap', False, 'NoOlderOverwrite', False, 2, 2
        yield 'negative-rollback-value', 'async', 'rollback', False, 'NoRollbackValue', False, 2, 1
        yield 'negative-split-group', 'async', 'splitGroup', False, 'GroupAtomic', False, 2, 1
        yield 'negative-no-success', 'async', 'neverSuccess', False, 'EveryWritePersists', True, 1, 1
        yield 'witness-retry', 'async', 'none', False, 'NoRetryWitness', False, 2, 1
        yield 'witness-concurrent-disjoint', 'async', 'none', False, 'NoConcurrentWitness', False, 2, 2
        yield 'witness-new-dirty-after-flush', 'async', 'none', False, 'NoPostFlushWriteWitness', False, 2, 1
        yield 'witness-non-prefix', 'async', 'none', False, 'NoNonPrefixWitness', False, 2, 1
        yield 'stop-healthy', 'async', 'none', False, None, False, 0, 1
        yield 'stop-transient', 'async', 'none', False, None, False, 1, 1
        yield 'stop-negative-skip-wait', 'async', 'skipWait', False, 'NoCloseDuringCommit', False, 0, 1
        yield 'stop-witness-exhausted', 'async', 'none', False, 'NoAbandonedDirty', False, 3, 1
    else:
        for mode in ('async', 'immediate', 'whenCommit'):
            yield 'crash-' + mode, mode, 'none', True, None, False, 2, 1
        yield 'witness-async-loss', 'async', 'none', True, 'NoAsyncLossWitness', False, 1, 1
        yield 'witness-durable-crash', 'immediate', 'none', True, 'NoDurableCrashWitness', False, 1, 1
        yield 'negative-early-ack', 'immediate', 'ackEarly', True, 'DurableAck', False, 1, 1
        yield 'negative-lost-medium', 'immediate', 'lostMedia', True, 'NoOlderOverwrite', False, 1, 1

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--section', choices=('checkpoint','recovery','all'), default='all')
    ap.add_argument('--only')
    args = ap.parse_args()
    assert digest(JAR) == SHA, 'TLC jar identity mismatch'
    results = []
    sections = ('checkpoint','recovery') if args.section == 'all' else (args.section,)
    for section in sections:
        home = FORMAL / section
        (home / 'configs').mkdir(exist_ok=True)
        for name,mode,bug,crash,expected,live,writes,slots in cases(section):
            if args.only and args.only not in name:
                continue
            cfg = home / 'configs' / (name + '.cfg')
            stop = name.startswith('stop-')
            invariants = ['NoRollbackValue','CleanSound','NoOlderOverwrite','DurableAck','Partition','GroupAtomic']
            if name.startswith('negative-') and not live:
                invariants = [expected]
            if expected and expected != 'EveryWritePersists' and expected not in invariants:
                invariants.append(expected)
            cfg.write_text('SPECIFICATION ' + ('LiveSpec' if live else 'Spec') + '\n'
                + 'CONSTANTS\n Keys = {a,b}\n Slots = {' + ','.join('s'+str(i) for i in range(slots)) + '}\n'
                + f' MaxWrites = {writes}\n MaxFailures = 1\n Mode = "{mode}"\n Bug = "{bug}"\n AllowCrash = {str(crash).upper()}\n'
                + 'CHECK_DEADLOCK FALSE\nINVARIANTS\n ' + '\n '.join(invariants) + '\n'
                + ('PROPERTY EveryWritePersists\n' if live else ''), encoding='utf-8')
            if stop:
                cfg.write_text('SPECIFICATION Spec\nCONSTANTS\n'
                    + f' MaxFailures = {writes}\n Bug = "{bug}"\nCHECK_DEADLOCK FALSE\nINVARIANTS\n '
                    + ('NoCloseDuringCommit\n ClosedSound\n' if not expected else expected+'\n'), encoding='utf-8')
            build = home / 'build' / name
            build.mkdir(parents=True, exist_ok=True)
            # Isolate both module lookup and TLC metadir from other chats/runs.
            (build / 'Checkpoint.tla').write_bytes((HERE / 'Checkpoint.tla').read_bytes())
            module = 'Checkpoint'
            if stop:
                module = 'Stop'
                (build / 'Stop.tla').write_bytes((HERE / 'Stop.tla').read_bytes())
            if section == 'recovery':
                module = 'Recovery'
                (build / 'Recovery.tla').write_bytes((home / 'Recovery.tla').read_bytes())
            cmd = [str(JAVA), '-Xmx768m', '-cp', str(JAR), 'tlc2.TLC', '-workers', '1',
                   '-metadir', str(build / ('states-' + str(time.time_ns()))),
                   '-config', str(cfg), module]
            start = time.monotonic()
            try:
                p = subprocess.run(cmd, cwd=build, capture_output=True, text=True, encoding='utf-8', errors='replace', timeout=240)
                log = p.stdout + p.stderr
                (build / 'tlc.log').write_text(log, encoding='utf-8')
                correct = p.returncode == 0 and 'Model checking completed. No error has been found.' in log
                if expected:
                    message = 'Temporal properties were violated' if live else f'Invariant {expected} is violated'
                    correct = p.returncode in (12,13) and message in log
                states = re.findall(r'([\d,]+) states generated, ([\d,]+) distinct states found', log)
                result = dict(name=name, expected=expected, exit=p.returncode, passed=correct,
                              seconds=round(time.monotonic()-start,2), states=states[-1] if states else None,
                              cfg_sha256=digest(cfg), module_sha256=digest(HERE/('Stop.tla' if stop else 'Checkpoint.tla')))
            except subprocess.TimeoutExpired as e:
                (build / 'tlc.log').write_bytes((e.stdout or b'') + (e.stderr or b''))
                result = dict(name=name, passed=False, timeout=True)
            results.append(result)
            print(json.dumps(result), flush=True)
        local = [r for r in results if (home/'configs'/(r['name']+'.cfg')).exists()]
        if args.only:
            output = home / 'build' / ('results-' + args.only + '.json')
        else:
            output = home / 'verification.json'
        sources = ['ZezeJava/ZezeJava/src/main/java/Zeze/' + s for s in
                   ['Application.java','LocalCacheResources.java','Transaction/Checkpoint.java',
                    'Transaction/RelativeRecordSet.java','Transaction/Record.java','Transaction/Record1.java',
                    'Transaction/TableX.java','Transaction/TransactionLog.java','Transaction/DatabaseRocksDb.java']]
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(json.dumps(dict(tool_sha256=SHA, java_version=subprocess.run([str(JAVA),'-version'],capture_output=True,text=True).stderr,
            git_head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
            sources={s:digest(ROOT/s) for s in sources}, results=local),indent=2),encoding='utf-8')
    return 0 if all(r['passed'] for r in results) else 1

if __name__ == '__main__':
    sys.exit(main())
