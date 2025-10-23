// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.GlobalCacheManagerWithRaft
{
    public interface LoginParamReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public LoginParam Copy();
        public void BuildString(System.Text.StringBuilder sb, int level);
        public long ObjectId { get; }
        public int VariableId { get; }
        public Zeze.Transaction.TableKey TableKey { get; }
        public bool IsManaged { get; }
        public int CapacityHintOfByteBuffer { get; }

        public int ServerId { get; }
        public int GlobalCacheManagerHashIndex { get; }
        public bool DebugMode { get; }
    }

    public sealed class LoginParam : Zeze.Transaction.Bean, LoginParamReadOnly
    {
        int _ServerId;
        int _GlobalCacheManagerHashIndex;
        bool _DebugMode; // 调试模式下不检查Release Timeout,方便单步调试

        public int ServerId
        {
            get
            {
                if (!IsManaged)
                    return _ServerId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ServerId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ServerId)txn.GetLog(ObjectId + 1);
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
                txn.PutLog(new Log__ServerId() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public int GlobalCacheManagerHashIndex
        {
            get
            {
                if (!IsManaged)
                    return _GlobalCacheManagerHashIndex;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _GlobalCacheManagerHashIndex;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__GlobalCacheManagerHashIndex)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _GlobalCacheManagerHashIndex;
            }
            set
            {
                if (!IsManaged)
                {
                    _GlobalCacheManagerHashIndex = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__GlobalCacheManagerHashIndex() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public bool DebugMode
        {
            get
            {
                if (!IsManaged)
                    return _DebugMode;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _DebugMode;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__DebugMode)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _DebugMode;
            }
            set
            {
                if (!IsManaged)
                {
                    _DebugMode = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__DebugMode() { Belong = this, VariableId = 3, Value = value });
            }
        }

        public LoginParam()
        {
        }

        public LoginParam(int _ServerId_, int _GlobalCacheManagerHashIndex_, bool _DebugMode_)
        {
            _ServerId = _ServerId_;
            _GlobalCacheManagerHashIndex = _GlobalCacheManagerHashIndex_;
            _DebugMode = _DebugMode_;
        }

        public void Assign(LoginParam other)
        {
            ServerId = other.ServerId;
            GlobalCacheManagerHashIndex = other.GlobalCacheManagerHashIndex;
            DebugMode = other.DebugMode;
        }

        public LoginParam CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override LoginParam Copy()
        {
            var copy = new LoginParam();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(LoginParam a, LoginParam b)
        {
            LoginParam save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = 8338257265267188489;
        public override long TypeId => TYPEID;

        sealed class Log__ServerId : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((LoginParam)Belong)._ServerId = this.Value; }
        }

        sealed class Log__GlobalCacheManagerHashIndex : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((LoginParam)Belong)._GlobalCacheManagerHashIndex = this.Value; }
        }

        sealed class Log__DebugMode : Zeze.Transaction.Log<bool>
        {
            public override void Commit() { ((LoginParam)Belong)._DebugMode = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.GlobalCacheManagerWithRaft.LoginParam: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ServerId").Append('=').Append(ServerId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("GlobalCacheManagerHashIndex").Append('=').Append(GlobalCacheManagerHashIndex).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("DebugMode").Append('=').Append(DebugMode).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                int _x_ = ServerId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                int _x_ = GlobalCacheManagerHashIndex;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                bool _x_ = DebugMode;
                if (_x_)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                    _o_.WriteByte(1);
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
                ServerId = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                GlobalCacheManagerHashIndex = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                DebugMode = _o_.ReadBool(_t_);
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
            if (ServerId < 0) return true;
            if (GlobalCacheManagerHashIndex < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _ServerId = vlog.IntValue(); break;
                    case 2: _GlobalCacheManagerHashIndex = vlog.IntValue(); break;
                    case 3: _DebugMode = vlog.BoolValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            ServerId = 0;
            GlobalCacheManagerHashIndex = 0;
            DebugMode = false;
        }
    }
}
