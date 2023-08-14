using System;
using System.Text;
using Zeze.Serialize;
using Zeze.Transaction;
using Zeze.Transaction.Collections;

namespace Zeze.Util
{
    public abstract class ConfBean : Serializable
    {
        public readonly int VariableId; // reserve

        protected ConfBean()
        {
        }

        protected ConfBean(int varId)
        {
            VariableId = varId;
        }

        public abstract long TypeId { get; }

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
            sb.Append(new string(' ', level)).Append("{}").Append(Environment.NewLine);
        }

        public virtual void FollowerApply(Log log)
        {
        }

        public virtual void ClearParameters()
        {
            throw new NotImplementedException();
        }
    }

    public class ConfEmptyBean : ConfBean
    {
        public const long TYPEID = 0; // 用0，而不是Bean.Hash("")，可能0更好吧。
        public static readonly ConfEmptyBean Instance = new ConfEmptyBean();

        public override long TypeId => TYPEID;

        public override void Decode(ByteBuffer bb)
        {
            bb.ReadByte();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteByte(0);
        }

        public override ConfBean Copy()
        {
            return Instance;
        }

        public override string ToString()
        {
            return "()";
        }

        public override void ClearParameters()
        {
        }
    }

    public class ConfDynamicBean : ConfBean
    {
        private long _TypeId;
        private ConfBean _Bean;

        public readonly Func<ConfBean, long> GetSpecialTypeIdFromBean;
        public readonly Func<long, ConfBean> CreateBeanFromSpecialTypeId;

        public ConfDynamicBean(int variableId, Func<ConfBean, long> get, Func<long, ConfBean> create) : base(variableId)
        {
            _Bean = ConfEmptyBean.Instance;
            _TypeId = ConfEmptyBean.TYPEID;

            GetSpecialTypeIdFromBean = get;
            CreateBeanFromSpecialTypeId = create;
        }

        public override long TypeId => _TypeId;

        public ConfBean Bean
        {
            get => _Bean;
            set
            {
                if (value == null)
                    throw new ArgumentNullException(nameof(value));

                _TypeId = GetSpecialTypeIdFromBean(value);
                _Bean = value;
            }
        }

        public bool IsEmpty()
        {
            return _TypeId == ConfEmptyBean.TYPEID && _Bean.GetType() == typeof(ConfEmptyBean);
        }

        public void Assign(ConfDynamicBean other)
        {
            Bean = other.Bean.Copy();
        }

        private void SetBeanWithSpecialTypeId(long specialTypeId, ConfBean bean)
        {
            _TypeId = specialTypeId;
            _Bean = bean;
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteLong(TypeId);
            Bean.Encode(bb);
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

        public override ConfBean Copy()
        {
            return new ConfDynamicBean(VariableId, GetSpecialTypeIdFromBean, CreateBeanFromSpecialTypeId)
            {
                _Bean = Bean.Copy(),
                _TypeId = TypeId
            };
        }

        public override bool NegativeCheck()
        {
            return Bean.NegativeCheck();
        }

        public override void FollowerApply(Log log)
        {
            var dLog = (LogConfDynamic)log;
            if (dLog.Value != null)
            {
                // 内部Bean整个被替换。
                _TypeId = dLog.SpecialTypeId;
                _Bean = dLog.Value;
            }
            else if (dLog.LogBean != null) // 安全写法，不检查应该是没问题的？
            {
                // 内部Bean发生了改变。
                _Bean.FollowerApply(dLog.LogBean);
            }
        }

        public override void ClearParameters()
        {
            Bean.ClearParameters();
        }
    }

    // see Zeze.Transaction.Bean.cs::LogDynamic
    public class LogConfDynamic : LogBean
    {
        public new static readonly string StableName = Reflect.GetStableName(typeof(LogConfDynamic));
        public new static readonly int TypeId_ = FixedHash.Hash32(StableName);

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
                var parentType = Reflect.GetType(parentTypeName);
                var factory = parentType.GetMethod("CreateBeanFromSpecialTypeId_" + varId, new[] { typeof(long) });
                // ReSharper disable once PossibleNullReferenceException
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
