"""Summarize the XML captured under the shared Gradle mutex, retaining source identities."""
import hashlib
import json
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[2]
paths = [REPO/"ZezeJava/ZezeJavaTest/src/Zeze/Transaction/TestFormalRollback.java",
         REPO/"ZezeJava/ZezeJavaTest/src/harness/FastServerIds.java",
         REPO/"ZezeJava/ZezeJavaTest/Gen/demo/Module1/BValue.java",
         REPO/"ZezeJava/ZezeJavaTest/Gen/demo/Module1/Table3.java"]
src = REPO/"ZezeJava/ZezeJava/src/main/java/Zeze/Transaction"
paths += [src/name for name in ("Transaction.java", "Savepoint.java", "TransactionLog.java",
          "RecordAccessed.java", "Record.java", "Record1.java", "TableX.java", "TableCache.java",
          "Collections/LogCollection.java", "Collections/LogMap.java", "Collections/LogMapDelta.java",
          "Collections/PMap1.java")]
results = []
for name in ("Zeze.Transaction.TestFormalRollback", "Zeze.TestFastAdmissionGuard"):
    path = ROOT/"build"/f"TEST-{name}.xml"
    suite = ET.parse(path).getroot()
    result = dict(suite.attrib)
    result["methods"] = [dict(c.attrib) for c in suite.findall("testcase")]
    result["xml_sha256"] = hashlib.sha256(path.read_bytes()).hexdigest()
    results.append(result)
    if any(int(result[k]) for k in ("failures", "errors", "skipped")):
        raise SystemExit("Java evidence contains failure or skipped tests")
data = dict(revision=subprocess.check_output(["git","rev-parse","HEAD"],cwd=REPO,text=True).strip(),
            sources_sha256={str(p.relative_to(REPO)):hashlib.sha256(p.read_bytes()).hexdigest() for p in paths},
            results=results, command="powershell.exe -NoProfile -ExecutionPolicy Bypass -File ZezeJava/formal/rollback/run-java.ps1")
for target in (ROOT, ROOT.parent/"record-lifecycle"):
    (target/"java-verification.json").write_text(json.dumps(data,indent=2),encoding="utf-8")
print("Archived Java evidence:", [(r["name"],r["tests"]) for r in results])
