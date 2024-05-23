// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Provider
{
    public interface BBroadcastReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BBroadcast Copy();

        public long ProtocolType { get; }
        public Zeze.Net.Binary ProtocolWholeData { get; }
        public int Time { get; }
    }

    public sealed class BBroadcast : Zeze.Transaction.Bean, BBroadcastReadOnly
    {
        long _protocolType;
        Zeze.Net.Binary _protocolWholeData; // 完整的协议打包，包括了 type, size
        int _time;

        public long ProtocolType
        {
            get
            {
                if (!IsManaged)
                    return _protocolType;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _protocolType;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__protocolType)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _protocolType;
            }
            set
            {
                if (!IsManaged)
                {
                    _protocolType = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__protocolType() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public Zeze.Net.Binary ProtocolWholeData
        {
            get
            {
                if (!IsManaged)
                    return _protocolWholeData;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _protocolWholeData;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__protocolWholeData)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _protocolWholeData;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _protocolWholeData = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__protocolWholeData() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public int Time
        {
            get
            {
                if (!IsManaged)
                    return _time;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _time;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__time)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _time;
            }
            set
            {
                if (!IsManaged)
                {
                    _time = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__time() { Belong = this, VariableId = 3, Value = value });
            }
        }

        public BBroadcast()
        {
            _protocolWholeData = Zeze.Net.Binary.Empty;
        }

        public BBroadcast(long _protocolType_, Zeze.Net.Binary _protocolWholeData_, int _time_)
        {
            _protocolType = _protocolType_;
            _protocolWholeData = _protocolWholeData_;
            _time = _time_;
        }

        public void Assign(BBroadcast other)
        {
            ProtocolType = other.ProtocolType;
            ProtocolWholeData = other.ProtocolWholeData;
            Time = other.Time;
        }

        public BBroadcast CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BBroadcast Copy()
        {
            var copy = new BBroadcast();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BBroadcast a, BBroadcast b)
        {
            BBroadcast save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = -6926497733546172658;
        public override long TypeId => TYPEID;

        sealed class Log__protocolType : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BBroadcast)Belong)._protocolType = this.Value; }
        }

        sealed class Log__protocolWholeData : Zeze.Transaction.Log<Zeze.Net.Binary>
        {
            public override void Commit() { ((BBroadcast)Belong)._protocolWholeData = this.Value; }
        }

        sealed class Log__time : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((BBroadcast)Belong)._time = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Provider.BBroadcast: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ProtocolType").Append('=').Append(ProtocolType).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ProtocolWholeData").Append('=').Append(ProtocolWholeData).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Time").Append('=').Append(Time).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                long _x_ = ProtocolType;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                var _x_ = ProtocolWholeData;
                if (_x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                    _o_.WriteBinary(_x_);
                }
            }
            {
                int _x_ = Time;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
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
                ProtocolType = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                ProtocolWholeData = _o_.ReadBinary(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                Time = _o_.ReadInt(_t_);
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
            if (ProtocolType < 0) return true;
            if (Time < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _protocolType = vlog.LongValue(); break;
                    case 2: _protocolWholeData = vlog.BinaryValue(); break;
                    case 3: _time = vlog.IntValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            ProtocolType = 0;
            ProtocolWholeData = Zeze.Net.Binary.Empty;
            Time = 0;
        }
    }
}
