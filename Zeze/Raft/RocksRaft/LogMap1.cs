using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;
using System.Collections.Immutable;

namespace Zeze.Raft.RocksRaft
{
	public class LogMap1<K, V> : LogMap<K, V>
	{
		public Dictionary<K, V> Putted { get; } = new Dictionary<K, V>();
		public ISet<K> Removed { get; } = new HashSet<K>();

		public V Get(K key)
		{
			if (Value.TryGetValue(key, out V exist))
				return exist;
			return default(V);
		}

		public void Add(K key, V value)
		{
			Value = Value.Add(key, value);
			Putted[key] = value;
			Removed.Remove(key);
		}

		public void Put(K key, V value)
		{
			Value = Value.SetItem(key, value);
			Putted[key] = value;
			Removed.Remove(key);
		}

		public void Remove(K key)
		{
			Value = Value.Remove(key);
			Putted.Remove(key);
			Removed.Add(key);
		}

		public void Clear()
        {
			foreach (var e in Value)
			{
				Remove(e.Key);
			}
			Value = ImmutableDictionary<K, V>.Empty;
        }

		public override void Decode(ByteBuffer bb)
		{
			Putted.Clear();
			for (int i = bb.ReadUInt(); i > 0; --i)
			{
				var key = SerializeHelper<K>.Decode(bb);
				var value = SerializeHelper<V>.Decode(bb);
				Putted.Add(key, value);
			}

			Removed.Clear();
			for (int i = bb.ReadUInt(); i > 0; --i)
			{
				var key = SerializeHelper<K>.Decode(bb);
				Removed.Add(key);
			}
		}

		public override void Encode(ByteBuffer bb)
		{
			bb.WriteUInt(Putted.Count);
			foreach (var p in Putted)
			{
				SerializeHelper<K>.Encode(bb, p.Key);
				SerializeHelper<V>.Encode(bb, p.Value);
			}

			bb.WriteUInt(Removed.Count);
			foreach (var r in Removed)
			{
				SerializeHelper<K>.Encode(bb, r);
			}
		}

		internal override void EndSavepoint(Savepoint currentsp)
		{
			if (currentsp.Logs.TryGetValue(LogKey, out var log))
			{
				var currentLog = (LogMap1<K, V>)log;
				currentLog.Value = this.Value;
				currentLog.MergeChangeNote(this);
			}
			else
            {
				currentsp.Logs[LogKey] = this;
			}
		}

		private void MergeChangeNote(LogMap1<K, V> another)
        {
			// Put,Remove 需要确认有没有顺序问题
			// this: replace 1,3 remove 2,4 nest: replace 2 remove 1
			foreach (var e in another.Putted)
			{
				// replace 1,2,3 remove 4
				Putted[e.Key] = e.Value;
				Removed.Remove(e.Key);
			}

			foreach (var e in another.Removed)
            {
				// replace 2,3 remove 1,4
				Putted.Remove(e);
				Removed.Add(e);
			}
		}

        internal override Log BeginSavepoint()
        {
			var dup = new LogMap1<K, V>();
			dup.Belong = Belong;
			dup.VariableId = VariableId;
			dup.Value = Value;
			return dup;
		}

		public override string ToString()
		{
			var sb = new StringBuilder();
			sb.Append(" Putted:");
			ByteBuffer.BuildString(sb, Putted);
			sb.Append(" Removed:");
			ByteBuffer.BuildString(sb, Removed);
			return sb.ToString();
		}
	}
}
