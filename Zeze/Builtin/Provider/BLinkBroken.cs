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
        public System.Collections.Generic.IReadOnlyList<long>States { get; }
        public Zeze.Net.Binary Statex { get; }
    }

    public sealed class BLinkBroken : Zeze.Transaction.Bean, BLinkBrokenReadOnly
    {
        public const int REASON_PEERCLOSE = 0;

        string _account;
        long _linkSid;
        int _reason;
        readonly Zeze.Transaction.Collections.PList1<long> _states; // SetUserState
        Zeze.Net.Binary _statex; // SetUserState

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

        public Zeze.Transaction.Collections.PList1<long> States => _states;
        System.Collections.Generic.IReadOnlyList<long> Zeze.Builtin.Provider.BLinkBrokenReadOnly.States => _states;

        public Zeze.Net.Binary Statex
        {
            get
            {
                if (!IsManaged)
                    return _statex;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _statex;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__statex)txn.GetLog(ObjectId + 6);
                return log != null ? log.Value : _statex;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException();
                if (!IsManaged)
                {
                    _statex = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__statex(this, value));
            }
        }

        public BLinkBroken() : this(0)
        {
        }

        public BLinkBroken(int _varId_) : base(_varId_)
        {
            _account = "";
            _states = new Zeze.Transaction.Collections.PList1<long>(ObjectId + 5, _v => new Log__states(this, _v));
            _statex = Zeze.Net.Binary.Empty;
        }

        public void Assign(BLinkBroken other)
        {
            Account = other.Account;
            LinkSid = other.LinkSid;
            Reason = other.Reason;
            States.Clear();
            foreach (var e in other.States)
                States.Add(e);
            Statex = other.Statex;
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
            public override long LogKey => this.Bean.ObjectId + 1;
            public override void Commit() { this.BeanTyped._account = this.Value; }
        }

        sealed class Log__linkSid : Zeze.Transaction.Log<BLinkBroken, long>
        {
            public Log__linkSid(BLinkBroken self, long value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 2;
            public override void Commit() { this.BeanTyped._linkSid = this.Value; }
        }

        sealed class Log__reason : Zeze.Transaction.Log<BLinkBroken, int>
        {
            public Log__reason(BLinkBroken self, int value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 3;
            public override void Commit() { this.BeanTyped._reason = this.Value; }
        }

        sealed class Log__states : Zeze.Transaction.Collections.PList1<long>.LogV
        {
            public Log__states(BLinkBroken host, System.Collections.Immutable.ImmutableList<long> value) : base(host, value) {}
            public override long LogKey => Bean.ObjectId + 5;
            public BLinkBroken BeanTyped => (BLinkBroken)Bean;
            public override void Commit() { Commit(BeanTyped._states); }
        }

        sealed class Log__statex : Zeze.Transaction.Log<BLinkBroken, Zeze.Net.Binary>
        {
            public Log__statex(BLinkBroken self, Zeze.Net.Binary value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 6;
            public override void Commit() { this.BeanTyped._statex = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("States").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var Item in States)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Item").Append('=').Append(Item).Append(',').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Statex").Append('=').Append(Statex).Append(Environment.NewLine);
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
                var _x_ = States;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.LIST);
                    _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                    foreach (var _v_ in _x_)
                        _o_.WriteLong(_v_);
                }
            }
            {
                var _x_ = Statex;
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
                var _x_ = States;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST)
                {
                    for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                        _x_.Add(_o_.ReadLong(_t_));
                }
                else
                    _o_.SkipUnknownField(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 6)
            {
                Statex = _o_.ReadBinary(_t_);
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
            _states.InitRootInfo(root, this);
        }

        public override bool NegativeCheck()
        {
            if (LinkSid < 0) return true;
            if (Reason < 0) return true;
            foreach (var _v_ in States)
            {
                if (_v_ < 0) return true;
            }
            return false;
        }
    }
}
