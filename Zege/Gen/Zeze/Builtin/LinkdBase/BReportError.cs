// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// linkd to client
// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.LinkdBase
{
    [System.Serializable]
    public sealed class BReportError : Zeze.Util.ConfBean
    {
        public const int FromLink = 0; // code字段见下面Code开头的枚举
        public const int FromProvider = 1; // code字段见BKick里定义的Error开头的枚举
        public const int FromDynamicModule = 2; // code字段是moduleId
        public const int CodeMuteKick = 0; // 只断客户端连接，不发送消息给客户端，用于重连时确保旧的连接快速断开
        public const int CodeNotAuthed = 1;
        public const int CodeNoProvider = 2;
        public const int CodeProviderBusy = 3;
        public const int CodeProviderBroken = 4; // link跟provider断开,跟此provider静态绑定的客户端需要收到此协议执行重新登录流程

        public int From; // FromLink, FromProvider, or FromDynamicModule
        public int Code;
        public string Desc;

        public BReportError()
        {
            Desc = "";
        }

        public const long TYPEID = -947669033141460287;
        public override long TypeId => -947669033141460287;

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.LinkdBase.BReportError: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("From").Append('=').Append(From).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Code").Append('=').Append(Code).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Desc").Append('=').Append(Desc).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                int _x_ = From;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                int _x_ = Code;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                string _x_ = Desc;
                if (_x_ != null && _x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
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
                From = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                Code = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                Desc = _o_.ReadString(_t_);
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
                    case 1: From = ((Zeze.Transaction.Log<int>)vlog).Value; break;
                    case 2: Code = ((Zeze.Transaction.Log<int>)vlog).Value; break;
                    case 3: Desc = ((Zeze.Transaction.Log<string>)vlog).Value; break;
                }
            }
        }

        public override void ClearParameters()
        {
            From = 0;
            Code = 0;
            Desc = "";
        }
    }
}
