package Zeze.Services;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Net.Protocol;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class GlobalCacheManagerPerf extends ReentrantLock {
	private static final int ACQUIRE_STATE_COUNT = 3;
	private static final Logger logger = LogManager.getLogger(GlobalCacheManagerPerf.class);

	private final String perfName;
	private final AtomicLong serialIdGenerator;
	private long lastSerialId;

	private final ConcurrentHashMap<Protocol<?>, Long> acquires = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Protocol<?>, Long> reduces = new ConcurrentHashMap<>();

	private final LongAdder[] totalAcquireCounts = new LongAdder[ACQUIRE_STATE_COUNT];
	private final LongAdder[] totalAcquireTimes = new LongAdder[ACQUIRE_STATE_COUNT];
	private final AtomicLong[] maxAcquireTimes = new AtomicLong[ACQUIRE_STATE_COUNT];
	private final LongAdder totalReduceCount = new LongAdder();
	private final LongAdder totalReduceTime = new LongAdder();
	private final AtomicLong maxReduceTime = new AtomicLong();
	@SuppressWarnings("unchecked")
	private final ConcurrentSkipListMap<Long, LongAdder>[] totalAcquireResults = new ConcurrentSkipListMap[ACQUIRE_STATE_COUNT];
	private final ConcurrentSkipListMap<Long, LongAdder> totalReduceResults = new ConcurrentSkipListMap<>();
	private final ConcurrentSkipListMap<String, LongAdder> others = new ConcurrentSkipListMap<>();

	GlobalCacheManagerPerf(@NotNull String perfName, @NotNull AtomicLong serialIdGenerator) {
		this.perfName = perfName;
		this.serialIdGenerator = serialIdGenerator;
		lastSerialId = serialIdGenerator.get();
		for (int i = 0; i < ACQUIRE_STATE_COUNT; i++) {
			totalAcquireCounts[i] = new LongAdder();
			totalAcquireTimes[i] = new LongAdder();
			maxAcquireTimes[i] = new AtomicLong();
			totalAcquireResults[i] = new ConcurrentSkipListMap<>();
		}
		Task.schedule(1000, 1000, this::report);
	}

	void onAcquireBegin(@NotNull Protocol<?> rpc, int state) {
		if (Integer.compareUnsigned(state, ACQUIRE_STATE_COUNT) < 0
				&& acquires.put(rpc, System.nanoTime()) != null)
			logger.warn("onAcquireBegin again");
	}

	void onAcquireEnd(@NotNull Protocol<?> rpc, int state) {
		var beginTime = acquires.remove(rpc);
		if (beginTime != null) {
			var time = System.nanoTime() - beginTime;
			totalAcquireCounts[state].increment();
			totalAcquireTimes[state].add(time);
			var maxAcquireTime = maxAcquireTimes[state];
			long maxTime;
			do
				maxTime = maxAcquireTime.get();
			while (time > maxTime && !maxAcquireTime.compareAndSet(maxTime, time));
			if (rpc.getResultCode() != 0)
				totalAcquireResults[state].computeIfAbsent(rpc.getResultCode(), __ -> new LongAdder()).increment();
		}
	}

	void onReduceBegin(@NotNull Protocol<?> rpc) {
		if (reduces.put(rpc, System.nanoTime()) != null)
			logger.warn("already onReduceBegin: {}", rpc);
	}

	void onReduceCancel(@NotNull Protocol<?> rpc) {
		if (reduces.remove(rpc) == null)
			logger.warn("already onReduceCancel: {}", rpc);
	}

	void onReduceEnd(@NotNull Protocol<?> rpc) {
		var beginTime = reduces.remove(rpc);
		if (beginTime != null) {
			var time = System.nanoTime() - beginTime;
			totalReduceCount.increment();
			totalReduceTime.add(time);
			long maxTime;
			do
				maxTime = maxReduceTime.get();
			while (time > maxTime && !maxReduceTime.compareAndSet(maxTime, time));
			if (rpc.getResultCode() != 0)
				totalReduceResults.computeIfAbsent(rpc.getResultCode(), __ -> new LongAdder()).increment();
		} else
			logger.warn("already onReduceEnd: {}", rpc);
	}

	void onOthers(@NotNull String info) {
		others.computeIfAbsent(info, __ -> new LongAdder()).increment();
	}

	private void report() {
		lock();
		try {
			long curSerialId = serialIdGenerator.get();
			long serialIds = curSerialId - lastSerialId;
			lastSerialId = curSerialId;
			long totalReduceCountSum = totalReduceCount.sumThenReset();
			long acquiresSize = acquires.size();
			long reducesSize = reduces.size();
			long[] totalAcquireCounts0 = new long[ACQUIRE_STATE_COUNT];
			for (int i = 0; i < ACQUIRE_STATE_COUNT; i++)
				totalAcquireCounts0[i] = totalAcquireCounts[i].sumThenReset();

			if ((serialIds | totalReduceCountSum | acquiresSize | reducesSize) == 0) {
				int i = 0;
				for (; i < ACQUIRE_STATE_COUNT; i++) {
					if (totalAcquireCounts0[i] != 0)
						break;
				}
				if (i == ACQUIRE_STATE_COUNT)
					return;
			}

			var sb = new StringBuilder().append("SerialIds = ").append(serialIds).append('\n');
			for (int i = 0; i < ACQUIRE_STATE_COUNT; i++) {
				long count = totalAcquireCounts0[i];
				sb.append("Acquires[").append(i).append("] = ").append(count);
				if (count > 0) {
					sb.append(", ").append(totalAcquireTimes[i].sumThenReset() / count / 1_000).append(" us/acquire, max: ")
							.append(maxAcquireTimes[i].getAndSet(0) / 1_000_000).append(" ms");
				}
				for (var e : totalAcquireResults[i].entrySet())
					sb.append(", r=").append(e.getKey()).append(':').append(e.getValue().sum());
				totalAcquireResults[i].clear();
				sb.append('\n');
			}
			sb.append("Reduces = ").append(totalReduceCountSum);
			if (totalReduceCountSum > 0) {
				sb.append(", ").append(totalReduceTime.sumThenReset() / totalReduceCountSum / 1_000)
						.append(" us/reduce, max: ").append(maxReduceTime.getAndSet(0) / 1_000_000).append(" ms");
				for (var e : totalReduceResults.entrySet())
					sb.append(", r=").append(e.getKey()).append(':').append(e.getValue().sum());
				totalReduceResults.clear();
			}
			sb.append("\nAcquire/Reduce Pendings = ").append(acquiresSize).append(" / ").append(reducesSize).append('\n');
			for (var e : others.entrySet())
				sb.append(e.getKey()).append(" = ").append(e.getValue().sum()).append('\n');
			others.clear();
			var es = Task.getThreadPool();
			if (es instanceof ThreadPoolExecutor) {
				var queueSize = ((ThreadPoolExecutor)es).getQueue().size();
				if (queueSize != 0)
					sb.append("ThreadPoolQueueSize = ").append(queueSize).append('\n');
			}
			logger.info("{}\n{}", perfName, sb.toString());
		} finally {
			unlock();
		}
	}
}
