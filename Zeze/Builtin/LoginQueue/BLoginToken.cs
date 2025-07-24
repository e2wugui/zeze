// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.LoginQueue
{
    public interface BLoginTokenReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BLoginToken Copy();

        public Zeze.Net.Binary Token { get; }
        public string LinkIp { get; }
        public int LinkPort { get; }
    }

    public sealed class BLoginToken : Zeze.Transaction.Bean, BLoginTokenReadOnly
    {
        Zeze.Net.Binary _Token;
        string _LinkIp;
        int _LinkPort;

        public Zeze.Net.Binary Token
        {
            get
            {
                if (!IsManaged)
                    return _Token;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Token;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Token)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _Token;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _Token = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Token() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public string LinkIp
        {
            get
            {
                if (!IsManaged)
                    return _LinkIp;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _LinkIp;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__LinkIp)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _LinkIp;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _LinkIp = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__LinkIp() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public int LinkPort
        {
            get
            {
                if (!IsManaged)
                    return _LinkPort;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _LinkPort;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__LinkPort)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _LinkPort;
            }
            set
            {
                if (!IsManaged)
                {
                    _LinkPort = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__LinkPort() { Belong = this, VariableId = 3, Value = value });
            }
        }

        public BLoginToken()
        {
            _Token = Zeze.Net.Binary.Empty;
            _LinkIp = "";
        }

        public BLoginToken(Zeze.Net.Binary _Token_, string _LinkIp_, int _LinkPort_)
        {
            _Token = _Token_;
            _LinkIp = _LinkIp_;
            _LinkPort = _LinkPort_;
        }

        public void Assign(BLoginToken other)
        {
            Token = other.Token;
            LinkIp = other.LinkIp;
            LinkPort = other.LinkPort;
        }

        public BLoginToken CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BLoginToken Copy()
        {
            var copy = new BLoginToken();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BLoginToken a, BLoginToken b)
        {
            BLoginToken save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = 4624437118588347002;
        public override long TypeId => TYPEID;

        sealed class Log__Token : Zeze.Transaction.Log<Zeze.Net.Binary>
        {
            public override void Commit() { ((BLoginToken)Belong)._Token = this.Value; }
        }

        sealed class Log__LinkIp : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BLoginToken)Belong)._LinkIp = this.Value; }
        }

        sealed class Log__LinkPort : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((BLoginToken)Belong)._LinkPort = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.LoginQueue.BLoginToken: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Token").Append('=').Append(Token).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LinkIp").Append('=').Append(LinkIp).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LinkPort").Append('=').Append(LinkPort).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                var _x_ = Token;
                if (_x_ != null && _x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                    _o_.WriteBinary(_x_);
                }
            }
            {
                string _x_ = LinkIp;
                if (_x_ != null && _x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                int _x_ = LinkPort;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
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
                Token = _o_.ReadBinary(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                LinkIp = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                LinkPort = _o_.ReadInt(_t_);
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
            if (LinkPort < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _Token = vlog.BinaryValue(); break;
                    case 2: _LinkIp = vlog.StringValue(); break;
                    case 3: _LinkPort = vlog.IntValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            Token = Zeze.Net.Binary.Empty;
            LinkIp = "";
            LinkPort = 0;
        }
    }
}
