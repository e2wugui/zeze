package Zeze.Util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.LongAdder;

import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Transaction.TableKey;
import org.jspecify.annotations.Nullable;

/** 性能计数器：按 protocol/procedure/table/runkey 维度聚合统计并周期输出日志。 */
public final class PerfCounter extends FastLock implements ZezeCounter {
	public static class LongAdderCounter extends LongAdder implements LongCounter {
		@Override
		public void inc(long v) {
			add(v);
		}
	}

	private static final LongObserver dummyLongObserver = v -> {
	};

	private static class RunInfo {
		static final int MAX_IDLE_COUNT = 10; // 最多几轮没有收集到信息就自动清除该条目

		final String name;
		final LongAdder procCount = new LongAdder(); // 处理次数
		final LongAdder procTime = new LongAdder(); // 处理时间(ns)
		long lastProcCount;
		long lastProcTime;
		int idleCount; // 没收集到信息的轮数

		RunInfo(String name) {
			this.name = name;
		}
	}

	private static final class RunInfoWithSerial extends RunInfo {
		volatile int serial;

		RunInfoWithSerial(String name, int serial) {
			super(name);
			this.serial = serial;
		}
	}

	private static final class ProtocolInfo extends RunInfo {
		final LongAdder recvSize = new LongAdder(); // 接收字节
		final LongAdder sendCount = new LongAdder(); // 发送次数
		final LongAdder sendSize = new LongAdder(); // 发送字节
		long lastRecvSize;
		long lastSendCount;
		long lastSendSize;

		ProtocolInfo(String name) {
			super(name);
		}
	}

	/**
	 * 在Procedure中统计，由于嵌套存储过程存在，总数会比实际事务数多。
	 * 一般嵌套存储过程很少用，事务数量也可以参考这里的数值，不单独统计。
	 * 另外Transaction在重做时会在这里保存重做次数的统计。通过name和存储过程区分开来。
	 */
	public static final class ProcedureInfo {
		static final int MAX_IDLE_COUNT = 10; // 最多几轮没有收集到信息就自动清除该条目

		final String name;
		// 热路径无锁读写、getLogAndReset 持锁换新，需 volatile 发布
		volatile LongConcurrentHashMap<LongAdder> resultMap = new LongConcurrentHashMap<>();
		// 锁内替换、外部查询与 Watcher 无锁读，需 volatile 发布
		volatile LongConcurrentHashMap<LongAdder> resultMapLast = new LongConcurrentHashMap<>();
		long totalCount;
		int succRatio; // 成功率百分比
		int idleCount; // 没收集到信息的轮数

		ProcedureInfo(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public LongConcurrentHashMap<LongAdder> getResultMapLast() {
			return resultMapLast;
		}

		/** 超出基数封顶的错误码的丢弃目标：自增不产生任何可见统计 */
		private static final LongAdder DISCARDED_RESULT = new LongAdder();

		private final ResultCodeCap codeCap = new ResultCodeCap();

		public LongAdder getOrAddResult(long resultCode) {
			if (!codeCap.accept(resultCode))
				return DISCARDED_RESULT;
			//noinspection DataFlowIssue
			return resultMap.computeIfAbsent(resultCode, __ -> new LongAdder());
		}

		public String toString(boolean last) {
			var sb = new StringBuilder();
			sb.append(name).append(':').append(succRatio).append('%');
			for (var it = (last ? resultMapLast : resultMap).entryIterator(); it.moveToNext(); ) {
				//noinspection DataFlowIssue
				sb.append(", ").append(it.key()).append(':').append(it.value().sum());
			}
			return sb.toString();
		}

		@Override
		public String toString() {
			return toString(true);
		}
	}

	public static final class TableInfo {
		private final String tableName;
		private final LongAdderCounter[] counters; // 按TableMetric.ordinal索引
		private final long[] lastCounts; // 按TableMetric.ordinal索引，checkpointAndReset时更新

		TableInfo(String tableName) {
			this.tableName = tableName;
			var metrics = TableMetric.values();
			counters = new LongAdderCounter[metrics.length];
			for (var i = 0; i < counters.length; i++)
				counters[i] = new LongAdderCounter();
			lastCounts = new long[metrics.length];
		}

