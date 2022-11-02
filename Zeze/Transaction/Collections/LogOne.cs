using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using Zeze.Arch.Gen;
using Zeze.Serialize;

namespace Zeze.Transaction.Collections
{
    public class LogOne<V> : LogBean where V : Bean, new()
    {
        public V Value { get; internal set; }
        public LogBean LogBean { get; private set; }

        public void SetValue(V value)
        {
            Value = value;
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

        public override void Encode(ByteBuffer bb)
        {
            // encode Value & SpecialTypeId. Value maybe null.
            var self = (CollOne<V>)This;
            if (null != Value)
            {
                bb.WriteBool(true);
                Value.Encode(bb);
            }
            else
            {
                bb.WriteBool(false); // Value Tag
                if (null != LogBean)
                {
                    bb.WriteBool(true);
                    LogBean.Encode(bb);
                }
                else
                {
                    bb.WriteBool(false);
                }
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
    }
}
