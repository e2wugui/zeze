using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	public abstract class CollSet<V> : Collection, IEnumerable<V>, IEnumerable
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
			for (int i = bb.ReadInt(); i > 0; --i)
			{
				var value = SerializeHelper<V>.Decode(bb);
				Add(value);
			}
		}

		public override void Encode(ByteBuffer bb)
		{
			var tmp = Set;
			bb.WriteInt(tmp.Count);
			foreach (var e in tmp)
			{
				SerializeHelper<V>.Encode(bb, e);
			}
		}
    }
}