		public LongAdderCounter counter(TableMetric metric) {
			return counters[metric.ordinal()];
		}

		long lastLockCount() {
			return lastCounts[TableMetric.READ_LOCK.ordinal()] + lastCounts[TableMetric.WRITE_LOCK.ordinal()];
		}

		boolean checkpointAndReset() {
			var active = false;
			for (var metric : TableMetric.values()) {
				var count = counters[metric.ordinal()].sumThenReset();
				lastCounts[metric.ordinal()] = count;
				active |= count != 0;
			}
			return active;
		}

		/** 就地清零计数并保留句柄身份，避免外部缓存的 LongAdderCounter 脱离收集。 */
		void reset() {
			for (var metric : TableMetric.values()) {
				counters[metric.ordinal()].reset();
				lastCounts[metric.ordinal()] = 0;
			}
		}

		Map<String, Long> snapshotResult() {
			var m = new LinkedHashMap<String, Long>(lastCounts.length * 2);
			for (var metric : TableMetric.values())
				m.put(metric.key, lastCounts[metric.ordinal()]);
			return m;
		}

		private long last(TableMetric metric) {
			return lastCounts[metric.ordinal()];
		}

		public static String getLogTitle() {
			return String.format("%-60s CacheHit AcqShrHit AcqModHit AcqShrCnt AcqModCnt AcqInvCnt ReduceCnt" +
				" CacGetCnt StoGetCnt LockCount  ReadLock WriteLock TryRdLock TryWtLock RedoCount", "TableName");
		}

		@Override
		public String toString() {
			long readLockCount = last(TableMetric.READ_LOCK);
			long writeLockCount = last(TableMetric.WRITE_LOCK);
			long lockCount = readLockCount + writeLockCount;
			long getCount = last(TableMetric.CACHE_GET) + last(TableMetric.STORAGE_GET);
			float cacheHit = getCount != 0 ? last(TableMetric.CACHE_GET) * 100.0f / getCount : 0;
			float acquireShareHit = lockCount != 0 ? (lockCount - last(TableMetric.ACQUIRE_SHARE)) * 100.0f / lockCount : 0;
			float acquireModifyHit = lockCount != 0 ? (lockCount - last(TableMetric.ACQUIRE_MODIFY)) * 100.0f / lockCount : 0;
			return String.format("%-60s%8.2f%%%9.2f%%%9.2f%%%10d%10d%10d%10d%10d%10d%10d%10d%10d%10d%10d%10d", tableName,
				cacheHit, acquireShareHit, acquireModifyHit,
				last(TableMetric.ACQUIRE_SHARE), last(TableMetric.ACQUIRE_MODIFY), last(TableMetric.ACQUIRE_INVALID),
				last(TableMetric.REDUCE_INVALID), last(TableMetric.CACHE_GET), last(TableMetric.STORAGE_GET),
				lockCount, readLockCount, writeLockCount, last(TableMetric.TRY_READ_LOCK),
				last(TableMetric.TRY_WRITE_LOCK), last(TableMetric.REDO));
		}
	}

	private static final class CountInfo extends LongAdder implements LongCounter {
		final String name;
		final boolean accumulate;
		long lastCount;

		CountInfo(String name, boolean accumulate) {
			this.name = name;
			this.accumulate = accumulate;
		}

		@Override
		public void inc(long v) {
			add(v);
		}
	}

