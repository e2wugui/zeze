using System.Collections.Generic;
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
