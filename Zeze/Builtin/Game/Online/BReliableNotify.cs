// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Builtin.Game.Online
{
    public interface BReliableNotifyReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public Zeze.Transaction.Bean CopyBean();

        public System.Collections.Generic.IReadOnlyList<Zeze.Net.Binary>Notifies { get; }
        public long ReliableNotifyTotalCountStart { get; }
    }

    public sealed class BReliableNotify : Zeze.Transaction.Bean, BReliableNotifyReadOnly
    {
        readonly Zeze.Transaction.Collections.PList1<Zeze.Net.Binary> _Notifies; // full encoded protocol list
        long _ReliableNotifyTotalCountStart; // Notify的计数开始。客户端收到的总计数为：start + Notifies.Count

        public Zeze.Transaction.Collections.PList1<Zeze.Net.Binary> Notifies => _Notifies;
        System.Collections.Generic.IReadOnlyList<Zeze.Net.Binary> Zeze.Builtin.Game.Online.BReliableNotifyReadOnly.Notifies => _Notifies;

        public long ReliableNotifyTotalCountStart
        {
            get
            {
                if (!IsManaged)
                    return _ReliableNotifyTotalCountStart;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ReliableNotifyTotalCountStart;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ReliableNotifyTotalCountStart)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _ReliableNotifyTotalCountStart;
            }
            set
            {
                if (!IsManaged)
                {
                    _ReliableNotifyTotalCountStart = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ReliableNotifyTotalCountStart(this, value));
            }
        }

        public BReliableNotify() : this(0)
        {
        }

        public BReliableNotify(int _varId_) : base(_varId_)
        {
            _Notifies = new Zeze.Transaction.Collections.PList1<Zeze.Net.Binary>(ObjectId + 1, _v => new Log__Notifies(this, _v));
        }

        public void Assign(BReliableNotify other)
        {
            Notifies.Clear();
            foreach (var e in other.Notifies)
                Notifies.Add(e);
            ReliableNotifyTotalCountStart = other.ReliableNotifyTotalCountStart;
        }

        public BReliableNotify CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public BReliableNotify Copy()
        {
            var copy = new BReliableNotify();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BReliableNotify a, BReliableNotify b)
        {
            BReliableNotify save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public override Zeze.Transaction.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = -6166834646872658332;
        public override long TypeId => TYPEID;

        sealed class Log__Notifies : Zeze.Transaction.Collections.PList1<Zeze.Net.Binary>.LogV
        {
            public Log__Notifies(BReliableNotify host, System.Collections.Immutable.ImmutableList<Zeze.Net.Binary> value) : base(host, value) {}
            public override long LogKey => Belong.ObjectId + 1;
            public BReliableNotify BeanTyped => (BReliableNotify)Belong;
            public override void Commit() { Commit(BeanTyped._Notifies); }
        }

        sealed class Log__ReliableNotifyTotalCountStart : Zeze.Transaction.Log<BReliableNotify, long>
        {
            public Log__ReliableNotifyTotalCountStart(BReliableNotify self, long value) : base(self, value) {}
            public override long LogKey => this.Belong.ObjectId + 2;
            public override void Commit() { this.BeanTyped._ReliableNotifyTotalCountStart = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Game.Online.BReliableNotify: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Notifies").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var Item in Notifies)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Item").Append('=').Append(Item).Append(',').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ReliableNotifyTotalCountStart").Append('=').Append(ReliableNotifyTotalCountStart).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                var _x_ = Notifies;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                    _o_.WriteListType(_n_, ByteBuffer.BYTES);
                    foreach (var _v_ in _x_)
                    {
                        _o_.WriteBinary(_v_);
                    }
                }
            }
            {
                long _x_ = ReliableNotifyTotalCountStart;
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
                var _x_ = Notifies;
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
            if (_i_ == 2)
            {
                ReliableNotifyTotalCountStart = _o_.ReadLong(_t_);
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
            _Notifies.InitRootInfo(root, this);
        }

        public override bool NegativeCheck()
        {
            if (ReliableNotifyTotalCountStart < 0) return true;
            return false;
        }
    }
}
