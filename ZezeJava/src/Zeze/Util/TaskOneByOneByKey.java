package Zeze.Util;

import Zeze.*;
import java.util.*;

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
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

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


	public void Execute(Object key, Action action, String actionName) {
		Execute(key, action, actionName, null);
	}

	public void Execute(Object key, Action action) {
		Execute(key, action, null, null);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void Execute(object key, Action action, string actionName = null, Action cancel = null)
	public void Execute(Object key, tangible.Action0Param action, String actionName, tangible.Action0Param cancel) {
		if (null == action) {
			throw new NullPointerException();
		}

		int h = Hash(key.hashCode());
		int index = h & (concurrency.length - 1);
		concurrency[index].Execute(action, actionName, cancel);
	}



	public void Execute(Object key, Func<Integer> action, String actionName) {
		Execute(key, action, actionName, null);
	}

	public void Execute(Object key, Func<Integer> action) {
		Execute(key, action, null, null);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void Execute(object key, Func<int> action, string actionName = null, Action cancel = null)
	public void Execute(Object key, tangible.Func0Param<Integer> action, String actionName, tangible.Action0Param cancel) {
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

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void Execute(object key, Zeze.Transaction.Procedure procedure, Action cancel = null)
	public void Execute(Object key, Zeze.Transaction.Procedure procedure, tangible.Action0Param cancel) {
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

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void Shutdown(bool cancel = true)
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
//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] static int Hash(int _h)
	private static int Hash(int _h) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint h = (uint)_h;
		int h = (int)_h;
		// This function ensures that hashCodes that differ only by
		// constant multiples at each bit position have a bounded
		// number of collisions (approximately 8 at default load factor).
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
		h ^= (h >>> 20) ^ (h >>> 12);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
		return (int)(h ^ (h >>> 7) ^ (h >>> 4));
	}

	public static class TaskOneByOne {
		private LinkedList<(tangible.Action0Param, String, tangible.Action0Param)> queue = new LinkedList<(tangible.Action0Param, String, tangible.Action0Param)>();

		private boolean IsShutdown = false;

		public final void Shutdown(boolean cancel) {
			LinkedList<(tangible.Action0Param, String, tangible.Action0Param)> tmp = null;
			synchronized (this) {
				if (IsShutdown) {
					return;
				}
				IsShutdown = true;
				if (cancel) {
					tmp = queue;
					queue = new LinkedList<(tangible.Action0Param, String, tangible.Action0Param)>(); // clear
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
				if (first) { // first is running task
					first = false;
					continue;
				}
				try {
					if (e.Item3 != null) {
						e.Item3.Invoke();
					}
				}
				catch (RuntimeException ex) {
					logger.Error(ex, String.format("CancelAction=%1$s", e.Item2));
				}
			}
		}

		public final void WaitComplete() {
			synchronized (this) {
				// wait running task.
				while (!queue.isEmpty()) {
					Monitor.Wait(this);
				}
			}
		}

		public TaskOneByOne() {
		}

		public final void Execute(tangible.Action0Param action, String actionName, tangible.Action0Param cancel) {
			synchronized (this) {
				if (IsShutdown) {
					if (cancel != null) {
						cancel.invoke();
					}
					return;
				}

				queue.addLast((() -> {
						try {
							action.invoke();
						}
						finally {
							RunNext();
						}
				}, actionName, cancel));

				if (queue.size() == 1) {
					Zeze.Util.Task.Run(queue.getFirst().Item1, queue.getFirst().Item2);
				}
			}
		}

		public final void Execute(tangible.Func0Param<Integer> action, String actionName, tangible.Action0Param cancel) {
			synchronized (this) {
				if (IsShutdown) {
					if (cancel != null) {
						cancel.invoke();
					}
					return;
				}

				queue.addLast((() -> {
						try {
							action.invoke();
						}
						finally {
							RunNext();
						}
				}, actionName, cancel));

				if (queue.size() == 1) {
					Zeze.Util.Task.Run(queue.getFirst().Item1, queue.getFirst().Item2);
				}
			}
		}

		private void RunNext() {
			synchronized (this) {
				if (!queue.isEmpty()) {
					queue.removeFirst();

					if (IsShutdown && queue.isEmpty()) {
						Monitor.PulseAll(this);
						return;
					}
				}
				if (!queue.isEmpty()) {
					Zeze.Util.Task.Run(queue.getFirst().Item1, queue.getFirst().Item2);
				}
			}
		}

	}
}