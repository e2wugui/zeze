using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;
using Zeze.Util;

namespace Zeze.Transaction.Collections
{
    public class LogBean : Log
    {
        public static readonly string StableName = Reflect.GetStableName(typeof(LogBean));
        public static readonly int TypeId_ = FixedHash.Hash32(StableName);

        public override int TypeId => TypeId_;

        public Dictionary<int, Log> Variables { get; } = new Dictionary<int, Log>();

        public static LogBean DecodeLogBean(ByteBuffer bb)
        {
            var type = bb.ReadByte();
            LogBean logBean;
            switch (type)
            {
                case 0:
                    logBean = new LogBean();
                    break;
                case 1:
                    logBean = new LogConfDynamic();
                    break;
                default:
                    throw new System.Exception("unknown logBean subclass type=" + type);
            }
            logBean.Decode(bb);
            return logBean;
        }

        public override void Decode(ByteBuffer bb)
        {
            for (int i = bb.ReadUInt(); i > 0; --i)
            {
                var typeId = bb.ReadInt4();
                var log = Create(typeId);

                var varId = bb.ReadUInt();
                log.VariableId = varId;
                log.Decode(bb);

                Variables.Add(varId, log);
            }
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteUInt(Variables.Count);
            foreach (var log in Variables.Values)
            {
                bb.WriteInt4(log.TypeId);
                bb.WriteUInt(log.VariableId);
                log.Encode(bb);
            }
        }

        public override string ToString()
        {
            var sb = new StringBuilder();
            ByteBuffer.BuildString(sb, Variables, ComparerInt.Instance);
            return sb.ToString();
        }
    }
}
