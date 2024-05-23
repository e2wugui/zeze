// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.AutoKey
{
    public interface BAutoKeyReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BAutoKey Copy();

        public long NextId { get; }
    }

    public sealed class BAutoKey : Zeze.Transaction.Bean, BAutoKeyReadOnly
    {
        long _NextId;

        public long NextId
        {
            get
            {
                if (!IsManaged)
                    return _NextId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _NextId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__NextId)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _NextId;
            }
            set
            {
                if (!IsManaged)
                {
                    _NextId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__NextId() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public BAutoKey()
        {
        }

        public BAutoKey(long _NextId_)
        {
            _NextId = _NextId_;
        }

        public void Assign(BAutoKey other)
        {
            NextId = other.NextId;
        }

        public BAutoKey CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BAutoKey Copy()
        {
            var copy = new BAutoKey();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BAutoKey a, BAutoKey b)
        {
            BAutoKey save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = 3694349315876280858;
        public override long TypeId => TYPEID;

        sealed class Log__NextId : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BAutoKey)Belong)._NextId = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.AutoKey.BAutoKey: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("NextId").Append('=').Append(NextId).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                long _x_ = NextId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
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
                NextId = _o_.ReadLong(_t_);
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
            if (NextId < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _NextId = vlog.LongValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            NextId = 0;
        }
    }
}
