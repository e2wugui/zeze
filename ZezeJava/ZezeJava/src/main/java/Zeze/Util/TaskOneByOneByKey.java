package Zeze.Util;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
public final class TaskOneByOneByKey {
	private static final Logger logger = LogManager.getLogger(TaskOneByOneByKey.class);

	private final TaskOneByOne[] concurrency;
	private final int hashMask;
	private final Executor executor;

	public TaskOneByOneByKey(Executor executor) {
		this(1024, executor);
	}

	public TaskOneByOneByKey() {
		this(1024, null);
	}

	public TaskOneByOneByKey(int concurrencyLevel) {
		this(concurrencyLevel, null);
	}

	public TaskOneByOneByKey(int concurrencyLevel, Executor executor) {
		this.executor = executor;
		if (concurrencyLevel < 1 || concurrencyLevel > 0x4000_0000)
			throw new IllegalArgumentException("Illegal concurrencyLevel: " + concurrencyLevel);

		int capacity = 1;
		while (capacity < concurrencyLevel)
			capacity <<= 1;
		concurrency = new TaskOneByOne[capacity];
		for (int i = 0; i < concurrency.length; i++)
			concurrency[i] = new TaskOneByOne();
		hashMask = capacity - 1;
	}

	public int getConcurrencyLevel() {
		return concurrency.length;
	}

	public int getQueueSize(int index) {
		return index >= 0 && index < concurrency.length ? concurrency[index].queue.size() : -1; // 可能有并发问题导致结果不准确,但通常问题不大
	}

	public static class Barrier {
		final Procedure procedure;
		private final Action0 cancelAction;
		private final ReentrantLock lock = new ReentrantLock();
		private int count;
		private boolean canceled = false;
		private final HashSet<TaskOneByOne.BatchTask> reached = new HashSet<>();

		public Barrier(Procedure action, int count, Action0 cancel) {
			procedure = action;
			cancelAction = cancel;
			this.count = count;
		}

		private void reachedRunNext() {
			for (var batch : reached)
				batch.runNext();
		}

		public boolean reach(TaskOneByOne.BatchTask batch, int sum) {
			lock.lock();
			try {
				if (canceled)
					return true;

				reached.add(batch);

				count -= sum;
				if (count > 0)
					return false;

				try {
					procedure.call();
				} catch (Exception ex) {
					logger.error("{} Run", procedure.getActionName(), ex);
				} finally {
					// 成功执行
					// 1. 触发所有桶的runNext，
					// 2. 自己也返回false，不再继续runNext。
					reachedRunNext();
				}
				return false; // 返回false
			} finally {
				lock.unlock();
			}
		}

		public void cancel() {
			lock.lock();
			try {
				if (canceled)
					return;

				canceled = true;
				try {
					if (null != cancelAction)
						cancelAction.run();
				} catch (Exception ex) {
					logger.error("{} Canceled", procedure.getActionName(), ex);
				} finally {
					// 取消的时候，
					// 1. 如果相关桶的任务已经执行，需要runNext。
					// 2. 如果相关桶的任务没有执行，不需要处理。相应的任务以后会发现已经取消，自动忽略执行。
					reachedRunNext();
				}
			} finally {
				lock.unlock();
			}
		}
	}

	public synchronized <T> void executeCyclicBarrier(Collection<T> keys, Procedure procedure, Action0 cancel, DispatchMode mode) {
		if (keys.isEmpty())
			throw new IllegalArgumentException("CyclicBarrier keys is empty.");

		var group = new HashMap<TaskOneByOne, OutInt>();
		int count = 0;
		for (var key : keys) {
			group.computeIfAbsent(bucket(key), __ -> new OutInt()).value++;
			count++;
		}
		var barrier = new Barrier(procedure, count, cancel);
		for (var e : group.entrySet()) {
			var sum = e.getValue().value;
			e.getKey().executeBarrier(barrier, sum, mode);
		}
	}

	public static class BarrierAction {
		private final String actionName;
		private final Action0 action;
		private final ReentrantLock lock = new ReentrantLock();
		private final Condition cond = lock.newCondition();
		private int count;
		private final Action0 cancel;
		private boolean canceled = false;

		public BarrierAction(String actionName, Action0 action, int count, Action0 cancel) {
			this.actionName = actionName;
			this.action = action;
			this.count = count;
			this.cancel = cancel;
		}

		public void reach(int sum) throws InterruptedException {
			lock.lock();
			try {
				if (canceled)
					return;

				count -= sum;
				if (count > 0)
					cond.await();
				else {
					try {
						action.run();
					} catch (Exception ex) {
						logger.error("{} Run", actionName, ex);
					} finally {
						cond.signalAll();
					}
				}
			} finally {
				lock.unlock();
			}
		}

