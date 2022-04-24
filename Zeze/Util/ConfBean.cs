using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Util
{
    public abstract class ConfBean : Serializable
    {
        public abstract long TypeId { get; }
        public int VariableId { get; } // reserve

        public abstract void Encode(ByteBuffer bb);
        public abstract void Decode(ByteBuffer bb);

        public abstract ConfBean CopyBean();

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
        public override ConfBean CopyBean()
        {
            return ConfEmptyBean.Instance;
        }
        public override string ToString()
        {
            return "()";
        }

        public readonly static ConfEmptyBean Instance = new ConfEmptyBean();
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
                    throw new System.ArgumentNullException();

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
            Bean = other.Bean.CopyBean();
        }

        public override bool NegativeCheck()
        {
            return Bean.NegativeCheck();
        }

        public override ConfBean CopyBean()
        {
            var copy = new ConfDynamicBean(VariableId, GetSpecialTypeIdFromBean, CreateBeanFromSpecialTypeId);
            copy._Bean = Bean.CopyBean();
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
    }
}
