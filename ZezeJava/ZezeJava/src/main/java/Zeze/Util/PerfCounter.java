package Zeze.Util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import Zeze.Builtin.Provider.Send;
import Zeze.Net.FamilyClass;
import Zeze.Net.Protocol;
import Zeze.Serialize.ByteBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PerfCounter {
	private static final Logger logger = LogManager.getLogger(PerfCounter.class);

	private static class RunInfo {
		static final int MAX_IDLE_COUNT = 10; // 最多几轮没有收集到信息就自动清除该条目

		final @NotNull String name;
		final LongAdder procCount = new LongAdder(); // 处理次数
		final LongAdder procTime = new LongAdder(); // 处理时间(ns)
		long lastProcCount;
		long lastProcTime;
		int idleCount; // 没收集到信息的轮数

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

	public static final int PERF_COUNT = Integer.parseInt(System.getProperty("perfCount", "20")); // 输出条目数
	public static final int PERF_PERIOD = Integer.parseInt(System.getProperty("perfPeriod", "100")); // 输出周期(秒)
	public static final boolean ENABLE_PERF = PERF_COUNT > 0;
	public static final PerfCounter instance = new PerfCounter(); // 通常用全局单例就够用了,也可以创建新实例

	private final ConcurrentHashMap<Object, RunInfo> runInfoMap = new ConcurrentHashMap<>(); // key: Class or others
	private final LongConcurrentHashMap<ProtocolInfo> protocolInfoMap = new LongConcurrentHashMap<>(); // key: typeId
	private final HashSet<Object> excludeRunKeys = new HashSet<>(); // value: Class or others
	private final LongHashSet excludeProtocolTypeIds = new LongHashSet(); // value: typeId
	private final DecimalFormat numFormatter = new DecimalFormat("#,###");
	private @NotNull String lastLog = "";
	private long lastLogTime = System.currentTimeMillis();

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
		for (; ; ) {
			var ri = runInfoMap.get(key);
			if (ri != null) {
				ri.procCount.increment();
				ri.procTime.add(timeNs);
				return;
			}
			runInfoMap.putIfAbsent(key,
					new RunInfo(key instanceof Class ? ((Class<?>)key).getName() : String.valueOf(key)));
		}
	}

	public void addRecvInfo(long typeId, @Nullable Class<?> cls, int size, long timeNs) {
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

	public void addSendInfo(byte @NotNull [] bytes, int offset, int length) {
		while (length >= 12) {
			int moduleId = ByteBuffer.ToInt(bytes, offset);
			int protocolId = ByteBuffer.ToInt(bytes, offset + 4);
			int size = ByteBuffer.ToInt(bytes, offset + 8);
			if (size < 0) {
				logger.warn("addSendInfo: moduleId={}, protocolId={}, size={} < 0", moduleId, protocolId, size);
				break;
			}
			size += Protocol.HEADER_SIZE;
			var typeId = Protocol.makeTypeId(moduleId, protocolId);
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
			if (typeId == Send.TypeId_)
				addSendRpc(bytes, offset + Protocol.HEADER_SIZE, length - Protocol.HEADER_SIZE);
			offset += size;
			length -= size;
		}
	}

	private void addSendRpc(byte @NotNull [] bytes, int offset, int length) {
		try {
			var bb = ByteBuffer.Wrap(bytes, offset, length);
			var header = bb.ReadInt();
			if ((header & FamilyClass.FamilyClassMask) != FamilyClass.Request)
				return;
			if ((header & FamilyClass.BitResultCode) != 0)
				bb.SkipLong(); // resultCode
			bb.SkipLong(); // sessionId

			int t = bb.ReadByte();
			int i = bb.ReadTagSize(t);
			if (i == 1) { // linkSids
				bb.SkipUnknownField(t);
				i += bb.ReadTagSize(t = bb.ReadByte());
			}
			if (i == 2) { // protocolType
				bb.SkipUnknownField(t);
				i += bb.ReadTagSize(t = bb.ReadByte());
			}
			if (i == 3 && (t & ByteBuffer.TAG_MASK) == ByteBuffer.BYTES) { // protocolWholeData
				int n = bb.ReadUInt();
				addSendInfo(bytes, bb.ReadIndex, Math.min(n, bb.size()));
			}
		} catch (Exception e) {
			logger.warn("addSendRpc: decode Send failed", e);
		}
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
		var curTime = System.currentTimeMillis();
		var time = curTime - lastLogTime;

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
		var sb = new StringBuilder(100 + 50 * 3 * PERF_COUNT).append("count last ").append(time).append("ms:\n")
				.append(" [run: ").append(procCountAll).append(',').append(' ')
				.append(procTimeAll / 1_000_000).append("ms]\n");
		rList.sort((ri0, ri1) -> Long.signum(ri1.lastProcTime - ri0.lastProcTime));
		for (int i = 0, n = Math.min(rList.size(), PERF_COUNT); i < n; i++) {
			var ri = rList.get(i);
			var perTime = ri.lastProcCount > 0 ? ri.lastProcTime / ri.lastProcCount : 0;
			sb.append(' ').append(' ').append(ri.name).append(':').append(' ').append(ri.lastProcTime / 1_000_000)
					.append("ms = ").append(ri.lastProcCount).append(" * ")
					.append(numFormatter.format(perTime)).append("ns\n");
		}

		procCountAll = 0;
		procTimeAll = 0;
		var recvSizeAll = 0L;
		var sendCountAll = 0L;
		var sendSizeAll = 0L;
		var pList = new ArrayList<ProtocolInfo>(protocolInfoMap.size());
		for (var it = protocolInfoMap.iterator(); it.hasNext(); ) {
			var pi = it.next();
			pi.lastProcCount = pi.procCount.sumThenReset();
			pi.lastSendCount = pi.sendCount.sumThenReset();
			if ((pi.lastProcCount | pi.lastSendCount) == 0) {
				if (++pi.idleCount >= RunInfo.MAX_IDLE_COUNT)
					it.remove();
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
		sb.append(" [recv: ").append(procCountAll).append(',').append(' ').append(recvSizeAll / 1000).append("K, ")
				.append(procTimeAll / 1_000_000).append("ms]\n");
		pList.sort((pi0, pi1) -> Long.signum(pi1.lastProcTime - pi0.lastProcTime));
		for (int i = 0, n = Math.min(pList.size(), PERF_COUNT); i < n; i++) {
			var pi = pList.get(i);
			var perTime = 0L;
			var perSize = 0L;
			if (pi.lastProcCount > 0) {
				perTime = pi.lastProcTime / pi.lastProcCount;
				perSize = pi.lastRecvSize / pi.lastProcCount;
			}
			sb.append(' ').append(' ').append(pi.name).append(':').append(' ').append(pi.lastProcTime / 1_000_000)
					.append("ms = ").append(pi.lastProcCount).append(" * ")
					.append(numFormatter.format(perTime)).append("ns,")
					.append(numFormatter.format(perSize)).append('B').append('\n');
		}
		sb.append(" [send: ").append(sendCountAll).append(',').append(' ').append(sendSizeAll / 1000).append("K]\n");
		pList.sort((pi0, pi1) -> Long.signum(pi1.lastSendSize - pi0.lastSendSize));
		for (int i = 0, n = Math.min(pList.size(), PERF_COUNT); i < n; i++) {
			var pi = pList.get(i);
			if (pi.lastSendSize == 0)
				break;
			var perSize = pi.lastSendCount > 0 ? pi.lastSendSize / pi.lastSendCount : 0;
			sb.append(' ').append(' ').append(pi.name).append(':').append(' ').append(pi.lastSendSize / 1_000)
					.append("K = ").append(pi.lastSendCount).append(" * ")
					.append(numFormatter.format(perSize)).append('B').append('\n');
		}

		lastLogTime = curTime;
		return lastLog = sb.toString();
	}
}
