package Zeze.Util;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Transaction.TableKey;
import com.sun.management.OperatingSystemMXBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

		final @NotNull String name;
		final LongAdder procCount = new LongAdder(); // 处理次数
		final LongAdder procTime = new LongAdder(); // 处理时间(ns)
		long lastProcCount;
		long lastProcTime;
		int idleCount; // 没收集到信息的轮数

		RunInfo(@NotNull String name) {
			this.name = name;
		}
	}

	private static final class RunInfoWithSerial extends RunInfo {
		final int serial;

		RunInfoWithSerial(@NotNull String name, int serial) {
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

		final @NotNull String name;
		// FND5-11：热路径getOrAddResult无锁读写resultMap，getLogAndReset持锁换新——引用必须
		// volatile发布，否则业务线程可无限期读旧引用，自增落在已消费的resultMapLast上丢统计。
		volatile @NotNull LongConcurrentHashMap<LongAdder> resultMap = new LongConcurrentHashMap<>();
		// FND5-11同族（FND5-11复审）：锁内写（getLogAndReset），getResultMapLast()被定时器线程
		// （ProcedureStatistics.Watcher）与HTTP查询线程无锁读——同样需要volatile发布，否则读者
		// 可长期读到旧快照，watch的(total-last)增量恒为0导致回调永不触发。
		volatile @NotNull LongConcurrentHashMap<LongAdder> resultMapLast = new LongConcurrentHashMap<>();
		long totalCount;
		int succRatio; // 成功率百分比
		int idleCount; // 没收集到信息的轮数

		ProcedureInfo(@NotNull String name) {
			this.name = name;
		}

		public @NotNull String getName() {
			return name;
		}

		public @NotNull LongConcurrentHashMap<LongAdder> getResultMapLast() {
			return resultMapLast;
		}

		public @NotNull LongAdder getOrAddResult(long resultCode) {
			return resultMap.computeIfAbsent(resultCode, __ -> new LongAdder());
		}

		public @NotNull String toString(boolean last) {
			var sb = new StringBuilder();
			sb.append(name).append(':').append(succRatio).append('%');
			for (var it = (last ? resultMapLast : resultMap).entryIterator(); it.moveToNext(); )
				sb.append(", ").append(it.key()).append(':').append(it.value().sum());
			return sb.toString();
		}

		@Override
		public @NotNull String toString() {
			return toString(true);
		}
	}

	public static final class TableInfo {
		private final @NotNull String tableName;
		private final LongAdderCounter[] counters; // 按TableMetric.ordinal索引
		private final long[] lastCounts; // 按TableMetric.ordinal索引，checkpointAndReset时更新

		TableInfo(@NotNull String tableName) {
			this.tableName = tableName;
			var metrics = TableMetric.values();
			counters = new LongAdderCounter[metrics.length];
			for (var i = 0; i < counters.length; i++)
				counters[i] = new LongAdderCounter();
			lastCounts = new long[metrics.length];
		}

		public @NotNull LongAdderCounter counter(@NotNull TableMetric metric) {
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

		@NotNull Map<String, Long> snapshotResult() {
			var m = new LinkedHashMap<String, Long>(lastCounts.length * 2);
			for (var metric : TableMetric.values())
				m.put(metric.key, lastCounts[metric.ordinal()]);
			return m;
		}

		private long last(@NotNull TableMetric metric) {
			return lastCounts[metric.ordinal()];
		}

		public static @NotNull String getLogTitle() {
			return String.format("%-60s CacheHit AcqShrHit AcqModHit AcqShrCnt AcqModCnt AcqInvCnt ReduceCnt" +
					" CacGetCnt StoGetCnt LockCount  ReadLock WriteLock TryRdLock TryWtLock RedoCount", "TableName");
		}

		@Override
		public @NotNull String toString() {
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
		final @NotNull String name;
		final boolean accumulate;
		long lastCount;

		CountInfo(@NotNull String name, boolean accumulate) {
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
	public static final OperatingSystemMXBean osBean = (OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
	public static final Field fMaxDirectMemory; // long
	public static final AtomicLong reservedDirectMemory;
	public static final AtomicLong totalDirectCapacity;
	public static final AtomicLong directCount;

	static {
		try {
			var cBits = Class.forName("java.nio.Bits");
			fMaxDirectMemory = Json.setAccessible(cBits.getDeclaredField("MAX_MEMORY"));
			reservedDirectMemory = (AtomicLong)Json.setAccessible(cBits.getDeclaredField("RESERVED_MEMORY")).get(null);
			totalDirectCapacity = (AtomicLong)Json.setAccessible(cBits.getDeclaredField("TOTAL_CAPACITY")).get(null);
			directCount = (AtomicLong)Json.setAccessible(cBits.getDeclaredField("COUNT")).get(null);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private final ConcurrentHashMap<Object, RunInfoWithSerial> runInfoMap = new ConcurrentHashMap<>(); // key: Class or others
	private final LongConcurrentHashMap<ProtocolInfo> protocolInfoMap = new LongConcurrentHashMap<>(); // key: typeId
	private final ConcurrentHashMap<String, ProcedureInfo> procedureInfoMap = new ConcurrentHashMap<>(); // key: procedureName
	private final LongConcurrentHashMap<TableInfo> tableInfoMap = new LongConcurrentHashMap<>(); // key: tableId
	private CountInfo[] countInfos = new CountInfo[0];
	// exclude 随时可配置；已存在的统计条目要等空闲回收才会消失，并发读写安全
	private final Set<Object> excludeRunKeys = ConcurrentHashMap.newKeySet(); // value: Class or others
	private final LongConcurrentHashMap<Boolean> excludeProtocolTypeIds = new LongConcurrentHashMap<>(); // key: typeId
	private final DecimalFormat numFormatter = new DecimalFormat("#,###");
	private volatile @NotNull Snapshot lastSnapshot = Snapshot.EMPTY;
	private long lastLogTime = System.currentTimeMillis();
	private long lastCpuTime = osBean.getProcessCpuTime();
	private int clearSerial;
	private @Nullable ScheduledFuture<?> scheduleFuture;
	private final LongCounter transactionRedoCounter = allocCounter("Transaction.Redo");
	private final LongCounter transactionRedoAndReleaseLockCounter = allocCounter("Transaction.RedoAndReleaseLock");

	public static @NotNull PerfCounter instance() {
		return Objects.requireNonNull((PerfCounter)ZezeCounter.instance);
	}

	public static long getMaxDirectMemory() {
		try {
			return fMaxDirectMemory.getLong(null);
		} catch (ReflectiveOperationException e) {
			throw Task.forceThrow(e);
		}
	}

	public static long getReservedDirectMemory() {
		return reservedDirectMemory.get();
	}

	public static long getTotalDirectCapacity() {
		return totalDirectCapacity.get();
	}

	public static long getDirectCount() {
		return directCount.get();
	}

	@Override
	public @NotNull LongCounter allocCounter(@NotNull String name) {
		return allocCounter(name, false);
	}

	@Override
	public @NotNull LabeledCounterCreator allocLabeledCounterCreator(@NotNull String name,
																	 @NotNull String... labelNames) {
		return labels -> allocCounter(labels.length > 0 ? name + "." + String.join(".", labels) : name);
	}

	@Override
	public @NotNull LabeledObserverCreator allocRunTimeObserverCreator(@NotNull String name,
																	   @NotNull String... labelNames) {
		return labels -> getRunTimeObserver(labels.length > 0 ? name + "." + String.join(".", labels) : name);
	}

	public @NotNull LongCounter allocCounter(@NotNull String name, boolean accumulate) {
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
	public boolean addExcludeRunKey(@NotNull String key) {
		return excludeRunKeys.add(key);
	}

	/** 随时可调用。 */
	public boolean addExcludeRunKey(@NotNull Class<?> cls) {
		return excludeRunKeys.add(cls);
	}

	/** 随时可调用。 */
	public boolean addExcludeProtocolTypeId(long typeId) {
		return excludeProtocolTypeIds.putIfAbsent(typeId, Boolean.TRUE) == null;
	}

	private @Nullable RunInfoWithSerial getRunInfoWithSerial(@NotNull Object key) {
		if (excludeRunKeys.contains(key))
			return null;
		for (; ; ) {
			var ri = runInfoMap.get(key);
			if (ri != null)
				return ri;
			runInfoMap.putIfAbsent(key, new RunInfoWithSerial(
					key instanceof Class ? ((Class<?>)key).getName() : String.valueOf(key), clearSerial));
		}
	}

	@Override
	public void addTaskRunTime(@NotNull Object key, long timeNs) {
		var ri = getRunInfoWithSerial(key);
		if (ri != null) {
			ri.procCount.increment();
			ri.procTime.add(timeNs);
		}
	}

	@Override
	public void serviceStart(@NotNull Service service) {
	}

	@Override
	public void serviceStop(@NotNull Service service) {
	}

	@Override
	public void procedureStart(@NotNull String name) {
	}

	@Override
	public void procedureEnd(@NotNull String name, long resultCode, long timeNs) {
		// addRunTime(name, timeNs);
		getOrAddProcedureInfo(name).getOrAddResult(resultCode).increment();
	}

	@Override
	public void procedureRedo(@NotNull String name) {
		transactionRedoCounter.increment();
	}

	@Override
	public void procedureRedoAndReleaseLock(@NotNull String name) {
		transactionRedoAndReleaseLockCounter.increment();
	}

	@Override
	public @NotNull LongObserver getRunTimeObserver(@NotNull Object key) {
		var ri = getRunInfoWithSerial(key);
		if (ri == null)
			return dummyLongObserver;
		var counterWrapper = new OutObject<>(ri);
		return v -> {
			var ri2 = counterWrapper.value;
			if (ri2 != null) {
				if (ri2.serial != clearSerial) {
					counterWrapper.value = ri2 = getRunInfoWithSerial(key);
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

	public @NotNull ConcurrentHashMap<String, ProcedureInfo> getProcedureInfoMap() {
		return procedureInfoMap;
	}

	public @Nullable ProcedureInfo getProcedureInfo(@NotNull String name) {
		return procedureInfoMap.get(name);
	}

	public @NotNull ProcedureInfo getOrAddProcedureInfo(@NotNull String name) {
		return procedureInfoMap.computeIfAbsent(name, ProcedureInfo::new);
	}

	public @NotNull LongConcurrentHashMap<TableInfo> getTableInfoMap() {
		return tableInfoMap;
	}

	public @Nullable TableInfo getTableInfo(long tableId) {
		return tableInfoMap.get(tableId);
	}

	public @NotNull TableInfo getOrAddTableInfo(long tableId) {
		return tableInfoMap.computeIfAbsent(tableId, k -> {
			var tableName = TableKey.tables.get(k);
			return new TableInfo(tableName != null ? tableName : String.valueOf(k));
		});
	}

	@Override
	public @NotNull LongCounter tableCounter(long tableId, @NotNull TableMetric metric) {
		return getOrAddTableInfo(tableId).counter(metric);
	}

	public @NotNull String getLastLog() {
		return lastSnapshot.formattedLog();
	}

	@Override
	public @NotNull Snapshot collectAndReset() {
		return collectAndResetCore();
	}

	@Override
	public @NotNull Snapshot getLast() {
		return lastSnapshot;
	}

	public long getLastLogTime() {
		return lastLogTime;
	}

	@Override
	public void init() {
		tryStartScheduledLog();
	}

	public @Nullable ScheduledFuture<?> getScheduleFuture() {
		return scheduleFuture;
	}

	public @NotNull ScheduledFuture<?> tryStartScheduledLog() {
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
		lock();
		try {
			var f = scheduleFuture;
			scheduleFuture = null;
			return f != null && f.cancel(false);
		} finally {
			unlock();
		}
	}

	public void resetCounter() {
		// 与getLogAndReset互斥（FND4-23）：clearSerial代际推进与四个map清空
		// 不是原子的，无锁并发reset会互相覆盖统计窗口。
		lock();
		try {
			clearSerial++;
			runInfoMap.clear();
			protocolInfoMap.clear();
			procedureInfoMap.clear();
			tableInfoMap.clear();
			for (var ci : countInfos) {
				ci.reset();
				ci.lastCount = 0;
			}
		} finally {
			unlock();
		}
	}

	public @NotNull String getLogAndReset() {
		return collectAndResetCore().formattedLog();
	}

	private @NotNull Snapshot collectAndResetCore() {
		lock();
		try {
			var curTime = System.currentTimeMillis();
			var time = curTime - lastLogTime;
			lastLogTime = curTime;
			var curCpuTime = osBean.getProcessCpuTime();
			var cpuTime = curCpuTime - lastCpuTime;
			lastCpuTime = curCpuTime;

			var procCountAll = 0L;
			var procTimeAll = 0L;
			var rList = new ArrayList<RunInfo>(runInfoMap.size());
			for (var it = runInfoMap.values().iterator(); it.hasNext(); ) {
				var ri = it.next();
				ri.lastProcCount = ri.procCount.sumThenReset();
				if (ri.lastProcCount == 0) {
					if (++ri.idleCount >= RunInfo.MAX_IDLE_COUNT)
						it.remove();
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
					.append(String.format("%.2f%%", osBean.getProcessCpuLoad() * 100))
					.append(" free/total/max:").append(runtime.freeMemory() >> 20)
					.append('/').append(runtime.totalMemory() >> 20).append('/').append(runtime.maxMemory() >> 20)
					.append("M direct:").append(getReservedDirectMemory() >> 20).append('/')
					.append(getMaxDirectMemory() >> 20).append("M,")
					.append(getTotalDirectCapacity() >> 20).append("M/").append(getDirectCount())
					.append(" commit/free/all:").append(osBean.getCommittedVirtualMemorySize() >> 20).append('/')
					.append(osBean.getFreePhysicalMemorySize() >> 20).append('+')
					.append(osBean.getFreeSwapSpaceSize() >> 20).append('/')
					.append(osBean.getTotalPhysicalMemorySize() >> 20).append('+')
					.append(osBean.getTotalSwapSpaceSize() >> 20)
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
					var v = it2.value().sum();
					results.put(it2.key(), v);
					totalCount += v;
					if (it2.key() == 0)
						succCount = v;
				}
				procedureResults.put(pi.name, results); // 含零计数条目，供名称列表与结果查询使用
				if (totalCount == 0) {
					if (++pi.idleCount >= ProcedureInfo.MAX_IDLE_COUNT)
						it.remove();
					continue;
				}
				procedureTotal += totalCount;
				procedureSucc += succCount;
				pi.totalCount = totalCount;
				pi.succRatio = (int)(succCount * 100 / totalCount);
				pi.idleCount = 0;
				prList.add(pi);
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

			return lastSnapshot = new Snapshot(Map.copyOf(procedureResults), Map.copyOf(tableResults), sb.toString());
		} finally {
			unlock();
		}
	}
}
