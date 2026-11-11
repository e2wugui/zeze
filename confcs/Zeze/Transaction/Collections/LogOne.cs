using Zeze.Serialize;
using Zeze.Util;

namespace Zeze.Transaction.Collections
{
    public class LogOne<V> : LogBean
        where V : ConfBean, new()
    {
        public new static readonly string StableName = Reflect.GetStableName(typeof(LogOne<V>));
        public new static readonly int TypeId_ = FixedHash.Hash32(StableName);

        public override int TypeId => TypeId_;

        public V Value { get; internal set; }
        public LogBean LogBean { get; private set; }

        public override void Encode(ByteBuffer bb)
        {
            if (Value != null)
            {
                bb.WriteBool(true);
                Value.Encode(bb);
            }
            else
            {
                bb.WriteBool(false); // Value Tag
                if (LogBean != null)
                {
                    bb.WriteBool(true);
                    EncodeLogBean(bb, LogBean);
                }
                else
                    bb.WriteBool(false);
            }
        }

        public override void Decode(ByteBuffer bb)
        {
            V value = null;
            LogBean logBean = null;
            if (bb.ReadBool())
            {
                value = new V();
                value.Decode(bb);
            }
            else if (bb.ReadBool())
                logBean = DecodeLogBean(bb);

            Value = value;
            LogBean = logBean;
        }

        public override string ToString()
        {
            return Value.ToString();
        }
    }
}
