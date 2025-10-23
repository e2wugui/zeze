// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// protocols
// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Game.Online
{
    public interface BLoginReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BLogin Copy();
        public void BuildString(System.Text.StringBuilder sb, int level);
        public long ObjectId { get; }
        public int VariableId { get; }
        public Zeze.Transaction.TableKey TableKey { get; }
        public bool IsManaged { get; }
        public int CapacityHintOfByteBuffer { get; }

        public long RoleId { get; }
    }

    public sealed class BLogin : Zeze.Transaction.Bean, BLoginReadOnly
    {
        long _RoleId;

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

        public BLogin()
        {
        }

        public BLogin(long _RoleId_)
        {
            _RoleId = _RoleId_;
        }

        public void Assign(BLogin other)
        {
            RoleId = other.RoleId;
        }

        public BLogin CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BLogin Copy()
        {
            var copy = new BLogin();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BLogin a, BLogin b)
        {
            BLogin save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = 4454573042979027680;
        public override long TypeId => TYPEID;

        sealed class Log__RoleId : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BLogin)Belong)._RoleId = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Game.Online.BLogin: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("RoleId").Append('=').Append(RoleId).Append(Environment.NewLine);
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
            while (_t_ != 0)
            {
                _o_.SkipUnknownField(_t_);
                _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
        }

        public override bool NegativeCheck()
        {
            if (RoleId < 0) return true;
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
                }
            }
        }

        public override void ClearParameters()
        {
            RoleId = 0;
        }
    }
}
