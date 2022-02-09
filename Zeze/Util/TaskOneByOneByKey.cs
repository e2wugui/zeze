using System;
using System.Collections.Generic;
using System.Runtime.CompilerServices;
using System.Threading;

namespace Zeze.Util
{
	/// <summary>
	/// 对每个相同的key，最多只提交一个 Task.Run。
	/// 
	/// 说明：
	/// 严格的来说应该对每个key建立一个队列，但是key可能很多，就需要很多队列。
	/// 如果队列为空，需要回收队列，会产生很多垃圾回收对象。
	/// 具体的实现对于相同的key.hash使用相同的队列。
	/// 固定总的队列数，不回收队列。
	/// 构造的时候，可以通过参数控制总的队列数量。
	/// </summary>
	public sealed class TaskOneByOneByKey
    {
		private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

		private readonly TaskOneByOne[] concurrency;

		public TaskOneByOneByKey() : this(1024)
        {
        }

		public TaskOneByOneByKey(int concurrencyLevel)
        {
			if (concurrencyLevel < 0 || concurrencyLevel > 0x40000000)
				throw new Exception("Illegal concurrencyLevel: " + concurrencyLevel);

			int capacity = 1;
			while (capacity < concurrencyLevel)
				capacity <<= 1;
			this.concurrency = new TaskOneByOne[capacity];
			for (int i = 0; i < this.concurrency.Length; ++i)
				this.concurrency[i] = new TaskOneByOne();
		}

		public void Execute(object key, Action action, string actionName = null, Action cancel = null)
        {
			if (null == action)
				throw new ArgumentNullException();

			int h = Hash(key.GetHashCode());
			int index = h & (concurrency.Length - 1);
			concurrency[index].Execute(action, actionName, cancel);
        }


		public void Execute(object key, Func<long> action, string actionName = null, Action cancel = null)
		{
			if (null == action)
				throw new ArgumentNullException();

			int h = Hash(key.GetHashCode());
			int index = h & (concurrency.Length - 1);
			concurrency[index].Execute(action, actionName, cancel);
		}

		public void Execute(object key, Zeze.Transaction.Procedure procedure, Action cancel = null)
		{
			if (null == procedure)
				throw new ArgumentNullException();

			int h = Hash(key.GetHashCode());
			int index = h & (concurrency.Length - 1);
			concurrency[index].Execute(procedure.Call, procedure.ActionName, cancel);
		}

		public void Shutdown(Action beforeWait = null, bool cancel = true)
        {
			foreach (var ts in concurrency)
            {
				ts.Shutdown(cancel);
            }
			beforeWait?.Invoke();
			foreach (var ts in concurrency)
            {
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
		[MethodImpl(MethodImplOptions.AggressiveInlining)]
		static int Hash(int _h)
		{
			uint h = (uint)_h;
			// This function ensures that hashCodes that differ only by
			// constant multiples at each bit position have a bounded
			// number of collisions (approximately 8 at default load factor).
			h ^= (h >> 20) ^ (h >> 12);
			return (int)(h ^ (h >> 7) ^ (h >> 4));
		}

		internal class TaskOneByOne
		{
			LinkedList<(Action, string, Action)> queue = new LinkedList<(Action, string, Action)>();

			bool IsShutdown = false;

			internal void Shutdown(bool cancel)
            {
				LinkedList<(Action, string, Action)> tmp = null;
				lock (this)
                {
					if (IsShutdown)
						return;
					IsShutdown = true;
					if (cancel)
                    {
						tmp = queue;
						queue = new LinkedList<(Action, string, Action)>(); // clear
						if (tmp.Count > 0)
							queue.AddLast(tmp.First.Value); // put back running task back.
					}
				}
				if (tmp == null)
					return;

				bool first = true;
				foreach (var e in tmp)
                {
					if (first) // first is running task
					{
						first = false;
						continue;
                    }
                    try
                    {
						e.Item3?.Invoke();
                    }
					catch (Exception ex)
                    {
						logger.Error(ex, $"CancelAction={e.Item2}");
                    }
                }
            }

			internal void WaitComplete()
            {
				lock (this)
				{
					// wait running task.
					while (queue.Count > 0)
					{
						Monitor.Wait(this);
					}
				}
			}

			public TaskOneByOne()
            {
            }

			internal void Execute(Action action, string actionName, Action cancel)
            {
				lock (this)
                {
					if (IsShutdown)
                    {
						cancel?.Invoke();
						return;
					}

					queue.AddLast((() =>
					{
						try
						{
							action();
						}
						finally
						{
							RunNext();
						}
					}, actionName, cancel));

					if (queue.Count == 1)
                    {
						Zeze.Util.Task.Run(queue.First.Value.Item1, queue.First.Value.Item2);
                    }
				}
			}

			internal void Execute(Func<long> action, string actionName, Action cancel)
			{
				lock (this)
				{
					if (IsShutdown)
					{
						cancel?.Invoke();
						return;
					}

					queue.AddLast((() =>
					{
						try
						{
							action();
						}
						finally
						{
							RunNext();
						}
					}, actionName, cancel));

					if (queue.Count == 1)
					{
						Zeze.Util.Task.Run(queue.First.Value.Item1, queue.First.Value.Item2);
					}
				}
			}

			private void RunNext()
			{
				lock (this)
				{
					if (queue.Count > 0)
					{
						queue.RemoveFirst();

						if (IsShutdown && queue.Count == 0)
                        {
							Monitor.PulseAll(this);
							return;
                        }
					}
					if (queue.Count > 0)
					{
						Zeze.Util.Task.Run(queue.First.Value.Item1, queue.First.Value.Item2);
					}
				}
			}

		}
	}
}
