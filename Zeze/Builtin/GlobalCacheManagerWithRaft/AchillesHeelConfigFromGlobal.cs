// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.GlobalCacheManagerWithRaft
{
    public interface AchillesHeelConfigFromGlobalReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public AchillesHeelConfigFromGlobal Copy();

        public int MaxNetPing { get; }
        public int ServerProcessTime { get; }
        public int ServerReleaseTimeout { get; }
    }

    public sealed class AchillesHeelConfigFromGlobal : Zeze.Transaction.Bean, AchillesHeelConfigFromGlobalReadOnly
    {
        int _MaxNetPing;
        int _ServerProcessTime;
        int _ServerReleaseTimeout;

        public int MaxNetPing
        {
            get
            {
                if (!IsManaged)
                    return _MaxNetPing;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _MaxNetPing;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__MaxNetPing)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _MaxNetPing;
            }
            set
            {
                if (!IsManaged)
                {
                    _MaxNetPing = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__MaxNetPing() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public int ServerProcessTime
        {
            get
            {
                if (!IsManaged)
                    return _ServerProcessTime;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ServerProcessTime;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ServerProcessTime)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _ServerProcessTime;
            }
            set
            {
                if (!IsManaged)
                {
                    _ServerProcessTime = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ServerProcessTime() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public int ServerReleaseTimeout
        {
            get
            {
                if (!IsManaged)
                    return _ServerReleaseTimeout;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ServerReleaseTimeout;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ServerReleaseTimeout)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _ServerReleaseTimeout;
            }
            set
            {
                if (!IsManaged)
                {
                    _ServerReleaseTimeout = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ServerReleaseTimeout() { Belong = this, VariableId = 3, Value = value });
            }
        }

        public AchillesHeelConfigFromGlobal()
        {
        }

        public AchillesHeelConfigFromGlobal(int _MaxNetPing_, int _ServerProcessTime_, int _ServerReleaseTimeout_)
        {
            _MaxNetPing = _MaxNetPing_;
            _ServerProcessTime = _ServerProcessTime_;
            _ServerReleaseTimeout = _ServerReleaseTimeout_;
        }

        public void Assign(AchillesHeelConfigFromGlobal other)
        {
            MaxNetPing = other.MaxNetPing;
            ServerProcessTime = other.ServerProcessTime;
            ServerReleaseTimeout = other.ServerReleaseTimeout;
        }

        public AchillesHeelConfigFromGlobal CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override AchillesHeelConfigFromGlobal Copy()
        {
            var copy = new AchillesHeelConfigFromGlobal();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(AchillesHeelConfigFromGlobal a, AchillesHeelConfigFromGlobal b)
        {
            AchillesHeelConfigFromGlobal save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = 279281372612785545;
        public override long TypeId => TYPEID;

        sealed class Log__MaxNetPing : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((AchillesHeelConfigFromGlobal)Belong)._MaxNetPing = this.Value; }
        }

        sealed class Log__ServerProcessTime : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((AchillesHeelConfigFromGlobal)Belong)._ServerProcessTime = this.Value; }
        }

        sealed class Log__ServerReleaseTimeout : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((AchillesHeelConfigFromGlobal)Belong)._ServerReleaseTimeout = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.GlobalCacheManagerWithRaft.AchillesHeelConfigFromGlobal: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("MaxNetPing").Append('=').Append(MaxNetPing).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ServerProcessTime").Append('=').Append(ServerProcessTime).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ServerReleaseTimeout").Append('=').Append(ServerReleaseTimeout).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                int _x_ = MaxNetPing;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                int _x_ = ServerProcessTime;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                int _x_ = ServerReleaseTimeout;
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
                MaxNetPing = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                ServerProcessTime = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                ServerReleaseTimeout = _o_.ReadInt(_t_);
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
            if (MaxNetPing < 0) return true;
            if (ServerProcessTime < 0) return true;
            if (ServerReleaseTimeout < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _MaxNetPing = vlog.IntValue(); break;
                    case 2: _ServerProcessTime = vlog.IntValue(); break;
                    case 3: _ServerReleaseTimeout = vlog.IntValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            MaxNetPing = 0;
            ServerProcessTime = 0;
            ServerReleaseTimeout = 0;
        }
    }
}
