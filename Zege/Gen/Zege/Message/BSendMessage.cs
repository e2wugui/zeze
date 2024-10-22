// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zege.Message
{
    [System.Serializable]
    public sealed class BSendMessage : Zeze.Util.ConfBean
    {
        public string Friend;
        public Zege.Message.BMessage Message;

        public BSendMessage()
        {
            Friend = "";
            Message = new Zege.Message.BMessage();
        }

        public const long TYPEID = 8089860965927886832;
        public override long TypeId => 8089860965927886832;

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zege.Message.BSendMessage: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Friend").Append('=').Append(Friend).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Message").Append('=').Append(Environment.NewLine);
            Message.BuildString(sb, level + 4);
            sb.Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                string _x_ = Friend;
                if (_x_ != null && _x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                var _x_ = Message;
                if (_x_ != null)
                {
                    int _a_ = _o_.WriteIndex;
                    int _j_ = _o_.WriteTag(_i_, 2, ByteBuffer.BEAN);
                    int _b_ = _o_.WriteIndex;
                    _x_.Encode(_o_);
                    if (_o_.WriteIndex <= _b_ + 1)
                        _o_.WriteIndex = _a_;
                    else
                        _i_ = _j_;
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
                Friend = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                _o_.ReadBean(Message, _t_);
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
                    case 1: Friend = ((Zeze.Transaction.Log<string>)vlog).Value; break;
                    case 2: Zeze.Transaction.Collections.CollApply.ApplyOne<Zege.Message.BMessage>(ref Message, vlog); break;
                }
            }
        }

        public override void ClearParameters()
        {
            Friend = "";
            Message.ClearParameters();
        }
    }
}