		public void cancel() {
			lock.lock();
			try {
				if (canceled)
					return;

				canceled = true;
				try {
					if (null != cancel)
						cancel.run();
				} catch (Exception ex) {
					logger.error("{} Canceled", actionName, ex);
				} finally {
					cond.signalAll();
				}
			} finally {
				lock.unlock();
			}
		}
	}

	public synchronized <T> void executeCyclicBarrier(
			Collection<T> keys,
			String actionName,
			Action0 action,
			Action0 cancel,
			DispatchMode mode) {

		if (keys.isEmpty())
			throw new IllegalArgumentException("CyclicBarrier keys is empty.");

		var group = new HashMap<TaskOneByOne, OutInt>();
		int count = 0;
		for (var key : keys) {
			group.computeIfAbsent(bucket(key), __ -> new OutInt()).value++;
			count++;
		}
		var barrier = new BarrierAction(actionName, action, count, cancel);
		for (var e : group.entrySet()) {
			var sum = e.getValue().value;
			e.getKey().execute(() -> barrier.reach(sum), actionName, barrier::cancel, mode);
		}
	}

	public static class Batch<T> {
		private final AtomicInteger keysCount;
		private final Action1<T> action;
		private final Action0 batchEnd;

		public Batch(int keysSize, Action1<T> action, Action0 batchEnd) {
			this.keysCount = new AtomicInteger(keysSize);
			this.action = action;
			this.batchEnd = batchEnd;
		}

		public void run(T key) throws Exception {
			action.run(key);
			if (keysCount.decrementAndGet() == 0)
				batchEnd.run();
		}
	}

	public <T> void executeBatch(Collection<T> keys, Action1<T> action, Action0 batchEnd, DispatchMode mode) {
		var batch = new Batch<>(keys.size(), action, batchEnd);
		for (var key : keys)
			Execute(key, () -> batch.run(key), mode);
	}

	public void executeBatch(LongList keys, Action1<Long> action, Action0 batchEnd, DispatchMode mode) {
		var batch = new Batch<>(keys.size(), action, batchEnd);
		keys.foreach((key) -> Execute(key, () -> batch.run(key), mode));
	}

	public void Execute(Object key, Action0 action) {
		Execute(key.hashCode(), action, DispatchMode.Normal);
	}

	public void Execute(Object key, Action0 action, DispatchMode mode) {
		Execute(key.hashCode(), action, mode);
	}

	public void Execute(Object key, Action0 action, String name) {
		Execute(key.hashCode(), action, name, DispatchMode.Normal);
	}

	public void Execute(Object key, Action0 action, String name, DispatchMode mode) {
		Execute(key.hashCode(), action, name, mode);
	}

	public void Execute(Object key, Action0 action, String name, Action0 cancel, DispatchMode mode) {
		Execute(key.hashCode(), action, name, cancel, mode);
	}

	public void Execute(Object key, Func0<?> func) {
		Execute(key.hashCode(), func, DispatchMode.Normal);
	}

	public void Execute(Object key, Func0<?> func, DispatchMode mode) {
		Execute(key.hashCode(), func, mode);
	}

	public void Execute(Object key, Func0<?> func, String name) {
		Execute(key.hashCode(), func, name, DispatchMode.Normal);
	}

	public void Execute(Object key, Func0<?> func, String name, DispatchMode mode) {
		Execute(key.hashCode(), func, name, mode);
	}

	public void Execute(Object key, Func0<?> func, String name, Action0 cancel, DispatchMode mode) {
		Execute(key.hashCode(), func, name, cancel, mode);
	}

	public void Execute(Object key, Procedure procedure) {
		Execute(key.hashCode(), procedure, DispatchMode.Normal);
	}

	public void Execute(Object key, Procedure procedure, DispatchMode mode) {
		Execute(key.hashCode(), procedure, mode);
	}

	public void Execute(Object key, Procedure procedure, Action0 cancel, DispatchMode mode) {
		Execute(key.hashCode(), procedure, cancel, mode);
	}

	public void Execute(int key, Action0 action) {
		Execute(key, action, null, null, DispatchMode.Normal);
	}

	public void Execute(int key, Action0 action, DispatchMode mode) {
		Execute(key, action, null, null, mode);
	}

	public void Execute(int key, Action0 action, String name) {
		Execute(key, action, name, null, DispatchMode.Normal);
	}

	public void Execute(int key, Action0 action, String name, DispatchMode mode) {
		Execute(key, action, name, null, mode);
	}

	public void Execute(int key, Action0 action, String name, Action0 cancel, DispatchMode mode) {
		if (action == null)
			throw new IllegalArgumentException("null action");
		concurrency[hash(key) & hashMask].execute(action, name, cancel, mode);
	}

