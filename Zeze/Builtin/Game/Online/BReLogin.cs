// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Game.Online
{
    public interface BReLoginReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BReLogin Copy();

        public long RoleId { get; }
        public long ReliableNotifyConfirmIndex { get; }
    }

    public sealed class BReLogin : Zeze.Transaction.Bean, BReLoginReadOnly
    {
        long _RoleId;
        long _ReliableNotifyConfirmIndex;

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

        public long ReliableNotifyConfirmIndex
        {
            get
            {
                if (!IsManaged)
                    return _ReliableNotifyConfirmIndex;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ReliableNotifyConfirmIndex;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ReliableNotifyConfirmIndex)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _ReliableNotifyConfirmIndex;
            }
            set
            {
                if (!IsManaged)
                {
                    _ReliableNotifyConfirmIndex = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ReliableNotifyConfirmIndex() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public BReLogin()
        {
        }

        public BReLogin(long _RoleId_, long _ReliableNotifyConfirmIndex_)
        {
            _RoleId = _RoleId_;
            _ReliableNotifyConfirmIndex = _ReliableNotifyConfirmIndex_;
        }

        public void Assign(BReLogin other)
        {
            RoleId = other.RoleId;
            ReliableNotifyConfirmIndex = other.ReliableNotifyConfirmIndex;
        }

        public BReLogin CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BReLogin Copy()
        {
            var copy = new BReLogin();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BReLogin a, BReLogin b)
        {
            BReLogin save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = 8551355014943125267;
        public override long TypeId => TYPEID;

        sealed class Log__RoleId : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BReLogin)Belong)._RoleId = this.Value; }
        }

        sealed class Log__ReliableNotifyConfirmIndex : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BReLogin)Belong)._ReliableNotifyConfirmIndex = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Game.Online.BReLogin: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("RoleId").Append('=').Append(RoleId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ReliableNotifyConfirmIndex").Append('=').Append(ReliableNotifyConfirmIndex).Append(Environment.NewLine);
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
                long _x_ = ReliableNotifyConfirmIndex;
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
                ReliableNotifyConfirmIndex = _o_.ReadLong(_t_);
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
            if (ReliableNotifyConfirmIndex < 0) return true;
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
                    case 2: _ReliableNotifyConfirmIndex = vlog.LongValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            RoleId = 0;
            ReliableNotifyConfirmIndex = 0;
        }
    }
}
