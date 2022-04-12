// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Beans.Provider
{
    public interface BSendConfirmReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public Zeze.Transaction.Bean CopyBean();

        public long ConfirmSerialId { get; }
        public string LinkName { get; }
    }

    public sealed class BSendConfirm : Zeze.Transaction.Bean, BSendConfirmReadOnly
    {
        long _ConfirmSerialId; // SendConfirm 参数，即Send.Argument.ConfirmSerialId
        string _LinkName;

        public long ConfirmSerialId
        {
            get
            {
                if (!IsManaged)
                    return _ConfirmSerialId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ConfirmSerialId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ConfirmSerialId)txn.GetLog(ObjectId + 1);
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

        public string LinkName
        {
            get
            {
                if (!IsManaged)
                    return _LinkName;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _LinkName;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__LinkName)txn.GetLog(ObjectId + 2);
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

        public BSendConfirm() : this(0)
        {
        }

        public BSendConfirm(int _varId_) : base(_varId_)
        {
            _LinkName = "";
        }

        public void Assign(BSendConfirm other)
        {
            ConfirmSerialId = other.ConfirmSerialId;
            LinkName = other.LinkName;
        }

        public BSendConfirm CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public BSendConfirm Copy()
        {
            var copy = new BSendConfirm();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BSendConfirm a, BSendConfirm b)
        {
            BSendConfirm save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public override Zeze.Transaction.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = 4100720956573778471;
        public override long TypeId => TYPEID;

        sealed class Log__ConfirmSerialId : Zeze.Transaction.Log<BSendConfirm, long>
        {
            public Log__ConfirmSerialId(BSendConfirm self, long value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 1;
            public override void Commit() { this.BeanTyped._ConfirmSerialId = this.Value; }
        }

        sealed class Log__LinkName : Zeze.Transaction.Log<BSendConfirm, string>
        {
            public Log__LinkName(BSendConfirm self, string value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 2;
            public override void Commit() { this.BeanTyped._LinkName = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Beans.Provider.BSendConfirm: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ConfirmSerialId").Append('=').Append(ConfirmSerialId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LinkName").Append('=').Append(LinkName).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                long _x_ = ConfirmSerialId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                string _x_ = LinkName;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
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
                ConfirmSerialId = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                LinkName = _o_.ReadString(_t_);
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
            if (ConfirmSerialId < 0) return true;
            return false;
        }
    }
}
