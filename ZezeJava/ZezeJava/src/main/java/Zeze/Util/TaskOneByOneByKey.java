package Zeze.Util;

import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 对每个相同的key，最多只提交一个 Task.Run。
 * <p>
 * 说明：
 * 严格的来说应该对每个key建立一个队列，但是key可能很多，就需要很多队列。
 * 如果队列为空，需要回收队列，会产生很多垃圾回收对象。
 * 具体的实现对于相同的key.hash使用相同的队列。
 * 固定总的队列数，不回收队列。
 * 构造的时候，可以通过参数控制总的队列数量。
 */
public final class TaskOneByOneByKey extends TaskOneByOneBase {
	private static final Logger logger = LogManager.getLogger(TaskOneByOneByKey.class);

	private final TaskOneByOneQueue @NotNull [] concurrency;
	private final int hashMask;
	private final @Nullable Executor executor;

	public Executor getExecutor() {
		return executor;
	}

	public TaskOneByOneByKey(@Nullable Executor executor) {
		this(1024, executor);
	}

	public TaskOneByOneByKey() {
		this(1024, null);
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

	@Override
	protected void execute(int key, TaskOneByOneQueue.Task task) {
		executeAndUnlock(getAndLockQueueWithHash(key), task);
	}

	@Override
	protected void execute(long key, TaskOneByOneQueue.Task task) {
		executeAndUnlock(getAndLockQueueWithHash(Long.hashCode(key)), task);
	}

	@Override
	protected @NotNull TaskOneByOneQueue getAndLockQueue(@NotNull Object key) {
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
		}
	}

	@Override
	public @NotNull String toString() {
		var sb = new StringBuilder();
		for (int i = 0; i < concurrency.length; i++) {
			var s = concurrency[i].toString();
			if (s.length() > 2)
				sb.append(i).append(": ").append(s).append('\n');
		}
		return sb.toString();
	}

	/**
	 * Applies a supplemental hash function to a given hashCode, which defends
	 * against poor quality hash functions. This is critical because HashMap uses
	 * power-of-two length hash tables, that otherwise encounter collisions for
	 * hashCodes that do not differ in lower bits. Note: Null keys always map to
	 * hash 0, thus index 0.
	 *
	 * @see java.util.HashMap
	 */
	private static int hash(int _h) {
		int h = _h;
		h ^= (h >>> 20) ^ (h >>> 12);
		return (h ^ (h >>> 7) ^ (h >>> 4));
	}
}
