
using System.Collections.Concurrent;
using System.Threading.Tasks;
using Zeze.Serialize;
using Zeze.Transaction;

namespace Zeze.Component
{
	public class AutoKey : AbstractAutoKey
	{
		public class Module : AbstractAutoKey
		{
			private readonly ConcurrentDictionary<string, AutoKey> map = new();
			public Application Zeze { get; }

			// 这个组件Zeze.Application会自动初始化，不需要应用初始化。
			public Module(Zeze.Application zeze)
			{
				Zeze = zeze;
				RegisterZezeTables(zeze);
			}

			public override void UnRegister()
			{
				UnRegisterZezeTables(Zeze);
			}

			/**
			 * 这个返回值，可以在自己模块内保存下来，效率高一些。
			 */
			public AutoKey GetOrAdd(string name)
			{
				return map.GetOrAdd(name, name2 => new AutoKey(this, name2));
			}
		}

		private const int AllocateCount = 1000;

		private readonly Module module;
		private readonly string name;
		private volatile Range range;
		private readonly long logKey;

		private AutoKey(Module module, string name)
		{
			this.module = module;
			this.name = name;
			logKey = Bean.GetNextObjectId();
		}

		public async Task<long> NextIdAsync()
		{
			if (null != range)
			{
				var next = range.TryNextId();
				if (next != null)
					return next.Value; // allocate in range success
			}

			var txn = Transaction.Transaction.Current;
			var log = (RangeLog)txn.GetLog(logKey);
			while (true)
			{
				if (null == log)
				{
					// allocate: 多线程，多事务，多服务器（缓存同步）由zeze保证。
					var key = await module._tAutoKeys.GetOrAddAsync(name);
					var start = key.NextId;
					var end = start + AllocateCount; // AllocateCount == 0 会死循环。
					key.NextId = end;
					// create log，本事务可见，
					log = new RangeLog(this, new Range(start, end));
					txn.PutLog(log);
				}
				var tryNext = log.range.TryNextId();
				if (tryNext != null)
					return tryNext.Value;

				// 事务内分配了超出Range范围的id，再次allocate。
				// 覆盖RangeLog是可以的。就像事务内多次改变变量。最后面的Log里面的数据是最新的。
				// 已分配的范围保存在_AutoKeys表内，事务内可以继续分配。
				log = null;
			}
		}

		private class Range
		{
			private readonly Util.AtomicLong atomicNextId;
			private readonly long max;

			public long? TryNextId()
			{
				// 每次都递增。超出范围以后，也不恢复。
				var next = atomicNextId.IncrementAndGet();
				if (next >= max)
					return null;
				return next;
			}

			public Range(long start, long end)
			{
				atomicNextId = new(start);
				max = end;
			}
		}

		private class RangeLog : Log
		{
			public AutoKey AutoKey;
			internal Range range;

			public RangeLog(AutoKey autoKey, Range range)
			{
				AutoKey = autoKey;
				this.range = range;
			}

            public override long LogKey => AutoKey.logKey;

            public override void Commit()
			{
				// 这里直接修改拥有者的引用，开放出去，以后其他事务就能看到新的Range了。
				// 并发：多线程实际上由 _autokeys 表的锁来达到互斥，commit的时候，是互斥锁。
				AutoKey.range = range;
			}

            public override void Encode(ByteBuffer bb)
            {
                throw new System.NotImplementedException();
            }

            public override void Decode(ByteBuffer bb)
            {
                throw new System.NotImplementedException();
            }

            internal override void EndSavepoint(Savepoint currentsp)
            {
				currentsp.Logs[LogKey] = this;
			}

            internal override Log BeginSavepoint()
            {
				return this;
            }
        }
	}
}