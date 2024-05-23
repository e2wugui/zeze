// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Online
{
    public interface BDelayLogoutCustomReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BDelayLogoutCustom Copy();

        public string Account { get; }
        public string ClientId { get; }
        public long LoginVersion { get; }
    }

    public sealed class BDelayLogoutCustom : Zeze.Transaction.Bean, BDelayLogoutCustomReadOnly
    {
        string _Account;
        string _ClientId;
        long _LoginVersion;

        public string Account
        {
            get
            {
                if (!IsManaged)
                    return _Account;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Account;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Account)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _Account;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _Account = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Account() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public string ClientId
        {
            get
            {
                if (!IsManaged)
                    return _ClientId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ClientId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ClientId)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _ClientId;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _ClientId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ClientId() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public long LoginVersion
        {
            get
            {
                if (!IsManaged)
                    return _LoginVersion;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _LoginVersion;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__LoginVersion)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _LoginVersion;
            }
            set
            {
                if (!IsManaged)
                {
                    _LoginVersion = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__LoginVersion() { Belong = this, VariableId = 3, Value = value });
            }
        }

        public BDelayLogoutCustom()
        {
            _Account = "";
            _ClientId = "";
        }

        public BDelayLogoutCustom(string _Account_, string _ClientId_, long _LoginVersion_)
        {
            _Account = _Account_;
            _ClientId = _ClientId_;
            _LoginVersion = _LoginVersion_;
        }

        public void Assign(BDelayLogoutCustom other)
        {
            Account = other.Account;
            ClientId = other.ClientId;
            LoginVersion = other.LoginVersion;
        }

        public BDelayLogoutCustom CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BDelayLogoutCustom Copy()
        {
            var copy = new BDelayLogoutCustom();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BDelayLogoutCustom a, BDelayLogoutCustom b)
        {
            BDelayLogoutCustom save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = 8209690781023670883;
        public override long TypeId => TYPEID;

        sealed class Log__Account : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BDelayLogoutCustom)Belong)._Account = this.Value; }
        }

        sealed class Log__ClientId : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BDelayLogoutCustom)Belong)._ClientId = this.Value; }
        }

        sealed class Log__LoginVersion : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BDelayLogoutCustom)Belong)._LoginVersion = this.Value; }
        }

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Online.BDelayLogoutCustom: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Account").Append('=').Append(Account).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ClientId").Append('=').Append(ClientId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LoginVersion").Append('=').Append(LoginVersion).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                string _x_ = Account;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                string _x_ = ClientId;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                long _x_ = LoginVersion;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
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
                Account = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                ClientId = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                LoginVersion = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            while (_t_ != 0)
            {
                _o_.SkipUnknownField(_t_);
                _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
        }

        public override bool NegativeCheck()
        {
            if (LoginVersion < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _Account = vlog.StringValue(); break;
                    case 2: _ClientId = vlog.StringValue(); break;
                    case 3: _LoginVersion = vlog.LongValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            Account = "";
            ClientId = "";
            LoginVersion = 0;
        }
    }
}
