// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Game.Bag
{
    public interface BDestroyReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BDestroy Copy();

        public string BagName { get; }
        public int Position { get; }
    }

    public sealed class BDestroy : Zeze.Transaction.Bean, BDestroyReadOnly
    {
        string _BagName;
        int _Position;

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
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _BagName = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__BagName() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public int Position
        {
            get
            {
                if (!IsManaged)
                    return _Position;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Position;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Position)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _Position;
            }
            set
            {
                if (!IsManaged)
                {
                    _Position = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Position() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public BDestroy()
        {
            _BagName = "";
        }

        public BDestroy(string _BagName_, int _Position_)
        {
            _BagName = _BagName_;
            _Position = _Position_;
        }

        public void Assign(BDestroy other)
        {
            BagName = other.BagName;
            Position = other.Position;
        }

        public BDestroy CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BDestroy Copy()
        {
            var copy = new BDestroy();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BDestroy a, BDestroy b)
        {
            BDestroy save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = -3139270057603893776;
        public override long TypeId => TYPEID;

        sealed class Log__BagName : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BDestroy)Belong)._BagName = this.Value; }
        }

        sealed class Log__Position : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((BDestroy)Belong)._Position = this.Value; }
        }

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Game.Bag.BDestroy: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("BagName").Append('=').Append(BagName).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Position").Append('=').Append(Position).Append(Environment.NewLine);
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
                int _x_ = Position;
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
                BagName = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                Position = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            while (_t_ != 0)
            {
                _o_.SkipUnknownField(_t_);
                _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
        }

        public override bool NegativeCheck()
        {
            if (Position < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _BagName = vlog.StringValue(); break;
                    case 2: _Position = vlog.IntValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            BagName = "";
            Position = 0;
        }
    }
}
