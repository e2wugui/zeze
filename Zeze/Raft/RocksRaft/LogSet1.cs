using System;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	public class LogSet1<V> : LogSet<V>
	{
		public ISet<V> Added { get; } = new HashSet<V>();
		public ISet<V> Removed { get; } = new HashSet<V>();

		public override void Collect(Changes changes, Bean recent, Log vlog)
		{
			throw new Exception($"Collect Not Implement.");
		}

		public bool Add(V item)
		{
			var newset = Value.Add(item);
			if (newset != Value)
            {
				Added.Add(item);
				Removed.Remove(item);
				Value = newset;
				return true;
            }
			return false;
		}

		public bool Remove(V item)
        {
			var newset = Value.Remove(item);
			if (newset != Value)
			{
				Removed.Add(item);
				Added.Remove(item);
				Value = newset;
				return true;
			}
			return false;
        }

		public void Clear()
		{
			foreach (var e in Value)
			{
				Remove(e);
			}
			Value = ImmutableHashSet<V>.Empty;
		}

		public override void Decode(ByteBuffer bb)
		{
			Added.Clear();
			for (int i = bb.ReadInt(); i > 0; --i)
			{
				var value = SerializeHelper<V>.Decode(bb);
				Added.Add(value);
			}

			Removed.Clear();
			for (int i = bb.ReadInt(); i > 0; --i)
			{
				var key = SerializeHelper<V>.Decode(bb);
				Removed.Add(key);
			}
		}

		public override void Encode(ByteBuffer bb)
		{
			bb.WriteInt(Added.Count);
			foreach (var e in Added)
			{
				SerializeHelper<V>.Encode(bb, e);
			}

			bb.WriteInt(Removed.Count);
			foreach (var e in Removed)
			{
				SerializeHelper<V>.Encode(bb, e);
			}
		}

		internal override void EndSavepoint(Savepoint currentsp)
		{
			if (currentsp.Logs.TryGetValue(LogKey, out var log))
			{
				var currentLog = (LogSet1<V>)log;
				currentLog.Value = this.Value;
				currentLog.Merge(this);
			}
			else
			{
				currentsp.Logs[LogKey] = this;
			}
		}

		internal void Merge(LogSet1<V> from)
		{
			// Put,Remove 需要确认有没有顺序问题
			// this: add 1,3 remove 2,4 nest: add 2 remove 1
			foreach (var e in from.Added) Add(e); // replace 1,2,3 remove 4
			foreach (var e in from.Removed) Remove(e); // replace 2,3 remove 1,4
		}

		internal override Log BeginSavepoint()
		{
			var dup = new LogSet<V>();
			dup.Belong = Belong;
			dup.VariableId = VariableId;
			dup.Value = Value;
			return dup;
		}

		public override string ToString()
		{
			var sb = new StringBuilder();
			sb.Append(" Added:");
			ByteBuffer.BuildString(sb, Added);
			sb.Append(" Removed:");
			ByteBuffer.BuildString(sb, Removed);
			return sb.ToString();
		}

	}
}