	private TaskOneByOne bucket(Object key) {
		return concurrency[hash(key.hashCode()) & hashMask];
	}

	public void Execute(int key, Func0<?> func) {
		Execute(key, func, null, null, DispatchMode.Normal);
	}

	public void Execute(int key, Func0<?> func, DispatchMode mode) {
		Execute(key, func, null, null, mode);
	}

	public void Execute(int key, Func0<?> func, String name) {
		Execute(key, func, name, null, DispatchMode.Normal);
	}

	public void Execute(int key, Func0<?> func, String name, DispatchMode mode) {
		Execute(key, func, name, null, mode);
	}

	public void Execute(int key, Func0<?> func, String name, Action0 cancel, DispatchMode mode) {
		if (func == null)
			throw new IllegalArgumentException("null func");
		concurrency[hash(key) & hashMask].execute(func, name, cancel, mode);
	}

	public void Execute(int key, Procedure procedure) {
		Execute(key, procedure, null, DispatchMode.Normal);
	}

	public void Execute(int key, Procedure procedure, DispatchMode mode) {
		Execute(key, procedure, null, mode);
	}

	public void Execute(int key, Procedure procedure, Action0 cancel, DispatchMode mode) {
		concurrency[hash(key) & hashMask].execute(procedure::call, procedure.getActionName(), cancel, mode);
	}

	public void Execute(long key, Action0 action) {
		Execute(Long.hashCode(key), action, DispatchMode.Normal);
	}

	public void Execute(long key, Action0 action, DispatchMode mode) {
		Execute(Long.hashCode(key), action, mode);
	}

	public void Execute(long key, Action0 action, String name) {
		Execute(Long.hashCode(key), action, name, DispatchMode.Normal);
	}

	public void Execute(long key, Action0 action, String name, DispatchMode mode) {
		Execute(Long.hashCode(key), action, name, mode);
	}

	public void Execute(long key, Action0 action, String name, Action0 cancel, DispatchMode mode) {
		Execute(Long.hashCode(key), action, name, cancel, mode);
	}

	public void Execute(long key, Func0<?> func) {
		Execute(Long.hashCode(key), func, DispatchMode.Normal);
	}

	public void Execute(long key, Func0<?> func, DispatchMode mode) {
		Execute(Long.hashCode(key), func, mode);
	}

	public void Execute(long key, Func0<?> func, String name) {
		Execute(Long.hashCode(key), func, name, DispatchMode.Normal);
	}

	public void Execute(long key, Func0<?> func, String name, DispatchMode mode) {
		Execute(Long.hashCode(key), func, name, mode);
	}

	public void Execute(long key, Func0<?> func, String name, Action0 cancel, DispatchMode mode) {
		Execute(Long.hashCode(key), func, name, cancel, mode);
	}

	public void Execute(long key, Procedure procedure) {
		Execute(Long.hashCode(key), procedure, DispatchMode.Normal);
	}

	public void Execute(long key, Procedure procedure, DispatchMode mode) {
		Execute(Long.hashCode(key), procedure, mode);
	}

