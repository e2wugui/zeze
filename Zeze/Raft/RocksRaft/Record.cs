using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Raft.RocksRaft
{
	public class Record
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

		public Bean Value { get; set; }
		public long Timestamp { get; set; }
		public bool Removed { get; internal set; }


		private static Util.AtomicLong _TimestampGen = new Util.AtomicLong();
		internal static long NextTimestamp => _TimestampGen.IncrementAndGet();
	}

	public class Record<K, V> : Record
	{
	}
}
