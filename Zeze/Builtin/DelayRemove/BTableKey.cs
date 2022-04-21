// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Builtin.DelayRemove
{
    public interface BTableKeyReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public Zeze.Transaction.Bean CopyBean();

        public string TableName { get; }
        public Zeze.Net.Binary EncodedKey { get; }
    }

    public sealed class BTableKey : Zeze.Transaction.Bean, BTableKeyReadOnly
    {
        string _TableName;
        Zeze.Net.Binary _EncodedKey;

        public string TableName
        {
            get
            {
                if (!IsManaged)
                    return _TableName;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _TableName;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__TableName)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _TableName;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException();
                if (!IsManaged)
                {
                    _TableName = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__TableName(this, value));
            }
        }

        public Zeze.Net.Binary EncodedKey
        {
            get
            {
                if (!IsManaged)
                    return _EncodedKey;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _EncodedKey;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__EncodedKey)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _EncodedKey;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException();
                if (!IsManaged)
                {
                    _EncodedKey = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__EncodedKey(this, value));
            }
        }

        public BTableKey() : this(0)
        {
        }

        public BTableKey(int _varId_) : base(_varId_)
        {
            _TableName = "";
            _EncodedKey = Zeze.Net.Binary.Empty;
        }

        public void Assign(BTableKey other)
        {
            TableName = other.TableName;
            EncodedKey = other.EncodedKey;
        }

        public BTableKey CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public BTableKey Copy()
        {
            var copy = new BTableKey();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BTableKey a, BTableKey b)
        {
            BTableKey save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public override Zeze.Transaction.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = 6060766480176216446;
        public override long TypeId => TYPEID;

        sealed class Log__TableName : Zeze.Transaction.Log<BTableKey, string>
        {
            public Log__TableName(BTableKey self, string value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 1;
            public override void Commit() { this.BeanTyped._TableName = this.Value; }
        }

        sealed class Log__EncodedKey : Zeze.Transaction.Log<BTableKey, Zeze.Net.Binary>
        {
            public Log__EncodedKey(BTableKey self, Zeze.Net.Binary value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 2;
            public override void Commit() { this.BeanTyped._EncodedKey = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.DelayRemove.BTableKey: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("TableName").Append('=').Append(TableName).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("EncodedKey").Append('=').Append(EncodedKey).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                string _x_ = TableName;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                var _x_ = EncodedKey;
                if (_x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
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
                TableName = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                EncodedKey = _o_.ReadBinary(_t_);
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
