package Zeze.Util;

import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * 基于固定数量哈希分桶的 one-by-one 队列调度器：相同 key 哈希映射到同一队列串行执行，不回收队列。
 * 构造时可通过 concurrencyLevel 控制总桶数（向上取 2 的幂）。
 */
public final class TaskOneByOneByKey extends TaskOneByOneBase {
	private static final Logger logger = LogManager.getLogger(TaskOneByOneByKey.class);

	private final TaskOneByOneQueue [] concurrency;
	private final int hashMask;
	private final @Nullable Executor executor;

	public TaskOneByOneByKey(@Nullable Executor executor) {
		this(2048, executor);
	}

	public TaskOneByOneByKey() {
		this(2048, null);
	}

	public TaskOneByOneByKey(int concurrencyLevel) {
		this(concurrencyLevel, null);
	}

	public TaskOneByOneByKey(int concurrencyLevel, @Nullable Executor executor) {
		this.executor = executor;
		if (concurrencyLevel < 1 || concurrencyLevel > 0x4000_0000)
			throw new IllegalArgumentException("Illegal concurrencyLevel: " + concurrencyLevel);

		int capacity = 1;
		while (capacity < concurrencyLevel)
			capacity <<= 1;
		concurrency = new TaskOneByOneQueue[capacity];
		for (int i = 0; i < concurrency.length; i++)
			concurrency[i] = new TaskOneByOneQueue(executor);
		hashMask = capacity - 1;
	}

	public int getConcurrencyLevel() {
		return concurrency.length;
	}

	public int getQueueSize(int index) {
		return Integer.compareUnsigned(index, concurrency.length) < 0 ? concurrency[index].size() : -1; // 可能有并发问题导致结果不准确,但通常问题不大
	}

	public @Nullable Executor getExecutor() {
		return executor;
	}

	@Override
	protected void execute(int key, TaskOneByOneQueue.Task task) {
		executeAndUnlock(getAndLockQueueWithHash(key), task);
	}

	@Override
	protected void execute(long key, TaskOneByOneQueue.Task task) {
		executeAndUnlock(getAndLockQueueWithHash(Long.hashCode(key)), task);
	}

	@Override
	protected TaskOneByOneQueue getAndLockQueue(Object key) {
		return getAndLockQueueWithHash(key.hashCode());
	}

	private TaskOneByOneQueue getAndLockQueueWithHash(int hash) {
		var queue = concurrency[hash(hash) & hashMask];
		queue.lock();
		return queue;
	}

	public void shutdown() {
		shutdown(true);
	}

	public void shutdown(boolean cancel) {
		for (var ts : concurrency)
			ts.shutdown(cancel);
		try {
			for (var ts : concurrency)
				ts.waitComplete();
		} catch (InterruptedException e) {
			logger.error("Shutdown interrupted", e);
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		for (int i = 0; i < concurrency.length; i++) {
			var s = concurrency[i].toString();
			if (s.length() > 2)
				sb.append(i).append(": ").append(s).append('\n');
		}
		return sb.toString();
	}

	/**
	 * 对 hashCode 做二次扰动，降低 2 的幂次桶表下的低位碰撞。
	 *
	 * @see java.util.HashMap
	 */
	private static int hash(int _h) {
		int h = _h;
		h ^= (h >>> 20) ^ (h >>> 12);
		return (h ^ (h >>> 7) ^ (h >>> 4));
	}
}
