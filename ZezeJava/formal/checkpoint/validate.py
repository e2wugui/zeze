"""Run the related Java tests through the shared Gradle mutex."""
import os
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET

FORMAL = Path(__file__).resolve().parents[1]
REPO = FORMAL.parents[1]
OUT = FORMAL / 'integration/build/java'
SELECTION = {
    'test': (
        'TestAchillesHeelDaemonReleaseRange', 'TestCommitPathSalvage',
        'TestDaemonDestroySubprocessIdempotent', 'TestDaemonDiagnosticDeadline',
        'TestDaemonShutdownKeepsUnregistered', 'TestFastAdmissionGuard',
        'TestFormalCacheCoherence', 'TestFormalCheckpoint', 'TestFormalLockProtocol',
        'TestFormalRollback', 'TestGlobalAgentSetActiveTimeNoDaemon',
        'TestGlobalFatalSaveWindow', 'TestHaltDeadlineNotBlockedByCheckpoint',
        'TestMidFlushHaltWindow', 'TestRecordFlushScope',
        'TestSerializableCommitHistories', 'TestSharedCacheCommitHandoff',
    ),
    'integrationTest': ('TestTransactionLevelSerializable',),
}


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    gradle = ['-I', 'formal/integration/validation.gradle', '--console=plain', '--max-workers=2']
    for task, names in SELECTION.items():
        gradle += [':ZezeJavaTest:' + task, '--rerun']
        for name in names:
            gradle += ['--tests', '*' + name]
    quoted = ','.join("'" + arg.replace("'", "''") + "'" for arg in gradle)
    command = '& .\\ZezeJava\\formal\\run-gradle.ps1 -GradleArgs @(' + quoted + ')'
    env = os.environ.copy()
    env.setdefault('JAVA_HOME', r'C:\Program Files\Eclipse Adoptium\jdk-21.0.13+9')
    with (OUT / 'gradle.log').open('wb') as log:
        result = subprocess.run(['powershell.exe', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', command],
                                cwd=REPO, env=env, stdout=log, stderr=subprocess.STDOUT)
    print(f'Gradle exit: {result.returncode}; log: {OUT / "gradle.log"}', flush=True)
    if result.returncode:
        print((OUT / 'gradle.log').read_text(encoding='utf-8', errors='replace')[-6000:])
        return result.returncode

    for task, names in SELECTION.items():
        reports = [ET.parse(p).getroot() for p in (OUT / task / 'xml').glob('TEST-*.xml')]
        observed = {r.attrib['name'].rsplit('.', 1)[-1] for r in reports}
        assert observed == set(names), ('unexpected test selection', task, observed)
        for report in reports:
            assert int(report.attrib['tests']) > 0, report.attrib
            assert all(int(report.attrib[k]) == 0 for k in ('failures', 'errors', 'skipped')), report.attrib
        print(f'{task}: {sum(int(r.attrib["tests"]) for r in reports)} tests passed; no failures/errors/skips.')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
