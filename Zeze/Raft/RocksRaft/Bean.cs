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
    public abstract class Bean : Serializable
	{
		private static Util.AtomicLong ObjectIdGenerator = new Util.AtomicLong();
		public const int ObjectIdStep = 4096;
		public const int MaxVariableId = ObjectIdStep - 1;

		public long ObjectId { get; } = ObjectIdGenerator.AddAndGet(ObjectIdStep);
		public Bean Parent { get; private set; }
		public int VariableId { get; set; }

		public Record.RootInfo RootInfo { get; private set; }
		public bool IsManaged => RootInfo != null;
		public TableKey TableKey => RootInfo?.TableKey;

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

		public virtual LogBean CreateLogBean()
		{
			return new LogBean() { Belong = Parent, This = this, VariableId = VariableId, };
		}

		public abstract void FollowerApply(Log log);
		public abstract void LeaderApplyNoRecursive(Log log);

		public virtual long TypeId => Zeze.Transaction.Bean.Hash64(GetType().FullName);

		public virtual void BuildString(System.Text.StringBuilder sb, int level)
		{
			sb.Append(new string(' ', level)).Append("{}").Append(System.Environment.NewLine);
		}
	}
}
