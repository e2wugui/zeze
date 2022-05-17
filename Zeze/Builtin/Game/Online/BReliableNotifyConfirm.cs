// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Builtin.Game.Online
{
    public interface BReliableNotifyConfirmReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public Zeze.Transaction.Bean CopyBean();

        public long ReliableNotifyConfirmIndex { get; }
    }

    public sealed class BReliableNotifyConfirm : Zeze.Transaction.Bean, BReliableNotifyConfirmReadOnly
    {
        long _ReliableNotifyConfirmIndex;

        public long ReliableNotifyConfirmIndex
        {
            get
            {
                if (!IsManaged)
                    return _ReliableNotifyConfirmIndex;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ReliableNotifyConfirmIndex;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ReliableNotifyConfirmIndex)txn.GetLog(ObjectId + 1);
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
                txn.PutLog(new Log__ReliableNotifyConfirmIndex() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public BReliableNotifyConfirm() : this(0)
        {
        }

        public BReliableNotifyConfirm(int _varId_) : base(_varId_)
        {
        }

        public void Assign(BReliableNotifyConfirm other)
        {
            ReliableNotifyConfirmIndex = other.ReliableNotifyConfirmIndex;
        }

        public BReliableNotifyConfirm CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public BReliableNotifyConfirm Copy()
        {
            var copy = new BReliableNotifyConfirm();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BReliableNotifyConfirm a, BReliableNotifyConfirm b)
        {
            BReliableNotifyConfirm save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public override Zeze.Transaction.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = -6588057877320371892;
        public override long TypeId => TYPEID;

        sealed class Log__ReliableNotifyConfirmIndex : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BReliableNotifyConfirm)Belong)._ReliableNotifyConfirmIndex = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Game.Online.BReliableNotifyConfirm: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ReliableNotifyConfirmIndex").Append('=').Append(ReliableNotifyConfirmIndex).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                long _x_ = ReliableNotifyConfirmIndex;
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
                ReliableNotifyConfirmIndex = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            while (_t_ != 0)
            {
                _o_.SkipUnknownField(_t_);
                _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
        }

        protected override void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root)
        {
        }

        public override bool NegativeCheck()
        {
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
                    case 1: _ReliableNotifyConfirmIndex = ((Zeze.Transaction.Log<long>)vlog).Value; break;
                }
            }
        }

    }
}
