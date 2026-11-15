"""Make a small durable result index from evidence captured under the Gradle mutex."""
import hashlib
import json
from pathlib import Path
ROOT = Path(__file__).resolve().parent
build = ROOT / 'build'
result = json.loads((build / 'java-results.json').read_text(encoding='utf-8-sig'))
identity = json.loads((build / 'java-identity.json').read_text(encoding='utf-8-sig'))
log = (build / 'java-output.log').read_text(encoding='utf-8')
evidence = {'identity': identity, 'result': result,
            'witnesses': [s for s in log.splitlines() if 'S6_' in s],
            'xml_sha256': hashlib.sha256((build/'java-tests.xml').read_bytes()).hexdigest()}
(ROOT / 'java-verification.json').write_text(json.dumps(evidence,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(result,ensure_ascii=False,indent=2))
assert result['tests'] == 8 and result['failed'] == 0 and result['skipped'] == 0
