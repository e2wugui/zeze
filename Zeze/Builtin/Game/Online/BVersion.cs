// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Builtin.Game.Online
{
    public interface BVersionReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public Zeze.Transaction.Bean CopyBean();

        public long LoginVersion { get; }
        public System.Collections.Generic.IReadOnlySet<string> ReliableNotifyMark { get; }
        public System.Collections.Generic.IReadOnlyList<Zeze.Net.Binary>ReliableNotifyQueue { get; }
        public long ReliableNotifyConfirmCount { get; }
        public long ReliableNotifyTotalCount { get; }
        public int ServerId { get; }
    }

    public sealed class BVersion : Zeze.Transaction.Bean, BVersionReadOnly
    {
        long _LoginVersion;
        readonly Zeze.Transaction.Collections.PSet1<string> _ReliableNotifyMark;
        readonly Zeze.Transaction.Collections.PList1<Zeze.Net.Binary> _ReliableNotifyQueue; // full encoded protocol list
        long _ReliableNotifyConfirmCount;
        long _ReliableNotifyTotalCount;
        int _ServerId;

        public long LoginVersion
        {
            get
            {
                if (!IsManaged)
                    return _LoginVersion;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _LoginVersion;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__LoginVersion)txn.GetLog(ObjectId + 1);
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
                txn.PutLog(new Log__LoginVersion(this, value));
            }
        }

        public Zeze.Transaction.Collections.PSet1<string> ReliableNotifyMark => _ReliableNotifyMark;
        System.Collections.Generic.IReadOnlySet<string> Zeze.Builtin.Game.Online.BVersionReadOnly.ReliableNotifyMark => _ReliableNotifyMark;

        public Zeze.Transaction.Collections.PList1<Zeze.Net.Binary> ReliableNotifyQueue => _ReliableNotifyQueue;
        System.Collections.Generic.IReadOnlyList<Zeze.Net.Binary> Zeze.Builtin.Game.Online.BVersionReadOnly.ReliableNotifyQueue => _ReliableNotifyQueue;

        public long ReliableNotifyConfirmCount
        {
            get
            {
                if (!IsManaged)
                    return _ReliableNotifyConfirmCount;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ReliableNotifyConfirmCount;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ReliableNotifyConfirmCount)txn.GetLog(ObjectId + 4);
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
                var log = (Log__ReliableNotifyTotalCount)txn.GetLog(ObjectId + 5);
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

        public int ServerId
        {
            get
            {
                if (!IsManaged)
                    return _ServerId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ServerId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ServerId)txn.GetLog(ObjectId + 6);
                return log != null ? log.Value : _ServerId;
            }
            set
            {
                if (!IsManaged)
                {
                    _ServerId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ServerId(this, value));
            }
        }

        public BVersion() : this(0)
        {
        }

        public BVersion(int _varId_) : base(_varId_)
        {
            _ReliableNotifyMark = new Zeze.Transaction.Collections.PSet1<string>(ObjectId + 2, _v => new Log__ReliableNotifyMark(this, _v));
            _ReliableNotifyQueue = new Zeze.Transaction.Collections.PList1<Zeze.Net.Binary>(ObjectId + 3, _v => new Log__ReliableNotifyQueue(this, _v));
        }

        public void Assign(BVersion other)
        {
            LoginVersion = other.LoginVersion;
            ReliableNotifyMark.Clear();
            foreach (var e in other.ReliableNotifyMark)
                ReliableNotifyMark.Add(e);
            ReliableNotifyQueue.Clear();
            foreach (var e in other.ReliableNotifyQueue)
                ReliableNotifyQueue.Add(e);
            ReliableNotifyConfirmCount = other.ReliableNotifyConfirmCount;
            ReliableNotifyTotalCount = other.ReliableNotifyTotalCount;
            ServerId = other.ServerId;
        }

        public BVersion CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public BVersion Copy()
        {
            var copy = new BVersion();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BVersion a, BVersion b)
        {
            BVersion save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public override Zeze.Transaction.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = -4544955921052723023;
        public override long TypeId => TYPEID;

        sealed class Log__LoginVersion : Zeze.Transaction.Log<BVersion, long>
        {
            public Log__LoginVersion(BVersion self, long value) : base(self, value) {}
            public override long LogKey => this.Belong.ObjectId + 1;
            public override void Commit() { this.BeanTyped._LoginVersion = this.Value; }
        }

        sealed class Log__ReliableNotifyMark : Zeze.Transaction.Collections.PSet1<string>.LogV
        {
            public Log__ReliableNotifyMark(BVersion host, System.Collections.Immutable.ImmutableHashSet<string> value) : base(host, value) {}
            public override long LogKey => Belong.ObjectId + 2;
            public BVersion BeanTyped => (BVersion)Belong;
            public override void Commit() { Commit(BeanTyped._ReliableNotifyMark); }
        }

        sealed class Log__ReliableNotifyQueue : Zeze.Transaction.Collections.PList1<Zeze.Net.Binary>.LogV
        {
            public Log__ReliableNotifyQueue(BVersion host, System.Collections.Immutable.ImmutableList<Zeze.Net.Binary> value) : base(host, value) {}
            public override long LogKey => Belong.ObjectId + 3;
            public BVersion BeanTyped => (BVersion)Belong;
            public override void Commit() { Commit(BeanTyped._ReliableNotifyQueue); }
        }

        sealed class Log__ReliableNotifyConfirmCount : Zeze.Transaction.Log<BVersion, long>
        {
            public Log__ReliableNotifyConfirmCount(BVersion self, long value) : base(self, value) {}
            public override long LogKey => this.Belong.ObjectId + 4;
            public override void Commit() { this.BeanTyped._ReliableNotifyConfirmCount = this.Value; }
        }

        sealed class Log__ReliableNotifyTotalCount : Zeze.Transaction.Log<BVersion, long>
        {
            public Log__ReliableNotifyTotalCount(BVersion self, long value) : base(self, value) {}
            public override long LogKey => this.Belong.ObjectId + 5;
            public override void Commit() { this.BeanTyped._ReliableNotifyTotalCount = this.Value; }
        }

        sealed class Log__ServerId : Zeze.Transaction.Log<BVersion, int>
        {
            public Log__ServerId(BVersion self, int value) : base(self, value) {}
            public override long LogKey => this.Belong.ObjectId + 6;
            public override void Commit() { this.BeanTyped._ServerId = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Game.Online.BVersion: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LoginVersion").Append('=').Append(LoginVersion).Append(',').Append(Environment.NewLine);
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ServerId").Append('=').Append(ServerId).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                long _x_ = LoginVersion;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                var _x_ = ReliableNotifyMark;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                    _o_.WriteListType(_n_, ByteBuffer.BYTES);
                    foreach (var _v_ in _x_)
                    {
                        _o_.WriteString(_v_);
                    }
                }
            }
            {
                var _x_ = ReliableNotifyQueue;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                    _o_.WriteListType(_n_, ByteBuffer.BYTES);
                    foreach (var _v_ in _x_)
                    {
                        _o_.WriteBinary(_v_);
                    }
                }
            }
            {
                long _x_ = ReliableNotifyConfirmCount;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                long _x_ = ReliableNotifyTotalCount;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                int _x_ = ServerId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
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
                LoginVersion = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                var _x_ = ReliableNotifyMark;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST)
                {
                    for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    {
                        _x_.Add(_o_.ReadString(_t_));
                    }
                }
                else
                    _o_.SkipUnknownField(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                var _x_ = ReliableNotifyQueue;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST)
                {
                    for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    {
                        _x_.Add(_o_.ReadBinary(_t_));
                    }
                }
                else
                    _o_.SkipUnknownField(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                ReliableNotifyConfirmCount = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 5)
            {
                ReliableNotifyTotalCount = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 6)
            {
                ServerId = _o_.ReadInt(_t_);
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
            if (LoginVersion < 0) return true;
            if (ReliableNotifyConfirmCount < 0) return true;
            if (ReliableNotifyTotalCount < 0) return true;
            if (ServerId < 0) return true;
            return false;
        }
    }
}
