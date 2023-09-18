// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zege.Message
{
    [System.Serializable]
    public sealed class BSetGroupMessageHasRead : Zeze.Util.ConfBean
    {
        public Zege.Message.BDepartmentKey GroupDepartment;
        public long MessageIdHashRead;

        public BSetGroupMessageHasRead()
        {
            GroupDepartment = new Zege.Message.BDepartmentKey();
        }

        public const long TYPEID = 6375239843770871467;
        public override long TypeId => 6375239843770871467;

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zege.Message.BSetGroupMessageHasRead: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("GroupDepartment").Append('=').Append(Environment.NewLine);
            GroupDepartment.BuildString(sb, level + 4);
            sb.Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("MessageIdHashRead").Append('=').Append(MessageIdHashRead).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                int _a_ = _o_.WriteIndex;
                int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
                int _b_ = _o_.WriteIndex;
                GroupDepartment.Encode(_o_);
                if (_b_ + 1 == _o_.WriteIndex)
                    _o_.WriteIndex = _a_;
                else
                    _i_ = _j_;
            }
            {
                long _x_ = MessageIdHashRead;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
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
                _o_.ReadBean(GroupDepartment, _t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                MessageIdHashRead = _o_.ReadLong(_t_);
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
                    case 1: GroupDepartment = ((Zeze.Transaction.Log<Zege.Message.BDepartmentKey>)vlog).Value; break;
                    case 2: MessageIdHashRead = ((Zeze.Transaction.Log<long>)vlog).Value; break;
                }
            }
        }

        public override void ClearParameters()
        {
            GroupDepartment = new Zege.Message.BDepartmentKey();
            MessageIdHashRead = 0;
        }
    }
}
