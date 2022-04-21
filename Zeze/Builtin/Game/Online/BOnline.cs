// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Builtin.Game.Online
{
    public interface BOnlineReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public Zeze.Transaction.Bean CopyBean();

        public string LinkName { get; }
        public long LinkSid { get; }
        public int State { get; }
        public System.Collections.Generic.IReadOnlySet<string> ReliableNotifyMark { get; }
        public System.Collections.Generic.IReadOnlyList<Zeze.Net.Binary>ReliableNotifyQueue { get; }
        public long ReliableNotifyConfirmCount { get; }
        public long ReliableNotifyTotalCount { get; }
        public int ProviderId { get; }
        public long ProviderSessionId { get; }
    }

    public sealed class BOnline : Zeze.Transaction.Bean, BOnlineReadOnly
    {
        public const int StateOffline = 0;
        public const int StateOnline = 2;
        public const int StateNetBroken = 3; // 客户端连接断开时，一定时间内可以重连。超时会删除 Online-Record。

        string _LinkName;
        long _LinkSid;
        int _State;
        readonly Zeze.Transaction.Collections.PSet1<string> _ReliableNotifyMark;
        readonly Zeze.Transaction.Collections.PList1<Zeze.Net.Binary> _ReliableNotifyQueue; // full encoded protocol list
        long _ReliableNotifyConfirmCount;
        long _ReliableNotifyTotalCount;
        int _ProviderId; // Config.AutoKeyLocalId
        long _ProviderSessionId; // 登录所在Linkd与当前Provider的连接在Linkd方的SessionId

        public string LinkName
        {
            get
            {
                if (!IsManaged)
                    return _LinkName;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _LinkName;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__LinkName)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _LinkName;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException();
                if (!IsManaged)
                {
                    _LinkName = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__LinkName(this, value));
            }
        }

        public long LinkSid
        {
            get
            {
                if (!IsManaged)
                    return _LinkSid;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _LinkSid;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__LinkSid)txn.GetLog(ObjectId + 2);
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

        public int State
        {
            get
            {
                if (!IsManaged)
                    return _State;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _State;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__State)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _State;
            }
            set
            {
                if (!IsManaged)
                {
                    _State = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__State(this, value));
            }
        }

        public Zeze.Transaction.Collections.PSet1<string> ReliableNotifyMark => _ReliableNotifyMark;
        System.Collections.Generic.IReadOnlySet<string> Zeze.Builtin.Game.Online.BOnlineReadOnly.ReliableNotifyMark => _ReliableNotifyMark;

        public Zeze.Transaction.Collections.PList1<Zeze.Net.Binary> ReliableNotifyQueue => _ReliableNotifyQueue;
        System.Collections.Generic.IReadOnlyList<Zeze.Net.Binary> Zeze.Builtin.Game.Online.BOnlineReadOnly.ReliableNotifyQueue => _ReliableNotifyQueue;

        public long ReliableNotifyConfirmCount
        {
            get
            {
                if (!IsManaged)
                    return _ReliableNotifyConfirmCount;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ReliableNotifyConfirmCount;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ReliableNotifyConfirmCount)txn.GetLog(ObjectId + 6);
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

        public long ReliableNotifyTotalCount
        {
            get
            {
                if (!IsManaged)
                    return _ReliableNotifyTotalCount;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ReliableNotifyTotalCount;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ReliableNotifyTotalCount)txn.GetLog(ObjectId + 7);
                return log != null ? log.Value : _ReliableNotifyTotalCount;
            }
            set
            {
                if (!IsManaged)
                {
                    _ReliableNotifyTotalCount = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ReliableNotifyTotalCount(this, value));
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
                var log = (Log__ProviderId)txn.GetLog(ObjectId + 8);
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
                var log = (Log__ProviderSessionId)txn.GetLog(ObjectId + 9);
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

        public BOnline() : this(0)
        {
        }

        public BOnline(int _varId_) : base(_varId_)
        {
            _LinkName = "";
            _State = StateOffline;
            _ReliableNotifyMark = new Zeze.Transaction.Collections.PSet1<string>(ObjectId + 4, _v => new Log__ReliableNotifyMark(this, _v));
            _ReliableNotifyQueue = new Zeze.Transaction.Collections.PList1<Zeze.Net.Binary>(ObjectId + 5, _v => new Log__ReliableNotifyQueue(this, _v));
        }

        public void Assign(BOnline other)
        {
            LinkName = other.LinkName;
            LinkSid = other.LinkSid;
            State = other.State;
            ReliableNotifyMark.Clear();
            foreach (var e in other.ReliableNotifyMark)
                ReliableNotifyMark.Add(e);
            ReliableNotifyQueue.Clear();
            foreach (var e in other.ReliableNotifyQueue)
                ReliableNotifyQueue.Add(e);
            ReliableNotifyConfirmCount = other.ReliableNotifyConfirmCount;
            ReliableNotifyTotalCount = other.ReliableNotifyTotalCount;
            ProviderId = other.ProviderId;
            ProviderSessionId = other.ProviderSessionId;
        }

        public BOnline CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public BOnline Copy()
        {
            var copy = new BOnline();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BOnline a, BOnline b)
        {
            BOnline save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public override Zeze.Transaction.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = -6079880688513613020;
        public override long TypeId => TYPEID;

        sealed class Log__LinkName : Zeze.Transaction.Log<BOnline, string>
        {
            public Log__LinkName(BOnline self, string value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 1;
            public override void Commit() { this.BeanTyped._LinkName = this.Value; }
        }

        sealed class Log__LinkSid : Zeze.Transaction.Log<BOnline, long>
        {
            public Log__LinkSid(BOnline self, long value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 2;
            public override void Commit() { this.BeanTyped._LinkSid = this.Value; }
        }

        sealed class Log__State : Zeze.Transaction.Log<BOnline, int>
        {
            public Log__State(BOnline self, int value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 3;
            public override void Commit() { this.BeanTyped._State = this.Value; }
        }

        sealed class Log__ReliableNotifyMark : Zeze.Transaction.Collections.PSet1<string>.LogV
        {
            public Log__ReliableNotifyMark(BOnline host, System.Collections.Immutable.ImmutableHashSet<string> value) : base(host, value) {}
            public override long LogKey => Bean.ObjectId + 4;
            public BOnline BeanTyped => (BOnline)Bean;
            public override void Commit() { Commit(BeanTyped._ReliableNotifyMark); }
        }

        sealed class Log__ReliableNotifyQueue : Zeze.Transaction.Collections.PList1<Zeze.Net.Binary>.LogV
        {
            public Log__ReliableNotifyQueue(BOnline host, System.Collections.Immutable.ImmutableList<Zeze.Net.Binary> value) : base(host, value) {}
            public override long LogKey => Bean.ObjectId + 5;
            public BOnline BeanTyped => (BOnline)Bean;
            public override void Commit() { Commit(BeanTyped._ReliableNotifyQueue); }
        }

        sealed class Log__ReliableNotifyConfirmCount : Zeze.Transaction.Log<BOnline, long>
        {
            public Log__ReliableNotifyConfirmCount(BOnline self, long value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 6;
            public override void Commit() { this.BeanTyped._ReliableNotifyConfirmCount = this.Value; }
        }

        sealed class Log__ReliableNotifyTotalCount : Zeze.Transaction.Log<BOnline, long>
        {
            public Log__ReliableNotifyTotalCount(BOnline self, long value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 7;
            public override void Commit() { this.BeanTyped._ReliableNotifyTotalCount = this.Value; }
        }

        sealed class Log__ProviderId : Zeze.Transaction.Log<BOnline, int>
        {
            public Log__ProviderId(BOnline self, int value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 8;
            public override void Commit() { this.BeanTyped._ProviderId = this.Value; }
        }

        sealed class Log__ProviderSessionId : Zeze.Transaction.Log<BOnline, long>
        {
            public Log__ProviderSessionId(BOnline self, long value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 9;
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Game.Online.BOnline: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LinkName").Append('=').Append(LinkName).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LinkSid").Append('=').Append(LinkSid).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("State").Append('=').Append(State).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ReliableNotifyMark").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var Item in ReliableNotifyMark)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Item").Append('=').Append(Item).Append(',').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ReliableNotifyQueue").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var Item in ReliableNotifyQueue)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Item").Append('=').Append(Item).Append(',').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ReliableNotifyConfirmCount").Append('=').Append(ReliableNotifyConfirmCount).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ReliableNotifyTotalCount").Append('=').Append(ReliableNotifyTotalCount).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ProviderId").Append('=').Append(ProviderId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ProviderSessionId").Append('=').Append(ProviderSessionId).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                string _x_ = LinkName;
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
                int _x_ = State;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                var _x_ = ReliableNotifyMark;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.LIST);
                    _o_.WriteListType(_n_, ByteBuffer.BYTES);
                    foreach (var _v_ in _x_)
                        _o_.WriteString(_v_);
                }
            }
            {
                var _x_ = ReliableNotifyQueue;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.LIST);
                    _o_.WriteListType(_n_, ByteBuffer.BYTES);
                    foreach (var _v_ in _x_)
                        _o_.WriteBinary(_v_);
                }
            }
            {
                long _x_ = ReliableNotifyConfirmCount;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                long _x_ = ReliableNotifyTotalCount;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                int _x_ = ProviderId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                long _x_ = ProviderSessionId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 9, ByteBuffer.INTEGER);
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
                LinkName = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                LinkSid = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                State = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                var _x_ = ReliableNotifyMark;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST)
                {
                    for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                        _x_.Add(_o_.ReadString(_t_));
                }
                else
                    _o_.SkipUnknownField(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 5)
            {
                var _x_ = ReliableNotifyQueue;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST)
                {
                    for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                        _x_.Add(_o_.ReadBinary(_t_));
                }
                else
                    _o_.SkipUnknownField(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 6)
            {
                ReliableNotifyConfirmCount = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 7)
            {
                ReliableNotifyTotalCount = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 8)
            {
                ProviderId = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 9)
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
            _ReliableNotifyMark.InitRootInfo(root, this);
            _ReliableNotifyQueue.InitRootInfo(root, this);
        }

        public override bool NegativeCheck()
        {
            if (LinkSid < 0) return true;
            if (State < 0) return true;
            if (ReliableNotifyConfirmCount < 0) return true;
            if (ReliableNotifyTotalCount < 0) return true;
            if (ProviderId < 0) return true;
            if (ProviderSessionId < 0) return true;
            return false;
        }
    }
}
