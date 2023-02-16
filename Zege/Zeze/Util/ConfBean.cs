using System;
using System.Reflection;
using System.Text;
using Zeze.Serialize;
using Zeze.Transaction;
using Zeze.Transaction.Collections;

namespace Zeze.Util
{
    public abstract class ConfBean : Serializable
    {
        public abstract long TypeId { get; }
        public int VariableId { get; } // reserve

        public virtual void Encode(ByteBuffer bb)
        {
            throw new NotImplementedException();
        }
        public abstract void Decode(ByteBuffer bb);

        public virtual ConfBean Copy()
        {
            throw new NotImplementedException();
        }

        public virtual bool NegativeCheck()
        {
            return false;
        }

        public virtual void BuildString(StringBuilder sb, int level)
        {
            sb.Append(new string(' ', level)).Append("{}").Append(System.Environment.NewLine);
        }

        public ConfBean()
        {

        }

        public ConfBean(int varid)
        {
            VariableId = varid;
        }

        public virtual void FollowerApply(Zeze.Transaction.Log log)
        { 
        }
    }

    public class ConfEmptyBean : ConfBean
    {
        public override void Decode(ByteBuffer bb)
        {
            bb.ReadByte();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteByte(0);
        }

        public const long TYPEID = 0; // 用0，而不是Bean.Hash("")，可能0更好吧。

        public override long TypeId => TYPEID;
        public override ConfBean Copy()
        {
            return ConfEmptyBean.Instance;
        }
        public override string ToString()
        {
            return "()";
        }

        public readonly static ConfEmptyBean Instance = new();
    }

    public class ConfDynamicBean : ConfBean
    {
        public override long TypeId => _TypeId;
        public ConfBean Bean
        {
            get
            {
                return _Bean;
            }

            set
            {
                if (null == value)
                    throw new System.ArgumentNullException(nameof(value));

                _TypeId = GetSpecialTypeIdFromBean(value);
                _Bean = value;
            }
        }

        private long _TypeId;
        private ConfBean _Bean;

        public Func<ConfBean, long> GetSpecialTypeIdFromBean { get; }
        public Func<long, ConfBean> CreateBeanFromSpecialTypeId { get; }

        public ConfDynamicBean(int variableId, Func<ConfBean, long> get, Func<long, ConfBean> create)
            : base(variableId)
        {
            _Bean = ConfEmptyBean.Instance;
            _TypeId = ConfEmptyBean.TYPEID;

            GetSpecialTypeIdFromBean = get;
            CreateBeanFromSpecialTypeId = create;
        }

        public bool IsEmpty()
        {
            return _TypeId == ConfEmptyBean.TYPEID && _Bean.GetType() == typeof(ConfEmptyBean);
        }

        public void Assign(ConfDynamicBean other)
        {
            Bean = other.Bean.Copy();
        }

        public override bool NegativeCheck()
        {
            return Bean.NegativeCheck();
        }

        public override ConfDynamicBean Copy()
        {
            var copy = new ConfDynamicBean(VariableId, GetSpecialTypeIdFromBean, CreateBeanFromSpecialTypeId);
            copy._Bean = Bean.Copy();
            copy._TypeId = TypeId;
            return copy;
        }

        private void SetBeanWithSpecialTypeId(long specialTypeId, ConfBean bean)
        {
            _TypeId = specialTypeId;
            _Bean = bean;
        }

        public override void Decode(ByteBuffer bb)
        {
            // 由于可能在事务中执行，这里仅修改Bean
            // TypeId 在 Bean 提交时才修改，但是要在事务中读到最新值，参见 TypeId 的 getter 实现。
            long typeId = bb.ReadLong();
            ConfBean real = CreateBeanFromSpecialTypeId(typeId);
            if (real != null)
            {
                real.Decode(bb);
                SetBeanWithSpecialTypeId(typeId, real);
            }
            else
            {
                bb.SkipUnknownField(ByteBuffer.BEAN);
                SetBeanWithSpecialTypeId(ConfEmptyBean.TYPEID, ConfEmptyBean.Instance);
            }
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteLong(TypeId);
            Bean.Encode(bb);
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var dlog = (LogConfDynamic)log;
            if (null != dlog.Value)
            {
                // 内部Bean整个被替换。
                _TypeId = dlog.SpecialTypeId;
                _Bean = dlog.Value;
            }
            else if (null != dlog.LogBean) // 安全写法，不检查应该是没问题的？
            {
                // 内部Bean发生了改变。
                _Bean.FollowerApply(dlog.LogBean);
            }
        }
    }

    // see Zeze.Transaction.Bean.cs::LogDynamic
    public class LogConfDynamic : LogBean
    {
        public readonly static new string StableName = Util.Reflect.GetStableName(typeof(LogConfDynamic));
        public readonly static new int TypeId_ = Util.FixedHash.Hash32(StableName);

        public override int TypeId => TypeId_;

        public long SpecialTypeId { get; private set; }
        public ConfBean Value { get; private set; }
        public LogBean LogBean { get; private set; }

        public override void Encode(ByteBuffer bb)
        {
            throw new NotImplementedException();
        }

        public override void Decode(ByteBuffer bb)
        {
            var parentTypeName = bb.ReadString();
            var varId = bb.ReadInt();
            var hasValue = bb.ReadBool();
            if (hasValue)
            {
                SpecialTypeId = bb.ReadLong();
                var parentType = Zeze.Util.Reflect.GetType(parentTypeName);
                var factory = parentType.GetMethod("CreateBeanFromSpecialTypeId_" + varId, BindingFlags.Static | BindingFlags.Public, new Type[] { typeof(long) });
                Value = (ConfBean)factory.Invoke(null, new object[] { SpecialTypeId });
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
