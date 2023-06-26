using System;
using System.Text;
using Zeze.Serialize;

namespace Zeze.Util
{
    public abstract class ConfBean : Serializable
    {
        public int VariableId { get; } // reserve

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

        public abstract void ClearParameters();
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

        public Func<ConfBean, long> GetSpecialTypeIdFromBean { get; }
        public Func<long, ConfBean> CreateBeanFromSpecialTypeId { get; }

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
                if (null == value)
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

        public override void ClearParameters()
        {
            Bean.ClearParameters();
        }
    }
}
