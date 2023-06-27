using Zeze.Serialize;
using Zeze.Util;

namespace Zeze.Transaction.Collections
{
    public class LogOne<V> : LogBean
#if !USE_CONFCS
        where V : Bean, new()
#else
        where V : ConfBean, new()
#endif
    {
        public new static readonly string StableName = Reflect.GetStableName(typeof(LogOne<V>));
        public new static readonly int TypeId_ = FixedHash.Hash32(StableName);

        public override int TypeId => TypeId_;

        public V Value { get; internal set; }
        public LogBean LogBean { get; private set; }

#if !USE_CONFCS
        public void SetValue(V value)
        {
            Value = value;
        }

        internal override Log BeginSavepoint()
        {
            var dup = new LogOne<V>();
            dup.This = This;
            dup.Belong = Belong;
            dup.VariableId = VariableId;
            dup.Value = Value;
            return dup;
        }

        internal override void EndSavepoint(Savepoint currentsp)
        {
            // 结束保存点，直接覆盖到当前的日志里面即可。
            currentsp.PutLog(this);
        }

        // 收集内部的Bean发生了改变。
        public override void Collect(Changes changes, Bean recent, Log vlog)
        {
            if (LogBean == null)
            {
                LogBean = (LogBean)vlog;
                changes.Collect(recent, this);
            }
        }

        public override void Commit()
        {
            if (Value != null)
            {
                ((CollOne<V>)This)._Value = Value;
            }
        }
#endif

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
