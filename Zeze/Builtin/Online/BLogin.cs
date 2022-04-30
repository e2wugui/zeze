// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Builtin.Online
{
    public interface BLoginReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public Zeze.Transaction.Bean CopyBean();

        public string ClientId { get; }
    }

    public sealed class BLogin : Zeze.Transaction.Bean, BLoginReadOnly
    {
        string _ClientId;

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

        public BLogin() : this(0)
        {
        }

        public BLogin(int _varId_) : base(_varId_)
        {
            _ClientId = "";
        }

        public void Assign(BLogin other)
        {
            ClientId = other.ClientId;
        }

        public BLogin CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public BLogin Copy()
        {
            var copy = new BLogin();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BLogin a, BLogin b)
        {
            BLogin save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public override Zeze.Transaction.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = -2914025305442353160;
        public override long TypeId => TYPEID;

        sealed class Log__ClientId : Zeze.Transaction.Log<BLogin, string>
        {
            public Log__ClientId(BLogin self, string value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 1;
            public override void Commit() { this.BeanTyped._ClientId = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Online.BLogin: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ClientId").Append('=').Append(ClientId).Append(Environment.NewLine);
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
            return false;
        }
    }
}
