#!/usr/bin/env python3
"""Compare JMH throughput and B/op by benchmark plus the complete parameter set.

Usage:
    python scripts/compare_jmh.py before.json after.json --output comparison.json
    python scripts/compare_jmh.py before.json after.json --expect-map-grid

Each input may also be a directory of disjoint jmh-*.json files. Duplicate rows
are errors: compare independent A/B repetitions separately, rather than silently
pooling measurements or cross-pairing parameter rows. Requires Python 3.10+ only.

Exit codes: 0 = all metric CIs overlap or favor after; 1 = at least one adverse
non-overlapping CI; 2 = invalid/mismatched input; 3 = no adverse separation but at
least one unavailable CI. Exit 0 is NOT proof of equivalence or no regression.
The CI separation rule is descriptive, not a paired hypothesis/equivalence test.
No improvement in one parameter row can cancel a regression in another row.
"""

from __future__ import annotations

import argparse
import itertools
import json
import math
import sys
from pathlib import Path
from typing import Any


class ComparisonError(ValueError):
    pass


# Do not compare different load/profiling/JVM configurations as a code-only A/B.
CONFIG_FIELDS = (
    "jmhVersion", "jdkVersion", "vmName", "vmVersion", "jvmArgs", "threads",
    "forks", "warmupIterations", "warmupTime", "warmupBatchSize",
    "measurementIterations", "measurementTime", "measurementBatchSize",
)
METRICS = (("throughput", True), ("gc.alloc.rate.norm", False))
RowKey = tuple[str, tuple[tuple[str, str], ...]]


def row_key(row: dict[str, Any], source: str) -> RowKey:
    benchmark = row.get("benchmark")
    params = row.get("params", {})
    if not isinstance(benchmark, str) or not benchmark:
        raise ComparisonError(f"{source}: missing benchmark name")
    if not isinstance(params, dict) or any(
        not isinstance(k, str) or not isinstance(v, str) for k, v in params.items()
    ):
        raise ComparisonError(f"{source}: params must contain string names and values")
    return benchmark, tuple(sorted(params.items()))


def label(key: RowKey) -> str:
    benchmark, params = key
    return benchmark + " [" + ", ".join(f"{k}={v}" for k, v in params) + "]"


def load_rows(path: Path) -> dict[RowKey, dict[str, Any]]:
    files = sorted(path.glob("jmh-*.json")) if path.is_dir() else [path]
    if not files:
        raise ComparisonError(f"{path}: no jmh-*.json files")
    rows: dict[RowKey, dict[str, Any]] = {}
    for file in files:
        try:
            data = json.loads(file.read_text(encoding="utf-8-sig"))
        except (OSError, ValueError) as ex:
            raise ComparisonError(f"{file}: {ex}") from ex
        if not isinstance(data, list) or not data:
            raise ComparisonError(f"{file}: expected a nonempty JMH result array")
        for index, row in enumerate(data):
            source = f"{file}, row {index + 1}"
            if not isinstance(row, dict):
                raise ComparisonError(f"{source}: expected an object")
            key = row_key(row, source)
            if key in rows:
                raise ComparisonError(f"{source}: duplicate parameter row: {label(key)}")
            if row.get("mode") != "thrpt":
                raise ComparisonError(f"{source}: expected mode=thrpt, got {row.get('mode')!r}")
            rows[key] = row
    return rows


def require_map_grid(rows: dict[RowKey, dict[str, Any]], side: str) -> None:
    """Reject jointly missing rows in the default MapBeanCommitBench matrix."""
    expected = set()
    for family, operation, entries, consumers in itertools.product(
        ("MAP2", "SORTED_MAP2"), ("EDIT", "REPLACE"), ("16", "1024"),
        ("NONE", "HISTORY_CAPTURE", "LISTENER", "BOTH"),
    ):
        params = {"changeCount": "1", "family": family, "operation": operation,
                  "entries": entries, "consumers": consumers}
        expected.add(("Benchmark.MapBeanCommitBench.commit", tuple(sorted(params.items()))))
    missing = sorted(expected - rows.keys())
    extra = sorted(rows.keys() - expected)
    if missing or extra:
        details = [f"{side}: expected exact 32-row MapBeanCommitBench grid; "
                   f"missing={len(missing)}, extra={len(extra)}"]
        details.extend("missing: " + label(key) for key in missing)
        details.extend("unexpected: " + label(key) for key in extra)
        raise ComparisonError("\n".join(details))


