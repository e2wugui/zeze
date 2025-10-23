// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Game.Online
{
    public interface BDelayLogoutCustomReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BDelayLogoutCustom Copy();
        public void BuildString(System.Text.StringBuilder sb, int level);
        public long ObjectId { get; }
        public int VariableId { get; }
        public Zeze.Transaction.TableKey TableKey { get; }
        public bool IsManaged { get; }
        public int CapacityHintOfByteBuffer { get; }

        public long RoleId { get; }
        public long LoginVersion { get; }
    }

    public sealed class BDelayLogoutCustom : Zeze.Transaction.Bean, BDelayLogoutCustomReadOnly
    {
        long _RoleId;
        long _LoginVersion;

        public long RoleId
        {
            get
            {
                if (!IsManaged)
                    return _RoleId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _RoleId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__RoleId)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _RoleId;
            }
            set
            {
                if (!IsManaged)
                {
                    _RoleId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__RoleId() { Belong = this, VariableId = 1, Value = value });
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
                var log = (Log__LoginVersion)txn.GetLog(ObjectId + 2);
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
                txn.PutLog(new Log__LoginVersion() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public BDelayLogoutCustom()
        {
        }

        public BDelayLogoutCustom(long _RoleId_, long _LoginVersion_)
        {
            _RoleId = _RoleId_;
            _LoginVersion = _LoginVersion_;
        }

        public void Assign(BDelayLogoutCustom other)
        {
            RoleId = other.RoleId;
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

        public const long TYPEID = -2195913431542088885;
        public override long TypeId => TYPEID;

        sealed class Log__RoleId : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BDelayLogoutCustom)Belong)._RoleId = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Game.Online.BDelayLogoutCustom: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("RoleId").Append('=').Append(RoleId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LoginVersion").Append('=').Append(LoginVersion).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                long _x_ = RoleId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                long _x_ = LoginVersion;
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
                RoleId = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
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
            if (RoleId < 0) return true;
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
                    case 1: _RoleId = vlog.LongValue(); break;
                    case 2: _LoginVersion = vlog.LongValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            RoleId = 0;
            LoginVersion = 0;
        }
    }
}
