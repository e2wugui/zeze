"""Run pinned TLC checks, keeping logs and counterexamples in build/."""

import argparse
from datetime import datetime, timezone
import hashlib
import json
import re
import subprocess
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent
JAR = ROOT / "build/tools/tla2tools.jar"
JAR_SHA256 = "936a262061c914694dfd669a543be24573c45d5aa0ff20a8b96b23d01e050e88"


def check(name, overrides=None, expected=None, witness=None, java="java", timeout=300,
          selected=False):
    target = ROOT / "build" / ("selected" if selected else "") / name
    target.mkdir(parents=True, exist_ok=True)
    config = (ROOT / "LostUpdate.cfg").read_text(encoding="utf-8")
    for key, value in (overrides or {}).items():
        config, count = re.subn(rf"(?m)^(\s*{key}\s+(?:=|<-)\s+)\S+$",
                                lambda match: match[1] + str(value), config)
        if count != 1:
            raise ValueError(f"Expected one configuration entry for {key}")
    if witness:
        config = config.replace("INVARIANTS ", f"INVARIANTS {witness} ")
        expected = witness
    config_path = target / "model.cfg"
    config_path.write_text(config, encoding="utf-8")
    command = [java, "-Xmx1g", "-XX:+UseParallelGC", "-cp", str(JAR),
               "tlc2.TLC", "-workers", "1", "-seed", "1", "-fp", "0",
               "-config", str(config_path), "-metadir", str(target / "states"), "Models"]
    started = time.monotonic()
    try:
        result = subprocess.run(command, cwd=ROOT, text=True, encoding="utf-8",
                                errors="replace", stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT, timeout=timeout)
        output, exit_code = result.stdout, result.returncode
    except subprocess.TimeoutExpired as error:
        output = error.stdout or b""
        if isinstance(output, bytes):
            output = output.decode("utf-8", errors="replace")
        output += f"\nTIMEOUT after {timeout}s: check is incomplete.\n"
        exit_code = None
    (target / "tlc.log").write_text(output, encoding="utf-8")
    if expected:
        passed = (exit_code == 12 and
                  f"Invariant {expected} is violated." in output)
    else:
        passed = (exit_code == 0 and
                  "Model checking completed. No error has been found." in output)
    counts = re.findall(r"([\d,]+) states generated, ([\d,]+) distinct states found", output)
    summary = {"name": name, "passed": passed, "exit_code": exit_code,
               "expected_violation": expected, "seconds": round(time.monotonic() - started, 2),
               "generated": int(counts[-1][0].replace(",", "")) if counts else None,
               "distinct": int(counts[-1][1].replace(",", "")) if counts else None,
               "log": (target / "tlc.log").relative_to(ROOT).as_posix(),
               "config_sha256": hashlib.sha256(config.encode("utf-8")).hexdigest()}
    print(json.dumps(summary, ensure_ascii=False), flush=True)
    if not passed:
        print(output[-10000:], flush=True)
    return summary


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java", default="java")
    parser.add_argument("--case", action="append", help="Run only named cases (repeatable)")
    parser.add_argument("--timeout", type=int, default=300, help="Seconds allowed per TLC process")
    args = parser.parse_args()
    if not JAR.exists() or hashlib.sha256(JAR.read_bytes()).hexdigest() != JAR_SHA256:
        raise SystemExit("Missing or incorrect TLC 1.7.4 jar; see README.md")
    if args.timeout <= 0:
        parser.error("--timeout must be positive")
    metadata = {
        "started_utc": datetime.now(timezone.utc).isoformat(),
        "revision": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT,
                                            text=True).strip(),
        "java": subprocess.check_output([args.java, "-version"], stderr=subprocess.STDOUT,
                                        text=True).strip(),
        "tlc_sha256": JAR_SHA256,
        "sources": {path.name: hashlib.sha256(path.read_bytes()).hexdigest()
                    for path in (ROOT / "Serializability.tla", ROOT / "Models.tla",
                                 ROOT / "LostUpdate.cfg", Path(__file__))},
    }
    cases = [
        ("early-unlock", {"HoldLocksUntilCommit": "FALSE"}, "CommitOrderSerializable"),
        ("skip-read-validation", {"Programs": "WriteSkew", "KeyCount": 2,
                                  "CheckReadVersions": "FALSE"}, "CommitOrderSerializable"),
        ("version-before-value", {"Programs": "RepeatedRead", "PublishValuesFirst": "FALSE"},
         "CommitOrderSerializable"),
        ("lost-update", {}),
        ("write-skew", {"Programs": "WriteSkew", "KeyCount": 2}),
        ("fractured-read", {"Programs": "FracturedRead", "KeyCount": 2}),
        ("repeated-read", {"Programs": "RepeatedRead"}),
        ("own-writes", {"Programs": "OwnWrites"}),
        ("missing-insert", {"Programs": "MissingInsert", "InitialData": "Absent"}),
        ("delete-reinsert", {"Programs": "DeleteReinsert", "InitialData": "Ones", "TxnCount": 3}),
        ("lock-upgrade", {"Programs": "LockUpgrade", "KeyCount": 2}),
        ("lock-insert", {"Programs": "LockInsert", "KeyCount": 2}),
        ("three-way-cycle", {"Programs": "ThreeWayCycle", "KeyCount": 3,
                              "TxnCount": 3, "MaxAttempts": 1}),
        ("generic-programs", {"Programs": "GenericPrograms", "KeyCount": 2,
                              "ExplorePrograms": "TRUE"}),
        ("witness-all-committed", {}, None, "NeverAllCommitted"),
        ("witness-retained", {}, None, "NeverRetained"),
        ("witness-released", {}, None, "NeverReleased"),
        ("witness-upgrade", {"Programs": "LockUpgrade", "KeyCount": 2}, None, "NeverUpgraded"),
        ("witness-insert", {"Programs": "LockInsert", "KeyCount": 2}, None, "NeverInsertedBeforeHeld"),
    ]
    if args.case:
        unknown = set(args.case) - {case[0] for case in cases}
        if unknown:
            parser.error(f"Unknown cases: {sorted(unknown)}")
        cases = [case for case in cases if case[0] in args.case]
    results = [check(*case, java=args.java, timeout=args.timeout, selected=bool(args.case))
               for case in cases]
    name = "selected-results" if args.case else "results"
    (ROOT / f"build/{name}.json").write_text(json.dumps(results, indent=2), encoding="utf-8")
    (ROOT / f"build/{name}-metadata.json").write_text(json.dumps(metadata, indent=2), encoding="utf-8")
    raise SystemExit(0 if all(result["passed"] for result in results) else 1)


if __name__ == "__main__":
    main()
