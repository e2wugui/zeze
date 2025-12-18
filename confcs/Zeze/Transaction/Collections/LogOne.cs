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
                    LogBean.Encode(bb);
                }
                else
                    bb.WriteBool(false);
            }
        }

        public override void Decode(ByteBuffer bb)
        {
            var hasValue = bb.ReadBool();
            if (hasValue)
            {
                Value = new V();
                Value.Decode(bb);
            }
            else
            {
                var hasLogBean = bb.ReadBool();
                if (hasLogBean)
                {
                    LogBean = new LogBean();
                    LogBean.Decode(bb);
                }
            }
        }

        public override string ToString()
        {
            return Value.ToString();
        }
    }
}
