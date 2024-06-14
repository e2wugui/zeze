using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;
using Zeze.Util;

namespace Zeze.Transaction.Collections
{
    public class LogMap1<K, V> : LogMap<K, V>
    {
        public new static readonly string StableName = Reflect.GetStableName(typeof(LogMap1<K, V>));
        public new static readonly int TypeId_ = FixedHash.Hash32(StableName);

        public override int TypeId => TypeId_;

        public readonly Dictionary<K, V> Replaced = new Dictionary<K, V>();
        public readonly ISet<K> Removed = new HashSet<K>();

#if !USE_CONFCS
        public V Get(K key)
        {
            if (Value.TryGetValue(key, out V exist))
                return exist;
            return default(V);
        }

        public void Add(K key, V value)
        {
            Value = Value.Add(key, value);
            Replaced[key] = value;
            Removed.Remove(key);
        }

        public void AddRange(IEnumerable<KeyValuePair<K, V>> pairs)
        {
            foreach (var pair in pairs)
            {
                Add(pair.Key, pair.Value);
            }
        }

        public void SetItem(K key, V value)
        {
            Value = Value.SetItem(key, value);
            Replaced[key] = value;
            Removed.Remove(key);
        }

        public void SetItems(IEnumerable<KeyValuePair<K, V>> items)
        {
            foreach (var item in items)
            {
                SetItem(item.Key, item.Value);
            }
        }

        public bool Remove(K key)
        {
            var newValue = Value.Remove(key);
            if (newValue != Value)
            {
                Value = newValue;
                Replaced.Remove(key);
                Removed.Add(key);
                return true;
            }
            return false;
        }

        public bool Remove(KeyValuePair<K, V> item)
        {
            if (Value.TryGetKey(item.Key, out var keyVal) && keyVal.Equals(item.Value))
            {
                return Remove(item.Key);
            }
            return false;
        }

        public void Clear()
        {
            foreach (var e in Value)
            {
                Remove(e.Key);
            }
            Value = System.Collections.Immutable.ImmutableDictionary<K, V>.Empty;
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
            foreach (var e in another.Replaced)
            {
                // replace 1,2,3 remove 4
                Replaced[e.Key] = e.Value;
                Removed.Remove(e.Key);
            }
            foreach (var e in another.Removed)
            {
                // replace 2,3 remove 1,4
                Replaced.Remove(e);
                Removed.Add(e);
            }
        }

        internal override Log BeginSavepoint()
        {
            var dup = new LogMap1<K, V>();
            dup.This = This;
            dup.Belong = Belong;
            dup.VariableId = VariableId;
            dup.Value = Value;
            return dup;
        }

#endif

        public override void Decode(ByteBuffer bb)
        {
            Replaced.Clear();
            for (int i = bb.ReadUInt(); i > 0; --i)
            {
                var key = SerializeHelper<K>.Decode(bb);
                var value = SerializeHelper<V>.Decode(bb);
                Replaced.Add(key, value);
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
            bb.WriteUInt(Replaced.Count);
            foreach (var p in Replaced)
            {
                SerializeHelper<K>.Encode(bb, p.Key);
                SerializeHelper<V>.Encode(bb, p.Value);
            }

            bb.WriteUInt(Removed.Count);
            foreach (var r in Removed)
                SerializeHelper<K>.Encode(bb, r);
        }

        public override string ToString()
        {
            var sb = new StringBuilder();
            sb.Append(" Putted:");
            ByteBuffer.BuildString(sb, Replaced);
            sb.Append(" Removed:");
            ByteBuffer.BuildString(sb, Removed);
            return sb.ToString();
        }
    }
}
