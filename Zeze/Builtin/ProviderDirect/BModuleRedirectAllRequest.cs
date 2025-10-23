// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.ProviderDirect
{
    public interface BModuleRedirectAllRequestReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BModuleRedirectAllRequest Copy();
        public void BuildString(System.Text.StringBuilder sb, int level);
        public long ObjectId { get; }
        public int VariableId { get; }
        public Zeze.Transaction.TableKey TableKey { get; }
        public bool IsManaged { get; }
        public int CapacityHintOfByteBuffer { get; }

        public int ModuleId { get; }
        public int HashCodeConcurrentLevel { get; }
        public System.Collections.Generic.IReadOnlySet<int> HashCodes { get; }
        public long SourceProvider { get; }
        public long SessionId { get; }
        public string MethodFullName { get; }
        public Zeze.Net.Binary Params { get; }
        public string ServiceNamePrefix { get; }
    }

    public sealed class BModuleRedirectAllRequest : Zeze.Transaction.Bean, BModuleRedirectAllRequestReadOnly
    {
        int _ModuleId;
        int _HashCodeConcurrentLevel; // 总的并发分组数量
        readonly Zeze.Transaction.Collections.CollSet1<int> _HashCodes; // 发送给具体进程时需要处理的分组hash-index（目前由linkd填写）
        long _SourceProvider; // linkd 转发的时候填写本地provider的sessionId。
        long _SessionId; // 发起请求者初始化，返回结果时带回。
        string _MethodFullName; // format="ModuleFullName:MethodName"
        Zeze.Net.Binary _Params;
        string _ServiceNamePrefix;

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

        public int HashCodeConcurrentLevel
        {
            get
            {
                if (!IsManaged)
                    return _HashCodeConcurrentLevel;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _HashCodeConcurrentLevel;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__HashCodeConcurrentLevel)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _HashCodeConcurrentLevel;
            }
            set
            {
                if (!IsManaged)
                {
                    _HashCodeConcurrentLevel = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__HashCodeConcurrentLevel() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public Zeze.Transaction.Collections.CollSet1<int> HashCodes => _HashCodes;
        System.Collections.Generic.IReadOnlySet<int> Zeze.Builtin.ProviderDirect.BModuleRedirectAllRequestReadOnly.HashCodes => _HashCodes;

        public long SourceProvider
        {
            get
            {
                if (!IsManaged)
                    return _SourceProvider;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _SourceProvider;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__SourceProvider)txn.GetLog(ObjectId + 4);
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
                txn.PutLog(new Log__SourceProvider() { Belong = this, VariableId = 4, Value = value });
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

        public string MethodFullName
        {
            get
            {
                if (!IsManaged)
                    return _MethodFullName;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _MethodFullName;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__MethodFullName)txn.GetLog(ObjectId + 6);
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
                txn.PutLog(new Log__MethodFullName() { Belong = this, VariableId = 6, Value = value });
            }
        }

        public Zeze.Net.Binary Params
        {
            get
            {
                if (!IsManaged)
                    return _Params;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Params;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Params)txn.GetLog(ObjectId + 7);
                return log != null ? log.Value : _Params;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _Params = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Params() { Belong = this, VariableId = 7, Value = value });
            }
        }

        public string ServiceNamePrefix
        {
            get
            {
                if (!IsManaged)
                    return _ServiceNamePrefix;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ServiceNamePrefix;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ServiceNamePrefix)txn.GetLog(ObjectId + 8);
                return log != null ? log.Value : _ServiceNamePrefix;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _ServiceNamePrefix = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ServiceNamePrefix() { Belong = this, VariableId = 8, Value = value });
            }
        }

        public BModuleRedirectAllRequest()
        {
            _HashCodes = new Zeze.Transaction.Collections.CollSet1<int>() { VariableId = 3 };
            _MethodFullName = "";
            _Params = Zeze.Net.Binary.Empty;
            _ServiceNamePrefix = "";
        }

        public BModuleRedirectAllRequest(int _ModuleId_, int _HashCodeConcurrentLevel_, long _SourceProvider_, long _SessionId_, string _MethodFullName_, Zeze.Net.Binary _Params_, string _ServiceNamePrefix_)
        {
            _ModuleId = _ModuleId_;
            _HashCodeConcurrentLevel = _HashCodeConcurrentLevel_;
            _HashCodes = new Zeze.Transaction.Collections.CollSet1<int>() { VariableId = 3 };
            _SourceProvider = _SourceProvider_;
            _SessionId = _SessionId_;
            _MethodFullName = _MethodFullName_;
            _Params = _Params_;
            _ServiceNamePrefix = _ServiceNamePrefix_;
        }

        public void Assign(BModuleRedirectAllRequest other)
        {
            ModuleId = other.ModuleId;
            HashCodeConcurrentLevel = other.HashCodeConcurrentLevel;
            HashCodes.Clear();
            foreach (var e in other.HashCodes)
                HashCodes.Add(e);
            SourceProvider = other.SourceProvider;
            SessionId = other.SessionId;
            MethodFullName = other.MethodFullName;
            Params = other.Params;
            ServiceNamePrefix = other.ServiceNamePrefix;
        }

        public BModuleRedirectAllRequest CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BModuleRedirectAllRequest Copy()
        {
            var copy = new BModuleRedirectAllRequest();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BModuleRedirectAllRequest a, BModuleRedirectAllRequest b)
        {
            BModuleRedirectAllRequest save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = -1938324199607833342;
        public override long TypeId => TYPEID;

        sealed class Log__ModuleId : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((BModuleRedirectAllRequest)Belong)._ModuleId = this.Value; }
        }

        sealed class Log__HashCodeConcurrentLevel : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((BModuleRedirectAllRequest)Belong)._HashCodeConcurrentLevel = this.Value; }
        }


        sealed class Log__SourceProvider : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BModuleRedirectAllRequest)Belong)._SourceProvider = this.Value; }
        }

        sealed class Log__SessionId : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BModuleRedirectAllRequest)Belong)._SessionId = this.Value; }
        }

        sealed class Log__MethodFullName : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BModuleRedirectAllRequest)Belong)._MethodFullName = this.Value; }
        }

        sealed class Log__Params : Zeze.Transaction.Log<Zeze.Net.Binary>
        {
            public override void Commit() { ((BModuleRedirectAllRequest)Belong)._Params = this.Value; }
        }

        sealed class Log__ServiceNamePrefix : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BModuleRedirectAllRequest)Belong)._ServiceNamePrefix = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.ProviderDirect.BModuleRedirectAllRequest: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ModuleId").Append('=').Append(ModuleId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("HashCodeConcurrentLevel").Append('=').Append(HashCodeConcurrentLevel).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("HashCodes").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var Item in HashCodes)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Item").Append('=').Append(Item).Append(',').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("SourceProvider").Append('=').Append(SourceProvider).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("SessionId").Append('=').Append(SessionId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("MethodFullName").Append('=').Append(MethodFullName).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Params").Append('=').Append(Params).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ServiceNamePrefix").Append('=').Append(ServiceNamePrefix).Append(Environment.NewLine);
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
                int _x_ = HashCodeConcurrentLevel;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                var _x_ = HashCodes;
                int _n_ = _x_?.Count ?? 0;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                    _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                    foreach (var _v_ in _x_)
                    {
                        _o_.WriteLong(_v_);
                        _n_--;
                    }
                    if (_n_ != 0)
                        throw new System.Exception(_n_.ToString());
                }
            }
            {
                long _x_ = SourceProvider;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
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
                string _x_ = MethodFullName;
                if (_x_ != null && _x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                var _x_ = Params;
                if (_x_ != null && _x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.BYTES);
                    _o_.WriteBinary(_x_);
                }
            }
            {
                string _x_ = ServiceNamePrefix;
                if (_x_ != null && _x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
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
                HashCodeConcurrentLevel = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                var _x_ = HashCodes;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST)
                {
                    for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    {
                        _x_.Add(_o_.ReadInt(_t_));
                    }
                }
                else
                    _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                SourceProvider = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 5)
            {
                SessionId = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 6)
            {
                MethodFullName = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 7)
            {
                Params = _o_.ReadBinary(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 8)
            {
                ServiceNamePrefix = _o_.ReadString(_t_);
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
            _HashCodes.InitRootInfo(root, this);
        }

        protected override void InitChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root)
        {
            _HashCodes.InitRootInfoWithRedo(root, this);
        }

        public override bool NegativeCheck()
        {
            if (ModuleId < 0) return true;
            if (HashCodeConcurrentLevel < 0) return true;
            foreach (var _v_ in HashCodes)
            {
                if (_v_ < 0) return true;
            }
            if (SourceProvider < 0) return true;
            if (SessionId < 0) return true;
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
                    case 2: _HashCodeConcurrentLevel = vlog.IntValue(); break;
                    case 3: _HashCodes.FollowerApply(vlog); break;
                    case 4: _SourceProvider = vlog.LongValue(); break;
                    case 5: _SessionId = vlog.LongValue(); break;
                    case 6: _MethodFullName = vlog.StringValue(); break;
                    case 7: _Params = vlog.BinaryValue(); break;
                    case 8: _ServiceNamePrefix = vlog.StringValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            ModuleId = 0;
            HashCodeConcurrentLevel = 0;
            HashCodes.Clear();
            SourceProvider = 0;
            SessionId = 0;
            MethodFullName = "";
            Params = Zeze.Net.Binary.Empty;
            ServiceNamePrefix = "";
        }
    }
}
