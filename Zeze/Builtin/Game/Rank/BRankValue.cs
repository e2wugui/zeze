// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Builtin.Game.Rank
{
    public interface BRankValueReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public Zeze.Transaction.Bean CopyBean();

        public long RoleId { get; }
        public long Value { get; }
        public Zeze.Net.Binary ValueEx { get; }
    }

    public sealed class BRankValue : Zeze.Transaction.Bean, BRankValueReadOnly
    {
        long _RoleId;
        long _Value; // 含义由 BConcurrentKey.RankType 决定
        Zeze.Net.Binary _ValueEx; // 自定义数据。

        public long RoleId
        {
            get
            {
                if (!IsManaged)
                    return _RoleId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _RoleId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__RoleId)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _RoleId;
            }
            set
            {
                if (!IsManaged)
                {
                    _RoleId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__RoleId(this, value));
            }
        }

        public long Value
        {
            get
            {
                if (!IsManaged)
                    return _Value;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Value;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Value)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _Value;
            }
            set
            {
                if (!IsManaged)
                {
                    _Value = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Value(this, value));
            }
        }

        public Zeze.Net.Binary ValueEx
        {
            get
            {
                if (!IsManaged)
                    return _ValueEx;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ValueEx;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ValueEx)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _ValueEx;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException();
                if (!IsManaged)
                {
                    _ValueEx = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ValueEx(this, value));
            }
        }

        public BRankValue() : this(0)
        {
        }

        public BRankValue(int _varId_) : base(_varId_)
        {
            _ValueEx = Zeze.Net.Binary.Empty;
        }

        public void Assign(BRankValue other)
        {
            RoleId = other.RoleId;
            Value = other.Value;
            ValueEx = other.ValueEx;
        }

        public BRankValue CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public BRankValue Copy()
        {
            var copy = new BRankValue();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BRankValue a, BRankValue b)
        {
            BRankValue save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public override Zeze.Transaction.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = 2276228832088785165;
        public override long TypeId => TYPEID;

        sealed class Log__RoleId : Zeze.Transaction.Log<BRankValue, long>
        {
            public Log__RoleId(BRankValue self, long value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 1;
            public override void Commit() { this.BeanTyped._RoleId = this.Value; }
        }

        sealed class Log__Value : Zeze.Transaction.Log<BRankValue, long>
        {
            public Log__Value(BRankValue self, long value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 2;
            public override void Commit() { this.BeanTyped._Value = this.Value; }
        }

        sealed class Log__ValueEx : Zeze.Transaction.Log<BRankValue, Zeze.Net.Binary>
        {
            public Log__ValueEx(BRankValue self, Zeze.Net.Binary value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 3;
            public override void Commit() { this.BeanTyped._ValueEx = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Game.Rank.BRankValue: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("RoleId").Append('=').Append(RoleId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Value").Append('=').Append(Value).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ValueEx").Append('=').Append(ValueEx).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                long _x_ = RoleId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                long _x_ = Value;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                var _x_ = ValueEx;
                if (_x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
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
                RoleId = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                Value = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                ValueEx = _o_.ReadBinary(_t_);
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
            if (RoleId < 0) return true;
            if (Value < 0) return true;
            return false;
        }
    }
}
