using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Raft.RocksRaft
{
	public abstract class Record
    {
		public class RootInfo
		{
			public Record Record { get; }
			public TableKey TableKey { get; }

			public RootInfo(Record record, TableKey tableKey)
			{
				Record = record;
				TableKey = tableKey;
			}
		}

		public RootInfo CreateRootInfoIfNeed(TableKey tkey)
		{
			var cur = Value?.RootInfo;
			if (null == cur)
				cur = new RootInfo(this, tkey);
			return cur;
		}

		public const int StateNew = 0;
		public const int StateLoad = 1;

		public int State { get; internal set; } = StateNew;
		public Bean Value { get; internal set; }
		public long Timestamp { get; set; }
		public bool Removed { get; internal set; }


		private static Util.AtomicLong _TimestampGen = new Util.AtomicLong();
		internal static long NextTimestamp => _TimestampGen.IncrementAndGet();
		internal abstract void LeaderCommit(Transaction.RecordAccessed accessed);
	}

	public class Record<K, V> : Record
	{
		internal override void LeaderCommit(Transaction.RecordAccessed accessed)
		{
			if (null != accessed.PutValueLog)
			{
				Value = accessed.PutValueLog.Value;
			}
			Timestamp = NextTimestamp; // 必须在 Value = 之后设置。防止出现新的事务得到新的Timestamp，但是数据时旧的。
		}
	}
}
