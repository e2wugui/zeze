using DotNext.Threading;
using RocksDbSharp;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

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
		public bool Removed { get; set; }


		private static readonly Util.AtomicLong TimestampGen = new();
		internal static long NextTimestamp => TimestampGen.IncrementAndGet();
		internal abstract void LeaderApply(Transaction.RecordAccessed accessed);
		internal abstract void Flush(WriteBatch batch);
		public Table Table { get; internal set; }
		public AsyncLock Mutex { get; } = AsyncLock.Exclusive();
	}

	public class Record<K, V> : Record
		where V : Bean, new()
	{
		public K Key { get; internal set; }

		internal override void LeaderApply(Transaction.RecordAccessed accessed)
		{
			if (null != accessed.PutLog)
			{
				Value = accessed.PutLog.Value;
			}
			Timestamp = NextTimestamp; // 必须在 Value = 之后设置。防止出现新的事务得到新的Timestamp，但是数据时旧的。
		}

		internal override void Flush(WriteBatch batch)
        {
			if (Value == null)
            {
				var keybb = ByteBuffer.Allocate();
				SerializeHelper<K>.Encode(keybb, Key);
				batch.Delete(keybb.Bytes, (ulong)keybb.Size, Table.ColumnFamily);
			}
			else
            {
				var keybb = ByteBuffer.Allocate();
				SerializeHelper<K>.Encode(keybb, Key);
				var valuebb = ByteBuffer.Allocate(1024);
				Value.Encode(valuebb);
				batch.Put(keybb.Bytes, (ulong)keybb.Size, valuebb.Bytes, (ulong)valuebb.Size, Table.ColumnFamily);
			}
		}
	}
}
