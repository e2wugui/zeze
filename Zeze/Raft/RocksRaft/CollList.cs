using System.Collections;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Text;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	public abstract class CollList<E> : Collection, IEnumerable<E>, IEnumerable
	{
		internal ImmutableList<E> _list = ImmutableList<E>.Empty;

		public abstract void Add(E item);
		public abstract void AddRange(IEnumerable<E> items);
		public abstract void Clear();
		public abstract void Insert(int index, E item);
		public abstract bool Remove(E item);
		public abstract void RemoveAt(int index);
		public abstract void RemoveRange(int index, int count);
		public abstract E this[int index] { get; set; }


		protected ImmutableList<E> Data
		{
			get
			{
				if (IsManaged)
				{
					var txn = Transaction.Current;
					if (txn == null)
						return _list;
					Log log = txn.GetLog(Parent.ObjectId + VariableId);
					if (log == null)
						return _list;
					var listLog = (LogList<E>)log;
					return listLog.Value;
				}
				return _list;
			}
		}

		public bool IsEmpty => Data.Count == 0;
		public int Count => Data.Count;

		public bool Contains(E v)
		{
			return Data.Contains(v);
		}

		public void CopyTo(E[] array, int arrayIndex)
		{
			Data.CopyTo(array, arrayIndex);
		}

		public int IndexOf(E o)
		{
			return Data.IndexOf(o);
		}

		public int LastIndexOf(E o)
		{
			return Data.LastIndexOf(o);
		}

		IEnumerator IEnumerable.GetEnumerator()
		{
			return Data.GetEnumerator();
		}

		IEnumerator<E> IEnumerable<E>.GetEnumerator()
		{
			return Data.GetEnumerator();
		}

		public ImmutableList<E>.Enumerator GetEnumerator()
		{
			return Data.GetEnumerator();
		}

		public override string ToString()
		{
			var sb = new StringBuilder();
			Zeze.Util.Str.BuildString(sb, Data);
			return sb.ToString();
		}

		public override void Decode(ByteBuffer bb)
		{
			Clear();
			for (int i = bb.ReadUInt(); i > 0; --i)
			{
				Add(SerializeHelper<E>.Decode(bb));
			}
		}

		public override void Encode(ByteBuffer bb)
		{
			var tmp = Data;
			bb.WriteUInt(tmp.Count);
			foreach (var e in tmp)
			{
				SerializeHelper<E>.Encode(bb, e);
			}
		}
	}
}