	public static final int PERF_COUNT = Integer.parseInt(System.getProperty("perfCount", "20")); // 输出条目数
	public static final int PERF_PERIOD = Integer.parseInt(System.getProperty("perfPeriod", "100")); // 输出周期(秒)
	private final ConcurrentHashMap<String, RunInfoWithSerial> runInfoMap = new ConcurrentHashMap<>(); // key: 统计名（归一化后）
	private final LongConcurrentHashMap<ProtocolInfo> protocolInfoMap = new LongConcurrentHashMap<>(); // key: typeId
	private final ConcurrentHashMap<String, ProcedureInfo> procedureInfoMap = new ConcurrentHashMap<>(); // key: procedureName
	// 按 name 缓存共享句柄，避免热路径重复创建并摊销代际重绑
	private final ConcurrentHashMap<String, ProcedureCounter> procedureCounterMap = new ConcurrentHashMap<>();
	// 按名称的单调累计执行数（跨收集周期不轮换），供速率差分使用
	private final ConcurrentHashMap<String, LongAdder> procedureTotalMap = new ConcurrentHashMap<>(); // key: procedureName
	private final LongConcurrentHashMap<TableInfo> tableInfoMap = new LongConcurrentHashMap<>(); // key: tableId
	private CountInfo[] countInfos = new CountInfo[0];
	// exclude 随时可配置；已存在的统计条目要等空闲回收才会消失，并发读写安全
	private final Set<String> excludeRunKeys = ConcurrentHashMap.newKeySet(); // value: 统计名（归一化后）
	private final LongConcurrentHashMap<Boolean> excludeProtocolTypeIds = new LongConcurrentHashMap<>(); // key: typeId
	private final DecimalFormat numFormatter = new DecimalFormat("#,###");
	private volatile Snapshot lastSnapshot = Snapshot.EMPTY;
	private long lastLogTime = System.currentTimeMillis();
	private long lastCpuTime = PlatformMetrics.osBean.getProcessCpuTime();
	// 代际版本号：锁内递增、锁外无锁读，volatile 保证缓存句柄及时感知重置与淘汰
	private volatile int clearSerial;
	private @Nullable ScheduledFuture<?> scheduleFuture;
	private final LongCounter transactionRedoCounter = allocCounter("Transaction.Redo");
	private final LongCounter transactionRedoAndReleaseLockCounter = allocCounter("Transaction.RedoAndReleaseLock");

	@Override
	public LongCounter allocCounter(String name) {
		return allocCounter(name, false);
	}

	@Override
	public LabeledCounterCreator allocLabeledCounterCreator(String name,
															String... labelNames) {
		return labels -> allocCounter(labels.length > 0 ? name + "." + String.join(".", labels) : name);
	}

	@Override
	public LabeledObserverCreator allocRunTimeObserverCreator(String name,
															  String... labelNames) {
		return labels -> getRunTimeObserver(labels.length > 0 ? name + "." + String.join(".", labels) : name);
	}

	public LongCounter allocCounter(String name, boolean accumulate) {
		lock();
		try {
			int n = countInfos.length;
			var cis = new CountInfo[n + 1];
			System.arraycopy(countInfos, 0, cis, 0, n);
			var ci = new CountInfo(name, accumulate);
			cis[n] = ci;
			countInfos = cis;
			return ci;
		} finally {
			unlock();
		}
	}

	/** 随时可调用。 */
	public boolean addExcludeRunKey(String key) {
		return excludeRunKeys.add(key);
	}

	/** 随时可调用。与同名String键排除同一条目（Class归一化为类名）。 */
	public boolean addExcludeRunKey(Class<?> cls) {
		return excludeRunKeys.add(cls.getName());
	}

	/** 随时可调用。 */
	public boolean addExcludeProtocolTypeId(long typeId) {
		return excludeProtocolTypeIds.putIfAbsent(typeId, Boolean.TRUE) == null;
	}

	private @Nullable RunInfoWithSerial getRunInfoWithSerial(String name) {
		if (excludeRunKeys.contains(name))
			return null;
		var alreadyLocked = isHeldByCurrentThread();
		if (!alreadyLocked)
			lock();
		try {
			// 与淘汰、重置同锁，避免给刚离开映射的条目盖上新代际，令旧观察句柄始终写到孤儿条目。
			var ri = runInfoMap.computeIfAbsent(name, k -> new RunInfoWithSerial(k, clearSerial));
			ri.serial = clearSerial;
			return ri;
		} finally {
			if (!alreadyLocked)
				unlock();
		}
	}

	@Override
	public void serviceStart(Service service) {
	}

	@Override
	public void serviceStop(Service service) {
	}

