package Zeze.Services;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import Zeze.Net.Protocol;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GlobalCacheManagerPerf {
	private static final int ACQUIRE_STATE_COUNT = 3;
	private static final Logger logger = LogManager.getLogger(GlobalCacheManagerPerf.class);

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

	GlobalCacheManagerPerf(AtomicLong serialIdGenerator) {
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

	void onAcquireBegin(Protocol<?> rpc, int state) {
		if ((state & 0xffff_ffffL) < ACQUIRE_STATE_COUNT) {
			if (acquires.put(rpc, System.nanoTime()) != null)
				logger.warn("onAcquireBegin again");
		}
	}

	void onAcquireEnd(Protocol<?> rpc, int state) {
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

	void onReduceBegin(Protocol<?> rpc) {
		if (reduces.put(rpc, System.nanoTime()) != null)
			logger.warn("onReduceBegin again");
	}

	void onReduceCancel(Protocol<?> rpc) {
		reduces.remove(rpc);
	}

	void onReduceEnd(Protocol<?> rpc) {
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
		}
	}

	void onOthers(String info) {
		others.computeIfAbsent(info, __ -> new LongAdder()).increment();
	}

	private void report() {
		long curSerialId = serialIdGenerator.get();
		long serialIds = curSerialId - lastSerialId;
		lastSerialId = curSerialId;

		if ((serialIds | totalReduceCount.sum() | acquires.size() | reduces.size()) == 0) {
			int i = 0;
			for (; i < ACQUIRE_STATE_COUNT; i++) {
				if (totalAcquireCounts[i].sum() != 0)
					break;
			}
			if (i == ACQUIRE_STATE_COUNT)
				return;
		}

		var sb = new StringBuilder().append("SerialIds = ").append(serialIds).append('\n');
		for (int i = 0; i < ACQUIRE_STATE_COUNT; i++) {
			long count = totalAcquireCounts[i].sum();
			sb.append("Acquires[").append(i).append("] = ").append(count);
			if (count > 0) {
				sb.append(", ").append(totalAcquireTimes[i].sum() / count / 1_000).append(" us/acquire, max: ")
						.append(maxAcquireTimes[i].get() / 1_000_000).append(" ms");
			}
			for (var e : totalAcquireResults[i].entrySet())
				sb.append(", r=").append(e.getKey()).append(':').append(e.getValue().sum());
			sb.append('\n');
		}
		long count = totalReduceCount.sum();
		sb.append("Reduces = ").append(count);
		if (count > 0) {
			sb.append(", ").append(totalReduceTime.sum() / count / 1_000).append(" us/reduce, max: ")
					.append(maxReduceTime.get() / 1_000_000).append(" ms");
			for (var e : totalReduceResults.entrySet())
				sb.append(", r=").append(e.getKey()).append(':').append(e.getValue().sum());
		}
		sb.append("\nAcquire/Reduce Pendings = ").append(acquires.size()).append(" / ")
				.append(reduces.size()).append('\n');
		for (var e : others.entrySet())
			sb.append(e.getKey()).append(" = ").append(e.getValue().sum()).append('\n');
		logger.info("\n{}", sb.toString());

		for (int i = 0; i < ACQUIRE_STATE_COUNT; i++) {
			totalAcquireCounts[i].reset();
			totalAcquireTimes[i].reset();
			maxAcquireTimes[i].set(0);
			totalAcquireResults[i].clear();
		}
		totalReduceCount.reset();
		totalReduceTime.reset();
		maxReduceTime.set(0);
		totalReduceResults.clear();
		others.clear();
	}
}
