package Zeze.Util;

import java.util.concurrent.Executor;

/*
 * 每个任务关联一个Key,每一个Key的任务是一个队列,最多只有一个任务在执行.
 * 最终某个Key相关的任务顺序一个接一个的执行.
 *
 * 1. TaskOneByOneByKey(旧的)
 * 固定concurrency级别,每个concurrency一个队列,
 * executeTask(key, task) {
 *     index=hash(key)%concurrency.length;
 *     queue=concurrency[index];
 *     queue.execute(task);
 * }
 * 这个实现符合上面的功能定义,
 * 优点: concurrency不需要很大,默认1024,内存少,稳定.
 * 缺点: 多个key会被映射到相同的队列,
 * 未来: 以后virtual-thread可以增加concurrency.
 *
 * 2. TaskOneByOneByKeyLru(This File)
 * ConcurrentLruLike<key, Queue> queues;
 * executeTask(key, task) {
 *     queue = queues.computeIfAbsent(key, Queue::new);
 *     queue.execute(task);
 * }
 *
 * 这个实现符合上面的功能定义,
 * 优点: 每个key有自己独立的队列,更符合需求.
 * 缺点: a)内存占用较大,默认capacity=10_0000;
 *      b)对需求有限制,同时存在的Key数量不能超过capacity.
 *      [当然]对于一台服务器来说,把capacity设置为100万,基本可以确定不会超出容量了.
 *      写这个第一需求就是为了把用户请求按账号排队.
 *      而一台服务器同时存在的需要执行的任务数量是有限的,
 *      受最大在线数量限制.
 * 未来: 以后virtual-thread不需要调整,默认size够大了.
 */
public class TaskOneByOneByKeyLru extends TaskOneByOneBase {
	private final ConcurrentLruLike<Object, TaskOneByOneQueue> queues;
	private final Executor executor;

	public TaskOneByOneByKeyLru() {
		this("Zeze.Util.TaskOneByOneMapKey", 10_0000, null);
	}

	public TaskOneByOneByKeyLru(Executor executor) {
		this("Zeze.Util.TaskOneByOneMapKey", 10_0000, executor);
	}

	public TaskOneByOneByKeyLru(String name, int capacity) {
		this(name, capacity, null);
	}

	public TaskOneByOneByKeyLru(String name, int capacity, Executor executor) {
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
				queues.remove(key);
				return true;
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
