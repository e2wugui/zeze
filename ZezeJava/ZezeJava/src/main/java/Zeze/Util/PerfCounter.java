package Zeze.Util;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
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
		@NotNull LongConcurrentHashMap<LongAdder> resultMap = new LongConcurrentHashMap<>();
		@NotNull LongConcurrentHashMap<LongAdder> resultMapLast = new LongConcurrentHashMap<>();
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

	public static final class TableInfo implements TableCounter {
		private final @NotNull String tableName;
		private final LongAdderCounter readLock = new LongAdderCounter();
		private final LongAdderCounter writeLock = new LongAdderCounter();
		private final LongAdderCounter storageGet = new LongAdderCounter();
		// 这两个统计用来观察cache清理的影响
		private final LongAdderCounter tryReadLock = new LongAdderCounter();
		private final LongAdderCounter tryWriteLock = new LongAdderCounter();
		// global acquire 的次数，即时没有开启cache-sync，也会有一点点计数，因为没人抢，所以以后总是成功了。
		private final LongAdderCounter acquireShare = new LongAdderCounter();
		private final LongAdderCounter acquireModify = new LongAdderCounter();
		private final LongAdderCounter acquireInvalid = new LongAdderCounter();
		private final LongAdderCounter reduceInvalid = new LongAdderCounter();
		private final LongAdderCounter redo = new LongAdderCounter();

		long readLockCount;
		long writeLockCount;
		long storageGetCount;
		long tryReadLockCount;
		long tryWriteLockCount;
		long acquireShareCount;
		long acquireModifyCount;
		long acquireInvalidCount;
		long reduceInvalidCount;
		long lockCount;
		long redoCount;

		TableInfo(@NotNull String tableName) {
			this.tableName = tableName;
		}

		@Override
		public @NotNull LongAdderCounter readLock() {
			return readLock;
		}

		@Override
		public @NotNull LongAdderCounter writeLock() {
			return writeLock;
		}

		@Override
		public @NotNull LongAdderCounter storageGet() {
			return storageGet;
		}

		@Override
		public @NotNull LongAdderCounter tryReadLock() {
			return tryReadLock;
		}

		@Override
		public @NotNull LongAdderCounter tryWriteLock() {
			return tryWriteLock;
		}

		@Override
		public @NotNull LongAdderCounter acquireShare() {
			return acquireShare;
		}

		@Override
		public @NotNull LongAdderCounter acquireModify() {
			return acquireModify;
		}

		@Override
		public @NotNull LongAdderCounter acquireInvalid() {
			return acquireInvalid;
		}

		@Override
		public @NotNull LongAdderCounter reduceInvalid() {
			return reduceInvalid;
		}

		@Override
		public @NotNull LongAdderCounter redo() {
			return redo;
		}

		boolean checkpointAndReset() {
			readLockCount = readLock.sumThenReset();
			writeLockCount = writeLock.sumThenReset();
			storageGetCount = storageGet.sumThenReset();
			tryReadLockCount = tryReadLock.sumThenReset();
			tryWriteLockCount = tryWriteLock.sumThenReset();
			acquireShareCount = acquireShare.sumThenReset();
			acquireModifyCount = acquireModify.sumThenReset();
			acquireInvalidCount = acquireInvalid.sumThenReset();
			reduceInvalidCount = reduceInvalid.sumThenReset();
			lockCount = readLockCount + writeLockCount;
			redoCount = redo.sumThenReset();
			return (readLockCount | writeLockCount | storageGetCount | tryReadLockCount | tryWriteLockCount
					| acquireShareCount | acquireModifyCount | acquireInvalidCount | reduceInvalidCount
					| redoCount) != 0;
		}

		public static @NotNull String getLogTitle() {
			return String.format("%-60s CacheHit AcqShrHit AcqModHit AcqShrCnt AcqModCnt AcqInvCnt ReduceCnt" +
					" StoGetCnt LockCount  ReadLock WriteLock TryRdLock TryWtLock RedoCount", "TableName");
		}

		@Override
		public @NotNull String toString() {
			float cacheHit = lockCount != 0 ? (lockCount - storageGetCount) * 100.0f / lockCount : 0;
			float acquireShareHit = lockCount != 0 ? (lockCount - acquireShareCount) * 100.0f / lockCount : 0;
			float acquireModifyHit = lockCount != 0 ? (lockCount - acquireModifyCount) * 100.0f / lockCount : 0;
			return String.format("%-60s%8.2f%%%9.2f%%%9.2f%%%10d%10d%10d%10d%10d%10d%10d%10d%10d%10d%10d", tableName,
					cacheHit, acquireShareHit, acquireModifyHit,
					acquireShareCount, acquireModifyCount, acquireInvalidCount, reduceInvalidCount,
					storageGetCount, lockCount, readLockCount, writeLockCount, tryReadLockCount, tryWriteLockCount,
					redoCount);
		}
	}

	public static final class ServiceInfo implements Action0 {
		private final @NotNull Service service;
		private @Nullable ScheduledFuture<?> statisticLogFuture;
		private int periodSec;
		private final long[] lastSizes = new long[]{-1, 0, 0, 0, 0, 0};

		public ServiceInfo(@NotNull Service service) {
			this.service = service;
		}

		public @NotNull ScheduledFuture<?> startStatisticLog(int periodSec) {
			if (periodSec <= 0)
				throw new IllegalArgumentException("periodSec(" + periodSec + ") < 0");
			var f = statisticLogFuture;
			if (f != null && !f.isCancelled())
				cancelStartStatisticLog();
			this.periodSec = periodSec;
			f = Task.scheduleUnsafe(Random.getInstance().nextLong(periodSec * 1000L), periodSec * 1000L, this);
			statisticLogFuture = f;
			return f;
		}

		public boolean cancelStartStatisticLog() {
			var f = statisticLogFuture;
			statisticLogFuture = null;
			return f != null && f.cancel(false);
		}

		@Override
		public void run() throws Exception {
			service.updateRecvSendSize();
			var selectors = service.getSelectors();
			long selectCount = selectors.getSelectCount();
			long recvCount = service.getRecvCount();
			long recvSize = service.getRecvSize();
			long sendCount = service.getSendCount();
			long sendSize = service.getSendSize();
			long sendRawSize = service.getSendRawSize();
			if (lastSizes[0] != -1) {
				long sn = (selectCount - lastSizes[0]) / periodSec;
				long rc = (recvCount - lastSizes[1]) / periodSec;
				long rs = (recvSize - lastSizes[2]) / periodSec;
				long sc = (sendCount - lastSizes[3]) / periodSec;
				long ss = (sendSize - lastSizes[4]) / periodSec;
				long sr = (sendRawSize - lastSizes[5]) / periodSec;
				var operates = new OutLong();
				var outBufSize = new OutLong();
				service.foreach(socket -> {
					operates.value += socket.getOperateSize();
					outBufSize.value += socket.getOutputBufferSize();
				});
				operates.value /= periodSec;
				outBufSize.value /= periodSec;
				logger.info("{}.{}.stat: select={}/{}, recv={}/{}, send={}/{}, sendRaw={}, sockets={}, ops={}, outBuf={}",
						service.getName(), service.getInstanceName(), sn, selectors.getCount(), rs, rc, ss, sc, sr,
						service.getSocketCount(), operates.value, outBufSize.value);
			}
			lastSizes[0] = selectCount;
			lastSizes[1] = recvCount;
			lastSizes[2] = recvSize;
			lastSizes[3] = sendCount;
			lastSizes[4] = sendSize;
			lastSizes[5] = sendRawSize;
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
	private final HashSet<Object> excludeRunKeys = new HashSet<>(); // value: Class or others
	private final LongHashSet excludeProtocolTypeIds = new LongHashSet(); // value: typeId
	private final DecimalFormat numFormatter = new DecimalFormat("#,###");
	private @NotNull String lastLog = "";
	private long lastLogTime = System.currentTimeMillis();
	private long lastCpuTime = osBean.getProcessCpuTime();
	private int clearSerial;
	private @Nullable ScheduledFuture<?> scheduleFuture;

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

	// 只能在启动统计前调用
	public boolean addExcludeRunKey(@NotNull String key) {
		lock();
		try {
			return excludeRunKeys.add(key);
		} finally {
			unlock();
		}
	}

	// 只能在启动统计前调用
	public boolean addExcludeRunKey(@NotNull Class<?> cls) {
		lock();
		try {
			return excludeRunKeys.add(cls);
		} finally {
			unlock();
		}
	}

	// 只能在启动统计前调用
	public boolean addExcludeProtocolTypeId(long typeId) {
		lock();
		try {
			return excludeProtocolTypeIds.add(typeId);
		} finally {
			unlock();
		}
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
		if (excludeProtocolTypeIds.contains(typeId))
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
		if (!excludeProtocolTypeIds.contains(typeId)) {
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

	@Override
	public @NotNull TableInfo getOrAddTableInfo(long tableId) {
		return tableInfoMap.computeIfAbsent(tableId, k -> {
			var tableName = TableKey.tables.get(k);
			return new TableInfo(tableName != null ? tableName : String.valueOf(k));
		});
	}

	public @NotNull String getLastLog() {
		return lastLog;
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
				scheduleFuture = f = Task.scheduleUnsafe(periodMs, periodMs, () -> logger.info(getLogAndReset()));
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
		clearSerial++;
		runInfoMap.clear();
		protocolInfoMap.clear();
		procedureInfoMap.clear();
		tableInfoMap.clear();
		for (var ci : countInfos) {
			ci.reset();
			ci.lastCount = 0;
		}
	}

	public @NotNull String getLogAndReset() {
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
			var prList = new ArrayList<ProcedureInfo>(procedureInfoMap.size());
			for (var it = procedureInfoMap.values().iterator(); it.hasNext(); ) {
				var pi = it.next();
				pi.resultMapLast = pi.resultMap;
				pi.resultMap = new LongConcurrentHashMap<>();
				var totalCount = 0L;
				var succCount = 0L;
				for (var it2 = pi.resultMapLast.entryIterator(); it2.moveToNext(); ) {
					var v = it2.value().sum();
					totalCount += v;
					if (it2.key() == 0)
						succCount = v;
				}
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

			var tList = new ArrayList<TableInfo>(tableInfoMap.size());
			for (var ti : tableInfoMap) {
				if (ti.checkpointAndReset())
					tList.add(ti);
			}
			sb.append(" [table: ").append(tList.size()).append("]\n");
			int n = Math.min(tList.size(), PERF_COUNT);
			if (n > 0) {
				tList.sort((ti0, ti1) -> Long.signum(ti1.lockCount - ti0.lockCount));
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

			return lastLog = sb.toString();
		} finally {
			unlock();
		}
	}
}
