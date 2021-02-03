using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;
using System.Runtime.CompilerServices;

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
		private TaskOneByOne[] concurrency;

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

		public void Execute(object key, Action action, string actionName = null)
        {
			if (null == action)
				throw new ArgumentNullException();

			int h = Hash(key.GetHashCode());
			int index = h & (concurrency.Length - 1);
			concurrency[index].Execute(action, actionName);
        }


		public void Execute(object key, Func<int> action, string actionName = null)
		{
			if (null == action)
				throw new ArgumentNullException();

			int h = Hash(key.GetHashCode());
			int index = h & (concurrency.Length - 1);
			concurrency[index].Execute(action, actionName);
		}

		public void Execute(object key, Zeze.Transaction.Procedure procedure)
		{
			if (null == procedure)
				throw new ArgumentNullException();

			int h = Hash(key.GetHashCode());
			int index = h & (concurrency.Length - 1);
			concurrency[index].Execute(procedure.Call, procedure.ActionName);
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
			LinkedList<(Action, string)> queue = new LinkedList<(Action, string)>();

            public TaskOneByOne()
            {
            }

			internal void Execute(Action action, string actionName)
            {
				lock (this)
                {
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
					}, actionName));

					if (queue.Count == 1)
                    {
						Zeze.Util.Task.Run(queue.First.Value.Item1, queue.First.Value.Item2);
                    }
				}
			}

			internal void Execute(Func<int> action, string actionName)
			{
				lock (this)
				{
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
					}, actionName));

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
					queue.RemoveFirst();

					if (queue.Count > 0)
					{
						Zeze.Util.Task.Run(queue.First.Value.Item1, queue.First.Value.Item2);
					}
				}
			}

		}
	}
}