	@Override
	public LongObserver getRunTimeObserver(String name) {
		var ri = getRunInfoWithSerial(name);
		if (ri == null)
			return dummyLongObserver;
		var counterWrapper = new OutObject<>(ri);
		return v -> {
			var ri2 = counterWrapper.value;
			if (ri2 != null) {
				if (ri2.serial != clearSerial) {
					counterWrapper.value = ri2 = getRunInfoWithSerial(name);
					if (ri2 == null)
						return;
				}
				ri2.procCount.increment();
				ri2.procTime.add(v);
			}
		};
	}

	@Override
	public void addRecvSizeTime(long typeId, @Nullable Class<?> cls, int size, long timeNs) {
		if (excludeProtocolTypeIds.containsKey(typeId))
			return;
		for (; ; ) {
			var pi = protocolInfoMap.get(typeId);
			if (pi != null) {
				pi.procCount.increment();
				pi.procTime.add(timeNs);
				pi.recvSize.add(size);
				return;
			}
			if (cls == null)
				cls = Protocol.getClassByTypeId(typeId);
			protocolInfoMap.putIfAbsent(typeId, new ProtocolInfo(cls != null ? cls.getName() : String.valueOf(typeId)));
		}
	}

	@Override
	public void addRecvDispatchTime(long typeId, long timeNs) {
	}

	@Override
	public void addSendSize(long typeId, int size) {
		if (!excludeProtocolTypeIds.containsKey(typeId)) {
			for (; ; ) {
				var pi = protocolInfoMap.get(typeId);
				if (pi != null) {
					pi.sendCount.increment();
					pi.sendSize.add(size);
					break;
				}
				var cls = Protocol.getClassByTypeId(typeId);
				protocolInfoMap.putIfAbsent(typeId,
					new ProtocolInfo(cls != null ? cls.getName() : String.valueOf(typeId)));
			}
		}
	}

	public ProcedureInfo getOrAddProcedureInfo(String name) {
		return procedureInfoMap.computeIfAbsent(name, ProcedureInfo::new);
	}

	/** 按名称的单调累计执行数（resetCounter会清零）；名称不存在返回0。 */
	public long getProcedureTotalCount(String name) {
		var total = procedureTotalMap.get(name);
		return total != null ? total.sum() : 0;
	}

	@Override
	public ProcedureCounter allocProcedureCounter(String name) {
		return procedureCounterMap.computeIfAbsent(name, PerfProcedureCounter::new);
	}

	// 按 clearSerial 代际重绑 ProcedureInfo；redo 聚合到全局计数
	private final class PerfProcedureCounter implements ProcedureCounter {
		private final String name;
		private volatile @Nullable Bound bound;

		private record Bound(ProcedureInfo info, int serial) {
		}

		PerfProcedureCounter(String name) {
			this.name = name;
		}

		private ProcedureInfo info() {
			var b = bound;
			if (b == null || b.serial() != clearSerial) {
				PerfCounter.this.lock();
				try {
					b = bound;
					if (b == null || b.serial() != clearSerial)
						bound = b = new Bound(getOrAddProcedureInfo(name), clearSerial);
				} finally {
					PerfCounter.this.unlock();
				}
			}
			return b.info();
		}

		@Override
		public void start() {
		}

		@Override
		public void end(long resultCode, long timeNs) {
			info().getOrAddResult(resultCode).increment();
			procedureTotalMap.computeIfAbsent(name, __ -> new LongAdder()).increment();
		}

		@Override
		public void redo() {
			transactionRedoCounter.increment();
		}

		@Override
		public void redoAndReleaseLock() {
			transactionRedoAndReleaseLockCounter.increment();
		}

		@Override
		public void manyLocks(int count) {
		}
	}

	public TableInfo getOrAddTableInfo(long tableId) {
		//noinspection DataFlowIssue
		return tableInfoMap.computeIfAbsent(tableId, k -> {
			var tableName = TableKey.tables.get(k);
			return new TableInfo(tableName != null ? tableName : String.valueOf(k));
		});
	}

	@Override
	public LongCounter tableCounter(long tableId, TableMetric metric) {
		return getOrAddTableInfo(tableId).counter(metric);
	}

