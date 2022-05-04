// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Builtin.Online
{
    public interface BReliableNotifyConfirmReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public Zeze.Transaction.Bean CopyBean();

        public string ClientId { get; }
        public long ReliableNotifyConfirmCount { get; }
    }

    public sealed class BReliableNotifyConfirm : Zeze.Transaction.Bean, BReliableNotifyConfirmReadOnly
    {
        string _ClientId;
        long _ReliableNotifyConfirmCount;

        public string ClientId
        {
            get
            {
                if (!IsManaged)
                    return _ClientId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ClientId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ClientId)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _ClientId;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException();
                if (!IsManaged)
                {
                    _ClientId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ClientId(this, value));
            }
        }

        public long ReliableNotifyConfirmCount
        {
            get
            {
                if (!IsManaged)
                    return _ReliableNotifyConfirmCount;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ReliableNotifyConfirmCount;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ReliableNotifyConfirmCount)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _ReliableNotifyConfirmCount;
            }
            set
            {
                if (!IsManaged)
                {
                    _ReliableNotifyConfirmCount = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ReliableNotifyConfirmCount(this, value));
            }
        }

        public BReliableNotifyConfirm() : this(0)
        {
        }

        public BReliableNotifyConfirm(int _varId_) : base(_varId_)
        {
            _ClientId = "";
        }

        public void Assign(BReliableNotifyConfirm other)
        {
            ClientId = other.ClientId;
            ReliableNotifyConfirmCount = other.ReliableNotifyConfirmCount;
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

        public const long TYPEID = 7657736965823286884;
        public override long TypeId => TYPEID;

        sealed class Log__ClientId : Zeze.Transaction.Log<BReliableNotifyConfirm, string>
        {
            public Log__ClientId(BReliableNotifyConfirm self, string value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 1;
            public override void Commit() { this.BeanTyped._ClientId = this.Value; }
        }

        sealed class Log__ReliableNotifyConfirmCount : Zeze.Transaction.Log<BReliableNotifyConfirm, long>
        {
            public Log__ReliableNotifyConfirmCount(BReliableNotifyConfirm self, long value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 2;
            public override void Commit() { this.BeanTyped._ReliableNotifyConfirmCount = this.Value; }
        }

        public override string ToString()
        {
            System.Text.StringBuilder sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Online.BReliableNotifyConfirm: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ClientId").Append('=').Append(ClientId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ReliableNotifyConfirmCount").Append('=').Append(ReliableNotifyConfirmCount).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                string _x_ = ClientId;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                long _x_ = ReliableNotifyConfirmCount;
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
                ClientId = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                ReliableNotifyConfirmCount = _o_.ReadLong(_t_);
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
            if (ReliableNotifyConfirmCount < 0) return true;
            return false;
        }
    }
}
