package Zeze.Util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import Zeze.Net.Protocol;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public final class PerfCounter {
	private static final Logger logger = LogManager.getLogger(PerfCounter.class);

	private static class RunInfo {
		final @NotNull String name;
		final LongAdder procCount = new LongAdder(); // 处理次数
		final LongAdder procTime = new LongAdder(); // 处理时间(ns)
		long lastProcCount;
		long lastProcTime;

		RunInfo(String name) {
			this.name = name;
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

	public static final int PERF_COUNT = Integer.parseInt(System.getProperty("perfCount", "10")); // 输出条目数
	public static final int PERF_PERIOD = Integer.parseInt(System.getProperty("perfPeriod", "10")); // 输出周期(秒)
	public static final boolean ENABLE_PERF = PERF_COUNT > 0;
	public static final PerfCounter instance = new PerfCounter(); // 通常用全局单例就够用了,也可以创建新实例

	private final ConcurrentHashMap<Object, RunInfo> runInfoMap = new ConcurrentHashMap<>(); // key: Class or others
	private final LongConcurrentHashMap<ProtocolInfo> protocolInfoMap = new LongConcurrentHashMap<>(); // key: typeId
	private final HashSet<Object> excludeRunKeys = new HashSet<>(); // value: Class or others
	private final LongHashSet excludeProtocolTypeIds = new LongHashSet(); // value: typeId
	private final DecimalFormat numFormatter = new DecimalFormat("#,###");
	private @NotNull String lastLog = "";
	private long lastLogTime;

	// 只能在启动统计前调用
	public synchronized boolean addExcludeRunKey(@NotNull String key) {
		return excludeRunKeys.add(key);
	}

	// 只能在启动统计前调用
	public synchronized boolean addExcludeRunKey(@NotNull Class<?> cls) {
		return excludeRunKeys.add(cls);
	}

	// 只能在启动统计前调用
	public synchronized boolean addExcludeProtocolTypeId(long typeId) {
		return excludeProtocolTypeIds.add(typeId);
	}

	public void addRunInfo(@NotNull Object key, long timeNs) {
		if (excludeRunKeys.contains(key))
			return;
		RunInfo ri;
		for (; ; ) {
			ri = runInfoMap.get(key);
			if (ri != null)
				break;
			runInfoMap.putIfAbsent(key,
					new RunInfo(key instanceof Class ? ((Class<?>)key).getName() : String.valueOf(key)));
		}
		ri.procCount.increment();
		ri.procTime.add(timeNs);
	}

	public void addRecvInfo(@NotNull Protocol<?> protocol, int size, long timeNs) {
		var typeId = protocol.getTypeId();
		if (excludeProtocolTypeIds.contains(typeId))
			return;
		ProtocolInfo pi;
		for (; ; ) {
			pi = protocolInfoMap.get(typeId);
			if (pi != null)
				break;
			protocolInfoMap.putIfAbsent(typeId, new ProtocolInfo(protocol.getClass().getName()));
		}
		pi.procCount.increment();
		pi.procTime.add(timeNs);
		pi.recvSize.add(size);
	}

	public void addSendInfo(@NotNull Protocol<?> protocol, int size) {
		var typeId = protocol.getTypeId();
		if (excludeProtocolTypeIds.contains(typeId))
			return;
		ProtocolInfo pi;
		for (; ; ) {
			pi = protocolInfoMap.get(typeId);
			if (pi != null)
				break;
			protocolInfoMap.putIfAbsent(typeId, new ProtocolInfo(protocol.getClass().getName()));
		}
		pi.sendCount.increment();
		pi.sendSize.add(size);
	}

	public @NotNull String getLastLog() {
		return lastLog;
	}

	public long getLastLogTime() {
		return lastLogTime;
	}

	public void startScheduledLog() {
		if (ENABLE_PERF) {
			long periodMs = PERF_PERIOD * 1000L;
			Task.scheduleUnsafe(periodMs, periodMs, () -> logger.info(getLogAndReset()));
		}
	}

	public void resetCounter() {
		runInfoMap.clear();
		protocolInfoMap.clear();
	}

	public synchronized @NotNull String getLogAndReset() {
		if (!ENABLE_PERF)
			return lastLog = "";
		var procCountAll = 0L;
		var procTimeAll = 0L;
		var rList = new ArrayList<RunInfo>(runInfoMap.size());
		for (var ri : runInfoMap.values()) {
			ri.lastProcCount = ri.procCount.sumThenReset();
			ri.lastProcTime = ri.procTime.sumThenReset();
			procCountAll += ri.lastProcCount;
			procTimeAll += ri.lastProcTime;
			rList.add(ri);
		}
		var sb = new StringBuilder(100 + 50 * 3 * PERF_COUNT).append("PerfCounter:\n")
				.append(" [run: ").append(procCountAll).append(',').append(' ')
				.append(procTimeAll / 1_000_000).append("ms]\n");
		rList.sort((ri0, ri1) -> Long.signum(ri1.lastProcTime - ri0.lastProcTime));
		int n = Math.min(rList.size(), PERF_COUNT);
		for (int i = 0; i < n; i++) {
			var ri = rList.get(i);
			var perTime = ri.lastProcCount > 0 ? ri.lastProcTime / ri.lastProcCount : 0;
			sb.append(' ').append(' ').append(ri.name).append(':').append(' ').append(ri.lastProcTime / 1_000_000)
					.append("ms = ").append(ri.lastProcCount).append('*')
					.append(numFormatter.format(perTime)).append("ns\n");
		}

		procCountAll = 0;
		procTimeAll = 0;
		var recvSizeAll = 0L;
		var sendCountAll = 0L;
		var sendSizeAll = 0L;
		var pList = new ArrayList<ProtocolInfo>(protocolInfoMap.size());
		for (var pi : protocolInfoMap) {
			pi.lastProcCount = pi.procCount.sumThenReset();
			pi.lastProcTime = pi.procTime.sumThenReset();
			pi.lastRecvSize = pi.recvSize.sumThenReset();
			pi.lastSendCount = pi.sendCount.sumThenReset();
			pi.lastSendSize = pi.sendSize.sumThenReset();
			procCountAll += pi.lastProcCount;
			procTimeAll += pi.lastProcTime;
			recvSizeAll += pi.lastRecvSize;
			sendCountAll += pi.lastSendCount;
			sendSizeAll += pi.lastSendSize;
			pList.add(pi);
		}
		sb.append(" [recv: ").append(procCountAll).append(',').append(' ').append(recvSizeAll / 1000).append("K, ")
				.append(procTimeAll / 1_000_000).append("ms]\n");
		pList.sort((ri0, ri1) -> Long.signum(ri1.lastProcTime - ri0.lastProcTime));
		n = Math.min(pList.size(), PERF_COUNT);
		for (int i = 0; i < n; i++) {
			var pi = pList.get(i);
			var perTime = pi.lastProcCount > 0 ? pi.lastProcTime / pi.lastProcCount : 0;
			var perSize = pi.lastProcCount > 0 ? pi.lastRecvSize / pi.lastProcCount : 0;
			sb.append(' ').append(' ').append(pi.name).append(':').append(' ').append(pi.lastProcTime / 1_000_000)
					.append("ms = ").append(pi.lastProcCount).append('*')
					.append(numFormatter.format(perTime)).append("ns, ")
					.append(numFormatter.format(perSize)).append('B').append('\n');
		}
		sb.append(" [send: ").append(sendCountAll).append(',').append(' ').append(sendSizeAll / 1000).append("K]\n");
		pList.sort((ri0, ri1) -> Long.signum(ri1.lastSendSize - ri0.lastSendSize));
		n = Math.min(pList.size(), PERF_COUNT);
		for (int i = 0; i < n; i++) {
			var pi = pList.get(i);
			var perSize = pi.lastSendCount > 0 ? pi.lastSendSize / pi.lastSendCount : 0;
			sb.append(' ').append(' ').append(pi.name).append(':').append(' ').append(pi.lastSendSize / 1_000)
					.append("K = ").append(pi.lastSendCount).append('*')
					.append(numFormatter.format(perSize)).append('B').append('\n');
		}

		lastLogTime = System.currentTimeMillis();
		return lastLog = sb.toString();
	}
}
