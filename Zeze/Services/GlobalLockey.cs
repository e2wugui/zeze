using System;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.Text;
using System.Runtime.CompilerServices;
using Zeze.Builtin.GlobalCacheManagerWithRaft;
using System.Threading;
using System.Threading.Tasks;
using Zeze.Raft.RocksRaft;

namespace Zeze.Services
{

	public sealed class GlobalLockAsync : IPessimismLock
	{ 
		public GlobalLockey GlobalLockey { get; }
		public GlobalLockAsync(GlobalLockey globalLockey)
        {
			GlobalLockey= globalLockey;
		}

		private IDisposable Acquired;

		public async Task<GlobalLockAsync> EnterAsync()
		{
			if (null != Acquired)
				throw new InvalidOperationException("null != Acquired");
			Acquired = await GlobalLockey.Monitor.EnterAsync();
			return this;
		}

		public void Dispose()
		{
			GC.SuppressFinalize(this);
			Acquired?.Dispose();
			Acquired = null;
		}

		public async Task<IPessimismLock> LockAsync()
		{
			return await EnterAsync();
		}

		public async Task WaitAsync()
		{
			await GlobalLockey.Monitor.WaitAsync();
		}

		public void Pulse()
		{
			GlobalLockey.Monitor.Pulse();
		}

		public void PulseAll()
		{
			GlobalLockey.Monitor.PulseAll();
		}

	}

	public sealed class GlobalLockey : System.IComparable<GlobalLockey>
    {
		public GlobalTableKey GlobalTableKey { get; }
		internal Nito.AsyncEx.AsyncMonitor Monitor { get; set; } = new();

		/// <summary>
		/// 相同值的 TableKey 要得到同一个 Lock 引用，必须使用 Locks 查询。
		/// 不要自己构造这个对象。开放出去仅仅为了测试。
		/// </summary>
		/// <param name="key"></param>
		public GlobalLockey(GlobalTableKey key)
		{
			GlobalTableKey = key;
		}

		public int CompareTo(GlobalLockey other)
        {
			if (other == null)
				return 1; // null always small

			return GlobalTableKey.CompareTo(other.GlobalTableKey);
        }

        public override int GetHashCode()
        {
            return GlobalTableKey.GetHashCode();
        }

        public override bool Equals(object obj)
        {
			if (this == obj)
				return true;

			if (obj is GlobalLockey another)
	            return GlobalTableKey.Equals(another.GlobalTableKey);

			return false;
        }
    }

	/**
	 * <p>
	 * Locks原来使用 单个容器管理锁，效率太低：
	 * <p>
	 * 1. 每次查询都会试图去回收; 以前java版实现一个懒惰的WeakHashSet。c# ConditionalWeakTable 使用 this==another 吧，没有调用 Equals，不能使用。
	 * 2. 并发访问效率低. 通过增加segment解决。
	 */
	public sealed class Locks
	{
		/**
		 * The maximum number of segments to allow; used to bound constructor arguments.
		 */
		private const int MAX_SEGMENTS = 1 << 16; // slightly conservative
		private readonly int segmentShift;
		private readonly uint segmentMask;
		private readonly Segment[] segments;

		/* ---------------- hash算法和映射规则都是来自 ConcurrentHashMap. -------------- */
		/**
		 * Returns the segment that should be used for key with given hash.
		 * 
		 * @param lockey the Lockey
		 * @return the segment
		 */
		private Segment SegmentFor(GlobalLockey lockey)
		{
			/**
			 * Applies a supplemental hash function to a given hashCode, which defends
			 * against poor quality hash functions. This is critical because
			 * ConcurrentHashMap uses power-of-two length hash tables, that otherwise
			 * encounter collisions for hashCodes that do not differ in lower or upper bits.
			 */
			// Spread bits to regularize both segment and index locations,
			// using variant of single-word Wang/Jenkins hash.
			uint h = (uint)lockey.GetHashCode();
			h += (h << 15) ^ 0xffffcd7d;
			h ^= (h >> 10);
			h += (h << 3);
			h ^= (h >> 6);
			h += (h << 2) + (h << 14);
			uint hash = h ^ (h >> 16);

			uint index = (hash >> segmentShift) & segmentMask;
			return segments[index];
		}

		public Locks() : this(1024)
		{
		}

		public Locks(int concurrencyLevel)
		{
			if (concurrencyLevel <= 0)
				throw new ArgumentException();

			if (concurrencyLevel > MAX_SEGMENTS)
				concurrencyLevel = MAX_SEGMENTS;

			// Find power-of-two sizes best matching arguments
			int sshift = 0;
			int ssize = 1;
			while (ssize < concurrencyLevel)
			{
				++sshift;
				ssize <<= 1;
			}
			this.segmentShift = 32 - sshift;
			this.segmentMask = (uint)(ssize - 1);
			this.segments = new Segment[ssize];
			for (int i = 0; i < this.segments.Length; ++i)
				this.segments[i] = new Segment();
		}

		/* ------------- 实现 --------------- */
		sealed class Segment
		{
			private readonly Util.WeakHashSet<GlobalLockey> locks = new ();

			public Segment()
			{
			}

			public bool Contains(GlobalLockey key)
			{
				// 需要sync，get不是线程安全的
				lock (this)
                {
					return locks.get(key) != null;
				}
			}

			public GlobalLockey Get(GlobalLockey key)
			{
				lock (this)
				{
					GlobalLockey exist = locks.get(key);
					if (null != exist)
						return exist;

					locks.add(key);
					return key;
				}
			}
		}

		public GlobalLockey Get(GlobalLockey lockey)
		{
			return SegmentFor(lockey).Get(lockey);
		}

		public GlobalLockAsync Get(GlobalTableKey tkey)
        {
			return new GlobalLockAsync(Get(new GlobalLockey(tkey)));
        }
	}

}
