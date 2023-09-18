// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Online
{
    [System.Serializable]
    public sealed class BAny : Zeze.Util.ConfBean
    {
        public Zeze.Util.ConfBean Any;

        public static long GetSpecialTypeIdFromBean_1(Zeze.Util.ConfBean bean)
        {
            return Zeze.Arch.Online.GetSpecialTypeIdFromBean(bean);
        }

        public static Zeze.Util.ConfBean CreateBeanFromSpecialTypeId_1(long typeId)
        {
            return Zeze.Arch.Online.CreateBeanFromSpecialTypeId(typeId);
        }

        public BAny()
        {
        }

        public const long TYPEID = 5253251427600819301;
        public override long TypeId => 5253251427600819301;

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
            Any?.BuildString(sb, level + 4);
            sb.Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                var _x_ = Any;
                if (_x_ != null)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.DYNAMIC);
                    _o_.WriteLong(GetSpecialTypeIdFromBean_1(_x_));
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
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.DYNAMIC)
                {
                    var _x_ = CreateBeanFromSpecialTypeId_1(_o_.ReadLong());
                    _x_.Decode(_o_);
                    Any = _x_;
                }
                else
                    _o_.SkipUnknownFieldOrThrow(_t_, "DynamicBean");
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            while (_t_ != 0)
            {
                _o_.SkipUnknownField(_t_);
                _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
        }


        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: Any.FollowerApply(vlog); break;
                }
            }
        }

        public override void ClearParameters()
        {
            Any.ClearParameters();
        }
    }
}
