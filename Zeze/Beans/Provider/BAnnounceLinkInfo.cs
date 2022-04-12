// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Beans.Provider
{
    public interface BAnnounceLinkInfoReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public Zeze.Transaction.Bean CopyBean();

        public int LinkId { get; }
        public long ProviderSessionId { get; }
    }

    public sealed class BAnnounceLinkInfo : Zeze.Transaction.Bean, BAnnounceLinkInfoReadOnly
    {
        int _LinkId; // reserve
        long _ProviderSessionId;

        public int LinkId
        {
            get
            {
                if (!IsManaged)
                    return _LinkId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _LinkId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__LinkId)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _LinkId;
            }
            set
            {
                if (!IsManaged)
                {
                    _LinkId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__LinkId(this, value));
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
                var log = (Log__ProviderSessionId)txn.GetLog(ObjectId + 2);
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

        public BAnnounceLinkInfo() : this(0)
        {
        }

        public BAnnounceLinkInfo(int _varId_) : base(_varId_)
        {
        }

        public void Assign(BAnnounceLinkInfo other)
        {
            LinkId = other.LinkId;
            ProviderSessionId = other.ProviderSessionId;
        }

        public BAnnounceLinkInfo CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public BAnnounceLinkInfo Copy()
        {
            var copy = new BAnnounceLinkInfo();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BAnnounceLinkInfo a, BAnnounceLinkInfo b)
        {
            BAnnounceLinkInfo save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public override Zeze.Transaction.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = -7068079009435311792;
        public override long TypeId => TYPEID;

        sealed class Log__LinkId : Zeze.Transaction.Log<BAnnounceLinkInfo, int>
        {
            public Log__LinkId(BAnnounceLinkInfo self, int value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 1;
            public override void Commit() { this.BeanTyped._LinkId = this.Value; }
        }

        sealed class Log__ProviderSessionId : Zeze.Transaction.Log<BAnnounceLinkInfo, long>
        {
            public Log__ProviderSessionId(BAnnounceLinkInfo self, long value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 2;
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Beans.Provider.BAnnounceLinkInfo: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LinkId").Append('=').Append(LinkId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ProviderSessionId").Append('=').Append(ProviderSessionId).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                int _x_ = LinkId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                long _x_ = ProviderSessionId;
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
                LinkId = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
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
            if (LinkId < 0) return true;
            if (ProviderSessionId < 0) return true;
            return false;
        }
    }
}
