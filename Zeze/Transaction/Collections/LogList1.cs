using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;
using Zeze.Util;

namespace Zeze.Transaction.Collections
{
	public class LogList1<E> : LogList<E>
	{
        public new static readonly string StableName = Reflect.GetStableName(typeof(LogList1<E>));
        public new static readonly int TypeId_ = FixedHash.Hash32(StableName);

        public override int TypeId => TypeId_;

        public class OpLog
		{
			public const int OP_MODIFY = 0;
			public const int OP_ADD = 1;
			public const int OP_REMOVE = 2;
			public const int OP_CLEAR = 3;

			public readonly int op;
			public readonly int index;
			public readonly E value;

			public OpLog(int op, int index, E value)
			{
				this.op = op;
				this.index = index;
				this.value = value;
			}

			public override string ToString()
			{
				return $"({op},{index},{value})";
			}
		}

		public readonly List<OpLog> OpLogs = new List<OpLog>();
		protected IdentityHashSet<E> AddSet; // 用于LogList2，由它初始化。

#if !USE_CONFCS
		public override void Collect(Changes changes, Bean recent, Log vlog)
		{
			throw new System.NotImplementedException("Collect Not Implement.");
		}

		public void Add(E item)
		{
			if (item == null)
				throw new System.ArgumentNullException("value is null");
			Value = Value.Add(item);
			OpLogs.Add(new OpLog(OpLog.OP_ADD, Value.Count - 1, item));
			AddSet?.Add(item);
		}

		public void AddRange(IEnumerable<E> items)
        {
			var start = Value.Count;
			Value = Value.AddRange(items);
			foreach (var item in items)
			{
				OpLogs.Add(new OpLog(OpLog.OP_ADD, start++, item));
                AddSet?.Add(item);
            }
        }

		public bool Remove(E item)
		{
			var index = Value.IndexOf(item);
			if (index < 0)
				return false;
			RemoveAt(index);
			AddSet?.Remove(item);
			return true;
		}

		public void Clear()
		{
			Value = System.Collections.Immutable.ImmutableList<E>.Empty;
			OpLogs.Clear();
			OpLogs.Add(new OpLog(OpLog.OP_CLEAR, 0, default));
			AddSet?.Clear();
		}

		public void Insert(int index, E item)
		{
			Value = Value.Insert(index, item);
			OpLogs.Add(new OpLog(OpLog.OP_ADD, index, item));
			AddSet?.Add(item);
		}

		public void SetItem(int index, E item)
		{
			var old = Value[index];
			Value = Value.SetItem(index, item);
			OpLogs.Add(new OpLog(OpLog.OP_MODIFY, index, item));
			if (null != AddSet)
			{
                AddSet.Add(item);
				AddSet.Remove(old);
            }
        }

		public void RemoveAt(int index)
		{
			var Old = Value[index];
			Value = Value.RemoveAt(index);
			OpLogs.Add(new OpLog(OpLog.OP_REMOVE, index, default));
			AddSet?.Remove(Old);
		}

		public void RemoveRange(int index, int count)
        {
            var end = index + count;
            var oldItems = new E[count];
			for (int i = index; i < end; ++i)
                oldItems[i - index] = Value[i];

            Value = Value.RemoveRange(index, count);
			for (int i = index; i < end; ++i)
			{
                OpLogs.Add(new OpLog(OpLog.OP_REMOVE, i, default));
				AddSet?.Remove(oldItems[i - index]);
            }
        }

		internal override void EndSavepoint(Savepoint currentSp)
		{
			if (currentSp.Logs.TryGetValue(LogKey, out var log))
			{
				var currentLog = (LogList1<E>)log;
				currentLog.Value = this.Value;
				currentLog.Merge(this);
			}
			else
			{
				currentSp.PutLog(this);
			}
		}

		public void Merge(LogList1<E> from)
		{
			if (from.OpLogs.Count > 0)
            {
				if (from.OpLogs[0].op == OpLog.OP_CLEAR)
					OpLogs.Clear();
				OpLogs.AddRange(from.OpLogs);

				if (AddSet != null && from.AddSet != null)
				{
					foreach (var item in from.AddSet)
						AddSet.Add(item);
                }
            }
		}

		internal override Log BeginSavepoint()
		{
			var dup = new LogList1<E>();
			dup.This = This;
			dup.Belong = Belong;
			dup.VariableId = VariableId;
			dup.Value = Value;
			return dup;
		}
#endif
		public override string ToString()
        {
			var sb = new StringBuilder();
			sb.Append("OpLogs:");
			Str.BuildString(sb, OpLogs);
            return sb.ToString();
        }

		public override void Encode(ByteBuffer bb)
		{
			bb.WriteUInt(OpLogs.Count);
			foreach (var opLog in OpLogs)
			{
				bb.WriteUInt(opLog.op);
				if (opLog.op < OpLog.OP_CLEAR)
				{
					bb.WriteUInt(opLog.index);
					if (opLog.op < OpLog.OP_REMOVE)
						SerializeHelper<E>.Encode(bb, opLog.value);
				}
			}
		}

        public override void Decode(ByteBuffer bb)
        {
			OpLogs.Clear();
			for (var logSize = bb.ReadUInt(); --logSize >= 0;)
			{
				int op = bb.ReadUInt();
				int index = op < OpLog.OP_CLEAR ? bb.ReadUInt() : 0;
				E value = default;
				if (op < OpLog.OP_REMOVE)
					value = SerializeHelper<E>.Decode(bb);
				OpLogs.Add(new OpLog(op, index, value));
			}
		}
	}
}
