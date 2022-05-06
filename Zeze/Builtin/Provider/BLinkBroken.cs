// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Builtin.Provider
{
    public interface BLinkBrokenReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public Zeze.Transaction.Bean CopyBean();

        public string Account { get; }
        public long LinkSid { get; }
        public int Reason { get; }
        public string Context { get; }
        public Zeze.Net.Binary Contextx { get; }
    }

    public sealed class BLinkBroken : Zeze.Transaction.Bean, BLinkBrokenReadOnly
    {
        public const int REASON_PEERCLOSE = 0;

        string _account;
        long _linkSid;
        int _reason;
        string _context; // SetUserState
        Zeze.Net.Binary _contextx; // SetUserState

        public string Account
        {
            get
            {
                if (!IsManaged)
                    return _account;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _account;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__account)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _account;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException();
                if (!IsManaged)
                {
                    _account = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__account(this, value));
            }
        }

        public long LinkSid
        {
            get
            {
                if (!IsManaged)
                    return _linkSid;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _linkSid;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__linkSid)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _linkSid;
            }
            set
            {
                if (!IsManaged)
                {
                    _linkSid = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__linkSid(this, value));
            }
        }

        public int Reason
        {
            get
            {
                if (!IsManaged)
                    return _reason;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _reason;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__reason)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _reason;
            }
            set
            {
                if (!IsManaged)
                {
                    _reason = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__reason(this, value));
            }
        }

        public string Context
        {
            get
            {
                if (!IsManaged)
                    return _context;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _context;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__context)txn.GetLog(ObjectId + 5);
                return log != null ? log.Value : _context;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException();
                if (!IsManaged)
                {
                    _context = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__context(this, value));
            }
        }

        public Zeze.Net.Binary Contextx
        {
            get
            {
                if (!IsManaged)
                    return _contextx;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _contextx;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__contextx)txn.GetLog(ObjectId + 6);
                return log != null ? log.Value : _contextx;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException();
                if (!IsManaged)
                {
                    _contextx = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__contextx(this, value));
            }
        }

        public BLinkBroken() : this(0)
        {
        }

        public BLinkBroken(int _varId_) : base(_varId_)
        {
            _account = "";
            _context = "";
            _contextx = Zeze.Net.Binary.Empty;
        }

        public void Assign(BLinkBroken other)
        {
            Account = other.Account;
            LinkSid = other.LinkSid;
            Reason = other.Reason;
            Context = other.Context;
            Contextx = other.Contextx;
        }

        public BLinkBroken CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public BLinkBroken Copy()
        {
            var copy = new BLinkBroken();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BLinkBroken a, BLinkBroken b)
        {
            BLinkBroken save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public override Zeze.Transaction.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = 1424702393060691138;
        public override long TypeId => TYPEID;

        sealed class Log__account : Zeze.Transaction.Log<BLinkBroken, string>
        {
            public Log__account(BLinkBroken self, string value) : base(self, value) {}
            public override long LogKey => this.Belong.ObjectId + 1;
            public override void Commit() { this.BeanTyped._account = this.Value; }
        }

        sealed class Log__linkSid : Zeze.Transaction.Log<BLinkBroken, long>
        {
            public Log__linkSid(BLinkBroken self, long value) : base(self, value) {}
            public override long LogKey => this.Belong.ObjectId + 2;
            public override void Commit() { this.BeanTyped._linkSid = this.Value; }
        }

        sealed class Log__reason : Zeze.Transaction.Log<BLinkBroken, int>
        {
            public Log__reason(BLinkBroken self, int value) : base(self, value) {}
            public override long LogKey => this.Belong.ObjectId + 3;
            public override void Commit() { this.BeanTyped._reason = this.Value; }
        }

        sealed class Log__context : Zeze.Transaction.Log<BLinkBroken, string>
        {
            public Log__context(BLinkBroken self, string value) : base(self, value) {}
            public override long LogKey => this.Belong.ObjectId + 5;
            public override void Commit() { this.BeanTyped._context = this.Value; }
        }

        sealed class Log__contextx : Zeze.Transaction.Log<BLinkBroken, Zeze.Net.Binary>
        {
            public Log__contextx(BLinkBroken self, Zeze.Net.Binary value) : base(self, value) {}
            public override long LogKey => this.Belong.ObjectId + 6;
            public override void Commit() { this.BeanTyped._contextx = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Provider.BLinkBroken: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Account").Append('=').Append(Account).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LinkSid").Append('=').Append(LinkSid).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Reason").Append('=').Append(Reason).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Context").Append('=').Append(Context).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Contextx").Append('=').Append(Contextx).Append(Environment.NewLine);
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
                long _x_ = LinkSid;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                int _x_ = Reason;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                string _x_ = Context;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                var _x_ = Contextx;
                if (_x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
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
                LinkSid = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                Reason = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            while (_t_ != 0 && _i_ < 5)
            {
                _o_.SkipUnknownField(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 5)
            {
                Context = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 6)
            {
                Contextx = _o_.ReadBinary(_t_);
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
            if (Reason < 0) return true;
            return false;
        }
    }
}
