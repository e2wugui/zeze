#!/bin/bash
# 四车道压测浸泡（机器外部负载批次）：test → integrationTest → extraTest → bench
# 用法：./test_soak.sh [起始轮] [总轮数]（默认 1 10）；批次结果落仓库根 soak-<日期>-<时分>/
# 批次方法论双保险：--continue（前车道红不掐死后车道）+ --rerun-tasks（Test 任务增量 UP-TO-DATE 假绿，test20-20261014 实锤）
# 来源于 test5-20261015.tmp 收编（2026-10-16）：轮数/批次目录参数化，XML 汇总路径去机器硬编码。
set -u
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT/ZezeJava" || exit 1
BATCH="$ROOT/soak-$(date +%Y%m%d-%H%M)"
mkdir -p "$BATCH"
XMLBASE="$ROOT/ZezeJava/ZezeJavaTest/build/test-results"

summarize() { # $1=task dir name -> "tests/failures"
  TASK_DIR="$1" XMLBASE="$XMLBASE" python - <<'EOF'
import sys, os, glob, re
base, d = os.environ['XMLBASE'], os.environ['TASK_DIR']
tests = fails = 0
for f in glob.glob(os.path.join(base, d, 'TEST-*.xml')):
    head = open(f, encoding='utf-8', errors='replace').read(2000)
    m = re.search(r'tests="(\d+)"', head)
    n = re.search(r'(?:failures|errors)="(\d+)"', head)
    if m:
        tests += int(m.group(1))
        fails += int(n.group(1)) if n else 0
print(f"{tests}/{fails}f")
EOF
}

START=${1:-1}
ROUNDS=${2:-10}
for i in $(seq "$START" "$ROUNDS"); do
  echo "=== round $i start $(date '+%Y-%m-%d %H:%M:%S') ===" >> "$BATCH/master.log"
  ./gradlew :ZezeJavaTest:test :ZezeJavaTest:integrationTest :ZezeJavaTest:extraTest :ZezeJavaTest:bench --continue --rerun-tasks --console=plain > "$BATCH/round$i.log" 2>&1
  rc=$?
  line="round$i: test=$(summarize test) integrationTest=$(summarize integrationTest) extraTest=$(summarize extraTest) bench=$(summarize bench) rc=$rc"
  echo "$line" >> "$BATCH/master.log"
  echo "=== round $i end $(date '+%Y-%m-%d %H:%M:%S') ===" >> "$BATCH/master.log"
done
echo "batch done $(date '+%Y-%m-%d %H:%M:%S')" >> "$BATCH/master.log"
