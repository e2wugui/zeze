
using System;
using System.Collections.Generic;
using System.Collections.Immutable;

namespace Zeze.Raft.RocksRaft
{

	public class LogList1<E> : LogList<E>
	{
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

		public List<OpLog> OpLogs { get; } = new();

		public override void Collect(Changes changes, Bean recent, Log vlog)
		{
			throw new NotImplementedException("Collect Not Implement.");
		}

		public void Add(E item)
		{
			if (item == null)
				throw new ArgumentNullException("value is null");
			Value = Value.Add(item);
			OpLogs.Add(new OpLog(OpLog.OP_ADD, Value.Count - 1, item));
		}

		public void AddRange(IEnumerable<E> items)
        {
			var start = Value.Count;
			Value = Value.AddRange(items);
			foreach (var item in items)
			{
				OpLogs.Add(new OpLog(OpLog.OP_ADD, start++, item));
			}
        }

		public bool Remove(E item)
		{
			var index = Value.IndexOf(item);
			if (index < 0)
				return false;
			RemoveAt(index);
			return true;
		}

		public void Clear()
		{
			Value = ImmutableList<E>.Empty;
			OpLogs.Clear();
			OpLogs.Add(new OpLog(OpLog.OP_CLEAR, 0, default));
		}

		public void Insert(int index, E item)
		{
			Value = Value.Insert(index, item);
			OpLogs.Add(new OpLog(OpLog.OP_ADD, index, item));
		}

		public void SetItem(int index, E item)
		{
			Value = Value.SetItem(index, item);
			OpLogs.Add(new OpLog(OpLog.OP_MODIFY, index, item));
		}

		public void RemoveAt(int index)
		{
			Value = Value.RemoveAt(index);
			OpLogs.Add(new OpLog(OpLog.OP_REMOVE, index, default));
		}

		public void RemoveRange(int index, int count)
        {
			Value = Value.RemoveRange(index, count);
			var end = index + count;
			for (int i = index; i < end; ++i)
				OpLogs.Add(new OpLog(OpLog.OP_REMOVE, i, default));
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
			}
		}

		internal override Log BeginSavepoint()
		{
			var dup = new LogList1<E>();
			dup.Belong = Belong;
			dup.VariableId = VariableId;
			dup.Value = Value;
			return dup;
		}
	}
}