	public void Execute(long key, Procedure procedure, Action0 cancel, DispatchMode mode) {
		Execute(Long.hashCode(key), procedure, cancel, mode);
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

	static abstract class Task {
		final String name;
		final Action0 cancel;
		final DispatchMode mode;

		Task(String name, Action0 cancel, DispatchMode mode) {
			this.name = name;
			this.cancel = cancel;
			this.mode = mode;
		}

		abstract boolean process(TaskOneByOne.BatchTask batch);
	}

	final class TaskOneByOne {
		final class TaskBarrier extends Task {
			final Barrier barrier;
			final int sum;

			TaskBarrier(Barrier barrier, int sum, DispatchMode mode) {
				super(barrier.procedure.getActionName(), barrier::cancel, mode);
				this.barrier = barrier;
				this.sum = sum;
			}

			@Override
			public boolean process(TaskOneByOne.BatchTask batch) {
				return barrier.reach(batch, sum);
			}
		}

		final class TaskAction extends Task {
			final Action0 action;

			TaskAction(Action0 action, String name, Action0 cancel, DispatchMode mode) {
				super(name != null ? name : action.getClass().getName(), cancel, mode);
				this.action = action;
			}

			@Override
			public boolean process(TaskOneByOne.BatchTask batch) {
				try {
					action.run();
				} catch (Exception e) {
					logger.error("TaskOneByOne: {}", name, e);
				}
				return true;
			}
		}

		final class TaskFunc extends Task {
			private final Func0<?> func;

			TaskFunc(Func0<?> func, String name, Action0 cancel, DispatchMode mode) {
				super(name, cancel, mode);
				this.func = func;
			}

			@Override
			public boolean process(TaskOneByOne.BatchTask batch) {
				try {
					func.call();
				} catch (Exception e) {
					logger.error("TaskOneByOne: {}", name, e);
				}
				return true;
			}
		}

		private final ReentrantLock lock = new ReentrantLock();
		private final Condition cond = lock.newCondition();
		private ArrayDeque<Task> queue = new ArrayDeque<>();
		private boolean isShutdown;
		private final BatchTask batch = new BatchTask();

		void execute(Action0 action, String name, Action0 cancel, DispatchMode mode) {
			execute(new TaskAction(action, name, cancel, mode));
		}

		void execute(Func0<?> func, String name, Action0 cancel, DispatchMode mode) {
			execute(new TaskFunc(func, name, cancel, mode));
		}

		final class BatchTask implements Runnable {
			Task[] tasks;
			int count;
			DispatchMode mode;
			int processedCount;

			void prepare() {
				if (!queue.isEmpty()) {
					var max = Math.min(queue.size(), 1000);
					if (null == tasks || max > tasks.length)
						tasks = new Task[max];
					mode = queue.peekFirst().mode;
					var i = 0;
					for (var task : queue) {
						if (mode != task.mode)
							break;
						tasks[i++] = task;
						if (i >= max)
							break;
					}
					count = i;
				} else {
					mode = DispatchMode.Normal;
					count = 0;
				}
			}

			@Override
			public void run() {
				for (processedCount = 0; processedCount < count; ++processedCount) {
					if (!tasks[processedCount].process(this)) {
						tasks[processedCount] = null; // gc
						++processedCount; // 下标索引转换成count。
						return; // 任务调度终端，当前任务以后完成的时候会触发runNext;
					}
					tasks[processedCount] = null; // gc
				}
				TaskOneByOne.this.runNext(processedCount);
			}

			void runNext() {
				TaskOneByOne.this.runNext(processedCount);
			}
		}

		void executeBarrier(Barrier barrier, int sum, DispatchMode mode) {
			execute(new TaskOneByOne.TaskBarrier(barrier, sum, mode));
		}

		private void execute(Task task) {
			boolean submit = false;
			lock.lock();
			try {
				if (!isShutdown) {
					queue.addLast(task);
					if (queue.size() != 1)
						return;
					submit = true;
					batch.prepare();
				}
			} finally {
				lock.unlock();
			}
			if (submit) {
				if (executor != null) {
					executor.execute(batch);
				} else {
					var threadPool = batch.mode == DispatchMode.Critical
							? Zeze.Util.Task.getCriticalThreadPool()
							: Zeze.Util.Task.getThreadPool();
					threadPool.submit(batch);
				}
			} else if (task.cancel != null) {
				try {
					task.cancel.run();
				} catch (Exception e) {
					logger.error("CancelAction={}", task.name, e);
				}
			}
		}

		private void runNext(int count) {
			lock.lock();
			try {
				while (count-- > 0)
					queue.pollFirst();
				if (queue.isEmpty()) {
					if (isShutdown)
						cond.signalAll();
					return;
				}
				batch.prepare();
			} finally {
				lock.unlock();
			}
			if (executor != null) {
				executor.execute(batch);
			} else {
				var threadPool = batch.mode == DispatchMode.Critical
						? Zeze.Util.Task.getCriticalThreadPool()
						: Zeze.Util.Task.getThreadPool();
				threadPool.submit(batch);
			}
		}

		void shutdown(boolean cancel) {
			ArrayDeque<Task> oldQueue;
			lock.lock();
			try {
				if (isShutdown)
					return;
				isShutdown = true;
				oldQueue = queue;
				if (!cancel || oldQueue.isEmpty())
					return;
				queue = new ArrayDeque<>(); // clear
				queue.addLast(oldQueue.pollFirst()); // put back running task back
			} finally {
				lock.unlock();
			}
			for (Task task : oldQueue) {
				if (task.cancel != null) {
					try {
						task.cancel.run();
					} catch (Exception e) {
						logger.error("CancelAction={}", task.name, e);
					}
				}
			}
		}

		void waitComplete() throws InterruptedException {
			lock.lock();
			try {
				while (!queue.isEmpty())
					cond.await(); // wait running task
			} finally {
				lock.unlock();
			}
		}

		@Override
		public String toString() {
			var sb = new StringBuilder().append('[');
			lock.lock();
			try {
				for (var task : queue)
					sb.append(task.name).append(',');
			} finally {
				lock.unlock();
			}
			int n = sb.length();
			if (n > 1)
				sb.setLength(n - 1);
			return sb.append(']').toString();
		}
	}
}
