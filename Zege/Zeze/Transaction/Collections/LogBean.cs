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

#if !USE_CONFCS
		public Bean This { get; set; }

		// LogBean仅在_final_commit的Collect过程中创建，不会参与Savepoint。
        internal override Log BeginSavepoint()
        {
            throw new System.NotImplementedException();
        }

        internal override void EndSavepoint(Savepoint currentsp)
        {
            throw new System.NotImplementedException();
        }

        // 仅发生在事务执行期间。Decode-Apply不会执行到这里。
        public override void Collect(Changes changes, Bean recent, Log vlog)
        {
            if (Variables.TryAdd(vlog.VariableId, vlog))
            {
                // 向上传递
                changes.Collect(recent, this);
            }
        }

        public override void Commit()
        {
            throw new System.NotImplementedException();
        }

        public static void EncodeLogBean(ByteBuffer bb, LogBean logBean)
        {
            // 使用byte，未来可能扩展其他LogBean子类。
            if (logBean is LogDynamic)
                bb.WriteByte(1);

            else // 全部都是LogBean子类，只能else。
                bb.WriteByte(0);
            logBean.Encode(bb);
        }
#endif

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
#if USE_CONFCS
                    logBean = new LogConfDynamic();
#else
                    logBean = new LogDynamic();
#endif
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
