// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.ProviderDirect
{
    public interface BModuleRedirectAllResultReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BModuleRedirectAllResult Copy();

        public int ModuleId { get; }
        public int ServerId { get; }
        public long SourceProvider { get; }
        public string MethodFullName { get; }
        public long SessionId { get; }
        public System.Collections.Generic.IReadOnlyDictionary<int,Zeze.Builtin.ProviderDirect.BModuleRedirectAllHashReadOnly> Hashs { get; }
    }

    public sealed class BModuleRedirectAllResult : Zeze.Transaction.Bean, BModuleRedirectAllResultReadOnly
    {
        int _ModuleId;
        int _ServerId; // 目标server的id。
        long _SourceProvider; // 从BModuleRedirectAllRequest里面得到。
        string _MethodFullName; // format="ModuleFullName:MethodName"
        long _SessionId; // 发起请求者初始化，返回结果时带回。
        readonly Zeze.Transaction.Collections.CollMap2<int, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash> _Hashs; // 发送给具体进程时需要处理的分组hash-index（目前由linkd填写）
        readonly Zeze.Transaction.Collections.CollMapReadOnly<int,Zeze.Builtin.ProviderDirect.BModuleRedirectAllHashReadOnly,Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash> _HashsReadOnly;

        public int ModuleId
        {
            get
            {
                if (!IsManaged)
                    return _ModuleId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ModuleId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ModuleId)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _ModuleId;
            }
            set
            {
                if (!IsManaged)
                {
                    _ModuleId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ModuleId() { Belong = this, VariableId = 1, Value = value });
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
                var log = (Log__ServerId)txn.GetLog(ObjectId + 2);
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
                txn.PutLog(new Log__ServerId() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public long SourceProvider
        {
            get
            {
                if (!IsManaged)
                    return _SourceProvider;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _SourceProvider;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__SourceProvider)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _SourceProvider;
            }
            set
            {
                if (!IsManaged)
                {
                    _SourceProvider = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__SourceProvider() { Belong = this, VariableId = 3, Value = value });
            }
        }

        public string MethodFullName
        {
            get
            {
                if (!IsManaged)
                    return _MethodFullName;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _MethodFullName;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__MethodFullName)txn.GetLog(ObjectId + 4);
                return log != null ? log.Value : _MethodFullName;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _MethodFullName = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__MethodFullName() { Belong = this, VariableId = 4, Value = value });
            }
        }

        public long SessionId
        {
            get
            {
                if (!IsManaged)
                    return _SessionId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _SessionId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__SessionId)txn.GetLog(ObjectId + 5);
                return log != null ? log.Value : _SessionId;
            }
            set
            {
                if (!IsManaged)
                {
                    _SessionId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__SessionId() { Belong = this, VariableId = 5, Value = value });
            }
        }

        public Zeze.Transaction.Collections.CollMap2<int, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash> Hashs => _Hashs;
        System.Collections.Generic.IReadOnlyDictionary<int,Zeze.Builtin.ProviderDirect.BModuleRedirectAllHashReadOnly> Zeze.Builtin.ProviderDirect.BModuleRedirectAllResultReadOnly.Hashs => _HashsReadOnly;

        public BModuleRedirectAllResult()
        {
            _MethodFullName = "";
            _Hashs = new Zeze.Transaction.Collections.CollMap2<int, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash>() { VariableId = 6 };
            _HashsReadOnly = new Zeze.Transaction.Collections.CollMapReadOnly<int,Zeze.Builtin.ProviderDirect.BModuleRedirectAllHashReadOnly,Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash>(_Hashs);
        }

        public BModuleRedirectAllResult(int _ModuleId_, int _ServerId_, long _SourceProvider_, string _MethodFullName_, long _SessionId_)
        {
            _ModuleId = _ModuleId_;
            _ServerId = _ServerId_;
            _SourceProvider = _SourceProvider_;
            _MethodFullName = _MethodFullName_;
            _SessionId = _SessionId_;
            _Hashs = new Zeze.Transaction.Collections.CollMap2<int, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash>() { VariableId = 6 };
            _HashsReadOnly = new Zeze.Transaction.Collections.CollMapReadOnly<int,Zeze.Builtin.ProviderDirect.BModuleRedirectAllHashReadOnly,Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash>(_Hashs);
        }

        public void Assign(BModuleRedirectAllResult other)
        {
            ModuleId = other.ModuleId;
            ServerId = other.ServerId;
            SourceProvider = other.SourceProvider;
            MethodFullName = other.MethodFullName;
            SessionId = other.SessionId;
            Hashs.Clear();
            foreach (var e in other.Hashs)
                Hashs.Add(e.Key, e.Value.Copy());
        }

        public BModuleRedirectAllResult CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BModuleRedirectAllResult Copy()
        {
            var copy = new BModuleRedirectAllResult();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BModuleRedirectAllResult a, BModuleRedirectAllResult b)
        {
            BModuleRedirectAllResult save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = -6979067915808179070;
        public override long TypeId => TYPEID;

        sealed class Log__ModuleId : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((BModuleRedirectAllResult)Belong)._ModuleId = this.Value; }
        }

        sealed class Log__ServerId : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((BModuleRedirectAllResult)Belong)._ServerId = this.Value; }
        }

        sealed class Log__SourceProvider : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BModuleRedirectAllResult)Belong)._SourceProvider = this.Value; }
        }

        sealed class Log__MethodFullName : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BModuleRedirectAllResult)Belong)._MethodFullName = this.Value; }
        }

        sealed class Log__SessionId : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BModuleRedirectAllResult)Belong)._SessionId = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.ProviderDirect.BModuleRedirectAllResult: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ModuleId").Append('=').Append(ModuleId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ServerId").Append('=').Append(ServerId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("SourceProvider").Append('=').Append(SourceProvider).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("MethodFullName").Append('=').Append(MethodFullName).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("SessionId").Append('=').Append(SessionId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Hashs").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var _kv_ in Hashs)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append('(').Append(Environment.NewLine);
                var Key = _kv_.Key;
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Key").Append('=').Append(Key).Append(',').Append(Environment.NewLine);
                var Value = _kv_.Value;
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Value").Append('=').Append(Environment.NewLine);
                Value.BuildString(sb, level + 4);
                sb.Append(',').Append(Environment.NewLine);
                sb.Append(Zeze.Util.Str.Indent(level)).Append(')').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                int _x_ = ModuleId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                int _x_ = ServerId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                long _x_ = SourceProvider;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                string _x_ = MethodFullName;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                long _x_ = SessionId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                var _x_ = Hashs;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.MAP);
                    _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                    foreach (var _e_ in _x_)
                    {
                        _o_.WriteLong(_e_.Key);
                        _e_.Value.Encode(_o_);
                        _n_--;
                    }
                    if (_n_ != 0)
                        throw new System.Exception(_n_.ToString());
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
                ModuleId = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                ServerId = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                SourceProvider = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                MethodFullName = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 5)
            {
                SessionId = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 6)
            {
                var _x_ = Hashs;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP)
                {
                    int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                    for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--)
                    {
                        var _k_ = _o_.ReadInt(_s_);
                        var _v_ = _o_.ReadBean(new Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash(), _t_);
                        _x_.Add(_k_, _v_);
                    }
                }
                else
                    _o_.SkipUnknownFieldOrThrow(_t_, "Map");
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
            _Hashs.InitRootInfo(root, this);
        }

        protected override void InitChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root)
        {
            _Hashs.InitRootInfoWithRedo(root, this);
        }

        public override bool NegativeCheck()
        {
            if (ModuleId < 0) return true;
            if (ServerId < 0) return true;
            if (SourceProvider < 0) return true;
            if (SessionId < 0) return true;
            foreach (var _v_ in Hashs.Values)
            {
                if (_v_.NegativeCheck()) return true;
            }
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _ModuleId = vlog.IntValue(); break;
                    case 2: _ServerId = vlog.IntValue(); break;
                    case 3: _SourceProvider = vlog.LongValue(); break;
                    case 4: _MethodFullName = vlog.StringValue(); break;
                    case 5: _SessionId = vlog.LongValue(); break;
                    case 6: _Hashs.FollowerApply(vlog); break;
                }
            }
        }

        public override void ClearParameters()
        {
            ModuleId = 0;
            ServerId = 0;
            SourceProvider = 0;
            MethodFullName = "";
            SessionId = 0;
            Hashs.Clear();
        }
    }
}
