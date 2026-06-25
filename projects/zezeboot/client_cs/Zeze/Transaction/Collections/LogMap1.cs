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
