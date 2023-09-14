using System.Collections.Generic;
#if !USE_CONFCS
using System.Linq;
#endif
using System.Text;
using Zeze.Serialize;
using Zeze.Util;

namespace Zeze.Transaction.Collections
{
    public class LogSet1<V> : LogSet<V>
    {
        public new static readonly string StableName = Reflect.GetStableName(typeof(LogSet1<V>));
        public new static readonly int TypeId_ = FixedHash.Hash32(StableName);

        public override int TypeId => TypeId_;

        public readonly ISet<V> Added = new HashSet<V>();
        public readonly ISet<V> Removed = new HashSet<V>();

#if !USE_CONFCS
		public override void Collect(Changes changes, Bean recent, Log vlog)
		{
			throw new System.Exception($"Collect Not Implement.");
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

		public void ExceptWith(IEnumerable<V> other)
		{
			var newset = Value.Except(other);
			if (newset != Value)
			{
				foreach (var item in other)
				{
					Removed.Add(item);
					Added.Remove(item);
				}
				Value = newset;
			}
		}

		public void IntersectWith(IEnumerable<V> other)
		{
			var newset = Value.Intersect(other);
			if (newset != Value)
			{
				foreach (var item in Value)
				{
					if (!other.Contains(item))
					{
						Removed.Add(item);
						Added.Remove(item);
					}
				}
				Value = newset;
			}
		}

		public void SymmetricExceptWith(IEnumerable<V> other)
		{
			var newset = Value.SymmetricExcept(other);
			if (newset != Value)
			{
				// this: 1,2 other: 2,3 result: 1,3
				foreach (var item in other)
				{
					if (Value.Contains(item))
					{
						Removed.Add(item);
						Added.Remove(item);
					}
					else
					{
						Added.Add(item);
						Removed.Remove(item);
					}
				}
				Value = newset;
			}
		}

		public void UnionWith(IEnumerable<V> other)
		{
			var newset = Value.Union(other);
			if (newset != Value)
			{
				foreach (var item in other)
				{
					if (!Value.Contains(item))
                    {
						Added.Add(item);
						Removed.Remove(item);
					}
				}
				Value = newset;
			}
		}

		public void Clear()
		{
			foreach (var e in Value)
			{
				Remove(e);
			}
			Value = System.Collections.Immutable.ImmutableHashSet<V>.Empty;
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
            var dup = new LogSet1<V>();
            dup.This = This;
            dup.Belong = Belong;
            dup.VariableId = VariableId;
            dup.Value = Value;
            return dup;
        }
#endif
        public override void Decode(ByteBuffer bb)
        {
            Added.Clear();
            for (int i = bb.ReadUInt(); i > 0; --i)
            {
                var value = SerializeHelper<V>.Decode(bb);
                Added.Add(value);
            }

            Removed.Clear();
            for (int i = bb.ReadUInt(); i > 0; --i)
            {
                var key = SerializeHelper<V>.Decode(bb);
                Removed.Add(key);
            }
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteUInt(Added.Count);
            foreach (var e in Added)
                SerializeHelper<V>.Encode(bb, e);

            bb.WriteUInt(Removed.Count);
            foreach (var e in Removed)
                SerializeHelper<V>.Encode(bb, e);
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
