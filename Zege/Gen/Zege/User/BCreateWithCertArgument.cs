// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zege.User
{
    [System.Serializable]
    public sealed class BCreateWithCertArgument : Zeze.Util.ConfBean
    {
        public string Account; // 用来确认。客户端可简化为自动填充。
        public Zeze.Net.Binary Cert; // der format
        public Zeze.Net.Binary Signed;

        public BCreateWithCertArgument()
        {
            Account = "";
            Cert = Zeze.Net.Binary.Empty;
            Signed = Zeze.Net.Binary.Empty;
        }

        public const long TYPEID = -7039670413162928638;
        public override long TypeId => -7039670413162928638;

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zege.User.BCreateWithCertArgument: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Account").Append('=').Append(Account).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Cert").Append('=').Append(Cert).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Signed").Append('=').Append(Signed).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                string _x_ = Account;
                if (_x_ != null && _x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                var _x_ = Cert;
                if (_x_ != null && _x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                    _o_.WriteBinary(_x_);
                }
            }
            {
                var _x_ = Signed;
                if (_x_ != null && _x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                    _o_.WriteBinary(_x_);
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
                Account = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                Cert = _o_.ReadBinary(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                Signed = _o_.ReadBinary(_t_);
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
                    case 1: Account = ((Zeze.Transaction.Log<string>)vlog).Value; break;
                    case 2: Cert = ((Zeze.Transaction.Log<Zeze.Net.Binary>)vlog).Value; break;
                    case 3: Signed = ((Zeze.Transaction.Log<Zeze.Net.Binary>)vlog).Value; break;
                }
            }
        }

        public override void ClearParameters()
        {
            Account = "";
            Cert = Zeze.Net.Binary.Empty;
            Signed = Zeze.Net.Binary.Empty;
        }
    }
}
