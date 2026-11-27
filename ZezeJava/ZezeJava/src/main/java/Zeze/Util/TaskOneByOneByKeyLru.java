package Zeze.Util;

import org.jspecify.annotations.Nullable;


import java.util.concurrent.Executor;

/**
 * 基于 {@link ConcurrentLruLike} 的按 key 独立队列调度器：每个 key 拥有独立串行队列，
 * 空闲时由 LRU 淘汰回收；并发活跃 key 数受 capacity 上限约束。
 */
public class TaskOneByOneByKeyLru extends TaskOneByOneBase {
	private final ConcurrentLruLike<Object, TaskOneByOneQueue> queues;
	private final @Nullable Executor executor;

	public TaskOneByOneByKeyLru() {
		this("Zeze.Util.TaskOneByOneMapKey", 10_0000, null);
	}

	public TaskOneByOneByKeyLru(@Nullable Executor executor) {
		this("Zeze.Util.TaskOneByOneMapKey", 10_0000, executor);
	}

	public TaskOneByOneByKeyLru(String name, int capacity) {
		this(name, capacity, null);
	}

	public TaskOneByOneByKeyLru(String name, int capacity, @Nullable Executor executor) {
		this.queues = new ConcurrentLruLike<>(name, capacity, this::tryRemove);
		this.executor = executor;
	}

	private boolean tryRemove(Object key, TaskOneByOneQueue queue) {
		queue.lock();
		return tryRemoveUnderLock(key, queue);
	}

	private boolean tryRemoveUnderLock(Object key, TaskOneByOneQueue queue) {
		try {
			if (queue.sizeUnderLock() == 0) {
				queue.setRemoved();
				// 条件删除：仅当映射仍指向本队列时移除，避免误删并发新建的同名队列
				return queues.remove(key, queue);
			}
			return false;
		} finally {
			queue.unlock();
		}
	}

	@Override
	protected TaskOneByOneQueue getAndLockQueue(Object key) {
		while (true) {
			var queue = queues.getOrAdd(key, () -> new TaskOneByOneQueue(executor));
			queue.lock();
			if (!queue.isRemoved()) // 防止获得删除掉的queue.
				return queue;
			queue.unlock();
		}
	}

	public boolean tryRemoveQueue(Object key) {
		var queue = getAndLockQueue(key);
		return tryRemoveUnderLock(key, queue);
	}
}
