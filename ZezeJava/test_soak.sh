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

# 预检：固定测试端口段 318xx 被占 = 上批孤儿测试 JVM（死批 daemon 挂死后 worker 无人收尸，
# 抓着监听端口+.pal/.raft 句柄，导致全批确定性 bind/delete 红——soak-20261018-1737 批 20×12f 实锤）。
# 只杀持有端口的 java 进程（taskkill /T 连同其子进程），清不干净则中止。
zombie_ports=$(netstat -ano | grep -E 'TCP +127\.0\.0\.1:318[0-9][0-9] .*LISTENING' || true)
if [ -n "$zombie_ports" ]; then
  echo "WARN: 318xx 端口段被占用，疑似上批孤儿测试 JVM："
  echo "$zombie_ports"
  for pid in $(echo "$zombie_ports" | awk '{print $NF}' | sort -u); do
    if tasklist //FI "PID eq $pid" 2>/dev/null | grep -qi 'java.exe'; then
      echo "WARN: 杀孤儿 java PID $pid（注意：若其父构建仍挂死，会因 --continue 复活续跑，须连脚本/launcher/daemon 一并清理）"
      taskkill //T //F //PID "$pid" 2>/dev/null || true
    else
      echo "ABORT: 端口被非 java 进程占用 PID $pid，不敢自动清理" >&2
      exit 1
    fi
  done
  sleep 2
  if netstat -ano | grep -qE 'TCP +127\.0\.0\.1:318[0-9][0-9] .*LISTENING'; then
    echo "ABORT: 清理后 318xx 仍被占用，退出" >&2
    exit 1
  fi
fi
for i in $(seq "$START" "$ROUNDS"); do
  echo "=== round $i start $(date '+%Y-%m-%d %H:%M:%S') ===" >> "$BATCH/master.log"
  ./gradlew :ZezeJavaTest:test :ZezeJavaTest:integrationTest :ZezeJavaTest:extraTest :ZezeJavaTest:bench --continue --rerun-tasks --console=plain > "$BATCH/round$i.log" 2>&1
  rc=$?
  line="round$i: test=$(summarize test) integrationTest=$(summarize integrationTest) extraTest=$(summarize extraTest) bench=$(summarize bench) rc=$rc"
  echo "$line" >> "$BATCH/master.log"
  echo "=== round $i end $(date '+%Y-%m-%d %H:%M:%S') ===" >> "$BATCH/master.log"
done
echo "batch done $(date '+%Y-%m-%d %H:%M:%S')" >> "$BATCH/master.log"