	public String getLastLog() {
		return lastSnapshot.formattedLog();
	}

	@Override
	public Snapshot snapshotAndReset() {
		return snapshotAndResetCore();
	}

	@Override
	public Snapshot getLastSnapshot() {
		return lastSnapshot;
	}

	@Override
	public void init() {
		tryStartScheduledLog();
	}

	public ScheduledFuture<?> tryStartScheduledLog() {
		lock();
		try {
			var f = scheduleFuture;
			if (f == null || f.isCancelled()) {
				var periodMs = Math.max(PERF_PERIOD, 1) * 1000L;
				scheduleFuture = f = TaskSpec.ofAction(() -> logger.info(getLogAndReset())).schedulePeriodNow(periodMs, periodMs);
			}
			return f;
		} finally {
			unlock();
		}
	}

	public boolean cancelScheduledLog() {
		ScheduledFuture<?> f;
		lock();
		try {
			f = scheduleFuture;
			scheduleFuture = null;
		} finally {
			unlock();
		}
		// 释放本锁后再 cancel，避免与定时任务体（TimerFuture 锁 -> 本锁）形成 ABBA 死锁
		return f != null && f.cancel(false);
	}

	public void resetCounter() {
		// 与 getLogAndReset 互斥，保证清零与代际推进原子可见
		lock();
		try {
			runInfoMap.clear();
			protocolInfoMap.clear();
			procedureInfoMap.clear();
			procedureTotalMap.clear(); // 单调累计随代际清零重开
			// tableInfoMap 就地清零，保留外部长期缓存的计数句柄
			for (var it = tableInfoMap.entryIterator(); it.moveToNext(); ) {
				//noinspection DataFlowIssue
				it.value().reset();
			}
			// info() 的慢路径同样持有此锁，旧条目与新 clearSerial 不会被组装成同一个 Bound。
			//noinspection NonAtomicOperationOnVolatileField
			clearSerial++;
			for (var ci : countInfos) {
				ci.reset();
				ci.lastCount = 0;
			}
		} finally {
			unlock();
		}
	}

	public String getLogAndReset() {
		return snapshotAndResetCore().formattedLog();
	}

