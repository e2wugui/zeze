// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Beans.ProviderDirect
{
    public interface BProviderInfoReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public Zeze.Transaction.Bean CopyBean();

        public string Ip { get; }
        public int Port { get; }
    }

    public sealed class BProviderInfo : Zeze.Transaction.Bean, BProviderInfoReadOnly
    {
        string _Ip;
        int _Port;

        public string Ip
        {
            get
            {
                if (!IsManaged)
                    return _Ip;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Ip;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Ip)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _Ip;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException();
                if (!IsManaged)
                {
                    _Ip = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Ip(this, value));
            }
        }

        public int Port
        {
            get
            {
                if (!IsManaged)
                    return _Port;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Port;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Port)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _Port;
            }
            set
            {
                if (!IsManaged)
                {
                    _Port = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Port(this, value));
            }
        }

        public BProviderInfo() : this(0)
        {
        }

        public BProviderInfo(int _varId_) : base(_varId_)
        {
            _Ip = "";
        }

        public void Assign(BProviderInfo other)
        {
            Ip = other.Ip;
            Port = other.Port;
        }

        public BProviderInfo CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public BProviderInfo Copy()
        {
            var copy = new BProviderInfo();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BProviderInfo a, BProviderInfo b)
        {
            BProviderInfo save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public override Zeze.Transaction.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = 2259458453801663225;
        public override long TypeId => TYPEID;

        sealed class Log__Ip : Zeze.Transaction.Log<BProviderInfo, string>
        {
            public Log__Ip(BProviderInfo self, string value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 1;
            public override void Commit() { this.BeanTyped._Ip = this.Value; }
        }

        sealed class Log__Port : Zeze.Transaction.Log<BProviderInfo, int>
        {
            public Log__Port(BProviderInfo self, int value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 2;
            public override void Commit() { this.BeanTyped._Port = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Beans.ProviderDirect.BProviderInfo: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Ip").Append('=').Append(Ip).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Port").Append('=').Append(Port).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                string _x_ = Ip;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                int _x_ = Port;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
                Ip = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                Port = _o_.ReadInt(_t_);
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
            if (Port < 0) return true;
            return false;
        }
    }
}
