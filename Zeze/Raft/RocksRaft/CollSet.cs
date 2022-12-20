using System.Collections;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Text;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	public abstract class CollSet<V> : Collection, IEnumerable<V>
	{
		internal ImmutableHashSet<V> _set = ImmutableHashSet<V>.Empty;

		public abstract bool Add(V item);

		public abstract bool Remove(V item);

		public abstract void Clear();

		protected ImmutableHashSet<V> Set
        {
			get
            {
				if (IsManaged)
				{
					if (Transaction.Current == null) return _set;
					if (false == Transaction.Current.TryGetLog(Parent.ObjectId + VariableId, out var log))
						return _set;
					var setlog = (LogSet<V>)log;
					return setlog.Value;
				}
				else
				{
					return _set;
				}
			}
		}

		public int Count => Set.Count;

		public bool Contains(V v)
        {
			return Set.Contains(v);
        }

		IEnumerator IEnumerable.GetEnumerator()
		{
			return Set.GetEnumerator();
		}
		IEnumerator<V> IEnumerable<V>.GetEnumerator()
		{
			return Set.GetEnumerator();
		}

		public override string ToString()
        {
			var sb = new StringBuilder();
			ByteBuffer.BuildString(sb, Set);
            return sb.ToString();
        }

		public override void Decode(ByteBuffer bb)
		{
			Clear();
			for (int i = bb.ReadUInt(); i > 0; --i)
			{
				var value = SerializeHelper<V>.Decode(bb);
				Add(value);
			}
		}

		public override void Encode(ByteBuffer bb)
		{
			var tmp = Set;
			bb.WriteUInt(tmp.Count);
			foreach (var e in tmp)
			{
				SerializeHelper<V>.Encode(bb, e);
			}
		}

		public override int GetHashCode()
		{
			int h = 0;
			foreach (var v in _set)
			{
				if (v != null)
					h += v.GetHashCode();
			}
			return h;
		}

		public override bool Equals(object obj)
		{
			if (obj == this)
				return true;
			if (obj is not CollSet<V> c)
				return false;
			if (c.Count != Count)
				return false;
			foreach (var v in c._set)
			{
				if (!_set.Contains(v))
					return false;
			}
			return true;
		}
    }
}
