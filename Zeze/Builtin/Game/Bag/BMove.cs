// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Builtin.Game.Bag
{
    public interface BMoveReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public Zeze.Transaction.Bean CopyBean();

        public string BagName { get; }
        public int PositionFrom { get; }
        public int PositionTo { get; }
        public int Number { get; }
    }

    public sealed class BMove : Zeze.Transaction.Bean, BMoveReadOnly
    {
        string _BagName;
        int _PositionFrom;
        int _PositionTo;
        int _number; // -1 表示全部

        public string BagName
        {
            get
            {
                if (!IsManaged)
                    return _BagName;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _BagName;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__BagName)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _BagName;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException();
                if (!IsManaged)
                {
                    _BagName = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__BagName(this, value));
            }
        }

        public int PositionFrom
        {
            get
            {
                if (!IsManaged)
                    return _PositionFrom;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _PositionFrom;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__PositionFrom)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _PositionFrom;
            }
            set
            {
                if (!IsManaged)
                {
                    _PositionFrom = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__PositionFrom(this, value));
            }
        }

        public int PositionTo
        {
            get
            {
                if (!IsManaged)
                    return _PositionTo;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _PositionTo;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__PositionTo)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _PositionTo;
            }
            set
            {
                if (!IsManaged)
                {
                    _PositionTo = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__PositionTo(this, value));
            }
        }

        public int Number
        {
            get
            {
                if (!IsManaged)
                    return _number;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _number;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__number)txn.GetLog(ObjectId + 4);
                return log != null ? log.Value : _number;
            }
            set
            {
                if (!IsManaged)
                {
                    _number = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__number(this, value));
            }
        }

        public BMove() : this(0)
        {
        }

        public BMove(int _varId_) : base(_varId_)
        {
            _BagName = "";
        }

        public void Assign(BMove other)
        {
            BagName = other.BagName;
            PositionFrom = other.PositionFrom;
            PositionTo = other.PositionTo;
            Number = other.Number;
        }

        public BMove CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public BMove Copy()
        {
            var copy = new BMove();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BMove a, BMove b)
        {
            BMove save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public override Zeze.Transaction.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = -7346236832819011963;
        public override long TypeId => TYPEID;

        sealed class Log__BagName : Zeze.Transaction.Log<BMove, string>
        {
            public Log__BagName(BMove self, string value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 1;
            public override void Commit() { this.BeanTyped._BagName = this.Value; }
        }

        sealed class Log__PositionFrom : Zeze.Transaction.Log<BMove, int>
        {
            public Log__PositionFrom(BMove self, int value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 2;
            public override void Commit() { this.BeanTyped._PositionFrom = this.Value; }
        }

        sealed class Log__PositionTo : Zeze.Transaction.Log<BMove, int>
        {
            public Log__PositionTo(BMove self, int value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 3;
            public override void Commit() { this.BeanTyped._PositionTo = this.Value; }
        }

        sealed class Log__number : Zeze.Transaction.Log<BMove, int>
        {
            public Log__number(BMove self, int value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 4;
            public override void Commit() { this.BeanTyped._number = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Game.Bag.BMove: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("BagName").Append('=').Append(BagName).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("PositionFrom").Append('=').Append(PositionFrom).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("PositionTo").Append('=').Append(PositionTo).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Number").Append('=').Append(Number).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                string _x_ = BagName;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                int _x_ = PositionFrom;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                int _x_ = PositionTo;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                int _x_ = Number;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
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
                BagName = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                PositionFrom = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                PositionTo = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                Number = _o_.ReadInt(_t_);
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
            if (PositionFrom < 0) return true;
            if (PositionTo < 0) return true;
            if (Number < 0) return true;
            return false;
        }
    }
}
