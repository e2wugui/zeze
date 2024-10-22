// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zege.Message
{
    [System.Serializable]
    public sealed class BMessage : Zeze.Util.ConfBean
    {
        public const int eTypeSystem = -1; // 系统消息，不加密，SecureKeyIndex is -1;
        public const int eTypeP2P = -2; // P2P协商消息，不加密，服务器要参与协商；BP2PMessage
        public const int eTypeText = 0; // 文本聊天消息，加密的，BTextMessage。
        public const int eTypeEmoji = 1; // 自定义表情消息，加密的，解密出来是系列化的BEmojiMessage。

        public int Type;
        public int SecureKeyIndex; // SecureKeyIndex 为 -1 表示不加密。
        public Zeze.Net.Binary SecureMessage; // eTypeSystem 时是未加密的。
        public string From; // 发送用户，服务器填写
        public string Group; // 群消息才填写，服务器填写，
        public long DepartmentId; // 群消息才填写，服务器填写。
        public long MessageId; // 服务器填写。简化读取消息时的数据结构定义。

        public BMessage()
        {
            SecureMessage = Zeze.Net.Binary.Empty;
            From = "";
            Group = "";
        }

        public const long TYPEID = -5480131348990622600;
        public override long TypeId => -5480131348990622600;

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zege.Message.BMessage: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Type").Append('=').Append(Type).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("SecureKeyIndex").Append('=').Append(SecureKeyIndex).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("SecureMessage").Append('=').Append(SecureMessage).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("From").Append('=').Append(From).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Group").Append('=').Append(Group).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("DepartmentId").Append('=').Append(DepartmentId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("MessageId").Append('=').Append(MessageId).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                int _x_ = Type;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                int _x_ = SecureKeyIndex;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                var _x_ = SecureMessage;
                if (_x_ != null && _x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                    _o_.WriteBinary(_x_);
                }
            }
            {
                string _x_ = From;
                if (_x_ != null && _x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                string _x_ = Group;
                if (_x_ != null && _x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                long _x_ = DepartmentId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                long _x_ = MessageId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
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
                Type = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                SecureKeyIndex = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                SecureMessage = _o_.ReadBinary(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                From = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 5)
            {
                Group = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 6)
            {
                DepartmentId = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 7)
            {
                MessageId = _o_.ReadLong(_t_);
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
                    case 1: Type = ((Zeze.Transaction.Log<int>)vlog).Value; break;
                    case 2: SecureKeyIndex = ((Zeze.Transaction.Log<int>)vlog).Value; break;
                    case 3: SecureMessage = ((Zeze.Transaction.Log<Zeze.Net.Binary>)vlog).Value; break;
                    case 4: From = ((Zeze.Transaction.Log<string>)vlog).Value; break;
                    case 5: Group = ((Zeze.Transaction.Log<string>)vlog).Value; break;
                    case 6: DepartmentId = ((Zeze.Transaction.Log<long>)vlog).Value; break;
                    case 7: MessageId = ((Zeze.Transaction.Log<long>)vlog).Value; break;
                }
            }
        }

        public override void ClearParameters()
        {
            Type = 0;
            SecureKeyIndex = 0;
            SecureMessage = Zeze.Net.Binary.Empty;
            From = "";
            Group = "";
            DepartmentId = 0;
            MessageId = 0;
        }
    }
}
