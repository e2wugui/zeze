// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.ProviderDirect
{
    public interface BProviderInfoReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BProviderInfo Copy();
        public void BuildString(System.Text.StringBuilder sb, int level);
        public long ObjectId { get; }
        public int VariableId { get; }
        public Zeze.Transaction.TableKey TableKey { get; }
        public bool IsManaged { get; }
        public int CapacityHintOfByteBuffer { get; }

        public string Ip { get; }
        public int Port { get; }
        public int ServerId { get; }
    }

    public sealed class BProviderInfo : Zeze.Transaction.Bean, BProviderInfoReadOnly
    {
        string _Ip;
        int _Port;
        int _ServerId;

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
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _Ip = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Ip() { Belong = this, VariableId = 1, Value = value });
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
                txn.PutLog(new Log__Port() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public int ServerId
        {
            get
            {
                if (!IsManaged)
                    return _ServerId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ServerId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ServerId)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _ServerId;
            }
            set
            {
                if (!IsManaged)
                {
                    _ServerId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ServerId() { Belong = this, VariableId = 3, Value = value });
            }
        }

        public BProviderInfo()
        {
            _Ip = "";
        }

        public BProviderInfo(string _Ip_, int _Port_, int _ServerId_)
        {
            _Ip = _Ip_;
            _Port = _Port_;
            _ServerId = _ServerId_;
        }

        public void Assign(BProviderInfo other)
        {
            Ip = other.Ip;
            Port = other.Port;
            ServerId = other.ServerId;
        }

        public BProviderInfo CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BProviderInfo Copy()
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

        public const long TYPEID = 858135112612157161;
        public override long TypeId => TYPEID;

        sealed class Log__Ip : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BProviderInfo)Belong)._Ip = this.Value; }
        }

        sealed class Log__Port : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((BProviderInfo)Belong)._Port = this.Value; }
        }

        sealed class Log__ServerId : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((BProviderInfo)Belong)._ServerId = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.ProviderDirect.BProviderInfo: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Ip").Append('=').Append(Ip).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Port").Append('=').Append(Port).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ServerId").Append('=').Append(ServerId).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                string _x_ = Ip;
                if (_x_ != null && _x_.Length != 0)
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
            {
                int _x_ = ServerId;
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
                Ip = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                Port = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                ServerId = _o_.ReadInt(_t_);
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
            if (Port < 0) return true;
            if (ServerId < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _Ip = vlog.StringValue(); break;
                    case 2: _Port = vlog.IntValue(); break;
                    case 3: _ServerId = vlog.IntValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            Ip = "";
            Port = 0;
            ServerId = 0;
        }
    }
}
