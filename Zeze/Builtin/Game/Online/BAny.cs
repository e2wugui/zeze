// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Builtin.Game.Online
{
    public interface BAnyReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public Zeze.Transaction.Bean CopyBean();

        public Zeze.Transaction.DynamicBeanReadOnly Any { get; }

    }

    public sealed class BAny : Zeze.Transaction.Bean, BAnyReadOnly
    {
        readonly Zeze.Transaction.DynamicBean _Any;

        public string _zeze_map_key_string_ { get; set; }

        public Zeze.Transaction.DynamicBean Any => _Any;
        Zeze.Transaction.DynamicBeanReadOnly Zeze.Builtin.Game.Online.BAnyReadOnly.Any => Any;

        public BAny() : this(0)
        {
        }

        public BAny(int _varId_) : base(_varId_)
        {
            _Any = new Zeze.Transaction.DynamicBean(1, Zeze.Game.Online.GetSpecialTypeIdFromBean, Zeze.Game.Online.CreateBeanFromSpecialTypeId);
        }

        public void Assign(BAny other)
        {
            Any.Assign(other.Any);
        }

        public BAny CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public BAny Copy()
        {
            var copy = new BAny();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BAny a, BAny b)
        {
            BAny save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public override Zeze.Transaction.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = 5085416693215220301;
        public override long TypeId => TYPEID;

        public static long GetSpecialTypeIdFromBean_Any(Zeze.Transaction.Bean bean)
        {
            switch (bean.TypeId)
            {
                case Zeze.Transaction.EmptyBean.TYPEID: return Zeze.Transaction.EmptyBean.TYPEID;
            }
            throw new System.Exception("Unknown Bean! dynamic@Zeze.Builtin.Game.Online.BAny:Any");
        }

        public static Zeze.Transaction.Bean CreateBeanFromSpecialTypeId_Any(long typeId)
        {
            switch (typeId)
            {
            }
            return null;
        }

        public override string ToString()
        {
            System.Text.StringBuilder sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Game.Online.BAny: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Any").Append('=').Append(Environment.NewLine);
            Any.Bean.BuildString(sb, level + 4);
            sb.Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                var _x_ = Any;
                if (!_x_.IsEmpty())
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.DYNAMIC);
                    _x_.Encode(_o_);
                }
            }
            _o_.WriteByte(0);
        }

        public override void Decode(ByteBuffer _o_)
        {
            int _t_ = _o_.ReadByte();
            int _i_ = _o_.ReadTagSize(_t_);
            if (_i_ == 1)
            {
                _o_.ReadDynamic(Any, _t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            while (_t_ != 0)
            {
                _o_.SkipUnknownField(_t_);
                _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
        }

        protected override void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root)
        {
            _Any.InitRootInfo(root, this);
        }

        public override bool NegativeCheck()
        {
            return false;
        }
        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _Any.FollowerApply(vlog); break;
                }
            }
        }

    }
}
