// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Builtin.ProviderDirect
{
    public interface BTransmitContextReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public Zeze.Transaction.Bean CopyBean();

        public long LinkSid { get; }
        public int ProviderId { get; }
        public long ProviderSessionId { get; }
    }

    public sealed class BTransmitContext : Zeze.Transaction.Bean, BTransmitContextReadOnly
    {
        long _LinkSid;
        int _ProviderId;
        long _ProviderSessionId;

        public long LinkSid
        {
            get
            {
                if (!IsManaged)
                    return _LinkSid;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _LinkSid;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__LinkSid)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _LinkSid;
            }
            set
            {
                if (!IsManaged)
                {
                    _LinkSid = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__LinkSid(this, value));
            }
        }

        public int ProviderId
        {
            get
            {
                if (!IsManaged)
                    return _ProviderId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ProviderId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ProviderId)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _ProviderId;
            }
            set
            {
                if (!IsManaged)
                {
                    _ProviderId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ProviderId(this, value));
            }
        }

        public long ProviderSessionId
        {
            get
            {
                if (!IsManaged)
                    return _ProviderSessionId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ProviderSessionId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ProviderSessionId)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _ProviderSessionId;
            }
            set
            {
                if (!IsManaged)
                {
                    _ProviderSessionId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ProviderSessionId(this, value));
            }
        }

        public BTransmitContext() : this(0)
        {
        }

        public BTransmitContext(int _varId_) : base(_varId_)
        {
        }

        public void Assign(BTransmitContext other)
        {
            LinkSid = other.LinkSid;
            ProviderId = other.ProviderId;
            ProviderSessionId = other.ProviderSessionId;
        }

        public BTransmitContext CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public BTransmitContext Copy()
        {
            var copy = new BTransmitContext();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BTransmitContext a, BTransmitContext b)
        {
            BTransmitContext save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public override Zeze.Transaction.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = 606535654553096889;
        public override long TypeId => TYPEID;

        sealed class Log__LinkSid : Zeze.Transaction.Log<BTransmitContext, long>
        {
            public Log__LinkSid(BTransmitContext self, long value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 1;
            public override void Commit() { this.BeanTyped._LinkSid = this.Value; }
        }

        sealed class Log__ProviderId : Zeze.Transaction.Log<BTransmitContext, int>
        {
            public Log__ProviderId(BTransmitContext self, int value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 2;
            public override void Commit() { this.BeanTyped._ProviderId = this.Value; }
        }

        sealed class Log__ProviderSessionId : Zeze.Transaction.Log<BTransmitContext, long>
        {
            public Log__ProviderSessionId(BTransmitContext self, long value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 3;
            public override void Commit() { this.BeanTyped._ProviderSessionId = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.ProviderDirect.BTransmitContext: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LinkSid").Append('=').Append(LinkSid).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ProviderId").Append('=').Append(ProviderId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ProviderSessionId").Append('=').Append(ProviderSessionId).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                long _x_ = LinkSid;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                int _x_ = ProviderId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                long _x_ = ProviderSessionId;
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
                LinkSid = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                ProviderId = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                ProviderSessionId = _o_.ReadLong(_t_);
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
            if (LinkSid < 0) return true;
            if (ProviderId < 0) return true;
            if (ProviderSessionId < 0) return true;
            return false;
        }
    }
}
