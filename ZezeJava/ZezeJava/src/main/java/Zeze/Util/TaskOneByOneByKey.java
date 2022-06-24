package Zeze.Util;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
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

	public TaskOneByOneByKey() {
		this(1024);
	}

	public TaskOneByOneByKey(int concurrencyLevel) {
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

	public void Execute(Object key, Action0 action) {
		Execute(key.hashCode(), action);
	}

	public void Execute(Object key, Action0 action, String name) {
		Execute(key.hashCode(), action, name);
	}

	public void Execute(Object key, Action0 action, String name, Action0 cancel) {
		Execute(key.hashCode(), action, name, cancel);
	}

	public void Execute(Object key, Func0<?> func) {
		Execute(key.hashCode(), func);
	}

	public void Execute(Object key, Func0<?> func, String name) {
		Execute(key.hashCode(), func, name);
	}

	public void Execute(Object key, Func0<?> func, String name, Action0 cancel) {
		Execute(key.hashCode(), func, name, cancel);
	}

	public void Execute(Object key, Procedure procedure) {
		Execute(key.hashCode(), procedure);
	}

	public static class Barrier {
		private final Procedure Procedure;
		private final Action0 CancelAction;
		private final ReentrantLock lock = new ReentrantLock();
		private final Condition cond = lock.newCondition();
		private int Count;
		private boolean Canceled;

		public Barrier(Procedure action, int count, Action0 cancel) {
			Procedure = action;
			CancelAction = cancel;
			Count = count;
		}

		public void Reach() throws InterruptedException {
			lock.lock();
			try {
				if (Canceled)
					return;

				if (--Count > 0)
					cond.await();
				else {
					try {
						Procedure.Call();
					} catch (Throwable ex) {
						logger.error(Procedure.getActionName() + " Run", ex);
					} finally {
						cond.signalAll();
					}
				}
			} finally {
				lock.unlock();
			}
		}

		public void Cancel() {
			lock.lock();
			try {
				if (Canceled)
					return;

				Canceled = true;
				try {
					if (null != CancelAction)
						CancelAction.run();
				} catch (Throwable ex) {
					logger.error(Procedure.getActionName() + " Canceled", ex);
				} finally {
					cond.signalAll();
				}
			} finally {
				lock.unlock();
			}
		}
	}

	public <T> void ExecuteCyclicBarrier(Collection<T> keys, Procedure procedure, Action0 cancel) {
		if (keys.isEmpty())
			throw new RuntimeException("CyclicBarrier keys is empty.");

		var barrier = new Barrier(procedure, keys.size(), cancel);
		for (var key : keys)
			Execute(key, barrier::Reach, barrier.Procedure.getActionName(), barrier::Cancel);
	}

	public void Execute(Object key, Procedure procedure, Action0 cancel) {
		Execute(key.hashCode(), procedure, cancel);
	}

	public void Execute(int key, Action0 action) {
		Execute(key, action, null, null);
	}

	public void Execute(int key, Action0 action, String name) {
		Execute(key, action, name, null);
	}

	public void Execute(int key, Action0 action, String name, Action0 cancel) {
		if (action == null)
			throw new NullPointerException();
		concurrency[Hash(key) & hashMask].Execute(action, name, cancel);
	}

	public void Execute(int key, Func0<?> func) {
		Execute(key, func, null, null);
	}

	public void Execute(int key, Func0<?> func, String name) {
		Execute(key, func, name, null);
	}

	public void Execute(int key, Func0<?> func, String name, Action0 cancel) {
		if (func == null)
			throw new NullPointerException();
		concurrency[Hash(key) & hashMask].Execute(func, name, cancel);
	}

	public void Execute(int key, Procedure procedure) {
		Execute(key, procedure, null);
	}

	public void Execute(int key, Procedure procedure, Action0 cancel) {
		concurrency[Hash(key) & hashMask].Execute(procedure::Call, procedure.getActionName(), cancel);
	}

	public void Execute(long key, Action0 action) {
		Execute(Long.hashCode(key), action);
	}

	public void Execute(long key, Action0 action, String name) {
		Execute(Long.hashCode(key), action, name);
	}

	public void Execute(long key, Action0 action, String name, Action0 cancel) {
		Execute(Long.hashCode(key), action, name, cancel);
	}

	public void Execute(long key, Func0<?> func) {
		Execute(Long.hashCode(key), func);
	}

	public void Execute(long key, Func0<?> func, String name) {
		Execute(Long.hashCode(key), func, name);
	}

	public void Execute(long key, Func0<?> func, String name, Action0 cancel) {
		Execute(Long.hashCode(key), func, name, cancel);
	}

	public void Execute(long key, Procedure procedure) {
		Execute(Long.hashCode(key), procedure);
	}

	public void Execute(long key, Procedure procedure, Action0 cancel) {
		Execute(Long.hashCode(key), procedure, cancel);
	}

	public void Shutdown() {
		Shutdown(true);
	}

	public void Shutdown(boolean cancel) {
		for (var ts : concurrency)
			ts.Shutdown(cancel);
		try {
			for (var ts : concurrency)
				ts.WaitComplete();
		} catch (InterruptedException e) {
			logger.error("Shutdown interrupted", e);
		}
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
	private static int Hash(int _h) {
		int h = _h;
		h ^= (h >>> 20) ^ (h >>> 12);
		return (h ^ (h >>> 7) ^ (h >>> 4));
	}

	static final class TaskOneByOne {
		static abstract class Task implements Runnable {
			final String name;
			final Action0 cancel;

			Task(String name, Action0 cancel) {
				this.name = name;
				this.cancel = cancel;
			}
		}

		final class TaskAction extends Task {
			final Action0 action;

			TaskAction(Action0 action, String name, Action0 cancel) {
				super(name != null ? name : action.getClass().getName(), cancel);
				this.action = action;
			}

			@Override
			public void run() {
				try {
					action.run();
				} catch (Throwable e) {
					logger.error("TaskOneByOne: " + name, e);
				} finally {
					RunNext();
				}
			}
		}

		final class TaskFunc extends Task {
			private final Func0<?> func;

			TaskFunc(Func0<?> func, String name, Action0 cancel) {
				super(name, cancel);
				this.func = func;
			}

			@Override
			public void run() {
				try {
					func.call();
				} catch (Throwable e) {
					logger.error("TaskOneByOne: " + name, e);
				} finally {
					RunNext();
				}
			}
		}

		private final ReentrantLock lock = new ReentrantLock();
		private final Condition cond = lock.newCondition();
		private ArrayDeque<Task> queue = new ArrayDeque<>();
		private boolean IsShutdown;

		void Execute(Action0 action, String name, Action0 cancel) {
			Execute(new TaskAction(action, name, cancel));
		}

		void Execute(Func0<?> func, String name, Action0 cancel) {
			Execute(new TaskFunc(func, name, cancel));
		}

		private void Execute(Task task) {
			boolean submit = false;
			lock.lock();
			try {
				if (!IsShutdown) {
					queue.addLast(task);
					if (queue.size() != 1)
						return;
					submit = true;
				}
			} finally {
				lock.unlock();
			}
			if (submit)
				Zeze.Util.Task.getThreadPool().submit(task);
			else if (task.cancel != null) {
				try {
					task.cancel.run();
				} catch (Throwable e) {
					logger.error("CancelAction={}", task.name, e);
				}
			}
		}

		private void RunNext() {
			Task task;
			lock.lock();
			try {
				queue.removeFirst();
				if (queue.isEmpty()) {
					if (IsShutdown)
						cond.signalAll();
					return;
				}
				task = queue.peekFirst();
			} finally {
				lock.unlock();
			}
			Zeze.Util.Task.getThreadPool().submit(task);
		}

		void Shutdown(boolean cancel) {
			ArrayDeque<Task> oldQueue;
			lock.lock();
			try {
				if (IsShutdown)
					return;
				IsShutdown = true;
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
					} catch (Throwable e) {
						logger.error("CancelAction={}", task.name, e);
					}
				}
			}
		}

		void WaitComplete() throws InterruptedException {
			lock.lock();
			try {
				while (!queue.isEmpty())
					cond.await(); // wait running task
			} finally {
				lock.unlock();
			}
		}
	}
}
