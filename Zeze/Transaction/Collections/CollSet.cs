using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Transaction.Collections
{
	public abstract class CollSet<V> : Collection, ISet<V>, IReadOnlySet<V>, IEnumerable<V>, IEnumerable
	{
		internal System.Collections.Immutable.ImmutableHashSet<V> _set = System.Collections.Immutable.ImmutableHashSet<V>.Empty;

		public abstract bool Add(V item);
        public abstract void Clear();
        public abstract void ExceptWith(IEnumerable<V> other);
        public abstract void IntersectWith(IEnumerable<V> other);
        public abstract bool Remove(V item);
        public abstract void SymmetricExceptWith(IEnumerable<V> other);
        public abstract void UnionWith(IEnumerable<V> other);

        protected System.Collections.Immutable.ImmutableHashSet<V> Set
        {
			get
            {
				if (IsManaged)
				{
                    var txn = Transaction.Current;
                    if (txn == null) return _set;
                    txn.VerifyRecordAccessed(this, true);
					if (false == txn.TryGetLog(Parent.ObjectId + VariableId, out var log))
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

        public bool IsReadOnly => false;

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

        public void AddAll(IEnumerable<V> items)
        {
            foreach (var item in items)
                Add(item);
        }

        public bool IsProperSubsetOf(IEnumerable<V> other)
        {
            return Set.IsProperSubsetOf(other);
        }

        public bool IsProperSupersetOf(IEnumerable<V> other)
        {
            return Set.IsProperSupersetOf(other);
        }

        public bool IsSubsetOf(IEnumerable<V> other)
        {
            return Set.IsSubsetOf(other);
        }

        public bool IsSupersetOf(IEnumerable<V> other)
        {
            return Set.IsSupersetOf(other);
        }

        public bool Overlaps(IEnumerable<V> other)
        {
            return Set.Overlaps(other);
        }

        public bool SetEquals(IEnumerable<V> other)
        {
            return Set.SetEquals(other);
        }

        void ICollection<V>.Add(V item)
        {
            Add(item);
        }

        public void CopyTo(V[] array, int arrayIndex)
        {
            int index = arrayIndex;
            foreach (var e in Set)
            {
                array[index++] = e;
            }
        }
    }
}