	private Snapshot snapshotAndResetCore() {
		lock();
		try {
			var curTime = System.currentTimeMillis();
			var time = curTime - lastLogTime;
			lastLogTime = curTime;
			var curCpuTime = PlatformMetrics.osBean.getProcessCpuTime();
			var cpuTime = curCpuTime - lastCpuTime;
			lastCpuTime = curCpuTime;

			var procCountAll = 0L;
			var procTimeAll = 0L;
			var rList = new ArrayList<RunInfo>(runInfoMap.size());
			var evicted = false;
			for (var it = runInfoMap.values().iterator(); it.hasNext(); ) {
				var ri = it.next();
				ri.lastProcCount = ri.procCount.sumThenReset();
				if (ri.lastProcCount == 0) {
					if (++ri.idleCount >= RunInfo.MAX_IDLE_COUNT) {
						it.remove();
						evicted = true;
					}
					continue;
				}
				ri.lastProcTime = ri.procTime.sumThenReset();
				procCountAll += ri.lastProcCount;
				procTimeAll += ri.lastProcTime;
				ri.idleCount = 0;
				rList.add(ri);
			}
			var runtime = Runtime.getRuntime();
			@SuppressWarnings("deprecation")
			var sb = new StringBuilder(100 + 50 * 3 * PERF_COUNT).append("count last ").append(time).append("ms:\n")
				.append(" [load: ").append(cpuTime / 1_000_000).append("ms ")
				.append(String.format("%.2f%%", PlatformMetrics.osBean.getProcessCpuLoad() * 100))
				.append(" free/total/max:").append(runtime.freeMemory() >> 20)
				.append('/').append(runtime.totalMemory() >> 20).append('/').append(runtime.maxMemory() >> 20)
				.append("M direct:").append(PlatformMetrics.getReservedDirectMemory() >> 20).append('/')
				.append(PlatformMetrics.getMaxDirectMemory() >> 20).append("M,")
				.append(PlatformMetrics.getTotalDirectCapacity() >> 20).append("M/").append(PlatformMetrics.getDirectCount())
				.append(" commit/free/all:").append(PlatformMetrics.osBean.getCommittedVirtualMemorySize() >> 20).append('/')
				.append(PlatformMetrics.osBean.getFreePhysicalMemorySize() >> 20).append('+')
				.append(PlatformMetrics.osBean.getFreeSwapSpaceSize() >> 20).append('/')
				.append(PlatformMetrics.osBean.getTotalPhysicalMemorySize() >> 20).append('+')
				.append(PlatformMetrics.osBean.getTotalSwapSpaceSize() >> 20)
				.append("M]\n [run: ").append(procCountAll).append(", ")
				.append(procTimeAll / 1_000_000).append("ms]\n");
			rList.sort((ri0, ri1) -> Long.signum(ri1.lastProcTime - ri0.lastProcTime));
			for (int i = 0, n = Math.min(rList.size(), PERF_COUNT); i < n; i++) {
				var ri = rList.get(i);
				var perTime = ri.lastProcTime / ri.lastProcCount;
				sb.append("  ").append(ri.name).append(": ").append(ri.lastProcTime / 1_000_000)
					.append("ms = ").append(ri.lastProcCount).append(" * ")
					.append(numFormatter.format(perTime)).append("ns\n");
			}

			procCountAll = 0;
			procTimeAll = 0;
			var recvSizeAll = 0L;
			var sendCountAll = 0L;
			var sendSizeAll = 0L;
			var pList = new ArrayList<ProtocolInfo>(protocolInfoMap.size());
			for (var it = protocolInfoMap.entryIterator(); it.moveToNext(); ) {
				var pi = it.value();
				//noinspection DataFlowIssue
				pi.lastProcCount = pi.procCount.sumThenReset();
				pi.lastSendCount = pi.sendCount.sumThenReset();
				if ((pi.lastProcCount | pi.lastSendCount) == 0) {
					if (++pi.idleCount >= RunInfo.MAX_IDLE_COUNT)
						protocolInfoMap.remove(it.key());
					continue;
				}
				pi.lastProcTime = pi.procTime.sumThenReset();
				pi.lastRecvSize = pi.recvSize.sumThenReset();
				pi.lastSendSize = pi.sendSize.sumThenReset();
				procCountAll += pi.lastProcCount;
				procTimeAll += pi.lastProcTime;
				recvSizeAll += pi.lastRecvSize;
				sendCountAll += pi.lastSendCount;
				sendSizeAll += pi.lastSendSize;
				pi.idleCount = 0;
				pList.add(pi);
			}
			sb.append(" [recv: ").append(procCountAll).append(", ").append(recvSizeAll / 1000).append("K, ")
				.append(procTimeAll / 1_000_000).append("ms]\n");
			pList.sort((pi0, pi1) -> Long.signum(pi1.lastProcTime - pi0.lastProcTime));
			for (int i = 0, n = Math.min(pList.size(), PERF_COUNT); i < n; i++) {
				var pi = pList.get(i);
				if (pi.lastProcCount == 0)
					continue;
				var perTime = pi.lastProcTime / pi.lastProcCount;
				var perSize = pi.lastRecvSize / pi.lastProcCount;
				sb.append("  ").append(pi.name).append(": ").append(pi.lastProcTime / 1_000_000)
					.append("ms = ").append(pi.lastProcCount).append(" * ")
					.append(numFormatter.format(perTime)).append("ns,")
					.append(numFormatter.format(perSize)).append("B\n");
			}
			sb.append(" [send: ").append(sendCountAll).append(", ").append(sendSizeAll / 1000).append("K]\n");
			pList.sort((pi0, pi1) -> Long.signum(pi1.lastSendSize - pi0.lastSendSize));
			for (int i = 0, n = Math.min(pList.size(), PERF_COUNT); i < n; i++) {
				var pi = pList.get(i);
				if (pi.lastSendCount == 0)
					break;
				var perSize = pi.lastSendSize / pi.lastSendCount;
				sb.append("  ").append(pi.name).append(": ").append(pi.lastSendSize / 1_000)
					.append("K = ").append(pi.lastSendCount).append(" * ")
					.append(numFormatter.format(perSize)).append("B\n");
			}

			var procedureTotal = 0L;
			var procedureSucc = 0L;
			var procedureResults = new LinkedHashMap<String, Map<Long, Long>>(Math.max(16, procedureInfoMap.size() * 2));
			var prList = new ArrayList<ProcedureInfo>(procedureInfoMap.size());
			for (var it = procedureInfoMap.values().iterator(); it.hasNext(); ) {
				var pi = it.next();
				pi.resultMapLast = pi.resultMap;
				pi.resultMap = new LongConcurrentHashMap<>();
				var totalCount = 0L;
				var succCount = 0L;
				var results = new LinkedHashMap<Long, Long>();
				for (var it2 = pi.resultMapLast.entryIterator(); it2.moveToNext(); ) {
					//noinspection DataFlowIssue
					var v = it2.value().sum();
					results.put(it2.key(), v);
					totalCount += v;
					if (it2.key() == 0)
						succCount = v;
				}
				procedureResults.put(pi.name, results); // 含零计数条目，供名称列表与结果查询使用
				if (totalCount == 0) {
					if (++pi.idleCount >= ProcedureInfo.MAX_IDLE_COUNT) {
						it.remove();
						evicted = true;
					}
					continue;
				}
				procedureTotal += totalCount;
				procedureSucc += succCount;
				pi.totalCount = totalCount;
				pi.succRatio = (int)(succCount * 100 / totalCount);
				pi.idleCount = 0;
				prList.add(pi);
			}
			// 空闲淘汰需推进代际，促使缓存句柄下次重绑
			if (evicted) {
				//noinspection NonAtomicOperationOnVolatileField
				clearSerial++;
			}
			sb.append(" [procedure: ").append(procedureSucc).append('/').append(procedureTotal).append('=')
				.append(procedureTotal != 0 ? procedureSucc * 100 / procedureTotal : 0).append("%]\n");
			prList.sort((pi0, pi1) -> {
				var c = pi0.succRatio - pi1.succRatio;
				return c != 0 ? Long.signum(c) : Long.signum(pi1.totalCount - pi0.totalCount);
			});
			for (int i = 0, n = Math.min(prList.size(), PERF_COUNT); i < n; i++)
				sb.append("  ").append(prList.get(i)).append('\n');

			var tableResults = new LinkedHashMap<String, Map<String, Long>>(Math.max(16, tableInfoMap.size() * 2));
			var tList = new ArrayList<TableInfo>(tableInfoMap.size());
			for (var ti : tableInfoMap) {
				var active = ti.checkpointAndReset();
				tableResults.put(ti.tableName, ti.snapshotResult());
				if (active)
					tList.add(ti);
			}
			sb.append(" [table: ").append(tList.size()).append("]\n");
			int n = Math.min(tList.size(), PERF_COUNT);
			if (n > 0) {
				tList.sort((ti0, ti1) -> Long.signum(ti1.lastLockCount() - ti0.lastLockCount()));
				sb.append("  ").append(TableInfo.getLogTitle()).append('\n');
				for (int i = 0; i < n; i++)
					sb.append("  ").append(tList.get(i)).append('\n');
			}

			var cList = new ArrayList<CountInfo>(countInfos.length);
			for (var ci : countInfos) {
				var newCount = ci.sumThenReset();
				if (ci.accumulate)
					ci.lastCount += newCount;
				else
					ci.lastCount = newCount;
				if (ci.lastCount != 0)
					cList.add(ci);
			}
			if (!cList.isEmpty()) {
				cList.sort((ci0, ci1) -> Long.signum(ci1.lastCount - ci0.lastCount));
				sb.append(" [count]\n");
				for (var ci : cList)
					sb.append("  ").append(ci.name).append(": ").append(ci.lastCount).append('\n');
			}

			// 结果Map均为本轮新建，发布后不再修改，由Snapshot按只读约定共享。
			return lastSnapshot = new Snapshot(procedureResults, tableResults, sb.toString());
		} finally {
			unlock();
		}
	}
}
