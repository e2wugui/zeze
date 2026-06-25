using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;
using Zeze.Util;

namespace Zeze.Transaction.Collections
{
    public class LogList1<E> : LogList<E>
    {
        public new static readonly string StableName = Reflect.GetStableName(typeof(LogList1<E>));
        public new static readonly int TypeId_ = FixedHash.Hash32(StableName);

        public override int TypeId => TypeId_;

        public class OpLog
        {
            public const int OP_MODIFY = 0;
            public const int OP_ADD = 1;
            public const int OP_REMOVE = 2;
            public const int OP_CLEAR = 3;

            public readonly int op;
            public readonly int index;
            public readonly E value;

            public OpLog(int op, int index, E value)
            {
                this.op = op;
                this.index = index;
                this.value = value;
            }

            public override string ToString()
            {
                return $"({op},{index},{value})";
            }
        }

        public readonly List<OpLog> OpLogs = new List<OpLog>();

        public override string ToString()
        {
            var sb = new StringBuilder();
            sb.Append("OpLogs:");
            Str.BuildString(sb, OpLogs);
            return sb.ToString();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteUInt(OpLogs.Count);
            foreach (var opLog in OpLogs)
            {
                bb.WriteUInt(opLog.op);
                if (opLog.op < OpLog.OP_CLEAR)
                {
                    bb.WriteUInt(opLog.index);
                    if (opLog.op < OpLog.OP_REMOVE)
                        SerializeHelper<E>.Encode(bb, opLog.value);
                }
            }
        }

        public override void Decode(ByteBuffer bb)
        {
            OpLogs.Clear();
            for (var logSize = bb.ReadUInt(); --logSize >= 0;)
            {
                int op = bb.ReadUInt();
                int index = op < OpLog.OP_CLEAR ? bb.ReadUInt() : 0;
                E value = default;
                if (op < OpLog.OP_REMOVE)
                    value = SerializeHelper<E>.Decode(bb);
                OpLogs.Add(new OpLog(op, index, value));
            }
        }
    }
}
