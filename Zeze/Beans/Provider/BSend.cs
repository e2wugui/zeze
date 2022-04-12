// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Beans.Provider
{
    public interface BSendReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public Zeze.Transaction.Bean CopyBean();

        public System.Collections.Generic.IReadOnlySet<long> LinkSids { get; }
        public long ProtocolType { get; }
        public Zeze.Net.Binary ProtocolWholeData { get; }
        public long ConfirmSerialId { get; }
    }

    public sealed class BSend : Zeze.Transaction.Bean, BSendReadOnly
    {
        readonly Zeze.Transaction.Collections.PSet1<long> _linkSids;
        long _protocolType;
        Zeze.Net.Binary _protocolWholeData; // 完整的协议打包，包括了 type, size
        long _ConfirmSerialId; // 不为0的时候，linkd发送SendConfirm回逻辑服务器

        public Zeze.Transaction.Collections.PSet1<long> LinkSids => _linkSids;
        System.Collections.Generic.IReadOnlySet<long> Zeze.Beans.Provider.BSendReadOnly.LinkSids => _linkSids;

        public long ProtocolType
        {
            get
            {
                if (!IsManaged)
                    return _protocolType;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _protocolType;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__protocolType)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _protocolType;
            }
            set
            {
                if (!IsManaged)
                {
                    _protocolType = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__protocolType(this, value));
            }
        }

        public Zeze.Net.Binary ProtocolWholeData
        {
            get
            {
                if (!IsManaged)
                    return _protocolWholeData;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _protocolWholeData;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__protocolWholeData)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _protocolWholeData;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException();
                if (!IsManaged)
                {
                    _protocolWholeData = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__protocolWholeData(this, value));
            }
        }

        public long ConfirmSerialId
        {
            get
            {
                if (!IsManaged)
                    return _ConfirmSerialId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ConfirmSerialId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ConfirmSerialId)txn.GetLog(ObjectId + 4);
                return log != null ? log.Value : _ConfirmSerialId;
            }
            set
            {
                if (!IsManaged)
                {
                    _ConfirmSerialId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ConfirmSerialId(this, value));
            }
        }

        public BSend() : this(0)
        {
        }

        public BSend(int _varId_) : base(_varId_)
        {
            _linkSids = new Zeze.Transaction.Collections.PSet1<long>(ObjectId + 1, _v => new Log__linkSids(this, _v));
            _protocolWholeData = Zeze.Net.Binary.Empty;
        }

        public void Assign(BSend other)
        {
            LinkSids.Clear();
            foreach (var e in other.LinkSids)
                LinkSids.Add(e);
            ProtocolType = other.ProtocolType;
            ProtocolWholeData = other.ProtocolWholeData;
            ConfirmSerialId = other.ConfirmSerialId;
        }

        public BSend CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public BSend Copy()
        {
            var copy = new BSend();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BSend a, BSend b)
        {
            BSend save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public override Zeze.Transaction.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = -4918715010945266727;
        public override long TypeId => TYPEID;

        sealed class Log__linkSids : Zeze.Transaction.Collections.PSet1<long>.LogV
        {
            public Log__linkSids(BSend host, System.Collections.Immutable.ImmutableHashSet<long> value) : base(host, value) {}
            public override long LogKey => Bean.ObjectId + 1;
            public BSend BeanTyped => (BSend)Bean;
            public override void Commit() { Commit(BeanTyped._linkSids); }
        }

        sealed class Log__protocolType : Zeze.Transaction.Log<BSend, long>
        {
            public Log__protocolType(BSend self, long value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 2;
            public override void Commit() { this.BeanTyped._protocolType = this.Value; }
        }

        sealed class Log__protocolWholeData : Zeze.Transaction.Log<BSend, Zeze.Net.Binary>
        {
            public Log__protocolWholeData(BSend self, Zeze.Net.Binary value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 3;
            public override void Commit() { this.BeanTyped._protocolWholeData = this.Value; }
        }

        sealed class Log__ConfirmSerialId : Zeze.Transaction.Log<BSend, long>
        {
            public Log__ConfirmSerialId(BSend self, long value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 4;
            public override void Commit() { this.BeanTyped._ConfirmSerialId = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Beans.Provider.BSend: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LinkSids").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var Item in LinkSids)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Item").Append('=').Append(Item).Append(',').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ProtocolType").Append('=').Append(ProtocolType).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ProtocolWholeData").Append('=').Append(ProtocolWholeData).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ConfirmSerialId").Append('=').Append(ConfirmSerialId).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                var _x_ = LinkSids;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                    _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                    foreach (var _v_ in _x_)
                        _o_.WriteLong(_v_);
                }
            }
            {
                long _x_ = ProtocolType;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                var _x_ = ProtocolWholeData;
                if (_x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                    _o_.WriteBinary(_x_);
                }
            }
            {
                long _x_ = ConfirmSerialId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
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
                var _x_ = LinkSids;
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
            if (_i_ == 2)
            {
                ProtocolType = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                ProtocolWholeData = _o_.ReadBinary(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                ConfirmSerialId = _o_.ReadLong(_t_);
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
            _linkSids.InitRootInfo(root, this);
        }

        public override bool NegativeCheck()
        {
            foreach (var _v_ in LinkSids)
            {
                if (_v_ < 0) return true;
            }
            if (ProtocolType < 0) return true;
            if (ConfirmSerialId < 0) return true;
            return false;
        }
    }
}
