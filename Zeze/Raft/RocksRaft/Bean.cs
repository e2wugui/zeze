using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	public class RocksLogs : Serializable
    {
		public Dictionary<TableKey, RecordLog> Records { get; } = new Dictionary<TableKey, RecordLog>();

        public void Decode(ByteBuffer bb)
        {
			Records.Clear();
			for (int i = bb.ReadInt(); i >= 0; i--)
            {
				var tkey = new TableKey();
				var blog = new RecordLog();
				tkey.Decode(bb);
				blog.Decode(bb);
				Records.Add(tkey, blog);
            }
        }

        public void Encode(ByteBuffer bb)
        {
			bb.WriteInt(Records.Count);
			foreach (var r in Records)
            {
				r.Key.Encode(bb);
				r.Value.Encode(bb);
            }
        }
    }

    public class TableKey : Serializable
    {
		public string Name { get; set; }
		public object Key { get; set; }

        public void Decode(ByteBuffer bb)
        {
            throw new NotImplementedException();
        }

        public void Encode(ByteBuffer bb)
        {
            throw new NotImplementedException();
        }
    }

	public class Transaction
    {
		public static Transaction Current => null;

		public bool LogTryGet(long logKey, out Log log)
		{
			log = null;
			return false;
		}

		public void LogPut(long logKey, Log log)
        {

        }

		public Log LogGetOrAdd(long logKey, Func<int, Log> varLogFactory)
        {
			return varLogFactory((int)(logKey & Bean.MaxVariableId));
        }
    }

    public abstract class Bean : Serializable
	{
		private static Util.AtomicLong ObjectIdGenerator = new Util.AtomicLong();
		public const int ObjectIdStep = 4096;
		public const int MaxVariableId = ObjectIdStep - 1;

		public long ObjectId { get; } = ObjectIdGenerator.AddAndGet(ObjectIdStep);
		public Bean Parent { get; private set; }
		public int VariableId { get; private set; }

		public Record.RootInfo RootInfo { get; private set; }
		public bool IsManaged => RootInfo != null;

		public Bean()
        {

        }

		public Bean(int varid)
        {
			VariableId = varid;
        }

		public void InitRootInfo(Record.RootInfo rootInfo, Bean parent)
		{
			if (IsManaged)
			{
				throw new Zeze.Transaction.HasManagedException();
			}
			this.RootInfo = rootInfo;
			this.Parent = parent;
			InitChildrenRootInfo(rootInfo);
		}

		// 用在第一次加载Bean时，需要初始化它的root
		protected abstract void InitChildrenRootInfo(Record.RootInfo root);

		public abstract void Decode(ByteBuffer bb);
		public abstract void Encode(ByteBuffer bb);

		int _i;
		long _l;
		CollMap<int, int> _map;
		public Bean _bean;

		public int I
		{
			get
			{
				if (false == Transaction.Current.LogTryGet(ObjectId + 1, out var log))
					return _i;
				return ((Log_i)log).Value;
			}

			set
			{
				Transaction.Current.LogPut(this.ObjectId, new Log_i() { VariableId = 1, Value = value, });
			}
		}

		public CollMap<int, int> Map => _map;

		public sealed class Log_i : Log<int>
        {
			public override void Apply(Bean holder) { holder._i = Value; }
        }

		public sealed class Log_bean : LogBean
        {
            public override void Apply(Bean holder) { base.Apply(holder._bean); }
        }
	}


	public class RecordLog : LogBean
    {
    }


}
