using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;
using Zeze.Util;

namespace Zeze.Transaction.Collections
{
    public class LogList2<E> : LogList1<E>
        where E : ConfBean, new()
    {
        public new static readonly string StableName = Reflect.GetStableName(typeof(LogList2<E>));
        public new static readonly int TypeId_ = FixedHash.Hash32(StableName);

        public override int TypeId => TypeId_;

        public class OutInt
        {
            public int Value;

            public override string ToString()
            {
                return Value.ToString();
            }
        }

        public readonly Dictionary<LogBean, OutInt> Changed = new Dictionary<LogBean, OutInt>(); // changed V logs. using in collect.

        public override void Encode(ByteBuffer bb)
        {
            throw new System.NotImplementedException();
        }

        public override void Decode(ByteBuffer bb)
        {
            Changed.Clear();
            for (int i = bb.ReadUInt(); i > 0; i--)
            {
                var value = DecodeLogBean(bb);
                var index = bb.ReadUInt();
                Changed[value] = new OutInt { Value = index };
            }
            // decode opLogs
            base.Decode(bb);
        }

        public override string ToString()
        {
            var sb = new StringBuilder();
            sb.Append("OpLogs:");
            ByteBuffer.BuildString(sb, OpLogs);
            sb.Append(" Changed:");
            ByteBuffer.BuildString(sb, Changed);
            return sb.ToString();
        }
    }
}