def numeric(value: Any, context: str, *, allow_nonfinite: bool = False) -> float:
    if isinstance(value, bool):
        raise ComparisonError(f"{context}: expected a number, got bool")
    try:
        result = float(value)
    except (TypeError, ValueError) as ex:
        raise ComparisonError(f"{context}: invalid number {value!r}") from ex
    if not allow_nonfinite and not math.isfinite(result):
        raise ComparisonError(f"{context}: nonfinite score {value!r}")
    return result


def read_metric(row: dict[str, Any], name: str, context: str) -> dict[str, Any]:
    if name == "throughput":
        metric = row.get("primaryMetric")
    else:
        secondary = row.get("secondaryMetrics")
        if not isinstance(secondary, dict):
            raise ComparisonError(f"{context}: missing secondaryMetrics; run JMH with -prof gc")
        metric = secondary.get(name)
    if not isinstance(metric, dict):
        raise ComparisonError(f"{context}: missing metric {name}; run JMH with -prof gc")
    score = numeric(metric.get("score"), f"{context}, {name} score")
    if score < 0:
        raise ComparisonError(f"{context}: negative {name} score")
    unit = metric.get("scoreUnit")
    if not isinstance(unit, str) or not unit:
        raise ComparisonError(f"{context}: missing {name} scoreUnit")
    if name == "gc.alloc.rate.norm" and unit != "B/op":
        raise ComparisonError(f"{context}: expected B/op for {name}, got {unit!r}")
    bounds = metric.get("scoreConfidence")
    if not isinstance(bounds, list) or len(bounds) != 2:
        raise ComparisonError(f"{context}: missing two-element {name} scoreConfidence")
    low, high = (numeric(v, f"{context}, {name} CI", allow_nonfinite=True) for v in bounds)
    ci = None
    if math.isfinite(low) and math.isfinite(high):
        if low > high:
            raise ComparisonError(f"{context}: reversed {name} confidence interval")
        if not low <= score <= high:
            raise ComparisonError(f"{context}: {name} score {score} lies outside its confidence interval "
                                  f"[{low}, {high}]")
        ci = [low, high]
    return {"score": score, "confidence": ci, "unit": unit}


def compare_metric(before: dict[str, Any], after: dict[str, Any], higher_better: bool) -> dict[str, Any]:
    if before["unit"] != after["unit"]:
        raise ComparisonError(f"metric units differ: {before['unit']!r} vs {after['unit']!r}")
    a, b = before["score"], after["score"]
    percent = 100 * (b - a) / a if a != 0 else (0.0 if b == 0 else None)
    ca, cb = before["confidence"], after["confidence"]
    if ca is None or cb is None:
        verdict = "NO_FINITE_CI"
    elif cb[0] > ca[1]:
        verdict = "CI_SEPARATED_BETTER" if higher_better else "CI_SEPARATED_WORSE"
    elif cb[1] < ca[0]:
        verdict = "CI_SEPARATED_WORSE" if higher_better else "CI_SEPARATED_BETTER"
    else:
        direction = (b - a) * (1 if higher_better else -1)
        verdict = "CI_OVERLAP_WORSE" if direction < 0 else (
            "CI_OVERLAP_BETTER" if direction > 0 else "CI_OVERLAP"
        )
    return {"before": before, "after": after, "change_percent": percent, "verdict": verdict}


