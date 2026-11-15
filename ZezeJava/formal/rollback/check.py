"""Pinned, complete finite TLC checks; negative controls must return invariant traces."""
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
JAR = ROOT.parent / "serializability/build/tools/tla2tools.jar"
PIN = "936a262061c914694dfd669a543be24573c45d5aa0ff20a8b96b23d01e050e88"


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--java", default="java")
    p.add_argument("--timeout", type=int, default=180)
    args = p.parse_args()
    if hashlib.sha256(JAR.read_bytes()).hexdigest() != PIN:
        raise SystemExit("Wrong TLC jar")
    is_rollback = ROOT.name == "rollback"
    model = "Rollback" if is_rollback else "Lifecycle"
    if is_rollback:
        cases = [(name, {"Programs": program}, None) for name, program in [
            ("nested-rollback", "NestedUndoPrograms"), ("nested-merge", "NestedMergePrograms"),
            ("replace-rollback", "ReplaceUndoPrograms"), ("replace-merge", "ReplaceMergePrograms"),
            ("top-abort", "TopAbortPrograms"), ("retry-clear", "RetryPrograms"),
            ("two-writers", "TwoWritersPrograms"), ("replace-writers", "ReplaceWritersPrograms"),
            ("deep-rollback", "DeepUndoPrograms")]]
        cases += [("negative-"+fault, {"Programs": program, "Fault": '"'+fault+'"'},
                   "CommitOrderSerializable") for fault, program in [
            ("skip-undo", "NestedUndoPrograms"), ("bad-merge", "NestedMergePrograms"),
            ("abort-leak", "TopAbortPrograms"), ("reuse-logs", "RetryPrograms")]]
        cases += [("witness-"+name, {"Programs": program}, inv) for name, program, inv in [
            ("nested-rollback", "NestedUndoPrograms", "NeverRolled"),
            ("replace", "ReplaceMergePrograms", "NeverReplaced"),
            ("retry", "RetryPrograms", "NeverRetried"),
            ("commit", "NestedUndoPrograms", "NeverAllCommitted")]]
        cases += [("map-persistent-snapshots", {"__model": "MapSavepoints"}, None),
                  ("negative-map-alias", {"__model": "MapSavepoints", "Fault": '"mutable-alias"'},
                   "CommitOrderSerializable"),
                  ("witness-map-rollback", {"__model": "MapSavepoints"}, "NeverRolled")]
    else:
        cases = [(event, {"Event": '"'+event+'"'}, None) for event in ("evict", "invalid", "delete")]
        cases += [("negative-"+fault, {"Event": '"'+event+'"', "Fault": '"'+fault+'"'},
                   "CommitOrderSerializable") for event, fault in [
            ("evict", "validate-current"), ("evict", "ignore-state"), ("invalid", "reuse-version")]]
        cases += [("witness-"+event+"-retry", {"Event": '"'+event+'"'}, "NeverRetried")
                  for event in ("evict", "invalid", "delete")]
        cases += [("witness-reload", {}, "NeverReloaded"), ("witness-write-guard", {}, "NeverStaleBlocked")]
    results = []
    for name, overrides, expected in cases:
        case_model = overrides.pop("__model", model)
        target = ROOT / "build" / name
        target.mkdir(parents=True, exist_ok=True)
        cfg = (ROOT / (case_model+".cfg")).read_text(encoding="utf-8")
        for key, value in overrides.items():
            cfg, count = re.subn(rf"(?m)^(\s*{key}\s+(?:=|<-)\s+)\S+$",
                                lambda m: m[1]+value, cfg)
            if count != 1:
                raise ValueError(key)
        if expected and expected != "CommitOrderSerializable":
            cfg = cfg.replace("INVARIANT ", "INVARIANT "+expected+" ")
        (target / "model.cfg").write_text(cfg, encoding="utf-8")
        command = [args.java, "-Xmx512m", "-XX:+UseParallelGC", "-cp", str(JAR), "tlc2.TLC",
                   "-workers", "1", "-seed", "1", "-fp", "0", "-config", str(target/"model.cfg"),
                   "-metadir", str(target/"states"), case_model]
        started = time.monotonic()
        try:
            run = subprocess.run(command, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                 text=True, encoding="utf-8", errors="replace", timeout=args.timeout)
            output, code = run.stdout, run.returncode
        except subprocess.TimeoutExpired as e:
            output = e.stdout or b""
            if isinstance(output, bytes):
                output = output.decode("utf-8", errors="replace")
            output += "\nTIMEOUT: INCOMPLETE\n"
            code = None
        (target/"tlc.log").write_text(output, encoding="utf-8")
        if expected:
            (ROOT/"traces").mkdir(exist_ok=True)
            (ROOT/"traces"/(name+".txt")).write_text(output, encoding="utf-8")
        ok = (code == 12 and f"Invariant {expected} is violated." in output) if expected else (
            code == 0 and "Model checking completed. No error has been found." in output)
        counts = re.findall(r"([\d,]+) states generated, ([\d,]+) distinct states found", output)
        item = dict(name=name, passed=ok, exit_code=code, expected_violation=expected,
                    seconds=round(time.monotonic()-started, 2),
                    generated=int(counts[-1][0].replace(",", "")) if counts else None,
                    distinct=int(counts[-1][1].replace(",", "")) if counts else None,
                    log=str((target/"tlc.log").relative_to(ROOT)),
                    config_sha256=hashlib.sha256(cfg.encode()).hexdigest())
        results.append(item)
        print(json.dumps(item), flush=True)
        if not ok:
            print(output[-5000:], flush=True)
    paths = list(ROOT.glob("*.tla"))+list(ROOT.glob("*.cfg"))+[Path(__file__)]
    src = REPO / "ZezeJava/ZezeJava/src/main/java/Zeze/Transaction"
    paths += [src/name for name in ("Transaction.java", "Savepoint.java", "TransactionLog.java",
              "RecordAccessed.java", "Record.java", "Record1.java", "TableX.java", "TableCache.java")]
    paths += [REPO / "ZezeJava/ZezeJavaTest/src/Zeze/Transaction/TestFormalRollback.java"]
    metadata = dict(utc=datetime.now(timezone.utc).isoformat(), tlc_sha256=PIN,
        revision=subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=REPO, text=True).strip(),
        java=subprocess.check_output([args.java,"-version"], stderr=subprocess.STDOUT,text=True).strip(),
        sha256={str(path.relative_to(REPO)):hashlib.sha256(path.read_bytes()).hexdigest() for path in paths})
    (ROOT/"verification.json").write_text(json.dumps(dict(metadata=metadata,results=results),indent=2),encoding="utf-8")
    raise SystemExit(0 if all(r["passed"] for r in results) else 1)


if __name__ == "__main__":
    main()
