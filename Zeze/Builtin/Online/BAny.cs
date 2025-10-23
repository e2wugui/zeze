// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Online
{
    public interface BAnyReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BAny Copy();
        public void BuildString(System.Text.StringBuilder sb, int level);
        public long ObjectId { get; }
        public int VariableId { get; }
        public Zeze.Transaction.TableKey TableKey { get; }
        public bool IsManaged { get; }
        public int CapacityHintOfByteBuffer { get; }

        public Zeze.Transaction.DynamicBeanReadOnly Any { get; }

    }

    public sealed class BAny : Zeze.Transaction.Bean, BAnyReadOnly
    {
        readonly Zeze.Transaction.DynamicBean _Any;
        public static Zeze.Transaction.DynamicBean NewDynamicBeanAny()
        {
            return new Zeze.Transaction.DynamicBean(1, Zeze.Arch.Online.GetSpecialTypeIdFromBean, Zeze.Arch.Online.CreateBeanFromSpecialTypeId);
        }

        public static long GetSpecialTypeIdFromBean_1(Zeze.Transaction.Bean bean)
        {
            return Zeze.Arch.Online.GetSpecialTypeIdFromBean(bean);
        }

        public static Zeze.Transaction.Bean CreateBeanFromSpecialTypeId_1(long typeId)
        {
            return Zeze.Arch.Online.CreateBeanFromSpecialTypeId(typeId);
        }


        public string _zeze_map_key_string_ { get; set; }

        public Zeze.Transaction.DynamicBean Any => _Any;
        Zeze.Transaction.DynamicBeanReadOnly Zeze.Builtin.Online.BAnyReadOnly.Any => Any;

        public BAny()
        {
            _Any = new Zeze.Transaction.DynamicBean(1, Zeze.Arch.Online.GetSpecialTypeIdFromBean, Zeze.Arch.Online.CreateBeanFromSpecialTypeId);
        }

        public void Assign(BAny other)
        {
            Any.Assign(other.Any);
        }

        public BAny CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BAny Copy()
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

        public const long TYPEID = 5253251427600819301;
        public override long TypeId => TYPEID;


        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Online.BAny: {").Append(Environment.NewLine);
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

        protected override void InitChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root)
        {
            _Any.InitRootInfoWithRedo(root, this);
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

        public override void ClearParameters()
        {
            Any.ClearParameters();
        }
    }
}