def compare(before: dict[RowKey, dict[str, Any]], after: dict[RowKey, dict[str, Any]]) -> dict[str, Any]:
    missing_after = sorted(before.keys() - after.keys())
    missing_before = sorted(after.keys() - before.keys())
    if missing_after or missing_before:
        details = ["parameter row sets differ; no unmatched rows are silently ignored"]
        details.extend("missing after: " + label(key) for key in missing_after)
        details.extend("missing before: " + label(key) for key in missing_before)
        raise ComparisonError("\n".join(details))
    if not before:
        raise ComparisonError("no rows to compare")

    results = []
    worse = unavailable = overlap_worse = 0
    for key in sorted(before):
        a, b = before[key], after[key]
        for field in CONFIG_FIELDS:
            for side, row in (("before", a), ("after", b)):
                if field not in row or row[field] is None:
                    raise ComparisonError(f"{side} {label(key)}: missing nonnull configuration {field}")
            if a[field] != b[field]:
                raise ComparisonError(f"{label(key)}: configuration {field} differs: "
                                      f"{a[field]!r} vs {b[field]!r}")
        metrics = {}
        for name, higher_better in METRICS:
            try:
                result = compare_metric(read_metric(a, name, "before " + label(key)),
                                        read_metric(b, name, "after " + label(key)), higher_better)
            except ComparisonError as ex:
                raise ComparisonError(f"{label(key)}: {ex}") from ex
            metrics[name] = result
            worse += result["verdict"] == "CI_SEPARATED_WORSE"
            unavailable += result["verdict"] == "NO_FINITE_CI"
            overlap_worse += result["verdict"] == "CI_OVERLAP_WORSE"
        results.append({"benchmark": key[0], "params": dict(key[1]), "metrics": metrics})
    return {
        "rows": results,
        "summary": {"paired_rows": len(results), "adverse_ci_separations": worse,
                    "overlapping_ci_worse_trends": overlap_worse, "unavailable_ci_metrics": unavailable},
        "interpretation": "CI overlap does not prove equivalence or absence of regression. "
                          "CI separation is descriptive; repeat adverse rows independently. "
                          "Rows and metrics are assessed individually; no cross-row offsets.",
    }


def fmt_metric(metric: dict[str, Any]) -> str:
    bounds = metric["confidence"]
    ci = "unavailable" if bounds is None else f"[{bounds[0]:.6g}, {bounds[1]:.6g}]"
    return f"mean={metric['score']:.6g} CI={ci} {metric['unit']}"


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("before", type=Path)
    parser.add_argument("after", type=Path)
    parser.add_argument("--output", type=Path, help="also write the complete machine-readable JSON report")
    parser.add_argument("--expect-map-grid", action="store_true",
                        help="require the exact default 32-row MapBeanCommitBench matrix on both sides")
    args = parser.parse_args(argv)
    try:
        before, after = load_rows(args.before), load_rows(args.after)
        if args.expect_map_grid:
            require_map_grid(before, "before")
            require_map_grid(after, "after")
        report = compare(before, after)
        report["expected_map_grid"] = args.expect_map_grid
        if args.output:
            args.output.parent.mkdir(parents=True, exist_ok=True)
            args.output.write_text(json.dumps(report, indent=2, ensure_ascii=False, allow_nan=False) + "\n",
                                   encoding="utf-8")
    except (ComparisonError, OSError) as ex:
        print(f"ERROR: {ex}", file=sys.stderr)
        return 2

    print(f"Before: {args.before}\nAfter:  {args.after}")
    for row in report["rows"]:
        print("\n" + label(row_key(row, "report")))
        for name, result in row["metrics"].items():
            change = result["change_percent"]
            percent = "undefined (zero baseline)" if change is None else f"{change:+.3f}%"
            print(f"  {name}: {fmt_metric(result['before'])} -> {fmt_metric(result['after'])}; "
                  f"{percent}; {result['verdict']}")
    summary = report["summary"]
    print("\n" + json.dumps(summary, ensure_ascii=False))
    print(report["interpretation"])
    if summary["adverse_ci_separations"]:
        return 1
    return 3 if summary["unavailable_ci_metrics"] else 0


if __name__ == "__main__":
    sys.exit(main())
