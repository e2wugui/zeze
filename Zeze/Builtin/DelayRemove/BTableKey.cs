// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.DelayRemove
{
    public interface BTableKeyReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BTableKey Copy();

        public string TableName { get; }
        public Zeze.Net.Binary EncodedKey { get; }
        public long EnqueueTime { get; }
    }

    public sealed class BTableKey : Zeze.Transaction.Bean, BTableKeyReadOnly
    {
        string _TableName;
        Zeze.Net.Binary _EncodedKey;
        long _EnqueueTime;

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
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _TableName = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__TableName() { Belong = this, VariableId = 1, Value = value });
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
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _EncodedKey = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__EncodedKey() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public long EnqueueTime
        {
            get
            {
                if (!IsManaged)
                    return _EnqueueTime;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _EnqueueTime;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__EnqueueTime)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _EnqueueTime;
            }
            set
            {
                if (!IsManaged)
                {
                    _EnqueueTime = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__EnqueueTime() { Belong = this, VariableId = 3, Value = value });
            }
        }

        public BTableKey()
        {
            _TableName = "";
            _EncodedKey = Zeze.Net.Binary.Empty;
        }

        public BTableKey(string _TableName_, Zeze.Net.Binary _EncodedKey_, long _EnqueueTime_)
        {
            _TableName = _TableName_;
            _EncodedKey = _EncodedKey_;
            _EnqueueTime = _EnqueueTime_;
        }

        public void Assign(BTableKey other)
        {
            TableName = other.TableName;
            EncodedKey = other.EncodedKey;
            EnqueueTime = other.EnqueueTime;
        }

        public BTableKey CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BTableKey Copy()
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

        public const long TYPEID = 6060766480176216446;
        public override long TypeId => TYPEID;

        sealed class Log__TableName : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BTableKey)Belong)._TableName = this.Value; }
        }

        sealed class Log__EncodedKey : Zeze.Transaction.Log<Zeze.Net.Binary>
        {
            public override void Commit() { ((BTableKey)Belong)._EncodedKey = this.Value; }
        }

        sealed class Log__EnqueueTime : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BTableKey)Belong)._EnqueueTime = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.DelayRemove.BTableKey: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("TableName").Append('=').Append(TableName).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("EncodedKey").Append('=').Append(EncodedKey).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("EnqueueTime").Append('=').Append(EnqueueTime).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                string _x_ = TableName;
                if (_x_ != null && _x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                var _x_ = EncodedKey;
                if (_x_ != null && _x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                    _o_.WriteBinary(_x_);
                }
            }
            {
                long _x_ = EnqueueTime;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
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
                TableName = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                EncodedKey = _o_.ReadBinary(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                EnqueueTime = _o_.ReadLong(_t_);
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
            if (EnqueueTime < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _TableName = vlog.StringValue(); break;
                    case 2: _EncodedKey = vlog.BinaryValue(); break;
                    case 3: _EnqueueTime = vlog.LongValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            TableName = "";
            EncodedKey = Zeze.Net.Binary.Empty;
            EnqueueTime = 0;
        }
    }
}
