package Zeze.Util;

import java.util.*;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** 
 对每个相同的key，最多只提交一个 Task.Run。
 
 说明：
 严格的来说应该对每个key建立一个队列，但是key可能很多，就需要很多队列。
 如果队列为空，需要回收队列，会产生很多垃圾回收对象。
 具体的实现对于相同的key.hash使用相同的队列。
 固定总的队列数，不回收队列。
 构造的时候，可以通过参数控制总的队列数量。
*/
public final class TaskOneByOneByKey {
	private static final Logger logger = LogManager.getLogger(TaskOneByOneByKey.class);

	private TaskOneByOne[] concurrency;

	public TaskOneByOneByKey() {
		this(1024);
	}

	public TaskOneByOneByKey(int concurrencyLevel) {
		if (concurrencyLevel < 0 || concurrencyLevel > 0x40000000) {
			throw new RuntimeException("Illegal concurrencyLevel: " + concurrencyLevel);
		}

		int capacity = 1;
		while (capacity < concurrencyLevel) {
			capacity <<= 1;
		}
		this.concurrency = new TaskOneByOne[capacity];
		for (int i = 0; i < this.concurrency.length; ++i) {
			this.concurrency[i] = new TaskOneByOne();
		}
	}


	public void Execute(Object key, Runnable action, String actionName) {
		Execute(key, action, actionName, null);
	}

	public void Execute(Object key, Runnable action) {
		Execute(key, action, null, null);
	}

	public void Execute(Object key, Runnable action, String actionName, Runnable cancel) {
		if (null == action) {
			throw new NullPointerException();
		}

		int h = Hash(key.hashCode());
		int index = h & (concurrency.length - 1);
		concurrency[index].Execute(action, actionName, cancel);
	}



	public void Execute(Object key, Callable<Integer> action, String actionName) {
		Execute(key, action, actionName, null);
	}

	public void Execute(Object key, Callable<Integer> action) {
		Execute(key, action, null, null);
	}

	public void Execute(Object key, Callable<Integer> action, String actionName, Runnable cancel) {
		if (null == action) {
			throw new NullPointerException();
		}

		int h = Hash(key.hashCode());
		int index = h & (concurrency.length - 1);
		concurrency[index].Execute(action, actionName, cancel);
	}


	public void Execute(Object key, Zeze.Transaction.Procedure procedure) {
		Execute(key, procedure, null);
	}

	public void Execute(Object key, Zeze.Transaction.Procedure procedure, Runnable cancel) {
		if (null == procedure) {
			throw new NullPointerException();
		}

		int h = Hash(key.hashCode());
		int index = h & (concurrency.length - 1);
		concurrency[index].Execute(procedure::Call, procedure.getActionName(), cancel);
	}


	public void Shutdown() {
		Shutdown(true);
	}

	public void Shutdown(boolean cancel) {
		for (var ts : concurrency) {
			ts.Shutdown(cancel);
		}
		for (var ts : concurrency) {
			ts.WaitComplete();
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
		int h = (int)_h;
		h ^= (h >>> 20) ^ (h >>> 12);
		return (int)(h ^ (h >>> 7) ^ (h >>> 4));
	}

	static class Task {
		public Runnable Action;
		public String Name;
		public Runnable Cancel;
		
		public Task(Runnable action, String name, Runnable cancel) {
			Action = action;
			Name = name;
			Cancel = cancel;
		}
	}

	public static class TaskOneByOne {
		private LinkedList<Task> queue = new LinkedList<Task>();

		private boolean IsShutdown = false;

		public final void Shutdown(boolean cancel) {
			LinkedList<Task> tmp = null;
			synchronized (this) {
				if (IsShutdown) {
					return;
				}
				IsShutdown = true;
				if (cancel) {
					tmp = queue;
					queue = new LinkedList<Task>(); // clear
					if (!tmp.isEmpty()) {
						queue.addLast(tmp.getFirst()); // put back running task back.
					}
				}
			}
			if (tmp == null) {
				return;
			}

			boolean first = true;
			for (var e : tmp) {
				if (first) { // first is running task, skip
					first = false;
					continue;
				}
				try {
					if (e.Cancel != null) {
						e.Cancel.run();
					}
				}
				catch (RuntimeException ex) {
					logger.error("CancelAction={}", e.Name, ex);
				}
			}
		}

		public final void WaitComplete() {
			synchronized (this) {
				// wait running task.
				while (!queue.isEmpty()) {
					try {
						this.wait();
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}

		public TaskOneByOne() {
		}

		public final void Execute(Runnable action, String actionName, Runnable cancel) {
			synchronized (this) {
				if (IsShutdown) {
					if (cancel != null) {
						cancel.run();
					}
					return;
				}

				queue.addLast(new Task(
					() -> {
						try {
							action.run();
						}
						finally {
							RunNext();
						}
					},
					actionName, cancel));

				if (queue.size() == 1) {
					var first = queue.getFirst();
					Zeze.Util.Task.Run(first.Action, first.Name);
				}
			}
		}

		public final void Execute(Callable<Integer> action, String actionName, Runnable cancel) {
			synchronized (this) {
				if (IsShutdown) {
					if (cancel != null) {
						cancel.run();
					}
					return;
				}

				queue.addLast(new Task(
					() -> {
						try {
							action.call();
						} catch (Exception skip) {
							// Zeze.Util.Task has handle error.
						}
						finally {
							RunNext();
						}
					},
					actionName, cancel));

				if (queue.size() == 1) {
					var first = queue.getFirst();
					Zeze.Util.Task.Run(first.Action, first.Name);
				}
			}
		}

		private void RunNext() {
			synchronized (this) {
				if (!queue.isEmpty()) {
					queue.removeFirst();

					if (IsShutdown && queue.isEmpty()) {
						this.notify();
						return;
					}
				}
				if (!queue.isEmpty()) {
					var first = queue.getFirst();
					Zeze.Util.Task.Run(first.Action, first.Name);
				}
			}
		}

	}
}